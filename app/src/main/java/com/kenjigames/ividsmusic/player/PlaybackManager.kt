package com.kenjigames.ividsmusic.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.kenjigames.ividsmusic.domain.model.PlayerState
import com.kenjigames.ividsmusic.domain.model.Song
import com.kenjigames.ividsmusic.network.NetworkModule
import com.kenjigames.ividsmusic.network.resolver.StreamResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Singleton controller managing ExoPlayer playback operations and queue state.
 * Exposes a reactive [StateFlow] of [PlayerState] to ViewModels and Compose UI components.
 */
class PlaybackManager private constructor() {

    private val tag = "PlaybackManager"
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var exoPlayer: ExoPlayer? = null
    
    private val queue = PlaybackQueue()
    private val streamResolver: StreamResolver = NetworkModule.invidiousStreamResolver
    private val fallbackResolver: StreamResolver = NetworkModule.youtubeHtmlScraper

    private val _playerState = MutableStateFlow(PlayerState())
    /** Reactive Flow emitting player state updates */
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val progressHandler = Handler(Looper.getMainLooper())
    private val progressRunnable = object : Runnable {
        override fun run() {
            exoPlayer?.let { player ->
                if (player.isPlaying) {
                    _playerState.update { state ->
                        state.copy(
                            positionMs = player.currentPosition.coerceAtLeast(0L),
                            durationMs = if (player.duration > 0) player.duration else state.durationMs
                        )
                    }
                    progressHandler.postDelayed(this, 1000)
                }
            }
        }
    }

    /** Binds ExoPlayer instance and registers listeners */
    fun initialize(player: ExoPlayer, context: Context? = null) {
        this.exoPlayer = player
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playerState.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) {
                    startProgressUpdate()
                    _playerState.update { it.copy(playbackStatus = "Playing") }
                } else {
                    stopProgressUpdate()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> _playerState.update { it.copy(isBuffering = true, playbackStatus = "Buffering") }
                    Player.STATE_READY -> _playerState.update { it.copy(isBuffering = false, playbackStatus = if (it.isPlaying) "Playing" else "Paused") }
                    Player.STATE_ENDED -> {
                        _playerState.update { it.copy(playbackStatus = "Ended") }
                        next()
                    }
                    Player.STATE_IDLE -> _playerState.update { it.copy(playbackStatus = "Idle") }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(tag, "ExoPlayer error: ${error.message}", error)
                _playerState.update { it.copy(isBuffering = false, playbackStatus = "Error: ${error.errorCodeName}") }
            }
        })
    }

    /** Sets queue and starts playing from index */
    fun setQueue(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        queue.setQueue(songs, startIndex)
        queue.currentSong?.let { loadSong(it) }
    }

    /** Plays a single song immediately */
    fun playSong(song: Song) {
        setQueue(listOf(song), 0)
    }

    /** Loads and resolves audio stream for a song */
    private fun loadSong(song: Song) {
        val player = exoPlayer ?: return
        _playerState.update {
            it.copy(
                currentSong = song,
                isPlaying = false,
                positionMs = 0L,
                durationMs = (song.durationSeconds * 1000).toLong().coerceAtLeast(0L),
                isBuffering = true,
                playbackStatus = "Resolving audio stream..."
            )
        }

        scope.launch {
            var streamUrl: String? = null
            var sourceDescription = ""

            withContext(Dispatchers.IO) {
                // 1. Check offline file
                if (song.isDownloaded && song.localFilePath != null) {
                    val file = File(song.localFilePath)
                    if (file.exists()) {
                        streamUrl = file.absolutePath
                        sourceDescription = "Offline Download"
                    }
                }

                // 2. Resolve via network if not offline
                if (streamUrl == null) {
                    var videoId = song.videoId
                    if (videoId.isEmpty()) {
                        val query = "${song.artistName} - ${song.title}"
                        videoId = streamResolver.resolveVideoId(query) ?: fallbackResolver.resolveVideoId(query) ?: ""
                    }
                    if (videoId.isNotEmpty()) {
                        streamUrl = streamResolver.resolveAudioUrl(videoId)
                        if (streamUrl != null) sourceDescription = "High-Quality Stream"
                    }
                }

                // 3. Fallback to Deezer Preview URL if stream resolution is unresolvable
                if (streamUrl == null && song.previewUrl.isNotEmpty()) {
                    streamUrl = song.previewUrl
                    sourceDescription = "Preview Stream"
                }
            }

            if (streamUrl != null) {
                try {
                    val mediaItem = MediaItem.fromUri(streamUrl!!)
                    player.setMediaItem(mediaItem)
                    player.prepare()
                    player.play()
                    _playerState.update { it.copy(playbackStatus = "Playing ($sourceDescription)") }
                } catch (e: Exception) {
                    Log.e(tag, "ExoPlayer play failed: ${e.message}")
                    _playerState.update { it.copy(isBuffering = false, playbackStatus = "Playback error") }
                }
            } else {
                _playerState.update { it.copy(isBuffering = false, playbackStatus = "Stream unavailable") }
            }
        }
    }

    /** Toggles play/pause */
    fun togglePlayPause() {
        val player = exoPlayer ?: return
        Log.d(tag, "togglePlayPause() called. Currently isPlaying: ${player.isPlaying}")
        if (player.isPlaying) {
            player.pause()
            Log.d(tag, "Playback paused successfully")
        } else {
            if (player.playbackState == Player.STATE_IDLE) {
                _playerState.value.currentSong?.let { 
                    Log.d(tag, "ExoPlayer was idle. Reloading current song: ${it.title}")
                    loadSong(it) 
                }
            } else {
                player.play()
                Log.d(tag, "Playback resumed successfully")
            }
        }
    }

    /** Skips to next song in queue */
    fun next() {
        Log.d(tag, "next() called. Navigating forward in queue.")
        queue.next()?.let { 
            Log.d(tag, "Next track found: ${it.title} by ${it.artistName}")
            loadSong(it) 
        } ?: Log.w(tag, "No next track found in queue")
    }

    /** Skips to previous song in queue */
    fun previous() {
        Log.d(tag, "previous() called. Checking if we should restart current track or go back.")
        exoPlayer?.let { player ->
            if (player.currentPosition > 3000) {
                Log.d(tag, "Current track position is > 3s. Restarting current track.")
                player.seekTo(0)
                return
            }
        }
        queue.previous()?.let { 
            Log.d(tag, "Previous track found: ${it.title} by ${it.artistName}")
            loadSong(it) 
        } ?: Log.w(tag, "No previous track found in queue")
    }

    /** Seeks to target timestamp in milliseconds */
    fun seekTo(positionMs: Long) {
        Log.d(tag, "seekTo() called. Seeking to: ${positionMs}ms")
        exoPlayer?.seekTo(positionMs)
    }

    /** Toggles queue shuffle mode */
    fun toggleShuffle() {
        val enabled = queue.toggleShuffle()
        Log.d(tag, "toggleShuffle() called. Shuffle enabled status changed to: $enabled")
        _playerState.update { it.copy(isShuffleEnabled = enabled) }
    }

    /** Toggles single track repeat mode */
    fun toggleRepeatOne() {
        val enabled = queue.toggleRepeatOne()
        Log.d(tag, "toggleRepeatOne() called. Repeat status changed to: $enabled")
        _playerState.update { it.copy(isRepeatOne = enabled) }
    }

    private fun startProgressUpdate() {
        stopProgressUpdate()
        progressHandler.post(progressRunnable)
    }

    private fun stopProgressUpdate() {
        progressHandler.removeCallbacks(progressRunnable)
    }

    companion object {
        val instance: PlaybackManager by lazy { PlaybackManager() }
    }
}
