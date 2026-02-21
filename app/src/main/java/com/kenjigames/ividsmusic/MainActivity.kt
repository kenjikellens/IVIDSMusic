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

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private val client = OkHttpClient()

    // Helpt bij het laden van lokale bestanden via een virtueel domein (voorkomt fetch blokkades)
    private val assetLoader by lazy {
        WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
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

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val url = request?.url ?: return null
                
                // 1. Onderschep verzoeken naar de lokale assets via het virtuele domein
                val assetResponse = assetLoader.shouldInterceptRequest(url)
                if (assetResponse != null) return assetResponse

                // 2. Onderschep de Proxy verzoeken (voor Deezer)
                if (url.toString().contains("localhost:3000/proxy?url=")) {
                    val targetUrl = url.getQueryParameter("url") ?: return null
                    return handleProxyRequest(targetUrl)
                }
                
                // 3. Onderschep de Play verzoeken (voor YouTube audio)
                if (url.toString().contains("localhost:3000/play")) {
                    val videoId = url.getQueryParameter("videoId") ?: return null
                    return handlePlayRequest(videoId)
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
                resultJson.put("status", "ready")
                resultJson.put("url", audioUrl)
            } else {
                resultJson.put("status", "error")
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
}
