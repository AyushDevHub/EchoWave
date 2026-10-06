package com.howdy.echowave.data.remote.lyrics

import com.howdy.echowave.data.remote.lyrics.LrcLine
import com.howdy.echowave.data.remote.lyrics.LyricsRepository
import com.howdy.echowave.data.remote.lyrics.currentLrcIndex
import com.howdy.echowave.data.remote.lyrics.parseLrc
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LyricsTest {
    @Test fun `parses synced lines sorted`() {
        val lines = parseLrc("[00:12.34]Hello\n[00:10.00]World\n[ar:Artist]")
        assertEquals(2, lines.size)
        assertEquals(LrcLine(10_000, "World"), lines[0])
        assertEquals(LrcLine(12_340, "Hello"), lines[1])
    }

    @Test fun `multi-stamp line expands`() {
        val lines = parseLrc("[00:01.00][00:02.00]Hey")
        assertEquals(listOf(LrcLine(1_000, "Hey"), LrcLine(2_000, "Hey")), lines)
    }

    @Test fun `current index follows position`() {
        val lines = listOf(LrcLine(0, "a"), LrcLine(5_000, "b"), LrcLine(10_000, "c"))
        assertEquals(-1, currentLrcIndex(lines, -1))
        assertEquals(0, currentLrcIndex(lines, 0))
        assertEquals(1, currentLrcIndex(lines, 7_000))
        assertEquals(2, currentLrcIndex(lines, 99_000))
        assertEquals(-1, currentLrcIndex(emptyList(), 5_000))
    }

    @Test fun `plain fallback folds to zero-timed lines`() {
        val repo = LyricsRepository()
        val parsed = repo.parseLrclib("""{"plainLyrics":"one\ntwo","syncedLyrics":null}""")
        assertEquals(listOf(LrcLine(0, "one"), LrcLine(0, "two")), parsed)
    }

    @Test fun `empty body is null`() {
        val repo = LyricsRepository()
        assertNull(repo.parseLrclib("""{"plainLyrics":null,"syncedLyrics":null}"""))
        assertNull(repo.parseLrclib("not json"))
    }

    @Test fun `word-sync inline tags strip to clean lines`() {
        val lines = parseLrc("[00:01.00]<00:01.00>Hel <00:01.50>lo world")
        assertEquals(1, lines.size)
        assertEquals(1_000L, lines[0].ms)
        assertEquals("Hel lo world", lines[0].text)
        assertEquals(2, lines[0].words.size)
    }

    @Test fun `kpoe items convert with syllables`() {
        val items = listOf(
            YouLyPlusItem(
                text = "Hello world",
                time = 1000.0,
                syllabus = listOf(
                    YouLyPlusSyllable("Hello ", 1000.0),
                    YouLyPlusSyllable("world", 1500.0),
                ),
            ),
            YouLyPlusItem(text = "Plain line", time = 5000.0),
        )
        val lrc = with(YouLyPlus) { items.convertToLrc() }!!
        val lines = parseLrc(lrc)
        assertEquals(2, lines.size)
        assertEquals("Hello world", lines[0].text)
        assertEquals(1_000L, lines[0].ms)
        assertEquals("Plain line", lines[1].text)
    }
}
