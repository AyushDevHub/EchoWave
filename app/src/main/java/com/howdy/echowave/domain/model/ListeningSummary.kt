package com.howdy.echowave.domain.model

enum class ListeningSummaryPeriod(val label: String) {
    TODAY("Today"),
    THIS_WEEK("This week"),
    THIS_MONTH("This month"),
    ALL_RECORDED("All recorded"),
}

data class ListeningSummaryItem(
    val key: String,
    val label: String,
    val count: Int,
)

data class ListeningSummaryDayPart(
    val label: String,
    val listeningMs: Long,
)

data class ListeningSummary(
    val period: ListeningSummaryPeriod,
    val listeningMs: Long = 0L,
    val plays: Int = 0,
    val completedTracks: Int = 0,
    val uniqueTracks: Int = 0,
    val uniqueArtists: Int = 0,
    val topArtists: List<ListeningSummaryItem> = emptyList(),
    val topTracks: List<ListeningSummaryItem> = emptyList(),
    val repeatedTracks: List<ListeningSummaryItem> = emptyList(),
    val languageHints: List<ListeningSummaryItem> = emptyList(),
    val newlyExploredArtists: List<ListeningSummaryItem> = emptyList(),
    val listeningByDayPart: List<ListeningSummaryDayPart> = emptyList(),
    val hasListeningEvents: Boolean = false,
)
