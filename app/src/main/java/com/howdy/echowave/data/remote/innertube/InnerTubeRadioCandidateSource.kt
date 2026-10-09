package com.howdy.echowave.data.remote.innertube

import com.howdy.echowave.core.common.AppResult
import com.howdy.echowave.domain.model.Track
import com.howdy.echowave.domain.recommendation.CandidateCache
import com.howdy.echowave.domain.recommendation.CandidateContext
import com.howdy.echowave.domain.recommendation.CandidateSource
import com.howdy.echowave.domain.recommendation.TrackCandidate

/**
 * Queries InnerTube `/next` radio endpoint with automatic LRU caching.
 * A single API response provides 15-25 tracks that satisfy subsequent
 * queue advance requests without repetitive network hits.
 */
class InnerTubeRadioCandidateSource(
    private val api: InnerTubeApi,
    private val visitorStore: VisitorStore,
    private val cache: CandidateCache = CandidateCache(),
) : CandidateSource {
    override val id = "innertube-radio"

    override suspend fun fetch(context: CandidateContext, limit: Int): AppResult<List<TrackCandidate>> {
        if (limit <= 0) return AppResult.Ok(emptyList())
        val seedId = context.seed.id
        if (seedId.isBlank()) return AppResult.Err("Radio seed is missing")

        // Check in-memory cache to save network bandwidth and deliver instant transitions
        val cached = cache.get(seedId)
        if (cached != null && cached.isNotEmpty()) {
            return AppResult.Ok(cached.take(limit).map { it.toCandidate() })
        }

        return try {
            val body = nextBody(seedId, visitorStore.current())
            val root = api.next(
                clientId = INNERTUBE_CLIENT_ID,
                clientVersion = INNERTUBE_CLIENT_VERSION,
                body = body,
            )
            visitorStore.offer(extractVisitorData(root))
            val tracks = InnerTubeNextParser.parse(root, limit = limit * 2)
            if (tracks.isNotEmpty()) {
                cache.put(seedId, tracks)
                AppResult.Ok(tracks.take(limit).map { it.toCandidate() })
            } else {
                AppResult.Err("No related radio tracks returned for: $seedId")
            }
        } catch (e: Exception) {
            AppResult.Err("Radio candidate fetch failed: ${e.message}")
        }
    }

    private fun Track.toCandidate(): TrackCandidate = TrackCandidate(
        track = this,
        sourceId = id,
        relevanceScore = 80,
    )
}
