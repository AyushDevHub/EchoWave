package com.howdy.echowave.domain.recommendation

import com.howdy.echowave.domain.model.Track
import java.util.Locale

/**
 * Maintains short-term session context (rolling window of recent plays/skips).
 * Allows recommendations to dynamically pivot when user taste shifts or when
 * rapid early-skips occur.
 */
class SessionTasteTracker(
    private val maxHistory: Int = 8,
) {
    private val history = mutableListOf<SessionEntry>()

    data class SessionEntry(
        val track: Track,
        val features: ExtractedTrackFeatures,
        val completed: Boolean,
        val earlySkip: Boolean,
        val timestamp: Long = System.currentTimeMillis(),
    )

    @Synchronized
    fun record(track: Track, completed: Boolean, earlySkip: Boolean) {
        val features = TrackFeatureExtractor.extract(track)
        if (history.size >= maxHistory) {
            history.removeAt(0)
        }
        history.add(SessionEntry(track, features, completed, earlySkip))
    }

    @Synchronized
    fun recentPlayedTracks(): List<Track> = history.map { it.track }

    @Synchronized
    fun recentSkippedEarly(): List<Track> = history.filter { it.earlySkip }.map { it.track }

    /**
     * Calculates affinity for a given language in the current session [-1.0 .. 1.0].
     * Recency is exponentially weighted (2^index) so latest plays dominate.
     */
    @Synchronized
    fun languageAffinity(language: String?): Float {
        if (language == null || history.isEmpty()) return 0f
        var score = 0f
        var weightTotal = 0f
        history.forEachIndexed { index, entry ->
            val recencyWeight = (1 shl index.coerceAtMost(20)).toFloat()
            weightTotal += recencyWeight
            if (entry.features.language.equals(language, ignoreCase = true)) {
                score += when {
                    entry.earlySkip -> -recencyWeight
                    entry.completed -> recencyWeight
                    else -> -recencyWeight * 0.5f
                }
            }
        }
        return if (weightTotal > 0f) (score / weightTotal).coerceIn(-1f, 1f) else 0f
    }

    /**
     * Calculates affinity for a given track style in the current session [-1.0 .. 1.0].
     */
    @Synchronized
    fun styleAffinity(style: TrackStyle): Float {
        if (history.isEmpty()) return 0f
        var score = 0f
        var weightTotal = 0f
        history.forEachIndexed { index, entry ->
            val recencyWeight = (1 shl index.coerceAtMost(20)).toFloat()
            weightTotal += recencyWeight
            if (style in entry.features.styles) {
                score += when {
                    entry.earlySkip -> -recencyWeight
                    entry.completed -> recencyWeight
                    else -> -recencyWeight * 0.5f
                }
            }
        }
        return if (weightTotal > 0f) (score / weightTotal).coerceIn(-1f, 1f) else 0f
    }

    /**
     * True if the artist was skipped early repeatedly in the current session.
     */
    @Synchronized
    fun isArtistFatigued(artist: String): Boolean {
        val normalized = artist.trim().lowercase(Locale.ROOT)
        if (normalized.isBlank() || normalized == "unknown artist") return false
        val recentSkips = history.takeLast(4).count { entry ->
            entry.earlySkip && entry.track.artist.trim().lowercase(Locale.ROOT) == normalized
        }
        return recentSkips >= 2
    }

    @Synchronized
    fun clear() {
        history.clear()
    }
}
