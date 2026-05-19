package com.kenjigames.ividsmusic.network;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Retrofit interface defining endpoints for the Deezer API to search for songs
 * and retrieve detailed track metadata.
 */
public interface DeezerApi {

    /**
     * Executes a general track search query.
     * Maps to: GET https://api.deezer.com/search?q={query}
     *
     * @param query The search keyword (e.g., "Artist - Song Title").
     * @return Retrofit call yielding the search response structure.
     */
    @GET("search")
    Call<DeezerSearchResponse> searchTracks(@Query("q") String query);

    /**
     * Fetches detailed metadata for a specific track by its ID.
     * Maps to: GET https://api.deezer.com/track/{id}
     *
     * @param id The unique Deezer track ID.
     * @return Retrofit call yielding the track metadata details.
     */
    @GET("track/{id}")
    Call<DeezerSearchResponse.TrackData> getTrackDetails(@Path("id") String id);
}
