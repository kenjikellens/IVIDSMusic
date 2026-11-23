package com.kenjigames.ividsmusic.ui.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.kenjigames.ividsmusic.domain.model.LayoutMode
import com.kenjigames.ividsmusic.domain.model.Playlist
import com.kenjigames.ividsmusic.domain.model.RepeatMode
import com.kenjigames.ividsmusic.resources.Res
import com.kenjigames.ividsmusic.resources.ic_ivids_brand
import com.kenjigames.ividsmusic.ui.components.BlurredArtworkBackdrop
import com.kenjigames.ividsmusic.ui.components.IvidsScrubber
import com.kenjigames.ividsmusic.ui.components.formatDurationMillis
import com.kenjigames.ividsmusic.ui.i18n.LocalStrings
import com.kenjigames.ividsmusic.ui.navigation.AppNavigator
import com.kenjigames.ividsmusic.ui.navigation.AppRoute
import com.kenjigames.ividsmusic.ui.player.NowPlayingSheet
import com.kenjigames.ividsmusic.ui.player.PlayerUiState
import com.kenjigames.ividsmusic.ui.player.PlayerViewModel
import com.kenjigames.ividsmusic.ui.theme.IvidsColorTokens
import com.kenjigames.ividsmusic.ui.theme.IvidsTextStyles
import com.kenjigames.ividsmusic.ui.theme.LocalDynamicAccent
import com.kenjigames.ividsmusic.ui.theme.tvFocusable
import org.jetbrains.compose.resources.painterResource

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
private data class NavEntry(
    val route: AppRoute,
    val labelKey: String,
    val icon: ImageVector,
    val matcher: (AppRoute) -> Boolean
)

/**
 * Adaptive root layout container rendering the pure neutral dark IVIDS Desktop AbyssRail +
 * compact Search Pill + artwork-blurred PlayerDeck, Mobile BottomBar + floating MiniPlayerBar, or TV drawer.
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
    content: @Composable (AppRoute, ShellMode) -> Unit
) {
    var nowPlayingExpanded by remember { mutableStateOf(false) }
    var desktopQueueOpen by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize().background(IvidsColorTokens.ObsidianStage)) {
        when (shellMode) {
            ShellMode.MOBILE -> {
                Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                    MobileTopHeader(
                        currentRoute = currentRoute,
                        canNavigateBack = canNavigateBack,
                        onBack = { navigator.navigateBack() },
                        onOpenDownloader = { navigator.navigateTo(AppRoute.Downloader) },
                        onOpenSettings = { navigator.navigateTo(AppRoute.Settings) }
                    )
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        content(currentRoute, shellMode)
                        if (playerState.currentTrack != null) {
                            MiniPlayerBar(
                                playerState = playerState,
                                onExpand = { nowPlayingExpanded = true },
                                onTogglePlayPause = playerViewModel::togglePlayPause,
                                onToggleLike = playerViewModel::toggleLikeCurrent,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }
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

                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    DesktopTopBar(
                                        canNavigateBack = canNavigateBack,
                                        searchFocusTrigger = searchFocusTrigger,
                                        onBack = { navigator.navigateBack() },
                                        onSearchSubmit = onHeaderSearchSubmit,
                                        onOpenProfile = { navigator.switchRootTab(AppRoute.Profile) }
                                    )
                                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
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
                                    QueueSideSheet(
                                        playerState = playerState,
                                        onJumpTo = playerViewModel::jumpToQueueIndex,
                                        onRemove = playerViewModel::removeFromQueue,
                                        onClear = playerViewModel::clearQueue,
                                        onClose = { desktopQueueOpen = false }
                                    )
                                }
                            }
                        }

                        HorizontalDivider(
                            thickness = 1.dp,
                            color = IvidsColorTokens.HairlineSubtle
                        )
                        DesktopPlayerDeck(
                            playerState = playerState,
                            queueOpen = desktopQueueOpen,
                            onToggleQueue = { desktopQueueOpen = !desktopQueueOpen },
                            onOpenNowPlaying = { nowPlayingExpanded = true },
                            onOpenArtist = { id, name ->
                                navigator.navigateTo(AppRoute.ArtistDetail(id, name))
                            },
                            onOpenSongDetail = { id ->
                                navigator.navigateTo(AppRoute.SongDetail(id))
                            },
                            playerViewModel = playerViewModel
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = nowPlayingExpanded,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            NowPlayingSheet(
                playerState = playerState,
                playlists = playlists,
                playerViewModel = playerViewModel,
                onCreatePlaylist = onCreatePlaylist,
                onOpenArtist = { id, name -> navigator.navigateTo(AppRoute.ArtistDetail(id, name)) },
                onOpenSongDetail = { id -> navigator.navigateTo(AppRoute.SongDetail(id)) },
                onDismiss = { nowPlayingExpanded = false }
            )
        }
    }
}

/**
 * Renders the borderless top navigation header on Mobile screens with back navigation and quick
 * Downloader and Settings shortcuts.
 */
@Composable
private fun MobileTopHeader(
    currentRoute: AppRoute,
    canNavigateBack: Boolean,
    onBack: () -> Unit,
    onOpenDownloader: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val strings = LocalStrings.current

    Column(modifier = Modifier.fillMaxWidth().background(IvidsColorTokens.ObsidianStage)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (canNavigateBack) {
                    IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
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
                IconButton(onClick = onOpenDownloader) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = strings["nav_downloader"],
                        tint = if (currentRoute is AppRoute.Downloader) IvidsColorTokens.PrimaryCyan else IvidsColorTokens.SlateMuted
                    )
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = strings["settings"],
                        tint = if (currentRoute is AppRoute.Settings) IvidsColorTokens.PrimaryCyan else IvidsColorTokens.SlateMuted
                    )
                }
            }
        }
        HorizontalDivider(thickness = 1.dp, color = IvidsColorTokens.HairlineSubtle)
    }
}

/**
 * Renders the 5-destination bottom navigation dock for compact Mobile viewports (`< 600dp`).
 */
@Composable
private fun MobileBottomBar(
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

    Column(modifier = Modifier.fillMaxWidth().background(IvidsColorTokens.AbyssRail)) {
        HorizontalDivider(thickness = 1.dp, color = IvidsColorTokens.HairlineSubtle)
        NavigationBar(
            containerColor = IvidsColorTokens.AbyssRail,
            contentColor = IvidsColorTokens.ChalkInk,
            modifier = Modifier.navigationBarsPadding()
        ) {
            items.forEach { entry ->
                val selected = entry.matcher(currentRoute)
                NavigationBarItem(
                    selected = selected,
                    onClick = { onSelectRoute(entry.route) },
                    icon = {
                        Icon(
                            imageVector = entry.icon,
                            contentDescription = strings[entry.labelKey]
                        )
                    },
                    label = {
                        Text(
                            text = strings[entry.labelKey],
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = IvidsColorTokens.PrimaryCyan,
                        selectedTextColor = IvidsColorTokens.ChalkInk,
                        indicatorColor = IvidsColorTokens.PrimaryCyan.copy(alpha = 0.14f),
                        unselectedIconColor = IvidsColorTokens.SlateMuted,
                        unselectedTextColor = IvidsColorTokens.SlateMuted
                    )
                )
            }
        }
    }
}

/**
 * Floating borderless mini-player bar anchored above MobileBottomBar with blurred track artwork
 * backdrop, progress bar, like toggle, and play/pause action; tapping opens [NowPlayingSheet].
 */
@Composable
private fun MiniPlayerBar(
    playerState: PlayerUiState,
    onExpand: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onToggleLike: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = playerState.currentTrack ?: return
    val accent = LocalDynamicAccent.current
    val progress = if (playerState.durationMs > 0L) {
        (playerState.positionMs.toFloat() / playerState.durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(18.dp), ambientColor = Color.Black, spotColor = Color.Black)
            .clickable(onClick = onExpand),
        shape = RoundedCornerShape(18.dp),
        color = IvidsColorTokens.GlassSurface
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            BlurredArtworkBackdrop(
                artworkUrl = track.coverUrl,
                tintColor = accent,
                scrimAlpha = 0.74f,
                blurRadius = 32.dp,
                fallbackColor = IvidsColorTokens.GlassSurface,
                modifier = Modifier.matchParentSize()
            )
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                        Text(
                            text = track.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = IvidsColorTokens.SlateMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = onToggleLike, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = if (playerState.isCurrentLiked) {
                                Icons.Rounded.Favorite
                            } else {
                                Icons.Rounded.FavoriteBorder
                            },
                            contentDescription = null,
                            tint = if (playerState.isCurrentLiked) accent else IvidsColorTokens.SlateMuted
                        )
                    }
                    Surface(
                        onClick = onTogglePlayPause,
                        shape = CircleShape,
                        color = accent,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (playerState.isBuffering) {
                                CircularProgressIndicator(
                                    color = IvidsColorTokens.OnAccentDark,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = if (playerState.isPlaying) {
                                        Icons.Rounded.Pause
                                    } else {
                                        Icons.Rounded.PlayArrow
                                    },
                                    contentDescription = null,
                                    tint = IvidsColorTokens.OnAccentDark
                                )
                            }
                        }
                    }
                }
                LinearProgressIndicator(
                    progress = { progress },
                    color = accent,
                    trackColor = IvidsColorTokens.RecessedTrough.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth().height(2.5.dp)
                )
            }
        }
    }
}

/**
 * Pure neutral dark borderless sidebar navigation rail rendered on Desktop (`>= 600dp`) and Android TV.
 */
@Composable
private fun SidebarNavigationRail(
    currentRoute: AppRoute,
    expanded: Boolean,
    isTv: Boolean,
    onSelectRoute: (AppRoute) -> Unit
) {
    val strings = LocalStrings.current
    val primaryEntries = remember {
        listOf(
            NavEntry(AppRoute.Home, "home", Icons.Rounded.Home) { it is AppRoute.Home },
            NavEntry(AppRoute.Recommended, "nav_foryou", Icons.Rounded.AutoAwesome) { it is AppRoute.Recommended },
            NavEntry(AppRoute.Search(), "search", Icons.Rounded.Search) { it is AppRoute.Search },
            NavEntry(AppRoute.Library, "library", Icons.Rounded.LibraryMusic) { it is AppRoute.Library },
            NavEntry(AppRoute.Profile, "you", Icons.Rounded.Person) { it is AppRoute.Profile }
        )
    }
    val utilityEntries = remember {
        listOf(
            NavEntry(AppRoute.Downloader, "nav_downloader", Icons.Rounded.Download) { it is AppRoute.Downloader },
            NavEntry(AppRoute.Settings, "settings", Icons.Rounded.Settings) { it is AppRoute.Settings }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(if (expanded) 236.dp else 78.dp)
            .background(IvidsColorTokens.AbyssRail)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Image(
                    painter = painterResource(Res.drawable.ic_ivids_brand),
                    contentDescription = "IVIDS Music",
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                )
                if (expanded) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "IVIDS",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = IvidsColorTokens.PrimaryCyan
                        )
                        Text(
                            text = if (isTv) "TV" else "MUSIC",
                            style = MaterialTheme.typography.titleSmall,
                            color = IvidsColorTokens.ChalkInk
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            primaryEntries.forEach { entry ->
                SidebarNavButton(
                    label = strings[entry.labelKey],
                    icon = entry.icon,
                    selected = entry.matcher(currentRoute),
                    expanded = expanded,
                    onClick = { onSelectRoute(entry.route) }
                )
            }

            HorizontalDivider(
                color = IvidsColorTokens.HairlineSubtle,
                modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)
            )

            utilityEntries.forEach { entry ->
                SidebarNavButton(
                    label = strings[entry.labelKey],
                    icon = entry.icon,
                    selected = entry.matcher(currentRoute),
                    expanded = expanded,
                    onClick = { onSelectRoute(entry.route) }
                )
            }
        }
    }
}

/**
 * Single borderless navigation pill inside [SidebarNavigationRail]: ONLY the opened ([selected])
 * item receives the signature IVIDS cyan accent, while hovered inactive items use a neutral grey hover.
 */
@Composable
private fun SidebarNavButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    expanded: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val bgColor by animateColorAsState(
        targetValue = when {
            selected -> IvidsColorTokens.PrimaryCyan.copy(alpha = 0.12f)
            isHovered -> IvidsColorTokens.GlassHover
            else -> Color.Transparent
        },
        animationSpec = tween(150),
        label = "NavButtonBg"
    )
    val iconColor by animateColorAsState(
        targetValue = when {
            selected -> IvidsColorTokens.PrimaryCyan
            isHovered -> IvidsColorTokens.ChalkInk
            else -> IvidsColorTokens.SlateMuted
        },
        animationSpec = tween(150),
        label = "NavButtonIcon"
    )
    val textColor by animateColorAsState(
        targetValue = when {
            selected || isHovered -> IvidsColorTokens.ChalkInk
            else -> IvidsColorTokens.SlateMuted
        },
        animationSpec = tween(150),
        label = "NavButtonText"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .tvFocusable(shape = RoundedCornerShape(10.dp))
            .hoverable(interactionSource)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (expanded) Arrangement.Start else Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(21.dp)
            )
            if (expanded) {
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                    ),
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (selected) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(IvidsColorTokens.PrimaryCyan)
                    )
                }
            }
        }
    }
}

/**
 * Frameless Desktop & TV header containing back navigation, a compact 440dp recessed search pill
 * with `Ctrl+K` hint, and a neutral profile shortcut button.
 */
@Composable
private fun DesktopTopBar(
    canNavigateBack: Boolean,
    searchFocusTrigger: Int,
    onBack: () -> Unit,
    onSearchSubmit: (String) -> Unit,
    onOpenProfile: () -> Unit
) {
    val strings = LocalStrings.current
    var query by remember { mutableStateOf("") }
    var searchFocused by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(searchFocusTrigger) {
        if (searchFocusTrigger > 0) {
            runCatching { searchFocusRequester.requestFocus() }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            if (canNavigateBack) {
                Surface(
                    onClick = onBack,
                    shape = CircleShape,
                    color = IvidsColorTokens.RecessedTrough,
                    modifier = Modifier.size(38.dp).tvFocusable(shape = CircleShape)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = strings["back"],
                            tint = IvidsColorTokens.ChalkInk,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            val pillColor by animateColorAsState(
                targetValue = if (searchFocused) IvidsColorTokens.GlassSurface else IvidsColorTokens.RecessedTrough,
                animationSpec = tween(160),
                label = "SearchPillColor"
            )

            Surface(
                shape = CircleShape,
                color = pillColor,
                modifier = Modifier
                    .widthIn(min = 240.dp, max = 440.dp)
                    .fillMaxWidth()
                    .height(42.dp)
                    .tvFocusable(shape = CircleShape)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = if (searchFocused) IvidsColorTokens.ChalkInk else IvidsColorTokens.SlateMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (query.isEmpty()) {
                            Text(
                                text = strings["search_placeholder"],
                                style = MaterialTheme.typography.bodyMedium,
                                color = IvidsColorTokens.SlateMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = IvidsColorTokens.ChalkInk),
                            cursorBrush = SolidColor(IvidsColorTokens.PrimaryCyan),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                if (query.isNotBlank()) onSearchSubmit(query.trim())
                            }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(searchFocusRequester)
                                .onFocusChanged { searchFocused = it.isFocused }
                        )
                    }
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = { query = "" },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = strings["close"],
                                tint = IvidsColorTokens.SlateMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0x12FFFFFF)
                        ) {
                            Text(
                                text = "Ctrl+K",
                                style = IvidsTextStyles.tabularBadge(),
                                color = IvidsColorTokens.SlateMuted,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        Surface(
            onClick = onOpenProfile,
            shape = CircleShape,
            color = IvidsColorTokens.GlassSurface,
            modifier = Modifier.size(40.dp).tvFocusable(shape = CircleShape)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = strings["you"],
                    tint = IvidsColorTokens.ChalkInk,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Persistent 84dp hi-fi player deck across Desktop and Android TV featuring a live blurred
 * cover-artwork backdrop ([BlurredArtworkBackdrop]), centered transport controls with signature
 * Play button, custom [IvidsScrubber] seek & volume bars, and queue toggle.
 */
@Composable
private fun DesktopPlayerDeck(
    playerState: PlayerUiState,
    queueOpen: Boolean,
    onToggleQueue: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    onOpenArtist: (String, String) -> Unit,
    onOpenSongDetail: (String) -> Unit,
    playerViewModel: PlayerViewModel
) {
    val accent = LocalDynamicAccent.current
    val strings = LocalStrings.current
    val track = playerState.currentTrack
    var dragPos by remember(track?.id) { mutableStateOf<Float?>(null) }
    val maxDur = playerState.durationMs.coerceAtLeast(1L).toFloat()
    val sliderPos = dragPos ?: playerState.positionMs.toFloat().coerceIn(0f, maxDur)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .background(IvidsColorTokens.RecessedTrough)
    ) {
        if (track != null) {
            BlurredArtworkBackdrop(
                artworkUrl = track.coverUrl,
                tintColor = accent,
                scrimAlpha = 0.76f,
                blurRadius = 38.dp,
                fallbackColor = IvidsColorTokens.RecessedTrough,
                modifier = Modifier.matchParentSize()
            )
        }

        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier.weight(0.30f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(IvidsColorTokens.GlassSurface)
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
                        modifier = Modifier.clickable(enabled = track != null, onClick = onOpenNowPlaying)
                    )
                    if (track != null) {
                        Text(
                            text = track.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = IvidsColorTokens.SlateMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable { onOpenArtist(track.artistId, track.artist) }
                        )
                    }
                }
                if (track != null) {
                    IconButton(
                        onClick = playerViewModel::toggleLikeCurrent,
                        modifier = Modifier.size(34.dp).tvFocusable(shape = CircleShape)
                    ) {
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
                modifier = Modifier.weight(0.44f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = playerViewModel::toggleShuffle,
                        modifier = Modifier.size(32.dp).tvFocusable(shape = CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = strings["shuffle"],
                            tint = if (playerState.isShuffleEnabled) accent else IvidsColorTokens.SlateMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = playerViewModel::previous,
                        modifier = Modifier.size(36.dp).tvFocusable(shape = CircleShape)
                    ) {
                        Icon(Icons.Rounded.SkipPrevious, strings["previous"], tint = IvidsColorTokens.ChalkInk)
                    }
                    Surface(
                        onClick = playerViewModel::togglePlayPause,
                        shape = CircleShape,
                        color = accent,
                        shadowElevation = 6.dp,
                        modifier = Modifier.size(40.dp).tvFocusable(shape = CircleShape)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (playerState.isBuffering) {
                                CircularProgressIndicator(
                                    color = IvidsColorTokens.OnAccentDark,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(19.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = if (playerState.isPlaying) {
                                        Icons.Rounded.Pause
                                    } else {
                                        Icons.Rounded.PlayArrow
                                    },
                                    contentDescription = strings["play"],
                                    tint = IvidsColorTokens.OnAccentDark,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = playerViewModel::next,
                        modifier = Modifier.size(36.dp).tvFocusable(shape = CircleShape)
                    ) {
                        Icon(Icons.Rounded.SkipNext, strings["next"], tint = IvidsColorTokens.ChalkInk)
                    }
                    IconButton(
                        onClick = playerViewModel::cycleRepeatMode,
                        modifier = Modifier.size(32.dp).tvFocusable(shape = CircleShape)
                    ) {
                        Icon(
                            imageVector = if (playerState.repeatMode == RepeatMode.ONE) {
                                Icons.Rounded.RepeatOne
                            } else {
                                Icons.Rounded.Repeat
                            },
                            contentDescription = "Repeat",
                            tint = if (playerState.repeatMode != RepeatMode.OFF) accent else IvidsColorTokens.SlateMuted,
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
                modifier = Modifier.weight(0.26f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                if (track != null) {
                    IconButton(
                        onClick = { playerViewModel.downloadCurrent() },
                        modifier = Modifier.size(34.dp).tvFocusable(shape = CircleShape)
                    ) {
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
                    IconButton(
                        onClick = { onOpenSongDetail(track.id) },
                        modifier = Modifier.size(34.dp).tvFocusable(shape = CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = strings["more_info"],
                            tint = IvidsColorTokens.SlateMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onToggleQueue,
                    modifier = Modifier.size(34.dp).tvFocusable(shape = CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                        contentDescription = strings["queue"],
                        tint = if (queueOpen) accent else IvidsColorTokens.SlateMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = playerViewModel::toggleMute,
                    modifier = Modifier.size(34.dp).tvFocusable(shape = CircleShape)
                ) {
                    Icon(
                        imageVector = if (playerState.isMuted) {
                            Icons.AutoMirrored.Rounded.VolumeOff
                        } else {
                            Icons.AutoMirrored.Rounded.VolumeUp
                        },
                        contentDescription = strings["volume"],
                        tint = IvidsColorTokens.SlateMuted,
                        modifier = Modifier.size(19.dp)
                    )
                }
                IvidsScrubber(
                    value = playerState.volume,
                    onValueChange = playerViewModel::setVolume,
                    valueRange = 0f..1f,
                    modifier = Modifier.width(98.dp)
                )
            }
        }
    }
}

/**
 * Collapsible borderless right-hand queue drawer on Desktop allowing quick track jumps and removal.
 */
@Composable
private fun QueueSideSheet(
    playerState: PlayerUiState,
    onJumpTo: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onClear: () -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalStrings.current
    val accent = LocalDynamicAccent.current

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(316.dp)
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
                    IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.Close, strings["close"], tint = IvidsColorTokens.SlateMuted)
                    }
                }
            }

            HorizontalDivider(color = IvidsColorTokens.HairlineSubtle, modifier = Modifier.padding(vertical = 10.dp))

            if (playerState.queue.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(strings["queue_empty"], style = MaterialTheme.typography.bodyMedium, color = IvidsColorTokens.SlateMuted)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    itemsIndexed(playerState.queue, key = { idx, t -> "${idx}_${t.id}" }) { idx, item ->
                        val active = idx == playerState.currentIndex
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .tvFocusable(shape = RoundedCornerShape(10.dp))
                                .clickable { onJumpTo(idx) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) Color(0x1CFFFFFF) else IvidsColorTokens.GlassSurface
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = (idx + 1).toString(),
                                    style = IvidsTextStyles.tabularBadge(),
                                    color = if (active) IvidsColorTokens.ChalkInk else IvidsColorTokens.SlateMuted
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
                                IconButton(onClick = { onRemove(idx) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Rounded.Close, strings["remove"], tint = IvidsColorTokens.SlateMuted, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
