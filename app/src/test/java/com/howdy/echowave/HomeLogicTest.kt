package com.howdy.echowave.ui.home

import com.howdy.echowave.domain.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLogicTest {
    @Test fun `greeting follows hour`() {
        assertEquals("Good morning", greetingForHour(7))
        assertEquals("Good afternoon", greetingForHour(13))
        assertEquals("Good evening", greetingForHour(19))
        assertEquals("Good night", greetingForHour(23))
        assertEquals("Good night", greetingForHour(3))
    }

    @Test fun `top artists ordered by count`() {
        val tracks = listOf(
            Track("1", "s1", "B"),
            Track("2", "s2", "A"),
            Track("3", "s3", "B"),
        )
        assertEquals(listOf("B" to 2, "A" to 1), topArtists(tracks))
        assertEquals(emptyList<Pair<String, Int>>(), topArtists(emptyList()))
    }

    @Test fun `waveform deterministic and bounded`() {
        val a = waveformBars("seed-1")
        val b = waveformBars("seed-1")
        assertEquals(a, b)
        assertEquals(48, a.size)
        assertTrue(a.all { it in 0.15f..1.0f })
    }
}
