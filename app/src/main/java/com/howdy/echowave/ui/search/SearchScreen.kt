package com.howdy.echowave.ui.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.howdy.echowave.EchoWaveApp
import com.howdy.echowave.domain.model.Track
import com.howdy.echowave.domain.usecase.SearchTracksUseCase
import com.howdy.echowave.ui.components.TrackRow

@Composable
private fun FavoriteToggleMini(favorited: Boolean, onToggle: () -> Unit) {
    IconButton(onClick = onToggle) {
        Icon(
            if (favorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = if (favorited) "Unfavorite" else "Favorite",
        )
    }
}

@Composable
private fun SectionLabel(title: String) {
    Text(
        title.uppercase(),
        style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun TopResultCard(
    track: Track,
    favorited: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    androidx.compose.material3.Card(
        onClick = onPlay,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            coil.compose.AsyncImage(
                model = track.artworkUrl,
                contentDescription = "Artwork for ${track.title}",
                modifier = Modifier.size(96.dp).clip(
                    androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                ),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(com.howdy.echowave.R.drawable.album),
                error = painterResource(com.howdy.echowave.R.drawable.album),
                fallback = painterResource(com.howdy.echowave.R.drawable.album),
            )
            Column(Modifier.weight(1f).padding(start = 16.dp)) {
                Text(
                    "Top result",
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    track.title,
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Text(
                    "${track.artist}${if (track.isVideo) " • Video" else ""}",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
            FavoriteToggleMini(favorited = favorited, onToggle = onToggleFavorite)
        }
    }
}

@Composable
fun SearchScreen(
    onPlay: (List<Track>, Int) -> Unit,
    favoriteIds: Set<String> = emptySet(),
    onToggleFavorite: (Track) -> Unit = {},
    vm: SearchViewModel = viewModel(
        factory = SearchViewModel.Factory(
            SearchTracksUseCase((LocalContext.current.applicationContext as EchoWaveApp).container.musicRepo),
        ),
    ),
) {
    val ui by vm.ui.collectAsState()
    Column(Modifier.fillMaxSize()) {
        // Fixed search bar: outside the lazy list, full width, stable padding.
        TextField(
            value = ui.query,
            onValueChange = vm::onQuery,
            placeholder = { Text("Search songs") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
        when {
            ui.loading -> CircularProgressIndicator()
            ui.error != null -> Column {
                Text("Couldn't load results. ${ui.error}")
                Button(onClick = { vm.onQuery(ui.query) }) { Text("Retry") }
            }
            ui.results.isEmpty() && ui.query.isNotBlank() -> Text("No results")
            else -> LazyColumn {
                ui.topResult?.let { top ->
                    item(key = "top-${top.id}") {
                        TopResultCard(
                            track = top,
                            favorited = top.id in favoriteIds,
                            onPlay = { onPlay(listOf(top), 0) },
                            onToggleFavorite = { onToggleFavorite(top) },
                        )
                    }
                }
                if (ui.songs.isNotEmpty()) {
                    item(key = "songs-header") {
                        SectionLabel("Songs")
                    }
                    items(ui.songs.indices.toList(), key = { "song-${ui.songs[it].id}" }) { i ->
                        val track = ui.songs[i]
                        TrackRow(track, onClick = { onPlay(ui.songs, i) }) {
                            FavoriteToggleMini(favorited = track.id in favoriteIds) { onToggleFavorite(track) }
                        }
                    }
                }
                if (ui.videos.isNotEmpty()) {
                    item(key = "videos-header") {
                        SectionLabel("Videos")
                    }
                    items(ui.videos.indices.toList(), key = { "video-${ui.videos[it].id}" }) { i ->
                        val track = ui.videos[i]
                        TrackRow(track, onClick = { onPlay(ui.videos, i) }) {
                            FavoriteToggleMini(favorited = track.id in favoriteIds) { onToggleFavorite(track) }
                        }
                    }
                }
            }
        }
    }
}
