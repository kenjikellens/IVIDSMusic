package com.kenjigames.ividsmusic.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.unit.dp
import com.kenjigames.ividsmusic.ui.theme.IvidsColorTokens
import kotlin.math.hypot
import kotlinx.coroutines.launch

/**
 * Semantic hover and ripple color roles decoupled from the center-zoom hover animation.
 */
enum class IvidsHoverStyle {
    NEUTRAL,
    ON_LIGHT,
    PRIMARY_ACCENT,
    DESTRUCTIVE
}

/**
 * Resolves a semantic [IvidsHoverStyle] to its translucent hover overlay tint color.
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
 * pointer-originated press ripples without Material's secondary hover overlay.
 */
fun ividsPressRipple(style: IvidsHoverStyle = IvidsHoverStyle.NEUTRAL): IndicationNodeFactory =
    IvidsPressRippleIndication(resolveRippleColor(style))

/**
 * Press-only [IndicationNodeFactory] that renders radial click ripples from pointer coordinates
 * while leaving hover visuals solely to [ividsHoverZoom].
 */
class IvidsPressRippleIndication(private val color: Color) : IndicationNodeFactory {
    /** Creates the [PressRippleNode] observing press interactions on [interactionSource]. */
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        PressRippleNode(interactionSource, color)

    /** Returns the hash code of the ripple [color] so Compose reuses identical indications. */
    override fun hashCode(): Int = color.hashCode()

    /** Compares indications strictly by ripple [color] equality. */
    override fun equals(other: Any?): Boolean =
        other is IvidsPressRippleIndication && other.color == color
}

/** Holds per-press expanding wave radius and fade-out alpha animations for [PressRippleNode]. */
private data class ActiveRippleWave(
    val press: PressInteraction.Press,
    val radius: Animatable<Float, AnimationVector1D>,
    val alpha: Animatable<Float, AnimationVector1D>
)

/** Modifier node tracking active [PressInteraction] waves and drawing clipped radial ripples. */
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
