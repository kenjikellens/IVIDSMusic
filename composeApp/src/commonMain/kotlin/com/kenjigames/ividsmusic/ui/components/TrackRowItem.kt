package com.kenjigames.ividsmusic.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.kenjigames.ividsmusic.domain.model.Track
import com.kenjigames.ividsmusic.ui.i18n.LocalStrings
import com.kenjigames.ividsmusic.ui.theme.IvidsColorTokens
import com.kenjigames.ividsmusic.ui.theme.IvidsTextStyles
import com.kenjigames.ividsmusic.ui.theme.LocalLayoutMetrics
import com.kenjigames.ividsmusic.ui.theme.tvFocusable

/**
 * Formats [durationSeconds] into a crisp `m:ss` or `h:mm:ss` timestamp string for tabular rendering.
 */
fun formatDurationSeconds(durationSeconds: Int): String {
    if (durationSeconds <= 0) return "0:00"
    val hours = durationSeconds / 3600
    val minutes = (durationSeconds % 3600) / 60
    val seconds = durationSeconds % 60
    val secPad = seconds.toString().padStart(2, '0')
    return if (hours > 0) {
        val minPad = minutes.toString().padStart(2, '0')
        "$hours:$minPad:$secPad"
    } else {
        "$minutes:$secPad"
    }
}

/**
 * Formats [durationMs] into a tabular `m:ss` playback scrubber timestamp string.
 */
fun formatDurationMillis(durationMs: Long): String {
    if (durationMs <= 0L) return "0:00"
    return formatDurationSeconds((durationMs / 1000L).toInt())
}

/**
 * Displays a responsive interactive track row adapting padding, touch target sizes, and metadata
 * density via [LocalLayoutMetrics] with crisp hover illumination and quick action menu.
 */
@Composable
fun TrackRowItem(
    track: Track,
    onPlay: () -> Unit,
    onToggleLike: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDownload: () -> Unit,
    onOpenArtist: () -> Unit,
    onOpenSongDetail: () -> Unit,
    modifier: Modifier = Modifier,
    index: Int? = null,
    isCurrentlyPlaying: Boolean = false,
    isLiked: Boolean = track.isLiked,
    isDownloaded: Boolean = track.isDownloaded,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val strings = LocalStrings.current
    val metrics = LocalLayoutMetrics.current
    var menuExpanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val artistHoverSource = remember { MutableInteractionSource() }
    val isArtistHovered by artistHoverSource.collectIsHoveredAsState()

    val rowBg by animateColorAsState(
        targetValue = when {
            isHovered && isCurrentlyPlaying -> Color(0xFF2F2F3B)
            isHovered -> Color(0xFF24242E)
            isCurrentlyPlaying -> Color(0xFF1A1A20)
            else -> Color.Transparent
        },
        animationSpec = tween(140, easing = FastOutSlowInEasing),
        label = "TrackRowBg"
    )
    val playOverlayAlpha by animateFloatAsState(
        targetValue = if (isHovered && !isCurrentlyPlaying) 1f else 0f,
        animationSpec = tween(140, easing = FastOutSlowInEasing),
        label = "TrackRowPlayOverlay"
    )
    val thumbSize = if (metrics.isCompactMobile) 42.dp else 46.dp
    val iconButtonSize = if (metrics.isCompactMobile) 42.dp else 36.dp

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .pointerHoverIcon(PointerIcon.Hand)
            .tvFocusable(shape = RoundedCornerShape(12.dp))
            .hoverable(interactionSource)
            .clickable(onClick = onPlay),
        shape = RoundedCornerShape(12.dp),
        color = rowBg
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (metrics.isCompactMobile) 8.dp else 12.dp,
                    vertical = if (metrics.isCompactMobile) 8.dp else 9.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (metrics.isCompactMobile) 10.dp else 14.dp)
        ) {
            if (index != null) {
                Box(
                    modifier = Modifier.width(if (metrics.isCompactMobile) 22.dp else 26.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isCurrentlyPlaying -> {
                            Icon(
                                imageVector = Icons.Rounded.GraphicEq,
                                contentDescription = null,
                                tint = IvidsColorTokens.ChalkInk,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        isHovered -> {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = strings["play"],
                                tint = IvidsColorTokens.ChalkInk,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        else -> {
                            Text(
                                text = index.toString(),
                                style = IvidsTextStyles.tabularBadge(),
                                color = IvidsColorTokens.SlateMuted
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(thumbSize)
                    .clip(RoundedCornerShape(10.dp))
                    .background(IvidsColorTokens.GlassSurface),
                contentAlignment = Alignment.Center
            ) {
                if (track.coverUrl.isNotBlank()) {
                    AsyncImage(
                        model = track.coverUrl,
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(thumbSize)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = IvidsColorTokens.SlateMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }
                if (playOverlayAlpha > 0.02f) {
                    Box(
                        modifier = Modifier
                            .size(thumbSize)
                            .graphicsLayer { alpha = playOverlayAlpha }
                            .background(Color(0x99000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = IvidsColorTokens.ChalkInk,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = IvidsColorTokens.ChalkInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isArtistHovered || isHovered) IvidsColorTokens.ChalkInk else IvidsColorTokens.SlateMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .pointerHoverIcon(PointerIcon.Hand)
                            .hoverable(artistHoverSource)
                            .clickable(onClick = onOpenArtist)
                    )
                    if (isDownloaded) {
                        Icon(
                            imageVector = Icons.Rounded.DownloadDone,
                            contentDescription = strings["saved_offline"],
                            tint = IvidsColorTokens.SuccessMint,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    if (!metrics.isCompactMobile) {
                        track.bitrateKbps?.takeIf { isDownloaded && it > 0 }?.let { kbps ->
                            Surface(
                                color = IvidsColorTokens.RecessedTrough,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "$kbps kbps",
                                    style = IvidsTextStyles.tabularBadge(),
                                    color = IvidsColorTokens.SlateMuted,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (track.durationSeconds > 0) {
                Text(
                    text = formatDurationSeconds(track.durationSeconds),
                    style = IvidsTextStyles.tabularTime(),
                    color = if (isHovered) IvidsColorTokens.ChalkInk.copy(alpha = 0.85f) else IvidsColorTokens.SlateMuted
                )
            }

            IconButton(
                onClick = onToggleLike,
                modifier = Modifier
                    .size(iconButtonSize)
                    .pointerHoverIcon(PointerIcon.Hand)
                    .tvFocusable(shape = CircleShape)
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = strings["like"],
                    tint = if (isLiked) IvidsColorTokens.DangerCoral else IvidsColorTokens.SlateMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (trailingContent != null) {
                trailingContent()
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier
                        .size(iconButtonSize)
                        .pointerHoverIcon(PointerIcon.Hand)
                        .tvFocusable(shape = CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = strings["more_info"],
                        tint = if (isHovered) IvidsColorTokens.ChalkInk else IvidsColorTokens.SlateMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    containerColor = IvidsColorTokens.GlassSurface
                ) {
                    DropdownMenuItem(
                        text = { Text(strings["play_next"], color = IvidsColorTokens.ChalkInk) },
                        onClick = { menuExpanded = false; onPlayNext() }
                    )
                    DropdownMenuItem(
                        text = { Text(strings["add_to_queue"], color = IvidsColorTokens.ChalkInk) },
                        onClick = { menuExpanded = false; onAddToQueue() }
                    )
                    DropdownMenuItem(
                        text = { Text(strings["add_to_playlist"], color = IvidsColorTokens.ChalkInk) },
                        onClick = { menuExpanded = false; onAddToPlaylist() }
                    )
                    DropdownMenuItem(
                        text = { Text(strings["download_track"], color = IvidsColorTokens.ChalkInk) },
                        onClick = { menuExpanded = false; onDownload() }
                    )
                    DropdownMenuItem(
                        text = { Text(strings["more_info"], color = IvidsColorTokens.ChalkInk) },
                        onClick = { menuExpanded = false; onOpenSongDetail() }
                    )
                    DropdownMenuItem(
                        text = { Text(strings["view_artist"], color = IvidsColorTokens.ChalkInk) },
                        onClick = { menuExpanded = false; onOpenArtist() }
                    )
                }
            }
        }
    }
}
