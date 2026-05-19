package com.kenjigames.ividsmusic;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebViewAssetLoader;

import com.kenjigames.ividsmusic.player.PlaybackService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Main Activity for the native Android mobile app flavor.
 * Hosts the IVIDS Music responsive web interface inside a fullscreen WebView,
 * replicating the TV flavor architecture. The responsive CSS stylesheet
 * automatically renders a portrait-optimized bottom navigation bar with all 6 tabs
 * (Home, For You, Search, Library, You, Settings) on mobile devices.
 *
 * Audio playback is handled via the native PlaybackService (ExoPlayer) for
 * background playback, lock-screen controls, and Bluetooth integration.
 *
 * Network requests for Deezer API proxying, YouTube stream resolution, and
 * local track storage are intercepted via WebViewClient.shouldInterceptRequest().
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "IVIDS";
    private WebView webView;
    private UpdateManager updateManager;

    /**
     * OkHttpClient instance configured with 15-second timeouts for audio stream
     * downloads and external API proxy requests.
     */
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build();

    /**
     * Lazy-initialized WebViewAssetLoader that maps local asset and storage directories
     * to the secure appassets.androidplatform.net domain, enabling the WebView to load
     * HTML/CSS/JS from the assets folder and cached/saved audio from internal storage.
     */
    private WebViewAssetLoader assetLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Prepare internal storage directories for audio cache and saved tracks
        File downloadDir = new File(getCacheDir(), "downloads");
        if (!downloadDir.exists()) downloadDir.mkdirs();

        File savedDir = new File(getFilesDir(), "saved");
        if (!savedDir.exists()) savedDir.mkdirs();

        // Build the WebViewAssetLoader with path handlers for assets, cached audio, and saved audio
        assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .addPathHandler("/temp/", new WebViewAssetLoader.InternalStoragePathHandler(this, downloadDir))
                .addPathHandler("/saved/", new WebViewAssetLoader.InternalStoragePathHandler(this, savedDir))
                .build();

        // Configure WebView with JavaScript, DOM storage, and file access enabled
        webView = findViewById(R.id.webView);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);

        // Register JavaScript interfaces for native track library and update features
        webView.addJavascriptInterface(new AndroidAPI(), "AndroidAPI");

        // Register the OTA update manager so Settings page can check for updates
        updateManager = new UpdateManager(this, webView);
        webView.addJavascriptInterface(updateManager, "AndroidUpdate");

        // Set the custom WebViewClient that intercepts proxy, play, save, and library requests
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                android.net.Uri url = request.getUrl();
                if (url == null) return null;

                String path = url.getPath() != null ? url.getPath() : "";

                // Intercept CORS proxy requests to forward them natively via OkHttp
                if (path.contains("/proxy")) {
                    String targetUrl = url.getQueryParameter("url");
                    if (targetUrl != null) return handleProxyRequest(targetUrl);
                }

                // Intercept play requests to resolve Invidious audio streams and cache locally
                if (path.contains("/play")) {
                    String videoId = url.getQueryParameter("videoId");
                    if (videoId != null) return handlePlayRequest(videoId);
                }

                // Intercept save requests to copy cached audio to permanent storage
                if (path.contains("/save")) {
                    String videoId = url.getQueryParameter("videoId");
                    String artist = url.getQueryParameter("artist");
                    String title = url.getQueryParameter("title");
                    if (artist == null) artist = "Unknown Artist";
                    if (title == null) title = "Unknown Title";
                    if (videoId != null) return handleSaveRequest(videoId, artist, title);
                }

                // Intercept /saved directory listing requests (not individual .m4a/.mp3 files)
                if (path.contains("/saved") && !path.endsWith(".m4a") && !path.endsWith(".mp3")) {
                    return new WebResourceResponse(
                            "application/json",
                            "UTF-8",
                            new ByteArrayInputStream(
                                    new AndroidAPI().getSavedTracks().getBytes(StandardCharsets.UTF_8)
                            )
                    );
                }

                // Delegate to the asset loader for serving local HTML/CSS/JS and cached media
                WebResourceResponse assetResponse = assetLoader.shouldInterceptRequest(url);
                if (assetResponse != null) return assetResponse;

                return super.shouldInterceptRequest(view, request);
            }
        });

        // Start the background ExoPlayer PlaybackService for media session support
        Intent serviceIntent = new Intent(this, PlaybackService.class);
        startService(serviceIntent);

        // Load the shared web frontend from local assets
        webView.loadUrl("https://appassets.androidplatform.net/assets/gui/index.html");
    }

    /**
     * Proxies an external HTTP request natively via OkHttp to bypass CORS restrictions
     * in the WebView. Used by the frontend to query Deezer and other external APIs.
     *
     * @param targetUrl The external URL to fetch and return as a WebResourceResponse.
     * @return WebResourceResponse containing the proxied JSON body.
     */
    private WebResourceResponse handleProxyRequest(String targetUrl) {
        try {
            Request request = new Request.Builder()
                    .url(targetUrl)
                    .addHeader("User-Agent", "Mozilla/5.0 (Android)")
                    .build();
            Response response = client.newCall(request).execute();
            String content = response.body() != null ? response.body().string() : "";

            return new WebResourceResponse(
                    "application/json",
                    "UTF-8",
                    new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception e) {
            Log.e(TAG, "Proxy error: " + e.getMessage());
            return createErrorResponse("Proxy error: " + e.getMessage());
        }
    }

    /**
     * Resolves a YouTube video's audio stream URL by querying multiple Invidious instances,
     * downloads the audio to a local cache directory, and returns a local playback URL.
     *
     * @param videoId The YouTube video ID to resolve and cache.
     * @return WebResourceResponse with status "ready" and the local cached URL, or an error.
     */
    private WebResourceResponse handlePlayRequest(String videoId) {
        Log.d(TAG, "Starting handlePlayRequest for videoId: " + videoId);
        try {
            // List of public Invidious instances for redundant resolution
            String[] instances = {
                    "https://invidious.flokinet.to",
                    "https://iv.melmac.space",
                    "https://invidious.drgns.space",
                    "https://invidious.perennialte.chs.org",
                    "https://yt.artemislena.eu"
            };
            String audioUrl = "";

            // Try each Invidious instance until an audio stream is found
            for (String instance : instances) {
                try {
                    Request request = new Request.Builder()
                            .url(instance + "/api/v1/videos/" + videoId)
                            .build();
                    Response response = client.newCall(request).execute();
                    String body = response.body() != null ? response.body().string() : "{}";
                    JSONObject json = new JSONObject(body);
                    JSONArray streams = json.optJSONArray("adaptiveFormats");

                    if (streams != null) {
                        for (int i = 0; i < streams.length(); i++) {
                            JSONObject stream = streams.getJSONObject(i);
                            if (stream.optString("type").contains("audio/")) {
                                audioUrl = stream.getString("url");
                                break;
                            }
                        }
                    }
                    if (!audioUrl.isEmpty()) break;
                } catch (Exception e) {
                    // Continue to next instance on failure
                }
            }

            JSONObject resultJson = new JSONObject();
            if (!audioUrl.isEmpty()) {
                // Clear the download cache and download the audio stream
                File downloadDir = new File(getCacheDir(), "downloads");
                if (!downloadDir.exists()) downloadDir.mkdirs();

                File[] existingFiles = downloadDir.listFiles();
                if (existingFiles != null) {
                    for (File f : existingFiles) {
                        if (f.isFile()) f.delete();
                    }
                }

                File outputFile = new File(downloadDir, videoId + ".m4a");

                Request req = new Request.Builder().url(audioUrl).build();
                Response response = client.newCall(req).execute();

                if (response.body() != null) {
                    InputStream inputStream = response.body().byteStream();
                    OutputStream outputStream = new FileOutputStream(outputFile);
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    outputStream.close();
                    inputStream.close();
                }

                if (outputFile.exists() && outputFile.length() > 0) {
                    String localUrl = "https://appassets.androidplatform.net/temp/" + videoId + ".m4a";
                    resultJson.put("status", "ready");
                    resultJson.put("url", localUrl);
                } else {
                    resultJson.put("status", "error");
                    resultJson.put("message", "File download failed or empty.");
                }
            } else {
                resultJson.put("status", "error");
                resultJson.put("message", "No audio streams found.");
            }

            return new WebResourceResponse(
                    "application/json",
                    "UTF-8",
                    new ByteArrayInputStream(resultJson.toString().getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception e) {
            Log.e(TAG, "Play request error: " + e.getMessage());
            return createErrorResponse("Play error: " + e.getMessage());
        }
    }

    /**
     * Copies a cached audio file from the download cache to the permanent saved tracks directory,
     * using a sanitized "Artist - Title.m4a" filename format.
     *
     * @param videoId The YouTube video ID identifying the cached source file.
     * @param artist  The artist name for the saved filename.
     * @param title   The track title for the saved filename.
     * @return WebResourceResponse with status "saved" and the local URL, or an error.
     */
    private WebResourceResponse handleSaveRequest(String videoId, String artist, String title) {
        try {
            File downloadDir = new File(getCacheDir(), "downloads");
            File sourceFile = new File(downloadDir, videoId + ".m4a");

            if (!sourceFile.exists()) {
                Log.e(TAG, "Save error: Source file not found in cache");
                return createErrorResponse("Source file not found");
            }

            File savedDir = new File(getFilesDir(), "saved");
            if (!savedDir.exists()) savedDir.mkdirs();

            // Sanitize filename by removing illegal filesystem characters
            String cleanArtist = artist.trim().replaceAll("[/\\\\?%*:|\"<>]", "");
            String cleanTitle = title.trim().replaceAll("[/\\\\?%*:|\"<>]", "");
            String filename = cleanArtist + " - " + cleanTitle + ".m4a";
            File destFile = new File(savedDir, filename);

            // Copy the cached file to permanent storage
            InputStream in = new java.io.FileInputStream(sourceFile);
            OutputStream out = new FileOutputStream(destFile);
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();

            JSONObject resultJson = new JSONObject();
            if (destFile.exists() && destFile.length() > 0) {
                String localUrl = "https://appassets.androidplatform.net/saved/" + Uri.encode(filename);
                resultJson.put("status", "saved");
                resultJson.put("message", "Track saved successfully");
                resultJson.put("url", localUrl);
            } else {
                resultJson.put("status", "error");
                resultJson.put("message", "Failed to save file.");
            }

            return new WebResourceResponse(
                    "application/json",
                    "UTF-8",
                    new ByteArrayInputStream(resultJson.toString().getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception e) {
            Log.e(TAG, "Save request error: " + e.getMessage());
            return createErrorResponse("Save error: " + e.getMessage());
        }
    }

    /**
     * Creates a standard JSON error response payload for failed WebView intercepts.
     *
     * @param message The human-readable error description.
     * @return WebResourceResponse containing a JSON error object.
     */
    private WebResourceResponse createErrorResponse(String message) {
        try {
            JSONObject errorJson = new JSONObject();
            errorJson.put("status", "error");
            errorJson.put("message", message);
            return new WebResourceResponse(
                    "application/json",
                    "UTF-8",
                    new ByteArrayInputStream(errorJson.toString().getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception e) {
            return new WebResourceResponse(
                    "application/json",
                    "UTF-8",
                    new ByteArrayInputStream("{\"status\":\"error\"}".getBytes(StandardCharsets.UTF_8))
            );
        }
    }

    /**
     * Handles the device back button. Navigates backward through the WebView's
     * browser history if available, otherwise delegates to the default Android behavior.
     */
    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * JavaScript interface exposed to the web frontend under window.AndroidAPI.
     * Provides native access to the saved track library stored in filesDir/saved/.
     */
    public class AndroidAPI {

        /**
         * Scans the saved tracks directory and returns a JSON array of track metadata.
         * Each entry contains filename, artist, title, and local playback URL fields.
         * Called by the frontend via window.AndroidAPI.getSavedTracks().
         *
         * @return JSON string array of saved track objects.
         */
        @JavascriptInterface
        public String getSavedTracks() {
            try {
                File savedDir = new File(getFilesDir(), "saved");
                if (!savedDir.exists()) return "[]";

                File[] files = savedDir.listFiles();
                if (files == null) return "[]";

                JSONArray jsonArray = new JSONArray();
                for (File file : files) {
                    if (file.isFile() && (file.getName().endsWith(".mp3") || file.getName().endsWith(".m4a"))) {
                        String nameWithoutExt = file.getName().replaceFirst("[.][^.]+$", "");
                        String[] parts = nameWithoutExt.split(" - ", 2);
                        String artist = parts.length > 0 ? parts[0].trim() : "Unknown Artist";
                        String trackTitle = parts.length > 1 ? parts[1].trim() : nameWithoutExt;

                        JSONObject trackObj = new JSONObject();
                        trackObj.put("filename", file.getName());
                        trackObj.put("artist", artist);
                        trackObj.put("title", trackTitle);
                        trackObj.put("url", "https://appassets.androidplatform.net/saved/" + Uri.encode(file.getName()));
                        jsonArray.put(trackObj);
                    }
                }
                return jsonArray.toString();
            } catch (Exception e) {
                Log.e(TAG, "getSavedTracks error: " + e.getMessage());
                return "[]";
            }
        }

        /**
         * Downloads a track's audio and saves it to the permanent saved directory.
         * Called by the frontend via window.AndroidAPI.downloadTrack().
         *
         * @param videoId The YouTube video ID of the track to save.
         * @param title   The track title for the saved filename.
         * @param artist  The artist name for the saved filename.
         * @param cover   The cover art URL (stored for metadata, not used for file saving).
         */
        @JavascriptInterface
        public void downloadTrack(String videoId, String title, String artist, String cover) {
            Log.d(TAG, "downloadTrack called: " + artist + " - " + title);
            // The save operation is handled via the /save intercept path.
            // This method exists for frontend compatibility with window.AndroidAPI.downloadTrack().
        }
    }
}
