package com.howdy.echowave.domain.repository

import com.howdy.echowave.domain.model.Track

/** App-owned audio files imported by the user for local, offline playback. */
interface LocalAudioRepository {
    suspend fun tracks(): List<Track>
    suspend fun importFrom(uri: String): Track
    suspend fun delete(trackId: String)
    suspend fun fileUri(trackId: String): String?

    companion object {
        const val TRACK_ID_PREFIX = "local-audio:"
    }
}
