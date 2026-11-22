package com.kenjigames.ividsmusic.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min

/**
 * Platform-specific extractor that samples a vibrant, high-contrast dominant color from
 * album artwork URLs for the Living Album Hero and Now Playing ambient glow.
 */
expect object ColorExtractor {
    /**
     * Fetches and samples an accent color from [imageUrl] asynchronously, returning
     * [fallback] if the image cannot be loaded or lacks sufficient contrast on ObsidianStage.
     */
    suspend fun extractDominantColor(
        imageUrl: String?,
        fallback: Color = IvidsColorTokens.PrimaryCyan
    ): Color
}

/**
 * Normalizes an extracted ARGB integer into a dark-theme-safe accent color by enforcing
 * minimum brightness and saturation thresholds against ObsidianStage (#0A0B0E).
 */
internal fun clampAccentContrast(argb: Int, fallback: Color): Color {
    val r = ((argb shr 16) and 0xFF) / 255f
    val g = ((argb shr 8) and 0xFF) / 255f
    val b = (argb and 0xFF) / 255f

    val maxC = max(r, max(g, b))
    val minC = min(r, min(g, b))
    val delta = maxC - minC
    val saturation = if (maxC == 0f) 0f else delta / maxC

    // Reject near-grayscale/monochrome covers so UI keeps IVIDS cyan vitality
    if (saturation < 0.22f && maxC < 0.45f) return fallback

    val boost = if (maxC < 0.55f && maxC > 0.05f) 0.55f / maxC else 1f
    val nr = (r * boost).coerceIn(0.18f, 0.96f)
    val ng = (g * boost).coerceIn(0.18f, 0.96f)
    val nb = (b * boost).coerceIn(0.22f, 0.98f)
    return Color(red = nr, green = ng, blue = nb, alpha = 1f)
}
