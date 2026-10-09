package com.howdy.echowave.features.dna

import com.howdy.echowave.domain.dna.ListeningEvent
import com.howdy.echowave.domain.dna.ListeningEventType
import com.howdy.echowave.domain.dna.PlayContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private fun event(
    artist: String,
    type: ListeningEventType,
    hoursAgo: Long = 1,
    now: Long = 1_000_000_000_000L,
    title: String = "Song",
    hour: Int = 22,
) = ListeningEvent(
    trackId = "$artist-$title-$type",
    title = title,
    artist = artist,
    type = type,
    playedAt = now - hoursAgo * 3_600_000L,
    listenMs = 180_000L,
    hourOfDay = hour,
    context = PlayContext.QUEUE,
    durationMs = 200_000L,
)

class DnaAggregatorTest {
    private val now = 1_000_000_000_000L

    @Test fun `empty events yield empty profile`() {
        val p = DnaAggregator.aggregate(emptyList(), now)
        assertEquals(0, p.totalEvents)
        assertTrue(p.topArtists.isEmpty())
    }

    @Test fun `repeats outrank completes for same artist`() {
        val events = listOf(
            event("Artist A", ListeningEventType.COMPLETE),
            event("Artist B", ListeningEventType.REPEAT),
            event("Artist B", ListeningEventType.REPEAT),
        )
        val p = DnaAggregator.aggregate(events, now)
        assertEquals("artist b", p.topArtists.first().key)
    }

    @Test fun `early skips penalize artist off the top`() {
        val events = listOf(
            event("Loved", ListeningEventType.COMPLETE),
            event("Loved", ListeningEventType.COMPLETE),
            event("Skipped", ListeningEventType.SKIP_EARLY),
            event("Skipped", ListeningEventType.SKIP_EARLY),
        )
        val p = DnaAggregator.aggregate(events, now)
        assertTrue(p.topArtists.none { it.key == "skipped" })
        assertEquals("loved", p.topArtists.first().key)
    }

    @Test fun `old events decay below recent ones`() {
        val old = DnaAggregator.decayedWeight(ListeningEventType.COMPLETE, 200L * 86_400_000L)
        val fresh = DnaAggregator.decayedWeight(ListeningEventType.COMPLETE, 1L * 3_600_000L)
        assertTrue(fresh > old * 2)
    }

    @Test fun `late night bucket wins best time`() {
        val events = List(6) { event("Night Owl", ListeningEventType.COMPLETE, hour = 23) } +
            listOf(event("Night Owl", ListeningEventType.COMPLETE, hour = 9))
        val p = DnaAggregator.aggregate(events, now)
        assertEquals("Late Night", p.bestListeningTime)
        assertEquals("Late-night listener", p.currentVibe)
    }

    @Test fun `bengali script detected as language`() {
        val events = listOf(
            event("অঞ্জন দত্ত", ListeningEventType.COMPLETE, title = "বৃষ্টি"),
            event("Anjan Dutta", ListeningEventType.COMPLETE, title = "Rain"),
        )
        val p = DnaAggregator.aggregate(events, now)
        assertTrue(p.languages.any { it.key == "Bengali" })
    }

    @Test fun `latin script stays unknown language`() {
        val events = listOf(event("Some Artist", ListeningEventType.COMPLETE, title = "Road Trip Song"))
        val p = DnaAggregator.aggregate(events, now)
        assertTrue(p.languages.isEmpty())
    }

    @Test fun `stats counted`() {
        val events = listOf(
            event("A", ListeningEventType.COMPLETE),
            event("A", ListeningEventType.REPEAT),
            event("B", ListeningEventType.SKIP_EARLY),
            event("C", ListeningEventType.SKIP_LATE),
            event("A", ListeningEventType.FAVORITE),
        )
        val p = DnaAggregator.aggregate(events, now)
        assertEquals(5, p.stats.totalEvents)
        assertEquals(1, p.stats.repeats)
        assertEquals(1, p.stats.earlySkips)
        assertEquals(1, p.stats.lateSkips)
        assertEquals(1, p.stats.favorites)
    }
}
