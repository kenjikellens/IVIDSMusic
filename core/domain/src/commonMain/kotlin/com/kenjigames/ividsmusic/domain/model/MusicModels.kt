package com.kenjigames.ividsmusic.domain.model

import kotlin.math.abs

/**
 * Defines supported audio bitrates for streaming preference and offline track downloads.
 * Ordered from lowest bandwidth footprint (`Q4`) to highest quality (`Q320`).
 */
enum class AudioQuality(val kbps: Int) {
    Q4(4),
    Q48(48),
    Q96(96),
    Q128(128),
    Q196(196),
    Q256(256),
    Q320(320);

    /** Formatted display label showing the bitrate in kbps for UI quality selectors. */
    val label: String get() = "$kbps kbps"

    companion object {
        /**
         * Maps an arbitrary numeric bitrate in kbps to the nearest [AudioQuality] tier.
         * Falls back to [Q196] when no closer tier applies and performs no state mutation.
         */
        fun fromKbps(kbps: Int): AudioQuality =
            entries.minByOrNull { abs(it.kbps - kbps) } ?: Q196
    }
}

/**
 * Immutable domain representation of a musical track across discovery, queue, library, and offline storage.
 * Combines remote catalog metadata with optional local download and like flags.
 */
data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val artistId: String = "",
    val album: String = "",
    val albumId: String = "",
    val coverUrl: String = "",
    val durationSeconds: Int = 0,
    val previewUrl: String = "",
    val genres: List<String> = emptyList(),
    val releaseYear: Int? = null,
    val rank: Int = 0,
    val videoId: String? = null,
    val isLiked: Boolean = false,
    val isDownloaded: Boolean = false,
    val localFilePath: String? = null,
    val bitrateKbps: Int? = null,
    val addedTimestampMs: Long = 0L
)

/**
 * Immutable domain model representing a music artist returned by discovery or recommendation feeds.
 * Used across Search, Artist detail, and Profile views.
 */
data class Artist(
    val id: String,
    val name: String,
    val coverUrl: String = "",
    val genre: String = "",
    val fanCount: Long = 0L,
    val albumCount: Int = 0
)

/**
 * Immutable domain model representing an album and whether the user saved it locally.
 * Used in Search results, Artist discographies, Album views, and Library storage.
 */
data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val artistId: String = "",
    val coverUrl: String = "",
    val trackCount: Int = 0,
    val releaseYear: Int? = null,
    val isSaved: Boolean = false
)

/**
 * Aggregated payload for an artist's detail screen combining profile metadata, top tracks, albums, and peers.
 * Produced by [com.kenjigames.ividsmusic.domain.port.MusicDiscoveryRepository.getArtistDetails].
 */
data class ArtistDetails(
    val artist: Artist,
    val topTracks: List<Track>,
    val albums: List<Album>,
    val relatedArtists: List<Artist> = emptyList()
)

/**
 * Aggregated payload containing album metadata alongside its ordered track listing.
 * Produced by [com.kenjigames.ividsmusic.domain.port.MusicDiscoveryRepository.getAlbumDetails].
 */
data class AlbumDetails(
    val album: Album,
    val tracks: List<Track>
)

/**
 * Named horizontal shelf of tracks for a specific music genre or chart category on Home and Recommended screens.
 * Populated by [com.kenjigames.ividsmusic.domain.port.MusicDiscoveryRepository.getGenreCharts].
 */
data class CategoryRow(
    val id: String,
    val title: String,
    val tracks: List<Track>
)

/**
 * Filter categories supported on the Search screen to narrow queries or switch to paginated grid mode.
 */
enum class SearchFilterType {
    ALL,
    SONG,
    ARTIST,
    ALBUM
}

/**
 * Combined multi-category overview returned when executing an unfiltered Search query.
 * Groups matching artists, tracks, and albums fetched concurrently from discovery providers.
 */
data class SearchOverview(
    val artists: List<Artist>,
    val songs: List<Track>,
    val albums: List<Album>
)

/**
 * User-created custom playlist holding ordered tracks persisted in local SQLite storage.
 * Cover artwork falls back to a deterministic gradient index matching legacy palette styling.
 */
data class Playlist(
    val id: Long,
    val title: String,
    val description: String = "",
    val coverGradientIndex: Int = 0,
    val tracks: List<Track> = emptyList(),
    val createdTimestampMs: Long = 0L
)

/**
 * Timestamped playback log entry stored in the 20-track capped listening history table.
 * Ordered newest-first for Home and Profile recently-played sections.
 */
data class HistoryEntry(
    val track: Track,
    val playedTimestampMs: Long
)

/**
 * Discriminator for local recommendation scoring entries in the discovery score matrix.
 */
enum class ScoreEntityType {
    TRACK,
    ARTIST,
    GENRE
}

/**
 * Local preference score (`0..100`) recorded for a track, artist, or genre with zero cloud telemetry.
 * Stored and queried by [com.kenjigames.ividsmusic.domain.port.ScoreRepository].
 */
data class InterestScore(
    val entityKey: String,
    val entityType: ScoreEntityType,
    val entityId: String,
    val displayName: String,
    val score: Int,
    val lastUpdatedMs: Long
)

/**
 * Personalized multi-row recommendation bundle computed by [com.kenjigames.ividsmusic.domain.usecase.RecommendationEngine].
 * Supplies Home's Recommended row and the dedicated For You screen sections.
 */
data class RecommendationFeed(
    val discoveryMix: List<Track>,
    val genreFusionTitle: String?,
    val genreFusionTracks: List<Track>,
    val tasteMatchTitle: String?,
    val tasteMatchTracks: List<Track>,
    val hasHistory: Boolean
)

/**
 * Local-only listener profile metadata and cumulative listening seconds shown on the Profile screen.
 * Persisted via [com.kenjigames.ividsmusic.domain.port.PreferencesRepository] with zero accounts.
 */
data class UserProfile(
    val displayName: String = "Listener",
    val bio: String = "",
    val totalListeningSeconds: Long = 0L
)

/**
 * Application update checking strategy selectable from the Settings screen.
 */
enum class UpdateMode {
    OFF,
    AUTO,
    MANUAL,
    DEVELOPER
}

/**
 * Adaptive shell layout override allowing automatic window-size detection or forced Mobile, Desktop, or TV modes.
 */
enum class LayoutMode {
    AUTO,
    MOBILE,
    DESKTOP,
    TV
}

/**
 * Reactive snapshot of all user-configurable application settings persisted locally in DataStore.
 * Observed by the Compose shell and DownloadManager via [com.kenjigames.ividsmusic.domain.port.PreferencesRepository].
 */
data class UserPreferences(
    val downloadQuality: AudioQuality = AudioQuality.Q196,
    val uiScale: Float = 1.0f,
    val languageCode: String = "en",
    val dynamicTintEnabled: Boolean = true,
    val autoUpdateEnabled: Boolean = true,
    val updateMode: UpdateMode = UpdateMode.AUTO,
    val layoutMode: LayoutMode = LayoutMode.AUTO,
    val volume: Float = 1.0f,
    val isMuted: Boolean = false
)

/**
 * Typed domain error hierarchy replacing raw exceptions across network, stream resolution, storage, and processes.
 * Enables exhaustive error mapping in repositories and ViewModels.
 */
sealed interface DomainError {
    val message: String

    /** Remote HTTP or connectivity failure with optional HTTP status code. */
    data class Network(override val message: String, val statusCode: Int? = null) : DomainError

    /** Failure indicating all stream resolution tiers failed to yield a playable audio source. */
    data class StreamNotFound(override val message: String) : DomainError

    /** Local filesystem, SQLDelight, or DataStore read/write failure. */
    data class Storage(override val message: String) : DomainError

    /** Invalid input argument, empty query, or malformed URL rejected by guard checks. */
    data class Validation(override val message: String) : DomainError

    /** External Desktop CLI process (`yt-dlp.exe` or `ffmpeg`) non-zero exit or timeout failure. */
    data class ProcessFailure(override val message: String, val exitCode: Int? = null) : DomainError

    /** Cooperative coroutine cancellation or user-requested task abort. */
    data class Cancelled(override val message: String = "Operation cancelled") : DomainError

    /** Unexpected fault captured at module boundaries while preserving diagnostic context. */
    data class Unknown(override val message: String, val causeName: String? = null) : DomainError
}

/**
 * Discriminated outcome wrapper enforcing explicit success and typed [DomainError] failure handling.
 * Used across domain ports and infrastructure repositories without silent exception swallowing.
 */
sealed interface AppResult<out T> {
    /** Successful operation wrapping the resulting [data] value. */
    data class Success<T>(val data: T) : AppResult<T>

    /** Failed operation wrapping a structured [DomainError]. */
    data class Failure(val error: DomainError) : AppResult<Nothing>
}

/**
 * Extracts the success value from this [AppResult] or returns `null` if it represents a failure.
 * Provides concise null-safe access for non-critical UI or enrichment paths without mutating state.
 */
fun <T> AppResult<T>.getOrNull(): T? = when (this) {
    is AppResult.Success -> data
    is AppResult.Failure -> null
}

/**
 * Transforms the contained [AppResult.Success] payload with [transform] while propagating any [AppResult.Failure].
 * Preserves typed errors across repository-to-use-case pipelines without state mutation.
 */
inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Failure -> this
}

/**
 * Exhaustively handles both [AppResult.Success] and [AppResult.Failure] branches and returns a unified result.
 * Encourages explicit error handling in callers without swallowing [DomainError]s.
 */
inline fun <T, R> AppResult<T>.fold(
    onSuccess: (T) -> R,
    onFailure: (DomainError) -> R
): R = when (this) {
    is AppResult.Success -> onSuccess(data)
    is AppResult.Failure -> onFailure(error)
}
