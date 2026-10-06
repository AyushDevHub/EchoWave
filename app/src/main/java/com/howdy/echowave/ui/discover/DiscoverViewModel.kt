package com.howdy.echowave.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.howdy.echowave.core.common.AppResult
import com.howdy.echowave.domain.model.Album
import com.howdy.echowave.domain.model.Genre
import com.howdy.echowave.domain.model.Track
import com.howdy.echowave.domain.repository.DiscoveryRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class DiscoverUiState(
    val loadingCharts: Boolean = false,
    val loadingAlbums: Boolean = false,
    val loadingMoods: Boolean = false,
    val charts: List<Track> = emptyList(),
    val albums: List<Album> = emptyList(),
    val moods: List<Genre> = emptyList(),
    val error: String? = null,
)

class DiscoverViewModel(
    private val repo: DiscoveryRepository,
) : ViewModel() {
    private val _ui = MutableStateFlow(DiscoverUiState())
    val ui: StateFlow<DiscoverUiState> = _ui
    private var job: Job? = null

    fun refresh() {
        job?.cancel()
        job = viewModelScope.launch {
            _ui.value = _ui.value.copy(
                loadingCharts = true, loadingAlbums = true, loadingMoods = true,
                error = null,
            )
            val charts = repo.charts()
            _ui.value = _ui.value.copy(
                loadingCharts = false,
                charts = (charts as? AppResult.Ok)?.value ?: emptyList(),
                error = (charts as? AppResult.Err)?.message,
            )
            val albums = repo.newReleases()
            _ui.value = _ui.value.copy(
                loadingAlbums = false,
                albums = (albums as? AppResult.Ok)?.value ?: emptyList(),
                error = _ui.value.error ?: (albums as? AppResult.Err)?.message,
            )
            val moods = repo.moods()
            _ui.value = _ui.value.copy(
                loadingMoods = false,
                moods = (moods as? AppResult.Ok)?.value ?: emptyList(),
                error = _ui.value.error ?: (moods as? AppResult.Err)?.message,
            )
        }
    }

    class Factory(private val repo: DiscoveryRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DiscoverViewModel(repo) as T
    }
}
