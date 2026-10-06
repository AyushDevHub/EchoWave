package com.howdy.echowave.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.howdy.echowave.R
import com.howdy.echowave.domain.model.PlaybackState
import com.howdy.echowave.domain.model.RepeatMode

@Composable
fun MiniPlayer(state: PlaybackState, onToggle: () -> Unit, onOpen: () -> Unit) {
    val t = state.currentTrack ?: return
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(horizontal = 12.dp, vertical = 8.dp),
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
            Text(t.title)
            Text(t.artist)
        }
        IconButton(onClick = onToggle) {
            Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Play/Pause")
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
) {
    val t = state.currentTrack
    Column {
        Text("Now Playing")
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
        Text(t.title)
        Text(t.artist)
        Slider(
            value = state.positionMs.toFloat(),
            onValueChange = { onSeek(it.toLong()) },
            valueRange = 0f..(state.durationMs.takeIf { it > 0 } ?: 1).toFloat(),
        )
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
            Text("${if (i == state.queueIndex) "▶ " else ""}${q.title} — ${q.artist}")
        }
    }
}
