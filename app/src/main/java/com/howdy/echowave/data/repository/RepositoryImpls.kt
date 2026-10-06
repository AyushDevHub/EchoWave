package com.howdy.echowave.data.repository

import com.howdy.echowave.data.local.PlaylistDao
import com.howdy.echowave.data.local.PlaylistEntity
import com.howdy.echowave.data.local.PlaylistTrackEntity
import com.howdy.echowave.data.local.TrackDao
import com.howdy.echowave.data.local.TrackEntity
import com.howdy.echowave.domain.model.Playlist
import com.howdy.echowave.domain.model.Track
import com.howdy.echowave.domain.repository.LibraryRepository
import com.howdy.echowave.domain.repository.MusicRepository
import com.howdy.echowave.domain.source.MusicSource
import com.howdy.echowave.domain.source.StreamResolver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
    private val playlists: PlaylistDao,
) : LibraryRepository {
    override fun observeFavorites(): Flow<List<Track>> =
        dao.favorites().map { it.map(TrackEntity::toDomain) }

    override fun observeFavoriteIds(): Flow<Set<String>> =
        dao.observeFavoriteIds().map { it.toSet() }

    override suspend fun favorites() = observeFavorites().first()

    override suspend fun toggleFavorite(track: Track): Boolean {
        val existing = dao.get(track.id)
        return if (existing == null) {
            dao.insertIgnore(track.toEntity(favorite = true))
            true
        } else {
            val next = !existing.favorite
            dao.setFavorite(track.id, next)
            next
        }
    }
    override suspend fun isFavorite(id: String) = dao.isFavorite(id)
    override suspend fun history(limit: Int) = dao.history(limit).map { it.toDomain() }
    override suspend fun recordPlayed(track: Track) {
        // Preserve the favorite flag: plain upsert would wipe it.
        val existing = dao.get(track.id)
        if (existing == null) {
            dao.insertIgnore(track.toEntity(lastPlayedAt = System.currentTimeMillis()))
        } else {
            dao.touchPlayed(track.id, System.currentTimeMillis())
        }
    }

    override fun observePlaylists(): Flow<List<Playlist>> =
        playlists.observePlaylists().map { list ->
            list.map { Playlist(it.playlist.id, it.playlist.name, it.trackCount) }
        }

    override fun observePlaylistTracks(playlistId: Long): Flow<List<Track>> =
        playlists.observeTracks(playlistId).map { list ->
            list.map {
                Track(it.trackId, it.title, it.artist, it.album, it.artworkUrl, it.durationMs)
            }
        }

    override suspend fun createPlaylist(name: String): Long {
        val clean = name.trim().take(80)
        require(clean.isNotEmpty())
        return playlists.createPlaylist(PlaylistEntity(name = clean, createdAt = System.currentTimeMillis()))
    }

    override suspend fun deletePlaylist(id: Long) {
        playlists.deletePlaylist(id)
    }

    override suspend fun addToPlaylist(playlistId: Long, track: Track) {
        if (playlists.contains(playlistId, track.id) > 0) return
        val pos = playlists.count(playlistId)
        playlists.addTrack(
            PlaylistTrackEntity(
                playlistId = playlistId,
                trackId = track.id,
                position = pos,
                title = track.title,
                artist = track.artist,
                album = track.album,
                artworkUrl = track.artworkUrl,
                durationMs = track.durationMs,
            ),
        )
    }

    override suspend fun removeFromPlaylist(playlistId: Long, trackId: String) {
        playlists.removeTrack(playlistId, trackId)
    }

    override suspend fun isInPlaylist(playlistId: Long, trackId: String): Boolean =
        playlists.contains(playlistId, trackId) > 0
}
