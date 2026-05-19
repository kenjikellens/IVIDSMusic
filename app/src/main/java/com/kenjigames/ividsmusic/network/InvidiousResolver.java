package com.kenjigames.ividsmusic.network;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Resolver class to search for YouTube video IDs and resolve direct audio stream URLs
 * by querying a pool of public Invidious instances using OkHttp.
 */
public class InvidiousResolver {

    private static final String TAG = "InvidiousResolver";
    
    private static final String[] INSTANCES = {
        "https://invidious.flokinet.to",
        "https://iv.melmac.space",
        "https://invidious.drgns.space",
        "https://invidious.perennialte.chs.org",
        "https://yt.artemislena.eu"
    };

    private static final OkHttpClient client = new OkHttpClient();

    /**
     * Searches for a YouTube video ID matching the query.
     * Iterates through the Invidious instances pool until a successful response is found.
     *
     * @param query The search query (e.g. "Artist Name - Song Title").
     * @return The resolved YouTube video ID, or null if resolution fails.
     */
    public static String resolveVideoId(String query) {
        for (String instance : INSTANCES) {
            try {
                String encodedQuery = URLEncoder.encode(query, "UTF-8");
                String url = instance + "/api/v1/search?q=" + encodedQuery + "&type=video";

                Request request = new Request.Builder()
                        .url(url)
                        .header("User-Agent", "Mozilla/5.0")
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String jsonResponse = response.body().string();
                        JSONArray jsonArray = new JSONArray(jsonResponse);
                        if (jsonArray.length() > 0) {
                            JSONObject firstResult = jsonArray.getJSONObject(0);
                            String videoId = firstResult.optString("videoId", "");
                            if (!videoId.isEmpty()) {
                                Log.d(TAG, "Successfully resolved videoId: " + videoId + " from instance: " + instance);
                                return videoId;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed resolving search from instance: " + instance + ", trying next. Error: " + e.getMessage());
            }
        }
        return null;
    }

    /**
     * Resolves the direct audio stream URL for a given YouTube video ID.
     * Queries the Invidious video detail API and filters adaptive formats for the audio stream.
     *
     * @param videoId The YouTube video ID.
     * @return The direct streaming audio URL, or null if resolution fails.
     */
    public static String resolveAudioStreamUrl(String videoId) {
        for (String instance : INSTANCES) {
            try {
                String url = instance + "/api/v1/videos/" + videoId;

                Request request = new Request.Builder()
                        .url(url)
                        .header("User-Agent", "Mozilla/5.0")
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String jsonResponse = response.body().string();
                        JSONObject jsonObject = new JSONObject(jsonResponse);
                        JSONArray adaptiveFormats = jsonObject.optJSONArray("adaptiveFormats");
                        if (adaptiveFormats != null) {
                            for (int i = 0; i < adaptiveFormats.length(); i++) {
                                JSONObject format = adaptiveFormats.getJSONObject(i);
                                String type = format.optString("type", "");
                                if (type.contains("audio/")) {
                                    String streamUrl = format.optString("url", "");
                                    if (!streamUrl.isEmpty()) {
                                        Log.d(TAG, "Resolved audio stream url from instance: " + instance);
                                        return streamUrl;
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed resolving video streams from instance: " + instance + ", trying next. Error: " + e.getMessage());
            }
        }
        return null;
    }
}
