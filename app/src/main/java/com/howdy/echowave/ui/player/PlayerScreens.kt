package com.howdy.echowave.ui.player

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    val miniTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val miniProgressColor = MaterialTheme.colorScheme.primary

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().padding(start = 8.dp, end = 2.dp, top = 6.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TrackArtwork(
                    url = track.artworkUrl,
                    description = "Artwork for ${track.title}",
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onOpen),
                )
                Column(
                    Modifier.weight(1f).clickable(onClick = onOpen).padding(horizontal = 10.dp),
                ) {
                    Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium)
                    Text(track.artist, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onPrevious, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous track", modifier = Modifier.size(22.dp))
                }
                IconButton(onClick = onToggle, modifier = Modifier.size(42.dp)) {
                    Icon(
                        if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(26.dp),
                    )
                }
                IconButton(onClick = onNext, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next track", modifier = Modifier.size(22.dp))
                }
            }
            Canvas(Modifier.fillMaxWidth().height(2.dp)) {
                drawLine(
                    color = miniTrackColor,
                    start = Offset.Zero,
                    end = Offset(size.width, 0f),
                    strokeWidth = size.height,
                )
                drawLine(
                    color = miniProgressColor,
                    start = Offset.Zero,
                    end = Offset(size.width * progress, 0f),
                    strokeWidth = size.height,
                )
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
) {
    var queueOpen by remember { mutableStateOf(false) }
    val track = state.currentTrack
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
            )
        }
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
        TrackArtwork(
            url = track.artworkUrl,
            description = "Album artwork for ${track.title}. Swipe to change tracks.",
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(28.dp))
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
            contentScale = ContentScale.Crop,
        )
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
        }

        val progress = if (state.durationMs > 0) {
            (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f)
        } else 0f
        WavyStrip(seed = track.id, progress = progress, playing = state.isPlaying)
        Slider(
            value = state.positionMs.toFloat().coerceIn(0f, state.durationMs.takeIf { it > 0 }?.toFloat() ?: 1f),
            onValueChange = { onSeek(it.toLong()) },
            valueRange = 0f..(state.durationMs.takeIf { it > 0 } ?: 1).toFloat(),
            modifier = Modifier.fillMaxWidth().height(34.dp),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDuration(state.positionMs), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatDuration(state.durationMs.takeIf { it > 0 }), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onShuffle(!state.shuffleEnabled) }) {
                Icon(Icons.Default.Shuffle, "Shuffle",
                    tint = if (state.shuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onPrev, modifier = Modifier.size(54.dp)) {
                Icon(Icons.Default.SkipPrevious, "Previous track", modifier = Modifier.size(32.dp))
            }
            Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(68.dp).clickable(onClick = onToggle)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(36.dp))
                }
            }
            IconButton(onClick = onNext, modifier = Modifier.size(54.dp)) {
                Icon(Icons.Default.SkipNext, "Next track", modifier = Modifier.size(32.dp))
            }
            IconButton(onClick = {
                onRepeat(when (state.repeatMode) {
                    RepeatMode.OFF -> RepeatMode.ALL
                    RepeatMode.ALL -> RepeatMode.ONE
                    RepeatMode.ONE -> RepeatMode.OFF
                })
            }) {
                Icon(if (state.repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                    "Repeat", tint = if (state.repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Clean, progress-aware bars with a subtle pulse instead of intersecting sine lines. */
@Composable
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
