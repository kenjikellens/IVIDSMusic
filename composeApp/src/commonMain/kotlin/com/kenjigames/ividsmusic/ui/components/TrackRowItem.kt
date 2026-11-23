package com.kenjigames.ividsmusic.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.kenjigames.ividsmusic.domain.model.Track
import com.kenjigames.ividsmusic.ui.i18n.LocalStrings
import com.kenjigames.ividsmusic.ui.theme.IvidsColorTokens
import com.kenjigames.ividsmusic.ui.theme.IvidsTextStyles
import com.kenjigames.ividsmusic.ui.theme.LocalDynamicAccent
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
 * Displays a sleek borderless interactive track row with hover elevation, active equalizer tint,
 * tabular duration/bitrate badges, like toggle, and overflow actions menu.
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
    val accent = LocalDynamicAccent.current
    val strings = LocalStrings.current
    var menuExpanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val rowBg by animateColorAsState(
        targetValue = when {
            isCurrentlyPlaying -> Color(0x1CFFFFFF)
            isHovered -> IvidsColorTokens.GlassHover
            else -> Color.Transparent
        },
        animationSpec = tween(150),
        label = "TrackRowBg"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .tvFocusable(shape = RoundedCornerShape(12.dp))
            .hoverable(interactionSource)
            .clickable(onClick = onPlay),
        shape = RoundedCornerShape(12.dp),
        color = rowBg
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (index != null) {
                Box(
                    modifier = Modifier.width(26.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCurrentlyPlaying) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = IvidsColorTokens.ChalkInk,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text(
                            text = index.toString(),
                            style = IvidsTextStyles.tabularBadge(),
                            color = IvidsColorTokens.SlateMuted
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(IvidsColorTokens.GlassSurface),
                contentAlignment = Alignment.Center
            ) {
                if (track.coverUrl.isNotBlank()) {
                    AsyncImage(
                        model = track.coverUrl,
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(46.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = IvidsColorTokens.SlateMuted,
                        modifier = Modifier.size(22.dp)
                    )
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
                        color = IvidsColorTokens.SlateMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable(onClick = onOpenArtist)
                    )
                    if (isDownloaded) {
                        Icon(
                            imageVector = Icons.Rounded.DownloadDone,
                            contentDescription = strings["saved_offline"],
                            tint = IvidsColorTokens.SuccessMint,
                            modifier = Modifier.size(14.dp)
                        )
                    }
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

            if (track.durationSeconds > 0) {
                Text(
                    text = formatDurationSeconds(track.durationSeconds),
                    style = IvidsTextStyles.tabularTime(),
                    color = IvidsColorTokens.SlateMuted
                )
            }

            IconButton(
                onClick = onToggleLike,
                modifier = Modifier
                    .size(36.dp)
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
                        .size(36.dp)
                        .tvFocusable(shape = CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = strings["more_info"],
                        tint = IvidsColorTokens.SlateMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    containerColor = IvidsColorTokens.GlassHover
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
