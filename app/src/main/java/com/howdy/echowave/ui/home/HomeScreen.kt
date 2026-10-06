package com.howdy.echowave.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.howdy.echowave.domain.model.Track
import com.howdy.echowave.ui.components.TrackRow

@Composable
fun HomeScreen(recent: List<Track>, onPlay: (List<Track>, Int) -> Unit) {
    Column {
        Text("EchoWave")
        Text("Recently Played")
        if (recent.isEmpty()) Text("Search and play something — it shows up here.")
        recent.take(10).forEachIndexed { i, t -> TrackRow(t, onClick = { onPlay(recent, i) }) }
        Text("Quick Access")
        Text("Favorites and history live in Library.")
    }
}
