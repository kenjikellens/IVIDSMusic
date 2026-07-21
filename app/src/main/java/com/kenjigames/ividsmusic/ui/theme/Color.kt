package com.kenjigames.ividsmusic.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Enhanced color tokens for the IVIDS Music dark glassmorphism mobile theme.
 */
val DarkBackground = Color(0xFF09090E)
val DarkSurface = Color(0xFF13131A)
val DarkSurfaceVariant = Color(0xFF1E1E28)
val GlassSurface = Color(0x1F222232)
val GlassBorder = Color(0x33FFFFFF)

val PrimaryAccent = Color(0xFF8B5CF6)
val PrimaryAccentVariant = Color(0xFF7C3AED)
val SecondaryAccent = Color(0xFF06B6D4)

val TextPrimary = Color(0xFFF3F4F6)
val TextSecondary = Color(0x99F3F4F6)
val TextMuted = Color(0x66F3F4F6)

/**
 * Generates a smooth ambient gradient brush color array from a dynamic accent color.
 */
fun getDynamicGradient(accentColor: Color): List<Color> {
    return listOf(
        accentColor.copy(alpha = 0.25f),
        Color(0x10000000),
        DarkBackground
    )
}
