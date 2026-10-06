package com.howdy.echowave.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import com.howdy.echowave.domain.model.Track
import com.howdy.echowave.ui.components.TrackArtwork
import com.howdy.echowave.ui.theme.DisplayHeadline
import com.howdy.echowave.ui.theme.capsLabel

private enum class HomeFilter { FOR_YOU, FAVORITES, HISTORY, ARTISTS }

@Composable
fun HomeScreen(
    recent: List<Track>,
    favorites: List<Track>,
    current: Track?,
    queue: List<Track> = emptyList(),
    queueIndex: Int = 0,
    charts: List<Track> = emptyList(),
    albums: List<com.howdy.echowave.domain.model.Album> = emptyList(),
    moods: List<com.howdy.echowave.domain.model.Genre> = emptyList(),
    displayName: String = "",
    greetingEnabled: Boolean = true,
    personalizedTracks: List<Track> = emptyList(),
    preferenceLoading: Boolean = false,
    onPlay: (List<Track>, Int) -> Unit,
    onOpenPlayer: () -> Unit = {},
    onSeeAllCharts: () -> Unit = {},
    onSeeAllAlbums: () -> Unit = {},
    onSeeAllMoods: () -> Unit = {},
    onPlayAlbum: (com.howdy.echowave.domain.model.Album) -> Unit = {},
    onPlayMood: (com.howdy.echowave.domain.model.Genre) -> Unit = {},
) {
    var filter by remember { mutableStateOf(HomeFilter.FOR_YOU) }
    val mix = remember(recent, favorites) {
        (favorites + recent).distinctBy { it.id }
    }
    val rail: List<Track> = when (filter) {
        HomeFilter.FOR_YOU -> mix
        HomeFilter.FAVORITES -> favorites
        HomeFilter.HISTORY -> recent
        HomeFilter.ARTISTS -> mix
    }
    val artists = remember(mix) { topArtists(mix).take(8) }
    val queuedTracks = remember(current, queue, queueIndex, recent, favorites) {
        val orderedQueue = if (queue.isNotEmpty()) {
            val safeIndex = queueIndex.coerceIn(queue.indices)
            queue.drop(safeIndex) + queue.take(safeIndex)
        } else {
            listOfNotNull(current) + recent + favorites
        }
        (listOfNotNull(current) + orderedQueue).distinctBy { it.id }
    }
    val featureCardWidth = (LocalConfiguration.current.screenWidthDp.dp - 48.dp).coerceAtLeast(260.dp)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    if (greetingEnabled && displayName.isNotBlank()) {
                        Text("HELLO,", style = capsLabel(MaterialTheme.typography.labelMedium),
                            color = MaterialTheme.colorScheme.primary)
                        Text(displayName.trim(), style = DisplayHeadline,
                            color = MaterialTheme.colorScheme.onBackground, maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                        Text("Your music, in full color.", style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        if (greetingEnabled) Text(currentGreeting().uppercase(),
                            style = capsLabel(MaterialTheme.typography.labelMedium),
                            color = MaterialTheme.colorScheme.primary)
                        Text("Your music,\nin full color.", style = DisplayHeadline,
                            color = MaterialTheme.colorScheme.onBackground)
                    }
                }
                Box(
                    Modifier.size(46.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("E", style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(HomeFilter.entries.toList()) { f ->
                    val label = when (f) {
                        HomeFilter.FOR_YOU -> "For You"
                        HomeFilter.FAVORITES -> "Favorites"
                        HomeFilter.HISTORY -> "History"
                        HomeFilter.ARTISTS -> "Artists"
                    }
                    val selected = filter == f
                    Surface(
                        onClick = { filter = f },
                        shape = CircleShape,
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    ) {
                        Text(label, style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 17.dp, vertical = 10.dp))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        if (queuedTracks.isNotEmpty()) {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(queuedTracks, key = { "feature-${it.id}" }) { featuredTrack ->
                        HeroCard(
                            track = featuredTrack,
                            width = featureCardWidth,
                            isCurrent = featuredTrack.id == current?.id,
                            onPlay = {
                                val index = queuedTracks.indexOfFirst { it.id == featuredTrack.id }
                                onPlay(queuedTracks, index.coerceAtLeast(0))
                            },
                            onOpen = onOpenPlayer,
                        )
                    }
                }
                Spacer(Modifier.height(22.dp))
            }
        }
        if (filter == HomeFilter.ARTISTS) {
            item {
                SectionHeader("Artists to watch", onSeeAll = {})
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(artists) { (name, count) ->
                        ArtistCircle(name, count) {
                            val byArtist = mix.filter { it.artist == name }
                            if (byArtist.isNotEmpty()) onPlay(byArtist, 0)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        } else {
            item {
                SectionHeader("Playlists for where you're headed", onSeeAll = {})
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(rail.take(10)) { t ->
                        PlaylistCard(t) {
                            val i = rail.indexOfFirst { it.id == t.id }
                            onPlay(rail, i.coerceAtLeast(0))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        if (charts.isNotEmpty()) {
            item {
                SectionHeader("Charts", onSeeAll = onSeeAllCharts)
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(charts.take(10)) { t ->
                        PlaylistCard(t) {
                            val i = charts.indexOfFirst { it.id == t.id }
                            onPlay(charts, i.coerceAtLeast(0))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        if (personalizedTracks.isNotEmpty() || preferenceLoading) {
            item { SectionHeader("Picked for you${if (displayName.isNotBlank()) " · taste-led" else ""}") }
            item {
                if (preferenceLoading && personalizedTracks.isEmpty()) {
                    Text("Finding your kind of sound…", color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp))
                } else LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(personalizedTracks.take(10)) { t ->
                        PlaylistCard(t) {
                            val i = personalizedTracks.indexOfFirst { it.id == t.id }
                            onPlay(personalizedTracks, i.coerceAtLeast(0))
                        }
                    }
                }
            }
        }
        if (albums.isNotEmpty()) {
            item {
                SectionHeader("New releases", onSeeAll = onSeeAllAlbums)
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(albums.take(10)) { a ->
                        com.howdy.echowave.ui.discover.AlbumCard(a) { onPlayAlbum(a) }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        if (moods.isNotEmpty()) {
            item {
                SectionHeader("Moods & genres", onSeeAll = onSeeAllMoods)
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(moods.take(10)) { m ->
                        com.howdy.echowave.ui.discover.MoodButton(m) { onPlayMood(m) }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, onSeeAll: () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title.uppercase(),
            style = capsLabel(MaterialTheme.typography.labelLarge),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            "See all",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clickable(onClick = onSeeAll),
        )
    }
}

@Composable
private fun HeroCard(
    track: Track,
    width: androidx.compose.ui.unit.Dp,
    isCurrent: Boolean,
    onPlay: () -> Unit,
    onOpen: () -> Unit,
) {
    Box(
        Modifier.width(width).height(260.dp)
            .clip(RoundedCornerShape(28.dp)).clickable(onClick = onOpen),
    ) {
        TrackArtwork(
            url = track.artworkUrl,
            description = "Artwork for ${track.title}",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.04f), Color.Black.copy(alpha = 0.84f)))
        ))
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(18.dp)) {
            Text(
                if (isCurrent) "NOW PLAYING · MADE FOR YOUR MOMENT" else "UP NEXT · IN YOUR QUEUE",
                style = capsLabel(MaterialTheme.typography.labelSmall),
                color = Color.White.copy(alpha = 0.78f))
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(track.title, style = MaterialTheme.typography.headlineSmall,
                        color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.artist, style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.78f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                androidx.compose.material3.FilledIconButton(
                    onClick = onPlay,
                    colors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    modifier = Modifier.size(54.dp),
                ) {
                    androidx.compose.material3.Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play ${track.title}",
                        modifier = Modifier.size(30.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistCard(track: Track, onClick: () -> Unit) {
    Column(Modifier.width(150.dp).clickable(onClick = onClick)) {
        TrackArtwork(
            url = track.artworkUrl,
            description = "Artwork for ${track.title}",
            modifier = Modifier.size(150.dp).clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Crop,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            track.title,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            track.artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ArtistCircle(name: String, count: Int, onClick: () -> Unit) {
    Column(
        Modifier.width(72.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(64.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                name.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            name,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            "$count track${if (count == 1) "" else "s"}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
