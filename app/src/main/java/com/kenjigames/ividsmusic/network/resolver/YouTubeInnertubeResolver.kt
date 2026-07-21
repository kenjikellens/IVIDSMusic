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
 * Direct YouTube Innertube API stream resolver querying YouTube's native client endpoints (iOS, Android, Web).
 * Returns direct high-quality googlevideo.com audio streams without proxy dependencies.
 */
class YouTubeInnertubeResolver(client: OkHttpClient) : StreamResolver {

    private val tag = "YouTubeInnertubeResolver"

    private val fastClient: OkHttpClient = client.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    override suspend fun resolveVideoId(query: String): String? = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://www.youtube.com/results?search_query=$encodedQuery"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()

            fastClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val html = response.body?.string() ?: ""
                    val videoIdRegex = """"videoId":"([a-zA-Z0-9_-]{11})"""".toRegex()
                    val watchRegex = """/watch\?v=([a-zA-Z0-9_-]{11})""".toRegex()

                    val match = videoIdRegex.find(html) ?: watchRegex.find(html)
                    val videoId = match?.groupValues?.get(1)
                    if (!videoId.isNullOrEmpty()) {
                        Log.d(tag, "Resolved videoId: $videoId for query '$query'")
                        return@withContext videoId
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed resolving videoId for query '$query': ${e.message}")
        }
        null
    }

    override suspend fun resolveAudioUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        // 1. Try iOS client payload (highest success rate for direct unencrypted audio stream URLs)
        val iosStream = queryPlayerEndpoint(
            videoId = videoId,
            clientName = "IOS",
            clientVersion = "19.22.1",
            userAgent = "com.google.ios.youtube/19.22.1 (iPhone16,2; U; CPU iOS 17_5_1 like Mac OS X; en_US)",
            osName = "iOS",
            osVersion = "17.5.1.21F90",
            deviceModel = "iPhone16,2"
        )
        if (iosStream != null) {
            Log.d(tag, "Resolved direct audio URL via iOS Innertube client")
            return@withContext iosStream
        }

        // 2. Try Android client payload
        val androidStream = queryPlayerEndpoint(
            videoId = videoId,
            clientName = "ANDROID",
            clientVersion = "19.02.39",
            userAgent = "com.google.android.youtube/19.02.39 (Linux; U; Android 14; US)",
            osName = "Android",
            osVersion = "14",
            deviceModel = null
        )
        if (androidStream != null) {
            Log.d(tag, "Resolved direct audio URL via Android Innertube client")
            return@withContext androidStream
        }

        // 3. Try Web Remix (YouTube Music) client payload
        val webStream = queryPlayerEndpoint(
            videoId = videoId,
            clientName = "WEB_REMIX",
            clientVersion = "1.20240408.01.00",
            userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36",
            osName = "Windows",
            osVersion = "10.0",
            deviceModel = null
        )
        if (webStream != null) {
            Log.d(tag, "Resolved direct audio URL via Web Remix Innertube client")
            return@withContext webStream
        }

        Log.w(tag, "All Innertube client payloads failed to extract direct audio URL for videoId: $videoId")
        null
    }

    private fun queryPlayerEndpoint(
        videoId: String,
        clientName: String,
        clientVersion: String,
        userAgent: String,
        osName: String,
        osVersion: String,
        deviceModel: String?
    ): String? {
        try {
            val url = "https://www.youtube.com/youtubei/v1/player"
            val clientJson = JSONObject().apply {
                put("clientName", clientName)
                put("clientVersion", clientVersion)
                put("osName", osName)
                put("osVersion", osVersion)
                if (deviceModel != null) put("deviceModel", deviceModel)
                put("hl", "en")
                put("gl", "US")
            }

            val jsonPayload = JSONObject().apply {
                put("videoId", videoId)
                put("context", JSONObject().apply {
                    put("client", clientJson)
                })
            }

            val requestBody = jsonPayload.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .header("User-Agent", userAgent)
                .header("Content-Type", "application/json")
                .build()

            fastClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val jsonStr = response.body?.string() ?: ""
                    val json = JSONObject(jsonStr)
                    val streamingData = json.optJSONObject("streamingData") ?: return null

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
                            return bestUrl
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "queryPlayerEndpoint ($clientName) exception: ${e.message}")
        }
        null
    }
}
