package com.kenjigames.ividsmusic.player;

import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

/**
 * Android Service subclass that hosts the ExoPlayer audio instance in the background.
 * Exposes a MediaSession to interface with system-level lock screen notifications
 * and Bluetooth controls.
 */
public class PlaybackService extends MediaSessionService {

    private MediaSession mediaSession;
    private ExoPlayer player;

    /**
     * Called when the service is first created.
     * Initializes ExoPlayer, configures wake modes, registers the player with
     * the PlaybackManager singleton, and builds the MediaSession.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize ExoPlayer with local wake mode to prevent CPU sleep during play
        player = new ExoPlayer.Builder(this).build();
        player.setWakeMode(C.WAKE_MODE_LOCAL);

        // Bind the player instance to the UI state manager
        PlaybackManager.getInstance().initialize(player);

        // Build the MediaSession
        mediaSession = new MediaSession.Builder(this, player).build();
    }

    /**
     * Resolves the MediaSession linked with the service.
     *
     * @param controllerInfo Details regarding the requesting media controller.
     * @return The active MediaSession.
     */
    @Nullable
    @Override
    public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) {
        return mediaSession;
    }

    /**
     * Called when the service is destroyed.
     * Releases media session and player resources safely to prevent leaks.
     */
    @Override
    public void onDestroy() {
        if (mediaSession != null) {
            mediaSession.release();
            mediaSession = null;
        }
        if (player != null) {
            player.release();
            player = null;
        }
        super.onDestroy();
    }
}
