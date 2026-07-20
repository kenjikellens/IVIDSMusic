package com.kenjigames.ividsmusic.player

import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Android Service subclass hosting the ExoPlayer audio instance in the background.
 * Exposes a MediaSession for system-level lockscreen notifications and Bluetooth controls.
 */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null

    override fun onCreate() {
        super.onCreate()

        // Create ExoPlayer instance with local wake mode to prevent CPU sleep during play
        val exoPlayer = ExoPlayer.Builder(this).build().apply {
            setWakeMode(C.WAKE_MODE_LOCAL)
        }
        player = exoPlayer

        // Bind ExoPlayer instance to PlaybackManager singleton
        PlaybackManager.instance.initialize(exoPlayer)

        // Build MediaSession
        mediaSession = MediaSession.Builder(this, exoPlayer).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            release()
            mediaSession = null
        }
        player?.run {
            release()
            player = null
        }
        super.onDestroy()
    }
}
