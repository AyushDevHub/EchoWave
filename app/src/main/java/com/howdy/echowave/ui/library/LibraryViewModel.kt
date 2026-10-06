package com.howdy.echowave.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.howdy.echowave.domain.model.Track
import com.howdy.echowave.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val repo: LibraryRepository,
) : ViewModel() {
    val favoriteIds: StateFlow<Set<String>> =
        repo.observeFavoriteIds().stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val favorites: StateFlow<List<Track>> =
        repo.observeFavorites().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _history = MutableStateFlow<List<Track>>(emptyList())
    val history: StateFlow<List<Track>> = _history

    fun refresh() {
        viewModelScope.launch { _history.value = repo.history(50) }
    }

    fun toggle(track: Track) {
        viewModelScope.launch { repo.toggleFavorite(track) }
    }

    class Factory(private val repo: LibraryRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LibraryViewModel(repo) as T
    }
}
