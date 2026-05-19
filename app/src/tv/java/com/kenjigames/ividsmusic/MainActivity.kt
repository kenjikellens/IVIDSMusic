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
 * The main activity container that hosts the IVIDS Music web interface for the Android TV app (WebView wrapper).
 * Sets up a full-screen WebView, registers Javascript interfaces, and intercepts web requests to perform native
 * proxy networking, stream URL resolution, and local file storage operations.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var updateManager: UpdateManager? = null
    
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
     * Called when the TV activity is created. Configures settings, sets up Javascript interfaces,
     * hooks up shouldInterceptRequest, and triggers loading the HTML assets.
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
        
        settings.allowFileAccessFromFileURLs = true
        settings.allowUniversalAccessFromFileURLs = true

        webView.addJavascriptInterface(AndroidAPI(), "AndroidAPI")

        updateManager = UpdateManager(this, webView)
        webView.addJavascriptInterface(updateManager!!, "AndroidUpdate")

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val url = request?.url ?: return null
                val urlString = url.toString()
                val path = url.path ?: ""
                
                if (path.contains("/proxy")) {
                    val targetUrl = url.getQueryParameter("url")
                    if (targetUrl != null) return handleProxyRequest(targetUrl)
                }
                
                if (path.contains("/play")) {
                    val videoId = url.getQueryParameter("videoId")
                    if (videoId != null) return handlePlayRequest(videoId)
                }

                if (path.contains("/save")) {
                    val videoId = url.getQueryParameter("videoId")
                    val artist = url.getQueryParameter("artist") ?: "Unknown Artist"
                    val title = url.getQueryParameter("title") ?: "Unknown Title"
                    if (videoId != null) return handleSaveRequest(videoId, artist, title)
                }

                if (path.contains("/saved") && !path.endsWith(".m4a") && !path.endsWith(".mp3")) {
                    return WebResourceResponse(
                        "application/json",
                        "UTF-8",
                        ByteArrayInputStream(AndroidAPI().getSavedTracks().toByteArray(StandardCharsets.UTF_8))
                    )
                }

                val assetResponse = assetLoader.shouldInterceptRequest(url)
                if (assetResponse != null) return assetResponse

                return super.shouldInterceptRequest(view, request)
            }
        }

        webView.loadUrl("https://appassets.androidplatform.net/assets/gui/index.html")
    }

    /**
     * Intercepts and proxies external Deezer searches natively to prevent CORS blockers.
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
     * Resolves streams and downloads audio to cache for playback inside the WebView.
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
                val downloadDir = File(cacheDir, "downloads")
                if (!downloadDir.exists()) downloadDir.mkdirs()
                
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
     * Copy track audio from cache to permanent filesDir/saved folder formatted as "Artist - Title.m4a".
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
            
            val cleanArtist = artist.trim().replace(Regex("[/\\\\?%*:|\"<>]"), "")
            val cleanTitle = title.trim().replace(Regex("[/\\\\?%*:|\"<>]"), "")
            val filename = "$cleanArtist - $cleanTitle.m4a"
            val destFile = File(savedDir, filename)
            
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
     * Generates error response payload.
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
     * Supports back navigation within the web layout history.
     */
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    /**
     * Cleans up background references on destroy.
     */
    override fun onDestroy() {
        updateManager?.shutdown()
        super.onDestroy()
    }

    /**
     * Exposes track library scanner helper to JavaScript context under window.AndroidAPI.
     */
    inner class AndroidAPI {
        
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
