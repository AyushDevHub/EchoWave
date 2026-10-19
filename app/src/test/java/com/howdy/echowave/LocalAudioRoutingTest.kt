package com.howdy.echowave

import com.howdy.echowave.core.common.AppResult
import com.howdy.echowave.domain.model.SearchFilter
import com.howdy.echowave.domain.model.SearchResults
import com.howdy.echowave.domain.model.Track
import com.howdy.echowave.domain.repository.LocalAudioRepository
import com.howdy.echowave.domain.source.MusicSource
import com.howdy.echowave.domain.source.StreamInfo
import com.howdy.echowave.domain.source.StreamResolver
import com.howdy.echowave.data.repository.MusicRepositoryImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalAudioRoutingTest {
    @Test
    fun `local track resolves to its app-owned file without calling network resolver`() = runBlocking {
        var networkCalled = false
        val repository = MusicRepositoryImpl(
            source = FakeSource,
            resolver = object : StreamResolver {
                override val name = "test-network"
                override suspend fun resolve(trackId: String): AppResult<StreamInfo> {
                    networkCalled = true
                    return AppResult.Err("unexpected network request")
                }
            },
            localAudio = FakeLocalAudio("file:///app/Music/EchoWave/Downloads/song.mp3"),
        )

        val result = repository.resolveStream("${LocalAudioRepository.TRACK_ID_PREFIX}song.mp3")

        assertTrue(result is AppResult.Ok)
        assertEquals(
            "file:///app/Music/EchoWave/Downloads/song.mp3",
            (result as AppResult.Ok).value.url,
        )
        assertFalse(networkCalled)
    }

    @Test
    fun `missing local file returns an error and never falls back to streaming`() = runBlocking {
        var networkCalled = false
        val repository = MusicRepositoryImpl(
            source = FakeSource,
            resolver = object : StreamResolver {
                override val name = "test-network"
                override suspend fun resolve(trackId: String): AppResult<StreamInfo> {
                    networkCalled = true
                    return AppResult.Err("network")
                }
            },
            localAudio = FakeLocalAudio(null),
        )

        val result = repository.resolveStream("${LocalAudioRepository.TRACK_ID_PREFIX}removed.mp3")

        assertTrue(result is AppResult.Err)
        assertFalse(networkCalled)
    }

    @Test
    fun `remote track keeps existing stream resolution path`() = runBlocking {
        var resolvedId: String? = null
        val repository = MusicRepositoryImpl(
            source = FakeSource,
            resolver = object : StreamResolver {
                override val name = "test-network"
                override suspend fun resolve(trackId: String): AppResult<StreamInfo> {
                    resolvedId = trackId
                    return AppResult.Ok(StreamInfo(trackId, "https://example.test/audio"))
                }
            },
            localAudio = FakeLocalAudio(null),
        )

        val result = repository.resolveStream("remote-track")

        assertEquals("remote-track", resolvedId)
        assertEquals("https://example.test/audio", (result as AppResult.Ok).value.url)
    }

    private class FakeLocalAudio(private val uri: String?) : LocalAudioRepository {
        override suspend fun tracks() = emptyList<Track>()
        override suspend fun importFrom(uri: String) = error("not used")
        override suspend fun delete(trackId: String) = Unit
        override suspend fun fileUri(trackId: String) = uri
    }

    private object FakeSource : MusicSource {
        override val name = "test"
        override suspend fun search(query: String, limit: Int) = AppResult.Ok(SearchResults.empty())
        override suspend fun searchFiltered(query: String, filter: SearchFilter, limit: Int) =
            AppResult.Ok(SearchResults.empty())
        override suspend fun getTrack(id: String) = AppResult.Err("not used")
    }
}
