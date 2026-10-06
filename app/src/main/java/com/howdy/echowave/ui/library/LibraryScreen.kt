package com.howdy.echowave.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.compose.BackHandler
import com.howdy.echowave.EchoWaveApp
import com.howdy.echowave.domain.model.Playlist
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
    val playlists by vm.playlists.collectAsState()
    var showCreate by remember { mutableStateOf(false) }
    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var newName by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { vm.refresh() }
    selectedPlaylist?.let { playlist ->
        PlaylistDetailScreen(playlist, vm, { selectedPlaylist = null }, onPlay)
        return
    }
    Column(Modifier.fillMaxSize()) {
        Text("Library")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Playlists", modifier = Modifier.weight(1f))
            IconButton(onClick = { showCreate = true }) {
                Icon(Icons.Default.Add, contentDescription = "New playlist")
            }
        }
        if (playlists.isEmpty()) Text("No playlists yet — create one above.")
        playlists.forEach { p ->
            Row(
                Modifier.fillMaxWidth().clickable { selectedPlaylist = p }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(p.name)
                    Text("${p.trackCount} tracks")
                }
                IconButton(onClick = { vm.deletePlaylist(p.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete playlist")
                }
            }
        }
        if (showCreate) {
            AlertDialog(
                onDismissRequest = { showCreate = false },
                title = { Text("New playlist") },
                text = {
                    TextField(
                        value = newName,
                        onValueChange = { newName = it },
                        placeholder = { Text("Name") },
                        singleLine = true,
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        vm.createPlaylist(newName)
                        newName = ""
                        showCreate = false
                    }) { Text("Create") }
                },
                dismissButton = {
                    TextButton(onClick = { showCreate = false }) { Text("Cancel") }
                },
            )
        }
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
private fun PlaylistDetailScreen(
    playlist: Playlist,
    vm: LibraryViewModel,
    onBack: () -> Unit,
    onPlay: (List<Track>, Int) -> Unit,
) {
    BackHandler(onBack = onBack)
    var retryKey by remember(playlist.id) { mutableIntStateOf(0) }
    val trackFlow = remember(playlist.id, retryKey) { vm.playlistTracksState(playlist.id) }
    val state by trackFlow.collectAsState(initial = PlaylistTracksUiState())
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to library")
            }
            Column(Modifier.weight(1f)) {
                Text(playlist.name, style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
                Text("${state.tracks.size} tracks", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (state.tracks.isNotEmpty()) Button(onClick = { onPlay(state.tracks, 0) }) { Text("Play all") }
        }
        Spacer(Modifier.height(8.dp))
        when {
            state.loading -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            state.error != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(state.error!!)
                TextButton(onClick = { retryKey++ }) { Text("Retry") }
            }
            state.tracks.isEmpty() -> Text("This playlist is empty. Add tracks from the player.")
            else -> LazyColumn {
                items(state.tracks.indices.toList()) { index ->
                    TrackRow(state.tracks[index], onClick = { onPlay(state.tracks, index) })
                }
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

/** Add-to-playlist sheet used from Now Playing. */
@Composable
fun AddToPlaylistDialog(
    track: Track,
    playlists: List<Playlist>,
    onCreateAndAdd: (String) -> Unit,
    onAdd: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var newName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to playlist") },
        text = {
            Column {
                LazyColumn {
                    items(playlists) { p ->
                        Text(
                            "${p.name} (${p.trackCount})",
                            modifier = Modifier.fillMaxWidth().clickable {
                                onAdd(p.id)
                                onDismiss()
                            }.padding(vertical = 10.dp),
                        )
                    }
                }
                TextField(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = { Text("New playlist name") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreateAndAdd(newName) }) { Text("Create + add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
