package com.howdy.echowave

import com.howdy.echowave.domain.model.SearchItem
import com.howdy.echowave.domain.model.SearchResults
import com.howdy.echowave.domain.model.Track
import com.howdy.echowave.domain.usecase.SearchResultRanker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchResultRankerTest {
    @Test fun `matching card shelf song is selected as top playable result`() {
        val exact = SearchItem.Song(Track("exact", "Dulhe Ka Sehra", "Nusrat Fateh Ali Khan"))
        val loose = SearchItem.Song(Track("loose", "Dhadkan", "Unrelated Singer"))
        val ranked = SearchResultRanker.rank(
            SearchResults(topResult = loose, items = listOf(loose, exact)),
            "Dulhe Ka Sehra",
        )
        assertEquals("exact", (ranked.topResult as SearchItem.Song).track.id)
        assertEquals("exact", ranked.songs.first().id)
    }

    @Test fun `card shelf song remains playable when response has no regular rows`() {
        val card = SearchItem.Song(Track("card-id", "Haule Haule", "Sukhwinder Singh"))
        val ranked = SearchResultRanker.rank(SearchResults(topResult = card), "Haule Haule by Sukhwinder Singh")
        assertEquals("card-id", (ranked.topResult as SearchItem.Song).track.id)
        assertEquals(listOf("card-id"), ranked.songs.map { it.id })
    }

    @Test fun `movie album preserves focused soundtrack source order`() {
        val intended = SearchItem.Song(Track("ost", "Dulhe Ka Sehra", "Nusrat"))
        val titleNoise = SearchItem.Song(Track("noise", "Dhadkan", "Other Singer"))
        val album = SearchItem.Album("ost-album", "Dhadkan Original Motion Picture Soundtrack", "Nadeem", null)
        val ranked = SearchResultRanker.rank(
            SearchResults(items = listOf(intended, titleNoise, album)),
            "Dhadkan",
        )
        assertEquals("ost", ranked.songs.first().id)
        assertTrue(ranked.topResult is SearchItem.Song)
        assertEquals("ost", (ranked.topResult as SearchItem.Song).track.id)
    }

    @Test fun `movie soundtrack artwork identifies tracks when album metadata is absent`() {
        val soundtrackCover = "https://img.test/dhadkan.jpg=w544-h544"
        val album = SearchItem.Album(
            "dhadkan-ost", "Dhadkan Original Motion Picture Soundtrack", "Nadeem", soundtrackCover,
        )
        val unrelated = SearchItem.Song(Track("wrong", "Agar Dil Kahe", "Sonu Nigam", artworkUrl = "https://img.test/other.jpg"))
        val original = SearchItem.Song(Track("right", "Dulhe Ka Sehra", "Nusrat", artworkUrl = "https://img.test/dhadkan.jpg=w120-h120"))
        val ranked = SearchResultRanker.rank(SearchResults(items = listOf(unrelated, original, album)), "Dhadkan")
        assertEquals(listOf("right", "wrong"), ranked.songs.map { it.id })
        assertEquals("right", (ranked.topResult as SearchItem.Song).track.id)
    }

    @Test fun `explicit artist in query promotes matching recording`() {
        val wrong = SearchItem.Song(Track("wrong", "Haule Haule", "Other Singer"))
        val right = SearchItem.Song(Track("right", "Haule Haule", "Sukhwinder Singh"))
        val ranked = SearchResultRanker.rank(
            SearchResults(items = listOf(wrong, right)),
            "Haule Haule by Sukhwinder Singh",
        )
        assertEquals("right", (ranked.topResult as SearchItem.Song).track.id)
    }

    @Test fun `unrelated playable card is not promoted as top result`() {
        val unrelated = SearchItem.Song(Track("unrelated", "Agar Dil Kahe", "Sonu Nigam"))
        val ranked = SearchResultRanker.rank(SearchResults(topResult = unrelated, items = listOf(unrelated)), "Dhadkan")
        assertEquals(null, ranked.topResult)
    }
}
