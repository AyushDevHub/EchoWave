package com.howdy.echowave.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.howdy.echowave.data.remote.lyrics.LrcLine
import com.howdy.echowave.data.remote.lyrics.LyricsRepository
import com.howdy.echowave.domain.model.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class LyricsUiState(
    val trackId: String? = null,
    val loading: Boolean = false,
    val lines: List<LrcLine>? = null,
    val error: String? = null,
)

class LyricsViewModel(
    private val repo: LyricsRepository,
) : ViewModel() {
    private val _ui = MutableStateFlow(LyricsUiState())
    val ui: StateFlow<LyricsUiState> = _ui
    private var loadedFor: String? = null
    private var job: Job? = null
    private var requestGeneration = 0L

    fun load(track: Track?, forceRefresh: Boolean = false) {
        if (track == null) {
            requestGeneration++
            job?.cancel()
            loadedFor = null
            _ui.value = LyricsUiState()
            return
        }
        if (!forceRefresh && track.id == loadedFor) return
        loadedFor = track.id
        val generation = ++requestGeneration
        job?.cancel()
        // A new song must never show the previous song's lyrics while its
        // lookup is running. Keep existing lines only for a same-song refresh.
        val previous = _ui.value.takeIf { forceRefresh && it.trackId == track.id }?.lines
        _ui.value = LyricsUiState(trackId = track.id, loading = true, lines = previous)
        job = viewModelScope.launch {
            try {
                val lines = repo.lyrics(track, forceRefresh)
                if (generation == requestGeneration) {
                    _ui.value = LyricsUiState(
                        trackId = track.id,
                        lines = lines ?: previous,
                        error = if (lines == null) "Lyrics unavailable" else null,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (generation == requestGeneration) {
                    _ui.value = LyricsUiState(
                        trackId = track.id,
                        lines = previous,
                        error = error.message ?: "Lyrics could not be loaded.",
                    )
                }
            }
        }
    }

    class Factory(private val repo: LyricsRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LyricsViewModel(repo) as T
    }
}
