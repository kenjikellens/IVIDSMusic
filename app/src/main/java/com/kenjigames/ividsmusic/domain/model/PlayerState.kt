package com.kenjigames.ividsmusic.domain.model

/**
 * Data class encapsulating the complete state of the audio player engine.
 * Exposed to ViewModels and Compose UI components via a reactive StateFlow.
 *
 * @property currentSong The currently loaded or playing track, or null if idle
 * @property isPlaying True if audio playback is actively playing
 * @property positionMs Current playback position in milliseconds
 * @property durationMs Total duration of current track in milliseconds
 * @property isBuffering True if media is loading/buffering from network
 * @property isShuffleEnabled True if queue shuffle mode is active
 * @property isRepeatOne True if single track repeat mode is active
 * @property playbackStatus Human-readable status string (e.g. "Playing", "Buffering", "Idle")
 */
data class PlayerState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isBuffering: Boolean = false,
    val isShuffleEnabled: Boolean = false,
    val isRepeatOne: Boolean = false,
    val playbackStatus: String = "Idle"
)
