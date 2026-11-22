package com.kenjigames.ividsmusic.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min

/**
 * Platform-specific extractor that samples a vibrant, true-to-artwork dominant color from
 * album cover URLs for blurred player backdrops, hero headers, and per-card ambient tinting.
 */
expect object ColorExtractor {
    /**
     * Fetches and samples an accent color from [imageUrl] asynchronously, returning
     * [fallback] if the image cannot be loaded or is near-grayscale.
     */
    suspend fun extractDominantColor(
        imageUrl: String?,
        fallback: Color = IvidsColorTokens.PrimaryCyan
    ): Color

    /**
     * Extracts a subtle per-card dominant tint from [imageUrl], or returns `null` when the cover
     * is missing or monochrome so the card remains a pure neutral dark surface.
     */
    suspend fun extractCardTintColor(imageUrl: String?): Color?
}

/**
 * Normalizes an extracted ARGB integer into a dark-theme-safe accent color by enforcing
 * minimum brightness and saturation thresholds without injecting any artificial blue bias.
 */
internal fun clampAccentContrast(argb: Int, fallback: Color): Color {
    val r = ((argb shr 16) and 0xFF) / 255f
    val g = ((argb shr 8) and 0xFF) / 255f
    val b = (argb and 0xFF) / 255f

    val maxC = max(r, max(g, b))
    val minC = min(r, min(g, b))
    val delta = maxC - minC
    val saturation = if (maxC == 0f) 0f else delta / maxC

    // Reject near-grayscale/monochrome covers so callers can keep neutral surfaces or brand fallback
    if (saturation < 0.16f || maxC < 0.12f) return fallback

    val boost = if (maxC < 0.58f && maxC > 0.05f) 0.58f / maxC else 1f
    val nr = (r * boost).coerceIn(0.14f, 0.96f)
    val ng = (g * boost).coerceIn(0.14f, 0.96f)
    val nb = (b * boost).coerceIn(0.14f, 0.96f)
    return Color(red = nr, green = ng, blue = nb, alpha = 1f)
}
