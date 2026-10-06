package com.howdy.echowave.data.repository

import com.howdy.echowave.data.local.TrackDao
import com.howdy.echowave.data.local.TrackEntity
import com.howdy.echowave.domain.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeDao : TrackDao {
    val rows = HashMap<String, TrackEntity>()
    private val favs = MutableStateFlow<List<TrackEntity>>(emptyList())

    private fun emit() {
        favs.value = rows.values.filter { it.favorite }.sortedBy { it.title }
    }

    override fun favorites(): Flow<List<TrackEntity>> = favs
    override fun observeFavoriteIds(): Flow<List<String>> =
        favs.map { list -> list.map { it.id } }

    override suspend fun history(limit: Int) =
        rows.values.filter { it.lastPlayedAt != null }
            .sortedByDescending { it.lastPlayedAt!! }.take(limit)

    override suspend fun get(id: String) = rows[id]
    override suspend fun isFavorite(id: String) = rows[id]?.favorite == true
    override suspend fun insertIgnore(entity: TrackEntity): Long {
        val added = rows.putIfAbsent(entity.id, entity) == null
        if (added) emit()
        return if (added) 1 else -1
    }

    override suspend fun upsert(entity: TrackEntity) {
        rows[entity.id] = entity
        emit()
    }

    override suspend fun setFavorite(id: String, fav: Boolean) {
        rows[id]?.let { rows[id] = it.copy(favorite = fav); emit() }
    }

    override suspend fun touchPlayed(id: String, ts: Long) {
        rows[id]?.let { rows[id] = it.copy(lastPlayedAt = ts) }
    }

    override suspend fun toggleFavorite(id: String) {
        rows[id]?.let { rows[id] = it.copy(favorite = !it.favorite); emit() }
    }
}

private fun track(id: String) = Track(id, "t$id", "a")

private class FakePlaylists : com.howdy.echowave.data.local.PlaylistDao {
    val lists = HashMap<Long, com.howdy.echowave.data.local.PlaylistEntity>()
    val tracks = HashMap<Long, MutableList<com.howdy.echowave.data.local.PlaylistTrackEntity>>()
    var nextId = 1L

    override suspend fun createPlaylist(entity: com.howdy.echowave.data.local.PlaylistEntity): Long {
        val id = nextId++
        lists[id] = entity.copy(id = id)
        return id
    }

    override suspend fun deletePlaylist(id: Long) {
        lists.remove(id)
        tracks.remove(id)
    }

    override suspend fun removeTrack(playlistId: Long, trackId: String) {
        tracks[playlistId]?.removeAll { it.trackId == trackId }
    }

    override suspend fun contains(playlistId: Long, trackId: String) =
        if (tracks[playlistId]?.any { it.trackId == trackId } == true) 1 else 0

    override suspend fun count(playlistId: Long) = tracks[playlistId]?.size ?: 0

    override suspend fun addTrack(entity: com.howdy.echowave.data.local.PlaylistTrackEntity) {
        tracks.getOrPut(entity.playlistId) { mutableListOf() }.add(entity)
    }

    override fun observePlaylists(): kotlinx.coroutines.flow.Flow<List<com.howdy.echowave.data.local.PlaylistWithCount>> =
        kotlinx.coroutines.flow.flowOf(
            lists.values.map {
                com.howdy.echowave.data.local.PlaylistWithCount(it, tracks[it.id]?.size ?: 0)
            },
        )

    override fun observeTracks(playlistId: Long): kotlinx.coroutines.flow.Flow<List<com.howdy.echowave.data.local.PlaylistTrackEntity>> =
        kotlinx.coroutines.flow.flowOf(tracks[playlistId].orEmpty())
}

private fun repoWithFakes(): LibraryRepositoryImpl {
    return LibraryRepositoryImpl(FakeDao(), FakePlaylists())
}

class LibraryRepositoryTest {
    @Test fun `toggle on then off`() = runBlocking {
        val repo = repoWithFakes()
        assertEquals(true, repo.toggleFavorite(track("v")))
        assertTrue(repo.isFavorite("v"))
        assertEquals(false, repo.toggleFavorite(track("v")))
        assertEquals(false, repo.isFavorite("v"))
    }

    @Test fun `playing a favorite keeps the flag`() = runBlocking {
        val repo = repoWithFakes()
        repo.toggleFavorite(track("v"))
        repo.recordPlayed(track("v"))
        assertTrue(repo.isFavorite("v"))
        assertEquals(listOf("v"), repo.history(10).map { it.id })
    }

    @Test fun `history orders most recent first`() = runBlocking {
        val repo = repoWithFakes()
        repo.recordPlayed(track("a"))
        Thread.sleep(5)
        repo.recordPlayed(track("b"))
        assertEquals(listOf("b", "a"), repo.history(10).map { it.id })
    }

    @Test fun `playlists create add delete`() = runBlocking {
        val repo = repoWithFakes()
        val id = repo.createPlaylist("  Road  ")
        assertEquals(1, repo.observePlaylists().first().size)
        repo.addToPlaylist(id, track("a"))
        repo.addToPlaylist(id, track("a"))
        assertEquals(listOf("a"), repo.observePlaylistTracks(id).first().map { it.id })
        assertTrue(repo.isInPlaylist(id, "a"))
        repo.removeFromPlaylist(id, "a")
        assertEquals(false, repo.isInPlaylist(id, "a"))
        repo.deletePlaylist(id)
        assertEquals(0, repo.observePlaylists().first().size)
    }

    @Test fun `blank playlist name rejected`() = runBlocking {
        val repo = repoWithFakes()
        var failed = false
        try {
            repo.createPlaylist("   ")
        } catch (_: IllegalArgumentException) {
            failed = true
        }
        assertTrue(failed)
    }
}
