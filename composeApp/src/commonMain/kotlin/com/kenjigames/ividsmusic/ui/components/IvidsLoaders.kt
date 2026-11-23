package com.kenjigames.ividsmusic.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kenjigames.ividsmusic.ui.theme.IvidsColorTokens

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
