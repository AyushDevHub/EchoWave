package com.howdy.echowave.domain.repository

import com.howdy.echowave.core.common.AppResult
import com.howdy.echowave.domain.model.Album
import com.howdy.echowave.domain.model.Genre
import com.howdy.echowave.domain.model.Track

/** Anonymous discovery feeds (charts, new releases). No login. */
interface DiscoveryRepository {
    suspend fun charts(limit: Int = 20): AppResult<List<Track>>
    suspend fun newReleases(limit: Int = 20): AppResult<List<Album>>
    suspend fun albumTracks(albumId: String, params: String? = null): AppResult<List<Track>>
    suspend fun moods(limit: Int = 40): AppResult<List<Genre>>
    suspend fun moodTracks(genre: Genre): AppResult<List<Track>>
}
