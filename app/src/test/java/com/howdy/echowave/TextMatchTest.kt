package com.howdy.echowave.core.common

import com.howdy.echowave.domain.usecase.queryMatchesTitle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextMatchTest {
    @Test fun `exact match scores highest`() {
        val exact = TextMatch.matchScore("Haule Haule", "Sukhbir", "haule haule", "")
        val loose = TextMatch.matchScore("Haule Haule Remix", "DJ X", "haule haule", "")
        assertTrue(exact > loose)
    }

    @Test fun `unexpected remix penalized below studio`() {
        val studio = TextMatch.matchScore("Gerua", "Arijit Singh", "gerua", "")
        val remix = TextMatch.matchScore("Gerua Remix", "DJ X", "gerua", "")
        assertTrue(studio > remix)
    }

    @Test fun `wrong artist penalized`() {
        val right = TextMatch.matchScore("Song", "Sukhbir", "song", "sukhbir")
        val wrong = TextMatch.matchScore("Song", "Someone Else", "song", "sukhbir")
        assertTrue(right > wrong)
    }

    @Test fun `unicode queries normalize`() {
        assertEquals(
            TextMatch.normalize("অঞ্জন দত্ত"),
            TextMatch.normalize("অঞ্জন  দত্ত"),
        )
        assertTrue(TextMatch.similarity("Zoobi Doobi", "zoobi  doobi") >= 85)
    }

    @Test fun `blank never matches`() {
        assertEquals(0, TextMatch.similarity("", "song"))
        assertEquals(0, TextMatch.similarity("song", ""))
        assertTrue(!TextMatch.isSafeTitleMatch("", "song"))
    }

    @Test fun `safe title match tolerates official video suffix`() {
        assertTrue(TextMatch.isSafeTitleMatch("Gerua (Official Video)", "Gerua"))
        assertTrue(!TextMatch.isSafeTitleMatch("Gerua", "Janam Janam"))
    }

    @Test fun `query title matching is typo tolerant`() {
        assertTrue(queryMatchesTitle("3 Idiots", "3 idiot"))
        assertTrue(queryMatchesTitle("3 Idiots Original Motion Picture Soundtrack", "3 idiot"))
        assertTrue(queryMatchesTitle("Dilwale Original Soundtrack", "dilwale"))
        assertTrue(queryMatchesTitle("Dilwale", "dilwale"))
        assertTrue(queryMatchesTitle("Dhadkan", "dhadkan"))
        assertTrue(queryMatchesTitle("Rab Ne Bana Di Jodi", "Rab Ne Bana Di Zodi"))
        assertTrue(!queryMatchesTitle("Dilwale", "chennai express"))
        assertTrue(!queryMatchesTitle("", "x"))
    }
}
