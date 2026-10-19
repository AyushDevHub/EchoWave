package com.howdy.echowave.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.howdy.echowave.domain.model.Playlist
import com.howdy.echowave.domain.model.Track
import com.howdy.echowave.domain.repository.LibraryRepository
import com.howdy.echowave.domain.repository.LocalAudioRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlaylistTracksUiState(
    val loading: Boolean = true,
    val tracks: List<Track> = emptyList(),
    val error: String? = null,
)

data class LocalAudioUiState(
    val loading: Boolean = true,
    val tracks: List<Track> = emptyList(),
    val importing: Boolean = false,
    val importProgress: String? = null,
    val error: String? = null,
)

class LibraryViewModel(
    private val repo: LibraryRepository,
    private val localAudioRepo: LocalAudioRepository? = null,
) : ViewModel() {
    val favoriteIds: StateFlow<Set<String>> =
        repo.observeFavoriteIds().stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val favorites: StateFlow<List<Track>> =
        repo.observeFavorites().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val playlists: StateFlow<List<Playlist>> =
        repo.observePlaylists().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _history = MutableStateFlow<List<Track>>(emptyList())
    val history: StateFlow<List<Track>> = _history
    private val _localAudio = MutableStateFlow(LocalAudioUiState())
    val localAudio: StateFlow<LocalAudioUiState> = _localAudio

    fun refresh() {
        viewModelScope.launch {
            runCatching { _history.value = repo.history(50) }
        }
    }

    fun refreshLocalAudio() {
        val local = localAudioRepo
        if (local == null) {
            _localAudio.value = LocalAudioUiState(
                loading = false,
                error = "Local audio storage is unavailable.",
            )
            return
        }
        viewModelScope.launch {
            _localAudio.value = _localAudio.value.copy(loading = true, error = null)
            try {
                _localAudio.value = LocalAudioUiState(loading = false, tracks = local.tracks())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _localAudio.value = _localAudio.value.copy(
                    loading = false,
                    error = "Your local music folder could not be read. Try again.",
                )
            }
        }
    }

    fun importLocalAudio(uris: List<String>) {
        val local = localAudioRepo ?: return
        if (uris.isEmpty() || _localAudio.value.importing) return
        viewModelScope.launch {
            _localAudio.value = _localAudio.value.copy(
                importing = true,
                importProgress = "Adding audio files…",
                error = null,
            )
            var failures = 0
            uris.forEachIndexed { index, uri ->
                _localAudio.value = _localAudio.value.copy(
                    importProgress = "Adding ${index + 1} of ${uris.size}…",
                )
                try {
                    local.importFrom(uri)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    failures++
                }
            }
            val tracks = try {
                Result.success(local.tracks())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Result.failure(error)
            }
            _localAudio.value = _localAudio.value.copy(
                loading = false,
                tracks = tracks.getOrDefault(_localAudio.value.tracks),
                importing = false,
                importProgress = null,
                error = when {
                    tracks.isFailure -> "The music was added, but the folder could not be refreshed."
                    failures == uris.size -> "None of the selected files could be added. Choose supported audio files and try again."
                    failures > 0 -> "$failures file(s) could not be added. Other selected files are ready offline."
                    else -> null
                },
            )
        }
    }

    fun deleteLocalAudio(trackId: String) {
        val local = localAudioRepo ?: return
        viewModelScope.launch {
            try {
                local.delete(trackId)
                _localAudio.value = _localAudio.value.copy(
                    tracks = local.tracks(),
                    error = null,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _localAudio.value = _localAudio.value.copy(
                    error = "This audio file could not be removed.",
                )
            }
        }
    }

    fun toggle(track: Track) {
        viewModelScope.launch { runCatching { repo.toggleFavorite(track) } }
    }

    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { runCatching { repo.createPlaylist(name) } }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch { runCatching { repo.deletePlaylist(id) } }
    }

    fun addToPlaylist(playlistId: Long, track: Track) {
        viewModelScope.launch { runCatching { repo.addToPlaylist(playlistId, track) } }
    }

    fun removeFromPlaylist(playlistId: Long, trackId: String) {
        viewModelScope.launch { runCatching { repo.removeFromPlaylist(playlistId, trackId) } }
    }

    fun playlistTracks(id: Long): StateFlow<List<Track>> =
        repo.observePlaylistTracks(id)
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun playlistTracksState(id: Long): Flow<PlaylistTracksUiState> =
        repo.observePlaylistTracks(id)
            .map { PlaylistTracksUiState(loading = false, tracks = it) }
            .catch { emit(PlaylistTracksUiState(loading = false, error = it.message ?: "Playlist could not be loaded.")) }

    class Factory(
        private val repo: LibraryRepository,
        private val localAudioRepo: LocalAudioRepository? = null,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LibraryViewModel(repo, localAudioRepo) as T
    }
}
