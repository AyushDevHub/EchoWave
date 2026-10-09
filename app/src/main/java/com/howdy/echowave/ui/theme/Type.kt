package com.howdy.echowave.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.howdy.echowave.R

private val EchoSans = FontFamily.SansSerif

private val OutfitFamily = FontFamily(Font(R.font.outfit, FontWeight.Normal), Font(R.font.outfit, FontWeight.Medium), Font(R.font.outfit, FontWeight.SemiBold), Font(R.font.outfit, FontWeight.Bold))
private val JakartaFamily = FontFamily(Font(R.font.plus_jakarta_sans, FontWeight.Normal), Font(R.font.plus_jakarta_sans, FontWeight.Medium), Font(R.font.plus_jakarta_sans, FontWeight.SemiBold), Font(R.font.plus_jakarta_sans, FontWeight.Bold))

/**
 * Font picker. Outfit and Plus Jakarta Sans are bundled OFL cuts.
 * Google Sans is proprietary and cannot be bundled, so it uses the
 * platform sans as a stand-in. Sans Flex maps to system sans with
 * wider tracking until an open cut is vendored.
 */
fun echoFontFamily(choice: com.howdy.echowave.data.local.FontChoice): FontFamily = when (choice) {
    com.howdy.echowave.data.local.FontChoice.OUTFIT -> OutfitFamily
    com.howdy.echowave.data.local.FontChoice.JAKARTA -> JakartaFamily
    else -> FontFamily.SansSerif
}

fun buildTypography(choice: com.howdy.echowave.data.local.FontChoice): Typography {
    val family = echoFontFamily(choice)
    val tight = when (choice) {
        com.howdy.echowave.data.local.FontChoice.OUTFIT -> (-0.5).sp
        com.howdy.echowave.data.local.FontChoice.JAKARTA -> 0.sp
        com.howdy.echowave.data.local.FontChoice.GOOGLE_SANS -> (-0.8).sp
        com.howdy.echowave.data.local.FontChoice.SANS_FLEX -> 0.15.sp
        else -> (-1.5).sp
    }
    return Typography(
        displayLarge = Typography.displayLarge.copy(fontFamily = family, letterSpacing = tight),
        displayMedium = Typography.displayMedium.copy(fontFamily = family),
        headlineLarge = Typography.headlineLarge.copy(fontFamily = family),
        headlineMedium = Typography.headlineMedium.copy(fontFamily = family),
        headlineSmall = Typography.headlineSmall.copy(fontFamily = family),
        titleLarge = Typography.titleLarge.copy(fontFamily = family),
        titleMedium = Typography.titleMedium.copy(fontFamily = family),
        bodyLarge = Typography.bodyLarge.copy(fontFamily = family),
        bodyMedium = Typography.bodyMedium.copy(fontFamily = family),
        bodySmall = Typography.bodySmall.copy(fontFamily = family),
        labelLarge = Typography.labelLarge.copy(fontFamily = family),
        labelMedium = Typography.labelMedium.copy(fontFamily = family),
        labelSmall = Typography.labelSmall.copy(fontFamily = family),
    )
}

/** Expressive, crisp sans-serif typography, using Android's bundled sans face. */
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = EchoSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 48.sp,
        lineHeight = 52.sp,
        letterSpacing = (-1.5).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = EchoSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 38.sp,
        lineHeight = 42.sp,
        letterSpacing = (-1.1).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = EchoSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.8).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = EchoSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = EchoSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.35).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = EchoSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp,
        lineHeight = 27.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = EchoSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = EchoSans,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = EchoSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.15.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = EchoSans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = EchoSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.5.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = EchoSans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.45.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = EchoSans,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp,
    ),
)
