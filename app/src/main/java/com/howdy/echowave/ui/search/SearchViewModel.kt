package com.howdy.echowave.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.howdy.echowave.core.common.AppResult
import com.howdy.echowave.domain.model.Track
import com.howdy.echowave.domain.usecase.SearchTracksUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val loading: Boolean = false,
    val results: List<Track> = emptyList(),
    val error: String? = null,
) {
    /** First hit overall; the rest split into songs vs videos. Pure derivation. */
    val topResult: Track? get() = results.firstOrNull()
    val songs: List<Track> get() = results.drop(1).filter { !it.isVideo }
    val videos: List<Track> get() = results.drop(1).filter { it.isVideo }
}

class SearchViewModel(
    private val search: SearchTracksUseCase,
) : ViewModel() {
    private val _ui = MutableStateFlow(SearchUiState())
    val ui: StateFlow<SearchUiState> = _ui
    private var job: Job? = null

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
                is AppResult.Ok -> _ui.value = _ui.value.copy(loading = false, results = r.value)
                is AppResult.Err -> _ui.value = _ui.value.copy(loading = false, error = r.message)
            }
        }
    }

    class Factory(private val useCase: SearchTracksUseCase) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SearchViewModel(useCase) as T
    }
}
