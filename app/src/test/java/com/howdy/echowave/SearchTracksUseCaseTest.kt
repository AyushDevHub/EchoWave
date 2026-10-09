package com.howdy.echowave.domain.usecase

import com.howdy.echowave.core.common.AppResult
import com.howdy.echowave.domain.model.SearchItem
import com.howdy.echowave.domain.model.SearchResults
import com.howdy.echowave.domain.model.Track
import com.howdy.echowave.domain.repository.MusicRepository
import com.howdy.echowave.domain.source.StreamInfo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class StubRepo(
    private val results: SearchResults = SearchResults(
        items = listOf(SearchItem.Song(Track("v1", "T", "A"))),
    ),
) : MusicRepository {
    var lastQuery: String? = null
    override suspend fun search(query: String): AppResult<SearchResults> {
        lastQuery = query
        return AppResult.Ok(results)
    }
    override suspend fun resolveStream(trackId: String): AppResult<StreamInfo> =
        AppResult.Err("stub")
}

class SearchTracksUseCaseTest {
    @Test fun `blank query short-circuits without hitting source`() = runBlocking {
        val repo = StubRepo()
        val r = SearchTracksUseCase(repo)("   ")
        assertTrue(r is AppResult.Ok)
        assertEquals(null, repo.lastQuery)
    }

    @Test fun `trims query before search`() = runBlocking {
        val repo = StubRepo()
        SearchTracksUseCase(repo)("  hello  ")
        assertEquals("hello", repo.lastQuery)
    }

    @Test fun `movie search does not inject unverified album browse rows`() = runBlocking {
        val repo = StubRepo(SearchResults(items = listOf(SearchItem.Album("MPRE1", "3 Idiots", "Various", null))))
        val result = SearchTracksUseCase(repo)("3 idiots") as AppResult.Ok
        assertTrue(result.value.songs.isEmpty())
    }

    @Test fun `direct soundtrack song outranks an unrelated exact title hit`() = runBlocking {
        val repo = StubRepo(SearchResults(items = listOf(
            SearchItem.Song(Track("same-title", "Dhadkan", "Other Singer", "Other Album")),
            SearchItem.Song(Track("dulhe", "Dulhe Ka Sehra", "Nusrat", "Dhadkan (Original Motion Picture Soundtrack)")),
        )))
        val result = SearchTracksUseCase(repo)("Dhadkan") as AppResult.Ok
        assertEquals(listOf("dulhe", "same-title"), result.value.songs.map { it.id })
    }

    @Test fun `movie soundtrack focused result keeps order ahead of same-title noise`() = runBlocking {
        val repo = StubRepo(SearchResults(items = listOf(
            SearchItem.Song(Track("focused", "Dulhe Ka Sehra", "Nusrat", "Dhadkan (Original Motion Picture Soundtrack)")),
            SearchItem.Song(Track("noise", "Dhadkan", "Other Singer", "Other Album")),
            SearchItem.Album("ost", "Dhadkan (Original Motion Picture Soundtrack)", "Nadeem", null),
        )))
        val result = SearchTracksUseCase(repo)("Dhadkan") as AppResult.Ok
        assertEquals("focused", result.value.songs.first().id)
        assertEquals("focused", (result.value.topResult as SearchItem.Song).track.id)
    }

    @Test fun `movie query album match outranks exact unrelated title hit`() = runBlocking {
        val repo = StubRepo(
            SearchResults(
                items = listOf(
                    SearchItem.Song(Track("same-title", "Dhadkan", "Other Singer", "Other Album")),
                    SearchItem.Song(
                        Track("soundtrack", "Dulhe Ka Sehra", "Nusrat", "Dhadkan (Original Motion Picture Soundtrack)"),
                    ),
                ),
            ),
        )
        val r = SearchTracksUseCase(repo)("Dhadkan")
        assertTrue(r is AppResult.Ok)
        assertEquals(listOf("soundtrack", "same-title"), (r as AppResult.Ok).value.songs.map { it.id })
    }

    @Test fun `unrelated album alone does not fabricate song results`() = runBlocking {
        val repo = StubRepo(
            SearchResults(
                items = listOf(SearchItem.Album("MPRE9", "Unrelated", "Various", null)),
            ),
        )
        val r = SearchTracksUseCase(repo)("3 idiot")
        assertTrue(r is AppResult.Ok)
        assertEquals(0, (r as AppResult.Ok).value.songs.size)
    }

    @Test fun `soundtrack songs outrank loose matches`() = runBlocking {
        val repo = StubRepo(
            SearchResults(
                items = listOf(
                    SearchItem.Song(Track("loose", "Random Remix", "DJ X")),
                    SearchItem.Song(Track("s2", "Zoobi Doobi", "Artist", "3 Idiots")),
                ),
            ),
        )
        val r = SearchTracksUseCase(repo)("3 idiots")
        assertTrue(r is AppResult.Ok)
        val ids = (r as AppResult.Ok).value.songs.map { it.id }
        assertEquals(listOf("s2", "loose"), ids)
    }

    @Test fun `matching film song replaces unrelated video top hit`() = runBlocking {
        val repo = StubRepo(
            SearchResults(
                topResult = SearchItem.Video(Track("video", "Movie review", "Channel", isVideo = true)),
                items = listOf(
                    SearchItem.Video(Track("video", "Movie review", "Channel", isVideo = true)),
                    SearchItem.Song(Track("song", "Dulhe Ka Sehra", "Nusrat Fateh Ali Khan", "Dhadkan")),
                ),
            ),
        )
        val r = SearchTracksUseCase(repo)("Dhadkan")
        assertTrue(r is AppResult.Ok)
        val results = (r as AppResult.Ok).value
        assertEquals("song", (results.topResult as SearchItem.Song).track.id)
        assertEquals(listOf("song"), results.songs.map { it.id })
        assertEquals(listOf("video"), results.videos.map { it.id })
    }

    @Test fun `errors pass through untouched`() = runBlocking {
        val repo = object : MusicRepository {
            override suspend fun search(query: String): AppResult<SearchResults> =
                AppResult.Err("down")
            override suspend fun resolveStream(trackId: String) = AppResult.Err("stub")
        }
        val r = SearchTracksUseCase(repo)("q")
        assertTrue(r is AppResult.Err)
    }

    @Test fun `artist query keeps server order`() = runBlocking {
        val repo = StubRepo(
            SearchResults(
                items = listOf(
                    SearchItem.Song(Track("g1", "Gerua", "Arijit Singh", "Dilwale")),
                    SearchItem.Song(Track("j1", "Janam Janam", "Arijit Singh", "Dilwale")),
                ),
            ),
        )
        val r = SearchTracksUseCase(repo)("arijit singh")
        assertTrue(r is AppResult.Ok)
        // No confident title or soundtrack signal — server order untouched.
        assertEquals(listOf("g1", "j1"), (r as AppResult.Ok).value.songs.map { it.id })
    }

    @Test fun `generic query does not expand loose album`() = runBlocking {
        val repo = StubRepo(
            SearchResults(
                items = listOf(
                    SearchItem.Song(Track("l1", "Love", "Artist")),
                    SearchItem.Album("MPRE9", "Love Songs Deluxe", "Various", null),
                ),
            ),
        )
        val r = SearchTracksUseCase(repo)("love")
        assertTrue(r is AppResult.Ok)
        assertEquals(listOf("l1"), (r as AppResult.Ok).value.songs.map { it.id })
    }
}
