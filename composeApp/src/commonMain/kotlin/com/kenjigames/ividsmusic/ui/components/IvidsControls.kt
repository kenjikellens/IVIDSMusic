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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kenjigames.ividsmusic.domain.model.TrackSortOption
import com.kenjigames.ividsmusic.ui.i18n.AppStrings
import com.kenjigames.ividsmusic.ui.theme.IvidsColorTokens
import com.kenjigames.ividsmusic.ui.theme.IvidsTextStyles
import com.kenjigames.ividsmusic.ui.theme.LocalLayoutMetrics
import com.kenjigames.ividsmusic.ui.theme.tvFocusable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Custom antialiased hi-fi scrubber and volume bar replacing default Material 3 Sliders with a
 * sleek track that thickens smoothly and reveals a crisp circular thumb on hover/drag.
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
            val activeAccent = if (enabled && (isHovered || isDragging)) {
                IvidsColorTokens.PrimaryCyan
            } else {
                IvidsColorTokens.ChalkInk
            }

            drawRoundRect(
                color = Color(0x28FFFFFF),
                topLeft = Offset(0f, barTop),
                size = Size(size.width, barH),
                cornerRadius = corner
            )

            if (fillWidth > 0.5f) {
                drawRoundRect(
                    color = activeAccent,
                    topLeft = Offset(0f, barTop),
                    size = Size(fillWidth, barH),
                    cornerRadius = corner
                )
            }

            if (thumbPx > 0.4f) {
                val thumbX = fillWidth.coerceIn(thumbPx, (size.width - thumbPx).coerceAtLeast(thumbPx))
                val centerY = size.height / 2f
                drawCircle(
                    color = if (isHovered || isDragging) {
                        IvidsColorTokens.PrimaryCyan.copy(alpha = 0.28f)
                    } else {
                        Color(0x33FFFFFF)
                    },
                    radius = thumbPx * 1.55f,
                    center = Offset(thumbX, centerY)
                )
                drawCircle(
                    color = activeAccent,
                    radius = thumbPx,
                    center = Offset(thumbX, centerY)
                )
            }
        }
    }
}

/**
 * Borderless hi-fi pill toggle with crisp white checked track, neutral dark off-track,
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
            checked -> IvidsColorTokens.ChalkInk
            isHovered -> Color(0xFF424242)
            else -> Color(0xFF2B2B2B)
        },
        animationSpec = tween(150, easing = FastOutSlowInEasing),
        label = "SwitchTrack"
    )
    val thumbColor by animateColorAsState(
        targetValue = when {
            checked -> IvidsColorTokens.ObsidianStage
            isHovered -> Color.White
            else -> IvidsColorTokens.SlateMuted
        },
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
 * Direct borderless YouTube/Spotify filter-chip strip without an outer wrapper box, keeping the UI
 * flat on black with high-contrast white active chips and neutral dark-gray inactive chips.
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
    val isCompactMobile = LocalLayoutMetrics.current.isCompactMobile

    /** Renders an individual borderless filter chip for [item] with white active contrast. */
    @Composable
    fun SegmentPill(item: T) {
        val isSelected = item == selected
        val interactionSource = remember { MutableInteractionSource() }
        val isHovered by interactionSource.collectIsHoveredAsState()

        val baseBg = when {
            isSelected -> IvidsColorTokens.ChalkInk
            else -> Color(0xFF1D1D1D)
        }
        val fg by animateColorAsState(
            targetValue = when {
                isSelected -> IvidsColorTokens.ObsidianStage
                else -> IvidsColorTokens.ChalkInk
            },
            animationSpec = tween(140, easing = FastOutSlowInEasing),
            label = "SegmentFg"
        )
        val delayedSelect = rememberDelayedOnClick(delayMs = 110L) { onSelect(item) }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(baseBg)
                .ividsHoverZoom(
                    hovered = !isSelected && isHovered,
                    shape = RoundedCornerShape(8.dp),
                    hoverColor = Color(0x28FFFFFF)
                )
                .ividsTouchFeedback(interactionSource = interactionSource, pressedScale = 0.95f)
                .pointerHoverIcon(PointerIcon.Hand)
                .hoverable(interactionSource)
                .tvFocusable(shape = RoundedCornerShape(8.dp))
                .clickable(
                    interactionSource = interactionSource,
                    indication = ividsPressRipple(if (isSelected) IvidsHoverStyle.ON_LIGHT else IvidsHoverStyle.NEUTRAL),
                    onClick = delayedSelect
                )
                .padding(horizontal = if (isCompactMobile) 14.dp else 15.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = labelProvider(item),
                style = if (tabular) {
                    IvidsTextStyles.tabularBadge()
                } else {
                    MaterialTheme.typography.labelLarge
                },
                color = fg,
                maxLines = 1,
                softWrap = false
            )
        }
    }

    if (isCompactMobile) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item -> SegmentPill(item) }
        }
    } else {
        FlowRow(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEach { item -> SegmentPill(item) }
        }
    }
}

/**
 * Tonal pill button pairing a base fill with semantic [hoverStyle] (or explicit [hoverColor]) and
 * the shared `0% -> 100%` [ividsHoverZoom] animation plus hand cursor and TV focus handling.
 */
@Composable
fun IvidsTonalPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    baseColor: Color = Color(0xFF1D1D1D),
    hoverColor: Color = IvidsColorTokens.GlassHover,
    hoverStyle: IvidsHoverStyle = IvidsHoverStyle.NEUTRAL,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val delayedClick = rememberDelayedOnClick(delayMs = 130L, onClick = onClick)

    val resolvedBase = if (baseColor == IvidsColorTokens.RecessedTrough || baseColor == IvidsColorTokens.GlassSurface) {
        Color(0xFF1D1D1D)
    } else {
        baseColor
    }
    val customOverlay = hoverColor.takeUnless {
        it == IvidsColorTokens.GlassHover || it == Color(0xFF282830)
    }

    Surface(
        shape = CircleShape,
        color = resolvedBase,
        modifier = modifier
            .clip(CircleShape)
            .ividsTouchFeedback(interactionSource = interactionSource, enabled = enabled)
            .graphicsLayer { alpha = if (enabled) 1f else 0.42f }
            .then(if (enabled) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
            .hoverable(interactionSource, enabled = enabled)
            .tvFocusable(enabled = enabled, shape = CircleShape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ividsPressRipple(hoverStyle),
                onClick = delayedClick
            )
    ) {
        Row(
            modifier = Modifier
                .ividsHoverZoom(
                    hovered = isHovered,
                    shape = CircleShape,
                    style = hoverStyle,
                    hoverColor = customOverlay,
                    enabled = enabled
                )
                .padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

/**
 * High-contrast primary pill button (e.g. Start Listening) delegating directly to [IvidsTonalPill]
 * with darker [IvidsHoverStyle.ON_LIGHT] hover zoom on white surfaces.
 */
@Composable
fun IvidsPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isDestructive: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 11.dp),
    content: @Composable RowScope.() -> Unit
) {
    val bg = if (isDestructive) IvidsColorTokens.DangerCoral else IvidsColorTokens.ChalkInk
    val fg = if (isDestructive) IvidsColorTokens.ChalkInk else IvidsColorTokens.ObsidianStage
    IvidsTonalPill(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        baseColor = bg,
        hoverStyle = IvidsHoverStyle.ON_LIGHT,
        contentPadding = contentPadding
    ) {
        CompositionLocalProvider(LocalContentColor provides fg) {
            content()
        }
    }
}

/**
 * Unified circular white Play/Pause button used across MiniPlayerBar, DesktopPlayerDeck, and
 * NowPlayingSheet with darker `0% -> 100%` hover zoom, dark ripple, and [IvidsVinylLoader] buffering.
 */
@Composable
fun IvidsPlayCircleButton(
    isPlaying: Boolean,
    isBuffering: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    iconSize: Dp = 24.dp,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Surface(
        shape = CircleShape,
        color = IvidsColorTokens.ChalkInk,
        shadowElevation = 6.dp,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .ividsTouchFeedback(interactionSource = interactionSource, enabled = enabled, pressedScale = 0.92f)
            .graphicsLayer { alpha = if (enabled) 1f else 0.42f }
            .then(if (enabled) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
            .hoverable(interactionSource, enabled = enabled)
            .tvFocusable(enabled = enabled, shape = CircleShape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ividsPressRipple(IvidsHoverStyle.ON_LIGHT),
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier.ividsHoverZoom(
                hovered = isHovered,
                shape = CircleShape,
                style = IvidsHoverStyle.ON_LIGHT,
                enabled = enabled
            ),
            contentAlignment = Alignment.Center
        ) {
            if (isBuffering) {
                IvidsVinylLoader(
                    size = (size * 0.52f).coerceAtLeast(18.dp),
                    centerLabelColor = IvidsColorTokens.ObsidianStage,
                    discColor = IvidsColorTokens.RecessedTrough
                )
            } else {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = contentDescription,
                    tint = IvidsColorTokens.ObsidianStage,
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    }
}

/**
 * Circular borderless icon button reusing [ividsHoverZoom] and [ividsTouchFeedback] with
 * configurable [hoverStyle] for dismiss (`X`) and toolbar icon controls.
 */
@Composable
fun IvidsIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    enabled: Boolean = true,
    hoverStyle: IvidsHoverStyle = IvidsHoverStyle.NEUTRAL,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val delayedClick = rememberDelayedOnClick(delayMs = 130L, onClick = onClick)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .ividsHoverZoom(hovered = isHovered, shape = CircleShape, style = hoverStyle, enabled = enabled)
            .ividsTouchFeedback(interactionSource = interactionSource, enabled = enabled, pressedScale = 0.92f)
            .graphicsLayer { alpha = if (enabled) 1f else 0.42f }
            .then(if (enabled) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
            .hoverable(interactionSource, enabled = enabled)
            .tvFocusable(enabled = enabled, shape = CircleShape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ividsPressRipple(hoverStyle),
                onClick = delayedClick
            ),
        contentAlignment = Alignment.Center,
        content = content
    )
}

/**
 * Shared hover-reactive selectable list item for modal pickers (language selector and playlist
 * chooser), rendering press feedback and a brief selection flash before closing the dialog.
 */
@Composable
fun IvidsSelectableDialogItem(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    commitDelayMs: Long = 90L,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val scope = rememberCoroutineScope()
    var pendingSelection by remember { mutableStateOf(false) }
    val visualSelected = selected || pendingSelection

    val bg by animateColorAsState(
        targetValue = if (visualSelected) Color(0xFF262626) else Color.Transparent,
        animationSpec = tween(120, easing = FastOutSlowInEasing),
        label = "DialogOptionBg"
    )

    Surface(
        onClick = {
            if (pendingSelection) return@Surface
            if (commitDelayMs > 0L) {
                pendingSelection = true
                scope.launch {
                    delay(commitDelayMs)
                    pendingSelection = false
                    onClick()
                }
            } else {
                onClick()
            }
        },
        interactionSource = interactionSource,
        shape = RoundedCornerShape(12.dp),
        color = bg,
        modifier = modifier
            .fillMaxWidth()
            .ividsTouchFeedback(interactionSource = interactionSource, pressedScale = 0.975f)
            .pointerHoverIcon(PointerIcon.Hand)
            .tvFocusable(shape = RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .ividsHoverZoom(hovered = isHovered, shape = RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
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

/**
 * Formats a non-negative byte count into a human-readable `B`, `KB`, `MB`, or `GB` label.
 */
fun formatByteSize(bytes: Long): String {
    val safe = bytes.coerceAtLeast(0L)
    return when {
        safe < 1024L -> "$safe B"
        safe < 1024L * 1024L -> "${safe / 1024L} KB"
        safe < 1024L * 1024L * 1024L -> {
            val tenths = (safe * 10L / (1024L * 1024L))
            "${tenths / 10L}.${tenths % 10L} MB"
        }
        else -> {
            val hundredths = (safe * 100L / (1024L * 1024L * 1024L))
            "${hundredths / 100L}.${(hundredths % 100L).toString().padStart(2, '0')} GB"
        }
    }
}

/**
 * Formats a cumulative duration in seconds into compact Spotify-style `Xh Ym` or `Xm Ys` text.
 */
fun formatPlaylistTotalDuration(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val hours = safe / 3600
    val minutes = (safe % 3600) / 60
    val seconds = safe % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}

/**
 * Returns standard dark-theme [TextFieldColors] for search and URL input fields across screens.
 * Eliminates duplicated `OutlinedTextFieldDefaults.colors(...)` blocks while preserving focus borders.
 */
@Composable
fun ividsSearchFieldColors(
    unfocusedContainerColor: Color = IvidsColorTokens.RecessedTrough
): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = IvidsColorTokens.GlassSurface,
    unfocusedContainerColor = unfocusedContainerColor,
    focusedBorderColor = IvidsColorTokens.HairlineBorder,
    unfocusedBorderColor = Color.Transparent,
    focusedTextColor = IvidsColorTokens.ChalkInk,
    unfocusedTextColor = IvidsColorTokens.ChalkInk
)

/**
 * Resolves the localized display label for a [TrackSortOption] inside sort menus and controls.
 */
private fun resolveSortLabel(option: TrackSortOption, strings: AppStrings): String = when (option) {
    TrackSortOption.RECENT -> strings["sort_recent"]
    TrackSortOption.TITLE -> strings["sort_title"]
    TrackSortOption.ARTIST -> strings["sort_artist"]
    TrackSortOption.DURATION -> strings["sort_duration"]
}

/**
 * Renders a compact single-line track control bar with a sort dropdown menu, `Offline only`
 * filter toggle, and optional trailing playback actions for [LibraryScreen] and [PlaylistDetailScreen].
 */
@Composable
fun TrackSortFilterRow(
    selectedSort: TrackSortOption,
    offlineOnly: Boolean,
    onSelectSort: (TrackSortOption) -> Unit,
    onToggleOfflineOnly: () -> Unit,
    strings: AppStrings,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable RowScope.() -> Unit)? = null
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box {
            IvidsTonalPill(
                onClick = { sortMenuExpanded = true },
                baseColor = IvidsColorTokens.GlassSurface,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.SwapVert,
                    contentDescription = strings["sort_by"],
                    tint = IvidsColorTokens.SlateMuted,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = resolveSortLabel(selectedSort, strings),
                    style = MaterialTheme.typography.labelMedium,
                    color = IvidsColorTokens.ChalkInk,
                    maxLines = 1,
                    softWrap = false
                )
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = IvidsColorTokens.SlateMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
            DropdownMenu(
                expanded = sortMenuExpanded,
                onDismissRequest = { sortMenuExpanded = false },
                containerColor = IvidsColorTokens.GlassSurface
            ) {
                TrackSortOption.entries.forEach { opt ->
                    val isSelected = selectedSort == opt
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = resolveSortLabel(opt, strings),
                                color = if (isSelected) IvidsColorTokens.PrimaryCyan else IvidsColorTokens.ChalkInk
                            )
                        },
                        trailingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = IvidsColorTokens.PrimaryCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else {
                            null
                        },
                        onClick = {
                            sortMenuExpanded = false
                            onSelectSort(opt)
                        }
                    )
                }
            }
        }

        IvidsTonalPill(
            onClick = onToggleOfflineOnly,
            baseColor = if (offlineOnly) IvidsColorTokens.PrimaryCyan.copy(alpha = 0.16f) else IvidsColorTokens.GlassSurface,
            hoverStyle = if (offlineOnly) IvidsHoverStyle.PRIMARY_ACCENT else IvidsHoverStyle.NEUTRAL,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.DownloadDone,
                contentDescription = null,
                tint = if (offlineOnly) IvidsColorTokens.PrimaryCyan else IvidsColorTokens.SlateMuted,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = strings["offline_only"],
                style = MaterialTheme.typography.labelMedium,
                color = if (offlineOnly) IvidsColorTokens.PrimaryCyan else IvidsColorTokens.ChalkInk,
                maxLines = 1,
                softWrap = false
            )
        }

        if (trailingContent != null) {
            Spacer(modifier = Modifier.weight(1f))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                content = trailingContent
            )
        }
    }
}

/**
 * Renders the desktop Spotify-style track table column header (`#`, Title, Duration icon) with
 * configurable [trailingWidth] above a hairline divider.
 */
@Composable
fun TrackTableHeader(
    strings: AppStrings,
    trailingWidth: Dp = 84.dp,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "#",
                style = IvidsTextStyles.tabularBadge(),
                color = IvidsColorTokens.SlateMuted,
                modifier = Modifier.width(26.dp)
            )
            Text(
                text = strings["title_column"],
                style = MaterialTheme.typography.labelMedium,
                color = IvidsColorTokens.SlateMuted,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Rounded.Schedule,
                contentDescription = strings["duration_column"],
                tint = IvidsColorTokens.SlateMuted,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(trailingWidth))
        }
        HorizontalDivider(color = IvidsColorTokens.HairlineSubtle)
    }
}
