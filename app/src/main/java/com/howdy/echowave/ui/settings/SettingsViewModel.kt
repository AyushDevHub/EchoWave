package com.howdy.echowave.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.howdy.echowave.data.local.Appearance
import com.howdy.echowave.data.local.SettingsRepository
import com.howdy.echowave.data.local.ThemePreset
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repo: SettingsRepository,
) : ViewModel() {
    val appearance: StateFlow<Appearance> =
        repo.appearance.stateIn(viewModelScope, SharingStarted.Eagerly, Appearance.SYSTEM)
    val themePreset = repo.themePreset.stateIn(viewModelScope, SharingStarted.Eagerly, ThemePreset.PURPLE)
    val displayName = repo.displayName.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val greetingEnabled = repo.greetingEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val musicPreferences = repo.musicPreferences.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    fun setAppearance(value: Appearance) {
        viewModelScope.launch { repo.setAppearance(value) }
    }

    fun setThemePreset(value: ThemePreset) { viewModelScope.launch { repo.setThemePreset(value) } }
    fun setDisplayName(value: String) { viewModelScope.launch { repo.setDisplayName(value) } }
    fun setGreetingEnabled(value: Boolean) { viewModelScope.launch { repo.setGreetingEnabled(value) } }
    fun setMusicPreferences(value: String) { viewModelScope.launch { repo.setMusicPreferences(value) } }

    class Factory(private val repo: SettingsRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(repo) as T
    }
}
