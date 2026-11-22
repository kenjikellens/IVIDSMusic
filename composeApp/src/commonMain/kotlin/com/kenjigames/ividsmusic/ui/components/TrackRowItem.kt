package com.kenjigames.ividsmusic.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
 * Displays an interactive track list row with tabular track index, cover thumbnail, active
 * playback indicator, offline badge, like button, and overflow actions menu.
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

    val rowBg = if (isCurrentlyPlaying) {
        accent.copy(alpha = 0.12f)
    } else {
        IvidsColorTokens.VinylSurface.copy(alpha = 0.85f)
    }
    val borderColor = if (isCurrentlyPlaying) {
        accent.copy(alpha = 0.45f)
    } else {
        IvidsColorTokens.HairlineBorder.copy(alpha = 0.7f)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .tvFocusable(shape = RoundedCornerShape(14.dp))
            .clickable(onClick = onPlay),
        shape = RoundedCornerShape(14.dp),
        color = rowBg,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (index != null) {
                Box(
                    modifier = Modifier.width(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCurrentlyPlaying) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = accent,
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
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(IvidsColorTokens.GrooveElevated),
                contentAlignment = Alignment.Center
            ) {
                if (track.coverUrl.isNotBlank()) {
                    AsyncImage(
                        model = track.coverUrl,
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(48.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = accent.copy(alpha = 0.7f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isCurrentlyPlaying) accent else IvidsColorTokens.ChalkInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                        Text(
                            text = "$kbps kbps",
                            style = IvidsTextStyles.tabularBadge(),
                            color = accent
                        )
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
                    tint = if (isLiked) accent else IvidsColorTokens.SlateMuted,
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
                    containerColor = IvidsColorTokens.GrooveElevated
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
