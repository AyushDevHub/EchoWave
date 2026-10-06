package com.howdy.echowave.playback

import com.howdy.echowave.core.common.AppResult
import com.howdy.echowave.domain.model.Track
import com.howdy.echowave.domain.repository.LibraryRepository
import com.howdy.echowave.domain.repository.MusicRepository
import com.howdy.echowave.domain.source.StreamInfo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private class FakeMusic(private val good: Set<String>) : MusicRepository {
    override suspend fun search(query: String) = AppResult.Ok(emptyList<Track>())
    override suspend fun resolveStream(trackId: String): AppResult<StreamInfo> =
        if (trackId in good) AppResult.Ok(StreamInfo(trackId, "http://x"))
        else AppResult.Err("unavailable")
}

private class FakeLibrary : LibraryRepository {
    override fun observeFavorites(): kotlinx.coroutines.flow.Flow<List<Track>> =
        kotlinx.coroutines.flow.flowOf(emptyList())
    override fun observeFavoriteIds(): kotlinx.coroutines.flow.Flow<Set<String>> =
        kotlinx.coroutines.flow.flowOf(emptySet())
    override suspend fun favorites() = emptyList<Track>()
    override suspend fun toggleFavorite(track: Track) = true
    override suspend fun isFavorite(id: String) = false
    override suspend fun history(limit: Int) = emptyList<Track>()
    override suspend fun recordPlayed(track: Track) {}
}

private fun track(id: String) = Track(id, "t$id", "a")

class AdvanceSkipTest {
    @org.junit.Before fun silenceLogs() {
        ctlLog = { _, _, _ -> }
    }

    @Test fun `next skips unplayable to playable`() = runBlocking {
        val c = Media3PlaybackController(FakeMusic(setOf("g")), FakeLibrary())
        // Seed queue via direct tap on first (bad -> error surfaces, no skip).
        c.play(listOf(track("b0"), track("bad"), track("g")))
        assertEquals("b0", c.state.value.currentTrack?.id)
        c.advancePlay(1)
        assertEquals("g", c.state.value.currentTrack?.id)
        assertNull(c.state.value.error)
    }

    @Test fun `all bad leaves last error`() = runBlocking {
        val c = Media3PlaybackController(FakeMusic(emptySet()), FakeLibrary())
        c.play(listOf(track("b0"), track("b1")))
        c.advancePlay(1)
        // Ended on the last failure with its reason visible.
        assertEquals("b1", c.state.value.currentTrack?.id)
        assertEquals("unavailable", c.state.value.error)
    }

    @Test fun `direct tap surfaces error without skipping`() = runBlocking {
        val c = Media3PlaybackController(FakeMusic(setOf("g")), FakeLibrary())
        c.play(listOf(track("bad"), track("g")))
        assertEquals("bad", c.state.value.currentTrack?.id)
        assertEquals("unavailable", c.state.value.error)
    }
}
