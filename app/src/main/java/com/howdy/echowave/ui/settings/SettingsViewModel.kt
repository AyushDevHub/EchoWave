package com.howdy.echowave.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import com.howdy.echowave.data.local.Appearance
import com.howdy.echowave.data.local.SettingsRepository
import com.howdy.echowave.data.local.ThemePreset
import com.howdy.echowave.domain.repository.DnaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repo: SettingsRepository,
    private val dnaRepo: DnaRepository? = null,
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
    val dnaEnabled: StateFlow<Boolean> = (dnaRepo?.observeEnabled() ?: flowOf(true))
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    private val _isResettingDna = MutableStateFlow(false)
    val isResettingDna = _isResettingDna.asStateFlow()
    private val _dnaError = MutableStateFlow<String?>(null)
    val dnaError = _dnaError.asStateFlow()

    fun setAppearance(value: Appearance) {
        viewModelScope.launch { runCatching { repo.setAppearance(value) } }
    }

    fun setThemePreset(value: ThemePreset) { viewModelScope.launch { runCatching { repo.setThemePreset(value) } } }
    fun setFontChoice(value: com.howdy.echowave.data.local.FontChoice) {
        viewModelScope.launch { runCatching { repo.setFontChoice(value) } }
    }
    fun setDisplayName(value: String) { viewModelScope.launch { runCatching { repo.setDisplayName(value) } } }
    fun setGreetingEnabled(value: Boolean) { viewModelScope.launch { runCatching { repo.setGreetingEnabled(value) } } }
    fun setMusicPreferences(value: String) { viewModelScope.launch { runCatching { repo.setMusicPreferences(value) } } }

    fun setDnaEnabled(enabled: Boolean) {
        val repository = dnaRepo ?: return
        viewModelScope.launch {
            try {
                repository.setEnabled(enabled)
                _dnaError.value = null
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _dnaError.value = "Music DNA setting could not be saved."
            }
        }
    }

    fun resetDna() {
        val repository = dnaRepo ?: return
        if (_isResettingDna.value) return
        viewModelScope.launch {
            _isResettingDna.value = true
            _dnaError.value = null
            try {
                repository.clearAll()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _dnaError.value = "Music DNA data could not be cleared. Try again."
            } finally {
                _isResettingDna.value = false
            }
        }
    }

    class Factory(
        private val repo: SettingsRepository,
        private val dnaRepo: DnaRepository? = null,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(repo, dnaRepo) as T
    }
}
