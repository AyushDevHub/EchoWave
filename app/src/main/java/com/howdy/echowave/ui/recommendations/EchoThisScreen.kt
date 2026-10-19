package com.howdy.echowave.ui.recommendations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.howdy.echowave.core.common.AppResult
import com.howdy.echowave.domain.model.Track
import com.howdy.echowave.domain.recommendation.RecommendedTrack
import com.howdy.echowave.ui.components.TrackArtwork

@Composable
fun EchoThisScreen(
    seed: Track?,
    onBack: () -> Unit,
    recommend: suspend (Track) -> AppResult<List<RecommendedTrack>>,
    onAddToQueue: (Track) -> Unit,
) {
    var retry by remember(seed?.id) { mutableStateOf(0) }
    var loading by remember(seed?.id) { mutableStateOf(seed != null) }
    var error by remember(seed?.id) { mutableStateOf<String?>(null) }
    var tracks by remember(seed?.id) { mutableStateOf(emptyList<RecommendedTrack>()) }
    var addedIds by remember(seed?.id) { mutableStateOf(emptySet<String>()) }
    var showInfo by remember { mutableStateOf(false) }
    var selectedExplanation by remember { mutableStateOf<RecommendedTrack?>(null) }

    LaunchedEffect(seed?.id, retry) {
        val current = seed
        if (current == null) {
            loading = false
            error = "Start a song first, then try Echo This."
            return@LaunchedEffect
        }
        loading = true
        error = null
        when (val result = recommend(current)) {
            is AppResult.Ok -> tracks = result.value
            is AppResult.Err -> {
                tracks = emptyList()
                error = result.message
            }
        }
        loading = false
    }

    Box(Modifier.fillMaxSize()) {
      Box(
          Modifier.align(Alignment.TopEnd).padding(top = 30.dp, end = 40.dp)
              .size(220.dp).blur(72.dp)
              .background(Brush.radialGradient(listOf(
                  MaterialTheme.colorScheme.primary.copy(alpha = 0.18f), Color.Transparent,
              )), CircleShape),
      )
      Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Column(Modifier.weight(1f)) {
                Text("Echo This", style = MaterialTheme.typography.headlineSmall)
                Text(seed?.title ?: "Similar tracks", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            IconButton(onClick = { showInfo = true }) {
                Icon(Icons.Default.Info, contentDescription = "About Echo This",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        when {
            loading -> Column(
                Modifier.fillMaxWidth().weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
                Text("Finding tracks…", Modifier.padding(top = 12.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            error != null -> Column(
                Modifier.fillMaxWidth().weight(1f).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(error.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (seed != null) TextButton(onClick = { retry++ }) { Text("Try again") }
            }
            tracks.isEmpty() -> Column(
                Modifier.fillMaxWidth().weight(1f).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("No similar tracks found", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Try another song when more candidates are available.",
                    Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> LazyColumn(
                Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(tracks, key = { it.track.id }) { recommendation ->
                    val track = recommendation.track
                    val added = track.id in addedIds
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.68f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                if (!added) {
                                    onAddToQueue(track)
                                    addedIds = addedIds + track.id
                                }
                            }.padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TrackArtwork(track.artworkUrl, "Artwork for ${track.title}",
                                Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)))
                            Column(Modifier.weight(1f).padding(start = 12.dp, end = 4.dp)) {
                                Text(track.title, style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                Text(track.artist, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            }
                            IconButton(onClick = { selectedExplanation = recommendation }, modifier = Modifier.size(40.dp)) {
                                Icon(Icons.Default.Info, contentDescription = "Why ${track.title}",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp))
                            }
                            IconButton(
                                onClick = {
                                    if (!added) {
                                        onAddToQueue(track)
                                        addedIds = addedIds + track.id
                                    }
                                },
                                enabled = !added,
                                modifier = Modifier.size(40.dp),
                            ) {
                                Icon(if (added) Icons.Default.Check else Icons.Default.Add,
                                    contentDescription = if (added) "Added" else "Add ${track.title} to queue",
                                    tint = if (added) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }
      }
    }

    if (showInfo) AlertDialog(
        onDismissRequest = { showInfo = false },
        title = { Text("Echo This") },
        text = { Text("Picks are based on this track and listening signals saved on this device. Add tracks to the end of your queue.") },
        confirmButton = { TextButton(onClick = { showInfo = false }) { Text("Done") } },
    )
    selectedExplanation?.let { recommendation ->
        AlertDialog(
            onDismissRequest = { selectedExplanation = null },
            title = { Text("Why this pick") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    recommendation.explanation.reasons.forEach { reason ->
                        Text(reason, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (recommendation.explanation.reasons.isEmpty()) {
                        Text("This track matches your current listening context.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { selectedExplanation = null }) { Text("Done") } },
        )
    }
}
