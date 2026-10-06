package com.howdy.echowave.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.howdy.echowave.R

/** Requests full-size provider artwork so compact search thumbnails are not enlarged. */
fun artworkDisplayUrl(url: String?): String? {
    if (url.isNullOrBlank()) return url
    val highResolution = url.replace(ART_DIMENSION, "=w720-h720")
    return if (YT_THUMBNAIL.matches(highResolution)) {
        highResolution.replace("/default.jpg", "/hqdefault.jpg")
    } else {
        highResolution
    }
}

@Composable
fun TrackArtwork(
    url: String?,
    description: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    AsyncImage(
        model = artworkDisplayUrl(url),
        contentDescription = description,
        modifier = modifier,
        contentScale = contentScale,
        placeholder = painterResource(R.drawable.album),
        error = painterResource(R.drawable.album),
        fallback = painterResource(R.drawable.album),
    )
}

private val ART_DIMENSION = Regex("=w\\d+-h\\d+|=s\\d+(?:-[A-Za-z0-9-]+)?$")
private val YT_THUMBNAIL = Regex("https?://i\\.ytimg\\.com/vi/[^/]+/default\\.jpg(?:\\?.*)?")
