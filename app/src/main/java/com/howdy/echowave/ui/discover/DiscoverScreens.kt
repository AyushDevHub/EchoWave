package com.howdy.echowave.ui.discover

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.howdy.echowave.R
import com.howdy.echowave.domain.model.Album
import com.howdy.echowave.domain.model.Track
import com.howdy.echowave.ui.components.TrackRow

@Composable
fun ChartsScreen(
    tracks: List<Track>,
    loading: Boolean,
    onPlay: (List<Track>, Int) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Text(
            "Charts",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp),
        )
        if (loading) {
            CircularProgressIndicator(Modifier.padding(16.dp))
            return
        }
        if (tracks.isEmpty()) {
            Text("Charts unavailable right now.", modifier = Modifier.padding(16.dp))
            return
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(1),
            contentPadding = PaddingValues(horizontal = 8.dp),
        ) {
            items(tracks.indices.toList(), key = { tracks[it].id }) { i ->
                TrackRow(tracks[i], onClick = { onPlay(tracks, i) })
            }
        }
    }
}

@Composable
fun NewReleasesScreen(
    albums: List<Album>,
    loading: Boolean,
    onOpenAlbum: (Album) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Text(
            "New releases",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp),
        )
        if (loading) {
            CircularProgressIndicator(Modifier.padding(16.dp))
            return
        }
        if (albums.isEmpty()) {
            Text("New releases unavailable right now.", modifier = Modifier.padding(16.dp))
            return
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(albums, key = { it.id }) { album ->
                AlbumCard(album, onClick = { onOpenAlbum(album) })
            }
        }
    }
}

@Composable
fun AlbumCard(album: Album, onClick: () -> Unit) {    Column(Modifier.clickable(onClick = onClick)) {
        AsyncImage(
            model = album.artworkUrl,
            contentDescription = "Artwork for ${album.title}",
            modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(R.drawable.album),
            error = painterResource(R.drawable.album),
            fallback = painterResource(R.drawable.album),
        )
        Text(
            album.title,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            album.artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun MoodsScreen(
    moods: List<com.howdy.echowave.domain.model.Genre>,
    loading: Boolean,
    onOpenMood: (com.howdy.echowave.domain.model.Genre) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Text(
            "Moods & genres",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp),
        )
        if (loading) {
            CircularProgressIndicator(Modifier.padding(16.dp))
            return
        }
        if (moods.isEmpty()) {
            Text("Moods unavailable right now.", modifier = Modifier.padding(16.dp))
            return
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(moods, key = { it.id + it.title }) { mood ->
                MoodButton(mood, onClick = { onOpenMood(mood) })
            }
        }
    }
}

@Composable
fun MoodButton(
    mood: com.howdy.echowave.domain.model.Genre,
    onClick: () -> Unit,
) {
    val stripe = mood.color?.let { argb ->
        androidx.compose.ui.graphics.Color(
            red = ((argb shr 16) and 0xFF) / 255f,
            green = ((argb shr 8) and 0xFF) / 255f,
            blue = (argb and 0xFF) / 255f,
        )
    } ?: MaterialTheme.colorScheme.surfaceVariant
    androidx.compose.material3.Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = stripe.copy(alpha = 0.35f),
    ) {
        Text(
            mood.title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(16.dp),
        )
    }
}
