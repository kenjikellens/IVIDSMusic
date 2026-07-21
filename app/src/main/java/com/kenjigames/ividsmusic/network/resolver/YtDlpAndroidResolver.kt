package com.kenjigames.ividsmusic.network.resolver

import android.content.Context
import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Native Android StreamResolver executing the actual yt-dlp binary engine via youtubedl-android.
 */
class YtDlpAndroidResolver(private val context: Context) : StreamResolver {

    private val tag = "YtDlpAndroidResolver"
    private var isInitialized = false

    private fun ensureInitialized() {
        if (!isInitialized) {
            try {
                YoutubeDL.getInstance().init(context)
                isInitialized = true
                Log.d(tag, "Native yt-dlp Android engine initialized successfully")
            } catch (e: Throwable) {
                Log.e(tag, "Failed initializing yt-dlp Android engine: ${e.message}", e)
            }
        }
    }

    override suspend fun resolveVideoId(query: String): String? = withContext(Dispatchers.IO) {
        ensureInitialized()
        try {
            val request = YoutubeDLRequest("ytsearch1:$query")
            request.addOption("--get-id")
            val response = YoutubeDL.getInstance().execute(request)
            val videoId = response.out.trim()
            if (videoId.isNotEmpty()) {
                Log.d(tag, "yt-dlp Android resolved videoId: $videoId")
                return@withContext videoId
            }
        } catch (e: Throwable) {
            Log.w(tag, "yt-dlp Android search failed for query '$query': ${e.message}")
        }
        null
    }

    override suspend fun resolveAudioUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        ensureInitialized()
        try {
            val url = "https://www.youtube.com/watch?v=$videoId"
            val request = YoutubeDLRequest(url)
            request.addOption("-f", "bestaudio/best")
            request.addOption("-g")
            val response = YoutubeDL.getInstance().execute(request)
            val audioUrl = response.out.trim()
            if (audioUrl.isNotEmpty()) {
                Log.d(tag, "yt-dlp Android resolved audio URL for videoId: $videoId")
                return@withContext audioUrl
            }
        } catch (e: Throwable) {
            Log.w(tag, "yt-dlp Android audio resolution failed for videoId '$videoId': ${e.message}")
        }
        null
    }
}
