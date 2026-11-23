package com.kenjigames.ividsmusic.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Holds responsive viewport spacing and card sizing metrics derived from the active [ShellMode],
 * keeping desktop/TV proportions intact while compacting margins and enabling touch affordances on phones.
 */
@Immutable
data class IvidsLayoutMetrics(
    val isCompactMobile: Boolean = false,
    val screenHorizontalPadding: Dp = 24.dp,
    val contentBottomClearance: Dp = 104.dp,
    val cardWidth: Dp = 176.dp,
    val showTouchPlayFab: Boolean = false
)

val LocalLayoutMetrics = staticCompositionLocalOf { IvidsLayoutMetrics() }

/**
 * Returns screen scroll content padding adapted to the current [LocalLayoutMetrics] so compact
 * mobile viewports gain tighter side gutters and sufficient clearance above MiniPlayerBar and nav bars.
 */
@Composable
fun ividsScreenPadding(top: Dp = 22.dp): PaddingValues {
    val metrics = LocalLayoutMetrics.current
    val resolvedTop = if (metrics.isCompactMobile && top > 16.dp) 16.dp else top
    return PaddingValues(
        start = metrics.screenHorizontalPadding,
        end = metrics.screenHorizontalPadding,
        top = resolvedTop,
        bottom = metrics.contentBottomClearance
    )
}

/**
 * Applies IVIDS Lumina's layered dark theme, 450ms animated album-art accent color, RTL/LTR layout
 * direction, TV mode flag, and user UI scale factor (keeping 100% 1:1 native crispness on HiDPI).
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
    val effectiveDensity = if (abs(clampedScale - 1.0f) > 0.01f) {
        Density(
            density = baseDensity.density * clampedScale,
            fontScale = baseDensity.fontScale
        )
    } else {
        baseDensity
    }

    val colorScheme = darkColorScheme(
        primary = animatedAccent,
        onPrimary = IvidsColorTokens.OnAccentDark,
        secondary = IvidsColorTokens.OceanAccent,
        onSecondary = IvidsColorTokens.ChalkInk,
        tertiary = IvidsColorTokens.CyanHighlight,
        background = IvidsColorTokens.ObsidianStage,
        onBackground = IvidsColorTokens.ChalkInk,
        surface = IvidsColorTokens.GlassSurface,
        onSurface = IvidsColorTokens.ChalkInk,
        surfaceVariant = IvidsColorTokens.GlassHover,
        onSurfaceVariant = IvidsColorTokens.SlateMuted,
        outline = IvidsColorTokens.HairlineSubtle,
        error = IvidsColorTokens.DangerCoral,
        onError = IvidsColorTokens.ChalkInk
    )

    CompositionLocalProvider(
        LocalIvidsColors provides IvidsExtendedColors(primaryCyan = animatedAccent),
        LocalDynamicAccent provides animatedAccent,
        LocalUiScale provides clampedScale,
        LocalIsTvMode provides isTvMode,
        LocalLayoutDirection provides layoutDirection,
        LocalDensity provides effectiveDensity
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = rememberIvidsTypography(),
            content = content
        )
    }
}
