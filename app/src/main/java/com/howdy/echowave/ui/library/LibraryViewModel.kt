package com.howdy.echowave.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.howdy.echowave.domain.model.Playlist
import com.howdy.echowave.domain.model.Track
import com.howdy.echowave.domain.repository.LibraryRepository
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

class LibraryViewModel(
    private val repo: LibraryRepository,
) : ViewModel() {
    val favoriteIds: StateFlow<Set<String>> =
        repo.observeFavoriteIds().stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val favorites: StateFlow<List<Track>> =
        repo.observeFavorites().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val playlists: StateFlow<List<Playlist>> =
        repo.observePlaylists().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _history = MutableStateFlow<List<Track>>(emptyList())
    val history: StateFlow<List<Track>> = _history

    fun refresh() {
        viewModelScope.launch { _history.value = repo.history(50) }
    }

    fun toggle(track: Track) {
        viewModelScope.launch { repo.toggleFavorite(track) }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch { runCatching { repo.createPlaylist(name) } }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch { repo.deletePlaylist(id) }
    }

    fun addToPlaylist(playlistId: Long, track: Track) {
        viewModelScope.launch { repo.addToPlaylist(playlistId, track) }
    }

    fun playlistTracks(id: Long): StateFlow<List<Track>> =
        repo.observePlaylistTracks(id)
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun playlistTracksState(id: Long): Flow<PlaylistTracksUiState> =
        repo.observePlaylistTracks(id)
            .map { PlaylistTracksUiState(loading = false, tracks = it) }
            .catch { emit(PlaylistTracksUiState(loading = false, error = it.message ?: "Playlist could not be loaded.")) }

    class Factory(private val repo: LibraryRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LibraryViewModel(repo) as T
    }
}
