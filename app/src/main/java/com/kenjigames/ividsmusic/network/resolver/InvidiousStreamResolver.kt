package com.kenjigames.ividsmusic.network.resolver

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Concrete implementation of [StreamResolver] querying a pool of public Invidious instances.
 */
class InvidiousStreamResolver(client: OkHttpClient) : StreamResolver {

    private val tag = "InvidiousResolver"

    /** Fast 3-second timeout client to avoid hanging or slow network bottlenecks */
    private val fastClient: OkHttpClient = client.newBuilder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    private val instances = arrayOf(
        "https://invidious.flokinet.to",
        "https://iv.melmac.space",
        "https://invidious.drgns.space",
        "https://invidious.perennialte.chs.org",
        "https://yt.artemislena.eu"
    )

    override suspend fun resolveVideoId(query: String): String? = withContext(Dispatchers.IO) {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        for (instance in instances) {
            try {
                val url = "$instance/api/v1/search?q=$encodedQuery&type=video"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .build()

                fastClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val jsonStr = response.body?.string() ?: ""
                        if (jsonStr.trim().startsWith("[")) {
                            val array = JSONArray(jsonStr)
                            if (array.length() > 0) {
                                val videoId = array.getJSONObject(0).optString("videoId", "")
                                if (videoId.isNotEmpty()) {
                                    Log.d(tag, "Resolved videoId: $videoId from $instance")
                                    return@withContext videoId
                                }
                            }
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.w(tag, "Instance $instance failed for search: ${e.message}")
            }
        }
        null
    }

    override suspend fun resolveAudioUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        for (instance in instances) {
            try {
                val url = "$instance/api/v1/videos/$videoId"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .build()

                fastClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val jsonStr = response.body?.string() ?: ""
                        if (jsonStr.trim().startsWith("{")) {
                            val json = JSONObject(jsonStr)
                            val formats = json.optJSONArray("adaptiveFormats")
                            if (formats != null) {
                                for (i in 0 until formats.length()) {
                                    val fmt = formats.getJSONObject(i)
                                    if (fmt.optString("type").contains("audio/")) {
                                        val streamUrl = fmt.optString("url", "")
                                        if (streamUrl.isNotEmpty()) {
                                            Log.d(tag, "Resolved stream URL from $instance")
                                            return@withContext streamUrl
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.w(tag, "Instance $instance failed for video stream: ${e.message}")
            }
        }
        null
    }
}
