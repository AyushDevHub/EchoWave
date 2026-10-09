package com.howdy.echowave.ui.player

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Shuffle
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.howdy.echowave.R
import com.howdy.echowave.core.common.formatDuration
import com.howdy.echowave.data.remote.lyrics.LrcLine
import com.howdy.echowave.domain.model.PlaybackState
import com.howdy.echowave.domain.model.RepeatMode
import com.howdy.echowave.data.local.PlayerStyle
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
    val track = state.currentTrack
    if (track == null) {
        // Keep layout stable when nothing is playing; show buffering/error
        // instead of collapsing the bottom bar.
        if (state.isBuffering || state.error != null) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                tonalElevation = 8.dp,
                shadowElevation = 12.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (state.isBuffering) {
                        androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Spacer(Modifier.size(12.dp))
                        Text("Loading…", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text(
                            state.error ?: "Nothing playing",
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        return
    }
    val progress = if (state.durationMs > 0) {
        (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f)
    } else 0f
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
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
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)),
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
                Text(
                    if (state.isBuffering) "Buffering… • ${track.artist}" else track.artist,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onPrevious, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous track",
                    tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(26.dp))
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
    onVolume: (Float) -> Unit = {},
    onRetry: () -> Unit = {},
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    onPlayQueueAt: (Int) -> Unit = {},
    playlists: List<com.howdy.echowave.domain.model.Playlist> = emptyList(),
    onCreateAndAdd: (String) -> Unit = {},
    onAddToPlaylist: (Long) -> Unit = {},
    lyrics: LyricsUiState = LyricsUiState(),
    onRetryLyrics: () -> Unit = {},
    playerStyle: PlayerStyle = PlayerStyle.APPLE,
    onBack: () -> Unit = {},
) {
    var queueOpen by rememberSaveable { mutableStateOf(false) }
    var lyricsOpen by rememberSaveable { mutableStateOf(false) }
    var addOpen by rememberSaveable { mutableStateOf(false) }
    val track = state.currentTrack
    // Lyric timestamps can extend past a track's duration. Seeking to the
    // exact end fires Media3's ENDED event and advances the queue.
    val seekFromLyrics: (Long) -> Unit = { requestedMs ->
        val duration = track?.durationMs?.takeIf { it > 0L }
            ?: state.durationMs.takeIf { it > 0L }
        if (duration != null) {
            // Invalid provider timestamps must not be clamped to the end;
            // doing so triggers Media3's ended event and advances the queue.
            val lastSafePosition = (duration - 5_000L).coerceAtLeast(0L)
            if (requestedMs in 0L..lastSafePosition) onSeek(requestedMs)
        }
    }
    // Own ambient theme: artwork feeds a deep gradient wash plus a soft
    // primary glow that breathes while playing (EchoMusic/Apple-inspired,
    // own implementation, no copied code or assets).
    val glow by animateFloatAsState(
        targetValue = if (state.isPlaying) 1f else 0.55f,
        animationSpec = tween(900),
        label = "playerGlow",
    )
    Box(Modifier.fillMaxSize()) {
        if (track != null && playerStyle != PlayerStyle.SPOTIFY) {
            TrackArtwork(
                url = track.artworkUrl,
                description = null,
                modifier = Modifier.fillMaxSize().blur(if (playerStyle == PlayerStyle.APPLE) 52.dp else 0.dp),
                contentScale = ContentScale.Crop,
            )
            Box(Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        (if (playerStyle == PlayerStyle.APPLE) Color(0xFF16062D) else MaterialTheme.colorScheme.background).copy(alpha = 0.50f),
                        Color(0xFF09090D).copy(alpha = if (playerStyle == PlayerStyle.APPLE) 0.82f else 0.94f),
                    ),
                ),
            ))
            Box(
                Modifier.fillMaxWidth().height(280.dp).align(Alignment.TopCenter).background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f * glow),
                            Color.Transparent,
                        ),
                    ),
                ),
            )
            if (playerStyle == PlayerStyle.APPLE) AppleLyricBackdrop(
                lyrics = lyrics,
                positionMs = state.positionMs,
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(top = 84.dp),
            )
        } else if (track != null) {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                listOf(Color(0xFF171717), Color(0xFF090909)),
            )))
        }
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(42.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to EchoWave")
            }
            Column(Modifier.weight(1f)) {
                Text("NOW PLAYING", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(track?.album ?: "E C H O W A V E", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = { queueOpen = true }) {
                Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Up next", modifier = Modifier.size(22.dp))
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
                onVolume = onVolume,
                onRetry = onRetry,
                isFavorite = isFavorite,
                onToggleFavorite = onToggleFavorite,
                onAddToPlaylist = { addOpen = true },
                lyrics = lyrics,
                onOpenLyrics = { lyricsOpen = true },
                onLyricTap = seekFromLyrics,
                onRetryLyrics = onRetryLyrics,
                playerStyle = playerStyle,
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
            durationMs = state.durationMs,
            onSeek = seekFromLyrics,
            onBack = { lyricsOpen = false },
            onRetry = onRetryLyrics,
            playerStyle = playerStyle,
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
                        itemsIndexed(
                            state.queue,
                            key = { index, item -> "${item.id}-$index" },
                        ) { index, item ->
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
private fun AppleLyricBackdrop(
    lyrics: LyricsUiState,
    positionMs: Long,
    modifier: Modifier = Modifier,
) {
    val lines = lyrics.lines
    if (lyrics.loading || lines.isNullOrEmpty()) return
    val active = com.howdy.echowave.data.remote.lyrics.currentLrcIndex(lines, positionMs)
        .takeIf { it >= 0 } ?: return
    val line = lines.getOrNull(active) ?: return
    AnimatedContent(
        targetState = line.text,
        transitionSpec = { fadeIn(tween(450)) togetherWith fadeOut(tween(300)) },
        label = "backdropLyric",
        modifier = modifier,
    ) { text ->
        Text(
            text,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        )
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
    onVolume: (Float) -> Unit,
    onRetry: () -> Unit,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit = {},
    lyrics: LyricsUiState,
    onOpenLyrics: () -> Unit,
    onLyricTap: (Long) -> Unit,
    onRetryLyrics: () -> Unit,
    playerStyle: PlayerStyle = PlayerStyle.APPLE,
) {
    val track = state.currentTrack ?: return
    Column(
        Modifier.fillMaxSize().padding(bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (state.isBuffering) {
            Text("Finding your sound…", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
        }
        if (state.isLoadingNext) {
            Text("Finding next songs…", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
        }
        state.nextTracksMessage?.let { message ->
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Text(message, Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall)
            }
        }
        state.error?.let { error ->
            Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Row(Modifier.padding(start = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(error, Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = onRetry) { Text("Retry") }
                }
            }
        }

        Spacer(Modifier.height(6.dp))
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
            modifier = Modifier.fillMaxWidth(0.78f).aspectRatio(1f)
                .shadow(28.dp, RoundedCornerShape(28.dp), ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                .clip(RoundedCornerShape(28.dp))
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
                description = "Album artwork for ${coverTrack.title}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
                Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = "Add to playlist")
            }
        }

        Spacer(Modifier.height(22.dp))
        LyricsPreview(
            state = lyrics,
            positionMs = state.positionMs,
            durationMs = state.durationMs,
            onOpen = onOpenLyrics,
            onSeek = onLyricTap,
            onRetry = onRetryLyrics,
            playerStyle = playerStyle,
        )
        Spacer(Modifier.height(8.dp))
        Spacer(Modifier.height(16.dp))
        PlaybackScrubber(
            positionMs = state.positionMs,
            durationMs = state.durationMs,
            onSeek = onSeek,
        )
        Row(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDuration(state.positionMs), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatDuration(state.durationMs.takeIf { it > 0 }), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            IconButton(onClick = { onShuffle(!state.shuffleEnabled) }, modifier = Modifier.size(46.dp)) {
                Icon(Icons.Default.Shuffle, "Shuffle",
                    tint = if (state.shuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)) {
                IconButton(onClick = onPrev, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.Default.SkipPrevious, "Previous track", modifier = Modifier.size(32.dp))
                }
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 10.dp,
                    modifier = Modifier.size(76.dp).clickable(onClick = onToggle)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (state.isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(36.dp))
                    }
                }
                IconButton(onClick = onNext, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.Default.SkipNext, "Next track", modifier = Modifier.size(32.dp))
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

        Spacer(Modifier.height(8.dp))
        VolumeSlider(volume = state.volume, onVolume = onVolume)
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun VolumeSlider(volume: Float, onVolume: (Float) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 4.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.AutoMirrored.Filled.VolumeDown, contentDescription = "Volume down",
            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Slider(
            value = volume.coerceIn(0f, 1f),
            onValueChange = onVolume,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                .semantics { contentDescription = "Player volume" },
        )
        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Volume up",
            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun PlaybackScrubber(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
) {
    var draggingProgress by remember { mutableStateOf<Float?>(null) }
    val progress = draggingProgress ?: if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)
    Canvas(
        Modifier.fillMaxWidth().height(30.dp)
            .pointerInput(durationMs) {
                detectTapGestures { tap ->
                    if (durationMs > 0 && size.width > 0) {
                        val frac = (tap.x / size.width).coerceIn(0f, 1f)
                        onSeek((frac * durationMs).toLong().coerceIn(0L, durationMs))
                    }
                }
            }
            .pointerInput(durationMs) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        if (durationMs > 0 && size.width > 0) {
                            draggingProgress = (offset.x / size.width).coerceIn(0f, 1f)
                        }
                    },
                    onDragEnd = {
                        draggingProgress?.let { frac ->
                            onSeek((frac * durationMs).toLong().coerceIn(0L, durationMs))
                        }
                        draggingProgress = null
                    },
                    onDragCancel = {
                        draggingProgress = null
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        if (durationMs > 0 && size.width > 0) {
                            val current = draggingProgress ?: progress
                            val deltaFrac = dragAmount / size.width
                            draggingProgress = (current + deltaFrac).coerceIn(0f, 1f)
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
        drawCircle(activeColor, radius = if (draggingProgress != null) 8.dp.toPx() else 6.dp.toPx(), center = Offset(thumbX, y))
    }
}

@Composable
private fun LyricsPreview(
    state: LyricsUiState,
    positionMs: Long,
    durationMs: Long,
    onOpen: () -> Unit,
    onSeek: (Long) -> Unit,
    onRetry: () -> Unit,
    playerStyle: PlayerStyle = PlayerStyle.APPLE,
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
    // Fixed height: line flips crossfade inside these bounds, so the
    // scrubber and volume below never bounce when lyrics grow.
    Column(
        Modifier.fillMaxWidth().height(176.dp)
            .clickable(onClick = onOpen)
            .padding(horizontal = 4.dp, vertical = 6.dp),
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
            else -> {
                // 1 past + active + 2 next: active leads, upcoming drifts
                // in blurred until their turn (own implementation).
                val indices = (-1..2).map { previewIndex + it }
                    .filter { it in lines.indices }
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    indices.forEach { idx ->
                        val line = lines[idx]
                        val distance = (idx - previewIndex).coerceAtLeast(0)
                        val lineBlur by animateDpAsState(
                            targetValue = when (distance) {
                                0 -> 0.dp
                                1 -> 2.dp
                                else -> 5.dp
                            },
                            animationSpec = tween(400),
                            label = "previewLyricBlur",
                        )
                        val lineAlpha by animateFloatAsState(
                            targetValue = when {
                                idx < previewIndex -> 0.45f
                                distance == 0 -> 1f
                                distance == 1 -> 0.6f
                                else -> 0.38f
                            },
                            animationSpec = tween(400),
                            label = "previewLyricAlpha",
                        )
                        val lineScale by animateFloatAsState(
                            targetValue = if (idx == previewIndex) 1.08f else 0.96f,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "previewLyricScale",
                        )
                        Box(
                            Modifier.fillMaxWidth()
                                .graphicsLayer {
                                    alpha = lineAlpha
                                    scaleX = lineScale
                                    scaleY = lineScale
                                }
                                .blur(lineBlur),
                        ) {
                            AppleLyricLine(
                                line = line,
                                nextLineMs = lines.getOrNull(idx + 1)?.ms,
                                positionMs = positionMs,
                                active = idx == previewIndex,
                                textStyle = if (idx == previewIndex) MaterialTheme.typography.titleMedium
                                else MaterialTheme.typography.bodyMedium,
                                durationMs = durationMs,
                                onSeek = onSeek,
                                karaoke = idx == previewIndex,
                                seekable = false,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AppleLyricLine(
    line: LrcLine,
    nextLineMs: Long?,
    positionMs: Long,
    active: Boolean,
    textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineSmall,
    durationMs: Long = 0L,
    onSeek: (Long) -> Unit = {},
    karaoke: Boolean = true,
    seekable: Boolean = true,
) {
    // Karaoke word-by-word: the sounding word pops with kinetic scale and
    // the primary glow; sung words stay bright, upcoming words stay dim.
    // No per-character split — one fluid highlight step per word.
    val dim = MaterialTheme.colorScheme.onSurface.copy(alpha = if (active) 0.42f else 0.30f)
    val sung = MaterialTheme.colorScheme.onSurface
    val glow = MaterialTheme.colorScheme.primary
    if (line.words.isEmpty() || !karaoke) {
        Text(
            line.text,
            style = textStyle,
            color = if (active) MaterialTheme.colorScheme.onSurface else dim,
            modifier = if (seekable) Modifier.fillMaxWidth().clickable { onSeek(line.ms) }
                else Modifier.fillMaxWidth(),
        )
        return
    }
    val activeWord = if (active) com.howdy.echowave.data.remote.lyrics
        .currentLrcWordIndex(line.words, positionMs) else -1
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        line.words.forEachIndexed { wordIndex, word ->
            val isActiveWord = active && wordIndex == activeWord
            val isPast = active && wordIndex < activeWord
            val scale by animateFloatAsState(
                targetValue = if (isActiveWord) 1.14f else 1f,
                animationSpec = spring(
                    stiffness = Spring.StiffnessMedium,
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                ),
                label = "karaokeWordScale",
            )
            val alpha by animateFloatAsState(
                targetValue = when {
                    isActiveWord -> 1f
                    isPast -> 0.95f
                    active -> 0.55f
                    else -> 0.4f
                },
                animationSpec = tween(180),
                label = "karaokeWordAlpha",
            )
            val wordColor by animateColorAsState(
                targetValue = when {
                    isActiveWord -> glow
                    isPast -> sung
                    else -> dim
                },
                animationSpec = tween(260),
                label = "karaokeWordColor",
            )
            val content = remember(word.text, isActiveWord, isPast, active, wordColor) {
                buildAnnotatedString {
                    when {
                        isActiveWord -> withStyle(
                            SpanStyle(color = wordColor, fontWeight = FontWeight.Bold),
                        ) { append(word.text) }
                        isPast -> withStyle(
                            SpanStyle(color = wordColor, fontWeight = FontWeight.SemiBold),
                        ) { append(word.text) }
                        else -> withStyle(SpanStyle(color = wordColor)) { append(word.text) }
                    }
                }
            }
            Text(
                content,
                style = textStyle,
                modifier = Modifier
                    .graphicsLayer {
                        this.alpha = alpha
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(RoundedCornerShape(8.dp))
                    .then(if (seekable) Modifier.clickable { onSeek(word.ms) } else Modifier)
                    .padding(horizontal = 2.dp, vertical = 1.dp),
            )
        }
    }
}

@Composable
private fun FullscreenLyrics(
    track: com.howdy.echowave.domain.model.Track,
    state: LyricsUiState,
    positionMs: Long,
    durationMs: Long = 0L,
    onSeek: (Long) -> Unit = {},
    onBack: () -> Unit,
    onRetry: () -> Unit,
    playerStyle: PlayerStyle = PlayerStyle.APPLE,
) {
    val lines = state.lines
    val activeIndex = lines?.let {
        com.howdy.echowave.data.remote.lyrics.currentLrcIndex(it, positionMs)
    } ?: -1
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    // EchoMusic-style follow: keep the sounding line centered with a
    // spring scroll; line highlight itself crossfades via scale+alpha
    // below (own implementation, no blur transition).
    androidx.compose.runtime.LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) runCatching {
            listState.animateScrollToItem(
                (activeIndex - 2).coerceAtLeast(0),
                scrollOffset = -120,
            )
        }
    }
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(
                    MaterialTheme.colorScheme.surface,
                    MaterialTheme.colorScheme.background,
                ),
            ),
        ),
    ) {
        if (playerStyle == PlayerStyle.APPLE) {
            TrackArtwork(
                url = track.artworkUrl,
                description = null,
                modifier = Modifier.fillMaxSize().blur(36.dp).graphicsLayer { alpha = 0.46f },
                contentScale = ContentScale.Crop,
            )
            Box(Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.scrim.copy(alpha = 0.48f),
                        MaterialTheme.colorScheme.scrim.copy(alpha = 0.88f),
                    ),
                ),
            ))
        }
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to player")
                }
                TrackArtwork(track.artworkUrl, "Artwork for ${track.title}",
                    Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)))
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
                    contentPadding = PaddingValues(top = 48.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(26.dp),
                ) {
                    itemsIndexed(lines, key = { index, line -> "${line.ms}-${line.text.hashCode()}-$index" }) { index, line ->
                        val active = index == activeIndex || activeIndex < 0 && index == 0
                        val scale by animateFloatAsState(
                            targetValue = if (active) 1.04f else 0.96f,
                            animationSpec = spring(
                                stiffness = Spring.StiffnessLow,
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                            ),
                            label = "fullscreenLyricScale",
                        )
                        val alpha by animateFloatAsState(
                            targetValue = if (active) 1f else 0.45f,
                            animationSpec = tween(350),
                            label = "fullscreenLyricAlpha",
                        )
                        Box(Modifier.fillMaxWidth().graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                        }) {
                            AppleLyricLine(
                                line = line,
                                nextLineMs = lines.getOrNull(index + 1)?.ms,
                                positionMs = positionMs,
                                active = active,
                                textStyle = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                                ),
                                durationMs = durationMs,
                                onSeek = onSeek,
                                karaoke = playerStyle != PlayerStyle.SPOTIFY,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Clean, progress-aware bars with a subtle pulse instead of intersecting sine lines. */@Composable
fun WavyStrip(seed: String, progress: Float, playing: Boolean) {
    // Only animate while playing; static bars when paused save battery.
    val pulse = if (playing) {
        val infinite = rememberInfiniteTransition(label = "waveform")
        infinite.animateFloat(
            initialValue = 0.88f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(850, easing = LinearEasing),
                repeatMode = AnimRepeatMode.Reverse,
            ),
            label = "waveformPulse",
        ).value
    } else 1f
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

/** Synced-lyrics sheet: highlights the active line and follows it. Tap a word to jump. */
@Composable
fun LyricsSheet(
    title: String?,
    state: LyricsUiState,
    positionMs: Long,
    durationMs: Long = 0L,
    onSeek: (Long) -> Unit = {},
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
                    items(lines.size, key = { i -> "${lines[i].ms}-${lines[i].text.hashCode()}-$i" }) { i ->
                        val active = i == current
                        val line = lines[i]
                        Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)) {
                            AppleLyricLine(
                                line = line,
                                nextLineMs = lines.getOrNull(i + 1)?.ms,
                                positionMs = positionMs,
                                active = active,
                                textStyle = if (active) MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                                else MaterialTheme.typography.bodyMedium,
                                durationMs = durationMs,
                                onSeek = onSeek,
                            )
                        }
                    }
                }
            }
        }
    }
}
