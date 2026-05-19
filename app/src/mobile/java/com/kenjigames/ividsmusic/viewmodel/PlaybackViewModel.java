package com.kenjigames.ividsmusic.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.kenjigames.ividsmusic.data.Track;
import com.kenjigames.ividsmusic.player.PlaybackManager;

import java.util.List;

/**
 * ViewModel exposing the playback state of the PlaybackManager singleton.
 * Observed by activities and fragments to keep player controls and seekbars synchronized.
 */
public class PlaybackViewModel extends AndroidViewModel {

    private final PlaybackManager playbackManager;

    /**
     * Constructs the PlaybackViewModel.
     *
     * @param application The hosting Application context.
     */
    public PlaybackViewModel(@NonNull Application application) {
        super(application);
        playbackManager = PlaybackManager.getInstance();
    }

    /**
     * Gets LiveData representing the currently active track.
     */
    public LiveData<Track> getCurrentTrack() {
        return playbackManager.getCurrentTrack();
    }

    /**
     * Gets LiveData indicating if the player is currently playing audio.
     */
    public LiveData<Boolean> getIsPlaying() {
        return playbackManager.getIsPlaying();
    }

    /**
     * Gets LiveData representing the current playback position in milliseconds.
     */
    public LiveData<Long> getProgress() {
        return playbackManager.getProgress();
    }

    /**
     * Gets LiveData representing the active track duration in milliseconds.
     */
    public LiveData<Long> getDuration() {
        return playbackManager.getDuration();
    }

    /**
     * Gets LiveData containing textual playback status (e.g. Loading, Playing, Paused, etc.).
     */
    public LiveData<String> getPlaybackStatus() {
        return playbackManager.getPlaybackStatus();
    }

    /**
     * Directs the player to load and play a single track.
     *
     * @param track The track to play.
     */
    public void playTrack(Track track) {
        playbackManager.playTrack(track);
    }

    /**
     * Populates the player queue and begins playing from the given index.
     *
     * @param queue The list of tracks to queue.
     * @param index The initial track index.
     */
    public void setQueue(List<Track> queue, int index) {
        playbackManager.setQueue(queue, index);
    }

    /**
     * Toggles playback play/pause state.
     */
    public void togglePlayPause() {
        playbackManager.togglePlayPause();
    }

    /**
     * Skips to the next track in the queue.
     */
    public void next() {
        playbackManager.next();
    }

    /**
     * Skips to the previous track or restarts the current track.
     */
    public void previous() {
        playbackManager.previous();
    }

    /**
     * Seeks to a specific timestamp inside the active track.
     *
     * @param positionMs The target position in milliseconds.
     */
    public void seekTo(long positionMs) {
        playbackManager.seekTo(positionMs);
    }
}
