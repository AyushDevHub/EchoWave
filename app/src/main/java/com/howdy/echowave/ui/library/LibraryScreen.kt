package com.howdy.echowave.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    var section by remember { mutableStateOf(LibrarySection.PLAYLISTS) }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 14.dp, top = 18.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Your Library", style = MaterialTheme.typography.headlineLarge)
                Text("${playlists.size} playlists | ${favorites.size} favorites",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { showCreate = true }) {
                Icon(Icons.Default.Add, contentDescription = "New playlist", tint = MaterialTheme.colorScheme.primary)
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(4.dp),
        ) {
            LibrarySection.entries.forEach { tab ->
                val selected = section == tab
                Surface(
                    onClick = { section = tab },
                    shape = RoundedCornerShape(24.dp),
                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(tab.label, style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(vertical = 12.dp))
                    }
                }
            }
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 20.dp)) {
            when (section) {
                LibrarySection.PLAYLISTS -> {
                    if (playlists.isEmpty()) item {
                        LibraryEmptyState("Start your collection", "Create a playlist and keep your favorite tracks together.")
                    } else items(playlists, key = { "playlist-${it.id}" }) { playlist ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                                .clickable { selectedPlaylist = playlist },
                        ) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(64.dp).clip(RoundedCornerShape(18.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Default.LibraryMusic, contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                                }
                                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                                    Text(playlist.name, style = MaterialTheme.typography.titleMedium)
                                    Text("${playlist.trackCount} tracks", color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodyMedium)
                                }
                                IconButton(onClick = { vm.deletePlaylist(playlist.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete ${playlist.name}",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                LibrarySection.FAVORITES -> {
                    if (favorites.isEmpty()) item {
                        LibraryEmptyState("Songs you love", "Tap the heart on any track to save it here.")
                    } else items(favorites.indices.toList(), key = { "favorite-${favorites[it].id}" }) { i ->
                        TrackRow(favorites[i], onClick = { onPlay(favorites, i) }) {
                            IconButton(onClick = { vm.toggle(favorites[i]) }) {
                                Icon(Icons.Default.Favorite, contentDescription = "Remove favorite",
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        LibraryRowDivider()
                    }
                }
                LibrarySection.HISTORY -> {
                    if (history.isEmpty()) item {
                        LibraryEmptyState("Your listening history", "Tracks you play will appear here.")
                    } else items(history.indices.toList(), key = { "history-${history[it].id}" }) { i ->
                        TrackRow(history[i], onClick = { onPlay(history, i) })
                        LibraryRowDivider()
                    }
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
    }
}

private enum class LibrarySection(val label: String) { PLAYLISTS("Playlists"), FAVORITES("Favorites"), HISTORY("History") }

@Composable
private fun LibraryEmptyState(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(subtitle, modifier = Modifier.padding(top = 6.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LibraryRowDivider() {
    androidx.compose.material3.HorizontalDivider(
        Modifier.padding(start = 78.dp, end = 16.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
    )
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
                items(state.tracks.indices.toList(), key = { "pl-${state.tracks[it].id}-$it" }) { index ->
                    val track = state.tracks[index]
                    TrackRow(
                        track = track,
                        onClick = { onPlay(state.tracks, index) },
                        trailing = {
                            IconButton(onClick = { vm.removeFromPlaylist(playlist.id, track.id) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Remove from playlist",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                    )
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
                if (playlists.isNotEmpty()) {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp)) {
                        items(playlists, key = { it.id }) { p ->
                            Text(
                                "${p.name} (${p.trackCount})",
                                modifier = Modifier.fillMaxWidth().clickable {
                                    onAdd(p.id)
                                    onDismiss()
                                }.padding(vertical = 10.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                TextField(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = { Text("New playlist name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
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
