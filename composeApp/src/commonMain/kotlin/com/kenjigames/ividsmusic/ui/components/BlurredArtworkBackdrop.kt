package com.kenjigames.ividsmusic.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.kenjigames.ividsmusic.ui.theme.ColorExtractor
import com.kenjigames.ividsmusic.ui.theme.IvidsColorTokens

/**
 * Returns true when the current platform supports hardware-accelerated [Modifier.blur] rendering
 * (Desktop Skia and Android 12+ RenderEffect), or false on older Android API 24-30 devices.
 */
expect fun isNativeRenderBlurSupported(): Boolean

/**
 * Renders a softly blurred album/track artwork backdrop with a 450ms crossfade and dark contrast
 * scrim, falling back to an artwork-sampled radial color wash when [artworkUrl] is absent or on Android < 12.
 */
@Composable
fun BlurredArtworkBackdrop(
    artworkUrl: String?,
    modifier: Modifier = Modifier,
    tintColor: Color? = null,
    scrimAlpha: Float = 0.72f,
    blurRadius: Dp = 48.dp,
    fallbackColor: Color = IvidsColorTokens.ObsidianStage
) {
    var extractedTint by remember(artworkUrl) { mutableStateOf<Color?>(null) }
    LaunchedEffect(artworkUrl) {
        extractedTint = if (!artworkUrl.isNullOrBlank()) {
            ColorExtractor.extractCardTintColor(artworkUrl)
        } else {
            null
        }
    }
    val effectiveTint = extractedTint ?: tintColor
    val canBlur = remember { isNativeRenderBlurSupported() }

    Box(modifier = modifier.background(fallbackColor)) {
        Crossfade(
            targetState = artworkUrl,
            animationSpec = tween(durationMillis = 450),
            label = "BlurredArtworkCrossfade"
        ) { currentUrl ->
            Box(modifier = Modifier.fillMaxSize().background(fallbackColor)) {
                if (!currentUrl.isNullOrBlank() && canBlur) {
                    AsyncImage(
                        model = currentUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(1.22f)
                            .blur(blurRadius)
                    )
                }

                if (effectiveTint != null) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    effectiveTint.copy(alpha = if (canBlur && !currentUrl.isNullOrBlank()) 0.30f else 0.46f),
                                    effectiveTint.copy(alpha = if (canBlur && !currentUrl.isNullOrBlank()) 0.14f else 0.22f),
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

/**
 * Renders a top-aligned blurred artwork wash that smoothly fades into [IvidsColorTokens.ObsidianStage]
 * behind Song, Album, Artist, and Playlist detail screens.
 */
@Composable
fun DetailHeaderArtworkWash(
    artworkUrl: String?,
    modifier: Modifier = Modifier
) {
    if (artworkUrl.isNullOrBlank()) return
    Box(modifier = modifier) {
        BlurredArtworkBackdrop(
            artworkUrl = artworkUrl,
            scrimAlpha = 0.56f,
            blurRadius = 56.dp,
            fallbackColor = IvidsColorTokens.ObsidianStage,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            IvidsColorTokens.ObsidianStage.copy(alpha = 0.55f),
                            IvidsColorTokens.ObsidianStage
                        )
                    )
                )
        )
    }
}
