package com.howdy.echowave.domain.recommendation

import com.howdy.echowave.domain.dna.MusicDnaProfile
import com.howdy.echowave.domain.model.Track
import com.howdy.echowave.domain.model.TrackIdentity
import java.util.Locale

data class RecommendationExplanation(
    val trackId: String,
    val sessionScore: Float,
    val dnaScore: Float,
    val similarityScore: Float,
    val historyScore: Float,
    val discoveryScore: Float,
    val penalties: Float,
    val totalScore: Float,
    val details: String,
)

/**
 * Deterministic, explainable recommendation scoring model based on:
 * - Current session context (30%)
 * - Long-term Music DNA (25%)
 * - Musical/Metadata similarity (20%)
 * - Positive listening history (15%)
 * - Relevant discovery (10%)
 * With penalties for recent repetition, skip fatigue, and artist saturation.
 */
class RecommendationScorer(
    val sessionWeight: Float = 0.30f,
    val dnaWeight: Float = 0.25f,
    val similarityWeight: Float = 0.20f,
    val historyWeight: Float = 0.15f,
    val discoveryWeight: Float = 0.10f,
) {
    fun score(
        seed: Track,
        candidate: Track,
        session: SessionTasteTracker? = null,
        dnaProfile: MusicDnaProfile? = null,
        favoriteTrackIds: Set<String> = emptySet(),
        recentlyPlayedIdentities: Set<TrackIdentity> = emptySet(),
        queuedArtistsCount: Map<String, Int> = emptyMap(),
    ): Pair<Float, RecommendationExplanation> {
        val seedFeatures = TrackFeatureExtractor.extract(seed)
        val candidateFeatures = TrackFeatureExtractor.extract(candidate)

        // Blend the current seed with recent session behavior. This gives a
        // useful radio for a first-time listener and lets repeated skips or
        // plays steer it as the session develops.
        val seedStyleMatch = styleMatch(seedFeatures.styles, candidateFeatures.styles)
        val seedLanguageMatch = languageMatch(seedFeatures.language, candidateFeatures.language)
        val seedContext = (seedStyleMatch * 0.65f + seedLanguageMatch * 0.35f)
        var rawSession = seedContext
        if (session != null) {
            val langAffinity = session.languageAffinity(candidateFeatures.language)
            val styleScore = candidateFeatures.styles.map { session.styleAffinity(it) }.maxOrNull() ?: 0f
            val sessionScore = (50f + (langAffinity * 25f) + (styleScore * 25f)).coerceIn(0f, 100f)
            rawSession = (seedContext * 0.55f + sessionScore * 0.45f).coerceIn(0f, 100f)
        }

        // 2. Long-term Personal Preference / Music DNA (25%)
        // Normalized by profile totals so long histories don't saturate to 100.
        var rawDna = 40f
        if (dnaProfile != null) {
            val normArtist = candidate.artist.trim().lowercase(Locale.ROOT)
            val topArtistTotal = dnaProfile.topArtists.sumOf { it.score }.coerceAtLeast(1.0)
            val topArtistBest = dnaProfile.topArtists.maxOfOrNull { it.score } ?: 0.0
            val artistScore = dnaProfile.topArtists.find {
                it.key == normArtist || it.label.lowercase(Locale.ROOT) == normArtist
            }?.score ?: 0.0
            val langTotal = dnaProfile.languages.sumOf { it.score }.coerceAtLeast(1.0)
            val langScore = candidateFeatures.language?.let { lang ->
                dnaProfile.languages.find { it.key.equals(lang, ignoreCase = true) }?.score
            } ?: 0.0
            // Relative share (0..1) scaled to 0..100, blended 60/40 artist/lang.
            val artistShare = if (topArtistBest > 0) (artistScore / topArtistBest).coerceIn(0.0, 1.0) else 0.0
            val langShare = (langScore / langTotal).coerceIn(0.0, 1.0)
            // Guard against unused totals (kept for future absolute scaling).
            @Suppress("UNUSED_VARIABLE")
            val totals = topArtistTotal + langTotal
            rawDna = ((artistShare * 60.0) + (langShare * 40.0)).toFloat().coerceIn(0f, 100f)
            if (rawDna == 0f && (dnaProfile.topArtists.isEmpty() && dnaProfile.languages.isEmpty())) {
                rawDna = 50f // Cold-start neutrality
            }
        }

        // 3. Musical/Metadata Similarity (20%)
        // Title overlap is deliberately excluded: it tends to promote more
        // search-result variants instead of finding a different song in the
        // same style. Mood/style and language provide the useful similarity.
        var rawSim = seedStyleMatch * 0.70f + seedLanguageMatch * 0.25f
        if (seedFeatures.era != null && seedFeatures.era == candidateFeatures.era) rawSim += 5f
        rawSim = rawSim.coerceIn(0f, 100f)

        // 4. Positive History (15%)
        var rawHistory = 0f
        if (candidate.id in favoriteTrackIds) {
            rawHistory = 100f
        }

        // 5. Relevant Discovery (10%)
        val candidateIdentity = TrackIdentity.from(candidate)
        val isUnplayed = candidateIdentity !in recentlyPlayedIdentities
        val rawDiscovery = if (isUnplayed && rawSim >= 35f) 80f else 20f

        // Penalties
        var penalties = 0f
        if (candidateIdentity in recentlyPlayedIdentities) {
            penalties += 100f
        }
        if (session != null && session.isArtistFatigued(candidate.artist)) {
            penalties += 60f
        }
        val artistKey = candidate.artist.trim().lowercase(Locale.ROOT)
        val artistQueueCount = queuedArtistsCount[artistKey] ?: 0
        if (artistQueueCount >= 2) {
            penalties += (artistQueueCount * 25f)
        }

        val weightedSum = (rawSession * sessionWeight) +
            (rawDna * dnaWeight) +
            (rawSim * similarityWeight) +
            (rawHistory * historyWeight) +
            (rawDiscovery * discoveryWeight)

        val finalScore = (weightedSum - penalties).coerceAtLeast(0f)

        val explanation = RecommendationExplanation(
            trackId = candidate.id,
            sessionScore = rawSession,
            dnaScore = rawDna,
            similarityScore = rawSim,
            historyScore = rawHistory,
            discoveryScore = rawDiscovery,
            penalties = penalties,
            totalScore = finalScore,
            details = "Session=${rawSession.toInt()}, DNA=${rawDna.toInt()}, Sim=${rawSim.toInt()}, Hist=${rawHistory.toInt()}, Disc=${rawDiscovery.toInt()}, Pen=-${penalties.toInt()}",
        )

        return finalScore to explanation
    }

    private fun styleMatch(seed: Set<TrackStyle>, candidate: Set<TrackStyle>): Float {
        if (seed.isEmpty()) return if (candidate.isEmpty()) 50f else 40f
        if (candidate.isEmpty()) return 30f
        val overlap = seed.intersect(candidate).size.toFloat() / seed.size.toFloat()
        return (overlap * 100f).coerceIn(0f, 100f)
    }

    private fun languageMatch(seed: String?, candidate: String?): Float = when {
        seed == null || candidate == null -> 50f
        seed.equals(candidate, ignoreCase = true) -> 100f
        else -> 0f
    }
}
