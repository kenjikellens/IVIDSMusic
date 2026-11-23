package com.kenjigames.ividsmusic.ui.shell

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kenjigames.ividsmusic.domain.model.LayoutMode
import com.kenjigames.ividsmusic.domain.model.Playlist
import com.kenjigames.ividsmusic.ui.navigation.AppNavigator
import com.kenjigames.ividsmusic.ui.navigation.AppRoute
import com.kenjigames.ividsmusic.ui.navigation.NavTransitionType
import com.kenjigames.ividsmusic.ui.player.NowPlayingSheet
import com.kenjigames.ividsmusic.ui.player.PlayerUiState
import com.kenjigames.ividsmusic.ui.player.PlayerViewModel
import com.kenjigames.ividsmusic.ui.theme.IvidsColorTokens
import com.kenjigames.ividsmusic.ui.theme.IvidsLayoutMetrics
import com.kenjigames.ividsmusic.ui.theme.LocalLayoutMetrics

/** Outcome of evaluating a horizontal drag gesture on MiniPlayerBar or cover artwork. */
internal enum class HorizontalSwipeAction { NONE, SKIP_NEXT, SKIP_PREVIOUS }

/**
 * Resolves the responsive [IvidsLayoutMetrics] for [shellMode], preserving Desktop/TV geometry
 * while tightening paddings, card widths, and touch affordances on compact phones.
 */
internal fun resolveLayoutMetrics(shellMode: ShellMode): IvidsLayoutMetrics =
    if (shellMode == ShellMode.MOBILE) {
        IvidsLayoutMetrics(
            isCompactMobile = true,
            screenHorizontalPadding = 16.dp,
            contentBottomClearance = 136.dp,
            cardWidth = 148.dp,
            showTouchPlayFab = true
        )
    } else {
        IvidsLayoutMetrics()
    }

/**
 * Evaluates whether a horizontal drag distance [offsetDp] exceeds [thresholdDp] toward an available
 * next or previous track target.
 */
internal fun resolveHorizontalSwipeAction(
    offsetDp: Float,
    thresholdDp: Float,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean
): HorizontalSwipeAction = when {
    offsetDp <= -thresholdDp && canSkipNext -> HorizontalSwipeAction.SKIP_NEXT
    offsetDp >= thresholdDp && canSkipPrevious -> HorizontalSwipeAction.SKIP_PREVIOUS
    else -> HorizontalSwipeAction.NONE
}

/**
 * Returns true when a downward vertical swipe distance [offsetDp] meets the [thresholdDp] to
 * dismiss the expanded Now Playing sheet.
 */
internal fun shouldDismissVerticalSwipe(offsetDp: Float, thresholdDp: Float = 110f): Boolean =
    offsetDp >= thresholdDp

/**
 * Adaptive form-factor shell modes governing navigation chrome, player dock, and D-pad rings.
 */
enum class ShellMode {
    MOBILE,
    DESKTOP,
    TV
}

/**
 * Resolves the active [ShellMode] from user [layoutMode] preference, device Leanback status, and
 * unscaled viewport [widthDp].
 */
fun resolveShellMode(layoutMode: LayoutMode, isTvDevice: Boolean, widthDp: Dp): ShellMode =
    when (layoutMode) {
        LayoutMode.MOBILE -> ShellMode.MOBILE
        LayoutMode.DESKTOP -> ShellMode.DESKTOP
        LayoutMode.TV -> ShellMode.TV
        LayoutMode.AUTO -> when {
            isTvDevice -> ShellMode.TV
            widthDp < 600.dp -> ShellMode.MOBILE
            else -> ShellMode.DESKTOP
        }
    }

/** Describes a primary sidebar or bottom navigation tab paired with its route matcher. */
internal data class NavEntry(
    val route: AppRoute,
    val labelKey: String,
    val icon: ImageVector,
    val matcher: (AppRoute) -> Boolean
)

/**
 * Adaptive root layout container providing [LocalLayoutMetrics] and rendering Desktop AbyssRail +
 * PlayerDeck, Mobile BottomBar + swipeable MiniPlayerBar, or TV navigation drawer.
 */
@Composable
fun AdaptiveAppShell(
    shellMode: ShellMode,
    currentRoute: AppRoute,
    canNavigateBack: Boolean,
    navigator: AppNavigator,
    playerState: PlayerUiState,
    playerViewModel: PlayerViewModel,
    playlists: List<Playlist>,
    searchFocusTrigger: Int,
    onHeaderSearchSubmit: (String) -> Unit,
    onCreatePlaylist: () -> Unit,
    modifier: Modifier = Modifier,
    activeDownloadCount: Int = 0,
    nowPlayingExpanded: Boolean? = null,
    onNowPlayingExpandedChange: ((Boolean) -> Unit)? = null,
    content: @Composable (AppRoute, ShellMode) -> Unit
) {
    var internalNowPlayingExpanded by remember { mutableStateOf(false) }
    val isNowPlayingExpanded = nowPlayingExpanded ?: internalNowPlayingExpanded
    val setNowPlayingExpanded: (Boolean) -> Unit = { expanded ->
        internalNowPlayingExpanded = expanded
        onNowPlayingExpandedChange?.invoke(expanded)
    }
    var desktopQueueOpen by remember { mutableStateOf(false) }
    val layoutMetrics = remember(shellMode) { resolveLayoutMetrics(shellMode) }
    val transitionType by navigator.transitionType.collectAsState()

    CompositionLocalProvider(LocalLayoutMetrics provides layoutMetrics) {
        Box(modifier = modifier.fillMaxSize().background(IvidsColorTokens.ObsidianStage)) {
            when (shellMode) {
                ShellMode.MOBILE -> {
                    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                        MobileTopHeader(
                            currentRoute = currentRoute,
                            canNavigateBack = canNavigateBack,
                            activeDownloadCount = activeDownloadCount,
                            onBack = { navigator.navigateBack() },
                            onOpenDownloader = { navigator.navigateTo(AppRoute.Downloader) },
                            onOpenSettings = { navigator.navigateTo(AppRoute.Settings) }
                        )
                        Box(modifier = Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
                            AnimatedContent(
                                targetState = currentRoute,
                                contentKey = { it },
                                transitionSpec = {
                                    when (transitionType) {
                                        NavTransitionType.PUSH -> (
                                            slideInHorizontally(tween(260, easing = FastOutSlowInEasing)) { it } +
                                                fadeIn(tween(220))
                                            ) togetherWith (
                                            slideOutHorizontally(tween(260, easing = FastOutSlowInEasing)) { -it / 4 } +
                                                fadeOut(tween(180))
                                            )
                                        NavTransitionType.POP -> (
                                            slideInHorizontally(tween(240, easing = FastOutSlowInEasing)) { -it / 4 } +
                                                fadeIn(tween(200))
                                            ) togetherWith (
                                            slideOutHorizontally(tween(240, easing = FastOutSlowInEasing)) { it } +
                                                fadeOut(tween(180))
                                            )
                                        NavTransitionType.TAB, NavTransitionType.NONE ->
                                            fadeIn(tween(160)) togetherWith fadeOut(tween(120))
                                    }.using(SizeTransform(clip = false))
                                },
                                label = "MobileRouteTransition",
                                modifier = Modifier.fillMaxSize()
                            ) { animatedRoute ->
                                content(animatedRoute, shellMode)
                            }
                            MiniPlayerHost(
                                playerViewModel = playerViewModel,
                                onExpand = { setNowPlayingExpanded(true) },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }
                        MobileBottomBar(
                            currentRoute = currentRoute,
                            onSelectRoute = { navigator.switchRootTab(it) }
                        )
                    }
                }

                ShellMode.DESKTOP, ShellMode.TV -> {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val isExpandedRail = shellMode == ShellMode.TV || maxWidth >= 880.dp
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                SidebarNavigationRail(
                                    currentRoute = currentRoute,
                                    expanded = isExpandedRail,
                                    isTv = shellMode == ShellMode.TV,
                                    onSelectRoute = { navigator.switchRootTab(it) }
                                )
                                VerticalDivider(
                                    thickness = 1.dp,
                                    color = IvidsColorTokens.HairlineSubtle
                                )

                                Box(modifier = Modifier.weight(1f).fillMaxHeight().clipToBounds()) {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        DesktopTopBar(
                                            canNavigateBack = canNavigateBack,
                                            searchFocusTrigger = searchFocusTrigger,
                                            onBack = { navigator.navigateBack() },
                                            onSearchSubmit = onHeaderSearchSubmit,
                                            onOpenProfile = { navigator.switchRootTab(AppRoute.Profile) }
                                        )
                                        Box(modifier = Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
                                            content(currentRoute, shellMode)
                                        }
                                    }
                                }

                                AnimatedVisibility(
                                    visible = desktopQueueOpen,
                                    enter = slideInHorizontally { it } + fadeIn(),
                                    exit = slideOutHorizontally { it } + fadeOut()
                                ) {
                                    Row {
                                        VerticalDivider(
                                            thickness = 1.dp,
                                            color = IvidsColorTokens.HairlineSubtle
                                        )
                                        QueueSideSheetHost(
                                            playerViewModel = playerViewModel,
                                            onClose = { desktopQueueOpen = false }
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(
                                thickness = 1.dp,
                                color = IvidsColorTokens.HairlineSubtle
                            )
                            DesktopPlayerHost(
                                playerViewModel = playerViewModel,
                                queueOpen = desktopQueueOpen,
                                onToggleQueue = { desktopQueueOpen = !desktopQueueOpen },
                                onOpenNowPlaying = { setNowPlayingExpanded(true) },
                                onOpenArtist = { id, name ->
                                    navigator.navigateTo(AppRoute.ArtistDetail(id, name))
                                },
                                onOpenSongDetail = { id ->
                                    navigator.navigateTo(AppRoute.SongDetail(id))
                                }
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isNowPlayingExpanded,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                NowPlayingSheetHost(
                    playerViewModel = playerViewModel,
                    playlists = playlists,
                    onCreatePlaylist = onCreatePlaylist,
                    onOpenArtist = { id, name -> navigator.navigateTo(AppRoute.ArtistDetail(id, name)) },
                    onOpenSongDetail = { id -> navigator.navigateTo(AppRoute.SongDetail(id)) },
                    onDismiss = { setNowPlayingExpanded(false) }
                )
            }
        }
    }
}

/**
 * Isolates live 250ms playback state collection for [MiniPlayerBar] so screens underneath do
 * not recompose on progress ticks.
 */
@Composable
private fun MiniPlayerHost(
    playerViewModel: PlayerViewModel,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val liveState by playerViewModel.uiState.collectAsState()
    if (liveState.currentTrack != null) {
        MiniPlayerBar(
            playerState = liveState,
            onExpand = onExpand,
            onTogglePlayPause = playerViewModel::togglePlayPause,
            onToggleLike = playerViewModel::toggleLikeCurrent,
            onRetry = playerViewModel::retryCurrent,
            onSkipPrevious = playerViewModel::previous,
            onSkipNext = playerViewModel::next,
            modifier = modifier
        )
    }
}

/**
 * Isolates live 250ms playback state collection for [DesktopPlayerDeck] so desktop rail and
 * active discovery screens never recompose on scrubber position ticks.
 */
@Composable
private fun DesktopPlayerHost(
    playerViewModel: PlayerViewModel,
    queueOpen: Boolean,
    onToggleQueue: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    onOpenArtist: (artistId: String, artistName: String) -> Unit,
    onOpenSongDetail: (trackId: String) -> Unit
) {
    val liveState by playerViewModel.uiState.collectAsState()
    DesktopPlayerDeck(
        playerState = liveState,
        queueOpen = queueOpen,
        onToggleQueue = onToggleQueue,
        onOpenNowPlaying = onOpenNowPlaying,
        onOpenArtist = onOpenArtist,
        onOpenSongDetail = onOpenSongDetail,
        playerViewModel = playerViewModel
    )
}

/**
 * Isolates live queue/track observation for Desktop's docked [QueueSideSheet].
 */
@Composable
private fun QueueSideSheetHost(
    playerViewModel: PlayerViewModel,
    onClose: () -> Unit
) {
    val liveState by playerViewModel.uiState.collectAsState()
    QueueSideSheet(
        playerState = liveState,
        onJumpTo = playerViewModel::jumpToQueueIndex,
        onRemove = playerViewModel::removeFromQueue,
        onClear = playerViewModel::clearQueue,
        onClose = onClose
    )
}

/**
 * Isolates live 250ms playback telemetry for the expandable [NowPlayingSheet] overlay.
 */
@Composable
private fun NowPlayingSheetHost(
    playerViewModel: PlayerViewModel,
    playlists: List<Playlist>,
    onCreatePlaylist: () -> Unit,
    onOpenArtist: (artistId: String, artistName: String) -> Unit,
    onOpenSongDetail: (trackId: String) -> Unit,
    onDismiss: () -> Unit
) {
    val liveState by playerViewModel.uiState.collectAsState()
    NowPlayingSheet(
        playerState = liveState,
        playlists = playlists,
        playerViewModel = playerViewModel,
        onCreatePlaylist = onCreatePlaylist,
        onOpenArtist = onOpenArtist,
        onOpenSongDetail = onOpenSongDetail,
        onDismiss = onDismiss
    )
}
