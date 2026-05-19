package com.kenjigames.ividsmusic.network;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Data model representing the search response structure returned by the Deezer API.
 * Designed to be parsed automatically using GSON.
 */
public class DeezerSearchResponse {

    @SerializedName("data")
    private List<TrackData> data;

    /**
     * Gets the list of track search results.
     *
     * @return The list of track data results.
     */
    public List<TrackData> getData() {
        return data;
    }

    /**
     * Sets the list of track search results.
     *
     * @param data The track data results list.
     */
    public void setData(List<TrackData> data) {
        this.data = data;
    }

    /**
     * Nested class modeling the details of a single track within the search results.
     */
    public static class TrackData {
        private String id;
        private String title;
        private int duration;
        private ArtistData artist;
        private AlbumData album;

        /**
         * Gets the track ID.
         *
         * @return The unique track ID.
         */
        public String getId() {
            return id;
        }

        /**
         * Sets the track ID.
         *
         * @param id The track ID.
         */
        public void setId(String id) {
            this.id = id;
        }

        /**
         * Gets the track title.
         *
         * @return The track title.
         */
        public String getTitle() {
            return title;
        }

        /**
         * Sets the track title.
         *
         * @param title The track title.
         */
        public void setTitle(String title) {
            this.title = title;
        }

        /**
         * Gets the duration of the track in seconds.
         *
         * @return The track duration.
         */
        public int getDuration() {
            return duration;
        }

        /**
         * Sets the track duration.
         *
         * @param duration The track duration in seconds.
         */
        public void setDuration(int duration) {
            this.duration = duration;
        }

        /**
         * Gets the artist details of the track.
         *
         * @return The artist details.
         */
        public ArtistData getArtist() {
            return artist;
        }

        /**
         * Sets the artist details of the track.
         *
         * @param artist The artist details.
         */
        public void setArtist(ArtistData artist) {
            this.artist = artist;
        }

        /**
         * Gets the album details of the track.
         *
         * @return The album details.
         */
        public AlbumData getAlbum() {
            return album;
        }

        /**
         * Sets the album details of the track.
         *
         * @param album The album details.
         */
        public void setAlbum(AlbumData album) {
            this.album = album;
        }

        /**
         * Nested class representing the artist of a track in search results.
         */
        public static class ArtistData {
            private String id;
            private String name;

            /**
             * Gets the artist ID.
             *
             * @return The artist ID.
             */
            public String getId() {
                return id;
            }

            /**
             * Sets the artist ID.
             *
             * @param id The artist ID.
             */
            public void setId(String id) {
                this.id = id;
            }

            /**
             * Gets the artist name.
             *
             * @return The artist name.
             */
            public String getName() {
                return name;
            }

            /**
             * Sets the artist name.
             *
             * @param name The artist name.
             */
            public void setName(String name) {
                this.name = name;
            }
        }

        /**
         * Nested class representing the album of a track in search results.
         */
        public static class AlbumData {
            private String id;
            private String title;
            
            @SerializedName("cover_medium")
            private String coverMedium;

            /**
             * Gets the album ID.
             *
             * @return The album ID.
             */
            public String getId() {
                return id;
            }

            /**
             * Sets the album ID.
             *
             * @param id The album ID.
             */
            public void setId(String id) {
                this.id = id;
            }

            /**
             * Gets the album title.
             *
             * @return The album title.
             */
            public String getTitle() {
                return title;
            }

            /**
             * Sets the album title.
             *
             * @param title The album title.
             */
            public void setTitle(String title) {
                this.title = title;
            }

            /**
             * Gets the cover image URL (medium resolution).
             *
             * @return The cover image URL.
             */
            public String getCoverMedium() {
                return coverMedium;
            }

            /**
             * Sets the cover image URL.
             *
             * @param coverMedium The cover image URL.
             */
            public void setCoverMedium(String coverMedium) {
                this.coverMedium = coverMedium;
            }
        }
    }
}
