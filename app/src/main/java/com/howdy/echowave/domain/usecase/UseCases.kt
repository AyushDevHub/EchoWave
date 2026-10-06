package com.howdy.echowave.domain.usecase

import com.howdy.echowave.core.common.AppResult
import com.howdy.echowave.domain.model.Track
import com.howdy.echowave.domain.repository.MusicRepository
import com.howdy.echowave.domain.source.StreamInfo

class SearchTracksUseCase(
    private val repo: MusicRepository,
) {
    suspend operator fun invoke(query: String): AppResult<List<Track>> {
        if (query.isBlank()) return AppResult.Ok(emptyList())
        return repo.search(query.trim())
    }
}

class ResolveStreamUseCase(
    private val repo: MusicRepository,
) {
    suspend operator fun invoke(trackId: String): AppResult<StreamInfo> =
        repo.resolveStream(trackId)
}
