package com.howdy.echowave.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.howdy.echowave.core.common.AppResult
import com.howdy.echowave.domain.repository.DiscoveryRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class DiscoverUiState(
    val loadingCharts: Boolean = false,
    val charts: List<com.howdy.echowave.domain.model.Track> = emptyList(),
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
            _ui.value = _ui.value.copy(loadingCharts = true, error = null)
            try {
                val charts = repo.charts()
                _ui.value = _ui.value.copy(
                    loadingCharts = false,
                    charts = (charts as? AppResult.Ok)?.value ?: _ui.value.charts,
                    error = (charts as? AppResult.Err)?.message,
                )
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(
                    loadingCharts = false,
                    error = e.message ?: "Charts could not be loaded.",
                )
            }
        }
    }

    class Factory(private val repo: DiscoveryRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DiscoverViewModel(repo) as T
    }
}
