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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.kenjigames.ividsmusic.ui.theme.ColorExtractor
import com.kenjigames.ividsmusic.ui.theme.IvidsColorTokens
import com.kenjigames.ividsmusic.ui.theme.IvidsTextStyles
import com.kenjigames.ividsmusic.ui.theme.LocalDynamicAccent
import com.kenjigames.ividsmusic.ui.theme.LocalIsTvMode
import com.kenjigames.ividsmusic.ui.theme.tvFocusable

/**
 * Renders a borderless tonal artwork card for tracks, albums, or circular artist portraits that
 * softly blends its own cover artwork's dominant color into the card surface and reveals a
 * signature IVIDS-cyan play button on hover.
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
    width: Dp = 176.dp
) {
    val accent = LocalDynamicAccent.current
    val isTvMode = LocalIsTvMode.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    var cardTint by remember(artworkUrl) { mutableStateOf<Color?>(null) }
    LaunchedEffect(artworkUrl) {
        cardTint = if (!artworkUrl.isNullOrBlank()) {
            ColorExtractor.extractCardTintColor(artworkUrl)
        } else {
            null
        }
    }

    val targetCardSurface = remember(cardTint, isHovered) {
        val base = if (isHovered) IvidsColorTokens.GlassHover else IvidsColorTokens.GlassSurface
        val tint = cardTint
        if (tint != null) {
            lerp(base, tint, if (isHovered) 0.28f else 0.18f)
        } else {
            base
        }
    }

    val surfaceColor by animateColorAsState(
        targetValue = targetCardSurface,
        animationSpec = tween(220),
        label = "MusicCardSurface"
    )
    val playFabAlpha by animateFloatAsState(
        targetValue = if (isHovered || isTvMode) 1f else 0f,
        animationSpec = tween(180),
        label = "MusicCardPlayAlpha"
    )
    val playFabScale by animateFloatAsState(
        targetValue = if (isHovered || isTvMode) 1f else 0.8f,
        animationSpec = tween(180),
        label = "MusicCardPlayScale"
    )

    Surface(
        modifier = modifier
            .width(width)
            .shadow(
                elevation = if (isHovered) 14.dp else 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black,
                spotColor = Color.Black
            )
            .tvFocusable(shape = RoundedCornerShape(16.dp))
            .hoverable(interactionSource)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = surfaceColor
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(if (isCircular) CircleShape else RoundedCornerShape(12.dp))
                    .background(IvidsColorTokens.RecessedTrough),
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
                        tint = IvidsColorTokens.SlateMuted,
                        modifier = Modifier.size(44.dp)
                    )
                }

                if (!badgeText.isNullOrBlank()) {
                    Surface(
                        color = IvidsColorTokens.AbyssRail.copy(alpha = 0.82f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = IvidsTextStyles.tabularBadge(),
                            color = IvidsColorTokens.ChalkInk,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }

                if (onPlayClick != null && playFabAlpha > 0.02f) {
                    Surface(
                        onClick = onPlayClick,
                        shape = CircleShape,
                        color = accent,
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                            .size(42.dp)
                            .graphicsLayer {
                                alpha = playFabAlpha
                                scaleX = playFabScale
                                scaleY = playFabScale
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "Play $title",
                                tint = IvidsColorTokens.OnAccentDark,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = IvidsColorTokens.ChalkInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
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
 * Renders the signature Living Album Hero banner using a true blurred cover-art backdrop
 * ([BlurredArtworkBackdrop]), crisp cover framing, and a signature IVIDS primary Play button.
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
        shape = RoundedCornerShape(24.dp),
        color = IvidsColorTokens.GlassSurface
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            BlurredArtworkBackdrop(
                artworkUrl = artworkUrl,
                tintColor = null,
                scrimAlpha = 0.68f,
                blurRadius = 46.dp,
                fallbackColor = IvidsColorTokens.GlassSurface,
                modifier = Modifier.matchParentSize()
            )

            if (onDismiss != null) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
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
                    .padding(horizontal = 28.dp, vertical = 26.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                if (!artworkUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = artworkUrl,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(144.dp)
                            .shadow(
                                elevation = 18.dp,
                                shape = RoundedCornerShape(18.dp),
                                ambientColor = Color.Black,
                                spotColor = Color.Black
                            )
                            .clip(RoundedCornerShape(18.dp))
                            .background(IvidsColorTokens.RecessedTrough)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.displaySmall,
                        color = IvidsColorTokens.ChalkInk,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitle.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyLarge,
                            color = IvidsColorTokens.SlateMuted,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else if (eyebrow.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = eyebrow,
                            style = MaterialTheme.typography.bodyMedium,
                            color = IvidsColorTokens.SlateMuted
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onPrimaryAction,
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accent,
                                contentColor = IvidsColorTokens.OnAccentDark
                            ),
                            modifier = Modifier.tvFocusable(shape = CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(primaryActionLabel, style = MaterialTheme.typography.labelLarge)
                        }

                        if (!secondaryActionLabel.isNullOrBlank() && onSecondaryAction != null) {
                            Surface(
                                onClick = onSecondaryAction,
                                shape = CircleShape,
                                color = Color(0x18FFFFFF),
                                border = BorderStroke(1.dp, IvidsColorTokens.HairlineSubtle),
                                modifier = Modifier.tvFocusable(shape = CircleShape)
                            ) {
                                Text(
                                    text = secondaryActionLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = IvidsColorTokens.ChalkInk,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Displays an animated horizontal row of borderless skeleton card placeholders while remote
 * discovery data is loading.
 */
@Composable
fun ShimmerSkeletonRow(
    modifier: Modifier = Modifier,
    itemCount: Int = 6,
    isCircular: Boolean = false
) {
    val transition = rememberInfiniteTransition(label = "SkeletonPulse")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.72f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SkeletonAlpha"
    )

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(itemCount) {
            Surface(
                modifier = Modifier.width(176.dp),
                shape = RoundedCornerShape(16.dp),
                color = IvidsColorTokens.GlassSurface.copy(alpha = alpha)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(if (isCircular) CircleShape else RoundedCornerShape(12.dp))
                            .background(IvidsColorTokens.RecessedTrough.copy(alpha = alpha + 0.15f))
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.78f)
                            .height(15.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(IvidsColorTokens.GlassHover.copy(alpha = alpha))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.48f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(IvidsColorTokens.GlassHover.copy(alpha = alpha * 0.7f))
                    )
                }
            }
        }
    }
}
