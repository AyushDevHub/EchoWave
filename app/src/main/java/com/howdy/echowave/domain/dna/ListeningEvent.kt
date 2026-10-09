package com.howdy.echowave.domain.dna

/** Raw playback signal. Stored 12 months, then pruned. Aggregates live until reset. */
enum class ListeningEventType {
    START,
    COMPLETE,
    SKIP_EARLY,
    SKIP_LATE,
    REPEAT,
    FAVORITE,
    PLAYLIST_ADD,
}

/** Where playback originated. Keeps scoring context-aware without provider branching. */
enum class PlayContext {
    UNKNOWN,
    SEARCH,
    LIBRARY,
    QUEUE,
    PLAYLIST,
    ECHO,
}

data class ListeningEvent(
    val trackId: String,
    val title: String,
    val artist: String,
    val type: ListeningEventType,
    val playedAt: Long,
    /** Time spent before skip/complete, for START this is 0. */
    val listenMs: Long = 0L,
    /** listenMs / duration when duration is known, else null. */
    val completionRatio: Float? = null,
    val hourOfDay: Int = 0,
    val context: PlayContext = PlayContext.UNKNOWN,
    val durationMs: Long? = null,
)
