package com.kenjigames.ividsmusic.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Color tokens for the IVIDS Music dark glassmorphism theme.
 */
val DarkBackground = Color(0xFF0F0F12)
val DarkSurface = Color(0xFF18181D)
val DarkSurfaceVariant = Color(0xFF24242C)
val GlassSurface = Color(0x33FFFFFF)
val GlassBorder = Color(0x22FFFFFF)

val PrimaryAccent = Color(0xFF7C4DFF)
val PrimaryAccentVariant = Color(0xFF651FFF)
val SecondaryAccent = Color(0xFF00E5FF)

val TextPrimary = Color(0xFFEEEEEE)
val TextSecondary = Color(0xB3EEEEEE)
val TextMuted = Color(0x80EEEEEE)

/**
 * Generates a smooth dark gradient brush color array from a dynamic accent color.
 */
fun getDynamicGradient(accentColor: Color): List<Color> {
    return listOf(
        accentColor.copy(alpha = 0.35f),
        DarkBackground
    )
}
