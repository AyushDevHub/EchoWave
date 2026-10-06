package com.howdy.echowave.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.howdy.echowave.EchoWaveApp
import com.howdy.echowave.domain.model.Track
import com.howdy.echowave.domain.usecase.SearchTracksUseCase
import com.howdy.echowave.ui.components.TrackArtwork

@Composable
private fun FavoriteToggleMini(favorited: Boolean, onToggle: () -> Unit) {
    IconButton(onClick = onToggle) {
        Icon(
            if (favorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = if (favorited) "Unfavorite" else "Favorite",
        )
    }
}

/** Row with inline play (search player) + favorite, home chip styling for sections. */
@Composable
private fun ResultRow(
    track: Track,
    favorited: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.weight(1f).clickable(onClick = onPlay)
                    .padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TrackArtwork(track.artworkUrl, "Artwork for ${track.title}",
                    Modifier.size(58.dp).clip(RoundedCornerShape(9.dp)))
                Column(Modifier.weight(1f).padding(start = 13.dp)) {
                    Text(track.title, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge)
                    val kind = when {
                        track.isEpisode -> "Episode"
                        track.isVideo -> "Music video"
                        else -> "Song"
                    }
                    Text("$kind · ${track.artist}", maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onPlay, modifier = Modifier.size(42.dp)) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play ${track.title}",
                    tint = androidx.compose.material3.MaterialTheme.colorScheme.primary)
            }
            FavoriteToggleMini(favorited = favorited, onToggle = onToggleFavorite)
        }
        androidx.compose.material3.HorizontalDivider(
            Modifier.padding(start = 91.dp),
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
        )
    }
}

@Composable
private fun SectionChips(
    selected: SearchSection,
    onSelect: (SearchSection) -> Unit,
    showTop: Boolean,
    showSongs: Boolean,
    showVideos: Boolean,
    showEpisodes: Boolean,
) {
    val options = buildList {
        add(SearchSection.ALL to "All")
        if (showTop) add(SearchSection.TOP to "Top result")
        if (showSongs) add(SearchSection.SONGS to "Songs")
        if (showVideos) add(SearchSection.VIDEOS to "Videos")
        if (showEpisodes) add(SearchSection.EPISODES to "Episodes")
    }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(options, key = { it.first.name }) { (section, label) ->
            FilterChip(
                selected = selected == section,
                onClick = { onSelect(section) },
                label = { Text(label) },
            )
        }
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
    browseTracks: List<Track> = emptyList(),
    vm: SearchViewModel = viewModel(
        factory = SearchViewModel.Factory(
            SearchTracksUseCase((LocalContext.current.applicationContext as EchoWaveApp).container.musicRepo),
            (LocalContext.current.applicationContext as EchoWaveApp).container.historyRepo,
        ),
    ),
) {
    val ui by vm.ui.collectAsState()
    var searchFocused by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Search", Modifier.weight(1f),
                style = androidx.compose.material3.MaterialTheme.typography.headlineLarge,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground)
            Box(
                Modifier.size(42.dp).clip(CircleShape)
                    .background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text("E", color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            }
        }
        TextField(
            value = ui.query,
            onValueChange = vm::onQuery,
            placeholder = { Text("Artists, songs, lyrics and more") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = CircleShape,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp)
                .onFocusChanged { searchFocused = it.isFocused },
        )
        when {
            ui.loading -> CircularProgressIndicator(Modifier.padding(16.dp))
            ui.error != null -> Column(Modifier.padding(16.dp)) {
                Text("Couldn't load results. ${ui.error}")
                Button(onClick = { vm.onQuery(ui.query) }) { Text("Retry") }
            }
            ui.results.isEmpty() && ui.query.isBlank() -> {
                if (searchFocused && ui.recent.isNotEmpty()) {
                    LazyColumn(contentPadding = PaddingValues(bottom = 18.dp)) {
                        item {
                            Row(
                                Modifier.fillMaxWidth().padding(start = 20.dp, end = 18.dp, top = 18.dp, bottom = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Recently searched", Modifier.weight(1f),
                                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                                androidx.compose.material3.TextButton(onClick = vm::clearRecent) {
                                    Text("Clear", color = Color(0xFFFF5673))
                                }
                            }
                            androidx.compose.material3.HorizontalDivider(
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            )
                        }
                        items(ui.recent, key = { "recent-$it" }) { q ->
                            Row(
                                Modifier.fillMaxWidth().clickable { vm.onQuery(q); searchFocused = false }
                                    .padding(start = 20.dp, end = 18.dp, top = 11.dp, bottom = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                coil.compose.AsyncImage(
                                    model = browseTracks.getOrNull(ui.recent.indexOf(q))?.artworkUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop,
                                )
                                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                    Text(q, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                    Text("Search", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                                }
                                Icon(Icons.Default.History, contentDescription = "Search again",
                                    tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            androidx.compose.material3.HorizontalDivider(
                                Modifier.padding(start = 84.dp),
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                            )
                        }
                    }
                } else {
                    val categories = browseCategories.chunked(2)
                    LazyColumn(contentPadding = PaddingValues(bottom = 18.dp)) {
                        item {
                            Text("Browse categories",
                                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 10.dp))
                        }
                        items(categories.size) { rowIndex ->
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                categories[rowIndex].forEach { category ->
                                    BrowseCategoryCard(
                                        category = category,
                                        onClick = { vm.onQuery(category.query) },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                if (categories[rowIndex].size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            ui.results.isEmpty() && ui.query.isNotBlank() -> Text(
                "No results",
                modifier = Modifier.padding(16.dp),
            )
            else -> LazyColumn {
                item(key = "section-chips") {
                    SectionChips(
                        selected = ui.section,
                        onSelect = vm::setSection,
                        showTop = ui.topResult != null,
                        showSongs = ui.songs.isNotEmpty(),
                        showVideos = ui.videos.isNotEmpty(),
                        showEpisodes = ui.episodes.isNotEmpty(),
                    )
                }
                if (ui.shows(SearchSection.TOP)) {
                    ui.topResult?.let { top ->
                        // Full result set as queue: music never stops after one tap.
                        val at = ui.results.indexOfFirst { it.id == top.id }.takeIf { it >= 0 } ?: 0
                        item(key = "top-${top.id}") {
                            TopResultCard(
                                track = top,
                                favorited = top.id in favoriteIds,
                                onPlay = { onPlay(ui.results, at) },
                                onToggleFavorite = { onToggleFavorite(top) },
                            )
                        }
                    }
                }
                if (ui.shows(SearchSection.SONGS) && ui.songs.isNotEmpty()) {
                    item(key = "songs-header") {
                        SectionLabel("Songs")
                    }
                    items(ui.songs.indices.toList(), key = { "song-${ui.songs[it].id}" }) { i ->
                        val track = ui.songs[i]
                        ResultRow(
                            track = track,
                            favorited = track.id in favoriteIds,
                            onPlay = { onPlay(ui.songs, i) },
                            onToggleFavorite = { onToggleFavorite(track) },
                        )
                    }
                }
                if (ui.shows(SearchSection.VIDEOS) && ui.videos.isNotEmpty()) {
                    item(key = "videos-header") {
                        SectionLabel("Videos")
                    }
                    items(ui.videos.indices.toList(), key = { "video-${ui.videos[it].id}" }) { i ->
                        val track = ui.videos[i]
                        ResultRow(
                            track = track,
                            favorited = track.id in favoriteIds,
                            onPlay = { onPlay(ui.videos, i) },
                            onToggleFavorite = { onToggleFavorite(track) },
                        )
                    }
                }
                if (ui.shows(SearchSection.EPISODES) && ui.episodes.isNotEmpty()) {
                    item(key = "episodes-header") {
                        SectionLabel("Episodes")
                    }
                    items(ui.episodes.indices.toList(), key = { "ep-${ui.episodes[it].id}" }) { i ->
                        val track = ui.episodes[i]
                        ResultRow(
                            track = track,
                            favorited = track.id in favoriteIds,
                            onPlay = { onPlay(ui.episodes, i) },
                            onToggleFavorite = { onToggleFavorite(track) },
                        )
                    }
                }
            }
        }
    }
}

private data class BrowseCategory(
    val title: String,
    val query: String,
    val tint: Color,
)

private val browseCategories = listOf(
    BrowseCategory("New & trending", "new trending songs", Color(0xFF8B48B9)),
    BrowseCategory("Pop", "pop hits", Color(0xFFE34F7D)),
    BrowseCategory("Bollywood", "Bollywood hits", Color(0xFF9B56C9)),
    BrowseCategory("Punjabi", "Punjabi hits", Color(0xFFCC6D3B)),
    BrowseCategory("Chill", "chill music", Color(0xFF328C9B)),
    BrowseCategory("For the drive", "road trip music", Color(0xFF386BB5)),
    BrowseCategory("Charts", "top songs", Color(0xFF829333)),
    BrowseCategory("Focus", "focus instrumental music", Color(0xFF5E59A8)),
)

@Composable
private fun BrowseCategoryCard(
    category: BrowseCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier.height(126.dp).clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick),
    ) {
        Box(Modifier.fillMaxSize().background(
            Brush.linearGradient(listOf(category.tint.copy(alpha = 0.8f),
                MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surface))))
        Box(Modifier.align(Alignment.TopEnd).padding(top = 9.dp, end = 5.dp)
            .size(100.dp).shadow(20.dp, CircleShape).clip(CircleShape)
            .background(category.tint.copy(alpha = 0.38f)).blur(14.dp))
        Box(Modifier.align(Alignment.CenterEnd).padding(end = 18.dp).size(64.dp)
            .rotate(-18f).border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(20.dp)))
        Box(Modifier.align(Alignment.CenterEnd).padding(end = 31.dp).size(38.dp)
            .rotate(12f).border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp)))
        Text(
            category.title,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.align(Alignment.BottomStart).padding(14.dp),
        )
    }
}
