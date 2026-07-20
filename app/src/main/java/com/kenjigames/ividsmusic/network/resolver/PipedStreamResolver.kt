package com.kenjigames.ividsmusic.network.resolver

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Concrete implementation of [StreamResolver] querying Piped API instances for full-length audio streams.
 */
class PipedStreamResolver(client: OkHttpClient) : StreamResolver {

    private val tag = "PipedStreamResolver"

    private val fastClient: OkHttpClient = client.newBuilder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val pipedInstances = arrayOf(
        "https://pipedapi.kavin.rocks",
        "https://api.piped.video",
        "https://pipedapi.drgns.space"
    )

    override suspend fun resolveVideoId(query: String): String? = withContext(Dispatchers.IO) {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        for (instance in pipedInstances) {
            try {
                val url = "$instance/search?q=$encodedQuery&filter=music_songs"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .build()

                fastClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val jsonStr = response.body?.string() ?: ""
                        val json = JSONObject(jsonStr)
                        val items = json.optJSONArray("items")
                        if (items != null && items.length() > 0) {
                            val firstItem = items.getJSONObject(0)
                            val urlPath = firstItem.optString("url", "")
                            val videoId = urlPath.replace("/watch?v=", "")
                            if (videoId.isNotEmpty()) {
                                Log.d(tag, "Resolved videoId: $videoId from Piped instance: $instance")
                                return@withContext videoId
                            }
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.w(tag, "Piped instance $instance failed search: ${e.message}")
            }
        }
        null
    }

    override suspend fun resolveAudioUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        for (instance in pipedInstances) {
            try {
                val url = "$instance/streams/$videoId"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .build()

                fastClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val jsonStr = response.body?.string() ?: ""
                        val json = JSONObject(jsonStr)
                        val audioStreams = json.optJSONArray("audioStreams")
                        if (audioStreams != null && audioStreams.length() > 0) {
                            // Pick highest bitrate audio stream
                            var bestUrl: String? = null
                            var maxBitrate = -1

                            for (i in 0 until audioStreams.length()) {
                                val stream = audioStreams.getJSONObject(i)
                                val bitrate = stream.optInt("bitrate", 0)
                                val streamUrl = stream.optString("url", "")
                                if (streamUrl.isNotEmpty() && bitrate > maxBitrate) {
                                    maxBitrate = bitrate
                                    bestUrl = streamUrl
                                }
                            }
                            if (bestUrl != null) {
                                Log.d(tag, "Resolved full audio stream URL from Piped instance: $instance")
                                return@withContext bestUrl
                            }
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.w(tag, "Piped instance $instance failed stream fetch: ${e.message}")
            }
        }
        null
    }
}
