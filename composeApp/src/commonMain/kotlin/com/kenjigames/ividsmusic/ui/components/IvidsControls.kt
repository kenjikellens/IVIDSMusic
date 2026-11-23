package com.kenjigames.ividsmusic.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.hypot
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kenjigames.ividsmusic.ui.theme.IvidsColorTokens
import com.kenjigames.ividsmusic.ui.theme.IvidsTextStyles
import com.kenjigames.ividsmusic.ui.theme.LocalDynamicAccent
import com.kenjigames.ividsmusic.ui.theme.LocalLayoutMetrics
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
                    color = IvidsColorTokens.ChalkInk,
                    topLeft = Offset(0f, barTop),
                    size = Size(fillWidth, barH),
                    cornerRadius = corner
                )
            }

            if (thumbPx > 0.4f) {
                val thumbX = fillWidth.coerceIn(thumbPx, (size.width - thumbPx).coerceAtLeast(thumbPx))
                val centerY = size.height / 2f
                drawCircle(
                    color = Color(0x33FFFFFF),
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
                    indication = ividsPressRipple(if (isSelected) IvidsHoverStyle.ON_LIGHT else IvidsHoverStyle.NEUTRAL)
                ) { onSelect(item) }
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
 * Semantic hover fill styles kept separate from [ividsHoverZoom]'s 0%-to-100% animation engine.
 */
enum class IvidsHoverStyle {
    NEUTRAL,
    ON_LIGHT,
    PRIMARY_ACCENT,
    DESTRUCTIVE
}

/**
 * Resolves a semantic [IvidsHoverStyle] to its translucent overlay tint color.
 */
fun resolveHoverColor(style: IvidsHoverStyle): Color = when (style) {
    IvidsHoverStyle.NEUTRAL -> IvidsColorTokens.HoverOverlay
    IvidsHoverStyle.ON_LIGHT -> Color(0x38000000)
    IvidsHoverStyle.PRIMARY_ACCENT -> IvidsColorTokens.PrimaryCyan.copy(alpha = 0.20f)
    IvidsHoverStyle.DESTRUCTIVE -> IvidsColorTokens.DangerCoral.copy(alpha = 0.11f)
}

/**
 * Resolves the pointer-originated press ripple color matching a semantic [IvidsHoverStyle].
 */
fun resolveRippleColor(style: IvidsHoverStyle): Color = when (style) {
    IvidsHoverStyle.NEUTRAL -> Color(0x40FFFFFF)
    IvidsHoverStyle.ON_LIGHT -> Color(0x40000000)
    IvidsHoverStyle.PRIMARY_ACCENT -> IvidsColorTokens.PrimaryCyan.copy(alpha = 0.30f)
    IvidsHoverStyle.DESTRUCTIVE -> IvidsColorTokens.DangerCoral.copy(alpha = 0.20f)
}

/**
 * Returns an [IvidsPressRippleIndication] configured for semantic [style] that renders only
 * pointer-originated press ripples while ignoring hover and focus state overlays.
 */
fun ividsPressRipple(style: IvidsHoverStyle = IvidsHoverStyle.NEUTRAL): IndicationNodeFactory =
    IvidsPressRippleIndication(resolveRippleColor(style))

/**
 * Custom press-only [IndicationNodeFactory] that renders radial click ripples from pointer
 * coordinates without Material 3's built-in non-zooming hover tint overlay.
 */
class IvidsPressRippleIndication(private val color: Color) : IndicationNodeFactory {
    /** Creates the [PressRippleNode] observing [interactionSource] press events. */
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        PressRippleNode(interactionSource, color)

    override fun hashCode(): Int = color.hashCode()

    override fun equals(other: Any?): Boolean =
        other is IvidsPressRippleIndication && other.color == color
}

private data class ActiveRippleWave(
    val press: PressInteraction.Press,
    val radius: Animatable<Float, AnimationVector1D>,
    val alpha: Animatable<Float, AnimationVector1D>
)

private class PressRippleNode(
    private val interactionSource: InteractionSource,
    private val color: Color
) : Modifier.Node(), DrawModifierNode {
    private val activeWaves = mutableStateListOf<ActiveRippleWave>()

    /** Subscribes to press events and launches expanding radial ripple animations on tap/click. */
    override fun onAttach() {
        coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> {
                        val wave = ActiveRippleWave(
                            press = interaction,
                            radius = Animatable(0f),
                            alpha = Animatable(1f)
                        )
                        activeWaves.add(wave)
                        launch {
                            wave.radius.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                            )
                        }
                    }
                    is PressInteraction.Release, is PressInteraction.Cancel -> {
                        val press = when (interaction) {
                            is PressInteraction.Release -> interaction.press
                            is PressInteraction.Cancel -> interaction.press
                            else -> null
                        }
                        val wave = activeWaves.lastOrNull { it.press == press }
                        if (wave != null) {
                            launch {
                                if (wave.radius.value < 0.85f) {
                                    wave.radius.animateTo(
                                        targetValue = 1f,
                                        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing)
                                    )
                                }
                                wave.alpha.animateTo(0f, tween(durationMillis = 160))
                                activeWaves.remove(wave)
                            }
                        }
                    }
                }
            }
        }
    }

    /** Clears active waves on node detach to avoid stale frame draws. */
    override fun onDetach() {
        activeWaves.clear()
    }

    /** Draws child content followed by clipped radial press waves expanding from click origin. */
    override fun ContentDrawScope.draw() {
        drawContent()
        if (activeWaves.isEmpty()) return
        clipRect {
            activeWaves.forEach { wave ->
                val maxR = maxOf(
                    hypot(wave.press.pressPosition.x, wave.press.pressPosition.y),
                    hypot(size.width - wave.press.pressPosition.x, wave.press.pressPosition.y),
                    hypot(wave.press.pressPosition.x, size.height - wave.press.pressPosition.y),
                    hypot(size.width - wave.press.pressPosition.x, size.height - wave.press.pressPosition.y)
                ).coerceAtLeast(1f)
                val alpha = wave.alpha.value.coerceIn(0f, 1f)
                if (alpha > 0.005f) {
                    drawCircle(
                        color = color.copy(alpha = color.alpha * alpha),
                        radius = maxR * wave.radius.value,
                        center = wave.press.pressPosition
                    )
                }
            }
        }
    }
}

/**
 * Draws a hover fill resolved from [style] (or optional [hoverColor] override) that zooms smoothly
 * outward from `0%` at the container's center to `100%` of [shape] without re-layouting children.
 */
fun Modifier.ividsHoverZoom(
    hovered: Boolean,
    shape: Shape = RoundedCornerShape(12.dp),
    style: IvidsHoverStyle = IvidsHoverStyle.NEUTRAL,
    hoverColor: Color? = null,
    enabled: Boolean = true
): Modifier = composed {
    val resolvedColor = hoverColor ?: resolveHoverColor(style)
    val progress by animateFloatAsState(
        targetValue = if (enabled && hovered) 1f else 0f,
        animationSpec = tween(durationMillis = 185, easing = FastOutSlowInEasing),
        label = "IvidsHoverZoom"
    )
    if (progress <= 0.005f) return@composed this
    val p = progress.coerceIn(0f, 1f)
    this.drawBehind {
        val outline = shape.createOutline(size, layoutDirection, this)
        val boundsPath = Path().apply { addOutline(outline) }
        clipPath(boundsPath) {
            scale(scaleX = p, scaleY = p, pivot = Offset(size.width / 2f, size.height / 2f)) {
                drawOutline(
                    outline = outline,
                    color = resolvedColor.copy(alpha = resolvedColor.alpha * p)
                )
            }
        }
    }
}

/**
 * Applies a tactile spring press-scale animation driven by [interactionSource] so touch taps on
 * cards, track rows, and buttons compress smoothly without intercepting gestures.
 */
fun Modifier.ividsTouchFeedback(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    pressedScale: Float = 0.965f
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (enabled && isPressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 900f),
        label = "IvidsTouchScale"
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
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
    baseBorderColor: Color = IvidsColorTokens.HairlineSubtle,
    hoverBorderColor: Color = IvidsColorTokens.HoverBorder,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

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
                onClick = onClick
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
                onClick = onClick
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
 * Unified spinning vinyl record loader rendered on Canvas across buffering play controls and
 * screen loading states with concentric grooves, rotating specular sheen, and signature center label.
 */
@Composable
fun IvidsVinylLoader(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    centerLabelColor: Color = IvidsColorTokens.PrimaryCyan,
    discColor: Color = Color(0xFF161616),
    grooveColor: Color = Color(0x30FFFFFF)
) {
    val transition = rememberInfiniteTransition(label = "VinylLoaderSpin")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1150, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "VinylRotation"
    )

    Canvas(modifier = modifier.size(size)) {
        val radius = this.size.minDimension / 2f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val grooveStroke = (radius * 0.055f).coerceAtLeast(1f)

        rotate(degrees = angle, pivot = center) {
            drawCircle(color = discColor, radius = radius, center = center)
            drawCircle(
                color = grooveColor,
                radius = radius - grooveStroke * 0.5f,
                center = center,
                style = Stroke(width = grooveStroke)
            )
            listOf(0.80f, 0.64f, 0.48f).forEach { factor ->
                drawCircle(
                    color = grooveColor.copy(alpha = 0.22f),
                    radius = radius * factor,
                    center = center,
                    style = Stroke(width = grooveStroke * 0.8f)
                )
            }
            val arcRadius = radius * 0.72f
            val arcTopLeft = Offset(center.x - arcRadius, center.y - arcRadius)
            val arcSize = Size(arcRadius * 2f, arcRadius * 2f)
            drawArc(
                color = IvidsColorTokens.ChalkInk.copy(alpha = 0.35f),
                startAngle = 15f,
                sweepAngle = 42f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = grooveStroke * 1.4f, cap = StrokeCap.Round)
            )
            drawArc(
                color = IvidsColorTokens.ChalkInk.copy(alpha = 0.35f),
                startAngle = 195f,
                sweepAngle = 42f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = grooveStroke * 1.4f, cap = StrokeCap.Round)
            )
            drawCircle(color = centerLabelColor, radius = radius * 0.33f, center = center)
            drawCircle(color = discColor, radius = (radius * 0.11f).coerceAtLeast(1.5f), center = center)
        }
    }
}

/**
 * Centered full-width loading state displaying [IvidsVinylLoader] with an optional localized label.
 */
@Composable
fun IvidsVinylLoadingBox(
    label: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        IvidsVinylLoader(size = 54.dp)
        if (!label.isNullOrBlank()) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = IvidsColorTokens.SlateMuted
            )
        }
    }
}
