package com.kenjigames.ividsmusic.ui.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.ThumbDownOffAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.kenjigames.ividsmusic.domain.model.Playlist
import com.kenjigames.ividsmusic.domain.model.RepeatMode
import com.kenjigames.ividsmusic.domain.model.Track
import com.kenjigames.ividsmusic.ui.components.PlaylistPickerDialog
import com.kenjigames.ividsmusic.ui.components.formatDurationMillis
import com.kenjigames.ividsmusic.ui.i18n.LocalStrings
import com.kenjigames.ividsmusic.ui.theme.IvidsColorTokens
import com.kenjigames.ividsmusic.ui.theme.IvidsTextStyles
import com.kenjigames.ividsmusic.ui.theme.LocalDynamicAccent
import com.kenjigames.ividsmusic.ui.theme.tvFocusable

/**
 * Full-screen expandable Now Playing sheet featuring Living Album ambient glow, interactive
 * scrubber, transport controls, action buttons, volume control, and interactive playback queue.
 */
@Composable
fun NowPlayingSheet(
    playerState: PlayerUiState,
    playlists: List<Playlist>,
    playerViewModel: PlayerViewModel,
    onCreatePlaylist: () -> Unit,
    onOpenArtist: (artistId: String, artistName: String) -> Unit,
    onOpenSongDetail: (trackId: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = LocalDynamicAccent.current
    val strings = LocalStrings.current
    val track = playerState.currentTrack
    var showPlaylistPicker by remember { mutableStateOf(false) }

    if (showPlaylistPicker && track != null) {
        PlaylistPickerDialog(
            playlists = playlists,
            onSelectPlaylist = { playlist ->
                playerViewModel.addCurrentToPlaylist(playlist.id)
                showPlaylistPicker = false
            },
            onCreateNew = {
                showPlaylistPicker = false
                onCreatePlaylist()
            },
            onDismiss = { showPlaylistPicker = false }
        )
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = IvidsColorTokens.ObsidianStage
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.28f),
                            IvidsColorTokens.OceanAccent.copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.25f),
                        radius = size.maxDimension * 0.72f
                    )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 1040.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.tvFocusable(shape = CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = strings["close"],
                            tint = IvidsColorTokens.ChalkInk
                        )
                    }
                    Text(
                        text = strings["now_playing"],
                        style = MaterialTheme.typography.titleMedium,
                        color = IvidsColorTokens.ChalkInk
                    )
                    IconButton(
                        onClick = {
                            if (track != null) {
                                onDismiss()
                                onOpenSongDetail(track.id)
                            }
                        },
                        enabled = track != null,
                        modifier = Modifier.tvFocusable(shape = CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = strings["more_info"],
                            tint = accent
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f).widthIn(max = 820.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        NowPlayingHeroCard(
                            track = track,
                            playerState = playerState,
                            accent = accent,
                            onOpenArtist = {
                                if (track != null) {
                                    onDismiss()
                                    onOpenArtist(track.artistId, track.artist)
                                }
                            },
                            onOpenPlaylistPicker = { showPlaylistPicker = true },
                            playerViewModel = playerViewModel
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings["next_up"],
                                style = MaterialTheme.typography.titleLarge,
                                color = IvidsColorTokens.ChalkInk
                            )
                            if (playerState.queue.isNotEmpty()) {
                                TextButton(onClick = playerViewModel::clearQueue) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteSweep,
                                        contentDescription = null,
                                        tint = IvidsColorTokens.SlateMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(strings["clear_queue"], color = IvidsColorTokens.SlateMuted)
                                }
                            }
                        }
                    }

                    if (playerState.queue.isEmpty()) {
                        item {
                            Text(
                                text = strings["queue_empty"],
                                style = MaterialTheme.typography.bodyMedium,
                                color = IvidsColorTokens.SlateMuted,
                                modifier = Modifier.padding(vertical = 24.dp)
                            )
                        }
                    } else {
                        itemsIndexed(
                            items = playerState.queue,
                            key = { idx, item -> "${idx}_${item.id}" }
                        ) { index, item ->
                            val isActive = index == playerState.currentIndex
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .tvFocusable(shape = RoundedCornerShape(12.dp))
                                    .clickable { playerViewModel.jumpToQueueIndex(index) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isActive) accent.copy(alpha = 0.14f) else IvidsColorTokens.VinylSurface,
                                border = BorderStroke(
                                    1.dp,
                                    if (isActive) accent else IvidsColorTokens.HairlineBorder
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = (index + 1).toString(),
                                        style = IvidsTextStyles.tabularBadge(),
                                        color = if (isActive) accent else IvidsColorTokens.SlateMuted,
                                        modifier = Modifier.width(24.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (isActive) accent else IvidsColorTokens.ChalkInk,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = item.artist,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = IvidsColorTokens.SlateMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(
                                        onClick = { playerViewModel.removeFromQueue(index) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = strings["remove"],
                                            tint = IvidsColorTokens.SlateMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Renders the central artwork, scrubber slider, transport controls, volume bar, and quick track
 * actions inside [NowPlayingSheet].
 */
@Composable
private fun NowPlayingHeroCard(
    track: Track?,
    playerState: PlayerUiState,
    accent: Color,
    onOpenArtist: () -> Unit,
    onOpenPlaylistPicker: () -> Unit,
    playerViewModel: PlayerViewModel
) {
    val strings = LocalStrings.current
    var dragPositionMs by remember(track?.id) { mutableStateOf<Float?>(null) }
    val durationMs = playerState.durationMs.coerceAtLeast(1L)
    val effectivePos = dragPositionMs ?: playerState.positionMs.toFloat().coerceIn(0f, durationMs.toFloat())

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .fillMaxWidth(0.7f)
                .aspectRatio(1f)
                .shadow(24.dp, RoundedCornerShape(24.dp), ambientColor = accent, spotColor = accent)
                .clip(RoundedCornerShape(24.dp))
                .background(IvidsColorTokens.GrooveElevated),
            contentAlignment = Alignment.Center
        ) {
            if (track != null && track.coverUrl.isNotBlank()) {
                AsyncImage(
                    model = track.coverUrl,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(64.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = track?.title ?: strings["song_no_track"],
            style = MaterialTheme.typography.headlineMedium,
            color = IvidsColorTokens.ChalkInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = track?.artist ?: "—",
            style = MaterialTheme.typography.titleMedium,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clickable(enabled = track != null, onClick = onOpenArtist)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Slider(
            value = effectivePos,
            onValueChange = { dragPositionMs = it },
            onValueChangeFinished = {
                dragPositionMs?.let { playerViewModel.seekTo(it.toLong()) }
                dragPositionMs = null
            },
            valueRange = 0f..durationMs.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = IvidsColorTokens.HairlineBorder
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDurationMillis(effectivePos.toLong()),
                style = IvidsTextStyles.tabularTime(),
                color = IvidsColorTokens.SlateMuted
            )
            Text(
                text = formatDurationMillis(playerState.durationMs),
                style = IvidsTextStyles.tabularTime(),
                color = IvidsColorTokens.SlateMuted
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = playerViewModel::toggleShuffle,
                modifier = Modifier.tvFocusable(shape = CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shuffle,
                    contentDescription = strings["shuffle"],
                    tint = if (playerState.isShuffleEnabled) accent else IvidsColorTokens.SlateMuted
                )
            }
            IconButton(
                onClick = playerViewModel::previous,
                modifier = Modifier.size(48.dp).tvFocusable(shape = CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipPrevious,
                    contentDescription = strings["previous"],
                    tint = IvidsColorTokens.ChalkInk,
                    modifier = Modifier.size(32.dp)
                )
            }
            Surface(
                onClick = playerViewModel::togglePlayPause,
                shape = CircleShape,
                color = accent,
                modifier = Modifier.size(64.dp).tvFocusable(shape = CircleShape)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (playerState.isBuffering) {
                        CircularProgressIndicator(
                            color = IvidsColorTokens.OnAccentDark,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(28.dp)
                        )
                    } else {
                        Icon(
                            imageVector = if (playerState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = strings["play"],
                            tint = IvidsColorTokens.OnAccentDark,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
            }
            IconButton(
                onClick = playerViewModel::next,
                modifier = Modifier.size(48.dp).tvFocusable(shape = CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = strings["next"],
                    tint = IvidsColorTokens.ChalkInk,
                    modifier = Modifier.size(32.dp)
                )
            }
            IconButton(
                onClick = playerViewModel::cycleRepeatMode,
                modifier = Modifier.tvFocusable(shape = CircleShape)
            ) {
                Icon(
                    imageVector = if (playerState.repeatMode == RepeatMode.ONE) {
                        Icons.Rounded.RepeatOne
                    } else {
                        Icons.Rounded.Repeat
                    },
                    contentDescription = "Repeat",
                    tint = if (playerState.repeatMode != RepeatMode.OFF) accent else IvidsColorTokens.SlateMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = playerViewModel::toggleLikeCurrent,
                modifier = Modifier.tvFocusable(shape = CircleShape)
            ) {
                Icon(
                    imageVector = if (playerState.isCurrentLiked) {
                        Icons.Rounded.Favorite
                    } else {
                        Icons.Rounded.FavoriteBorder
                    },
                    contentDescription = strings["like"],
                    tint = if (playerState.isCurrentLiked) accent else IvidsColorTokens.SlateMuted
                )
            }
            IconButton(
                onClick = playerViewModel::dislikeCurrent,
                modifier = Modifier.tvFocusable(shape = CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ThumbDownOffAlt,
                    contentDescription = strings["dislike"],
                    tint = IvidsColorTokens.SlateMuted
                )
            }
            IconButton(
                onClick = onOpenPlaylistPicker,
                enabled = track != null,
                modifier = Modifier.tvFocusable(shape = CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                    contentDescription = strings["add_to_playlist"],
                    tint = IvidsColorTokens.ChalkInk
                )
            }
            Surface(
                onClick = { playerViewModel.downloadCurrent() },
                shape = RoundedCornerShape(12.dp),
                color = IvidsColorTokens.GrooveElevated,
                border = BorderStroke(1.dp, IvidsColorTokens.HairlineBorder),
                modifier = Modifier.tvFocusable(shape = RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (playerState.isCurrentDownloaded) {
                            Icons.Rounded.DownloadDone
                        } else {
                            Icons.Rounded.Download
                        },
                        contentDescription = strings["download_track"],
                        tint = if (playerState.isCurrentDownloaded) {
                            IvidsColorTokens.SuccessMint
                        } else {
                            accent
                        },
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = playerState.activeQuality.label,
                        style = IvidsTextStyles.tabularBadge(),
                        color = IvidsColorTokens.ChalkInk
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(
                onClick = playerViewModel::toggleMute,
                modifier = Modifier.tvFocusable(shape = CircleShape)
            ) {
                Icon(
                    imageVector = if (playerState.isMuted) {
                        Icons.AutoMirrored.Rounded.VolumeOff
                    } else {
                        Icons.AutoMirrored.Rounded.VolumeUp
                    },
                    contentDescription = strings["volume"],
                    tint = IvidsColorTokens.SlateMuted
                )
            }
            Slider(
                value = playerState.volume,
                onValueChange = playerViewModel::setVolume,
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = accent,
                    activeTrackColor = accent,
                    inactiveTrackColor = IvidsColorTokens.HairlineBorder
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}
