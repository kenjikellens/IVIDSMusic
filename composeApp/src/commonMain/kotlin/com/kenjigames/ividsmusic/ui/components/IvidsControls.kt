package com.kenjigames.ividsmusic.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kenjigames.ividsmusic.ui.theme.IvidsColorTokens
import com.kenjigames.ividsmusic.ui.theme.IvidsTextStyles
import com.kenjigames.ividsmusic.ui.theme.LocalDynamicAccent
import com.kenjigames.ividsmusic.ui.theme.tvFocusable

/**
 * Custom antialiased hi-fi scrubber and volume bar replacing default Material 3 Sliders with a
 * sleek artwork-tinted track that thickens smoothly and reveals a crisp circular thumb on hover/drag.
 */
@Composable
fun IvidsScrubber(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onValueChangeFinished: () -> Unit = {},
    trackHeight: Dp = 4.dp,
    alwaysShowThumb: Boolean = false,
    enabled: Boolean = true
) {
    val accent = LocalDynamicAccent.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    var isDragging by remember { mutableStateOf(false) }
    var widthPx by remember { mutableStateOf(1f) }

    val latestOnValueChange by rememberUpdatedState(onValueChange)
    val latestOnFinished by rememberUpdatedState(onValueChangeFinished)
    val latestRange by rememberUpdatedState(valueRange)

    val span = (valueRange.endInclusive - valueRange.start).coerceAtLeast(1e-5f)
    val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)
    val activeInteraction = enabled && (isHovered || isDragging || alwaysShowThumb)

    val animatedTrackHeight by animateDpAsState(
        targetValue = if (enabled && (isHovered || isDragging)) trackHeight + 2.dp else trackHeight,
        animationSpec = tween(140, easing = FastOutSlowInEasing),
        label = "ScrubberTrackHeight"
    )
    val animatedThumbRadius by animateDpAsState(
        targetValue = if (activeInteraction) 6.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMedium),
        label = "ScrubberThumbRadius"
    )

    /** Maps a horizontal pointer coordinate to the clamped value range and notifies caller. */
    fun dispatchFraction(xPx: Float) {
        val ratio = (xPx / widthPx.coerceAtLeast(1f)).coerceIn(0f, 1f)
        val range = latestRange
        val nextValue = range.start + ratio * (range.endInclusive - range.start)
        latestOnValueChange(nextValue)
    }

    Box(
        modifier = modifier
            .height(24.dp)
            .then(if (enabled) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
            .hoverable(interactionSource, enabled = enabled)
            .onSizeChanged { size -> widthPx = size.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    dispatchFraction(offset.x)
                    latestOnFinished()
                }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        dispatchFraction(offset.x)
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        dispatchFraction(change.position.x)
                    },
                    onDragEnd = {
                        isDragging = false
                        latestOnFinished()
                    },
                    onDragCancel = {
                        isDragging = false
                        latestOnFinished()
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(24.dp)) {
            val barH = animatedTrackHeight.toPx()
            val barTop = (size.height - barH) / 2f
            val corner = CornerRadius(barH / 2f, barH / 2f)
            val thumbPx = animatedThumbRadius.toPx()
            val fillWidth = (size.width * fraction).coerceIn(0f, size.width)

            drawRoundRect(
                color = Color(0x28FFFFFF),
                topLeft = Offset(0f, barTop),
                size = Size(size.width, barH),
                cornerRadius = corner
            )

            if (fillWidth > 0.5f) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(accent.copy(alpha = 0.72f), accent)
                    ),
                    topLeft = Offset(0f, barTop),
                    size = Size(fillWidth, barH),
                    cornerRadius = corner
                )
            }

            if (thumbPx > 0.4f) {
                val thumbX = fillWidth.coerceIn(thumbPx, (size.width - thumbPx).coerceAtLeast(thumbPx))
                val centerY = size.height / 2f
                drawCircle(
                    color = accent.copy(alpha = 0.28f),
                    radius = thumbPx * 1.55f,
                    center = Offset(thumbX, centerY)
                )
                drawCircle(
                    color = IvidsColorTokens.ChalkInk,
                    radius = thumbPx,
                    center = Offset(thumbX, centerY)
                )
            }
        }
    }
}

/**
 * Borderless hi-fi pill toggle with crisp white thumb, signature IVIDS cyan active track,
 * and clean tonal hover brightening without outline noise.
 */
@Composable
fun IvidsSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val trackColor by animateColorAsState(
        targetValue = when {
            checked -> IvidsColorTokens.PrimaryCyan
            isHovered -> Color(0xFF32323B)
            else -> Color(0xFF23232A)
        },
        animationSpec = tween(150, easing = FastOutSlowInEasing),
        label = "SwitchTrack"
    )
    val thumbColor by animateColorAsState(
        targetValue = if (checked || isHovered) Color.White else IvidsColorTokens.SlateMuted,
        animationSpec = tween(150, easing = FastOutSlowInEasing),
        label = "SwitchThumb"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium),
        label = "SwitchThumbOffset"
    )

    Surface(
        modifier = modifier
            .size(width = 48.dp, height = 28.dp)
            .pointerHoverIcon(PointerIcon.Hand)
            .hoverable(interactionSource, enabled = enabled)
            .tvFocusable(shape = CircleShape)
            .clip(CircleShape)
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
        shape = CircleShape,
        color = trackColor
    ) {
        Box(
            modifier = Modifier.padding(3.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .size(22.dp)
                    .shadow(elevation = 2.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(thumbColor)
            )
        }
    }
}

/**
 * Borderless recessed pill trough hosting selectable option pills with crisp tonal surface
 * contrast on hover and selection, avoiding wireframe borders.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> IvidsSegmentedBar(
    items: List<T>,
    selected: T,
    labelProvider: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    tabular: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = IvidsColorTokens.RecessedTrough
    ) {
        FlowRow(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items.forEach { item ->
                val isSelected = item == selected
                val interactionSource = remember { MutableInteractionSource() }
                val isHovered by interactionSource.collectIsHoveredAsState()

                val bg by animateColorAsState(
                    targetValue = when {
                        isSelected && isHovered -> Color(0xFF383842)
                        isSelected -> Color(0xFF2D2D36)
                        isHovered -> Color(0xFF222229)
                        else -> Color.Transparent
                    },
                    animationSpec = tween(140, easing = FastOutSlowInEasing),
                    label = "SegmentBg"
                )
                val fg by animateColorAsState(
                    targetValue = when {
                        isSelected || isHovered -> IvidsColorTokens.ChalkInk
                        else -> IvidsColorTokens.SlateMuted
                    },
                    animationSpec = tween(140, easing = FastOutSlowInEasing),
                    label = "SegmentFg"
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(bg)
                        .pointerHoverIcon(PointerIcon.Hand)
                        .hoverable(interactionSource)
                        .tvFocusable(shape = RoundedCornerShape(10.dp))
                        .clickable { onSelect(item) }
                        .padding(horizontal = 13.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = labelProvider(item),
                        style = if (tabular) {
                            IvidsTextStyles.tabularBadge()
                        } else {
                            MaterialTheme.typography.labelLarge
                        },
                        color = fg
                    )
                }
            }
        }
    }
}

/**
 * Borderless tonal pill button communicating interactivity purely through smooth surface
 * luminance lift (`150ms`) and desktop hand cursor without boxy outlines or scaling.
 */
@Composable
fun IvidsTonalPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    baseColor: Color = Color(0xFF1D1D23),
    hoverColor: Color = Color(0xFF2F2F38),
    baseBorderColor: Color = Color.Transparent,
    hoverBorderColor: Color = Color.Transparent,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val resolvedBase = if (baseColor == IvidsColorTokens.RecessedTrough || baseColor == IvidsColorTokens.GlassSurface) {
        Color(0xFF1E1E24)
    } else {
        baseColor
    }
    val resolvedHover = if (hoverColor == IvidsColorTokens.GlassHover) {
        Color(0xFF303039)
    } else {
        hoverColor
    }

    val bg by animateColorAsState(
        targetValue = if (enabled && isHovered) resolvedHover else resolvedBase,
        animationSpec = tween(150, easing = FastOutSlowInEasing),
        label = "TonalPillBg"
    )

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = bg,
        modifier = modifier
            .then(if (enabled) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
            .hoverable(interactionSource, enabled = enabled)
            .tvFocusable(shape = CircleShape)
    ) {
        Row(
            modifier = Modifier.padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

/**
 * Centers page content cleanly inside [maxWidth] on widescreen monitors so settings, downloader,
 * profile, and discovery pages preserve ergonomic reading measure without ultrawide stretching.
 */
@Composable
fun FramedScreenContainer(
    modifier: Modifier = Modifier,
    maxWidth: Dp = 880.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier.widthIn(max = maxWidth).fillMaxWidth(),
            content = content
        )
    }
}
