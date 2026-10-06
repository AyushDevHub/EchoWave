package com.howdy.echowave.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.howdy.echowave.R
import com.howdy.echowave.core.common.formatDuration
import com.howdy.echowave.domain.model.PlaybackState
import com.howdy.echowave.domain.model.RepeatMode

@Composable
fun MiniPlayer(state: PlaybackState, onToggle: () -> Unit, onOpen: () -> Unit) {
    val t = state.currentTrack ?: return
    Surface(
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = t.artworkUrl,
                contentDescription = "Artwork for ${t.title}",
                modifier = Modifier.size(40.dp),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.album),
                error = painterResource(R.drawable.album),
                fallback = painterResource(R.drawable.album),
            )
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(t.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    t.artist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onToggle) {
                Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Play/Pause")
            }
        }
    }
}

@Composable
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
    val t = state.currentTrack
    Column(Modifier.padding(16.dp)) {
        Text("Now Playing", style = MaterialTheme.typography.titleLarge)
        if (t == null) {
            Text("Nothing playing. Search for a song.")
            return
        }
        if (state.isBuffering) Text("Loading…")
        state.error?.let {
            Text("Unable to play this track. $it")
            androidx.compose.material3.Button(onClick = onRetry) { Text("Retry") }
        }
        AsyncImage(
            model = t.artworkUrl,
            contentDescription = "Artwork for ${t.title}",
            modifier = Modifier.size(240.dp).align(Alignment.CenterHorizontally),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(R.drawable.album),
            error = painterResource(R.drawable.album),
            fallback = painterResource(R.drawable.album),
        )
        Text(
            t.title,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            t.artist,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row {
            Text("Favorite", modifier = Modifier.weight(1f))
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavorite) "Unfavorite" else "Favorite",
                )
            }
        }
        Slider(
            value = state.positionMs.toFloat(),
            onValueChange = { onSeek(it.toLong()) },
            valueRange = 0f..(state.durationMs.takeIf { it > 0 } ?: 1).toFloat(),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                formatDuration(state.positionMs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            Text(
                formatDuration(state.durationMs.takeIf { it > 0 }),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row {
            IconButton(onClick = { onShuffle(!state.shuffleEnabled) }) { Icon(Icons.Default.Shuffle, "Shuffle") }
            IconButton(onClick = onPrev) { Icon(Icons.Default.SkipPrevious, "Previous") }
            IconButton(onClick = onToggle) {
                Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play/Pause")
            }
            IconButton(onClick = onNext) { Icon(Icons.Default.SkipNext, "Next") }
            IconButton(onClick = {
                onRepeat(when (state.repeatMode) {
                    RepeatMode.OFF -> RepeatMode.ALL
                    RepeatMode.ALL -> RepeatMode.ONE
                    RepeatMode.ONE -> RepeatMode.OFF
                })
            }) {
                Icon(if (state.repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat, "Repeat")
            }
        }
        Text("Up next")
        state.queue.forEachIndexed { i, q ->
            com.howdy.echowave.ui.components.TrackRow(q, onClick = { onPlayQueueAt(i) })
        }
    }
}
