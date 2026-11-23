package com.kenjigames.ividsmusic.ui.player

import com.kenjigames.ividsmusic.domain.model.Album
import com.kenjigames.ividsmusic.domain.model.AppResult
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
import com.kenjigames.ividsmusic.domain.port.StreamPrefetcher
import com.kenjigames.ividsmusic.domain.port.StreamResolver
import com.kenjigames.ividsmusic.domain.usecase.PlaybackQueueController
import com.kenjigames.ividsmusic.domain.usecase.RecommendationEngine
import com.kenjigames.ividsmusic.domain.usecase.ResolveTrackStreamUseCase
import com.kenjigames.ividsmusic.ui.components.ToastController
import com.kenjigames.ividsmusic.ui.input.TextInputShortcutGuard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Verifies P0 [PlayerViewModel] behaviors: volume persistence, paused session restore, next-track prefetching,
 * explicit cache-invalidated retries, and [TextInputShortcutGuard] input depth tracking.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelP0Test {

    private val trackOne = Track(id = "t1", title = "First Song", artist = "Artist A", durationSeconds = 180)
    private val trackTwo = Track(id = "t2", title = "Second Song", artist = "Artist B", durationSeconds = 200)

    /** Configures an unconfined main dispatcher before each ViewModel test. */
    @BeforeTest
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    /** Resets main dispatcher and shortcut guard state after each test. */
    @AfterTest
    fun tearDown() {
        while (TextInputShortcutGuard.isTyping()) {
            TextInputShortcutGuard.onSessionEnded()
        }
        Dispatchers.resetMain()
    }

    /** Verifies persisted volume, paused session restore on startup, retry invalidation, and shortcut focus guard. */
    @Test
    fun restoresVolumeAndSessionPausedSupportsRetryAndShortcutGuard() = runBlocking {
        val engine = FakeAudioEngine()
        val prefs = FakePreferencesRepository(UserPreferences(volume = 0.35f, isMuted = false))
        val resolver = FakeStreamResolver()
        val sessionRepo = FakeSessionRepository(
            PlaybackSessionSnapshot(
                queue = listOf(trackOne, trackTwo),
                currentIndex = 0,
                positionMs = 42_000L,
                isShuffleEnabled = false,
                repeatMode = RepeatMode.ALL
            )
        )
        val prefetchedIds = mutableListOf<String>()
        val prefetcher = object : StreamPrefetcher {
            override suspend fun prefetch(track: Track, preferredQuality: AudioQuality) {
                prefetchedIds += track.id
            }
        }

        val vm = PlayerViewModel(
            audioEngine = engine,
            queueController = PlaybackQueueController(),
            resolveStream = ResolveTrackStreamUseCase(FakeLibraryRepository(), resolver),
            libraryRepository = FakeLibraryRepository(),
            recommendationEngine = RecommendationEngine(FakeScoreRepository(), FakeDiscoveryRepository()),
            preferencesRepository = prefs,
            offlineDownloadManager = FakeDownloadManager(),
            toastController = ToastController(),
            sessionRepository = sessionRepo,
            streamPrefetcher = prefetcher
        )

        delay(80L)
        assertEquals(0.35f, vm.uiState.value.volume, 0.01f)
        assertEquals(0, engine.playCalls)
        assertEquals("t1", vm.uiState.value.currentTrack?.id)
        assertEquals(42_000L, vm.uiState.value.positionMs)

        vm.togglePlayPause()
        delay(80L)
        assertEquals(1, engine.playCalls)
        assertEquals(42_000L, engine.lastSeekMs)

        vm.retryCurrent()
        delay(80L)
        assertTrue(resolver.invalidatedIds.contains("t1"))
        assertEquals(2, engine.playCalls)

        assertFalse(TextInputShortcutGuard.isTyping())
        TextInputShortcutGuard.onSessionStarted()
        assertTrue(TextInputShortcutGuard.isTyping())
        TextInputShortcutGuard.onSessionEnded()
        assertFalse(TextInputShortcutGuard.isTyping())

        vm.dispose()
    }

    /** Confirms next-track prefetch fires after 2.5 seconds of active playback for the queued neighbor. */
    @Test
    fun prefetchesUpcomingTrackAfterPlaybackStabilizes() = runBlocking {
        val engine = FakeAudioEngine()
        val prefetchedIds = mutableListOf<String>()
        val prefetcher = object : StreamPrefetcher {
            override suspend fun prefetch(track: Track, preferredQuality: AudioQuality) {
                prefetchedIds += track.id
            }
        }

        val vm = PlayerViewModel(
            audioEngine = engine,
            queueController = PlaybackQueueController(),
            resolveStream = ResolveTrackStreamUseCase(FakeLibraryRepository(), FakeStreamResolver()),
            libraryRepository = FakeLibraryRepository(),
            recommendationEngine = RecommendationEngine(FakeScoreRepository(), FakeDiscoveryRepository()),
            preferencesRepository = FakePreferencesRepository(UserPreferences()),
            offlineDownloadManager = FakeDownloadManager(),
            toastController = ToastController(),
            sessionRepository = FakeSessionRepository(null),
            streamPrefetcher = prefetcher
        )

        vm.playTrack(trackOne, listOf(trackOne, trackTwo))
        delay(2_700L)
        assertEquals(listOf("t2"), prefetchedIds)
        vm.dispose()
    }

    private class FakeAudioEngine : AudioEngine {
        private val _snapshot = MutableStateFlow(EngineSnapshot())
        override val snapshot: StateFlow<EngineSnapshot> = _snapshot.asStateFlow()
        override val externalCommands: Flow<AudioEngineCommand> = MutableSharedFlow()
        var playCalls = 0
        var lastSeekMs: Long? = null

        override suspend fun play(source: StreamSource, track: Track): AppResult<Unit> {
            playCalls++
            _snapshot.value = _snapshot.value.copy(
                status = PlaybackStatus.PLAYING,
                isPlaying = true,
                isBuffering = false,
                durationMs = track.durationSeconds * 1000L,
                errorMessage = null
            )
            return AppResult.Success(Unit)
        }

        override suspend fun pause() {
            _snapshot.value = _snapshot.value.copy(status = PlaybackStatus.PAUSED, isPlaying = false)
        }

        override suspend fun resume() {
            _snapshot.value = _snapshot.value.copy(status = PlaybackStatus.PLAYING, isPlaying = true)
        }

        override suspend fun seekTo(positionMs: Long) {
            lastSeekMs = positionMs
            _snapshot.value = _snapshot.value.copy(positionMs = positionMs)
        }

        override suspend fun setVolume(volume: Float) {
            _snapshot.value = _snapshot.value.copy(volume = volume)
        }

        override suspend fun stop() {
            _snapshot.value = EngineSnapshot(volume = _snapshot.value.volume)
        }

        override fun release() {}
    }

    private class FakeSessionRepository(
        private var stored: PlaybackSessionSnapshot?
    ) : PlaybackSessionRepository {
        override suspend fun load(): PlaybackSessionSnapshot? = stored
        override suspend fun save(snapshot: PlaybackSessionSnapshot): AppResult<Unit> {
            stored = snapshot
            return AppResult.Success(Unit)
        }
        override suspend fun clear(): AppResult<Unit> {
            stored = null
            return AppResult.Success(Unit)
        }
    }

    private class FakeStreamResolver : StreamResolver {
        val invalidatedIds = mutableListOf<String>()
        override suspend fun resolve(track: Track, preferredQuality: AudioQuality): AppResult<StreamSource> =
            AppResult.Success(StreamSource.RemoteUrl("https://cdn.example.com/${track.id}.m4a", quality = preferredQuality))
        override suspend fun resolveUrl(videoUrlOrId: String, preferredQuality: AudioQuality): AppResult<StreamSource> =
            AppResult.Failure(DomainError.StreamNotFound("unused"))
        override fun invalidate(trackId: String) {
            invalidatedIds += trackId
        }
    }

    private class FakePreferencesRepository(initial: UserPreferences) : PreferencesRepository {
        private val state = MutableStateFlow(initial)
        override val preferences: Flow<UserPreferences> = state
        override val profile: Flow<UserProfile> = MutableStateFlow(UserProfile())
        override suspend fun setDownloadQuality(quality: AudioQuality) = AppResult.Success(Unit)
        override suspend fun setUiScale(scale: Float) = AppResult.Success(Unit)
        override suspend fun setLanguageCode(code: String) = AppResult.Success(Unit)
        override suspend fun setDynamicTintEnabled(enabled: Boolean) = AppResult.Success(Unit)
        override suspend fun setAutoUpdateEnabled(enabled: Boolean) = AppResult.Success(Unit)
        override suspend fun setUpdateMode(mode: UpdateMode) = AppResult.Success(Unit)
        override suspend fun setLayoutMode(mode: LayoutMode) = AppResult.Success(Unit)
        override suspend fun setVolumeState(volume: Float, isMuted: Boolean): AppResult<Unit> {
            state.value = state.value.copy(volume = volume, isMuted = isMuted)
            return AppResult.Success(Unit)
        }
        override suspend fun updateProfile(displayName: String, bio: String) = AppResult.Success(Unit)
        override suspend fun addListeningSeconds(seconds: Long) = AppResult.Success(Unit)
    }

    private class FakeLibraryRepository : LibraryRepository {
        override fun observeLikedTracks(): Flow<List<Track>> = MutableStateFlow(emptyList())
        override fun observeDownloadedTracks(): Flow<List<Track>> = MutableStateFlow(emptyList())
        override fun observeSavedAlbums(): Flow<List<Album>> = MutableStateFlow(emptyList())
        override fun observePlaylists(): Flow<List<Playlist>> = MutableStateFlow(emptyList())
        override fun observePlaylist(playlistId: Long): Flow<Playlist?> = emptyFlow()
        override fun observeHistory(): Flow<List<HistoryEntry>> = MutableStateFlow(emptyList())
        override suspend fun getTrack(trackId: String): Track? = null
        override suspend fun upsertTrack(track: Track) = AppResult.Success(Unit)
        override suspend fun setTrackLiked(track: Track, liked: Boolean) = AppResult.Success(Unit)
        override suspend fun setAlbumSaved(album: Album, saved: Boolean) = AppResult.Success(Unit)
        override suspend fun markTrackDownloaded(track: Track, filePath: String, quality: AudioQuality) = AppResult.Success(Unit)
        override suspend fun removeDownloadedTrack(trackId: String) = AppResult.Success(Unit)
        override suspend fun recordHistory(track: Track, playedTimestampMs: Long) = AppResult.Success(Unit)
        override suspend fun clearHistory() = AppResult.Success(Unit)
        override suspend fun createPlaylist(title: String, description: String) =
            AppResult.Success(Playlist(1L, title, description))
        override suspend fun renamePlaylist(playlistId: Long, newTitle: String, newDescription: String?) = AppResult.Success(Unit)
        override suspend fun deletePlaylist(playlistId: Long) = AppResult.Success(Unit)
        override suspend fun addTrackToPlaylist(playlistId: Long, track: Track) = AppResult.Success(Unit)
        override suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: String) = AppResult.Success(Unit)
        override suspend fun reorderPlaylistTracks(playlistId: Long, orderedTrackIds: List<String>) = AppResult.Success(Unit)
    }

    private class FakeScoreRepository : ScoreRepository {
        override fun observeAllScores(): Flow<List<InterestScore>> = emptyFlow()
        override suspend fun getAllScores(): List<InterestScore> = emptyList()
        override suspend fun getScore(entityKey: String): InterestScore? = null
        override suspend fun upsertScore(score: InterestScore) = AppResult.Success(Unit)
        override suspend fun clearAllScores() = AppResult.Success(Unit)
    }

    private class FakeDiscoveryRepository : MusicDiscoveryRepository {
        override suspend fun searchTracks(query: String, limit: Int, offset: Int, yearRange: IntRange?) =
            AppResult.Success(emptyList<Track>())
        override suspend fun searchArtists(query: String, limit: Int, offset: Int) =
            AppResult.Success(emptyList<com.kenjigames.ividsmusic.domain.model.Artist>())
        override suspend fun searchAlbums(query: String, limit: Int, offset: Int, yearRange: IntRange?) =
            AppResult.Success(emptyList<Album>())
        override suspend fun searchOverview(query: String, limitPerSection: Int, yearRange: IntRange?) =
            AppResult.Success(SearchOverview())
        override suspend fun getGenreCharts(genres: List<String>) = AppResult.Success(emptyList<CategoryRow>())
        override suspend fun getChartTracks(limit: Int) = AppResult.Success(emptyList<Track>())
        override suspend fun getTrackDetails(trackId: String) = AppResult.Failure(DomainError.StreamNotFound("unused"))
        override suspend fun getArtistDetails(artistId: String, artistNameHint: String) =
            AppResult.Failure(DomainError.StreamNotFound("unused"))
        override suspend fun getAlbumDetails(albumId: String) =
            AppResult.Failure(DomainError.StreamNotFound("unused"))
        override suspend fun getRelatedTracks(trackId: String, limit: Int) = AppResult.Success(emptyList<Track>())
        override suspend fun getGenreTracks(genreName: String, limit: Int) = AppResult.Success(emptyList<Track>())
    }

    private class FakeDownloadManager : OfflineDownloadManager {
        override val downloads: StateFlow<List<DownloadItem>> = MutableStateFlow(emptyList())
        override val downloadDirectoryPath: StateFlow<String> = MutableStateFlow("")
        override suspend fun previewUrl(youtubeUrl: String) = AppResult.Success(emptyList<Track>())
        override suspend fun enqueue(track: Track, quality: AudioQuality) = AppResult.Success("id")
        override suspend fun enqueueUrl(youtubeUrl: String, quality: AudioQuality, customTitle: String?) = AppResult.Success("id")
        override suspend fun cancel(downloadId: String) {}
        override suspend fun deleteOfflineFile(trackId: String) = AppResult.Success(Unit)
        override suspend fun scanSavedDirectory() = AppResult.Success(emptyList<Track>())
        override suspend fun getSavedStorageBytes(): Long = 0L
        override suspend fun setCustomSaveDirectory(path: String) = AppResult.Success(Unit)
        override suspend fun clearAudioCache() = AppResult.Success(Unit)
        override fun getSavedDirectoryPath(): String = ""
    }
}
