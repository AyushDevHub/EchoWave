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
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.viewmodel.compose.viewModel
import com.howdy.echowave.domain.model.Track
import com.howdy.echowave.ui.components.AppLogo
import com.howdy.echowave.ui.components.TrackArtwork
import com.howdy.echowave.ui.update.UpdateStatus
import com.howdy.echowave.ui.update.UpdateViewModel
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
    displayName: String = "",
    greetingEnabled: Boolean = true,
    personalizedTracks: List<Track> = emptyList(),
    preferenceLoading: Boolean = false,
    onPlay: (List<Track>, Int) -> Unit,
    onOpenPlayer: () -> Unit = {},
    onSeeAllCharts: () -> Unit = {},
    updateViewModel: UpdateViewModel = viewModel(),
) {
    val updateStatus by updateViewModel.status.collectAsState()
    val uriHandler = LocalUriHandler.current
    LaunchedEffect(updateViewModel) { updateViewModel.check() }

    var filter by rememberSaveable { mutableStateOf(HomeFilter.FOR_YOU) }
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
        // Bound hero carousel so empty-library fallback can't grow unbounded.
        (listOfNotNull(current) + orderedQueue).distinctBy { it.id }.take(25)
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
                    if (greetingEnabled) {
                        if (displayName.isNotBlank()) {
                            Text("HELLO,", style = capsLabel(MaterialTheme.typography.labelMedium),
                                color = MaterialTheme.colorScheme.primary)
                            Text(displayName.trim(), style = DisplayHeadline,
                                color = MaterialTheme.colorScheme.onBackground, maxLines = 1,
                                overflow = TextOverflow.Ellipsis)
                        } else {
                            Text(currentGreeting().uppercase(),
                                style = capsLabel(MaterialTheme.typography.labelMedium),
                                color = MaterialTheme.colorScheme.primary)
                            Text("Welcome to EchoWave.",
                                style = DisplayHeadline,
                                color = MaterialTheme.colorScheme.onBackground)
                        }
                    } else {
                        Text("EchoWave.",
                            style = DisplayHeadline,
                            color = MaterialTheme.colorScheme.onBackground)
                    }
                }
                ReleaseUpdateChip(
                    status = updateStatus,
                    onRetry = { updateViewModel.check(force = true) },
                    onOpenRelease = { uri -> uriHandler.openUri(uri) },
                )
                AppLogo(size = 46.dp)
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
                    items(queuedTracks, key = { "feature-${it.id}" }, contentType = { "hero" }) { featuredTrack ->
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
                SectionHeader("Artists to watch", onSeeAll = null)
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
                SectionHeader("Playlists for where you're headed", onSeeAll = null)
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
        if (queuedTracks.isEmpty() && rail.isEmpty() && charts.isEmpty() && personalizedTracks.isEmpty() && !preferenceLoading) {
            item {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Your library is empty", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Search for songs to start your mix.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReleaseUpdateChip(
    status: UpdateStatus,
    onRetry: () -> Unit,
    onOpenRelease: (String) -> Unit,
) {
    val isAvailable = status is UpdateStatus.Available
    val isFailed = status is UpdateStatus.Failed
    if (status is UpdateStatus.Current) return

    val label = when (status) {
        UpdateStatus.Checking -> "Checking"
        UpdateStatus.Current -> return
        UpdateStatus.Failed -> "Retry"
        is UpdateStatus.Available -> "Update ${status.release.version}"
    }
    val chipColor = if (isAvailable) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceVariant
    val foreground = if (isAvailable) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick = {
            when (status) {
                UpdateStatus.Failed -> onRetry()
                is UpdateStatus.Available -> onOpenRelease(status.release.releasePageUrl)
                else -> Unit
            }
        },
        enabled = isAvailable || isFailed,
        shape = CircleShape,
        color = chipColor,
        contentColor = foreground,
        border = BorderStroke(1.dp, foreground.copy(alpha = 0.18f)),
        modifier = Modifier.padding(end = 10.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            when (status) {
                UpdateStatus.Checking -> CircularProgressIndicator(
                    modifier = Modifier.size(13.dp),
                    strokeWidth = 1.5.dp,
                    color = foreground,
                )
                UpdateStatus.Current -> Unit
                UpdateStatus.Failed -> androidx.compose.material3.Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                )
                is UpdateStatus.Available -> androidx.compose.material3.Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    onSeeAll: (() -> Unit)? = null,
) {
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
        // Only show affordance when there's somewhere to go.
        if (onSeeAll != null) {
            Text(
                "See all",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onSeeAll),
            )
        }
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
