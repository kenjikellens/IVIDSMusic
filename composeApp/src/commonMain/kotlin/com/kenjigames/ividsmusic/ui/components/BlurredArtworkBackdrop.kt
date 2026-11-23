package com.kenjigames.ividsmusic.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.kenjigames.ividsmusic.ui.theme.ColorExtractor
import com.kenjigames.ividsmusic.ui.theme.IvidsColorTokens

/**
 * Downloads, downsamples, and pre-blurs artwork at [imageUrl] once on a background dispatcher,
 * returning a small cached [ImageBitmap] that renders at 60+ FPS with zero per-frame blur cost.
 */
expect suspend fun loadBlurredArtworkBitmap(imageUrl: String?): ImageBitmap?

/**
 * Renders a strictly clipped, pre-blurred album/track artwork backdrop with a 420ms crossfade
 * and dark contrast scrim, avoiding live per-frame Gaussian blur passes for smooth 60+ FPS UI.
 */
@Composable
fun BlurredArtworkBackdrop(
    artworkUrl: String?,
    modifier: Modifier = Modifier,
    tintColor: Color? = null,
    scrimAlpha: Float = 0.72f,
    fallbackColor: Color = IvidsColorTokens.ObsidianStage
) {
    var blurredBitmap by remember(artworkUrl) { mutableStateOf<ImageBitmap?>(null) }
    var extractedTint by remember(artworkUrl) { mutableStateOf<Color?>(null) }

    LaunchedEffect(artworkUrl) {
        if (artworkUrl.isNullOrBlank()) {
            blurredBitmap = null
            extractedTint = null
        } else {
            blurredBitmap = loadBlurredArtworkBitmap(artworkUrl)
            extractedTint = ColorExtractor.extractCardTintColor(artworkUrl)
        }
    }
    val effectiveTint = extractedTint ?: tintColor

    Box(modifier = modifier.clipToBounds().background(fallbackColor)) {
        Crossfade(
            targetState = blurredBitmap,
            animationSpec = tween(durationMillis = 420),
            label = "BlurredArtworkCrossfade"
        ) { currentBitmap ->
            Box(modifier = Modifier.fillMaxSize().clipToBounds().background(fallbackColor)) {
                if (currentBitmap != null) {
                    Image(
                        bitmap = currentBitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        filterQuality = FilterQuality.Medium,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (effectiveTint != null) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    effectiveTint.copy(alpha = if (currentBitmap != null) 0.24f else 0.42f),
                                    effectiveTint.copy(alpha = if (currentBitmap != null) 0.10f else 0.18f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width * 0.25f, size.height * 0.35f),
                                radius = size.maxDimension * 0.85f
                            )
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    fallbackColor.copy(alpha = (scrimAlpha - 0.10f).coerceIn(0.35f, 0.92f)),
                                    fallbackColor.copy(alpha = (scrimAlpha + 0.08f).coerceIn(0.45f, 0.96f))
                                )
                            )
                        )
                )
            }
        }
    }
}
