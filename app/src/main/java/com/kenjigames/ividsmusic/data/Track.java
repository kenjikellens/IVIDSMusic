package com.kenjigames.ividsmusic.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entity class representing a music track stored in the local SQLite Room database.
 * Contains track metadata, offline availability details, and download states.
 */
@Entity(tableName = "tracks")
public class Track {

    @PrimaryKey
    @NonNull
    private String id;
    
    private String title;
    private String artist;
    private String cover;
    private String localFilePath;
    private String videoId;
    private boolean isDownloaded;
    private long timestamp;

    /**
     * Default constructor required by Room.
     */
    public Track() {
        this.id = "";
    }

    /**
     * Parameterized constructor to initialize a track with ID, title, artist, and cover art.
     *
     * @param id The unique identifier for the track (usually Deezer track ID).
     * @param title The title of the track.
     * @param artist The artist of the track.
     * @param cover The URL or local path of the track cover image.
     */
    public Track(@NonNull String id, String title, String artist, String cover) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.cover = cover;
        this.isDownloaded = false;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Gets the unique ID of the track.
     *
     * @return The track ID.
     */
    @NonNull
    public String getId() {
        return id;
    }

    /**
     * Sets the unique ID of the track.
     *
     * @param id The track ID to set.
     */
    public void setId(@NonNull String id) {
        this.id = id;
    }

    /**
     * Gets the title of the track.
     *
     * @return The track title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title of the track.
     *
     * @param title The track title to set.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Gets the artist of the track.
     *
     * @return The artist name.
     */
    public String getArtist() {
        return artist;
    }

    /**
     * Sets the artist of the track.
     *
     * @param artist The artist name to set.
     */
    public void setArtist(String artist) {
        this.artist = artist;
    }

    /**
     * Gets the cover image URL or local path.
     *
     * @return The cover image path.
     */
    public String getCover() {
        return cover;
    }

    /**
     * Sets the cover image URL or local path.
     *
     * @param cover The cover image path to set.
     */
    public void setCover(String cover) {
        this.cover = cover;
    }

    /**
     * Gets the local file storage path.
     *
     * @return The path where the offline audio is saved.
     */
    public String getLocalFilePath() {
        return localFilePath;
    }

    /**
     * Sets the local file storage path.
     *
     * @param localFilePath The disk storage path to set.
     */
    public void setLocalFilePath(String localFilePath) {
        this.localFilePath = localFilePath;
    }

    /**
     * Gets the YouTube video identifier used to resolve the stream.
     *
     * @return The YouTube video ID.
     */
    public String getVideoId() {
        return videoId;
    }

    /**
     * Sets the YouTube video identifier.
     *
     * @param videoId The YouTube video ID to set.
     */
    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    /**
     * Checks if the track is saved and available for offline playback.
     *
     * @return True if the track is downloaded, False otherwise.
     */
    public boolean isDownloaded() {
        return isDownloaded;
    }

    /**
     * Sets the download state of the track.
     *
     * @param downloaded The download state to set.
     */
    public void setDownloaded(boolean downloaded) {
        isDownloaded = downloaded;
    }

    /**
     * Gets the timestamp when this track entry was saved or created.
     *
     * @return The creation timestamp.
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the creation timestamp.
     *
     * @param timestamp The timestamp to set.
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
