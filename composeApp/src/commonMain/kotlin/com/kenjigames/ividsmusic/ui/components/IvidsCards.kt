package com.kenjigames.ividsmusic.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.kenjigames.ividsmusic.ui.theme.IvidsColorTokens
import com.kenjigames.ividsmusic.ui.theme.IvidsTextStyles
import com.kenjigames.ividsmusic.ui.theme.LocalDynamicAccent
import com.kenjigames.ividsmusic.ui.theme.tvFocusable

/**
 * Renders an artwork card for tracks, albums, or circular artist avatars with IVIDS elevated
 * surfaces, hover/focus play badge reveal, and D-pad TV focus rings.
 */
@Composable
fun MusicCard(
    title: String,
    subtitle: String,
    artworkUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCircular: Boolean = false,
    badgeText: String? = null,
    onPlayClick: (() -> Unit)? = null,
    width: Dp = 164.dp
) {
    val accent = LocalDynamicAccent.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isHovered) 1.025f else 1f,
        animationSpec = tween(160),
        label = "CardHoverScale"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isHovered) accent.copy(alpha = 0.55f) else IvidsColorTokens.HairlineBorder,
        animationSpec = tween(180),
        label = "CardBorderColor"
    )

    Surface(
        modifier = modifier
            .width(width)
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .tvFocusable(shape = RoundedCornerShape(16.dp))
            .hoverable(interactionSource)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = IvidsColorTokens.GrooveElevated,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(if (isCircular) CircleShape else RoundedCornerShape(12.dp))
                    .background(IvidsColorTokens.VinylSurface),
                contentAlignment = Alignment.Center
            ) {
                if (!artworkUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = artworkUrl,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = accent.copy(alpha = 0.7f),
                        modifier = Modifier.size(42.dp)
                    )
                }

                if (!badgeText.isNullOrBlank()) {
                    Surface(
                        color = IvidsColorTokens.ObsidianStage.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = IvidsTextStyles.tabularBadge(),
                            color = accent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (onPlayClick != null) {
                    Surface(
                        onClick = onPlayClick,
                        shape = CircleShape,
                        color = accent,
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "Play $title",
                                tint = IvidsColorTokens.OnAccentDark,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = IvidsColorTokens.ChalkInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = IvidsColorTokens.SlateMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Renders the signature Living Album Hero banner with ambient radial cyan/album-art glow blobs,
 * optional featured cover art, primary/secondary action buttons, and dismiss button.
 */
@Composable
fun LivingAlbumHero(
    eyebrow: String,
    title: String,
    subtitle: String,
    artworkUrl: String?,
    primaryActionLabel: String,
    onPrimaryAction: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null
) {
    val accent = LocalDynamicAccent.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = IvidsColorTokens.VinylSurface,
        border = BorderStroke(1.dp, IvidsColorTokens.HairlineBorder)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.24f),
                            IvidsColorTokens.OceanAccent.copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.18f, size.height * 0.35f),
                        radius = size.maxDimension * 0.72f
                    )
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            IvidsColorTokens.OceanAccent.copy(alpha = 0.18f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.85f, size.height * 0.8f),
                        radius = size.maxDimension * 0.55f
                    )
                )
            }

            if (onDismiss != null) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .tvFocusable(shape = CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Dismiss",
                        tint = IvidsColorTokens.SlateMuted
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (!artworkUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = artworkUrl,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(116.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(IvidsColorTokens.GrooveElevated)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    if (eyebrow.isNotBlank()) {
                        Text(
                            text = eyebrow.uppercase(),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = accent
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = IvidsColorTokens.ChalkInk,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = IvidsColorTokens.SlateMuted,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onPrimaryAction,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accent,
                                contentColor = IvidsColorTokens.OnAccentDark
                            ),
                            modifier = Modifier.tvFocusable(shape = RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(primaryActionLabel, style = MaterialTheme.typography.labelLarge)
                        }

                        if (!secondaryActionLabel.isNullOrBlank() && onSecondaryAction != null) {
                            OutlinedButton(
                                onClick = onSecondaryAction,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, IvidsColorTokens.HairlineBorder),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = IvidsColorTokens.GrooveElevated.copy(alpha = 0.7f),
                                    contentColor = IvidsColorTokens.ChalkInk
                                ),
                                modifier = Modifier.tvFocusable(shape = RoundedCornerShape(12.dp))
                            ) {
                                Text(secondaryActionLabel, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Displays an animated horizontal row of skeleton card placeholders while remote discovery
 * data is loading.
 */
@Composable
fun ShimmerSkeletonRow(
    modifier: Modifier = Modifier,
    itemCount: Int = 6,
    isCircular: Boolean = false
) {
    val transition = rememberInfiniteTransition(label = "SkeletonPulse")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SkeletonAlpha"
    )

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(itemCount) {
            Surface(
                modifier = Modifier.width(164.dp),
                shape = RoundedCornerShape(16.dp),
                color = IvidsColorTokens.GrooveElevated.copy(alpha = alpha),
                border = BorderStroke(1.dp, IvidsColorTokens.HairlineBorder.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(if (isCircular) CircleShape else RoundedCornerShape(12.dp))
                            .background(IvidsColorTokens.VinylSurface.copy(alpha = alpha + 0.2f))
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(IvidsColorTokens.HairlineBorder.copy(alpha = alpha))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(11.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(IvidsColorTokens.HairlineBorder.copy(alpha = alpha * 0.8f))
                    )
                }
            }
        }
    }
}
