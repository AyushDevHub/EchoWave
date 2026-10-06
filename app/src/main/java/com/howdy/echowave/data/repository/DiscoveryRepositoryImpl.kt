package com.howdy.echowave.data.repository

import com.howdy.echowave.core.common.AppResult
import com.howdy.echowave.core.network.EchoWaveError
import com.howdy.echowave.core.network.userMessage
import com.howdy.echowave.data.remote.innertube.INNERTUBE_CLIENT_ID
import com.howdy.echowave.data.remote.innertube.INNERTUBE_CLIENT_VERSION
import com.howdy.echowave.data.remote.innertube.InnerTubeApi
import com.howdy.echowave.data.remote.innertube.VisitorStore
import com.howdy.echowave.data.remote.innertube.browseBody
import com.howdy.echowave.data.remote.innertube.extractVisitorData
import com.howdy.echowave.data.remote.innertube.parseMoodGenres
import com.howdy.echowave.data.remote.innertube.parseSearchResponse
import com.howdy.echowave.data.remote.innertube.parseTwoRowAlbums
import com.howdy.echowave.domain.model.Album
import com.howdy.echowave.domain.model.Genre
import com.howdy.echowave.domain.model.Track
import com.howdy.echowave.domain.repository.DiscoveryRepository

class DiscoveryRepositoryImpl(
    private val api: InnerTubeApi,
    private val visitors: VisitorStore = VisitorStore.inMemory(),
) : DiscoveryRepository {
    override suspend fun charts(limit: Int): AppResult<List<Track>> {
        return try {
            val root = api.browse(
                clientId = INNERTUBE_CLIENT_ID,
                clientVersion = INNERTUBE_CLIENT_VERSION,
                body = browseBody(CHARTS_ID, CHARTS_PARAMS, visitors.current()),
            )
            visitors.offer(extractVisitorData(root))
            AppResult.Ok(parseSearchResponse(root, limit))
        } catch (e: Exception) {
            AppResult.Err(EchoWaveError.Network(e.message ?: "charts failed").userMessage(), e)
        }
    }

    override suspend fun newReleases(limit: Int): AppResult<List<Album>> {
        return try {
            val root = api.browse(
                clientId = INNERTUBE_CLIENT_ID,
                clientVersion = INNERTUBE_CLIENT_VERSION,
                body = browseBody(NEW_RELEASES_ID, null, visitors.current()),
            )
            visitors.offer(extractVisitorData(root))
            AppResult.Ok(parseTwoRowAlbums(root, limit))
        } catch (e: Exception) {
            AppResult.Err(EchoWaveError.Network(e.message ?: "new releases failed").userMessage(), e)
        }
    }

    override suspend fun albumTracks(albumId: String): AppResult<List<Track>> {
        return try {
            val root = api.browse(
                clientId = INNERTUBE_CLIENT_ID,
                clientVersion = INNERTUBE_CLIENT_VERSION,
                body = browseBody(albumId, null, visitors.current()),
            )
            visitors.offer(extractVisitorData(root))
            AppResult.Ok(parseSearchResponse(root, 100))
        } catch (e: Exception) {
            AppResult.Err(EchoWaveError.Network(e.message ?: "album failed").userMessage(), e)
        }
    }

    override suspend fun moods(limit: Int): AppResult<List<Genre>> {
        return try {
            val root = api.browse(
                clientId = INNERTUBE_CLIENT_ID,
                clientVersion = INNERTUBE_CLIENT_VERSION,
                body = browseBody(MOODS_ID, null, visitors.current()),
            )
            visitors.offer(extractVisitorData(root))
            AppResult.Ok(parseMoodGenres(root, limit))
        } catch (e: Exception) {
            AppResult.Err(EchoWaveError.Network(e.message ?: "moods failed").userMessage(), e)
        }
    }

    override suspend fun moodTracks(genre: Genre): AppResult<List<Track>> {
        return try {
            val root = api.browse(
                clientId = INNERTUBE_CLIENT_ID,
                clientVersion = INNERTUBE_CLIENT_VERSION,
                body = browseBody(genre.id, genre.params, visitors.current()),
            )
            visitors.offer(extractVisitorData(root))
            AppResult.Ok(parseSearchResponse(root, 100))
        } catch (e: Exception) {
            AppResult.Err(EchoWaveError.Network(e.message ?: "mood failed").userMessage(), e)
        }
    }

    companion object {
        const val CHARTS_ID = "FEmusic_charts"
        const val CHARTS_PARAMS = "ggMGCgQIgAQ%3D"
        const val NEW_RELEASES_ID = "FEmusic_new_releases_albums"
        const val MOODS_ID = "FEmusic_moods_and_genres"
    }
}
