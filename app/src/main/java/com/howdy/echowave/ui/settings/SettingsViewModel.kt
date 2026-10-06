package com.howdy.echowave.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.howdy.echowave.data.local.Appearance
import com.howdy.echowave.data.local.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repo: SettingsRepository,
) : ViewModel() {
    val appearance: StateFlow<Appearance> =
        repo.appearance.stateIn(viewModelScope, SharingStarted.Eagerly, Appearance.SYSTEM)

    fun setAppearance(value: Appearance) {
        viewModelScope.launch { repo.setAppearance(value) }
    }

    class Factory(private val repo: SettingsRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(repo) as T
    }
}
