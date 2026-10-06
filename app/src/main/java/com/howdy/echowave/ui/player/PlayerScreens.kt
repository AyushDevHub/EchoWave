package com.howdy.echowave.ui.player

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import coil.compose.AsyncImage
import com.howdy.echowave.R
import com.howdy.echowave.core.common.formatDuration
import com.howdy.echowave.domain.model.PlaybackState
import com.howdy.echowave.domain.model.RepeatMode
import com.howdy.echowave.ui.components.TrackArtwork
import com.howdy.echowave.ui.components.TrackRow
import com.howdy.echowave.ui.home.waveformBars
import kotlin.math.sin

@Composable
fun MiniPlayer(
    state: PlaybackState,
    onToggle: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpen: () -> Unit,
) {
    val track = state.currentTrack ?: return
    val progress = if (state.durationMs > 0) {
        (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f)
    } else 0f
    Surface(
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onOpen)
                .padding(start = 10.dp, end = 8.dp, top = 7.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val progressColor = MaterialTheme.colorScheme.primary
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                TrackArtwork(
                    url = track.artworkUrl,
                    description = "Artwork for ${track.title}",
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(9.dp)),
                )
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 2.dp.toPx()
                    drawArc(
                        color = Color.White.copy(alpha = 0.22f), startAngle = -90f,
                        sweepAngle = 360f, useCenter = false,
                        topLeft = Offset(stroke / 2, stroke / 2),
                        size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                        style = Stroke(width = stroke),
                    )
                    drawArc(
                        color = progressColor, startAngle = -90f,
                        sweepAngle = 360f * progress, useCenter = false,
                        topLeft = Offset(stroke / 2, stroke / 2),
                        size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(track.artist, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onToggle, modifier = Modifier.size(44.dp)) {
                Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(26.dp))
            }
            IconButton(onClick = onNext, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next track",
                    tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(28.dp))
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun NowPlayingScreen(
    state: PlaybackState,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onSeek: (Long) -> Unit,
    onShuffle: (Boolean) -> Unit,
    onRepeat: (RepeatMode) -> Unit,
    onRetry: () -> Unit = {},
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    onPlayQueueAt: (Int) -> Unit = {},
    playlists: List<com.howdy.echowave.domain.model.Playlist> = emptyList(),
    onCreateAndAdd: (String) -> Unit = {},
    onAddToPlaylist: (Long) -> Unit = {},
    lyrics: LyricsUiState = LyricsUiState(),
    onRetryLyrics: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    var queueOpen by remember { mutableStateOf(false) }
    var lyricsOpen by remember { mutableStateOf(false) }
    var addOpen by remember { mutableStateOf(false) }
    val track = state.currentTrack
    Box(Modifier.fillMaxSize()) {
        if (track != null) {
            val ambientBlur by animateDpAsState(
                targetValue = if (state.isPlaying) 48.dp else 34.dp,
                animationSpec = tween(700),
                label = "ambientArtworkBlur",
            )
            AsyncImage(
                model = track.artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(ambientBlur),
            )
            Box(Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = if (state.isPlaying) 0.40f else 0.56f),
                        Color(0xFF09090D).copy(alpha = 0.86f),
                    ),
                ),
            ))
        }
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(42.dp)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back to EchoWave")
            }
            Column(Modifier.weight(1f)) {
                Text("NOW PLAYING", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(track?.album ?: "E C H O W A V E", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
            TextButton(onClick = { queueOpen = true }) {
                Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, modifier = Modifier.size(20.dp))
                Text("Queue", modifier = Modifier.padding(start = 6.dp))
            }
        }
        if (track == null) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {
                Text("Nothing playing", style = MaterialTheme.typography.headlineSmall)
                Text("Search for a song and start listening.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            PlayerPage(
                state = state,
                onToggle = onToggle,
                onNext = onNext,
                onPrev = onPrev,
                onSeek = onSeek,
                onShuffle = onShuffle,
                onRepeat = onRepeat,
                onRetry = onRetry,
                isFavorite = isFavorite,
                onToggleFavorite = onToggleFavorite,
                onAddToPlaylist = { addOpen = true },
                lyrics = lyrics,
                onOpenLyrics = { lyricsOpen = true },
                onRetryLyrics = onRetryLyrics,
            )
        }
        }
    }

    if (addOpen && track != null) {
        com.howdy.echowave.ui.library.AddToPlaylistDialog(
            track = track,
            playlists = playlists,
            onCreateAndAdd = { onCreateAndAdd(it); addOpen = false },
            onAdd = { onAddToPlaylist(it); addOpen = false },
            onDismiss = { addOpen = false },
        )
    }

    if (lyricsOpen && track != null) {
        FullscreenLyrics(
            track = track,
            state = lyrics,
            positionMs = state.positionMs,
            onBack = { lyricsOpen = false },
            onRetry = onRetryLyrics,
        )
    }

    if (queueOpen) {
        ModalBottomSheet(
            onDismissRequest = { queueOpen = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                Text("Up next", style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                if (state.queue.isEmpty()) {
                    Text("Your queue is empty.", color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
                } else {
                    LazyColumn {
                        itemsIndexed(state.queue, key = { _, item -> item.id }) { index, item ->
                            TrackRow(
                                track = item,
                                onClick = { onPlayQueueAt(index); queueOpen = false },
                                trailing = if (index == state.queueIndex) ({
                                    Text("PLAYING", style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(start = 8.dp))
                                }) else null,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerPage(
    state: PlaybackState,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onSeek: (Long) -> Unit,
    onShuffle: (Boolean) -> Unit,
    onRepeat: (RepeatMode) -> Unit,
    onRetry: () -> Unit,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit = {},
    lyrics: LyricsUiState,
    onOpenLyrics: () -> Unit,
    onRetryLyrics: () -> Unit,
) {
    val track = state.currentTrack ?: return
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (state.isBuffering) {
            Text("Finding your sound…", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
        }
        state.error?.let { error ->
            Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                Row(Modifier.padding(start = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(error, Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = onRetry) { Text("Retry") }
                }
            }
        }

        var dragDistance by remember(track.id) { mutableStateOf(0f) }
        val coverOffset by animateFloatAsState(
            targetValue = dragDistance,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "coverSwipeReturn",
        )
        AnimatedContent(
            targetState = track,
            transitionSpec = { fadeIn(tween(320)) togetherWith fadeOut(tween(220)) },
            label = "albumCoverTransition",
            modifier = Modifier.fillMaxWidth(0.84f).aspectRatio(1f)
                .shadow(24.dp, RoundedCornerShape(26.dp)).clip(RoundedCornerShape(26.dp))
                .graphicsLayer {
                    translationX = coverOffset.coerceIn(-size.width * 0.12f, size.width * 0.12f)
                    rotationZ = (coverOffset / 35f).coerceIn(-5f, 5f)
                }
                .pointerInput(track.id) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            dragDistance += amount
                        },
                        onDragEnd = {
                            when {
                                dragDistance < -72.dp.toPx() -> onNext()
                                dragDistance > 72.dp.toPx() -> onPrev()
                            }
                            dragDistance = 0f
                        },
                        onDragCancel = { dragDistance = 0f },
                    )
                },
        ) { coverTrack ->
            TrackArtwork(
                url = coverTrack.artworkUrl,
                description = "Album artwork for ${coverTrack.title}. Swipe to change tracks.",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        Text("SWIPE COVER TO CHANGE TRACK", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp, bottom = 2.dp))

        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.headlineSmall)
                Text(track.artist, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onAddToPlaylist) {
                Icon(Icons.Default.PlaylistAdd, contentDescription = "Add to playlist")
            }
        }

        LyricsPreview(
            state = lyrics,
            positionMs = state.positionMs,
            onOpen = onOpenLyrics,
            onRetry = onRetryLyrics,
        )
        PlaybackScrubber(
            positionMs = state.positionMs,
            durationMs = state.durationMs,
            onSeek = onSeek,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDuration(state.positionMs), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatDuration(state.durationMs.takeIf { it > 0 }), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onShuffle(!state.shuffleEnabled) }, modifier = Modifier.size(46.dp)) {
                Icon(Icons.Default.Shuffle, "Shuffle",
                    tint = if (state.shuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconButton(onClick = onPrev, modifier = Modifier.size(50.dp)) {
                    Icon(Icons.Default.SkipPrevious, "Previous track", modifier = Modifier.size(30.dp))
                }
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(70.dp).clickable(onClick = onToggle)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (state.isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(34.dp))
                    }
                }
                IconButton(onClick = onNext, modifier = Modifier.size(50.dp)) {
                    Icon(Icons.Default.SkipNext, "Next track", modifier = Modifier.size(30.dp))
                }
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {
                onRepeat(when (state.repeatMode) {
                    RepeatMode.OFF -> RepeatMode.ALL
                    RepeatMode.ALL -> RepeatMode.ONE
                    RepeatMode.ONE -> RepeatMode.OFF
                })
            }, modifier = Modifier.size(46.dp)) {
                Icon(if (state.repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                    "Repeat", tint = if (state.repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PlaybackScrubber(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
) {
    val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)
    Canvas(
        Modifier.fillMaxWidth().height(30.dp)
            .pointerInput(durationMs) {
                detectTapGestures { tap ->
                    if (durationMs > 0 && size.width > 0) {
                        onSeek((tap.x / size.width * durationMs).toLong().coerceIn(0L, durationMs))
                    }
                }
            }
            .pointerInput(durationMs) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        if (durationMs > 0 && size.width > 0) {
                            onSeek((change.position.x / size.width * durationMs).toLong().coerceIn(0L, durationMs))
                        }
                    },
                )
            }
            .semantics {
                contentDescription = "Playback progress"
                progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
            },
    ) {
        val y = size.height / 2f
        val thumbX = size.width * progress
        val stroke = 3.dp.toPx()
        drawLine(inactiveColor, Offset(0f, y), Offset(size.width, y), strokeWidth = stroke, cap = StrokeCap.Round)
        if (thumbX > 0f) drawLine(activeColor, Offset(0f, y), Offset(thumbX, y), strokeWidth = stroke, cap = StrokeCap.Round)
        drawCircle(activeColor, radius = 6.dp.toPx(), center = Offset(thumbX, y))
    }
}

@Composable
private fun LyricsPreview(
    state: LyricsUiState,
    positionMs: Long,
    onOpen: () -> Unit,
    onRetry: () -> Unit,
) {
    val lines = state.lines
    val activeIndex = lines?.let {
        com.howdy.echowave.data.remote.lyrics.currentLrcIndex(it, positionMs)
    } ?: -1
    val previewIndex = when {
        lines.isNullOrEmpty() -> -1
        activeIndex >= 0 -> activeIndex
        else -> 0
    }
    Column(
        Modifier.fillMaxWidth().height(108.dp).clickable(onClick = onOpen)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        when {
            state.loading -> Text("Finding lyrics…", color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium)
            lines.isNullOrEmpty() -> Row(verticalAlignment = Alignment.CenterVertically) {
                Text(state.error ?: "Lyrics unavailable", Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onRetry) { Text("Retry") }
            }
            else -> (-1..1).forEach { offset ->
                val line = lines.getOrNull(previewIndex + offset)
                if (line != null) {
                    Text(
                        line.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = if (offset == 0) MaterialTheme.typography.titleMedium
                        else MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(
                            alpha = if (offset == 0) 1f else 0.38f,
                        ),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun FullscreenLyrics(
    track: com.howdy.echowave.domain.model.Track,
    state: LyricsUiState,
    positionMs: Long,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    val lines = state.lines
    val activeIndex = lines?.let {
        com.howdy.echowave.data.remote.lyrics.currentLrcIndex(it, positionMs)
    } ?: -1
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    androidx.compose.runtime.LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) runCatching { listState.animateScrollToItem(activeIndex) }
    }
    Box(Modifier.fillMaxSize()) {
        AsyncImage(
            model = track.artworkUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().blur(42.dp),
        )
        Box(Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.52f), Color(0xFF130B1B).copy(alpha = 0.94f))),
        ))
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 28.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back to player")
                }
                TrackArtwork(track.artworkUrl, "Artwork for ${track.title}",
                    Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)))
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium)
                    Text(track.artist, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.Close, contentDescription = "Close lyrics")
                }
            }
            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Finding lyrics…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                lines.isNullOrEmpty() -> Column(
                    Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(state.error ?: "No lyrics found for this track.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = onRetry) { Text("Try again") }
                }
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 64.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp),
                ) {
                    itemsIndexed(lines) { index, line ->
                        val active = index == activeIndex || activeIndex < 0 && index == 0
                        val activeWord = if (active) com.howdy.echowave.data.remote.lyrics
                            .currentLrcWordIndex(line.words, positionMs) else -1
                        val wordScales = line.words.mapIndexed { wordIndex, _ ->
                            animateFloatAsState(
                                targetValue = if (active && wordIndex == activeWord) 1.12f else 1f,
                                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                                label = "fullscreenLyricWord",
                            ).value
                        }
                        val content = buildAnnotatedString {
                            if (line.words.isEmpty()) {
                                append(line.text)
                            } else {
                                line.words.forEachIndexed { wordIndex, word ->
                                    withStyle(SpanStyle(
                                        color = if (wordIndex == activeWord && active) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = if (active) 0.84f else 0.34f),
                                        fontWeight = if (wordIndex == activeWord && active) FontWeight.Bold else FontWeight.SemiBold,
                                        fontSize = wordScales[wordIndex].em,
                                    )) { append(word.text) }
                                }
                            }
                        }
                        Text(
                            content,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (active) 1f else 0.34f),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

/** Clean, progress-aware bars with a subtle pulse instead of intersecting sine lines. */@Composable
fun WavyStrip(seed: String, progress: Float, playing: Boolean) {
    val pulse by rememberInfiniteTransition(label = "waveform").animateFloat(
        initialValue = 0.88f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = LinearEasing),
            repeatMode = AnimRepeatMode.Reverse,
        ),
        label = "waveformPulse",
    )
    val bars = remember(seed) { waveformBars(seed, 56) }
    val active = MaterialTheme.colorScheme.primary
    val inactive = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
    Canvas(Modifier.fillMaxWidth().height(42.dp).padding(vertical = 2.dp)) {
        val centerY = size.height / 2f
        val slot = size.width / bars.size
        val stroke = minOf(slot * 0.56f, 3.dp.toPx())
        bars.forEachIndexed { index, value ->
            val x = slot * (index + 0.5f)
            val pulseFactor = if (playing) 0.88f + (pulse - 0.88f) * ((index % 5 + 1) / 5f) else 1f
            val halfHeight = (size.height * (0.12f + value * 0.37f) * pulseFactor).coerceAtLeast(2.dp.toPx())
            drawLine(
                color = if ((index + 0.5f) / bars.size <= progress) active else inactive,
                start = Offset(x, centerY - halfHeight),
                end = Offset(x, centerY + halfHeight),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

/** Synced-lyrics sheet: highlights the active line and follows it. Display only. */
@Composable
fun LyricsSheet(
    title: String?,
    state: LyricsUiState,
    positionMs: Long,
    onRetry: () -> Unit = {},
) {
    val lines = state.lines
    Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        Text(
            title ?: "Lyrics",
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        when {
            state.loading -> Text(
                "Finding lyrics…",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
            lines == null -> Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Text(state.error ?: "No lyrics found for this track.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onRetry) { Text("Try again") }
            }
            else -> {
                val current = com.howdy.echowave.data.remote.lyrics.currentLrcIndex(lines, positionMs)
                val listState = androidx.compose.foundation.lazy.rememberLazyListState()
                androidx.compose.runtime.LaunchedEffect(current) {
                    if (current >= 0) {
                        runCatching {
                            listState.animateScrollToItem((current - 2).coerceAtLeast(0))
                        }
                    }
                }
                androidx.compose.foundation.lazy.LazyColumn(state = listState) {
                    items(lines.size) { i ->
                        val active = i == current
                        val line = lines[i]
                        val activeWord = if (active) com.howdy.echowave.data.remote.lyrics
                            .currentLrcWordIndex(line.words, positionMs) else -1
                        val wordScales = line.words.mapIndexed { wordIndex, _ ->
                            animateFloatAsState(
                                targetValue = if (wordIndex == activeWord) 1.18f else 1f,
                                animationSpec = tween(durationMillis = 140),
                                label = "lyricWordZoom",
                            ).value
                        }
                        val content = buildAnnotatedString {
                            if (line.words.isEmpty()) {
                                withStyle(SpanStyle(color = if (active) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant)) {
                                    append(line.text.ifBlank { "♪" })
                                }
                            } else {
                                line.words.forEachIndexed { wordIndex, word ->
                                    withStyle(SpanStyle(
                                        color = if (wordIndex == activeWord) MaterialTheme.colorScheme.primary
                                        else if (active) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (wordIndex == activeWord) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = wordScales[wordIndex].em,
                                    )) { append(word.text) }
                                }
                            }
                        }
                        Text(
                            content,
                            style = if (active) MaterialTheme.typography.bodyLarge
                            else MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }
}
