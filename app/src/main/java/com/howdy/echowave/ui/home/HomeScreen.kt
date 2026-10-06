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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    onPlay: (List<Track>, Int) -> Unit,
    onOpenPlayer: () -> Unit = {},
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
    val hero = current ?: recent.firstOrNull()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item {
            Text(
                currentGreeting(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
            )
            Text(
                "New sounds live here.",
                style = DisplayHeadline,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            )
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
                    FilterChip(
                        selected = filter == f,
                        onClick = { filter = f },
                        label = { Text(label) },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        if (hero != null) {
            item {
                HeroCard(
                    track = hero,
                    onPlay = {
                        val i = mix.indexOfFirst { it.id == hero.id }.takeIf { it >= 0 } ?: 0
                        onPlay(mix.ifEmpty { listOf(hero) }, i)
                    },
                    onOpen = onOpenPlayer,
                )
                Spacer(Modifier.height(20.dp))
            }
        }
        if (filter == HomeFilter.ARTISTS) {
            item {
                SectionHeader("Artists to watch")
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
                SectionHeader("Playlists for where you're headed")
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
    }
}

@Composable
private fun SectionHeader(title: String) {
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
        )
    }
}

@Composable
private fun HeroCard(track: Track, onPlay: () -> Unit, onOpen: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onOpen)
            .padding(16.dp),
    ) {
        TrackArtwork(
            url = track.artworkUrl,
            description = "Artwork for ${track.title}",
            modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Crop,
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    track.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    track.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            androidx.compose.material3.FilledTonalButton(onClick = onPlay) {
                Text("Play")
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
