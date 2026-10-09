package com.howdy.echowave.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.howdy.echowave.core.common.AppResult
import com.howdy.echowave.data.local.SearchHistoryRepository
import com.howdy.echowave.domain.model.SearchItem
import com.howdy.echowave.domain.model.SearchResults
import com.howdy.echowave.domain.model.Track
import com.howdy.echowave.domain.usecase.SearchTracksUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Section filter chips, home style. Pure state holder. */
enum class SearchSection { ALL, TOP, SONGS, ALBUMS, ARTISTS, PLAYLISTS, VIDEOS, EPISODES }

data class SearchUiState(
    val query: String = "",
    val loading: Boolean = false,
    val results: SearchResults = SearchResults.empty(),
    val section: SearchSection = SearchSection.ALL,
    val recent: List<String> = emptyList(),
    val error: String? = null,
) {
    /**
     * Every result stays present in its section (nothing dropped):
     * top = best hit, then one section per kind in fixed display order
     * (songs → albums → artists → playlists → videos → episodes).
     * Empty sections render nothing.
     */
    val topResult: SearchItem? get() = results.topResult
    val songs: List<Track> get() = results.songs
    val albums: List<SearchItem.Album>
        get() = results.items.filterIsInstance<SearchItem.Album>()
    val artists: List<SearchItem.Artist>
        get() = results.items.filterIsInstance<SearchItem.Artist>()
    val playlists: List<SearchItem.Playlist>
        get() = results.items.filterIsInstance<SearchItem.Playlist>()
    val videos: List<Track> get() = results.videos
    val episodes: List<Track> get() = results.episodes

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
            _ui.value = _ui.value.copy(results = SearchResults.empty(), loading = false)
            return
        }
        job = viewModelScope.launch {
            delay(350)
            _ui.value = _ui.value.copy(loading = true)
            try {
                when (val r = search(q)) {
                    is AppResult.Ok -> {
                        _ui.value = _ui.value.copy(loading = false, results = r.value)
                        if (r.value.items.isNotEmpty()) history.save(q)
                    }
                    is AppResult.Err -> _ui.value = _ui.value.copy(loading = false, error = r.message)
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _ui.value = _ui.value.copy(loading = false, error = e.message ?: "Search failed")
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
