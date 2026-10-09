package com.howdy.echowave.domain.repository

import com.howdy.echowave.core.common.AppResult
import com.howdy.echowave.domain.model.Playlist
import com.howdy.echowave.domain.model.SearchResults
import com.howdy.echowave.domain.model.Track
import com.howdy.echowave.domain.source.StreamInfo
import kotlinx.coroutines.flow.Flow

interface MusicRepository {
    suspend fun search(query: String): AppResult<SearchResults>
    suspend fun resolveStream(trackId: String): AppResult<StreamInfo>
}

interface LibraryRepository {
    fun observeFavorites(): Flow<List<Track>>
    fun observeFavoriteIds(): Flow<Set<String>>
    suspend fun favorites(): List<Track>
    suspend fun toggleFavorite(track: Track): Boolean
    suspend fun isFavorite(id: String): Boolean
    suspend fun history(limit: Int = 50): List<Track>
    suspend fun recordPlayed(track: Track)
    fun observePlaylists(): Flow<List<Playlist>>
    fun observePlaylistTracks(playlistId: Long): Flow<List<Track>>
    suspend fun createPlaylist(name: String): Long
    suspend fun deletePlaylist(id: Long)
    suspend fun addToPlaylist(playlistId: Long, track: Track)
    suspend fun removeFromPlaylist(playlistId: Long, trackId: String)
    suspend fun isInPlaylist(playlistId: Long, trackId: String): Boolean
}
