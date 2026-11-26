package com.kenjigames.ividsmusic.ui.shell

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.kenjigames.ividsmusic.domain.model.RepeatMode
import com.kenjigames.ividsmusic.ui.components.BlurredArtworkBackdrop
import com.kenjigames.ividsmusic.ui.components.IvidsHoverStyle
import com.kenjigames.ividsmusic.ui.components.IvidsIconButton
import com.kenjigames.ividsmusic.ui.components.IvidsPlayCircleButton
import com.kenjigames.ividsmusic.ui.components.IvidsScrubber
import com.kenjigames.ividsmusic.ui.components.IvidsTonalPill
import com.kenjigames.ividsmusic.ui.components.formatDurationMillis
import com.kenjigames.ividsmusic.ui.components.ividsHoverZoom
import com.kenjigames.ividsmusic.ui.i18n.LocalStrings
import com.kenjigames.ividsmusic.ui.player.PlayerUiState
import com.kenjigames.ividsmusic.ui.player.PlayerViewModel
import com.kenjigames.ividsmusic.ui.theme.IvidsColorTokens
import com.kenjigames.ividsmusic.ui.theme.IvidsTextStyles
import com.kenjigames.ividsmusic.ui.theme.tvFocusable

/**
 * Persistent 88dp hi-fi player deck across Desktop and Android TV featuring blurred cover-artwork
 * backdrop, unified [IvidsIconButton] / [IvidsPlayCircleButton] controls, and [IvidsScrubber] bars.
 */
@Composable
internal fun DesktopPlayerDeck(
    playerState: PlayerUiState,
    queueOpen: Boolean,
    onToggleQueue: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    onOpenArtist: (String, String) -> Unit,
    onOpenSongDetail: (String) -> Unit,
    playerViewModel: PlayerViewModel
) {
    val strings = LocalStrings.current
    val track = playerState.currentTrack
    var dragPos by remember(track?.id) { mutableStateOf<Float?>(null) }
    val maxDur = playerState.durationMs.coerceAtLeast(1L).toFloat()
    val sliderPos = dragPos ?: playerState.positionMs.toFloat().coerceIn(0f, maxDur)
    val hasPlayable = track != null || playerState.queue.isNotEmpty()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .clipToBounds()
            .background(IvidsColorTokens.AbyssRail)
    ) {
        if (track != null) {
            BlurredArtworkBackdrop(
                artworkUrl = track.coverUrl,
                tintColor = null,
                scrimAlpha = 0.78f,
                fallbackColor = IvidsColorTokens.AbyssRail,
                modifier = Modifier.matchParentSize()
            )
        }

        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.weight(0.28f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(IvidsColorTokens.GlassSurface)
                        .then(if (track != null) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
                        .tvFocusable(shape = RoundedCornerShape(10.dp))
                        .clickable(enabled = track != null, onClick = onOpenNowPlaying),
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
                        Icon(Icons.Rounded.MusicNote, null, tint = IvidsColorTokens.SlateMuted)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track?.title ?: strings["song_no_track"],
                        style = MaterialTheme.typography.titleMedium,
                        color = if (track != null) IvidsColorTokens.ChalkInk else IvidsColorTokens.SlateMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .then(if (track != null) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
                            .clickable(enabled = track != null, onClick = onOpenNowPlaying)
                    )
                    if (track != null) {
                        val errorText = playerState.errorMessage
                        if (errorText != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = errorText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = IvidsColorTokens.DangerCoral,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                IvidsIconButton(
                                    onClick = playerViewModel::retryCurrent,
                                    size = 26.dp,
                                    hoverStyle = IvidsHoverStyle.DESTRUCTIVE
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = strings["retry"],
                                        tint = IvidsColorTokens.DangerCoral,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                IvidsIconButton(
                                    onClick = playerViewModel::dismissError,
                                    size = 26.dp
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = strings["close"],
                                        tint = IvidsColorTokens.SlateMuted,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = track.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = IvidsColorTokens.SlateMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .pointerHoverIcon(PointerIcon.Hand)
                                    .clickable { onOpenArtist(track.artistId, track.artist) }
                            )
                        }
                    }
                }
                if (track != null) {
                    IvidsIconButton(onClick = playerViewModel::toggleLikeCurrent, size = 34.dp) {
                        Icon(
                            imageVector = if (playerState.isCurrentLiked) {
                                Icons.Rounded.Favorite
                            } else {
                                Icons.Rounded.FavoriteBorder
                            },
                            contentDescription = strings["like"],
                            tint = if (playerState.isCurrentLiked) IvidsColorTokens.DangerCoral else IvidsColorTokens.SlateMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(0.42f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IvidsIconButton(
                        onClick = playerViewModel::toggleShuffle,
                        size = 34.dp,
                        hoverStyle = if (playerState.isShuffleEnabled) IvidsHoverStyle.PRIMARY_ACCENT else IvidsHoverStyle.NEUTRAL
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = strings["shuffle"],
                            tint = if (playerState.isShuffleEnabled) IvidsColorTokens.PrimaryCyan else IvidsColorTokens.SlateMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IvidsIconButton(
                        onClick = playerViewModel::previous,
                        enabled = hasPlayable,
                        size = 36.dp
                    ) {
                        Icon(Icons.Rounded.SkipPrevious, strings["previous"], tint = IvidsColorTokens.ChalkInk)
                    }
                    IvidsPlayCircleButton(
                        isPlaying = playerState.isPlaying,
                        isBuffering = playerState.isBuffering,
                        contentDescription = strings["play"],
                        onClick = playerViewModel::togglePlayPause,
                        enabled = hasPlayable,
                        size = 40.dp,
                        iconSize = 22.dp
                    )
                    IvidsIconButton(
                        onClick = playerViewModel::next,
                        enabled = hasPlayable,
                        size = 36.dp
                    ) {
                        Icon(Icons.Rounded.SkipNext, strings["next"], tint = IvidsColorTokens.ChalkInk)
                    }
                    IvidsIconButton(
                        onClick = playerViewModel::cycleRepeatMode,
                        size = 34.dp,
                        hoverStyle = if (playerState.repeatMode != RepeatMode.OFF) IvidsHoverStyle.PRIMARY_ACCENT else IvidsHoverStyle.NEUTRAL
                    ) {
                        Icon(
                            imageVector = if (playerState.repeatMode == RepeatMode.ONE) {
                                Icons.Rounded.RepeatOne
                            } else {
                                Icons.Rounded.Repeat
                            },
                            contentDescription = "Repeat",
                            tint = if (playerState.repeatMode != RepeatMode.OFF) IvidsColorTokens.PrimaryCyan else IvidsColorTokens.SlateMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = formatDurationMillis(sliderPos.toLong()),
                        style = IvidsTextStyles.tabularTime(),
                        color = IvidsColorTokens.SlateMuted
                    )
                    IvidsScrubber(
                        value = sliderPos,
                        onValueChange = { dragPos = it },
                        onValueChangeFinished = {
                            dragPos?.let { playerViewModel.seekTo(it.toLong()) }
                            dragPos = null
                        },
                        valueRange = 0f..maxDur,
                        enabled = track != null,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formatDurationMillis(playerState.durationMs),
                        style = IvidsTextStyles.tabularTime(),
                        color = IvidsColorTokens.SlateMuted
                    )
                }
            }

            Row(
                modifier = Modifier.weight(0.30f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                if (track != null) {
                    IvidsIconButton(onClick = { playerViewModel.downloadCurrent() }, size = 34.dp) {
                        Icon(
                            imageVector = if (playerState.isCurrentDownloaded) {
                                Icons.Rounded.DownloadDone
                            } else {
                                Icons.Rounded.Download
                            },
                            contentDescription = strings["download_track"],
                            tint = if (playerState.isCurrentDownloaded) IvidsColorTokens.SuccessMint else IvidsColorTokens.SlateMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IvidsIconButton(onClick = { onOpenSongDetail(track.id) }, size = 34.dp) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = strings["more_info"],
                            tint = IvidsColorTokens.SlateMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                IvidsIconButton(
                    onClick = onToggleQueue,
                    size = 34.dp,
                    hoverStyle = if (queueOpen) IvidsHoverStyle.PRIMARY_ACCENT else IvidsHoverStyle.NEUTRAL
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                        contentDescription = strings["queue"],
                        tint = if (queueOpen) IvidsColorTokens.PrimaryCyan else IvidsColorTokens.SlateMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
                val soundActive = !playerState.isMuted
                IvidsIconButton(
                    onClick = playerViewModel::toggleMute,
                    size = 34.dp,
                    hoverStyle = if (soundActive) IvidsHoverStyle.PRIMARY_ACCENT else IvidsHoverStyle.NEUTRAL
                ) {
                    Icon(
                        imageVector = if (playerState.isMuted) {
                            Icons.AutoMirrored.Rounded.VolumeOff
                        } else {
                            Icons.AutoMirrored.Rounded.VolumeUp
                        },
                        contentDescription = strings["volume"],
                        tint = if (soundActive) IvidsColorTokens.PrimaryCyan else IvidsColorTokens.SlateMuted,
                        modifier = Modifier.size(19.dp)
                    )
                }
                IvidsScrubber(
                    value = playerState.volume,
                    onValueChange = playerViewModel::setVolume,
                    valueRange = 0f..1f,
                    modifier = Modifier.widthIn(min = 64.dp, max = 96.dp).fillMaxWidth(0.45f)
                )
            }
        }
    }
}

/**
 * Collapsible borderless right-hand queue drawer on Desktop showing Now Playing, reorderable
 * Next Up items with cover art, and an Autoplay Radio toggle.
 */
@Composable
internal fun QueueSideSheet(
    playerState: PlayerUiState,
    onJumpTo: (Int) -> Unit,
    onMoveTrack: (Int, Int) -> Unit,
    onRemove: (Int) -> Unit,
    onClear: () -> Unit,
    onToggleAutoplay: () -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalStrings.current

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(332.dp)
            .background(IvidsColorTokens.AbyssRail)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings["queue"],
                    style = MaterialTheme.typography.titleLarge,
                    color = IvidsColorTokens.ChalkInk
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (playerState.queue.isNotEmpty()) {
                        TextButton(onClick = onClear) {
                            Icon(Icons.Rounded.DeleteSweep, null, tint = IvidsColorTokens.SlateMuted, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings["clear_queue"], color = IvidsColorTokens.SlateMuted)
                        }
                    }
                    IvidsIconButton(onClick = onClose, size = 34.dp) {
                        Icon(Icons.Rounded.Close, strings["close"], tint = IvidsColorTokens.SlateMuted)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            IvidsTonalPill(
                onClick = onToggleAutoplay,
                baseColor = if (playerState.autoplayEnabled) IvidsColorTokens.PrimaryCyan.copy(alpha = 0.16f) else IvidsColorTokens.GlassSurface,
                hoverStyle = if (playerState.autoplayEnabled) IvidsHoverStyle.PRIMARY_ACCENT else IvidsHoverStyle.NEUTRAL,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = if (playerState.autoplayEnabled) IvidsColorTokens.PrimaryCyan else IvidsColorTokens.SlateMuted,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = strings["autoplay_radio"],
                    style = MaterialTheme.typography.labelLarge,
                    color = if (playerState.autoplayEnabled) IvidsColorTokens.PrimaryCyan else IvidsColorTokens.SlateMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            HorizontalDivider(color = IvidsColorTokens.HairlineSubtle, modifier = Modifier.padding(vertical = 10.dp))

            if (playerState.queue.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(strings["queue_empty"], style = MaterialTheme.typography.bodyMedium, color = IvidsColorTokens.SlateMuted)
                }
            } else {
                val activeIndex = playerState.currentIndex.coerceIn(-1, playerState.queue.lastIndex)
                val nowPlayingTrack = playerState.queue.getOrNull(activeIndex)
                val upNextEntries = playerState.queue.mapIndexed { idx, track -> idx to track }
                    .filter { (idx, _) -> idx != activeIndex }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (nowPlayingTrack != null) {
                        item(key = "q_hdr_now") {
                            Text(
                                text = strings["now_playing_section"],
                                style = MaterialTheme.typography.labelMedium,
                                color = IvidsColorTokens.SlateMuted,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                        item(key = "q_now_${nowPlayingTrack.id}") {
                            QueueTrackCard(
                                item = nowPlayingTrack,
                                isActive = true,
                                canMoveUp = false,
                                canMoveDown = false,
                                onJump = { onJumpTo(activeIndex) },
                                onMoveUp = {},
                                onMoveDown = {},
                                onRemove = { onRemove(activeIndex) }
                            )
                        }
                    }

                    if (upNextEntries.isNotEmpty()) {
                        item(key = "q_hdr_next") {
                            Text(
                                text = strings["next_up_section"],
                                style = MaterialTheme.typography.labelMedium,
                                color = IvidsColorTokens.SlateMuted,
                                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                            )
                        }
                        itemsIndexed(upNextEntries, key = { _, pair -> "q_next_${pair.first}_${pair.second.id}" }) { _, (idx, item) ->
                            QueueTrackCard(
                                item = item,
                                isActive = false,
                                canMoveUp = idx > 0 && idx - 1 != activeIndex,
                                canMoveDown = idx < playerState.queue.lastIndex && idx + 1 != activeIndex,
                                onJump = { onJumpTo(idx) },
                                onMoveUp = { onMoveTrack(idx, idx - 1) },
                                onMoveDown = { onMoveTrack(idx, idx + 1) },
                                onRemove = { onRemove(idx) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Renders a single borderless queue track card with artwork thumbnail, reorder arrows, and remove button.
 */
@Composable
private fun QueueTrackCard(
    item: com.kenjigames.ividsmusic.domain.model.Track,
    isActive: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onJump: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    val strings = LocalStrings.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val bg by animateColorAsState(
        targetValue = if (isActive) Color(0xFF1A1A1A) else IvidsColorTokens.GlassSurface,
        animationSpec = tween(160, easing = FastOutSlowInEasing),
        label = "QueueCardBg"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .pointerHoverIcon(PointerIcon.Hand)
            .hoverable(interactionSource)
            .tvFocusable(shape = RoundedCornerShape(10.dp))
            .clickable(onClick = onJump),
        shape = RoundedCornerShape(10.dp),
        color = bg
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .ividsHoverZoom(hovered = isHovered, shape = RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(IvidsColorTokens.RecessedTrough),
                contentAlignment = Alignment.Center
            ) {
                if (item.coverUrl.isNotBlank()) {
                    AsyncImage(
                        model = item.coverUrl,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Rounded.MusicNote, null, tint = IvidsColorTokens.SlateMuted, modifier = Modifier.size(18.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isActive) IvidsColorTokens.PrimaryCyan else IvidsColorTokens.ChalkInk,
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
            if (isActive) {
                Icon(
                    imageVector = Icons.Rounded.GraphicEq,
                    contentDescription = null,
                    tint = IvidsColorTokens.PrimaryCyan,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                if (canMoveUp) {
                    IconButton(onClick = onMoveUp, modifier = Modifier.size(26.dp)) {
                        Icon(Icons.Rounded.ArrowUpward, strings["move_up"], tint = IvidsColorTokens.SlateMuted, modifier = Modifier.size(14.dp))
                    }
                }
                if (canMoveDown) {
                    IconButton(onClick = onMoveDown, modifier = Modifier.size(26.dp)) {
                        Icon(Icons.Rounded.ArrowDownward, strings["move_down"], tint = IvidsColorTokens.SlateMuted, modifier = Modifier.size(14.dp))
                    }
                }
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(26.dp)) {
                Icon(Icons.Rounded.Close, strings["remove"], tint = IvidsColorTokens.SlateMuted, modifier = Modifier.size(14.dp))
            }
        }
    }
}
