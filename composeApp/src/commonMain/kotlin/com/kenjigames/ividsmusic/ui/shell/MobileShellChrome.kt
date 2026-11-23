package com.kenjigames.ividsmusic.ui.shell

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.kenjigames.ividsmusic.resources.Res
import com.kenjigames.ividsmusic.resources.ic_ivids_brand
import com.kenjigames.ividsmusic.ui.components.BlurredArtworkBackdrop
import com.kenjigames.ividsmusic.ui.components.IvidsHoverStyle
import com.kenjigames.ividsmusic.ui.components.IvidsIconButton
import com.kenjigames.ividsmusic.ui.components.IvidsPlayCircleButton
import com.kenjigames.ividsmusic.ui.components.ividsTouchFeedback
import com.kenjigames.ividsmusic.ui.i18n.LocalStrings
import com.kenjigames.ividsmusic.ui.navigation.AppRoute
import com.kenjigames.ividsmusic.ui.player.PlayerUiState
import com.kenjigames.ividsmusic.ui.theme.IvidsColorTokens
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.painterResource

/**
 * Renders the aligned top navigation header on Mobile screens with back navigation, active download
 * count badge, and quick Downloader/Settings shortcuts.
 */
@Composable
internal fun MobileTopHeader(
    currentRoute: AppRoute,
    canNavigateBack: Boolean,
    activeDownloadCount: Int,
    onBack: () -> Unit,
    onOpenDownloader: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val strings = LocalStrings.current

    Column(modifier = Modifier.fillMaxWidth().background(IvidsColorTokens.ObsidianStage)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (canNavigateBack) {
                    IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = strings["back"],
                            tint = IvidsColorTokens.ChalkInk
                        )
                    }
                } else {
                    Image(
                        painter = painterResource(Res.drawable.ic_ivids_brand),
                        contentDescription = "IVIDS Music",
                        modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp))
                    )
                }
                Text(
                    text = "IVIDS",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = IvidsColorTokens.PrimaryCyan
                )
                Text(
                    text = "MUSIC",
                    style = MaterialTheme.typography.titleSmall,
                    color = IvidsColorTokens.ChalkInk
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(contentAlignment = Alignment.TopEnd) {
                    IconButton(onClick = onOpenDownloader) {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = strings["nav_downloader"],
                            tint = if (currentRoute is AppRoute.Downloader) IvidsColorTokens.ChalkInk else IvidsColorTokens.SlateMuted
                        )
                    }
                    if (activeDownloadCount > 0) {
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp, end = 6.dp)
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(IvidsColorTokens.ChalkInk),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = activeDownloadCount.coerceAtMost(9).toString(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = IvidsColorTokens.ObsidianStage
                            )
                        }
                    }
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = strings["settings"],
                        tint = if (currentRoute is AppRoute.Settings || currentRoute is AppRoute.SettingsDetail) IvidsColorTokens.ChalkInk else IvidsColorTokens.SlateMuted
                    )
                }
            }
        }
        HorizontalDivider(thickness = 1.dp, color = IvidsColorTokens.HairlineSubtle)
    }
}

/**
 * Renders the 5-destination compact bottom navigation dock for Mobile viewports (`< 600dp`) using
 * full-width tab cells and single-line `10.sp` labels so `"Your Library"` never truncates.
 */
@Composable
internal fun MobileBottomBar(
    currentRoute: AppRoute,
    onSelectRoute: (AppRoute) -> Unit
) {
    val strings = LocalStrings.current
    val items = remember {
        listOf(
            NavEntry(AppRoute.Home, "home", Icons.Rounded.Home) { it is AppRoute.Home },
            NavEntry(AppRoute.Recommended, "nav_foryou", Icons.Rounded.AutoAwesome) { it is AppRoute.Recommended },
            NavEntry(AppRoute.Search(), "search", Icons.Rounded.Search) { it is AppRoute.Search },
            NavEntry(AppRoute.Library, "library", Icons.Rounded.LibraryMusic) { it is AppRoute.Library },
            NavEntry(AppRoute.Profile, "you", Icons.Rounded.Person) { it is AppRoute.Profile }
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = IvidsColorTokens.AbyssRail
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(thickness = 1.dp, color = IvidsColorTokens.HairlineSubtle)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(64.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { entry ->
                    val selected = entry.matcher(currentRoute)
                    val indicatorColor by animateColorAsState(
                        targetValue = if (selected) IvidsColorTokens.PrimaryCyan.copy(alpha = 0.16f) else Color.Transparent,
                        animationSpec = tween(150, easing = FastOutSlowInEasing),
                        label = "BottomNavIndicator"
                    )
                    val tabInteraction = remember { MutableInteractionSource() }
                    val iconTint = if (selected) IvidsColorTokens.PrimaryCyan else IvidsColorTokens.SlateMuted
                    val labelColor = if (selected) IvidsColorTokens.ChalkInk else IvidsColorTokens.SlateMuted

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .ividsTouchFeedback(interactionSource = tabInteraction, pressedScale = 0.92f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(interactionSource = tabInteraction, indication = null) {
                                onSelectRoute(entry.route)
                            }
                            .padding(horizontal = 2.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 48.dp, height = 28.dp)
                                .clip(CircleShape)
                                .background(indicatorColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = entry.icon,
                                contentDescription = strings[entry.labelKey],
                                tint = iconTint,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = strings[entry.labelKey],
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            color = labelColor,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * Floating mini-player bar anchored above [MobileBottomBar] supporting swipe-up to open
 * the expanded sheet, horizontal swipe skip, retry on error, and `44.dp` touch action targets.
 */
@Composable
internal fun MiniPlayerBar(
    playerState: PlayerUiState,
    onExpand: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onToggleLike: () -> Unit,
    onRetry: () -> Unit,
    onSkipPrevious: () -> Unit = {},
    onSkipNext: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val track = playerState.currentTrack ?: return
    val strings = LocalStrings.current
    val density = LocalDensity.current
    var dragOffsetXPx by remember(track.id) { mutableStateOf(0f) }
    var dragOffsetYPx by remember(track.id) { mutableStateOf(0f) }
    val animatedOffsetXPx by animateFloatAsState(
        targetValue = dragOffsetXPx,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMedium),
        label = "MiniPlayerSwipeX"
    )
    val maxHorizontalPx = with(density) { 88.dp.toPx() }
    val progress = if (playerState.durationMs > 0L) {
        (playerState.positionMs.toFloat() / playerState.durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .offset { IntOffset(animatedOffsetXPx.roundToInt(), 0) }
            .pointerInput(track.id, playerState.canSkipPrevious, playerState.canSkipNext) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        dragOffsetXPx = (dragOffsetXPx + dragAmount).coerceIn(-maxHorizontalPx, maxHorizontalPx)
                    },
                    onDragEnd = {
                        val draggedDp = with(density) { dragOffsetXPx.toDp().value }
                        dragOffsetXPx = 0f
                        when (
                            resolveHorizontalSwipeAction(
                                offsetDp = draggedDp,
                                thresholdDp = 56f,
                                canSkipPrevious = playerState.canSkipPrevious,
                                canSkipNext = playerState.canSkipNext
                            )
                        ) {
                            HorizontalSwipeAction.SKIP_NEXT -> onSkipNext()
                            HorizontalSwipeAction.SKIP_PREVIOUS -> onSkipPrevious()
                            HorizontalSwipeAction.NONE -> Unit
                        }
                    },
                    onDragCancel = { dragOffsetXPx = 0f }
                )
            }
            .pointerInput(track.id) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount ->
                        dragOffsetYPx += dragAmount
                    },
                    onDragEnd = {
                        val draggedDp = with(density) { dragOffsetYPx.toDp().value }
                        dragOffsetYPx = 0f
                        if (draggedDp <= -48f) {
                            onExpand()
                        }
                    },
                    onDragCancel = { dragOffsetYPx = 0f }
                )
            }
            .shadow(14.dp, RoundedCornerShape(18.dp), ambientColor = Color.Black, spotColor = Color.Black)
            .clickable(onClick = onExpand),
        shape = RoundedCornerShape(18.dp),
        color = IvidsColorTokens.GlassSurface
    ) {
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))) {
            BlurredArtworkBackdrop(
                artworkUrl = track.coverUrl,
                tintColor = null,
                scrimAlpha = 0.74f,
                fallbackColor = IvidsColorTokens.GlassSurface,
                modifier = Modifier.matchParentSize()
            )
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(IvidsColorTokens.RecessedTrough),
                        contentAlignment = Alignment.Center
                    ) {
                        if (track.coverUrl.isNotBlank()) {
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
                            text = track.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = IvidsColorTokens.ChalkInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val errorText = playerState.errorMessage
                        Text(
                            text = errorText ?: track.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (errorText != null) IvidsColorTokens.DangerCoral else IvidsColorTokens.SlateMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (playerState.errorMessage != null) {
                        IvidsIconButton(onClick = onRetry, size = 44.dp, hoverStyle = IvidsHoverStyle.DESTRUCTIVE) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = strings["retry"],
                                tint = IvidsColorTokens.DangerCoral,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        IvidsIconButton(onClick = onToggleLike, size = 44.dp) {
                            Icon(
                                imageVector = if (playerState.isCurrentLiked) {
                                    Icons.Rounded.Favorite
                                } else {
                                    Icons.Rounded.FavoriteBorder
                                },
                                contentDescription = strings["like"],
                                tint = if (playerState.isCurrentLiked) IvidsColorTokens.DangerCoral else IvidsColorTokens.SlateMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    IvidsPlayCircleButton(
                        isPlaying = playerState.isPlaying,
                        isBuffering = playerState.isBuffering,
                        contentDescription = strings["play"],
                        onClick = onTogglePlayPause,
                        size = 44.dp,
                        iconSize = 24.dp
                    )
                }
                LinearProgressIndicator(
                    progress = { progress },
                    color = IvidsColorTokens.ChalkInk,
                    trackColor = IvidsColorTokens.RecessedTrough.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth().height(2.5.dp)
                )
            }
        }
    }
}
