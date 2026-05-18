package com.kenjigames.ividsmusic

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewAssetLoader
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import org.json.JSONObject
import org.json.JSONArray
import android.webkit.JavascriptInterface
import java.io.File

/**
 * The main activity container that hosts the IVIDS Music web interface.
 * Sets up a full-screen WebView with the required settings, coordinates custom javascript
 * interfaces with the android environment, and intercepts web resource requests to perform
 * native proxy networking, audio streaming resolution, and file download management.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    
    // OkHttpClient with extended 15-second timeouts to optimize large audio downloads
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()


    private val assetLoader by lazy {
        val downloadDir = File(cacheDir, "downloads")
        if (!downloadDir.exists()) downloadDir.mkdirs()
        
        val savedDir = File(filesDir, "saved")
        if (!savedDir.exists()) savedDir.mkdirs()

        WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .addPathHandler("/temp/", WebViewAssetLoader.InternalStoragePathHandler(this, downloadDir))
            .addPathHandler("/saved/", WebViewAssetLoader.InternalStoragePathHandler(this, savedDir))
            .build()
    }

    /**
     * Called when the activity is starting. Configures full-screen layout settings,
     * initializes WebView configurations, registers the native Android interface, and initiates loading the UI.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        
        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        
        // Nodig voor cross-origin verzoeken vanuit lokale bestanden
        settings.allowFileAccessFromFileURLs = true
        settings.allowUniversalAccessFromFileURLs = true

        webView.addJavascriptInterface(AndroidAPI(), "AndroidAPI")

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val url = request?.url ?: return null
                val urlString = url.toString()
                val path = url.path ?: ""
                
                // Prioritize API requests to avoid potential conflicts with AssetLoader
                // 1. Onderschep de Proxy verzoeken (voor Deezer en YouTube scraper)
                if (path.contains("/proxy")) {
                    val targetUrl = url.getQueryParameter("url")
                    if (targetUrl != null) return handleProxyRequest(targetUrl)
                }
                
                // 2. Onderschep de Play verzoeken (voor YouTube audio)
                if (path.contains("/play")) {
                    val videoId = url.getQueryParameter("videoId")
                    if (videoId != null) return handlePlayRequest(videoId)
                }

                // 3. Onderschep de Save verzoeken (extract artist and title for formatted filename)
                if (path.contains("/save")) {
                    val videoId = url.getQueryParameter("videoId")
                    val artist = url.getQueryParameter("artist") ?: "Unknown Artist"
                    val title = url.getQueryParameter("title") ?: "Unknown Title"
                    if (videoId != null) return handleSaveRequest(videoId, artist, title)
                }

                // 4. Onderschep de Saved-tracks lijst verzoek
                if (path.contains("/saved") && !path.endsWith(".m4a") && !path.endsWith(".mp3")) {
                    return WebResourceResponse(
                        "application/json",
                        "UTF-8",
                        ByteArrayInputStream(AndroidAPI().getSavedTracks().toByteArray(StandardCharsets.UTF_8))
                    )
                }

                // fallback to assets
                val assetResponse = assetLoader.shouldInterceptRequest(url)
                if (assetResponse != null) return assetResponse

                return super.shouldInterceptRequest(view, request)
            }
        }

        // Gebruik het virtuele domein in plaats van file:///
        webView.loadUrl("https://appassets.androidplatform.net/assets/gui/index.html")
    }

    /**
     * Intercepts and proxies standard external GET requests (such as Deezer searches) natively.
     * Prevents browser CORS errors by routing external API calls through OkHttp.
     *
     * @param targetUrl The target URL to request natively.
     * @return WebResourceResponse containing the direct response input stream.
     */
    private fun handleProxyRequest(targetUrl: String): WebResourceResponse? {
        return try {
            val request = Request.Builder()
                .url(targetUrl)
                .addHeader("User-Agent", "Mozilla/5.0 (Android)")
                .build()
            val response = client.newCall(request).execute()
            val content = response.body?.string() ?: ""
            
            WebResourceResponse(
                "application/json",
                "UTF-8",
                ByteArrayInputStream(content.toByteArray(StandardCharsets.UTF_8))
            )
        } catch (e: Exception) {
            Log.e("IVIDS", "Proxy error: ${e.message}")
            createErrorResponse("Proxy error: ${e.message}")
        }
    }


    /**
     * Handles stream resolution and background audio caching for a given video ID.
     * Queries redundant Invidious instances to find a valid adaptive audio stream,
     * downloads it directly into the temporary downloads folder, and returns a local asset URL.
     *
     * @param videoId The YouTube Video ID of the track.
     * @return WebResourceResponse with local URL JSON on success, or error details.
     */
    private fun handlePlayRequest(videoId: String): WebResourceResponse? {
        Log.d("IVIDS", "Starting handlePlayRequest for videoId: $videoId")
        return try {
            val instances = arrayOf(
                "https://invidious.flokinet.to", 
                "https://iv.melmac.space", 
                "https://invidious.drgns.space",
                "https://invidious.perennialte.chs.org",
                "https://yt.artemislena.eu"
            )
            var audioUrl = ""
            
            for (instance in instances) {
                try {
                    val request = Request.Builder().url("$instance/api/v1/videos/$videoId").build()
                    val response = client.newCall(request).execute()
                    val json = JSONObject(response.body?.string() ?: "{}")
                    val streams = json.optJSONArray("adaptiveFormats")
                    
                    if (streams != null) {
                        for (i in 0 until streams.length()) {
                            val stream = streams.getJSONObject(i)
                            if (stream.optString("type").contains("audio/")) {
                                audioUrl = stream.getString("url")
                                break
                            }
                        }
                    }
                    if (audioUrl.isNotEmpty()) break
                } catch (e: Exception) { continue }
            }

            val resultJson = JSONObject()
            if (audioUrl.isNotEmpty()) {
                // Download audio file to cache folder (temp)
                val downloadDir = File(cacheDir, "downloads")
                if (!downloadDir.exists()) downloadDir.mkdirs()
                
                // Clear older files to prevent hoarding
                downloadDir.listFiles()?.forEach { if (it.isFile) it.delete() }
                
                val outputFile = File(downloadDir, "$videoId.m4a")
                
                val req = Request.Builder().url(audioUrl).build()
                val response = client.newCall(req).execute()
                
                response.body?.byteStream()?.use { input ->
                    java.io.FileOutputStream(outputFile).use { output ->
                        input.copyTo(output)
                    }
                }
                
                if (outputFile.exists() && outputFile.length() > 0) {
                    val localUrl = "https://appassets.androidplatform.net/temp/$videoId.m4a"
                    resultJson.put("status", "ready")
                    resultJson.put("url", localUrl)
                } else {
                    resultJson.put("status", "error")
                    resultJson.put("message", "File download failed or empty.")
                }
            } else {
                resultJson.put("status", "error")
                resultJson.put("message", "No audio streams found.")
            }

            WebResourceResponse(
                "application/json",
                "UTF-8",
                ByteArrayInputStream(resultJson.toString().toByteArray(StandardCharsets.UTF_8))
            )
        } catch (e: Exception) {
            Log.e("IVIDS", "Play request error: ${e.message}")
            createErrorResponse("Play error: ${e.message}")
        }
    }


    /**
     * Handles track saving requests by safely copying the cached track audio from temporary storage
     * to the permanent library storage under filesDir. The destination file is named and saved using the
     * format "Artist - Title.m4a" with safe characters, matching the structure getSavedTracks() expects.
     *
     * @param videoId The YouTube Video ID of the track.
     * @param artist The name of the artist.
     * @param title The title of the song.
     * @return WebResourceResponse containing saved status details.
     */
    private fun handleSaveRequest(videoId: String, artist: String, title: String): WebResourceResponse? {
        return try {
            val downloadDir = File(cacheDir, "downloads")
            val sourceFile = File(downloadDir, "$videoId.m4a")
            
            if (!sourceFile.exists()) {
                Log.e("IVIDS", "Save error: Source file not found in cache")
                return createErrorResponse("Source file not found")
            }

            val savedDir = File(filesDir, "saved")
            if (!savedDir.exists()) savedDir.mkdirs()
            
            // Clean illegal characters in filename while keeping standard spaces
            val cleanArtist = artist.trim().replace(Regex("[/\\\\?%*:|\"<>]"), "")
            val cleanTitle = title.trim().replace(Regex("[/\\\\?%*:|\"<>]"), "")
            val filename = "$cleanArtist - $cleanTitle.m4a"
            val destFile = File(savedDir, filename)
            
            // Move file (copy then delete source to handle potential cross-volume issues)
            sourceFile.copyTo(destFile, overwrite = true)
            
            val resultJson = JSONObject()
            if (destFile.exists() && destFile.length() > 0) {
                val localUrl = "https://appassets.androidplatform.net/saved/${Uri.encode(filename)}"
                resultJson.put("status", "saved")
                resultJson.put("message", "Track saved successfully")
                resultJson.put("url", localUrl)
            } else {
                resultJson.put("status", "error")
                resultJson.put("message", "Failed to save file.")
            }

            WebResourceResponse(
                "application/json",
                "UTF-8",
                ByteArrayInputStream(resultJson.toString().toByteArray(StandardCharsets.UTF_8))
            )
        } catch (e: Exception) {
            Log.e("IVIDS", "Save request error: ${e.message}")
            createErrorResponse("Save error: ${e.message}")
        }
    }


    /**
     * Utility method to generate a serialized JSON error response block.
     *
     * @param message The detailed error message to return.
     * @return WebResourceResponse containing the error JSON payload.
     */
    private fun createErrorResponse(message: String): WebResourceResponse {
        val errorJson = JSONObject()
        errorJson.put("status", "error")
        errorJson.put("message", message)
        return WebResourceResponse(
            "application/json",
            "UTF-8",
            ByteArrayInputStream(errorJson.toString().toByteArray(StandardCharsets.UTF_8))
        )
    }

    /**
     * Intercepts the back button event to navigate back inside the WebView history.
     * If the web interface has no back history, closes the activity/app.
     */
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    /**
     * JavaScript Interface Bridge exposed to the WebView context.
     * Declares custom interfaces mapped under window.AndroidAPI.
     */
    inner class AndroidAPI {
        
        /**
         * Scans the native filesDir/saved directory for downloaded audio tracks (.mp3 or .m4a)
         * and returns them serialized in a JSON array format to the frontend library page.
         *
         * @return String representing a JSON array of saved track details.
         */
        @JavascriptInterface
        fun getSavedTracks(): String {
            return try {
                val savedDir = File(filesDir, "saved")
                if (!savedDir.exists()) return "[]"

                val files = savedDir.listFiles() ?: return "[]"
                val jsonArray = JSONArray()

                for (file in files) {
                    if (file.isFile && (file.name.endsWith(".mp3") || file.name.endsWith(".m4a"))) {
                        val nameWithoutExt = file.nameWithoutExtension
                        val parts = nameWithoutExt.split(" - ", limit = 2)
                        val artist = if (parts.isNotEmpty()) parts[0].trim() else "Unknown Artist"
                        val title = if (parts.size > 1) parts[1].trim() else nameWithoutExt

                        val trackObj = JSONObject()
                        trackObj.put("filename", file.name)
                        trackObj.put("artist", artist)
                        trackObj.put("title", title)
                        trackObj.put("url", "https://appassets.androidplatform.net/saved/${Uri.encode(file.name)}")
                        jsonArray.put(trackObj)
                    }
                }
                jsonArray.toString()
            } catch (e: Exception) {
                Log.e("IVIDS", "getSavedTracks error: ${e.message}")
                "[]"
            }
        }
    }
}

