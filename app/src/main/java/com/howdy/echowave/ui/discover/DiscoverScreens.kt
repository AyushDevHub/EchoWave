package com.howdy.echowave.ui.discover

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.howdy.echowave.domain.model.Track
import com.howdy.echowave.ui.components.TrackRow

@Composable
fun ChartsScreen(
    tracks: List<Track>,
    loading: Boolean,
    error: String? = null,
    onRetry: () -> Unit = {},
    onPlay: (List<Track>, Int) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Text(
            "Charts",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp),
        )
        if (loading && tracks.isEmpty()) {
            CircularProgressIndicator(Modifier.padding(16.dp))
            return
        }
        if (error != null && tracks.isEmpty()) {
            Column(Modifier.padding(16.dp)) {
                Text("Couldn't load charts. $error")
                androidx.compose.material3.TextButton(onClick = onRetry) {
                    Text("Retry")
                }
            }
            return
        }
        if (tracks.isEmpty()) {
            Text("Charts unavailable right now.", modifier = Modifier.padding(16.dp))
            return
        }
        androidx.compose.foundation.lazy.LazyColumn(
            contentPadding = PaddingValues(horizontal = 8.dp),
        ) {
            items(tracks.size, key = { i -> "${tracks[i].id}-$i" }) { i ->
                TrackRow(tracks[i], onClick = { onPlay(tracks, i) })
            }
        }
    }
}
