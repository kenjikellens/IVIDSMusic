package com.kenjigames.ividsmusic.domain.model

/**
 * Queue repeat progression modes cycled by player transport controls.
 */
enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

/**
 * Lifecycle states reported by platform [com.kenjigames.ividsmusic.domain.port.AudioEngine] implementations.
 */
enum class PlaybackStatus {
    IDLE,
    BUFFERING,
    PLAYING,
    PAUSED,
    ENDED,
    ERROR
}

/**
 * Hardware, notification, or lockscreen transport commands emitted asynchronously by platform audio engines.
 * Observed by the presentation queue coordinator to advance or rewind the active queue.
 */
enum class AudioEngineCommand {
    NEXT,
    PREVIOUS
}

/**
 * Resolved audio media source passed to an [com.kenjigames.ividsmusic.domain.port.AudioEngine] or downloader.
 * Represents either a local offline file on disk or a time-limited remote audio stream URL.
 */
sealed interface StreamSource {
    val quality: AudioQuality

    /** Locally stored `.m4a` or `.mp3` track inside the offline saved directory. */
    data class LocalFile(
        val filePath: String,
        override val quality: AudioQuality = AudioQuality.Q196
    ) : StreamSource

    /** Resolved remote audio stream URL with optional HTTP request headers and MIME type. */
    data class RemoteUrl(
        val url: String,
        val headers: Map<String, String> = emptyMap(),
        val mimeType: String = "audio/mp4",
        val videoId: String? = null,
        override val quality: AudioQuality = AudioQuality.Q196,
        val resolvedAtMs: Long = 0L
    ) : StreamSource
}

/**
 * Real-time low-level snapshot emitted by Media3 ExoPlayer or JavaFX MediaPlayer.
 * Drives higher-level [PlaybackState] projection without leaking platform player APIs.
 */
data class EngineSnapshot(
    val status: PlaybackStatus = PlaybackStatus.IDLE,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val volume: Float = 1.0f,
    val errorMessage: String? = null
)

/**
 * Unified UI-facing playback state combining active queue metadata with live [EngineSnapshot] telemetry.
 * Consumed by MiniPlayer, PlayerBar, and Now Playing screens.
 */
data class PlaybackState(
    val currentTrack: Track? = null,
    val queue: List<Track> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isResolvingStream: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val volume: Float = 1.0f,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val errorMessage: String? = null
)

/**
 * Immutable persisted snapshot of the user's last playback queue, active track pointer, and position.
 * Restored in a paused state across app restarts by `PlayerViewModel`.
 */
data class PlaybackSessionSnapshot(
    val queue: List<Track> = emptyList(),
    val currentIndex: Int = -1,
    val positionMs: Long = 0L,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val updatedAtMs: Long = 0L
)

/**
 * Exhaustive state machine for an individual offline track download task.
 * Emits real-time throughput and progress snapshots for Downloader and Track UI cards.
 */
sealed interface DownloadState {
    /** Initial state before a task starts executing. */
    data object Idle : DownloadState

    /** Task is waiting in the download queue for an available worker slot. */
    data class Queued(val track: Track, val quality: AudioQuality) : DownloadState

    /** Task is currently resolving a playable remote audio stream URL. */
    data class Resolving(val track: Track, val quality: AudioQuality) : DownloadState

    /** Task is actively streaming audio bytes to a temporary `.part` file. */
    data class Downloading(
        val progress: Float,
        val bytesPerSec: Long,
        val downloadedBytes: Long = 0L,
        val totalBytes: Long? = null
    ) : DownloadState

    /** Desktop-only optional `ffmpeg` transcoding step adjusting bitrate or container format. */
    data class Transcoding(val track: Track, val targetQuality: AudioQuality) : DownloadState

    /** File has been atomically finalized on disk and registered in [com.kenjigames.ividsmusic.domain.port.LibraryRepository]. */
    data class Completed(val filePath: String, val quality: AudioQuality) : DownloadState

    /** Task failed with a structured [DomainError] after cleaning up partial `.part` files. */
    data class Failed(val error: DomainError) : DownloadState

    /** Task was aborted by user cancellation and cleaned up cleanly. */
    data object Cancelled : DownloadState
}

/**
 * Immutable snapshot of an active, queued, completed, or failed download entry managed by [com.kenjigames.ividsmusic.domain.port.OfflineDownloadManager].
 */
data class DownloadItem(
    val id: String,
    val track: Track,
    val requestedQuality: AudioQuality,
    val state: DownloadState,
    val createdAtMs: Long
)
