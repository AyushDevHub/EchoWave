package com.howdy.echowave.domain.repository

import com.howdy.echowave.core.common.AppResult
import com.howdy.echowave.domain.model.Track
import com.howdy.echowave.domain.source.StreamInfo
import kotlinx.coroutines.flow.Flow

interface MusicRepository {
    suspend fun search(query: String): AppResult<List<Track>>
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
}
