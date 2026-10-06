package com.howdy.echowave.data.repository

import com.howdy.echowave.data.local.TrackDao
import com.howdy.echowave.data.local.TrackEntity
import com.howdy.echowave.domain.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
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

class LibraryRepositoryTest {
    @Test fun `toggle on then off`() = runBlocking {
        val repo = LibraryRepositoryImpl(FakeDao())
        assertEquals(true, repo.toggleFavorite(track("v")))
        assertTrue(repo.isFavorite("v"))
        assertEquals(false, repo.toggleFavorite(track("v")))
        assertEquals(false, repo.isFavorite("v"))
    }

    @Test fun `playing a favorite keeps the flag`() = runBlocking {
        val repo = LibraryRepositoryImpl(FakeDao())
        repo.toggleFavorite(track("v"))
        repo.recordPlayed(track("v"))
        assertTrue(repo.isFavorite("v"))
        assertEquals(listOf("v"), repo.history(10).map { it.id })
    }

    @Test fun `history orders most recent first`() = runBlocking {
        val repo = LibraryRepositoryImpl(FakeDao())
        repo.recordPlayed(track("a"))
        Thread.sleep(5)
        repo.recordPlayed(track("b"))
        assertEquals(listOf("b", "a"), repo.history(10).map { it.id })
    }
}
