package com.howdy.echowave.domain.usecase

import com.howdy.echowave.core.common.AppResult
import com.howdy.echowave.domain.model.Track
import com.howdy.echowave.domain.repository.MusicRepository
import com.howdy.echowave.domain.source.StreamInfo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class StubRepo : MusicRepository {
    var lastQuery: String? = null
    override suspend fun search(query: String): AppResult<List<Track>> {
        lastQuery = query
        return AppResult.Ok(listOf(Track("v1", "T", "A")))
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
}
