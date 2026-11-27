package com.kenjigames.ividsmusic.ui.visual

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import com.kenjigames.ividsmusic.domain.model.Album
import com.kenjigames.ividsmusic.domain.model.AlbumDetails
import com.kenjigames.ividsmusic.domain.model.AppResult
import com.kenjigames.ividsmusic.domain.model.Artist
import com.kenjigames.ividsmusic.domain.model.ArtistDetails
import com.kenjigames.ividsmusic.domain.model.AudioEngineCommand
import com.kenjigames.ividsmusic.domain.model.AudioQuality
import com.kenjigames.ividsmusic.domain.model.CategoryRow
import com.kenjigames.ividsmusic.domain.model.DomainError
import com.kenjigames.ividsmusic.domain.model.DownloadItem
import com.kenjigames.ividsmusic.domain.model.EngineSnapshot
import com.kenjigames.ividsmusic.domain.model.HistoryEntry
import com.kenjigames.ividsmusic.domain.model.InterestScore
import com.kenjigames.ividsmusic.domain.model.LayoutMode
import com.kenjigames.ividsmusic.domain.model.PlaybackSessionSnapshot
import com.kenjigames.ividsmusic.domain.model.PlaybackStatus
import com.kenjigames.ividsmusic.domain.model.Playlist
import com.kenjigames.ividsmusic.domain.model.RepeatMode
import com.kenjigames.ividsmusic.domain.model.SearchOverview
import com.kenjigames.ividsmusic.domain.model.StreamSource
import com.kenjigames.ividsmusic.domain.model.Track
import com.kenjigames.ividsmusic.domain.model.UpdateMode
import com.kenjigames.ividsmusic.domain.model.UserPreferences
import com.kenjigames.ividsmusic.domain.model.UserProfile
import com.kenjigames.ividsmusic.domain.port.AudioEngine
import com.kenjigames.ividsmusic.domain.port.LibraryRepository
import com.kenjigames.ividsmusic.domain.port.MusicDiscoveryRepository
import com.kenjigames.ividsmusic.domain.port.OfflineDownloadManager
import com.kenjigames.ividsmusic.domain.port.PlaybackSessionRepository
import com.kenjigames.ividsmusic.domain.port.PreferencesRepository
import com.kenjigames.ividsmusic.domain.port.ScoreRepository
import com.kenjigames.ividsmusic.domain.port.StreamResolver
import com.kenjigames.ividsmusic.domain.usecase.PlaybackQueueController
import com.kenjigames.ividsmusic.domain.usecase.RecommendationEngine
import com.kenjigames.ividsmusic.domain.usecase.ResolveTrackStreamUseCase
import com.kenjigames.ividsmusic.platform.AppUpdateChecker
import com.kenjigames.ividsmusic.platform.PlatformBridge
import com.kenjigames.ividsmusic.ui.components.ToastController
import com.kenjigames.ividsmusic.ui.i18n.AppStrings
import com.kenjigames.ividsmusic.ui.i18n.LocalStrings
import com.kenjigames.ividsmusic.ui.i18n.LocalizationManager
import com.kenjigames.ividsmusic.ui.i18n.SupportedLanguage
import com.kenjigames.ividsmusic.ui.navigation.AppNavigator
import com.kenjigames.ividsmusic.ui.navigation.AppRoute
import com.kenjigames.ividsmusic.ui.player.NowPlayingSheet
import com.kenjigames.ividsmusic.ui.player.PlayerViewModel
import com.kenjigames.ividsmusic.ui.screens.home.HomeScreen
import com.kenjigames.ividsmusic.ui.screens.home.HomeViewModel
import com.kenjigames.ividsmusic.ui.screens.library.LibraryScreen
import com.kenjigames.ividsmusic.ui.screens.library.LibraryViewModel
import com.kenjigames.ividsmusic.ui.screens.playlist.PlaylistDetailScreen
import com.kenjigames.ividsmusic.ui.screens.playlist.PlaylistDetailViewModel
import com.kenjigames.ividsmusic.ui.screens.search.SearchScreen
import com.kenjigames.ividsmusic.ui.screens.search.SearchViewModel
import com.kenjigames.ividsmusic.ui.screens.settings.SettingsScreen
import com.kenjigames.ividsmusic.ui.screens.settings.SettingsViewModel
import com.kenjigames.ividsmusic.ui.shell.AdaptiveAppShell
import com.kenjigames.ividsmusic.ui.shell.QueueSideSheet
import com.kenjigames.ividsmusic.ui.shell.ShellMode
import com.kenjigames.ividsmusic.ui.theme.IvidsColorTokens
import com.kenjigames.ividsmusic.ui.theme.IvidsTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Headless Compose Desktop snapshot runner rendering key Desktop and Mobile screens to PNG files
 * in `build/reports/ui-audit/` for real visual inspection during UI audits.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UiVisualSnapshotRunner {

    /**
     * Captures Desktop (`1280x820`) and Mobile (`390x844`) PNG renders across Home, Search,
     * Library, Settings, and Now Playing views using populated deterministic sample data.
     */
    @Test
    fun renderUiAuditSnapshots() {
        System.setProperty("skiko.renderApi", "SOFTWARE")
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            val outDir = File("build/reports/ui-audit").apply { mkdirs() }
            val strings = loadEnglishStrings()
            val sample = SampleDataFixture()

            val homeVm = HomeViewModel(sample.discoveryRepo, sample.libraryRepo)
            val searchVm = SearchViewModel(sample.discoveryRepo, sample.prefsRepo)
            val libraryVm = LibraryViewModel(sample.libraryRepo, sample.toastController)
            val playlistVm = PlaylistDetailViewModel(1L, sample.libraryRepo, sample.toastController)
            val settingsVm = SettingsViewModel(
                preferencesRepository = sample.prefsRepo,
                scoreRepository = sample.scoreRepo,
                offlineDownloadManager = sample.downloadMgr,
                localizationManager = LocalizationManager(snapshotJson),
                appUpdateChecker = AppUpdateChecker(sample.platformBridge),
                platformBridge = sample.platformBridge,
                toastController = sample.toastController
            )
            val navigator = AppNavigator(AppRoute.Home)
            val queueController = PlaybackQueueController().apply {
                setQueue(sample.tracks, startIndex = 0)
            }
            val playerVm = PlayerViewModel(
                audioEngine = sample.audioEngine,
                queueController = queueController,
                resolveStream = ResolveTrackStreamUseCase(sample.libraryRepo, sample.streamResolver),
                libraryRepository = sample.libraryRepo,
                recommendationEngine = RecommendationEngine(sample.scoreRepo, sample.discoveryRepo) { 1_000L },
                preferencesRepository = sample.prefsRepo,
                offlineDownloadManager = sample.downloadMgr,
                toastController = sample.toastController
            )
            val playerState = playerVm.uiState.value

            renderPng(outDir, "after_desktop_home.png", 1280, 820, strings) {
                AdaptiveAppShell(
                    shellMode = ShellMode.DESKTOP,
                    currentRoute = AppRoute.Home,
                    canNavigateBack = false,
                    navigator = navigator,
                    playerState = playerState,
                    playerViewModel = playerVm,
                    playlists = sample.playlists,
                    likedCount = sample.likedIds.size,
                    searchFocusTrigger = 0,
                    onHeaderSearchSubmit = {},
                    onCreatePlaylist = {}
                ) { _, _ ->
                    HomeScreen(
                        viewModel = homeVm,
                        onPlayTrack = { _, _ -> },
                        onOpenTrack = {},
                        onOpenArtist = { _, _ -> },
                        onOpenAlbum = {},
                        onExploreTrends = {},
                        playlists = sample.playlists,
                        likedTracks = sample.tracks.filter { it.isLiked },
                        playerViewModel = playerVm,
                        libraryRepository = sample.libraryRepo,
                        toastController = sample.toastController
                    )
                }
            }

            renderPng(outDir, "after_desktop_search.png", 1280, 820, strings) {
                AdaptiveAppShell(
                    shellMode = ShellMode.DESKTOP,
                    currentRoute = AppRoute.Search(),
                    canNavigateBack = true,
                    navigator = navigator,
                    playerState = playerState,
                    playerViewModel = playerVm,
                    playlists = sample.playlists,
                    likedCount = sample.likedIds.size,
                    searchFocusTrigger = 0,
                    onHeaderSearchSubmit = {},
                    onCreatePlaylist = {}
                ) { _, _ ->
                    SearchScreen(
                        initialQuery = "",
                        viewModel = searchVm,
                        playerState = playerState,
                        playerViewModel = playerVm,
                        playlists = sample.playlists,
                        likedIds = sample.likedIds,
                        downloadedIds = sample.downloadedIds,
                        libraryRepository = sample.libraryRepo,
                        toastController = sample.toastController,
                        onOpenArtist = { _, _ -> },
                        onOpenAlbum = {},
                        onOpenSongDetail = {}
                    )
                }
            }

            renderPng(outDir, "after_desktop_library.png", 1280, 820, strings) {
                AdaptiveAppShell(
                    shellMode = ShellMode.DESKTOP,
                    currentRoute = AppRoute.Library,
                    canNavigateBack = true,
                    navigator = navigator,
                    playerState = playerState,
                    playerViewModel = playerVm,
                    playlists = sample.playlists,
                    likedCount = sample.likedIds.size,
                    searchFocusTrigger = 0,
                    onHeaderSearchSubmit = {},
                    onCreatePlaylist = {}
                ) { _, _ ->
                    LibraryScreen(
                        viewModel = libraryVm,
                        playerState = playerState,
                        playerViewModel = playerVm,
                        likedIds = sample.likedIds,
                        downloadedIds = sample.downloadedIds,
                        libraryRepository = sample.libraryRepo,
                        toastController = sample.toastController,
                        onOpenAlbum = {},
                        onOpenArtist = { _, _ -> },
                        onOpenSongDetail = {},
                        onOpenPlaylist = {},
                        onExploreMusic = {}
                    )
                }
            }

            renderPng(outDir, "after_desktop_playlist.png", 1280, 820, strings) {
                AdaptiveAppShell(
                    shellMode = ShellMode.DESKTOP,
                    currentRoute = AppRoute.PlaylistDetail(1L),
                    canNavigateBack = true,
                    navigator = navigator,
                    playerState = playerState,
                    playerViewModel = playerVm,
                    playlists = sample.playlists,
                    likedCount = sample.likedIds.size,
                    searchFocusTrigger = 0,
                    onHeaderSearchSubmit = {},
                    onCreatePlaylist = {}
                ) { _, _ ->
                    PlaylistDetailScreen(
                        viewModel = playlistVm,
                        playerState = playerState,
                        playerViewModel = playerVm,
                        allPlaylists = sample.playlists,
                        likedIds = sample.likedIds,
                        downloadedIds = sample.downloadedIds,
                        libraryRepository = sample.libraryRepo,
                        toastController = sample.toastController,
                        onOpenArtist = { _, _ -> },
                        onOpenSongDetail = {},
                        onNavigateBack = {}
                    )
                }
            }

            renderPng(outDir, "after_desktop_queue.png", 360, 720, strings) {
                QueueSideSheet(
                    playerState = playerState,
                    onJumpTo = {},
                    onMoveTrack = { _, _ -> },
                    onRemove = {},
                    onClear = {},
                    onToggleAutoplay = {},
                    onClose = {}
                )
            }

            renderPng(outDir, "after_desktop_settings.png", 1280, 820, strings) {
                AdaptiveAppShell(
                    shellMode = ShellMode.DESKTOP,
                    currentRoute = AppRoute.Settings,
                    canNavigateBack = true,
                    navigator = navigator,
                    playerState = playerState,
                    playerViewModel = playerVm,
                    playlists = sample.playlists,
                    likedCount = sample.likedIds.size,
                    searchFocusTrigger = 0,
                    onHeaderSearchSubmit = {},
                    onCreatePlaylist = {}
                ) { _, _ ->
                    SettingsScreen(viewModel = settingsVm, onOpenSection = {})
                }
            }

            renderPng(outDir, "after_desktop_now_playing.png", 1280, 820, strings) {
                NowPlayingSheet(
                    playerState = playerState,
                    playlists = sample.playlists,
                    playerViewModel = playerVm,
                    onCreatePlaylist = {},
                    onOpenArtist = { _, _ -> },
                    onOpenSongDetail = {},
                    onDismiss = {}
                )
            }

            renderPng(outDir, "after_mobile_home.png", 390, 844, strings) {
                AdaptiveAppShell(
                    shellMode = ShellMode.MOBILE,
                    currentRoute = AppRoute.Home,
                    canNavigateBack = false,
                    navigator = navigator,
                    playerState = playerState,
                    playerViewModel = playerVm,
                    playlists = sample.playlists,
                    likedCount = sample.likedIds.size,
                    searchFocusTrigger = 0,
                    onHeaderSearchSubmit = {},
                    onCreatePlaylist = {}
                ) { _, _ ->
                    HomeScreen(
                        viewModel = homeVm,
                        onPlayTrack = { _, _ -> },
                        onOpenTrack = {},
                        onOpenArtist = { _, _ -> },
                        onOpenAlbum = {},
                        onExploreTrends = {},
                        playlists = sample.playlists,
                        likedTracks = sample.tracks.filter { it.isLiked },
                        playerViewModel = playerVm,
                        libraryRepository = sample.libraryRepo,
                        toastController = sample.toastController
                    )
                }
            }

            renderPng(outDir, "after_mobile_library.png", 390, 844, strings) {
                AdaptiveAppShell(
                    shellMode = ShellMode.MOBILE,
                    currentRoute = AppRoute.Library,
                    canNavigateBack = true,
                    navigator = navigator,
                    playerState = playerState,
                    playerViewModel = playerVm,
                    playlists = sample.playlists,
                    likedCount = sample.likedIds.size,
                    searchFocusTrigger = 0,
                    onHeaderSearchSubmit = {},
                    onCreatePlaylist = {}
                ) { _, _ ->
                    LibraryScreen(
                        viewModel = libraryVm,
                        playerState = playerState,
                        playerViewModel = playerVm,
                        likedIds = sample.likedIds,
                        downloadedIds = sample.downloadedIds,
                        libraryRepository = sample.libraryRepo,
                        toastController = sample.toastController,
                        onOpenAlbum = {},
                        onOpenArtist = { _, _ -> },
                        onOpenSongDetail = {},
                        onOpenPlaylist = {},
                        onExploreMusic = {}
                    )
                }
            }

            listOf(
                "after_desktop_home.png",
                "after_desktop_search.png",
                "after_desktop_library.png",
                "after_desktop_playlist.png",
                "after_desktop_queue.png",
                "after_desktop_settings.png",
                "after_desktop_now_playing.png",
                "after_mobile_home.png",
                "after_mobile_library.png"
            ).forEach { name ->
                val f = File(outDir, name)
                assertTrue(f.exists() && f.length() > 1_000L, "Expected non-empty snapshot $name")
            }

            homeVm.dispose()
            searchVm.dispose()
            libraryVm.dispose()
            playlistVm.dispose()
            settingsVm.dispose()
            playerVm.dispose()
        } finally {
            Dispatchers.resetMain()
        }
    }

    /** Renders [content] into a headless Skia PNG at [width]x[height] inside [IvidsTheme]. */
    private fun renderPng(
        outDir: File,
        fileName: String,
        width: Int,
        height: Int,
        strings: AppStrings,
        content: @androidx.compose.runtime.Composable () -> Unit
    ) {
        val scene = ImageComposeScene(
            width = width,
            height = height,
            density = Density(1f),
            content = {
                IvidsTheme(dynamicTintEnabled = false) {
                    androidx.compose.runtime.CompositionLocalProvider(LocalStrings provides strings) {
                        Box(modifier = Modifier.fillMaxSize().background(IvidsColorTokens.ObsidianStage)) {
                            content()
                        }
                    }
                }
            }
        )
        try {
            scene.render(nanoTime = 1_000_000_000L)
            scene.render(nanoTime = 2_000_000_000L)
            val image = scene.render(nanoTime = 3_000_000_000L)
            val data = image.encodeToData(EncodedImageFormat.PNG)
                ?: error("Failed to encode PNG for $fileName")
            File(outDir, fileName).writeBytes(data.bytes)
        } finally {
            scene.close()
        }
    }

    /** Loads English JSON strings directly from `composeResources/files/lang/en.json`. */
    private fun loadEnglishStrings(): AppStrings {
        val enFile = File("src/commonMain/composeResources/files/lang/en.json")
        val parsed = snapshotJson.parseToJsonElement(enFile.readText()).jsonObject
        val entries = parsed.mapValues { it.value.jsonPrimitive.content }
        return AppStrings(language = SupportedLanguage.EN, entries = entries, englishFallback = entries)
    }

    private companion object {
        val snapshotJson = Json { ignoreUnknownKeys = true }
    }

    /** Deterministic in-memory sample fixtures populating Home, Search, Library, and Player. */
    private class SampleDataFixture {
        val tracks = listOf(
            Track("t1", "Midnight City", "M83", "a1", "Hurry Up, We're Dreaming", "al1", "", 243, isLiked = true, isDownloaded = true, bitrateKbps = 320),
            Track("t2", "Blinding Lights", "The Weeknd", "a2", "After Hours", "al2", "", 200, isLiked = true),
            Track("t3", "Instant Crush", "Daft Punk", "a3", "Random Access Memories", "al3", "", 337, isDownloaded = true, bitrateKbps = 196),
            Track("t4", "Resonance", "HOME", "a4", "Odyssey", "al4", "", 212),
            Track("t5", "Nightcall", "Kavinsky", "a5", "OutRun", "al5", "", 258, isLiked = true)
        )
        val artists = listOf(
            Artist("a1", "M83", "", "Electronic", 2_400_000),
            Artist("a2", "The Weeknd", "", "Pop", 12_000_000),
            Artist("a3", "Daft Punk", "", "Electronic", 9_500_000),
            Artist("a4", "HOME", "", "Synthwave", 850_000)
        )
        val albums = listOf(
            Album("al1", "Hurry Up, We're Dreaming", "M83", "a1", "", 22, 2011),
            Album("al2", "After Hours", "The Weeknd", "a2", "", 14, 2020),
            Album("al3", "Random Access Memories", "Daft Punk", "a3", "", 13, 2013)
        )
        val playlists = listOf(
            Playlist(1L, "Night Drive", "Synthwave & late night beats", 0, tracks.take(3)),
            Playlist(2L, "Focus Flow", "Deep electronic instrumental", 1, tracks.drop(2))
        )
        val likedIds = tracks.filter { it.isLiked }.map { it.id }.toSet()
        val downloadedIds = tracks.filter { it.isDownloaded }.map { it.id }.toSet()
        val toastController = ToastController()
        val platformBridge = PlatformBridge()

        val audioEngine = object : AudioEngine {
            override val snapshot = MutableStateFlow(
                EngineSnapshot(
                    status = PlaybackStatus.PLAYING,
                    isPlaying = true,
                    positionMs = 84_000L,
                    durationMs = 243_000L,
                    volume = 0.8f
                )
            )
            override val externalCommands: Flow<AudioEngineCommand> = MutableSharedFlow()
            override suspend fun play(source: StreamSource, track: Track) = AppResult.Success(Unit)
            override suspend fun pause() {}
            override suspend fun resume() {}
            override suspend fun seekTo(positionMs: Long) {}
            override suspend fun setVolume(volume: Float) {}
            override suspend fun stop() {}
            override fun release() {}
        }

        val streamResolver = object : StreamResolver {
            override suspend fun resolve(track: Track, preferredQuality: AudioQuality) =
                AppResult.Success(StreamSource.RemoteUrl("https://example.com/${track.id}.m4a"))
            override suspend fun resolveUrl(videoUrlOrId: String, preferredQuality: AudioQuality) =
                AppResult.Failure(DomainError.StreamNotFound("unused"))
            override fun invalidate(trackId: String) {}
        }

        val prefsRepo = object : PreferencesRepository {
            private val state = MutableStateFlow(
                UserPreferences(
                    languageCode = "en",
                    uiScale = 1.0f,
                    recentSearches = listOf("Daft Punk", "Synthwave", "M83")
                )
            )
            override val preferences: Flow<UserPreferences> = state
            override val profile: Flow<UserProfile> = MutableStateFlow(UserProfile("Kenji", "Hi-Fi listener", 3_600L))
            override suspend fun setDownloadQuality(quality: AudioQuality) = AppResult.Success(Unit)
            override suspend fun setUiScale(scale: Float) = AppResult.Success(Unit)
            override suspend fun setLanguageCode(code: String) = AppResult.Success(Unit)
            override suspend fun setDynamicTintEnabled(enabled: Boolean) = AppResult.Success(Unit)
            override suspend fun setAutoUpdateEnabled(enabled: Boolean) = AppResult.Success(Unit)
            override suspend fun setUpdateMode(mode: UpdateMode) = AppResult.Success(Unit)
            override suspend fun setLayoutMode(mode: LayoutMode) = AppResult.Success(Unit)
            override suspend fun updateProfile(displayName: String, bio: String) = AppResult.Success(Unit)
            override suspend fun setVolumeState(volume: Float, isMuted: Boolean) = AppResult.Success(Unit)
            override suspend fun addListeningSeconds(seconds: Long) = AppResult.Success(Unit)
            override suspend fun addRecentSearch(query: String) = AppResult.Success(Unit)
            override suspend fun removeRecentSearch(query: String) = AppResult.Success(Unit)
            override suspend fun clearRecentSearches() = AppResult.Success(Unit)
            override suspend fun setAutoplayEnabled(enabled: Boolean) = AppResult.Success(Unit)
        }

        val libraryRepo = object : LibraryRepository {
            override fun observeLikedTracks(): Flow<List<Track>> = MutableStateFlow(tracks.filter { it.isLiked })
            override fun observeDownloadedTracks(): Flow<List<Track>> = MutableStateFlow(tracks.filter { it.isDownloaded })
            override fun observeSavedAlbums(): Flow<List<Album>> = MutableStateFlow(albums)
            override fun observePlaylists(): Flow<List<Playlist>> = MutableStateFlow(playlists)
            override fun observePlaylist(playlistId: Long): Flow<Playlist?> = MutableStateFlow(playlists.firstOrNull { it.id == playlistId })
            override fun observeHistory(): Flow<List<HistoryEntry>> = MutableStateFlow(tracks.map { HistoryEntry(it, 1_000L) })
            override suspend fun getTrack(trackId: String): Track? = tracks.firstOrNull { it.id == trackId }
            override suspend fun upsertTrack(track: Track) = AppResult.Success(Unit)
            override suspend fun setTrackLiked(track: Track, liked: Boolean) = AppResult.Success(Unit)
            override suspend fun setAlbumSaved(album: Album, saved: Boolean) = AppResult.Success(Unit)
            override suspend fun markTrackDownloaded(track: Track, filePath: String, quality: AudioQuality) = AppResult.Success(Unit)
            override suspend fun removeDownloadedTrack(trackId: String) = AppResult.Success(Unit)
            override suspend fun recordHistory(track: Track, playedTimestampMs: Long) = AppResult.Success(Unit)
            override suspend fun clearHistory() = AppResult.Success(Unit)
            override suspend fun createPlaylist(title: String, description: String) = AppResult.Success(playlists.first())
            override suspend fun renamePlaylist(playlistId: Long, newTitle: String, newDescription: String?) = AppResult.Success(Unit)
            override suspend fun deletePlaylist(playlistId: Long) = AppResult.Success(Unit)
            override suspend fun addTrackToPlaylist(playlistId: Long, track: Track) = AppResult.Success(Unit)
            override suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: String) = AppResult.Success(Unit)
            override suspend fun reorderPlaylistTracks(playlistId: Long, orderedTrackIds: List<String>) = AppResult.Success(Unit)
        }

        val scoreRepo = object : ScoreRepository {
            override fun observeAllScores(): Flow<List<InterestScore>> = emptyFlow()
            override suspend fun getAllScores(): List<InterestScore> = emptyList()
            override suspend fun getScore(entityKey: String): InterestScore? = null
            override suspend fun upsertScore(score: InterestScore) = AppResult.Success(Unit)
            override suspend fun clearAllScores() = AppResult.Success(Unit)
        }

        val discoveryRepo = object : MusicDiscoveryRepository {
            override suspend fun searchTracks(query: String, limit: Int, offset: Int, yearRange: IntRange?) = AppResult.Success(tracks)
            override suspend fun searchArtists(query: String, limit: Int, offset: Int) = AppResult.Success(artists)
            override suspend fun searchAlbums(query: String, limit: Int, offset: Int, yearRange: IntRange?) = AppResult.Success(albums)
            override suspend fun searchOverview(query: String, limitPerSection: Int, yearRange: IntRange?) =
                AppResult.Success(SearchOverview(artists, tracks, albums))
            override suspend fun getGenreCharts(genres: List<String>) = AppResult.Success(
                listOf(
                    CategoryRow("pop", "Pop Hits", tracks),
                    CategoryRow("elec", "Electronic", tracks.reversed())
                )
            )
            override suspend fun getChartTracks(limit: Int) = AppResult.Success(tracks)
            override suspend fun getTrackDetails(trackId: String) = AppResult.Success(tracks.first())
            override suspend fun getArtistDetails(artistId: String, artistNameHint: String) =
                AppResult.Success(ArtistDetails(artists.first(), tracks, albums, artists.drop(1)))
            override suspend fun getAlbumDetails(albumId: String) =
                AppResult.Success(AlbumDetails(albums.first(), tracks))
            override suspend fun getRelatedTracks(trackId: String, limit: Int) = AppResult.Success(tracks)
            override suspend fun getGenreTracks(genreName: String, limit: Int) = AppResult.Success(tracks)
        }

        val downloadMgr = object : OfflineDownloadManager {
            override val downloads: StateFlow<List<DownloadItem>> = MutableStateFlow(emptyList())
            override val downloadDirectoryPath: StateFlow<String> = MutableStateFlow("C:/Music/IVIDS")
            override suspend fun previewUrl(youtubeUrl: String) = AppResult.Success(tracks)
            override suspend fun enqueue(track: Track, quality: AudioQuality) = AppResult.Success("d1")
            override suspend fun enqueueUrl(youtubeUrl: String, quality: AudioQuality, customTitle: String?) = AppResult.Success("d2")
            override suspend fun cancel(downloadId: String) {}
            override suspend fun deleteOfflineFile(trackId: String) = AppResult.Success(Unit)
            override suspend fun scanSavedDirectory() = AppResult.Success(tracks.filter { it.isDownloaded })
            override suspend fun getSavedStorageBytes(): Long = 42_000_000L
            override suspend fun setCustomSaveDirectory(path: String) = AppResult.Success(Unit)
            override suspend fun clearAudioCache() = AppResult.Success(Unit)
            override suspend fun getAudioCacheBytes(): Long = 25_600_000L
            override fun getSavedDirectoryPath(): String = "C:/Music/IVIDS"
        }
    }
}
