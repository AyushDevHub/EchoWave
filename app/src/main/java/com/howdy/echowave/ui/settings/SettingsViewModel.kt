package com.howdy.echowave.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.howdy.echowave.data.local.Appearance
import com.howdy.echowave.data.local.SettingsRepository
import com.howdy.echowave.data.local.ThemePreset
import com.howdy.echowave.data.local.PlayerStyle
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
    val fontChoice = repo.fontChoice.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        com.howdy.echowave.data.local.FontChoice.SYSTEM,
    )
    val displayName = repo.displayName.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val greetingEnabled = repo.greetingEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val musicPreferences = repo.musicPreferences.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val onboardingCompleted = repo.onboardingCompleted.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val playerStyle = repo.playerStyle.stateIn(viewModelScope, SharingStarted.Eagerly, PlayerStyle.APPLE)

    fun setAppearance(value: Appearance) {
        viewModelScope.launch { runCatching { repo.setAppearance(value) } }
    }

    fun setThemePreset(value: ThemePreset) { viewModelScope.launch { runCatching { repo.setThemePreset(value) } } }
    fun setFontChoice(value: com.howdy.echowave.data.local.FontChoice) {
        viewModelScope.launch { runCatching { repo.setFontChoice(value) } }
    }
    fun setPlayerStyle(value: PlayerStyle) { viewModelScope.launch { runCatching { repo.setPlayerStyle(value) } } }
    fun setDisplayName(value: String) { viewModelScope.launch { runCatching { repo.setDisplayName(value) } } }
    fun setGreetingEnabled(value: Boolean) { viewModelScope.launch { runCatching { repo.setGreetingEnabled(value) } } }
    fun setMusicPreferences(value: String) { viewModelScope.launch { runCatching { repo.setMusicPreferences(value) } } }

    class Factory(private val repo: SettingsRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(repo) as T
    }
}
