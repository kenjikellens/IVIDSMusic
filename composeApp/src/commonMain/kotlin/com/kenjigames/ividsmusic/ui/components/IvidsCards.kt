package com.kenjigames.ividsmusic.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.kenjigames.ividsmusic.ui.theme.IvidsColorTokens
import com.kenjigames.ividsmusic.ui.theme.IvidsTextStyles
import com.kenjigames.ividsmusic.ui.theme.LocalIsTvMode
import com.kenjigames.ividsmusic.ui.theme.LocalLayoutMetrics
import com.kenjigames.ividsmusic.ui.theme.tvFocusable

/**
 * Descriptor for an individual desktop right-click context menu action on [MusicCard].
 */
data class CardContextMenuAction(
    val label: String,
    val onClick: () -> Unit
)

/**
 * Renders a borderless album, artist, or track card with smooth hover illumination, optional
 * desktop right-click context menu, and a floating Play action button.
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
    contextMenuItems: List<CardContextMenuAction> = emptyList(),
    width: Dp = LocalLayoutMetrics.current.cardWidth
) {
    val isTvMode = LocalIsTvMode.current
    val metrics = LocalLayoutMetrics.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    var menuExpanded by remember { mutableStateOf(false) }

    val playFabAlpha by animateFloatAsState(
        targetValue = when {
            isHovered || isTvMode -> 1f
            metrics.showTouchPlayFab -> 0.92f
            else -> 0f
        },
        animationSpec = tween(150, easing = FastOutSlowInEasing),
        label = "MusicCardPlayAlpha"
    )

    val delayedClick = rememberDelayedOnClick(delayMs = 130L, onClick = onClick)
    Box(modifier = modifier.width(width)) {
        Surface(
            onClick = delayedClick,
            interactionSource = interactionSource,
            modifier = Modifier
                .fillMaxWidth()
                .onSecondaryClick(enabled = contextMenuItems.isNotEmpty()) { menuExpanded = true }
                .ividsTouchFeedback(interactionSource = interactionSource, pressedScale = 0.965f)
                .pointerHoverIcon(PointerIcon.Hand)
                .tvFocusable(shape = RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            color = Color.Transparent
        ) {
        Column(
            modifier = Modifier
                .ividsHoverZoom(
                    hovered = isHovered && !metrics.isCompactMobile,
                    shape = RoundedCornerShape(14.dp),
                    hoverColor = Color(0x1EFFFFFF)
                )
                .padding(if (metrics.isCompactMobile) 4.dp else 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(if (isCircular) CircleShape else RoundedCornerShape(10.dp))
                    .background(IvidsColorTokens.GlassSurface),
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
                    val fabHover = remember { MutableInteractionSource() }
                    val isFabHovered by fabHover.collectIsHoveredAsState()
                    Surface(
                        onClick = onPlayClick,
                        interactionSource = fabHover,
                        shape = CircleShape,
                        color = IvidsColorTokens.ChalkInk,
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                            .size(42.dp)
                            .ividsTouchFeedback(interactionSource = fabHover, pressedScale = 0.92f)
                            .pointerHoverIcon(PointerIcon.Hand)
                            .graphicsLayer { alpha = playFabAlpha }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .ividsHoverZoom(hovered = isFabHovered, shape = CircleShape, style = IvidsHoverStyle.ON_LIGHT),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "Play $title",
                                tint = IvidsColorTokens.ObsidianStage,
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

        if (contextMenuItems.isNotEmpty()) {
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                containerColor = IvidsColorTokens.GlassSurface
            ) {
                contextMenuItems.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item.label, color = IvidsColorTokens.ChalkInk) },
                        onClick = {
                            menuExpanded = false
                            item.onClick()
                        }
                    )
                }
            }
        }
    }
}

/**
 * Compact horizontal Spotify-style quick-pick tile with leading 56dp artwork or icon, bold title,
 * and hover-revealed play button on a borderless neutral charcoal surface.
 */
@Composable
fun SpotifyQuickPickTile(
    title: String,
    artworkUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: ImageVector = Icons.Rounded.MusicNote,
    leadingIconTint: Color = IvidsColorTokens.SlateMuted,
    onPlayClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val delayedClick = rememberDelayedOnClick(delayMs = 130L, onClick = onClick)
    val playAlpha by animateFloatAsState(
        targetValue = if (isHovered) 1f else 0f,
        animationSpec = tween(140, easing = FastOutSlowInEasing),
        label = "QuickPickPlayAlpha"
    )

    Surface(
        onClick = delayedClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF181818),
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(10.dp))
            .ividsTouchFeedback(interactionSource = interactionSource, pressedScale = 0.975f)
            .pointerHoverIcon(PointerIcon.Hand)
            .tvFocusable(shape = RoundedCornerShape(10.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .ividsHoverZoom(hovered = isHovered, shape = RoundedCornerShape(10.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .background(IvidsColorTokens.GlassSurface),
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
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = leadingIconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = IvidsColorTokens.ChalkInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = IvidsColorTokens.SlateMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (onPlayClick != null && playAlpha > 0.02f) {
                IvidsPlayCircleButton(
                    isPlaying = false,
                    isBuffering = false,
                    contentDescription = "Play $title",
                    onClick = onPlayClick,
                    size = 36.dp,
                    iconSize = 20.dp,
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .graphicsLayer { alpha = playAlpha }
                )
            }
        }
    }
}

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
    secondaryEnabled: Boolean = true,
    compact: Boolean = false,
    trailingActions: (@Composable () -> Unit)? = null
) {
    val isCompactMobile = LocalLayoutMetrics.current.isCompactMobile
    val cornerRadius = if (compact || isCompactMobile) 18.dp else 24.dp

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cornerRadius),
        color = IvidsColorTokens.GlassSurface
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(cornerRadius))
        ) {
            BlurredArtworkBackdrop(
                artworkUrl = artworkUrl,
                tintColor = null,
                scrimAlpha = 0.68f,
                fallbackColor = IvidsColorTokens.GlassSurface,
                modifier = Modifier.matchParentSize()
            )

            @Composable
            fun HeroBodyContent(bodyModifier: Modifier = Modifier) {
                Column(modifier = bodyModifier) {
                    Text(
                        text = title,
                        style = if (isCompactMobile || compact) {
                            MaterialTheme.typography.headlineSmall
                        } else {
                            MaterialTheme.typography.displaySmall
                        },
                        color = IvidsColorTokens.ChalkInk,
                        maxLines = if (compact) 1 else 2,
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
                        Spacer(modifier = Modifier.height(if (compact || isCompactMobile) 4.dp else 8.dp))
                        Text(
                            text = subtitle,
                            style = if (compact || isCompactMobile) {
                                MaterialTheme.typography.bodyMedium
                            } else {
                                MaterialTheme.typography.bodyLarge
                            },
                            color = IvidsColorTokens.SlateMuted,
                            maxLines = if (compact) 1 else 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else if (eyebrow.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = eyebrow,
                            style = MaterialTheme.typography.bodyMedium,
                            color = IvidsColorTokens.SlateMuted
                        )
                    }
                    Spacer(modifier = Modifier.height(if (compact) 12.dp else if (isCompactMobile) 14.dp else 18.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IvidsPrimaryButton(
                            onClick = onPrimaryAction,
                            enabled = primaryEnabled,
                            contentPadding = PaddingValues(
                                horizontal = if (compact || isCompactMobile) 16.dp else 22.dp,
                                vertical = if (compact || isCompactMobile) 9.dp else 12.dp
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = IvidsColorTokens.ObsidianStage,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = primaryActionLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = IvidsColorTokens.ObsidianStage,
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
                                contentPadding = PaddingValues(
                                    horizontal = if (compact || isCompactMobile) 15.dp else 20.dp,
                                    vertical = if (compact || isCompactMobile) 9.dp else 12.dp
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

                        trailingActions?.invoke()
                    }
                }
            }

            if (isCompactMobile) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(if (compact) 14.dp else 18.dp),
                    verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp)
                ) {
                    if (!artworkUrl.isNullOrBlank()) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            AsyncImage(
                                model = artworkUrl,
                                contentDescription = title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(if (compact) 84.dp else 118.dp)
                                    .align(Alignment.CenterStart)
                                    .shadow(
                                        elevation = 16.dp,
                                        shape = RoundedCornerShape(14.dp),
                                        ambientColor = Color.Black,
                                        spotColor = Color.Black
                                    )
                                    .clip(RoundedCornerShape(14.dp))
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
                        .padding(
                            horizontal = if (compact) 22.dp else 28.dp,
                            vertical = if (compact) 16.dp else 26.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (compact) 18.dp else 24.dp)
                ) {
                    if (!artworkUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = artworkUrl,
                            contentDescription = title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(if (compact) 104.dp else 144.dp)
                                .shadow(
                                    elevation = 18.dp,
                                    shape = RoundedCornerShape(if (compact) 14.dp else 18.dp),
                                    ambientColor = Color.Black,
                                    spotColor = Color.Black
                                )
                                .clip(RoundedCornerShape(if (compact) 14.dp else 18.dp))
                                .background(IvidsColorTokens.RecessedTrough)
                        )
                    }
                    HeroBodyContent(bodyModifier = Modifier.weight(1f))
                }
            }

            // Place dismiss button last so it always stays above foreground layout in z-order.
            if (onDismiss != null) {
                IvidsIconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Dismiss",
                        tint = IvidsColorTokens.ChalkInk.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}
