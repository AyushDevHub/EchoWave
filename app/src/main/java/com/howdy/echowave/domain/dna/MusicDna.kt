package com.howdy.echowave.domain.dna

import kotlinx.serialization.Serializable

@Serializable
data class ScoredItem(
    val key: String,
    val label: String,
    val score: Double,
    val count: Int,
)

@Serializable
data class DnaStats(
    val totalEvents: Int = 0,
    val completes: Int = 0,
    val repeats: Int = 0,
    val earlySkips: Int = 0,
    val lateSkips: Int = 0,
    val favorites: Int = 0,
    val playlistAdds: Int = 0,
)

/**
 * On-device taste snapshot. Evolving, decayed aggregate — never raw history.
 * Genres stay empty until track metadata carries them; v1 ranks by
 * artist / language-heuristic / daypart only (no fabricated moods).
 */
@Serializable
data class MusicDnaProfile(
    val version: Int = 1,
    val updatedAt: Long = 0L,
    val totalEvents: Int = 0,
    val topArtists: List<ScoredItem> = emptyList(),
    /** Reserved for future metadata enrichment; empty in v1. */
    val topGenres: List<ScoredItem> = emptyList(),
    val languages: List<ScoredItem> = emptyList(),
    /** MORNING / AFTERNOON / EVENING / LATE_NIGHT -> normalized share. */
    val dayparts: Map<String, Double> = emptyMap(),
    val stats: DnaStats = DnaStats(),
    val bestListeningTime: String? = null,
    /** Honest pattern label (e.g. "Late-night listener"), not a fake mood. */
    val currentVibe: String? = null,
) {
    companion object {
        fun empty(now: Long = 0L) = MusicDnaProfile(updatedAt = now)
    }
}

fun MusicDnaProfile.isEmpty(): Boolean = totalEvents == 0
