package com.howdy.echowave.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.howdy.echowave.EchoWaveApp
import com.howdy.echowave.domain.model.Track
import com.howdy.echowave.ui.components.TrackRow

@Composable
fun LibraryScreen(
    onPlay: (List<Track>, Int) -> Unit,
    vm: LibraryViewModel = viewModel(
        factory = LibraryViewModel.Factory(
            (LocalContext.current.applicationContext as EchoWaveApp).container.libraryRepo,
        ),
    ),
) {
    val favorites by vm.favorites.collectAsState()
    val history by vm.history.collectAsState()
    LaunchedEffect(Unit) { vm.refresh() }
    Column(Modifier.fillMaxSize()) {
        Text("Library")
        Text("Favorites (${favorites.size})")
        if (favorites.isEmpty()) Text("Tap the heart on any track to keep it here.")
        LazyColumn(Modifier.weight(1f)) {
            items(favorites.indices.toList()) { i ->
                TrackRow(favorites[i], onClick = { onPlay(favorites, i) }) {
                    IconButton(onClick = { vm.toggle(favorites[i]) }) {
                        Icon(Icons.Default.Favorite, contentDescription = "Unfavorite")
                    }
                }
            }
        }
        Text("History")
        if (history.isEmpty()) Text("Nothing played yet.")
        LazyColumn(Modifier.weight(1f)) {
            items(history.indices.toList()) { i ->
                TrackRow(history[i], onClick = { onPlay(history, i) })
            }
        }
    }
}

@Composable
fun FavoriteToggle(favorited: Boolean, onToggle: () -> Unit) {
    IconButton(onClick = onToggle) {
        Icon(
            if (favorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = if (favorited) "Unfavorite" else "Favorite",
        )
    }
}
