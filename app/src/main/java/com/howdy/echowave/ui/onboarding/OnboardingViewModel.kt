package com.howdy.echowave.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.howdy.echowave.EchoWaveApp
import com.howdy.echowave.core.common.AppResult
import com.howdy.echowave.data.local.SettingsRepository
import com.howdy.echowave.domain.model.SearchItem
import com.howdy.echowave.domain.repository.MusicRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Genre picks offered during onboarding (offline preset chips). */
val ONBOARDING_GENRES = listOf(
    "Pop",
    "Bollywood",
    "Punjabi",
    "Hip-Hop",
    "Rock",
    "Chill",
    "Classical",
    "Dance",
    "Indie",
    "Lo-fi",
)

data class OnboardingUiState(
    val step: Int = 0,
    val name: String = "",
    val genres: Set<String> = emptySet(),
    val artists: List<SearchItem.Artist> = emptyList(),
    val artistQuery: String = "",
    val artistLoading: Boolean = false,
    val artistResults: List<SearchItem.Artist> = emptyList(),
    val artistError: String? = null,
    val saving: Boolean = false,
) {
    /** Step 0 (name) requires a non-blank name; step 1 requires a genre. */
    fun canContinue(): Boolean = when (step) {
        0 -> name.trim().isNotBlank()
        1 -> genres.isNotEmpty()
        else -> true
    }

    /** Taste string feeding the home personalized search + future recommendations. */
    fun tasteQuery(): String =
        (artists.map { it.name } + genres).joinToString(", ")
}

/**
 * First-start onboarding state. Name/genres/artists live in memory until
 * Done — nothing round-trips through DataStore while typing, so the text
 * cursor can never jump (the old Settings fields rewrote every keystroke
 * via trim()+DataStore re-emission).
 */
class OnboardingViewModel(
    private val music: MusicRepository,
    private val settings: SettingsRepository,
    /** Overridden in unit tests (viewModelScope needs the Main dispatcher). */
    private val workScope: kotlinx.coroutines.CoroutineScope? = null,
) : ViewModel() {
    private val _ui = MutableStateFlow(OnboardingUiState())
    val ui: StateFlow<OnboardingUiState> = _ui
    private val jobs get() = workScope ?: viewModelScope
    private var searchJob: Job? = null

    fun prefillName(current: String) {
        if (_ui.value.name.isBlank() && current.isNotBlank()) {
            _ui.value = _ui.value.copy(name = current)
        }
    }

    fun onName(value: String) {
        _ui.value = _ui.value.copy(name = value.take(40))
    }

    fun toggleGenre(genre: String) {
        val genres = _ui.value.genres.toMutableSet()
        if (!genres.add(genre)) genres.remove(genre)
        _ui.value = _ui.value.copy(genres = genres)
    }

    fun next() {
        val s = _ui.value
        if (s.canContinue() && s.step < 2) _ui.value = s.copy(step = s.step + 1)
    }

    fun back() {
        val s = _ui.value
        if (s.step > 0) _ui.value = s.copy(step = s.step - 1)
    }

    fun onArtistQuery(q: String) {
        _ui.value = _ui.value.copy(artistQuery = q, artistError = null)
        searchJob?.cancel()
        if (q.isBlank()) {
            _ui.value = _ui.value.copy(artistResults = emptyList(), artistLoading = false)
            return
        }
        searchJob = jobs.launch {
            delay(350)
            _ui.value = _ui.value.copy(artistLoading = true)
            try {
                when (val r = music.search(q)) {
                    is AppResult.Ok -> _ui.value = _ui.value.copy(
                        artistLoading = false,
                        artistResults = r.value.items.filterIsInstance<SearchItem.Artist>(),
                    )
                    is AppResult.Err -> _ui.value = _ui.value.copy(
                        artistLoading = false, artistError = r.message,
                    )
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _ui.value = _ui.value.copy(artistLoading = false, artistError = e.message)
            }
        }
    }

    fun toggleArtist(artist: SearchItem.Artist) {
        val artists = _ui.value.artists.toMutableList()
        val at = artists.indexOfFirst { it.id == artist.id }
        if (at >= 0) artists.removeAt(at) else artists.add(artist)
        _ui.value = _ui.value.copy(artists = artists)
    }

    fun isSelected(artist: SearchItem.Artist): Boolean =
        _ui.value.artists.any { it.id == artist.id }

    fun complete(editMode: Boolean, onDone: () -> Unit) {
        val s = _ui.value
        val cleanName = s.name.trim().take(40)
        if (cleanName.isBlank() || s.saving) return
        _ui.value = s.copy(saving = true, name = cleanName)
        jobs.launch {
            try {
                settings.setDisplayName(cleanName)
                val taste = s.tasteQuery()
                if (taste.isNotBlank()) settings.setMusicPreferences(taste)
                if (!editMode) settings.setOnboardingCompleted(true)
                onDone()
            } catch (_: Exception) {
                _ui.value = _ui.value.copy(saving = false)
            }
        }
    }

    class Factory(
        private val music: MusicRepository,
        private val settings: SettingsRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            OnboardingViewModel(music, settings) as T
    }

    companion object {
        fun factory(app: EchoWaveApp): Factory =
            Factory(app.container.musicRepo, app.container.settingsRepo)
    }
}
