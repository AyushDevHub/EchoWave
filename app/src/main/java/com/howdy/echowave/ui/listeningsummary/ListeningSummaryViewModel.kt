package com.howdy.echowave.ui.listeningsummary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.howdy.echowave.data.local.SettingsRepository
import com.howdy.echowave.domain.model.ListeningSummary
import com.howdy.echowave.domain.model.ListeningSummaryPeriod
import com.howdy.echowave.domain.repository.DnaRepository
import com.howdy.echowave.domain.repository.ListeningSummaryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ListeningSummaryUiState(
    val period: ListeningSummaryPeriod = ListeningSummaryPeriod.TODAY,
    val loading: Boolean = true,
    val summary: ListeningSummary? = null,
    val error: String? = null,
    val clearing: Boolean = false,
)

class ListeningSummaryViewModel(
    private val summaryRepository: ListeningSummaryRepository,
    private val dnaRepository: DnaRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ListeningSummaryUiState())
    val state: StateFlow<ListeningSummaryUiState> = _state.asStateFlow()
    val dayPartsEnabled = settingsRepository.summaryTimeOfDayEnabled.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        false,
    )
    private var loadJob: Job? = null

    fun selectPeriod(period: ListeningSummaryPeriod) {
        if (_state.value.period == period) return
        _state.value = _state.value.copy(period = period, summary = null, error = null)
        refresh()
    }

    fun setDayPartsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.setSummaryTimeOfDayEnabled(enabled)
                refresh()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.value = _state.value.copy(error = "This summary setting could not be saved.")
            }
        }
    }

    fun refresh() {
        loadJob?.cancel()
        val period = _state.value.period
        loadJob = viewModelScope.launch {
            _state.value = _state.value.copy(
                loading = _state.value.summary == null,
                error = null,
            )
            try {
                val includeDayParts = settingsRepository.summaryTimeOfDayEnabled.first()
                val summary = summaryRepository.load(period, includeDayParts)
                _state.value = _state.value.copy(loading = false, summary = summary)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = "Your listening summary could not be loaded. Try again.",
                )
            }
        }
    }

    fun clearHistory() {
        if (_state.value.clearing) return
        viewModelScope.launch {
            _state.value = _state.value.copy(clearing = true, error = null)
            try {
                dnaRepository.clearAll()
                refresh()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.value = _state.value.copy(error = "Listening data could not be cleared. Try again.")
            } finally {
                _state.value = _state.value.copy(clearing = false)
            }
        }
    }

    class Factory(
        private val summaryRepository: ListeningSummaryRepository,
        private val dnaRepository: DnaRepository,
        private val settingsRepository: SettingsRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ListeningSummaryViewModel(summaryRepository, dnaRepository, settingsRepository) as T
    }
}
