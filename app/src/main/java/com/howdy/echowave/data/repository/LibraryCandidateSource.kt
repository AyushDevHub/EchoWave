package com.howdy.echowave.data.repository

import com.howdy.echowave.core.common.AppResult
import com.howdy.echowave.data.local.TrackDao
import com.howdy.echowave.domain.model.Track
import com.howdy.echowave.domain.recommendation.CandidateContext
import com.howdy.echowave.domain.recommendation.CandidateSource
import com.howdy.echowave.domain.recommendation.TrackCandidate
import com.howdy.echowave.domain.recommendation.TrackFeatureExtractor
import kotlinx.coroutines.flow.firstOrNull

/**
 * Local candidate source: surfaces matching favorites from Room DB.
 * Zero network latency and zero bandwidth usage.
 */
class LibraryCandidateSource(
    private val trackDao: TrackDao,
) : CandidateSource {
    override val id = "library-favorites"

    override suspend fun fetch(context: CandidateContext, limit: Int): AppResult<List<TrackCandidate>> {
        return try {
            val favs = trackDao.favorites().firstOrNull() ?: emptyList()
            if (favs.isEmpty()) return AppResult.Ok(emptyList())

            val seedFeatures = TrackFeatureExtractor.extract(context.seed)
            val matching = favs.map { entity ->
                Track(
                    id = entity.id,
                    title = entity.title,
                    artist = entity.artist,
                    album = entity.album,
                    artworkUrl = entity.artworkUrl,
                    durationMs = entity.durationMs,
                )
            }.filter { track ->
                val feat = TrackFeatureExtractor.extract(track)
                (feat.language != null && feat.language == seedFeatures.language) ||
                    feat.styles.intersect(seedFeatures.styles).isNotEmpty() ||
                    (feat.normalizedArtist != null && feat.normalizedArtist == seedFeatures.normalizedArtist)
            }.take(limit)

            AppResult.Ok(matching.map {
                TrackCandidate(
                    track = it,
                    sourceId = id,
                    relevanceScore = 70,
                    tasteScore = 90,
                )
            })
        } catch (e: Exception) {
            AppResult.Err("Library candidate fetch failed: ${e.message}")
        }
    }
}
