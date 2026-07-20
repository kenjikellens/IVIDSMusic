package com.kenjigames.ividsmusic.network.resolver

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Fallback implementation of [StreamResolver] that parses YouTube HTML search results using regex.
 */
class YouTubeHtmlScraper(private val client: OkHttpClient) : StreamResolver {

    private val tag = "YouTubeHtmlScraper"

    override suspend fun resolveVideoId(query: String): String? = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://www.youtube.com/results?search_query=$encodedQuery"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val html = response.body?.string() ?: ""
                    val regex = """"videoId":"([a-zA-Z0-9_-]{11})"""".toRegex()
                    val match = regex.find(html)
                    val videoId = match?.groupValues?.get(1)
                    if (!videoId.isNullOrEmpty()) {
                        Log.d(tag, "HTML Scraper resolved videoId: $videoId")
                        return@withContext videoId
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "HTML Scraping failed: ${e.message}")
        }
        null
    }

    override suspend fun resolveAudioUrl(videoId: String): String? {
        // Scraper resolves videoId; audio stream is fetched via primary resolvers
        return null
    }
}
