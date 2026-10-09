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

    fun load(track: Track?, forceRefresh: Boolean = false) {
        if (track == null || (!forceRefresh && track.id == loadedFor)) return
        loadedFor = track.id
        job?.cancel()
        job = viewModelScope.launch {
            // Keep previous lines while refreshing to avoid flicker.
            val previous = _ui.value.lines
            _ui.value = LyricsUiState(loading = true, lines = previous)
            try {
                _ui.value = LyricsUiState(lines = repo.lyrics(track, forceRefresh))
                if (_ui.value.lines == null) {
                    _ui.value = LyricsUiState(lines = previous, error = "Lyrics unavailable")
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _ui.value = LyricsUiState(lines = previous, error = error.message ?: "Lyrics could not be loaded.")
            }
        }
    }

    class Factory(private val repo: LyricsRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LyricsViewModel(repo) as T
    }
}
