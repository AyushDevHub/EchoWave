package com.howdy.echowave.domain.source

import com.howdy.echowave.core.common.AppResult
import com.howdy.echowave.domain.model.Track

/** Plugin seam. One implementation per catalog (YTM first). UI never sees this directly. */
interface MusicSource {
    val name: String
    suspend fun search(query: String, limit: Int = 25): AppResult<List<Track>>
    suspend fun getTrack(id: String): AppResult<Track>
}
