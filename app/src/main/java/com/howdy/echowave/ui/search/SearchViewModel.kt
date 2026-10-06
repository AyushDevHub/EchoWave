package com.howdy.echowave.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.howdy.echowave.core.common.AppResult
import com.howdy.echowave.data.local.SearchHistoryRepository
import com.howdy.echowave.domain.model.Track
import com.howdy.echowave.domain.usecase.SearchTracksUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Section filter chips, home style. Pure state holder. */
enum class SearchSection { ALL, TOP, SONGS, VIDEOS, EPISODES }

data class SearchUiState(
    val query: String = "",
    val loading: Boolean = false,
    val results: List<Track> = emptyList(),
    val section: SearchSection = SearchSection.ALL,
    val recent: List<String> = emptyList(),
    val error: String? = null,
) {
    /**
     * Every result stays present in its section (nothing dropped):
     * top = best hit, songs = all tracks, videos = all videos,
     * episodes = all episodes. Empty sections render nothing.
     */
    val topResult: Track? get() = results.firstOrNull()
    val songs: List<Track> get() = results.filter { !it.isVideo && !it.isEpisode }
    val videos: List<Track> get() = results.filter { it.isVideo }
    val episodes: List<Track> get() = results.filter { it.isEpisode }

    fun shows(s: SearchSection): Boolean = section == SearchSection.ALL || section == s
}

class SearchViewModel(
    private val search: SearchTracksUseCase,
    private val history: SearchHistoryRepository,
) : ViewModel() {
    private val _ui = MutableStateFlow(SearchUiState())
    val ui: StateFlow<SearchUiState> = _ui
    private var job: Job? = null

    init {
        viewModelScope.launch {
            history.recent.collect { recent ->
                _ui.value = _ui.value.copy(recent = recent)
            }
        }
    }

    fun onQuery(q: String) {
        _ui.value = _ui.value.copy(query = q, error = null)
        job?.cancel()
        if (q.isBlank()) {
            _ui.value = _ui.value.copy(results = emptyList(), loading = false)
            return
        }
        job = viewModelScope.launch {
            delay(350)
            _ui.value = _ui.value.copy(loading = true)
            when (val r = search(q)) {
                is AppResult.Ok -> {
                    _ui.value = _ui.value.copy(loading = false, results = r.value)
                    if (r.value.isNotEmpty()) history.save(q)
                }
                is AppResult.Err -> _ui.value = _ui.value.copy(loading = false, error = r.message)
            }
        }
    }

    fun clearRecent() {
        viewModelScope.launch { history.clear() }
    }

    fun setSection(section: SearchSection) {
        _ui.value = _ui.value.copy(section = section)
    }

    class Factory(
        private val useCase: SearchTracksUseCase,
        private val history: SearchHistoryRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SearchViewModel(useCase, history) as T
    }
}
