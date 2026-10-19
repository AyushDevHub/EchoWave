package com.howdy.echowave

import com.howdy.echowave.domain.dna.ListeningEvent
import com.howdy.echowave.domain.dna.ListeningEventType
import com.howdy.echowave.domain.model.ListeningSummaryPeriod
import com.howdy.echowave.features.listening.ListeningSummaryAggregator
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ListeningSummaryAggregatorTest {
    private val utc = ZoneId.of("UTC")
    private val now = Instant.parse("2026-10-19T12:00:00Z").toEpochMilli()

    @Test
    fun `only completed or thirty second outcomes count as plays`() {
        val events = listOf(
            event("a", "Song A", "Artist A", ListeningEventType.START, now, 0),
            event("a", "Song A", "Artist A", ListeningEventType.COMPLETE, now, 150_000),
            event("b", "Song B", "Artist B", ListeningEventType.SKIP_EARLY, now, 12_000),
            event("c", "Song C", "Artist C", ListeningEventType.SKIP_LATE, now, 45_000),
        )

        val summary = ListeningSummaryAggregator.aggregate(
            events, ListeningSummaryPeriod.TODAY, now, utc,
        )

        assertEquals(2, summary.plays)
        assertEquals(1, summary.completedTracks)
        assertEquals(2, summary.uniqueTracks)
        assertEquals(207_000L, summary.listeningMs)
        assertEquals(listOf("Artist A", "Artist C"), summary.topArtists.map { it.label })
    }

    @Test
    fun `calendar periods use local day week and month boundaries`() {
        val sunday = Instant.parse("2026-10-18T23:00:00Z").toEpochMilli()
        val monday = Instant.parse("2026-10-19T01:00:00Z").toEpochMilli()
        val events = listOf(
            event("sun", "Sunday", "Weekend", ListeningEventType.COMPLETE, sunday, 90_000),
            event("mon", "Monday", "Weekday", ListeningEventType.COMPLETE, monday, 90_000),
            event("old", "September", "Old", ListeningEventType.COMPLETE,
                Instant.parse("2026-09-30T12:00:00Z").toEpochMilli(), 90_000),
        )

        val week = ListeningSummaryAggregator.aggregate(events, ListeningSummaryPeriod.THIS_WEEK, now, utc)
        val today = ListeningSummaryAggregator.aggregate(events, ListeningSummaryPeriod.TODAY, now, utc)
        val month = ListeningSummaryAggregator.aggregate(events, ListeningSummaryPeriod.THIS_MONTH, now, utc)

        assertEquals(1, week.plays)
        assertEquals(1, today.plays)
        assertEquals(2, month.plays)
    }

    @Test
    fun `repeats new artists language hints and optional day parts are evidence based`() {
        val bengali = "বৃষ্টি"
        val events = listOf(
            event("same", "Song", "Repeat Artist", ListeningEventType.COMPLETE, now - 1_000, 120_000, hour = 8),
            event("same", "Song", "Repeat Artist", ListeningEventType.REPEAT, now, 60_000, hour = 20),
            event("bn", bengali, "নতুন শিল্পী", ListeningEventType.COMPLETE, now, 100_000, hour = 14),
        )

        val hidden = ListeningSummaryAggregator.aggregate(events, ListeningSummaryPeriod.TODAY, now, utc)
        val shown = ListeningSummaryAggregator.aggregate(
            events, ListeningSummaryPeriod.TODAY, now, utc, includeDayParts = true,
        )

        assertEquals(1, hidden.repeatedTracks.size)
        assertTrue(hidden.newlyExploredArtists.any { it.label == "নতুন শিল্পী" })
        assertTrue(hidden.languageHints.any { it.label == "Bengali" })
        assertTrue(hidden.listeningByDayPart.isEmpty())
        assertEquals(setOf("Morning", "Afternoon", "Evening"), shown.listeningByDayPart.map { it.label }.toSet())
    }

    @Test
    fun `all recorded excludes data older than retention window`() {
        val old = now - ListeningSummaryAggregator.RETAINED_HISTORY_MS - 1
        val summary = ListeningSummaryAggregator.aggregate(
            listOf(
                event("old", "Old Song", "Old Artist", ListeningEventType.COMPLETE, old, 100_000),
                event("new", "Recent Song", "Recent Artist", ListeningEventType.COMPLETE, now, 100_000),
            ),
            ListeningSummaryPeriod.ALL_RECORDED,
            now,
            utc,
        )

        assertEquals(1, summary.plays)
        assertEquals(listOf("Recent Artist"), summary.topArtists.map { it.label })
        assertTrue(summary.hasListeningEvents)
    }

    @Test
    fun `no outcomes report an empty summary`() {
        val summary = ListeningSummaryAggregator.aggregate(
            listOf(event("start", "Started", "Artist", ListeningEventType.START, now, 0)),
            ListeningSummaryPeriod.TODAY,
            now,
            utc,
        )

        assertFalse(summary.hasListeningEvents)
        assertEquals(0, summary.plays)
        assertTrue(summary.topTracks.isEmpty())
    }

    private fun event(
        trackId: String,
        title: String,
        artist: String,
        type: ListeningEventType,
        playedAt: Long,
        listenMs: Long,
        hour: Int = 12,
    ) = ListeningEvent(
        trackId = trackId,
        title = title,
        artist = artist,
        type = type,
        playedAt = playedAt,
        listenMs = listenMs,
        hourOfDay = hour,
        durationMs = 180_000L,
    )
}
