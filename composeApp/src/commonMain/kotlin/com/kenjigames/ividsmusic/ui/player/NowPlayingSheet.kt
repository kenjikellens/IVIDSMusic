package com.kenjigames.ividsmusic.ui.player

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.kenjigames.ividsmusic.domain.model.Playlist
import com.kenjigames.ividsmusic.domain.model.RepeatMode
import com.kenjigames.ividsmusic.domain.model.Track
import com.kenjigames.ividsmusic.ui.components.BlurredArtworkBackdrop
import com.kenjigames.ividsmusic.ui.components.IvidsHoverStyle
import com.kenjigames.ividsmusic.ui.components.IvidsIconButton
import com.kenjigames.ividsmusic.ui.components.IvidsPlayCircleButton
import com.kenjigames.ividsmusic.ui.components.IvidsScrubber
import com.kenjigames.ividsmusic.ui.components.IvidsSegmentedBar
import com.kenjigames.ividsmusic.ui.components.IvidsTonalPill
import com.kenjigames.ividsmusic.ui.components.PlaylistPickerDialog
import com.kenjigames.ividsmusic.ui.components.formatDurationMillis
import com.kenjigames.ividsmusic.ui.i18n.LocalStrings
import com.kenjigames.ividsmusic.ui.shell.HorizontalSwipeAction
import com.kenjigames.ividsmusic.ui.shell.resolveHorizontalSwipeAction
import com.kenjigames.ividsmusic.ui.shell.shouldDismissVerticalSwipe
import com.kenjigames.ividsmusic.ui.theme.IvidsColorTokens
import com.kenjigames.ividsmusic.ui.theme.IvidsTextStyles
import com.kenjigames.ividsmusic.ui.theme.LocalLayoutMetrics
import com.kenjigames.ividsmusic.ui.theme.tvFocusable
import kotlin.math.roundToInt

/** Portrait sub-tabs shown inside [NowPlayingSheet] on compact screens to separate player and queue. */
private enum class MobileSheetTab { PLAYER, QUEUE }

/**
 * Full-screen expandable Now Playing sheet featuring [BlurredArtworkBackdrop], split-pane Desktop
 * layout, portrait Player/Queue tabs with vertical swipe-to-dismiss, cover swipe track skip, and
 * adaptive system navigation bar insets.
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
    val strings = LocalStrings.current
    val metrics = LocalLayoutMetrics.current
    val density = LocalDensity.current
    val track = playerState.currentTrack
    var showPlaylistPicker by remember { mutableStateOf(false) }
    var activeMobileTab by remember { mutableStateOf(MobileSheetTab.PLAYER) }
    var dismissOffsetPx by remember { mutableStateOf(0f) }
    val animatedDismissOffsetPx by animateFloatAsState(
        targetValue = dismissOffsetPx,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
        label = "SheetDismissOffset"
    )
    val maxDismissOffsetPx = with(density) { 260.dp.toPx() }
    val swipeDismissModifier = Modifier.pointerInput(onDismiss) {
        detectVerticalDragGestures(
            onVerticalDrag = { _, dragAmount ->
                dismissOffsetPx = (dismissOffsetPx + dragAmount).coerceIn(0f, maxDismissOffsetPx)
            },
            onDragEnd = {
                val draggedDp = with(density) { dismissOffsetPx.toDp().value }
                dismissOffsetPx = 0f
                if (shouldDismissVerticalSwipe(draggedDp)) {
                    onDismiss()
                }
            },
            onDragCancel = { dismissOffsetPx = 0f }
        )
    }

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
        modifier = modifier
            .fillMaxSize()
            .offset { IntOffset(0, animatedDismissOffsetPx.roundToInt()) },
        color = IvidsColorTokens.ObsidianStage.copy(
            alpha = (1f - (animatedDismissOffsetPx / maxDismissOffsetPx) * 0.35f).coerceIn(0.65f, 1f)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            BlurredArtworkBackdrop(
                artworkUrl = track?.coverUrl,
                tintColor = null,
                scrimAlpha = 0.62f,
                fallbackColor = IvidsColorTokens.ObsidianStage,
                modifier = Modifier.matchParentSize()
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = if (metrics.isCompactMobile) 16.dp else 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Visual drag pill + top bar gesture handle
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 1140.dp)
                        .then(swipeDismissModifier),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (metrics.isCompactMobile) {
                        Box(
                            modifier = Modifier
                                .padding(bottom = 8.dp)
                                .size(width = 40.dp, height = 4.dp)
                                .clip(CircleShape)
                                .background(IvidsColorTokens.ChalkInk.copy(alpha = 0.28f))
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IvidsIconButton(onClick = onDismiss, size = 44.dp) {
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
                        IvidsIconButton(
                            onClick = {
                                if (track != null) {
                                    onDismiss()
                                    onOpenSongDetail(track.id)
                                }
                            },
                            enabled = track != null,
                            size = 44.dp
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = strings["more_info"],
                                tint = IvidsColorTokens.ChalkInk
                            )
                        }
                    }
                }

                BoxWithConstraints(
                    modifier = Modifier.weight(1f).widthIn(max = 1140.dp).fillMaxWidth()
                ) {
                    val isWideSplit = maxWidth >= 860.dp
                    val portraitCoverLimit = if (maxHeight < 680.dp) 188.dp else 276.dp

                    if (isWideSplit) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(32.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.weight(0.54f).fillMaxHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                NowPlayingHeroCard(
                                    track = track,
                                    playerState = playerState,
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

                            Surface(
                                modifier = Modifier.weight(0.46f).fillMaxHeight(0.88f),
                                shape = RoundedCornerShape(22.dp),
                                color = IvidsColorTokens.GlassSurface.copy(alpha = 0.78f)
                            ) {
                                Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                                    NowPlayingQueueHeader(
                                        hasItems = playerState.queue.isNotEmpty(),
                                        onClear = playerViewModel::clearQueue
                                    )
                                    HorizontalDivider(
                                        color = IvidsColorTokens.HairlineSubtle,
                                        modifier = Modifier.padding(vertical = 10.dp)
                                    )
                                    if (playerState.queue.isEmpty()) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text(
                                                text = strings["queue_empty"],
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = IvidsColorTokens.SlateMuted
                                            )
                                        }
                                    } else {
                                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            itemsIndexed(
                                                items = playerState.queue,
                                                key = { idx, item -> "${idx}_${item.id}" }
                                            ) { index, item ->
                                                NowPlayingQueueRow(
                                                    index = index,
                                                    item = item,
                                                    isActive = index == playerState.currentIndex,
                                                    onJump = { playerViewModel.jumpToQueueIndex(index) },
                                                    onRemove = { playerViewModel.removeFromQueue(index) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize().widthIn(max = 760.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            IvidsSegmentedBar(
                                items = listOf(MobileSheetTab.PLAYER, MobileSheetTab.QUEUE),
                                selected = activeMobileTab,
                                labelProvider = { tab ->
                                    when (tab) {
                                        MobileSheetTab.PLAYER -> strings["tab_player"]
                                        MobileSheetTab.QUEUE -> "${strings["tab_queue"]} (${playerState.queue.size})"
                                    }
                                },
                                onSelect = { activeMobileTab = it }
                            )

                            when (activeMobileTab) {
                                MobileSheetTab.PLAYER -> {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                            .then(swipeDismissModifier)
                                            .verticalScroll(rememberScrollState())
                                            .padding(vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        NowPlayingHeroCard(
                                            track = track,
                                            playerState = playerState,
                                            onOpenArtist = {
                                                if (track != null) {
                                                    onDismiss()
                                                    onOpenArtist(track.artistId, track.artist)
                                                }
                                            },
                                            onOpenPlaylistPicker = { showPlaylistPicker = true },
                                            playerViewModel = playerViewModel,
                                            showVolumeControls = !metrics.isCompactMobile,
                                            maxArtworkSize = portraitCoverLimit,
                                            onSwipePrevious = playerViewModel::previous,
                                            onSwipeNext = playerViewModel::next
                                        )
                                    }
                                }

                                MobileSheetTab.QUEUE -> {
                                    Surface(
                                        modifier = Modifier.weight(1f).fillMaxWidth(),
                                        shape = RoundedCornerShape(20.dp),
                                        color = IvidsColorTokens.GlassSurface.copy(alpha = 0.78f)
                                    ) {
                                        Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                                            NowPlayingQueueHeader(
                                                hasItems = playerState.queue.isNotEmpty(),
                                                onClear = playerViewModel::clearQueue
                                            )
                                            HorizontalDivider(
                                                color = IvidsColorTokens.HairlineSubtle,
                                                modifier = Modifier.padding(vertical = 8.dp)
                                            )
                                            if (playerState.queue.isEmpty()) {
                                                Box(
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = strings["queue_empty"],
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = IvidsColorTokens.SlateMuted
                                                    )
                                                }
                                            } else {
                                                LazyColumn(
                                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                                    contentPadding = PaddingValues(bottom = 12.dp)
                                                ) {
                                                    itemsIndexed(
                                                        items = playerState.queue,
                                                        key = { idx, item -> "${idx}_${item.id}" }
                                                    ) { index, item ->
                                                        NowPlayingQueueRow(
                                                            index = index,
                                                            item = item,
                                                            isActive = index == playerState.currentIndex,
                                                            onJump = { playerViewModel.jumpToQueueIndex(index) },
                                                            onRemove = { playerViewModel.removeFromQueue(index) }
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
            }
        }
    }
}

/**
 * Renders the Next Up header and optional Clear Queue button inside [NowPlayingSheet].
 */
@Composable
private fun NowPlayingQueueHeader(
    hasItems: Boolean,
    onClear: () -> Unit
) {
    val strings = LocalStrings.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = strings["next_up"],
            style = MaterialTheme.typography.titleLarge,
            color = IvidsColorTokens.ChalkInk
        )
        if (hasItems) {
            TextButton(onClick = onClear) {
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

/**
 * Renders a single borderless queue item inside [NowPlayingSheet].
 */
@Composable
private fun NowPlayingQueueRow(
    index: Int,
    item: Track,
    isActive: Boolean,
    onJump: () -> Unit,
    onRemove: () -> Unit
) {
    val strings = LocalStrings.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .tvFocusable(shape = RoundedCornerShape(12.dp))
            .clickable(onClick = onJump),
        shape = RoundedCornerShape(12.dp),
        color = if (isActive) Color(0x1CFFFFFF) else Color(0x0AFFFFFF)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = (index + 1).toString(),
                style = IvidsTextStyles.tabularBadge(),
                color = if (isActive) IvidsColorTokens.ChalkInk else IvidsColorTokens.SlateMuted,
                modifier = Modifier.width(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = IvidsColorTokens.ChalkInk,
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
                onClick = onRemove,
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

/**
 * Renders the central artwork (supporting horizontal swipe skip), [IvidsScrubber] seek bar,
 * transport controls, quick actions, and optional software volume controls inside [NowPlayingSheet].
 */
@Composable
private fun NowPlayingHeroCard(
    track: Track?,
    playerState: PlayerUiState,
    onOpenArtist: () -> Unit,
    onOpenPlaylistPicker: () -> Unit,
    playerViewModel: PlayerViewModel,
    showVolumeControls: Boolean = true,
    maxArtworkSize: Dp = 310.dp,
    onSwipePrevious: (() -> Unit)? = null,
    onSwipeNext: (() -> Unit)? = null
) {
    val strings = LocalStrings.current
    val density = LocalDensity.current
    var dragPositionMs by remember(track?.id) { mutableStateOf<Float?>(null) }
    var coverSwipePx by remember(track?.id) { mutableStateOf(0f) }
    val animatedCoverSwipePx by animateFloatAsState(
        targetValue = coverSwipePx,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMedium),
        label = "CoverSwipeOffset"
    )
    val maxSwipePx = with(density) { 96.dp.toPx() }
    val durationMs = playerState.durationMs.coerceAtLeast(1L)
    val effectivePos = dragPositionMs ?: playerState.positionMs.toFloat().coerceIn(0f, durationMs.toFloat())

    Column(
        modifier = Modifier.fillMaxWidth().widthIn(max = 460.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = maxArtworkSize)
                .fillMaxWidth(0.76f)
                .aspectRatio(1f)
                .offset { IntOffset(animatedCoverSwipePx.roundToInt(), 0) }
                .pointerInput(track?.id, playerState.canSkipPrevious, playerState.canSkipNext) {
                    if (onSwipeNext == null && onSwipePrevious == null) return@pointerInput
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            coverSwipePx = (coverSwipePx + dragAmount).coerceIn(-maxSwipePx, maxSwipePx)
                        },
                        onDragEnd = {
                            val swipeDp = with(density) { coverSwipePx.toDp().value }
                            coverSwipePx = 0f
                            when (
                                resolveHorizontalSwipeAction(
                                    offsetDp = swipeDp,
                                    thresholdDp = 72f,
                                    canSkipPrevious = playerState.canSkipPrevious,
                                    canSkipNext = playerState.canSkipNext
                                )
                            ) {
                                HorizontalSwipeAction.SKIP_NEXT -> onSwipeNext?.invoke()
                                HorizontalSwipeAction.SKIP_PREVIOUS -> onSwipePrevious?.invoke()
                                HorizontalSwipeAction.NONE -> Unit
                            }
                        },
                        onDragCancel = { coverSwipePx = 0f }
                    )
                }
                .shadow(28.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black, spotColor = Color.Black)
                .clip(RoundedCornerShape(24.dp))
                .background(IvidsColorTokens.GlassSurface),
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
                    tint = IvidsColorTokens.SlateMuted,
                    modifier = Modifier.size(72.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

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
            color = IvidsColorTokens.SlateMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clickable(enabled = track != null, onClick = onOpenArtist)
        )

        Spacer(modifier = Modifier.height(18.dp))

        IvidsScrubber(
            value = effectivePos,
            onValueChange = { dragPositionMs = it },
            onValueChangeFinished = {
                dragPositionMs?.let { playerViewModel.seekTo(it.toLong()) }
                dragPositionMs = null
            },
            valueRange = 0f..durationMs.toFloat(),
            alwaysShowThumb = true,
            trackHeight = 5.dp,
            enabled = track != null,
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

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IvidsIconButton(
                onClick = playerViewModel::toggleShuffle,
                size = 44.dp,
                hoverStyle = if (playerState.isShuffleEnabled) IvidsHoverStyle.PRIMARY_ACCENT else IvidsHoverStyle.NEUTRAL
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shuffle,
                    contentDescription = strings["shuffle"],
                    tint = if (playerState.isShuffleEnabled) IvidsColorTokens.PrimaryCyan else IvidsColorTokens.SlateMuted
                )
            }
            IvidsIconButton(onClick = playerViewModel::previous, size = 48.dp) {
                Icon(
                    imageVector = Icons.Rounded.SkipPrevious,
                    contentDescription = strings["previous"],
                    tint = IvidsColorTokens.ChalkInk,
                    modifier = Modifier.size(32.dp)
                )
            }
            IvidsPlayCircleButton(
                isPlaying = playerState.isPlaying,
                isBuffering = playerState.isBuffering,
                contentDescription = strings["play"],
                onClick = playerViewModel::togglePlayPause,
                size = 64.dp,
                iconSize = 34.dp
            )
            IvidsIconButton(onClick = playerViewModel::next, size = 48.dp) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = strings["next"],
                    tint = IvidsColorTokens.ChalkInk,
                    modifier = Modifier.size(32.dp)
                )
            }
            IvidsIconButton(
                onClick = playerViewModel::cycleRepeatMode,
                size = 44.dp,
                hoverStyle = if (playerState.repeatMode != RepeatMode.OFF) IvidsHoverStyle.PRIMARY_ACCENT else IvidsHoverStyle.NEUTRAL
            ) {
                Icon(
                    imageVector = if (playerState.repeatMode == RepeatMode.ONE) {
                        Icons.Rounded.RepeatOne
                    } else {
                        Icons.Rounded.Repeat
                    },
                    contentDescription = "Repeat",
                    tint = if (playerState.repeatMode != RepeatMode.OFF) IvidsColorTokens.PrimaryCyan else IvidsColorTokens.SlateMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IvidsIconButton(onClick = playerViewModel::toggleLikeCurrent, size = 44.dp) {
                Icon(
                    imageVector = if (playerState.isCurrentLiked) {
                        Icons.Rounded.Favorite
                    } else {
                        Icons.Rounded.FavoriteBorder
                    },
                    contentDescription = strings["like"],
                    tint = if (playerState.isCurrentLiked) IvidsColorTokens.DangerCoral else IvidsColorTokens.SlateMuted
                )
            }
            IvidsIconButton(onClick = playerViewModel::dislikeCurrent, size = 44.dp) {
                Icon(
                    imageVector = Icons.Rounded.ThumbDownOffAlt,
                    contentDescription = strings["dislike"],
                    tint = IvidsColorTokens.SlateMuted
                )
            }
            IvidsIconButton(
                onClick = onOpenPlaylistPicker,
                enabled = track != null,
                size = 44.dp
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                    contentDescription = strings["add_to_playlist"],
                    tint = IvidsColorTokens.ChalkInk
                )
            }
            IvidsTonalPill(
                onClick = { playerViewModel.downloadCurrent() },
                baseColor = IvidsColorTokens.GlassSurface.copy(alpha = 0.85f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)
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
                        IvidsColorTokens.ChalkInk
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

        if (showVolumeControls) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val soundActive = !playerState.isMuted
                IvidsIconButton(
                    onClick = playerViewModel::toggleMute,
                    size = 40.dp,
                    hoverStyle = if (soundActive) IvidsHoverStyle.PRIMARY_ACCENT else IvidsHoverStyle.NEUTRAL
                ) {
                    Icon(
                        imageVector = if (playerState.isMuted) {
                            Icons.AutoMirrored.Rounded.VolumeOff
                        } else {
                            Icons.AutoMirrored.Rounded.VolumeUp
                        },
                        contentDescription = strings["volume"],
                        tint = if (soundActive) IvidsColorTokens.PrimaryCyan else IvidsColorTokens.SlateMuted
                    )
                }
                IvidsScrubber(
                    value = playerState.volume,
                    onValueChange = playerViewModel::setVolume,
                    valueRange = 0f..1f,
                    alwaysShowThumb = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
