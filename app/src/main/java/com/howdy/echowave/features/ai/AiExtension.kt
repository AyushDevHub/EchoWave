package com.howdy.echowave.features.ai

import com.howdy.echowave.domain.model.Track

/**
 * AI extension seam. Core playback/search never depend on AI;
 * AI features depend on core. Adding recommendations, DJ, tagging
 * later must not touch playback/ or domain/source/.
 */
interface RecommendationProvider {
    suspend fun related(seed: Track): List<Track>
}

class NoOpRecommendationProvider : RecommendationProvider {
    override suspend fun related(seed: Track) = emptyList<Track>()
}
