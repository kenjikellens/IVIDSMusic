package com.kenjigames.ividsmusic.ui.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
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

private data class NavEntry(
    val route: AppRoute,
    val labelKey: String,
    val icon: ImageVector,
    val matcher: (AppRoute) -> Boolean
)

/**
 * Adaptive root layout container rendering Desktop NavigationRail + PlayerDeck, Mobile BottomBar +
 * floating MiniPlayerBar, or Android TV Leanback drawer, plus expandable NowPlayingSheet.
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
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
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
                            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
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
                            AnimatedVisibility(
                                visible = desktopQueueOpen,
                                enter = slideInHorizontally { it } + fadeIn(),
                                exit = slideOutHorizontally { it } + fadeOut()
                            ) {
                                QueueSideSheet(
                                    playerState = playerState,
                                    onJumpTo = playerViewModel::jumpToQueueIndex,
                                    onRemove = playerViewModel::removeFromQueue,
                                    onClear = playerViewModel::clearQueue,
                                    onClose = { desktopQueueOpen = false }
                                )
                            }
                        }

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
 * Renders the top navigation header on Mobile screens with back navigation and quick Downloader
 * and Settings shortcuts.
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
    val accent = LocalDynamicAccent.current

    Surface(
        color = IvidsColorTokens.VinylSurface,
        border = BorderStroke(1.dp, IvidsColorTokens.HairlineBorder)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
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
                    color = accent
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
                        tint = if (currentRoute is AppRoute.Downloader) accent else IvidsColorTokens.SlateMuted
                    )
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = strings["settings"],
                        tint = if (currentRoute is AppRoute.Settings) accent else IvidsColorTokens.SlateMuted
                    )
                }
            }
        }
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
    val accent = LocalDynamicAccent.current
    val items = remember {
        listOf(
            NavEntry(AppRoute.Home, "home", Icons.Rounded.Home) { it is AppRoute.Home },
            NavEntry(AppRoute.Recommended, "nav_foryou", Icons.Rounded.AutoAwesome) { it is AppRoute.Recommended },
            NavEntry(AppRoute.Search(), "search", Icons.Rounded.Search) { it is AppRoute.Search },
            NavEntry(AppRoute.Library, "library", Icons.Rounded.LibraryMusic) { it is AppRoute.Library },
            NavEntry(AppRoute.Profile, "you", Icons.Rounded.Person) { it is AppRoute.Profile }
        )
    }

    NavigationBar(
        containerColor = IvidsColorTokens.VinylSurface,
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
                    selectedIconColor = IvidsColorTokens.OnAccentDark,
                    selectedTextColor = accent,
                    indicatorColor = accent,
                    unselectedIconColor = IvidsColorTokens.SlateMuted,
                    unselectedTextColor = IvidsColorTokens.SlateMuted
                )
            )
        }
    }
}

/**
 * Floating compact mini-player bar anchored above MobileBottomBar with progress bar, like button,
 * and play/pause toggle; tapping opens [NowPlayingSheet].
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
            .clickable(onClick = onExpand),
        shape = RoundedCornerShape(16.dp),
        color = IvidsColorTokens.GrooveElevated,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        shadowElevation = 10.dp
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(IvidsColorTokens.VinylSurface),
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
                        Icon(Icons.Rounded.MusicNote, null, tint = accent)
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
                trackColor = IvidsColorTokens.HairlineBorder,
                modifier = Modifier.fillMaxWidth().height(2.dp)
            )
        }
    }
}

/**
 * Left sidebar navigation rail rendered on Desktop (`>= 600dp`) and Android TV Leanback shells.
 */
@Composable
private fun SidebarNavigationRail(
    currentRoute: AppRoute,
    expanded: Boolean,
    isTv: Boolean,
    onSelectRoute: (AppRoute) -> Unit
) {
    val strings = LocalStrings.current
    val accent = LocalDynamicAccent.current
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

    Surface(
        modifier = Modifier.fillMaxHeight().width(if (expanded) 224.dp else 78.dp),
        color = IvidsColorTokens.VinylSurface,
        border = BorderStroke(1.dp, IvidsColorTokens.HairlineBorder)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Image(
                    painter = painterResource(Res.drawable.ic_ivids_brand),
                    contentDescription = "IVIDS Music",
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                )
                if (expanded) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "IVIDS",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = accent
                        )
                        Text(
                            text = if (isTv) "TV" else "MUSIC",
                            style = MaterialTheme.typography.titleMedium,
                            color = IvidsColorTokens.ChalkInk
                        )
                    }
                }
            }

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
                color = IvidsColorTokens.HairlineBorder,
                modifier = Modifier.padding(vertical = 8.dp)
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
 * Single focusable navigation item inside [SidebarNavigationRail].
 */
@Composable
private fun SidebarNavButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    expanded: Boolean,
    onClick: () -> Unit
) {
    val accent = LocalDynamicAccent.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .tvFocusable(shape = RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) accent.copy(alpha = 0.16f) else Color.Transparent,
        border = BorderStroke(
            1.dp,
            if (selected) accent.copy(alpha = 0.55f) else Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (expanded) Arrangement.Start else Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) accent else IvidsColorTokens.SlateMuted,
                modifier = Modifier.size(22.dp)
            )
            if (expanded) {
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (selected) IvidsColorTokens.ChalkInk else IvidsColorTokens.SlateMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Top bar for Desktop & TV shells containing back button, `Ctrl+K` focusable search bar,
 * and profile shortcut chip.
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
    val accent = LocalDynamicAccent.current
    var query by remember { mutableStateOf("") }
    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(searchFocusTrigger) {
        if (searchFocusTrigger > 0) {
            runCatching { searchFocusRequester.requestFocus() }
        }
    }

    Surface(
        color = IvidsColorTokens.ObsidianStage,
        border = BorderStroke(1.dp, IvidsColorTokens.HairlineBorder.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (canNavigateBack) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.tvFocusable(shape = CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = strings["back"],
                        tint = IvidsColorTokens.ChalkInk
                    )
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = {
                    Text(
                        text = "${strings["search_placeholder"]} (Ctrl+K)",
                        color = IvidsColorTokens.SlateMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(Icons.Rounded.Search, null, tint = accent)
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Rounded.Close, strings["close"], tint = IvidsColorTokens.SlateMuted)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    if (query.isNotBlank()) onSearchSubmit(query.trim())
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = IvidsColorTokens.GrooveElevated,
                    unfocusedContainerColor = IvidsColorTokens.VinylSurface,
                    focusedBorderColor = accent,
                    unfocusedBorderColor = IvidsColorTokens.HairlineBorder,
                    focusedTextColor = IvidsColorTokens.ChalkInk,
                    unfocusedTextColor = IvidsColorTokens.ChalkInk
                ),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(searchFocusRequester)
                    .tvFocusable(shape = RoundedCornerShape(14.dp))
            )

            Surface(
                onClick = onOpenProfile,
                shape = CircleShape,
                color = IvidsColorTokens.GrooveElevated,
                border = BorderStroke(1.dp, IvidsColorTokens.HairlineBorder),
                modifier = Modifier.size(42.dp).tvFocusable(shape = CircleShape)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = strings["you"],
                        tint = accent
                    )
                }
            }
        }
    }
}

/**
 * Persistent bottom player deck rendered across Desktop and Android TV layouts with transport
 * buttons, tabular scrubber timestamps, quality download badge, volume slider, and queue toggle.
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

    Surface(
        color = IvidsColorTokens.VinylSurface,
        border = BorderStroke(1.dp, IvidsColorTokens.HairlineBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.weight(0.32f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(IvidsColorTokens.GrooveElevated)
                        .tvFocusable(shape = RoundedCornerShape(12.dp))
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
                        Icon(Icons.Rounded.MusicNote, null, tint = accent)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track?.title ?: strings["song_no_track"],
                        style = MaterialTheme.typography.titleMedium,
                        color = IvidsColorTokens.ChalkInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable(enabled = track != null, onClick = onOpenNowPlaying)
                    )
                    Text(
                        text = track?.artist ?: "—",
                        style = MaterialTheme.typography.bodySmall,
                        color = IvidsColorTokens.SlateMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable(enabled = track != null) {
                            if (track != null) onOpenArtist(track.artistId, track.artist)
                        }
                    )
                }
                IconButton(
                    onClick = playerViewModel::toggleLikeCurrent,
                    enabled = track != null,
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
                    onClick = { playerViewModel.downloadCurrent() },
                    enabled = track != null,
                    modifier = Modifier.tvFocusable(shape = CircleShape)
                ) {
                    Icon(
                        imageVector = if (playerState.isCurrentDownloaded) {
                            Icons.Rounded.DownloadDone
                        } else {
                            Icons.Rounded.Download
                        },
                        contentDescription = strings["download_track"],
                        tint = if (playerState.isCurrentDownloaded) IvidsColorTokens.SuccessMint else accent
                    )
                }
                IconButton(
                    onClick = { if (track != null) onOpenSongDetail(track.id) },
                    enabled = track != null,
                    modifier = Modifier.tvFocusable(shape = CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = strings["more_info"],
                        tint = IvidsColorTokens.SlateMuted
                    )
                }
            }

            Column(
                modifier = Modifier.weight(0.42f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = playerViewModel::toggleShuffle,
                        modifier = Modifier.size(34.dp).tvFocusable(shape = CircleShape)
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
                        modifier = Modifier.size(38.dp).tvFocusable(shape = CircleShape)
                    ) {
                        Icon(Icons.Rounded.SkipPrevious, strings["previous"], tint = IvidsColorTokens.ChalkInk)
                    }
                    Surface(
                        onClick = playerViewModel::togglePlayPause,
                        shape = CircleShape,
                        color = accent,
                        modifier = Modifier.size(42.dp).tvFocusable(shape = CircleShape)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (playerState.isBuffering) {
                                CircularProgressIndicator(
                                    color = IvidsColorTokens.OnAccentDark,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = if (playerState.isPlaying) {
                                        Icons.Rounded.Pause
                                    } else {
                                        Icons.Rounded.PlayArrow
                                    },
                                    contentDescription = strings["play"],
                                    tint = IvidsColorTokens.OnAccentDark
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = playerViewModel::next,
                        modifier = Modifier.size(38.dp).tvFocusable(shape = CircleShape)
                    ) {
                        Icon(Icons.Rounded.SkipNext, strings["next"], tint = IvidsColorTokens.ChalkInk)
                    }
                    IconButton(
                        onClick = playerViewModel::cycleRepeatMode,
                        modifier = Modifier.size(34.dp).tvFocusable(shape = CircleShape)
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formatDurationMillis(sliderPos.toLong()),
                        style = IvidsTextStyles.tabularTime(),
                        color = IvidsColorTokens.SlateMuted
                    )
                    Slider(
                        value = sliderPos,
                        onValueChange = { dragPos = it },
                        onValueChangeFinished = {
                            dragPos?.let { playerViewModel.seekTo(it.toLong()) }
                            dragPos = null
                        },
                        valueRange = 0f..maxDur,
                        colors = SliderDefaults.colors(
                            thumbColor = accent,
                            activeTrackColor = accent,
                            inactiveTrackColor = IvidsColorTokens.HairlineBorder
                        ),
                        modifier = Modifier.weight(1f).height(24.dp)
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
                IconButton(
                    onClick = onToggleQueue,
                    modifier = Modifier.tvFocusable(shape = CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                        contentDescription = strings["queue"],
                        tint = if (queueOpen) accent else IvidsColorTokens.SlateMuted
                    )
                }
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
                    modifier = Modifier.width(105.dp)
                )
            }
        }
    }
}

/**
 * Collapsible right-hand queue drawer on Desktop allowing quick track jumps, removals, and clear.
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

    Surface(
        modifier = Modifier.fillMaxHeight().width(310.dp),
        color = IvidsColorTokens.VinylSurface,
        border = BorderStroke(1.dp, IvidsColorTokens.HairlineBorder)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
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
                Row {
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

            HorizontalDivider(color = IvidsColorTokens.HairlineBorder, modifier = Modifier.padding(vertical = 8.dp))

            if (playerState.queue.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(strings["queue_empty"], color = IvidsColorTokens.SlateMuted)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(playerState.queue, key = { idx, t -> "${idx}_${t.id}" }) { idx, item ->
                        val active = idx == playerState.currentIndex
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .tvFocusable(shape = RoundedCornerShape(10.dp))
                                .clickable { onJumpTo(idx) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) accent.copy(alpha = 0.14f) else IvidsColorTokens.GrooveElevated,
                            border = BorderStroke(1.dp, if (active) accent else IvidsColorTokens.HairlineBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = (idx + 1).toString(),
                                    style = IvidsTextStyles.tabularBadge(),
                                    color = if (active) accent else IvidsColorTokens.SlateMuted
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (active) accent else IvidsColorTokens.ChalkInk,
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
