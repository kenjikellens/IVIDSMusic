package com.kenjigames.ividsmusic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.unit.LayoutDirection
import com.kenjigames.ividsmusic.ui.input.TextInputShortcutGuard
import com.kenjigames.ividsmusic.domain.model.AppResult
import com.kenjigames.ividsmusic.domain.model.UserPreferences
import com.kenjigames.ividsmusic.domain.port.LibraryRepository
import com.kenjigames.ividsmusic.domain.port.OfflineDownloadManager
import com.kenjigames.ividsmusic.domain.port.PreferencesRepository
import com.kenjigames.ividsmusic.platform.PlatformBridge
import com.kenjigames.ividsmusic.ui.components.CreatePlaylistDialog
import com.kenjigames.ividsmusic.ui.components.ToastController
import com.kenjigames.ividsmusic.ui.components.ToastHost
import com.kenjigames.ividsmusic.ui.i18n.LocalStrings
import com.kenjigames.ividsmusic.ui.i18n.LocalizationManager
import com.kenjigames.ividsmusic.ui.navigation.AppNavigator
import com.kenjigames.ividsmusic.ui.navigation.AppRoute
import com.kenjigames.ividsmusic.ui.player.PlayerViewModel
import com.kenjigames.ividsmusic.ui.screens.album.AlbumDetailScreen
import com.kenjigames.ividsmusic.ui.screens.album.AlbumViewModel
import com.kenjigames.ividsmusic.ui.screens.artist.ArtistDetailScreen
import com.kenjigames.ividsmusic.ui.screens.artist.ArtistViewModel
import com.kenjigames.ividsmusic.ui.screens.downloader.DownloaderScreen
import com.kenjigames.ividsmusic.ui.screens.downloader.DownloaderViewModel
import com.kenjigames.ividsmusic.ui.screens.home.HomeScreen
import com.kenjigames.ividsmusic.ui.screens.home.HomeViewModel
import com.kenjigames.ividsmusic.ui.screens.library.LibraryScreen
import com.kenjigames.ividsmusic.ui.screens.library.LibraryViewModel
import com.kenjigames.ividsmusic.ui.screens.playlist.PlaylistDetailScreen
import com.kenjigames.ividsmusic.ui.screens.playlist.PlaylistDetailViewModel
import com.kenjigames.ividsmusic.ui.screens.profile.ProfileScreen
import com.kenjigames.ividsmusic.ui.screens.profile.ProfileViewModel
import com.kenjigames.ividsmusic.ui.screens.recommended.RecommendedScreen
import com.kenjigames.ividsmusic.ui.screens.recommended.RecommendedViewModel
import com.kenjigames.ividsmusic.ui.screens.search.SearchScreen
import com.kenjigames.ividsmusic.ui.screens.search.SearchViewModel
import com.kenjigames.ividsmusic.ui.screens.settings.SettingsScreen
import com.kenjigames.ividsmusic.ui.screens.settings.SettingsViewModel
import com.kenjigames.ividsmusic.ui.screens.song.SongDetailScreen
import com.kenjigames.ividsmusic.ui.screens.song.SongDetailViewModel
import com.kenjigames.ividsmusic.ui.shell.AdaptiveAppShell
import com.kenjigames.ividsmusic.ui.shell.ShellMode
import com.kenjigames.ividsmusic.ui.shell.resolveShellMode
import com.kenjigames.ividsmusic.ui.theme.IvidsTheme
import kotlinx.coroutines.launch
import org.koin.compose.getKoin
import org.koin.core.parameter.parametersOf

/**
 * Root Compose Multiplatform entrypoint wiring [IvidsTheme], [LocalizationManager],
 * [AdaptiveAppShell], all 11 screens, and transient toast overlays across Desktop, Mobile, and TV.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun IvidsApp(
    searchFocusTrigger: Int = 0
) {
    val koin = getKoin()
    val preferencesRepository = remember { koin.get<PreferencesRepository>() }
    val libraryRepository = remember { koin.get<LibraryRepository>() }
    val offlineDownloadManager = remember { koin.get<OfflineDownloadManager>() }
    val localizationManager = remember { koin.get<LocalizationManager>() }
    val navigator = remember { koin.get<AppNavigator>() }
    val toastController = remember { koin.get<ToastController>() }
    val platformBridge = remember { koin.get<PlatformBridge>() }
    val playerViewModel = remember { koin.get<PlayerViewModel>() }

    val homeViewModel = remember { koin.get<HomeViewModel>() }
    val searchViewModel = remember { koin.get<SearchViewModel>() }
    val libraryViewModel = remember { koin.get<LibraryViewModel>() }
    val downloaderViewModel = remember { koin.get<DownloaderViewModel>() }
    val profileViewModel = remember { koin.get<ProfileViewModel>() }
    val recommendedViewModel = remember { koin.get<RecommendedViewModel>() }
    val settingsViewModel = remember { koin.get<SettingsViewModel>() }

    val preferences by preferencesRepository.preferences.collectAsState(initial = UserPreferences())
    val strings by localizationManager.strings.collectAsState()
    val playerState by playerViewModel.uiState.collectAsState()
    val backStack by navigator.backStack.collectAsState()
    val playlists by libraryRepository.observePlaylists().collectAsState(initial = emptyList())
    val likedTracks by libraryRepository.observeLikedTracks().collectAsState(initial = emptyList())
    val downloadedTracks by libraryRepository.observeDownloadedTracks().collectAsState(initial = emptyList())

    val likedIds = remember(likedTracks) { likedTracks.map { it.id }.toSet() }
    val downloadedIds = remember(downloadedTracks) { downloadedTracks.map { it.id }.toSet() }
    val scope = rememberCoroutineScope()
    var showCreatePlaylistModal by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        offlineDownloadManager.scanSavedDirectory()
    }

    LaunchedEffect(preferences.languageCode) {
        localizationManager.initialize(preferences.languageCode)
    }

    // Measure unscaled viewport width before IvidsTheme applies uiScale density so scaling to 150%
    // never flips Desktop/Tablet layouts down to Mobile mode.
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val shellMode = resolveShellMode(
            layoutMode = preferences.layoutMode,
            isTvDevice = platformBridge.isTvDevice,
            widthDp = maxWidth
        )
        val layoutDirection = if (strings.language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

        IvidsTheme(
            uiScale = preferences.uiScale,
            dynamicAccent = playerState.dynamicAccent,
            dynamicTintEnabled = preferences.dynamicTintEnabled,
            isTvMode = shellMode == ShellMode.TV,
            layoutDirection = layoutDirection
        ) {
            CompositionLocalProvider(LocalStrings provides strings) {
                InterceptPlatformTextInput(
                    interceptor = { request, nextHandler ->
                        TextInputShortcutGuard.onSessionStarted()
                        try {
                            nextHandler.startInputMethod(request)
                        } finally {
                            TextInputShortcutGuard.onSessionEnded()
                        }
                    }
                ) {
                    if (showCreatePlaylistModal) {
                    CreatePlaylistDialog(
                        onDismiss = { showCreatePlaylistModal = false },
                        onCreate = { title, desc ->
                            showCreatePlaylistModal = false
                            scope.launch {
                                when (val res = libraryRepository.createPlaylist(title, desc)) {
                                    is AppResult.Success -> {
                                        playerState.currentTrack?.let { current ->
                                            libraryRepository.addTrackToPlaylist(res.data.id, current)
                                        }
                                        toastController.show("Created ${res.data.title}")
                                    }
                                    is AppResult.Failure -> toastController.show(res.error.message, isError = true)
                                }
                            }
                        }
                    )
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    val currentRoute = backStack.lastOrNull() ?: AppRoute.Home

                    AdaptiveAppShell(
                        shellMode = shellMode,
                        currentRoute = currentRoute,
                        canNavigateBack = backStack.size > 1,
                        navigator = navigator,
                        playerState = playerState,
                        playerViewModel = playerViewModel,
                        playlists = playlists,
                        searchFocusTrigger = searchFocusTrigger,
                        onHeaderSearchSubmit = { q ->
                            searchViewModel.submitQuery(q)
                            navigator.navigateTo(AppRoute.Search(q))
                        },
                        onCreatePlaylist = { showCreatePlaylistModal = true }
                    ) { activeRoute, activeShellMode ->
                        when (activeRoute) {
                            is AppRoute.Home -> HomeScreen(
                                viewModel = homeViewModel,
                                onPlayTrack = { track, queue -> playerViewModel.playTrack(track, queue) },
                                onOpenTrack = { track -> navigator.navigateTo(AppRoute.SongDetail(track.id)) },
                                onOpenArtist = { id, name -> navigator.navigateTo(AppRoute.ArtistDetail(id, name)) },
                                onOpenAlbum = { id -> navigator.navigateTo(AppRoute.AlbumDetail(id)) },
                                onExploreTrends = {
                                    searchViewModel.submitQuery("Top Hits")
                                    navigator.navigateTo(AppRoute.Search("Top Hits"))
                                }
                            )

                            is AppRoute.Search -> SearchScreen(
                                initialQuery = activeRoute.initialQuery,
                                viewModel = searchViewModel,
                                playerState = playerState,
                                playerViewModel = playerViewModel,
                                playlists = playlists,
                                likedIds = likedIds,
                                downloadedIds = downloadedIds,
                                libraryRepository = libraryRepository,
                                toastController = toastController,
                                onOpenArtist = { id, name -> navigator.navigateTo(AppRoute.ArtistDetail(id, name)) },
                                onOpenAlbum = { id -> navigator.navigateTo(AppRoute.AlbumDetail(id)) },
                                onOpenSongDetail = { id -> navigator.navigateTo(AppRoute.SongDetail(id)) }
                            )

                            is AppRoute.ArtistDetail -> {
                                val vm = remember(activeRoute.artistId, activeRoute.artistName) {
                                    koin.get<ArtistViewModel> {
                                        parametersOf(activeRoute.artistId, activeRoute.artistName)
                                    }
                                }
                                DisposableEffect(vm) { onDispose { vm.dispose() } }
                                ArtistDetailScreen(
                                    viewModel = vm,
                                    playerState = playerState,
                                    playerViewModel = playerViewModel,
                                    playlists = playlists,
                                    likedIds = likedIds,
                                    downloadedIds = downloadedIds,
                                    libraryRepository = libraryRepository,
                                    toastController = toastController,
                                    onOpenAlbum = { id -> navigator.navigateTo(AppRoute.AlbumDetail(id)) },
                                    onOpenArtist = { id, name -> navigator.navigateTo(AppRoute.ArtistDetail(id, name)) },
                                    onOpenSongDetail = { id -> navigator.navigateTo(AppRoute.SongDetail(id)) }
                                )
                            }

                            is AppRoute.AlbumDetail -> {
                                val vm = remember(activeRoute.albumId) {
                                    koin.get<AlbumViewModel> { parametersOf(activeRoute.albumId) }
                                }
                                DisposableEffect(vm) { onDispose { vm.dispose() } }
                                AlbumDetailScreen(
                                    viewModel = vm,
                                    playerState = playerState,
                                    playerViewModel = playerViewModel,
                                    playlists = playlists,
                                    likedIds = likedIds,
                                    downloadedIds = downloadedIds,
                                    libraryRepository = libraryRepository,
                                    toastController = toastController,
                                    onOpenArtist = { id, name -> navigator.navigateTo(AppRoute.ArtistDetail(id, name)) },
                                    onOpenSongDetail = { id -> navigator.navigateTo(AppRoute.SongDetail(id)) }
                                )
                            }

                            is AppRoute.SongDetail -> {
                                val vm = remember(activeRoute.trackId) {
                                    koin.get<SongDetailViewModel> { parametersOf(activeRoute.trackId) }
                                }
                                DisposableEffect(vm) { onDispose { vm.dispose() } }
                                SongDetailScreen(
                                    viewModel = vm,
                                    playerState = playerState,
                                    playerViewModel = playerViewModel,
                                    playlists = playlists,
                                    likedIds = likedIds,
                                    downloadedIds = downloadedIds,
                                    libraryRepository = libraryRepository,
                                    toastController = toastController,
                                    onOpenArtist = { id, name -> navigator.navigateTo(AppRoute.ArtistDetail(id, name)) },
                                    onOpenAlbum = { id -> navigator.navigateTo(AppRoute.AlbumDetail(id)) },
                                    onOpenSongDetail = { id -> navigator.navigateTo(AppRoute.SongDetail(id)) }
                                )
                            }

                            is AppRoute.Library -> LibraryScreen(
                                viewModel = libraryViewModel,
                                playerState = playerState,
                                playerViewModel = playerViewModel,
                                likedIds = likedIds,
                                downloadedIds = downloadedIds,
                                libraryRepository = libraryRepository,
                                toastController = toastController,
                                onOpenAlbum = { id -> navigator.navigateTo(AppRoute.AlbumDetail(id)) },
                                onOpenArtist = { id, name -> navigator.navigateTo(AppRoute.ArtistDetail(id, name)) },
                                onOpenSongDetail = { id -> navigator.navigateTo(AppRoute.SongDetail(id)) },
                                onOpenPlaylist = { id -> navigator.navigateTo(AppRoute.PlaylistDetail(id)) },
                                onExploreMusic = { navigator.switchRootTab(AppRoute.Home) }
                            )

                            is AppRoute.PlaylistDetail -> {
                                val vm = remember(activeRoute.playlistId) {
                                    koin.get<PlaylistDetailViewModel> { parametersOf(activeRoute.playlistId) }
                                }
                                DisposableEffect(vm) { onDispose { vm.dispose() } }
                                PlaylistDetailScreen(
                                    viewModel = vm,
                                    playerState = playerState,
                                    playerViewModel = playerViewModel,
                                    allPlaylists = playlists,
                                    likedIds = likedIds,
                                    downloadedIds = downloadedIds,
                                    libraryRepository = libraryRepository,
                                    toastController = toastController,
                                    onOpenArtist = { id, name -> navigator.navigateTo(AppRoute.ArtistDetail(id, name)) },
                                    onOpenSongDetail = { id -> navigator.navigateTo(AppRoute.SongDetail(id)) },
                                    onNavigateBack = { navigator.navigateBack() }
                                )
                            }

                            is AppRoute.Downloader -> DownloaderScreen(
                                viewModel = downloaderViewModel,
                                playerState = playerState,
                                playerViewModel = playerViewModel,
                                playlists = playlists,
                                likedIds = likedIds,
                                libraryRepository = libraryRepository,
                                toastController = toastController,
                                isDesktopPlatform = activeShellMode == ShellMode.DESKTOP,
                                onOpenArtist = { id, name -> navigator.navigateTo(AppRoute.ArtistDetail(id, name)) },
                                onOpenSongDetail = { id -> navigator.navigateTo(AppRoute.SongDetail(id)) }
                            )

                            is AppRoute.Profile -> ProfileScreen(
                                viewModel = profileViewModel,
                                playerViewModel = playerViewModel,
                                onOpenSongDetail = { id -> navigator.navigateTo(AppRoute.SongDetail(id)) },
                                onOpenSettings = { navigator.navigateTo(AppRoute.Settings) },
                                onOpenDownloader = { navigator.navigateTo(AppRoute.Downloader) },
                                onOpenGenreSearch = { genre ->
                                    searchViewModel.submitQuery(genre)
                                    navigator.navigateTo(AppRoute.Search(genre))
                                }
                            )

                            is AppRoute.Recommended -> RecommendedScreen(
                                viewModel = recommendedViewModel,
                                playerViewModel = playerViewModel,
                                onOpenSongDetail = { id -> navigator.navigateTo(AppRoute.SongDetail(id)) },
                                onOpenArtist = { id, name -> navigator.navigateTo(AppRoute.ArtistDetail(id, name)) },
                                onSearchTopic = { topic ->
                                    searchViewModel.submitQuery(topic)
                                    navigator.navigateTo(AppRoute.Search(topic))
                                },
                                onExploreMusic = { navigator.switchRootTab(AppRoute.Home) }
                            )

                            is AppRoute.Settings -> SettingsScreen(viewModel = settingsViewModel)
                        }
                    }

                    ToastHost(controller = toastController)
                }
                }
            }
        }
    }
}
