package com.kenjigames.ividsmusic.network.resolver

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Direct YouTube Innertube API stream resolver querying YouTube's native Android client endpoints.
 * Returns direct high-quality googlevideo.com audio streams without proxy dependencies.
 */
class YouTubeInnertubeResolver(client: OkHttpClient) : StreamResolver {

    private val tag = "YouTubeInnertubeResolver"

    private val fastClient: OkHttpClient = client.newBuilder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    override suspend fun resolveVideoId(query: String): String? = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://www.youtube.com/results?search_query=$encodedQuery"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            fastClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val html = response.body?.string() ?: ""
                    val regex = """"videoId":"([a-zA-Z0-9_-]{11})"""".toRegex()
                    val match = regex.find(html)
                    val videoId = match?.groupValues?.get(1)
                    if (!videoId.isNullOrEmpty()) {
                        Log.d(tag, "Resolved videoId: $videoId")
                        return@withContext videoId
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed resolving videoId: ${e.message}")
        }
        null
    }

    override suspend fun resolveAudioUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = "https://www.youtube.com/youtubei/v1/player"
            val jsonPayload = JSONObject().apply {
                put("videoId", videoId)
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "ANDROID")
                        put("clientVersion", "19.02.39")
                        put("androidSdkVersion", 34)
                        put("hl", "en")
                        put("gl", "US")
                    })
                })
            }

            val requestBody = jsonPayload.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .header("User-Agent", "com.google.android.youtube/19.02.39 (Linux; U; Android 14; US)")
                .header("Content-Type", "application/json")
                .build()

            fastClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val jsonStr = response.body?.string() ?: ""
                    val json = JSONObject(jsonStr)
                    val streamingData = json.optJSONObject("streamingData")
                    if (streamingData != null) {
                        val adaptiveFormats = streamingData.optJSONArray("adaptiveFormats")
                        if (adaptiveFormats != null && adaptiveFormats.length() > 0) {
                            var bestUrl: String? = null
                            var maxBitrate = -1

                            for (i in 0 until adaptiveFormats.length()) {
                                val fmt = adaptiveFormats.getJSONObject(i)
                                val mimeType = fmt.optString("mimeType", "")
                                if (mimeType.contains("audio/")) {
                                    val streamUrl = fmt.optString("url", "")
                                    val bitrate = fmt.optInt("bitrate", 0)
                                    if (streamUrl.isNotEmpty() && bitrate > maxBitrate) {
                                        maxBitrate = bitrate
                                        bestUrl = streamUrl
                                    }
                                }
                            }

                            if (bestUrl != null) {
                                Log.d(tag, "Resolved direct YouTube audio stream URL (bitrate: $maxBitrate)")
                                return@withContext bestUrl
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed resolving direct audio URL: ${e.message}")
        }
        null
    }
}
