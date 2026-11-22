package com.kenjigames.ividsmusic.domain.port

import com.kenjigames.ividsmusic.domain.model.Album
import com.kenjigames.ividsmusic.domain.model.AlbumDetails
import com.kenjigames.ividsmusic.domain.model.AppResult
import com.kenjigames.ividsmusic.domain.model.Artist
import com.kenjigames.ividsmusic.domain.model.ArtistDetails
import com.kenjigames.ividsmusic.domain.model.AudioEngineCommand
import com.kenjigames.ividsmusic.domain.model.AudioQuality
import com.kenjigames.ividsmusic.domain.model.CategoryRow
import com.kenjigames.ividsmusic.domain.model.DownloadItem
import com.kenjigames.ividsmusic.domain.model.EngineSnapshot
import com.kenjigames.ividsmusic.domain.model.HistoryEntry
import com.kenjigames.ividsmusic.domain.model.InterestScore
import com.kenjigames.ividsmusic.domain.model.LayoutMode
import com.kenjigames.ividsmusic.domain.model.Playlist
import com.kenjigames.ividsmusic.domain.model.SearchOverview
import com.kenjigames.ividsmusic.domain.model.StreamSource
import com.kenjigames.ividsmusic.domain.model.Track
import com.kenjigames.ividsmusic.domain.model.UpdateMode
import com.kenjigames.ividsmusic.domain.model.UserPreferences
import com.kenjigames.ividsmusic.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Port for querying remote music metadata catalogs, genre charts, artist discographies, and related tracks.
 * Implemented in `:core:data` using Deezer as primary source with automatic iTunes fallback.
 */
interface MusicDiscoveryRepository {
    /** Searches tracks matching [query] with pagination ([limit], [offset]) and optional [yearRange] filter. */
    suspend fun searchTracks(
        query: String,
        limit: Int = 24,
        offset: Int = 0,
        yearRange: IntRange? = null
    ): AppResult<List<Track>>

    /** Searches artists matching [query] with pagination via [limit] and [offset]. */
    suspend fun searchArtists(query: String, limit: Int = 24, offset: Int = 0): AppResult<List<Artist>>

    /** Searches albums matching [query] with pagination ([limit], [offset]) and optional [yearRange] filter. */
    suspend fun searchAlbums(
        query: String,
        limit: Int = 24,
        offset: Int = 0,
        yearRange: IntRange? = null
    ): AppResult<List<Album>>

    /** Fetches grouped artist, song, and album shelves concurrently for an unfiltered Search query. */
    suspend fun searchOverview(
        query: String,
        limitPerSection: Int = 12,
        yearRange: IntRange? = null
    ): AppResult<SearchOverview>

    /** Loads curated genre chart rows deduplicated by artist for Home and Recommended screens. */
    suspend fun getGenreCharts(genres: List<String> = DEFAULT_GENRES): AppResult<List<CategoryRow>>

    /** Retrieves top global chart tracks up to [limit] items. */
    suspend fun getChartTracks(limit: Int = 20): AppResult<List<Track>>

    /** Fetches full metadata including album release year and genre tags for [trackId]. */
    suspend fun getTrackDetails(trackId: String): AppResult<Track>

    /** Fetches artist profile, top tracks, albums, and related artists using [artistId] or [artistNameHint]. */
    suspend fun getArtistDetails(artistId: String, artistNameHint: String = ""): AppResult<ArtistDetails>

    /** Fetches album metadata and its complete tracklist for [albumId]. */
    suspend fun getAlbumDetails(albumId: String): AppResult<AlbumDetails>

    /** Retrieves radio/related tracks seeded from [trackId] up to [limit] items. */
    suspend fun getRelatedTracks(trackId: String, limit: Int = 15): AppResult<List<Track>>

    /** Retrieves popular chart tracks associated with [genreName] up to [limit] items. */
    suspend fun getGenreTracks(genreName: String, limit: Int = 20): AppResult<List<Track>>

    companion object {
        val DEFAULT_GENRES = listOf("Pop", "Rock", "Hip-Hop", "Hardcore", "Electronic", "Jazz", "Dance")
    }
}

/**
 * Port for resolving playable local or remote audio streams for tracks and direct YouTube URLs.
 * Implemented as a multi-tier resolver prioritizing local offline files before remote extraction.
 */
interface StreamResolver {
    /** Resolves a playable [StreamSource] for [track] targeting [preferredQuality]. */
    suspend fun resolve(track: Track, preferredQuality: AudioQuality = AudioQuality.Q196): AppResult<StreamSource>

    /** Resolves a playable [StreamSource] directly from a YouTube watch URL or video ID. */
    suspend fun resolveUrl(videoUrlOrId: String, preferredQuality: AudioQuality = AudioQuality.Q196): AppResult<StreamSource>

    /** Evicts cached remote stream entries for [trackId] so subsequent playback re-resolves a fresh URL. */
    fun invalidate(trackId: String)
}

/**
 * Platform audio playback engine abstraction implemented by Media3 ExoPlayer on Android and JavaFX on Desktop.
 * Exposes reactive [snapshot] state and system media [externalCommands] without leaking platform player classes.
 */
interface AudioEngine {
    /** Observable real-time playback status, position, duration, buffering, and error telemetry. */
    val snapshot: StateFlow<EngineSnapshot>

    /** Asynchronous transport commands originating from OS lockscreen, notification, or media key events. */
    val externalCommands: Flow<AudioEngineCommand>

    /** Loads and begins playing [source] accompanied by [track] metadata for media notifications. */
    suspend fun play(source: StreamSource, track: Track): AppResult<Unit>

    /** Pauses active audio playback while retaining current media item and position. */
    suspend fun pause()

    /** Resumes paused audio playback from the current position. */
    suspend fun resume()

    /** Seeks playback to [positionMs] clamped within the active media duration. */
    suspend fun seekTo(positionMs: Long)

    /** Sets output gain in the normalized range `0.0f..1.0f`. */
    suspend fun setVolume(volume: Float)

    /** Stops active playback and clears the engine's current media source. */
    suspend fun stop()

    /** Releases native player threads, audio focus, and platform handles deterministically. */
    fun release()
}

/**
 * Local persistence port managing liked tracks, offline downloads, saved albums, custom playlists, and history.
 * Implemented on top of SQLDelight SQLite with relational foreign-key cascades and transactional guarantees.
 */
interface LibraryRepository {
    /** Emits all user-liked tracks ordered by most recently added. */
    fun observeLikedTracks(): Flow<List<Track>>

    /** Emits all locally downloaded offline tracks whose files exist on disk. */
    fun observeDownloadedTracks(): Flow<List<Track>>

    /** Emits all albums saved to the user's library. */
    fun observeSavedAlbums(): Flow<List<Album>>

    /** Emits all custom playlists populated with their ordered tracks. */
    fun observePlaylists(): Flow<List<Playlist>>

    /** Emits live updates for a single playlist identified by [playlistId], or `null` when deleted. */
    fun observePlaylist(playlistId: Long): Flow<Playlist?>

    /** Emits listening history capped at the 20 most recently played tracks, newest first. */
    fun observeHistory(): Flow<List<HistoryEntry>>

    /** Looks up a persisted track snapshot by [trackId] or returns `null` if absent. */
    suspend fun getTrack(trackId: String): Track?

    /** Inserts or updates [track] metadata while preserving local like/download state where applicable. */
    suspend fun upsertTrack(track: Track): AppResult<Unit>

    /** Marks [track] as liked or unliked in local persistent storage. */
    suspend fun setTrackLiked(track: Track, liked: Boolean): AppResult<Unit>

    /** Saves or removes [album] from the local saved albums table. */
    suspend fun setAlbumSaved(album: Album, saved: Boolean): AppResult<Unit>

    /** Records an offline download [filePath] and [quality] bitrate for [track] in SQLite. */
    suspend fun markTrackDownloaded(track: Track, filePath: String, quality: AudioQuality): AppResult<Unit>

    /** Clears the offline file reference and downloaded flag for [trackId] in SQLite. */
    suspend fun removeDownloadedTrack(trackId: String): AppResult<Unit>

    /** Atomically inserts [track] into listening history at [playedTimestampMs] while enforcing the 20-item cap. */
    suspend fun recordHistory(track: Track, playedTimestampMs: Long): AppResult<Unit>

    /** Deletes all entries from the listening history table. */
    suspend fun clearHistory(): AppResult<Unit>

    /** Creates a new custom playlist with [title] and optional [description]. */
    suspend fun createPlaylist(title: String, description: String = ""): AppResult<Playlist>

    /** Updates the title and optional description of playlist [playlistId]. */
    suspend fun renamePlaylist(playlistId: Long, newTitle: String, newDescription: String? = null): AppResult<Unit>

    /** Deletes playlist [playlistId] and cascades removal of its `playlist_tracks` rows. */
    suspend fun deletePlaylist(playlistId: Long): AppResult<Unit>

    /** Appends [track] to playlist [playlistId] without introducing duplicate entries. */
    suspend fun addTrackToPlaylist(playlistId: Long, track: Track): AppResult<Unit>

    /** Removes [trackId] from playlist [playlistId] and compacts remaining track indices. */
    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: String): AppResult<Unit>

    /** Persists a new ordering of [orderedTrackIds] for playlist [playlistId] inside one transaction. */
    suspend fun reorderPlaylistTracks(playlistId: Long, orderedTrackIds: List<String>): AppResult<Unit>
}

/**
 * Persistence port storing local privacy-first interest scores (`0..100`) for tracks, artists, and genres.
 * Consumed exclusively by [com.kenjigames.ividsmusic.domain.usecase.RecommendationEngine] and Profile stats.
 */
interface ScoreRepository {
    /** Emits reactive changes across all stored [InterestScore] entries. */
    fun observeAllScores(): Flow<List<InterestScore>>

    /** Returns a point-in-time snapshot of all stored [InterestScore] entries. */
    suspend fun getAllScores(): List<InterestScore>

    /** Retrieves a single score record matching [entityKey], or `null` if none exists yet. */
    suspend fun getScore(entityKey: String): InterestScore?

    /** Inserts or updates [score] after clamping its value to `0..100`. */
    suspend fun upsertScore(score: InterestScore): AppResult<Unit>

    /** Clears all stored recommendation interest scores locally. */
    suspend fun clearAllScores(): AppResult<Unit>
}

/**
 * Persistence port for reactive user settings and local listener profile state backed by DataStore Preferences.
 * Maintains zero external network telemetry or account state.
 */
interface PreferencesRepository {
    /** Reactive stream of persisted [UserPreferences] updated immediately on any mutation. */
    val preferences: Flow<UserPreferences>

    /** Reactive stream of the local [UserProfile] including cumulative listening minutes. */
    val profile: Flow<UserProfile>

    /** Persists the preferred offline [AudioQuality] bitrate preset. */
    suspend fun setDownloadQuality(quality: AudioQuality): AppResult<Unit>

    /** Persists UI scale factor clamped safely within `0.75f..1.50f`. */
    suspend fun setUiScale(scale: Float): AppResult<Unit>

    /** Persists active two-letter localization [code] (one of the 16 supported languages). */
    suspend fun setLanguageCode(code: String): AppResult<Unit>

    /** Enables or disables dynamic album-art ambient background tinting. */
    suspend fun setDynamicTintEnabled(enabled: Boolean): AppResult<Unit>

    /** Toggles automatic background update checks and synchronizes [UpdateMode]. */
    suspend fun setAutoUpdateEnabled(enabled: Boolean): AppResult<Unit>

    /** Persists the selected [UpdateMode] and synchronizes `autoUpdateEnabled`. */
    suspend fun setUpdateMode(mode: UpdateMode): AppResult<Unit>

    /** Persists the shell [LayoutMode] override (`AUTO`, `MOBILE`, `DESKTOP`, or `TV`). */
    suspend fun setLayoutMode(mode: LayoutMode): AppResult<Unit>

    /** Updates the local profile [displayName] and [bio]. */
    suspend fun updateProfile(displayName: String, bio: String): AppResult<Unit>

    /** Increments cumulative listening duration by [seconds] when positive. */
    suspend fun addListeningSeconds(seconds: Long): AppResult<Unit>
}

/**
 * Port managing offline audio downloads, YouTube URL metadata previews, storage metrics, and `saved/` scanning.
 * Implemented in `:core:data` with atomic `.part` -> `.m4a`/`.mp3` commits and optional Desktop `ffmpeg` transcoding.
 */
interface OfflineDownloadManager {
    /** Observable list of queued, active, completed, and failed download tasks. */
    val downloads: StateFlow<List<DownloadItem>>

    /** Observable path of the active offline audio output directory (`saved/` or user override). */
    val downloadDirectoryPath: StateFlow<String>

    /** Extracts metadata for a single video or playlist from [youtubeUrl] before starting downloads. */
    suspend fun previewUrl(youtubeUrl: String): AppResult<List<Track>>

    /** Enqueues [track] for offline download at target [quality] and returns its download item ID. */
    suspend fun enqueue(track: Track, quality: AudioQuality): AppResult<String>

    /** Enqueues a raw [youtubeUrl] download at [quality] with an optional [customTitle] override. */
    suspend fun enqueueUrl(
        youtubeUrl: String,
        quality: AudioQuality,
        customTitle: String? = null
    ): AppResult<String>

    /** Cancels an active or queued task identified by [downloadId] and removes temporary `.part` files. */
    suspend fun cancel(downloadId: String)

    /** Deletes the offline audio file for [trackId] and clears its download flag in [LibraryRepository]. */
    suspend fun deleteOfflineFile(trackId: String): AppResult<Unit>

    /** Scans the offline directory for `.mp3` and `.m4a` files and reconciles them with SQLite storage. */
    suspend fun scanSavedDirectory(): AppResult<List<Track>>

    /** Computes total byte size occupied by offline audio files in the saved directory. */
    suspend fun getSavedStorageBytes(): Long

    /** Validates, persists, and switches the active offline save folder to [path], then rescans it. */
    suspend fun setCustomSaveDirectory(path: String): AppResult<Unit>

    /** Deletes cached stream/audio temp artifacts under the app cache directory and clears stream TTL cache. */
    suspend fun clearAudioCache(): AppResult<Unit>

    /** Returns the current saved directory absolute path synchronously. */
    fun getSavedDirectoryPath(): String
}
