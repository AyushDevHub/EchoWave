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
import com.howdy.echowave.data.local.ThemePreset

/**
 * EchoWave's night listening palette: ink black, soft white and electric lilac.
 */
val PlumBlack = Color(0xFF09090D)
val PlumSurface = Color(0xFF121219)
val PlumVariant = Color(0xFF1C1C26)
val Terracotta = Color(0xFFE3B8FF)
val Peach = Color(0xFFE3B8FF)
val Cream = Color(0xFFF8F7FC)
val MutedRose = Color(0xFFA5A3B2)
val DeepTerracotta = Color(0xFF9A55C6)
val PlumText = Color(0xFF17131D)
val CreamBg = Color(0xFFF7F4FA)
val CreamSurface = Color(0xFFFFFFFF)
val EchoLime = Color(0xFFD995F7)
val EchoLavender = Color(0xFFBE7DE5)

private val EchoDarkScheme = darkColorScheme(
    primary = EchoLime,
    onPrimary = Color(0xFF22132B),
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
    primary = Color(0xFF8B42B8),
    onPrimary = Cream,
    secondary = EchoLavender,
    onSecondary = PlumText,
    tertiary = Terracotta,
    background = CreamBg,
    onBackground = PlumText,
    surface = CreamSurface,
    onSurface = PlumText,
    surfaceVariant = Color(0xFFECE6F1),
    onSurfaceVariant = Color(0xFF625A69),
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
    preset: ThemePreset = ThemePreset.PURPLE,
    fontChoice: com.howdy.echowave.data.local.FontChoice = com.howdy.echowave.data.local.FontChoice.SYSTEM,
    content: @Composable () -> Unit,
) {
    val presetScheme = darkColorScheme(
        primary = Color(preset.primary.toInt()),
        onPrimary = Color(preset.background.toInt()),
        secondary = Color(preset.primary.toInt()).copy(alpha = 0.82f),
        onSecondary = Color(preset.background.toInt()),
        tertiary = Color(preset.primary.toInt()).copy(alpha = 0.72f),
        background = Color(preset.background.toInt()),
        onBackground = Color(preset.text.toInt()),
        surface = Color(preset.surface.toInt()),
        onSurface = Color(preset.text.toInt()),
        surfaceVariant = Color(preset.surface.toInt()).copy(alpha = 0.9f),
        onSurfaceVariant = Color(preset.text.toInt()).copy(alpha = 0.66f),
    )
    // Presets are dark palettes; in light mode fall back to the
    // designed light scheme so light theme is actually reachable.
    val scheme = if (darkTheme) presetScheme else EchoLightScheme
    MaterialTheme(
        colorScheme = scheme,
        typography = buildTypography(fontChoice),
        content = content,
    )
}
