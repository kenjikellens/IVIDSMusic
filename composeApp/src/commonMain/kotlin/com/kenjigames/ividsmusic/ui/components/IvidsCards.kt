package com.kenjigames.ividsmusic.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
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
import com.kenjigames.ividsmusic.ui.theme.LocalLayoutMetrics
import com.kenjigames.ividsmusic.ui.theme.tvFocusable

/**
 * Renders a tonal artwork card for tracks, albums, or circular artist portraits using
 * [LocalLayoutMetrics] width, hover border/surface illumination, and touch/hover Play FAB visibility.
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
    width: Dp = LocalLayoutMetrics.current.cardWidth
) {
    val accent = LocalDynamicAccent.current
    val isTvMode = LocalIsTvMode.current
    val metrics = LocalLayoutMetrics.current
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
            lerp(base, tint, if (isHovered) 0.30f else 0.16f)
        } else {
            base
        }
    }

    val surfaceColor by animateColorAsState(
        targetValue = targetCardSurface,
        animationSpec = tween(160, easing = FastOutSlowInEasing),
        label = "MusicCardSurface"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isHovered) IvidsColorTokens.HoverBorder else Color.Transparent,
        animationSpec = tween(160, easing = FastOutSlowInEasing),
        label = "MusicCardBorder"
    )
    val playFabAlpha by animateFloatAsState(
        targetValue = when {
            isHovered || isTvMode -> 1f
            metrics.showTouchPlayFab -> 0.92f
            else -> 0f
        },
        animationSpec = tween(150, easing = FastOutSlowInEasing),
        label = "MusicCardPlayAlpha"
    )

    Surface(
        modifier = modifier
            .width(width)
            .pointerHoverIcon(PointerIcon.Hand)
            .tvFocusable(shape = RoundedCornerShape(16.dp))
            .hoverable(interactionSource)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = surfaceColor,
        border = if (borderColor != Color.Transparent) BorderStroke(1.dp, borderColor) else null
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
                            .pointerHoverIcon(PointerIcon.Hand)
                            .graphicsLayer { alpha = playFabAlpha }
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
 * ([BlurredArtworkBackdrop]), crisp cover framing, and smooth hoverable action pills.
 */
/**
 * Renders the adaptive Living Album Hero banner using [BlurredArtworkBackdrop], stacking content
 * cleanly on compact mobile viewports so titles and [FlowRow] CTAs never clip or misalign with `×`.
 */
@OptIn(ExperimentalLayoutApi::class)
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
    onDismiss: (() -> Unit)? = null,
    primaryEnabled: Boolean = true,
    secondaryEnabled: Boolean = true
) {
    val accent = LocalDynamicAccent.current
    val isCompactMobile = LocalLayoutMetrics.current.isCompactMobile

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(if (isCompactMobile) 20.dp else 24.dp),
        color = IvidsColorTokens.GlassSurface
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(if (isCompactMobile) 20.dp else 24.dp))
        ) {
            BlurredArtworkBackdrop(
                artworkUrl = artworkUrl,
                tintColor = null,
                scrimAlpha = 0.68f,
                blurRadius = 46.dp,
                fallbackColor = IvidsColorTokens.GlassSurface,
                modifier = Modifier.matchParentSize()
            )

            @Composable
            fun HeroBodyContent(bodyModifier: Modifier = Modifier) {
                Column(modifier = bodyModifier) {
                    Text(
                        text = title,
                        style = if (isCompactMobile) {
                            MaterialTheme.typography.headlineSmall
                        } else {
                            MaterialTheme.typography.displaySmall
                        },
                        color = IvidsColorTokens.ChalkInk,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = if (onDismiss != null && (isCompactMobile || artworkUrl.isNullOrBlank())) {
                            Modifier.padding(end = 36.dp)
                        } else if (onDismiss != null) {
                            Modifier.padding(end = 32.dp)
                        } else {
                            Modifier
                        }
                    )
                    if (subtitle.isNotBlank()) {
                        Spacer(modifier = Modifier.height(if (isCompactMobile) 6.dp else 8.dp))
                        Text(
                            text = subtitle,
                            style = if (isCompactMobile) {
                                MaterialTheme.typography.bodyMedium
                            } else {
                                MaterialTheme.typography.bodyLarge
                            },
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
                    Spacer(modifier = Modifier.height(if (isCompactMobile) 14.dp else 18.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onPrimaryAction,
                            enabled = primaryEnabled,
                            shape = CircleShape,
                            contentPadding = PaddingValues(
                                horizontal = if (isCompactMobile) 18.dp else 22.dp,
                                vertical = if (isCompactMobile) 10.dp else 12.dp
                            ),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accent,
                                contentColor = IvidsColorTokens.OnAccentDark,
                                disabledContainerColor = accent.copy(alpha = 0.32f),
                                disabledContentColor = IvidsColorTokens.OnAccentDark.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier
                                .then(if (primaryEnabled) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
                                .tvFocusable(enabled = primaryEnabled, shape = CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = primaryActionLabel,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        if (!secondaryActionLabel.isNullOrBlank() && onSecondaryAction != null) {
                            IvidsTonalPill(
                                onClick = onSecondaryAction,
                                enabled = secondaryEnabled,
                                baseColor = Color(0x22FFFFFF),
                                hoverColor = Color(0x3DFFFFFF),
                                baseBorderColor = IvidsColorTokens.HairlineBorder,
                                hoverBorderColor = IvidsColorTokens.HoverBorder,
                                contentPadding = PaddingValues(
                                    horizontal = if (isCompactMobile) 16.dp else 20.dp,
                                    vertical = if (isCompactMobile) 10.dp else 12.dp
                                )
                            ) {
                                Text(
                                    text = secondaryActionLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = IvidsColorTokens.ChalkInk,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            if (isCompactMobile) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (!artworkUrl.isNullOrBlank()) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            AsyncImage(
                                model = artworkUrl,
                                contentDescription = title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(118.dp)
                                    .align(Alignment.CenterStart)
                                    .shadow(
                                        elevation = 16.dp,
                                        shape = RoundedCornerShape(16.dp),
                                        ambientColor = Color.Black,
                                        spotColor = Color.Black
                                    )
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(IvidsColorTokens.RecessedTrough)
                            )
                        }
                    }
                    HeroBodyContent(bodyModifier = Modifier.fillMaxWidth())
                }
            } else {
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
                    HeroBodyContent(bodyModifier = Modifier.weight(1f))
                }
            }

            // Place dismiss button last so it always stays above foreground layout in z-order.
            if (onDismiss != null) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(40.dp)
                        .pointerHoverIcon(PointerIcon.Hand)
                        .tvFocusable(shape = CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Dismiss",
                        tint = IvidsColorTokens.ChalkInk.copy(alpha = 0.8f)
                    )
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

    val cardWidth = LocalLayoutMetrics.current.cardWidth

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(itemCount) {
            Surface(
                modifier = Modifier.width(cardWidth),
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
