package com.howdy.echowave.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.howdy.echowave.R
import com.howdy.echowave.domain.model.Track

@Composable
fun TrackRow(track: Track, onClick: () -> Unit, trailing: @Composable (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = track.artworkUrl,
            contentDescription = "Artwork for ${track.title}",
            modifier = Modifier.size(48.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(R.drawable.album),
            error = painterResource(R.drawable.album),
            fallback = painterResource(R.drawable.album),
        )
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(track.title)
            Text("${track.artist}${track.album?.let { " • $it" } ?: ""}")
        }
        trailing?.invoke()
    }
}
