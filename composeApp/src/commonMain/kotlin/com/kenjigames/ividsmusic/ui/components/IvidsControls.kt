package com.kenjigames.ividsmusic.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.graphicsLayer
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
            isHovered -> Color(0xFF3E3E4B)
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
    val borderColor by animateColorAsState(
        targetValue = if (isHovered && enabled) IvidsColorTokens.HoverBorder else Color.Transparent,
        animationSpec = tween(150, easing = FastOutSlowInEasing),
        label = "SwitchBorder"
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
        color = trackColor,
        border = BorderStroke(1.dp, borderColor)
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
                        isSelected && isHovered -> Color(0xFF424250)
                        isSelected -> Color(0xFF2D2D36)
                        isHovered -> Color(0xFF2E2E3A)
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
                val borderColor by animateColorAsState(
                    targetValue = when {
                        isHovered -> IvidsColorTokens.HoverBorder
                        isSelected -> IvidsColorTokens.HairlineBorder
                        else -> Color.Transparent
                    },
                    animationSpec = tween(140, easing = FastOutSlowInEasing),
                    label = "SegmentBorder"
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(bg)
                        .border(1.dp, borderColor, RoundedCornerShape(10.dp))
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
 * Tonal pill button communicating interactivity through surface luminance lift, border highlight,
 * hand cursor, and disabled alpha dimming while unifying [MutableInteractionSource] for hover/click.
 */
@Composable
fun IvidsTonalPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    baseColor: Color = Color(0xFF1D1D23),
    hoverColor: Color = IvidsColorTokens.GlassHover,
    baseBorderColor: Color = IvidsColorTokens.HairlineSubtle,
    hoverBorderColor: Color = IvidsColorTokens.HoverBorder,
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
    val resolvedHover = if (hoverColor == Color(0xFF282830)) {
        IvidsColorTokens.GlassHover
    } else {
        hoverColor
    }
    val resolvedHoverBorder = if (hoverBorderColor == Color.Transparent && baseBorderColor != Color.Transparent) {
        IvidsColorTokens.HoverBorder
    } else {
        hoverBorderColor
    }

    val bg by animateColorAsState(
        targetValue = if (enabled && isHovered) resolvedHover else resolvedBase,
        animationSpec = tween(150, easing = FastOutSlowInEasing),
        label = "TonalPillBg"
    )
    val border by animateColorAsState(
        targetValue = if (enabled && isHovered) resolvedHoverBorder else baseBorderColor,
        animationSpec = tween(150, easing = FastOutSlowInEasing),
        label = "TonalPillBorder"
    )

    Surface(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = CircleShape,
        color = bg,
        border = if (border != Color.Transparent) BorderStroke(1.dp, border) else null,
        modifier = modifier
            .graphicsLayer { alpha = if (enabled) 1f else 0.42f }
            .then(if (enabled) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
            .tvFocusable(enabled = enabled, shape = CircleShape)
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
 * Shared hover-reactive selectable list item for modal pickers (language selector and playlist
 * chooser), rendering high-contrast surface lift, accent border highlight, and hand cursor.
 */
@Composable
fun IvidsSelectableDialogItem(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val accent = LocalDynamicAccent.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val bg by animateColorAsState(
        targetValue = when {
            selected && isHovered -> accent.copy(alpha = 0.28f)
            selected -> accent.copy(alpha = 0.16f)
            isHovered -> IvidsColorTokens.GlassHover
            else -> IvidsColorTokens.RecessedTrough
        },
        animationSpec = tween(140, easing = FastOutSlowInEasing),
        label = "DialogOptionBg"
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            selected -> accent
            isHovered -> IvidsColorTokens.HoverBorder
            else -> IvidsColorTokens.HairlineSubtle
        },
        animationSpec = tween(140, easing = FastOutSlowInEasing),
        label = "DialogOptionBorder"
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(12.dp),
        color = bg,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor),
        modifier = modifier
            .fillMaxWidth()
            .pointerHoverIcon(PointerIcon.Hand)
            .tvFocusable(shape = RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
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
