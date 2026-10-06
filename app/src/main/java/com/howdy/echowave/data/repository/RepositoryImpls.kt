package com.howdy.echowave.data.repository

import com.howdy.echowave.data.local.TrackDao
import com.howdy.echowave.data.local.TrackEntity
import com.howdy.echowave.domain.model.Track
import com.howdy.echowave.domain.repository.LibraryRepository
import com.howdy.echowave.domain.repository.MusicRepository
import com.howdy.echowave.domain.source.MusicSource
import com.howdy.echowave.domain.source.StreamResolver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private fun Track.toEntity(favorite: Boolean = false, lastPlayedAt: Long? = null) = TrackEntity(
    id, title, artist, album, artworkUrl, durationMs, favorite, lastPlayedAt,
)
private fun TrackEntity.toDomain() = Track(id, title, artist, album, artworkUrl, durationMs)

class MusicRepositoryImpl(
    private val source: MusicSource,
    private val resolver: StreamResolver,
) : MusicRepository {
    override suspend fun search(query: String) = source.search(query)
    override suspend fun resolveStream(trackId: String) = resolver.resolve(trackId)
}

class LibraryRepositoryImpl(
    private val dao: TrackDao,
) : LibraryRepository {
    fun observeFavorites(): Flow<List<Track>> = dao.favorites().map { it.map(TrackEntity::toDomain) }
    override suspend fun favorites() = emptyList<Track>()
    override suspend fun toggleFavorite(track: Track): Boolean {
        dao.upsert(track.toEntity())
        dao.toggleFavorite(track.id)
        return dao.isFavorite(track.id)
    }
    override suspend fun isFavorite(id: String) = dao.isFavorite(id)
    override suspend fun history(limit: Int) = dao.history(limit).map { it.toDomain() }
    override suspend fun recordPlayed(track: Track) {
        dao.upsert(track.toEntity(lastPlayedAt = System.currentTimeMillis()))
    }
}
