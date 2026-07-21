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
import com.kenjigames.ividsmusic.network.resolver.YtDlpAndroidResolver
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
    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val queue = PlaybackQueue()
    private var exoPlayer: ExoPlayer? = null
    private var ytDlpAndroidResolver: StreamResolver? = null

    /** Resolvers implementing yt-dlp engine and protocol resolvers */
    private val primaryResolver: StreamResolver = NetworkModule.youtubeInnertubeResolver
    private val secondaryResolver: StreamResolver = NetworkModule.pipedStreamResolver
    private val fallbackResolver: StreamResolver = NetworkModule.invidiousStreamResolver

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
        if (context != null && ytDlpAndroidResolver == null) {
            this.ytDlpAndroidResolver = YtDlpAndroidResolver(context.applicationContext)
        }

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
                    Player.STATE_BUFFERING -> _playerState.update { it.copy(isBuffering = true, playbackStatus = "Buffering full stream...") }
                    Player.STATE_READY -> _playerState.update { it.copy(isBuffering = false, playbackStatus = if (it.isPlaying) "Playing" else "Paused") }
                    Player.STATE_ENDED -> {
                        _playerState.update { it.copy(playbackStatus = "Ended") }
                        next()
                    }
                    Player.STATE_IDLE -> _playerState.update { it.copy(playbackStatus = "Idle") }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(tag, "ExoPlayer playback error: ${error.message}", error)
                _playerState.update { it.copy(isBuffering = false, playbackStatus = "Playback error: ${error.errorCodeName}") }
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

    /** Loads and resolves full-length YouTube audio stream for a song */
    private fun loadSong(song: Song) {
        _playerState.update {
            it.copy(
                currentSong = song,
                isPlaying = false,
                positionMs = 0L,
                durationMs = (song.durationSeconds * 1000).toLong().coerceAtLeast(0L),
                isBuffering = true,
                playbackStatus = "Resolving yt-dlp audio..."
            )
        }

        scope.launch {
            try {
                var streamUrl: String? = null
                var sourceDescription = ""

                // 1. Check offline downloaded file
                if (song.isDownloaded && song.localFilePath != null) {
                    val file = File(song.localFilePath)
                    if (file.exists()) {
                        streamUrl = file.absolutePath
                        sourceDescription = "Offline Download"
                    }
                }

                // 2. Resolve full YouTube audio stream using native yt-dlp engine -> Innertube -> Piped -> Invidious
                if (streamUrl == null) {
                    withContext(Dispatchers.IO) {
                        try {
                            val ytDlp = ytDlpAndroidResolver
                            var videoId = song.videoId
                            if (videoId.isEmpty()) {
                                val query = "${song.artistName} - ${song.title}"
                                videoId = ytDlp?.resolveVideoId(query)
                                    ?: primaryResolver.resolveVideoId(query) 
                                    ?: secondaryResolver.resolveVideoId(query) 
                                    ?: fallbackResolver.resolveVideoId(query) 
                                    ?: ""
                            }
                            if (videoId.isNotEmpty()) {
                                streamUrl = ytDlp?.resolveAudioUrl(videoId)
                                    ?: primaryResolver.resolveAudioUrl(videoId) 
                                    ?: secondaryResolver.resolveAudioUrl(videoId) 
                                    ?: fallbackResolver.resolveAudioUrl(videoId)
                                if (streamUrl != null) sourceDescription = "Full yt-dlp YouTube Stream"
                            }
                        } catch (e: Throwable) {
                            Log.w(tag, "Stream resolver error: ${e.message}")
                        }
                    }
                }

                val player = exoPlayer
                if (player == null) {
                    Log.e(tag, "ExoPlayer instance is null. Cannot play stream.")
                    _playerState.update { it.copy(isBuffering = false, playbackStatus = "Audio engine offline") }
                    return@launch
                }

                if (streamUrl != null) {
                    try {
                        Log.d(tag, "Setting ExoPlayer mediaItem full audio URI: $streamUrl ($sourceDescription)")
                        val mediaItem = MediaItem.fromUri(streamUrl!!)
                        player.setMediaItem(mediaItem)
                        player.prepare()
                        player.play()
                        _playerState.update { it.copy(playbackStatus = "Playing ($sourceDescription)") }
                    } catch (e: Throwable) {
                        Log.e(tag, "ExoPlayer play failed: ${e.message}")
                        _playerState.update { it.copy(isBuffering = false, playbackStatus = "Playback error") }
                    }
                } else {
                    _playerState.update { it.copy(isBuffering = false, playbackStatus = "YouTube stream unavailable") }
                }
            } catch (t: Throwable) {
                Log.e(tag, "Fatal loadSong error: ${t.message}", t)
                _playerState.update { it.copy(isBuffering = false, playbackStatus = "Error: ${t.message}") }
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

    /** Plays next song in queue */
    fun next() {
        queue.next()?.let { loadSong(it) }
    }

    /** Plays previous song in queue */
    fun previous() {
        queue.previous()?.let { loadSong(it) }
    }

    /** Toggles queue shuffle mode */
    fun toggleShuffle() {
        val enabled = queue.toggleShuffle()
        _playerState.update { it.copy(isShuffleEnabled = enabled) }
    }

    /** Toggles repeat single track mode */
    fun toggleRepeatOne() {
        val enabled = queue.toggleRepeatOne()
        _playerState.update { it.copy(isRepeatOne = enabled) }
    }

    /** Seeks to position in milliseconds */
    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        _playerState.update { it.copy(positionMs = positionMs) }
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
