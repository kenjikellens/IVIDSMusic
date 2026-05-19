package com.kenjigames.ividsmusic.player;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

import com.kenjigames.ividsmusic.data.Track;
import com.kenjigames.ividsmusic.network.InvidiousResolver;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Singleton controller managing music playback operations.
 * Holds the ExoPlayer reference, active queue, player states,
 * and exposes LiveData channels to sync views with the playback state.
 */
public class PlaybackManager {

    private static final String TAG = "PlaybackManager";
    private static volatile PlaybackManager instance;

    private ExoPlayer player;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler progressHandler = new Handler(Looper.getMainLooper());

    // Playback state LiveData
    private final MutableLiveData<Track> currentTrack = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> isPlaying = new MutableLiveData<>(false);
    private final MutableLiveData<Long> progress = new MutableLiveData<>(0L);
    private final MutableLiveData<Long> duration = new MutableLiveData<>(0L);
    private final MutableLiveData<String> playbackStatus = new MutableLiveData<>("Idle");

    private List<Track> queue = new ArrayList<>();
    private int currentIndex = -1;

    private final Runnable progressUpdater = new Runnable() {
        @Override
        public void run() {
            if (player != null && player.isPlaying()) {
                progress.postValue(player.getCurrentPosition());
                duration.postValue(player.getDuration() > 0 ? player.getDuration() : 0L);
                progressHandler.postDelayed(this, 1000);
            }
        }
    };

    /**
     * Private constructor to enforce Singleton pattern.
     */
    private PlaybackManager() {}

    /**
     * Gets the singleton instance of PlaybackManager.
     *
     * @return The PlaybackManager instance.
     */
    public static PlaybackManager getInstance() {
        if (instance == null) {
            synchronized (PlaybackManager.class) {
                if (instance == null) {
                    instance = new PlaybackManager();
                }
            }
        }
        return instance;
    }

    /**
     * Initializes the manager with a running ExoPlayer instance.
     * Registers player listeners to update status parameters.
     *
     * @param exoPlayer The active ExoPlayer instance.
     */
    public void initialize(ExoPlayer exoPlayer) {
        this.player = exoPlayer;
        this.player.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean playing) {
                isPlaying.postValue(playing);
                if (playing) {
                    startProgressUpdate();
                    playbackStatus.postValue("Playing");
                } else {
                    stopProgressUpdate();
                    if (player.getPlaybackState() == Player.STATE_BUFFERING) {
                        playbackStatus.postValue("Buffering");
                    } else if (player.getPlaybackState() == Player.STATE_ENDED) {
                        playbackStatus.postValue("Ended");
                        next(); // Auto-skip to next
                    } else {
                        playbackStatus.postValue("Paused");
                    }
                }
            }

            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_BUFFERING) {
                    playbackStatus.postValue("Buffering");
                } else if (state == Player.STATE_READY) {
                    playbackStatus.postValue(player.isPlaying() ? "Playing" : "Paused");
                    duration.postValue(player.getDuration() > 0 ? player.getDuration() : 0L);
                } else if (state == Player.STATE_ENDED) {
                    playbackStatus.postValue("Ended");
                    next(); // Circular playback behavior
                }
            }
        });
    }

    /**
     * Starts the periodic seekbar/progress LiveData updates.
     */
    private void startProgressUpdate() {
        stopProgressUpdate();
        progressHandler.post(progressUpdater);
    }

    /**
     * Stops the periodic progress updater.
     */
    private void stopProgressUpdate() {
        progressHandler.removeCallbacks(progressUpdater);
    }

    /**
     * Sets the active queue and starts playing from the specified index.
     *
     * @param trackList The list of tracks in the queue.
     * @param index The initial index to play.
     */
    public void setQueue(List<Track> trackList, int index) {
        if (trackList == null || trackList.isEmpty()) return;
        this.queue = new ArrayList<>(trackList);
        this.currentIndex = Math.max(0, Math.min(index, queue.size() - 1));
        loadTrack(queue.get(currentIndex));
    }

    /**
     * Plays a single track instantly, clearing any active queue.
     *
     * @param track The track to play.
     */
    public void playTrack(Track track) {
        this.queue.clear();
        this.queue.add(track);
        this.currentIndex = 0;
        loadTrack(track);
    }

    /**
     * Resolves the target audio source and sets up ExoPlayer.
     * Handles local files or streams using Invidious searches on a background thread.
     *
     * @param track The track to load.
     */
    private void loadTrack(Track track) {
        if (player == null || track == null) return;

        currentTrack.postValue(track);
        isPlaying.postValue(false);
        progress.postValue(0L);
        duration.postValue(0L);
        playbackStatus.postValue("Loading...");

        executorService.execute(() -> {
            try {
                String sourceUrl = null;

                // Check if file is downloaded locally
                if (track.isDownloaded() && track.getLocalFilePath() != null) {
                    File localFile = new File(track.getLocalFilePath());
                    if (localFile.exists()) {
                        sourceUrl = localFile.getAbsolutePath();
                    }
                }

                // If not downloaded, resolve streaming URL via Invidious
                if (sourceUrl == null) {
                    String query = track.getArtist() + " - " + track.getTitle();
                    String videoId = track.getVideoId();
                    
                    if (videoId == null || videoId.isEmpty()) {
                        videoId = InvidiousResolver.resolveVideoId(query);
                        if (videoId != null) {
                            track.setVideoId(videoId);
                            // We could save videoId to DB later
                        }
                    }

                    if (videoId != null) {
                        sourceUrl = InvidiousResolver.resolveAudioStreamUrl(videoId);
                    }
                }

                if (sourceUrl != null) {
                    final String streamUrl = sourceUrl;
                    new Handler(Looper.getMainLooper()).post(() -> {
                        try {
                            MediaItem mediaItem = MediaItem.fromUri(streamUrl);
                            player.setMediaItem(mediaItem);
                            player.prepare();
                            player.play();
                        } catch (Exception e) {
                            Log.e(TAG, "ExoPlayer setup failed", e);
                            playbackStatus.postValue("Error: Player setup failed");
                        }
                    });
                } else {
                    Log.e(TAG, "Could not resolve audio source url");
                    playbackStatus.postValue("Error: Source resolution failed");
                }
            } catch (Exception e) {
                Log.e(TAG, "Background stream resolution failed", e);
                playbackStatus.postValue("Error: Stream resolution failed");
            }
        });
    }

    /**
     * Toggles play/pause playback state.
     */
    public void togglePlayPause() {
        if (player == null) return;
        if (player.isPlaying()) {
            player.pause();
        } else {
            if (player.getPlaybackState() == Player.STATE_IDLE) {
                if (currentTrack.getValue() != null) {
                    loadTrack(currentTrack.getValue());
                }
            } else {
                player.play();
            }
        }
    }

    /**
     * Plays the next track in the queue circularly.
     */
    public void next() {
        if (queue.isEmpty() || currentIndex == -1) return;
        currentIndex = (currentIndex + 1) % queue.size();
        loadTrack(queue.get(currentIndex));
    }

    /**
     * Plays the previous track in the queue, or restarts current track if played > 3s.
     */
    public void previous() {
        if (player != null && player.getCurrentPosition() > 3000) {
            player.seekTo(0);
            return;
        }
        if (queue.isEmpty() || currentIndex == -1) return;
        currentIndex = (currentIndex - 1 + queue.size()) % queue.size();
        loadTrack(queue.get(currentIndex));
    }

    /**
     * Seeks to a specific timestamp inside the active track.
     *
     * @param positionMs The target position in milliseconds.
     */
    public void seekTo(long positionMs) {
        if (player != null) {
            player.seekTo(positionMs);
        }
    }

    // Getters for observing LiveData playback parameters

    public LiveData<Track> getCurrentTrack() {
        return currentTrack;
    }

    public LiveData<Boolean> getIsPlaying() {
        return isPlaying;
    }

    public LiveData<Long> getProgress() {
        return progress;
    }

    public LiveData<Long> getDuration() {
        return duration;
    }

    public LiveData<String> getPlaybackStatus() {
        return playbackStatus;
    }
}
