package com.howdy.echowave.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

/**
 * ECHO/FIELD signature theme (pinned, not dynamic): deep plum + terracotta
 * + cream, editorial serif display, letterspaced caps labels.
 */
val PlumBlack = Color(0xFF17141B)
val PlumSurface = Color(0xFF211D27)
val PlumVariant = Color(0xFF302A36)
val Terracotta = Color(0xFFE9959E)
val Peach = Color(0xFFE6A5B2)
val Cream = Color(0xFFF5F0F1)
val MutedRose = Color(0xFFB9AAB8)
val DeepTerracotta = Color(0xFF9D536A)
val PlumText = Color(0xFF21161F)
val CreamBg = Color(0xFFF5EEF0)
val CreamSurface = Color(0xFFFFFAFC)
val EchoLime = Color(0xFFD5EA72)
val EchoLavender = Color(0xFFB88ED8)

private val EchoDarkScheme = darkColorScheme(
    primary = EchoLime,
    onPrimary = PlumText,
    secondary = EchoLavender,
    onSecondary = PlumText,
    tertiary = Terracotta,
    background = PlumBlack,
    onBackground = Cream,
    surface = PlumSurface,
    onSurface = Cream,
    surfaceVariant = PlumVariant,
    onSurfaceVariant = MutedRose,
)

private val EchoLightScheme = lightColorScheme(
    primary = DeepTerracotta,
    onPrimary = Cream,
    secondary = EchoLavender,
    onSecondary = PlumText,
    tertiary = Terracotta,
    background = CreamBg,
    onBackground = PlumText,
    surface = CreamSurface,
    onSurface = PlumText,
    surfaceVariant = Color(0xFFEADDCF),
    onSurfaceVariant = Color(0xFF6B4A3E),
)

val SerifDisplay = FontFamily.SansSerif

/** Editorial display headline, e.g. "New sounds live here." */
val DisplayHeadline = TextStyle(
    fontFamily = SerifDisplay,
    fontSize = 32.sp,
    lineHeight = 38.sp,
    letterSpacing = (-0.8).sp,
    color = Color.Unspecified,
)

/** Letterspaced uppercase section label, e.g. "ARTISTS TO WATCH". */
fun capsLabel(base: TextStyle = TextStyle.Default): TextStyle = base.copy(
    letterSpacing = 0.18.sp,
    fontSize = 12.sp,
)

@Composable
fun EchoWaveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) EchoDarkScheme else EchoLightScheme,
        typography = Typography,
        content = content,
    )
}
