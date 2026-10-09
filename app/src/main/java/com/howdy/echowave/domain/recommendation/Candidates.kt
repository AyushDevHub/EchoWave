package com.howdy.echowave.domain.recommendation

import com.howdy.echowave.core.common.AppResult
import com.howdy.echowave.domain.model.Track
import com.howdy.echowave.domain.model.TrackIdentity
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

data class CandidateContext(
    val seed: Track,
    val played: Set<TrackIdentity> = emptySet(),
    val preferredArtists: Set<String> = emptySet(),
    val maxPerArtist: Int = 3,
    val nowEpochMs: Long = System.currentTimeMillis(),
    val sessionTaste: SessionTasteTracker? = null,
    val dnaProfile: com.howdy.echowave.domain.dna.MusicDnaProfile? = null,
    val favoriteIds: Set<String> = emptySet(),
    val queuedArtistsCount: Map<String, Int> = emptyMap(),
)

data class TrackCandidate(
    val track: Track,
    val identity: TrackIdentity = TrackIdentity.from(track),
    val sourceId: String,
    val relevanceScore: Int = 0,
    val tasteScore: Int = 0,
    val publishedAtEpochMs: Long? = null,
)

interface CandidateSource {
    val id: String
    suspend fun fetch(context: CandidateContext, limit: Int): AppResult<List<TrackCandidate>>
}

fun interface CandidateFilter {
    fun apply(context: CandidateContext, candidates: List<TrackCandidate>): List<TrackCandidate>
}

fun interface CandidateSorter {
    fun score(context: CandidateContext, candidate: TrackCandidate): Long
}

fun interface CandidateDeduper {
    fun dedupe(candidates: List<TrackCandidate>): List<TrackCandidate>
}

/** Runs source fanout, filters, stable sorters, then identity deduplication. */
class CandidatePipeline(
    private val sources: List<CandidateSource>,
    private val filters: List<CandidateFilter> = defaultFilters(),
    private val sorters: List<CandidateSorter> = listOf(WeightedRecommendationSorter(), RelevanceSorter, TasteSorter, FreshnessSorter),
    private val deduper: CandidateDeduper = IdentityCandidateDeduper,
) {
    suspend fun recommend(context: CandidateContext, limit: Int = DEFAULT_LIMIT): AppResult<List<TrackCandidate>> {
        if (limit <= 0) return AppResult.Ok(emptyList())
        if (sources.isEmpty()) return AppResult.Ok(emptyList())

        // Parallel fanout: total latency = slowest source, not sum.
        val fetched: List<Pair<CandidateSource, AppResult<List<TrackCandidate>>>> = coroutineScope {
            sources.map { source ->
                async {
                    val result = try {
                        source.fetch(context, limit)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        AppResult.Err(error.message ?: "${source.id} failed")
                    }
                    source to result
                }
            }.awaitAll()
        }
        val candidates = mutableListOf<TrackCandidate>()
        var successfulSources = 0
        var lastError: String? = null
        for ((_, result) in fetched) {
            when (result) {
                is AppResult.Ok -> {
                    successfulSources++
                    candidates += result.value
                }
                is AppResult.Err -> lastError = result.message
            }
        }
        if (successfulSources == 0) return AppResult.Err(lastError ?: "No candidate source is available")

        return try {
            var processed: List<TrackCandidate> = candidates
            filters.forEach { processed = it.apply(context, processed) }
            // Precompute scores once (O(n)) instead of per-comparison O(n log n).
            val scored = processed.map { candidate ->
                val keys = sorters.map { it.score(context, candidate) }
                candidate to keys
            }
            val ordered = scored.withIndex().sortedWith(
                Comparator<IndexedValue<Pair<TrackCandidate, List<Long>>>> { left, right ->
                    for (i in sorters.indices) {
                        val comparison = right.value.second[i].compareTo(left.value.second[i])
                        if (comparison != 0) return@Comparator comparison
                    }
                    left.index.compareTo(right.index)
                },
            ).map { it.value.first }
            AppResult.Ok(deduper.dedupe(ordered).take(limit))
        } catch (error: Exception) {
            AppResult.Err(error.message ?: "Could not rank next songs")
        }
    }

    companion object {
        const val DEFAULT_LIMIT = 25

        private fun defaultFilters() = listOf(
            CandidateFilter(::filterSeedAndPlayed),
            CandidateFilter(::filterJunk),
            CandidateFilter(::limitArtists),
        )

        private fun filterSeedAndPlayed(
            context: CandidateContext,
            candidates: List<TrackCandidate>,
        ): List<TrackCandidate> {
            val seedIdentity = TrackIdentity.from(context.seed)
            return candidates.filter { it.identity != seedIdentity && it.identity !in context.played }
        }

        private fun filterJunk(
            @Suppress("UNUSED_PARAMETER") context: CandidateContext,
            candidates: List<TrackCandidate>,
        ): List<TrackCandidate> {
            // Note: "mashup" intentionally kept (REMIX_CLUB style ranks it);
            // junk here is non-music filler only.
            val junk = setOf("reaction", "review", "interview", "compilation", "non stop", "nonstop")
            return candidates.filter { candidate ->
                val title = candidate.track.title.lowercase(Locale.ROOT)
                !candidate.track.isVideo && !candidate.track.isEpisode && junk.none(title::contains)
            }
        }

        private fun limitArtists(
            context: CandidateContext,
            candidates: List<TrackCandidate>,
        ): List<TrackCandidate> {
            val max = context.maxPerArtist.coerceAtLeast(1)
            val artistCounts = mutableMapOf<String, Int>()
            return candidates.filter { candidate ->
                val artist = candidate.track.artist.trim().lowercase(Locale.ROOT)
                if (artist.isBlank() || artist == "unknown artist") true
                else {
                    val next = (artistCounts[artist] ?: 0) + 1
                    artistCounts[artist] = next
                    next <= max
                }
            }
        }
    }
}

object IdentityCandidateDeduper : CandidateDeduper {
    override fun dedupe(candidates: List<TrackCandidate>): List<TrackCandidate> {
        val seen = mutableSetOf<TrackIdentity>()
        return candidates.filter { seen.add(it.identity) }
    }
}

object RelevanceSorter : CandidateSorter {
    override fun score(context: CandidateContext, candidate: TrackCandidate): Long = candidate.relevanceScore.toLong()
}

object TasteSorter : CandidateSorter {
    override fun score(context: CandidateContext, candidate: TrackCandidate): Long =
        candidate.tasteScore.toLong() + if (candidate.track.artist.lowercase(Locale.ROOT) in context.preferredArtists) 100 else 0
}

object FreshnessSorter : CandidateSorter {
    override fun score(context: CandidateContext, candidate: TrackCandidate): Long =
        candidate.publishedAtEpochMs ?: Long.MIN_VALUE
}

class WeightedRecommendationSorter(
    private val scorer: RecommendationScorer = RecommendationScorer(),
) : CandidateSorter {
    override fun score(context: CandidateContext, candidate: TrackCandidate): Long {
        val (finalScore, _) = scorer.score(
            seed = context.seed,
            candidate = candidate.track,
            session = context.sessionTaste,
            dnaProfile = context.dnaProfile,
            favoriteTrackIds = context.favoriteIds,
            recentlyPlayedIdentities = context.played,
            queuedArtistsCount = context.queuedArtistsCount,
        )
        return (finalScore * 1000f).toLong()
    }
}
