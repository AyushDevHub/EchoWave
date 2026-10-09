package com.howdy.echowave.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.howdy.echowave.R

/**
 * App brand mark (EchoWave Retro emblem). Single source so Home, Search
 * and Settings headers stay identical. Rounded-square source, circle
 * cropped by default to match the previous "E" avatar slots.
 */
@Composable
fun AppLogo(
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    shape: Shape = CircleShape,
    contentDescription: String = "EchoWave logo",
) {
    Image(
        painter = painterResource(R.drawable.app_logo),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier.size(size).clip(shape),
    )
}
