package com.howdy.echowave.data.remote.innertube

import com.howdy.echowave.core.common.AppResult
import com.howdy.echowave.core.network.EchoWaveError
import com.howdy.echowave.core.network.userMessage
import com.howdy.echowave.domain.model.Track
import com.howdy.echowave.domain.source.MusicSource

/**
 * Production InnerTube source.
 * Port target for donor decipher logic (Echo-Music `innertube/`, GPL-3.0):
 * ciphered-only player responses are surfaced, not guessed.
 */
class InnerTubeMusicSource(
    private val api: InnerTubeApi,
    private val visitors: VisitorStore = VisitorStore.inMemory(),
) : MusicSource {
    override val name = "innertube-primary"

    override suspend fun search(query: String, limit: Int): AppResult<List<Track>> {
        android.util.Log.d("EchoWaveSearch", "search start q=$query")
        return try {
            val root = api.search(
                clientId = INNERTUBE_CLIENT_ID,
                clientVersion = INNERTUBE_CLIENT_VERSION,
                body = searchBody(query, visitors.current()),
            )
            if (visitors.offer(extractVisitorData(root))) {
                android.util.Log.d("EchoWaveSearch", "visitorData refreshed")
            }
            val tracks = parseSearchResponse(root, limit)
            android.util.Log.d("EchoWaveSearch", "search OK q=$query count=${tracks.size}")
            AppResult.Ok(tracks)
        } catch (e: Exception) {
            AppResult.Err(
                EchoWaveError.Network(e.message ?: "search failed").userMessage(),
                e,
            )
        }
    }

    override suspend fun getTrack(id: String): AppResult<Track> {
        return when (val r = search(id, 10)) {
            is AppResult.Ok -> r.value.firstOrNull { it.id == id }?.let { AppResult.Ok(it) }
                ?: AppResult.Err("track not found")
            is AppResult.Err -> r
        }
    }
}
