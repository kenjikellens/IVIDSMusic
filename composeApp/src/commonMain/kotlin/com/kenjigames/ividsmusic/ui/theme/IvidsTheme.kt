package com.kenjigames.ividsmusic.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * Applies IVIDS Music's Obsidian & Cyan dark theme, 450ms animated album-art accent color,
 * RTL/LTR layout direction, TV mode flag, and user UI scale factor (75%..150%).
 */
@Composable
fun IvidsTheme(
    uiScale: Float = 1.0f,
    dynamicAccent: Color = IvidsColorTokens.PrimaryCyan,
    dynamicTintEnabled: Boolean = true,
    isTvMode: Boolean = false,
    layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    content: @Composable () -> Unit
) {
    val targetAccent = if (dynamicTintEnabled) dynamicAccent else IvidsColorTokens.PrimaryCyan
    val animatedAccent by animateColorAsState(
        targetValue = targetAccent,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "DynamicAlbumAccent"
    )
    val clampedScale = uiScale.coerceIn(0.75f, 1.50f)
    val baseDensity = LocalDensity.current
    val scaledDensity = Density(
        density = baseDensity.density * clampedScale,
        fontScale = baseDensity.fontScale
    )

    val colorScheme = darkColorScheme(
        primary = animatedAccent,
        onPrimary = IvidsColorTokens.OnAccentDark,
        secondary = IvidsColorTokens.OceanAccent,
        onSecondary = IvidsColorTokens.ChalkInk,
        tertiary = IvidsColorTokens.CyanHighlight,
        background = IvidsColorTokens.ObsidianStage,
        onBackground = IvidsColorTokens.ChalkInk,
        surface = IvidsColorTokens.VinylSurface,
        onSurface = IvidsColorTokens.ChalkInk,
        surfaceVariant = IvidsColorTokens.GrooveElevated,
        onSurfaceVariant = IvidsColorTokens.SlateMuted,
        outline = IvidsColorTokens.HairlineBorder,
        error = IvidsColorTokens.DangerCoral,
        onError = IvidsColorTokens.ChalkInk
    )

    CompositionLocalProvider(
        LocalIvidsColors provides IvidsExtendedColors(primaryCyan = animatedAccent),
        LocalDynamicAccent provides animatedAccent,
        LocalUiScale provides clampedScale,
        LocalIsTvMode provides isTvMode,
        LocalLayoutDirection provides layoutDirection,
        LocalDensity provides scaledDensity
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = rememberIvidsTypography(),
            content = content
        )
    }
}
