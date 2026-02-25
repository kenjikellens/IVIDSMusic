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

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private val client = OkHttpClient()

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
                
                // 1. Onderschep verzoeken naar de lokale assets via het virtuele domein
                val assetResponse = assetLoader.shouldInterceptRequest(url)
                if (assetResponse != null) return assetResponse

                // 2. Onderschep de Proxy verzoeken (voor Deezer en YouTube scraper)
                if (url.toString().contains("localhost:3000/proxy") || url.toString().contains("/api/proxy")) {
                    val targetUrl = url.getQueryParameter("url") ?: return null
                    return handleProxyRequest(targetUrl)
                }
                
                // 3. Onderschep de Play verzoeken (voor YouTube audio)
                if (url.toString().contains("localhost:3000/play") || url.toString().contains("/api/play")) {
                    val videoId = url.getQueryParameter("videoId") ?: return null
                    return handlePlayRequest(videoId)
                }

                // 4. Onderschep de Save verzoeken
                if (url.toString().contains("localhost:3000/save") || url.toString().contains("/api/save")) {
                    val videoId = url.getQueryParameter("videoId") ?: return null
                    return handleSaveRequest(videoId)
                }

                return super.shouldInterceptRequest(view, request)
            }
        }

        // Gebruik het virtuele domein in plaats van file:///
        webView.loadUrl("https://appassets.androidplatform.net/assets/gui/index.html")
    }

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

    private fun handlePlayRequest(videoId: String): WebResourceResponse? {
        return try {
            val instances = arrayOf("https://invidious.flokinet.to", "https://iv.melmac.space", "https://invidious.drgns.space")
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

    private fun handleSaveRequest(videoId: String): WebResourceResponse? {
        return try {
            val downloadDir = File(cacheDir, "downloads")
            val sourceFile = File(downloadDir, "$videoId.m4a")
            
            if (!sourceFile.exists()) {
                Log.e("IVIDS", "Save error: Source file not found in cache")
                return createErrorResponse("Source file not found")
            }

            val savedDir = File(filesDir, "saved")
            if (!savedDir.exists()) savedDir.mkdirs()
            
            val destFile = File(savedDir, "$videoId.m4a")
            
            // Move file (copy then delete source to handle potential cross-volume issues)
            sourceFile.copyTo(destFile, overwrite = true)
            
            val resultJson = JSONObject()
            if (destFile.exists() && destFile.length() > 0) {
                val localUrl = "https://appassets.androidplatform.net/saved/$videoId.m4a"
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

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

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
