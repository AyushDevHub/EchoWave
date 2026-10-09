package com.howdy.echowave.playback

import com.howdy.echowave.core.common.AppResult
import com.howdy.echowave.domain.model.SearchResults
import com.howdy.echowave.domain.model.Track
import com.howdy.echowave.domain.recommendation.CandidatePipeline
import com.howdy.echowave.domain.recommendation.CandidateSource
import com.howdy.echowave.domain.recommendation.TrackCandidate
import com.howdy.echowave.domain.repository.LibraryRepository
import com.howdy.echowave.domain.repository.MusicRepository
import com.howdy.echowave.domain.source.StreamInfo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private class FakeMusic(private val good: Set<String>) : MusicRepository {
    override suspend fun search(query: String) = AppResult.Ok(SearchResults())
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
    override fun observePlaylists(): kotlinx.coroutines.flow.Flow<List<com.howdy.echowave.domain.model.Playlist>> =
        kotlinx.coroutines.flow.flowOf(emptyList())
    override fun observePlaylistTracks(playlistId: Long): kotlinx.coroutines.flow.Flow<List<Track>> =
        kotlinx.coroutines.flow.flowOf(emptyList())
    override suspend fun createPlaylist(name: String) = 0L
    override suspend fun deletePlaylist(id: Long) {}
    override suspend fun addToPlaylist(playlistId: Long, track: Track) {}
    override suspend fun removeFromPlaylist(playlistId: Long, trackId: String) {}
    override suspend fun isInPlaylist(playlistId: Long, trackId: String) = false
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

    @Test fun `queue end appends and starts provider candidates`() = runBlocking {
        val radioTrack = Track("radio", "Next Song", "Another Artist")
        val source = object : CandidateSource {
            override val id = "fake-radio"
            override suspend fun fetch(
                context: com.howdy.echowave.domain.recommendation.CandidateContext,
                limit: Int,
            ) = AppResult.Ok(listOf(TrackCandidate(radioTrack, sourceId = id)))
        }
        val c = Media3PlaybackController(
            FakeMusic(setOf("seed", "radio")),
            FakeLibrary(),
            candidatePipeline = CandidatePipeline(listOf(source)),
        )
        c.play(listOf(Track("seed", "Seed", "Seed Artist")))
        c.requestNextAtQueueEnd()
        assertEquals(listOf("seed", "radio"), c.state.value.queue.map { it.id })
        assertEquals("radio", c.state.value.currentTrack?.id)
        assertEquals(false, c.state.value.isLoadingNext)
    }

    @Test fun `queue end reports empty and failed recommendation outcomes`() = runBlocking {
        val track = Track("seed", "Seed", "Seed Artist")
        val empty = Media3PlaybackController(
            FakeMusic(emptySet()), FakeLibrary(), candidatePipeline = CandidatePipeline(emptyList()),
        )
        empty.play(listOf(track))
        empty.requestNextAtQueueEnd()
        assertEquals("No more songs found.", empty.state.value.nextTracksMessage)

        val failedSource = object : CandidateSource {
            override val id = "offline"
            override suspend fun fetch(
                context: com.howdy.echowave.domain.recommendation.CandidateContext,
                limit: Int,
            ): AppResult<List<TrackCandidate>> = AppResult.Err("offline")
        }
        val failed = Media3PlaybackController(
            FakeMusic(emptySet()), FakeLibrary(), candidatePipeline = CandidatePipeline(listOf(failedSource)),
        )
        failed.play(listOf(track))
        failed.requestNextAtQueueEnd()
        assertEquals("Couldn't find next songs: offline", failed.state.value.nextTracksMessage)
        assertEquals(false, failed.state.value.isLoadingNext)
    }

    @Test fun `manual queue append deduplicates by track identity`() = runBlocking {
        val c = Media3PlaybackController(FakeMusic(emptySet()), FakeLibrary())
        val first = Track("one", "Haule Haule", "Sukhwinder Singh")
        c.play(listOf(first))
        c.appendToQueue(listOf(
            Track("two", "Haule Haule (Official Video)", "Sukhwinder Singh"),
            Track("three", "Haule Haule (Live)", "Sukhwinder Singh"),
        ))
        assertEquals(listOf("one", "three"), c.state.value.queue.map { it.id })
    }

    @Test fun `radio play uses one selected seed instead of the search result list`() = runBlocking {
        val selected = Track("selected", "Beedi Jalaile", "Sunidhi Chauhan")
        val c = Media3PlaybackController(FakeMusic(setOf("selected")), FakeLibrary())

        c.playRadio(selected)

        assertEquals(selected, c.state.value.currentTrack)
        assertEquals(listOf(selected), c.state.value.queue)
        assertEquals(0, c.state.value.queueIndex)
    }

    @Test fun `recommendations receive on device profile favorites and recent history`() = runBlocking {
        val seed = Track("seed", "Beedi Jalaile", "Sunidhi Chauhan")
        val recent = Track("recent", "Recent Song", "Recent Artist")
        var received: com.howdy.echowave.domain.recommendation.CandidateContext? = null
        val source = object : CandidateSource {
            override val id = "capture"
            override suspend fun fetch(
                context: com.howdy.echowave.domain.recommendation.CandidateContext,
                limit: Int,
            ): AppResult<List<TrackCandidate>> {
                received = context
                return AppResult.Ok(emptyList())
            }
        }
        val profile = com.howdy.echowave.domain.dna.MusicDnaProfile(totalEvents = 12)
        val c = Media3PlaybackController(
            FakeMusic(setOf("seed")), FakeLibrary(),
            candidatePipeline = CandidatePipeline(listOf(source)),
            dnaProfileProvider = { profile },
            favoriteTrackIdsProvider = { setOf("favorite") },
            recentPlayedProvider = { listOf(recent) },
        )
        c.play(listOf(seed))
        c.requestNextAtQueueEnd()

        assertEquals(profile, received?.dnaProfile)
        assertEquals(setOf("favorite"), received?.favoriteIds)
        assertEquals(true, com.howdy.echowave.domain.model.TrackIdentity.from(recent) in (received?.played ?: emptySet()))
    }
}
