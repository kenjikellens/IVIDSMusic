package com.kenjigames.ividsmusic.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Adds D-pad / keyboard focus handling with a crisp 2dp PrimaryCyan focus border, subtle
 * OceanAccent elevation glow, and gentle focus scale when running in TV mode or keyboard nav.
 */
fun Modifier.tvFocusable(
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(14.dp),
    borderWidth: Dp = 2.dp,
    scaleOnFocus: Float = 1.03f,
    onFocused: (() -> Unit)? = null
): Modifier = composed {
    val isTv = LocalIsTvMode.current
    val accent = LocalDynamicAccent.current
    var isFocused by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isFocused && isTv) scaleOnFocus else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "TvFocusScale"
    )
    val borderColor = when {
        isFocused && isTv -> IvidsColorTokens.PrimaryCyan
        isFocused -> accent.copy(alpha = 0.85f)
        else -> Color.Transparent
    }

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .then(
            if (isFocused && isTv) {
                Modifier.shadow(
                    elevation = 14.dp,
                    shape = shape,
                    ambientColor = IvidsColorTokens.OceanAccent,
                    spotColor = IvidsColorTokens.PrimaryCyan
                )
            } else {
                Modifier
            }
        )
        .border(width = borderWidth, color = borderColor, shape = shape)
        .onFocusChanged { focusState ->
            isFocused = focusState.isFocused
            if (focusState.isFocused) {
                onFocused?.invoke()
            }
        }
        .focusable(enabled = enabled)
}
