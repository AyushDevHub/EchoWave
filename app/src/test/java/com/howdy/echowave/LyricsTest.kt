package com.howdy.echowave.data.remote.lyrics

import com.howdy.echowave.data.remote.lyrics.LrcLine
import com.howdy.echowave.data.remote.lyrics.LyricsRepository
import com.howdy.echowave.data.remote.lyrics.currentLrcIndex
import com.howdy.echowave.data.remote.lyrics.normalizeLyricTiming
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

    @Test fun `word index follows position inside line`() {
        val words = listOf(LrcWord(1000, "Hel "), LrcWord(1500, "lo"))
        assertEquals(-1, currentLrcWordIndex(words, 999))
        assertEquals(0, currentLrcWordIndex(words, 1000))
        assertEquals(1, currentLrcWordIndex(words, 2000))
        assertEquals(-1, currentLrcWordIndex(emptyList(), 5000))
    }

    @Test fun `corrects lyrics whose timestamps were scaled twice`() {
        val lines = listOf(
            LrcLine(10_000_000, "one", listOf(LrcWord(10_500_000, "one"))),
            LrcLine(40_000_000, "two", listOf(LrcWord(40_500_000, "two"))),
        )

        assertEquals(
            listOf(
                LrcLine(10_000, "one", listOf(LrcWord(10_500, "one"))),
                LrcLine(40_000, "two", listOf(LrcWord(40_500, "two"))),
            ),
            normalizeLyricTiming(lines, 200_000L),
        )
    }

    @Test fun `corrects doubled timestamps without track duration and infers line start from words`() {
        val lines = listOf(
            LrcLine(0L, "hello", listOf(LrcWord(12_000_000L, "hello"))),
        )

        assertEquals(
            listOf(LrcLine(12_000L, "hello", listOf(LrcWord(12_000L, "hello")))),
            normalizeLyricTiming(lines, null),
        )
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
                time = 1.0,
                syllabus = listOf(
                    YouLyPlusSyllable("Hello ", 1.0),
                    YouLyPlusSyllable("world", 1.5),
                ),
            ),
            YouLyPlusItem(text = "Plain line", time = 5.0),
        )
        val lrc = with(YouLyPlus) { items.convertToLrc() }!!
        val lines = parseLrc(lrc)
        assertEquals(2, lines.size)
        assertEquals("Hello world", lines[0].text)
        assertEquals(1_000L, lines[0].ms)
        assertEquals("Plain line", lines[1].text)
    }
}
