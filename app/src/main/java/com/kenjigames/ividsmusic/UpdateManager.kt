package com.kenjigames.ividsmusic

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * UpdateManager handles the automatic OTA (Over-The-Air) update checking and installation process.
 * It interacts with the GitHub API to find the latest releases, compares version tags,
 * downloads the new APK securely via HTTP, and initiates the Android package installation intent.
 * It acts as a bridge between the WebView (JavaScript) and native Android code.
 */
class UpdateManager(private val mActivity: Activity, private val mWebView: WebView) {

    private val mExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var mDownloadUrl: String? = null
    private var mLatestVersion: String? = null

    companion object {
        private const val TAG = "UpdateManager"
        private const val GITHUB_API_URL = "https://api.github.com/repos/kenjikellens/IVIDSMusic/releases"
        private const val REPO_APK_URL = "https://github.com/kenjikellens/IVIDSMusic/raw/main/IVIDSMusic.apk"
    }

    /**
     * Checks the GitHub repository releases API to see if a newer version of the app is available.
     * This method is exposed to JavaScript via the @JavascriptInterface annotation and runs on a background thread.
     */
    @JavascriptInterface
    fun checkForUpdates() {
        Log.d(TAG, "Checking for updates...")
        if (!isNetworkAvailable()) {
            Log.e(TAG, "No network connection available.")
            notifyWebUpdateError()
            return
        }

        mExecutor.execute {
            var conn: HttpURLConnection? = null
            var reader: BufferedReader? = null
            try {
                val url = URI.create(GITHUB_API_URL).toURL()
                notifyWebUpdateStatus("connecting-api")
                conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                conn.setRequestProperty("User-Agent", "IVIDS-Music-Android-App")

                val responseCode = conn.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }

                    val releases = JSONArray(response.toString())
                    notifyWebUpdateStatus("fetching-releases")
                    if (releases.length() == 0) {
                        Log.d(TAG, "No releases found")
                        notifyWebNoUpdateFound()
                        return@execute
                    }

                    val latestRelease = releases.getJSONObject(0)
                    mLatestVersion = latestRelease.getString("tag_name")

                    val currentVersion = mActivity.packageManager
                        .getPackageInfo(mActivity.packageName, 0).versionName ?: "0.1.6"

                    Log.d(TAG, "Current version: $currentVersion, Latest version: $mLatestVersion")

                    if (isNewerVersion(currentVersion, mLatestVersion!!)) {
                        Log.d(TAG, "New update found: $mLatestVersion")
                        val assets = latestRelease.getJSONArray("assets")
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            if (asset.getString("name").lowercase().endsWith(".apk")) {
                                mDownloadUrl = asset.getString("browser_download_url")
                                break
                            }
                        }

                        if (mDownloadUrl != null) {
                            Log.d(TAG, "Found APK download URL: $mDownloadUrl")
                            notifyWebFoundUpdate(mLatestVersion!!)
                        } else {
                            Log.e(TAG, "No APK found in the latest release")
                            notifyWebNoUpdateFound()
                        }
                    } else {
                        Log.d(TAG, "App is up to date")
                        notifyWebNoUpdateFound()
                    }
                } else {
                    Log.e(TAG, "GitHub API returned error: $responseCode")
                    notifyWebUpdateError()
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error connecting to GitHub API", e)
                notifyWebUpdateError()
            } catch (e: JSONException) {
                Log.e(TAG, "Error parsing JSON response", e)
                notifyWebUpdateError()
            } catch (e: Exception) {
                Log.e(TAG, "An unexpected error occurred while checking for updates", e)
                notifyWebUpdateError()
            } finally {
                if (reader != null) {
                    try {
                        reader.close()
                    } catch (e: IOException) {
                        Log.e(TAG, "Error closing reader", e)
                    }
                }
                conn?.disconnect()
            }
        }
    }

    /**
     * Compares the current installed app version against the latest version tag from GitHub.
     * Handles semantic versioning comparison (e.g., v1.0.1 vs v1.0.2).
     */
    private fun isNewerVersion(current: String, latest: String): Boolean {
        try {
            notifyWebUpdateStatus("comparing-versions")

            if ("1.0" == current || "1.0.0" == current) {
                return true
            }

            val c = if (current.startsWith("v")) current.substring(1) else current
            val l = if (latest.startsWith("v")) latest.substring(1) else latest

            val cParts = c.split(".")
            val lParts = l.split(".")

            val length = maxOf(cParts.size, lParts.size)
            for (i in 0 until length) {
                val cPart = if (i < cParts.size) cParts[i].replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0 else 0
                val lPart = if (i < lParts.size) lParts[i].replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0 else 0

                if (lPart > cPart) return true
                if (cPart > lPart) return false
            }
        } catch (e: NumberFormatException) {
            Log.e(TAG, "Error comparing versions", e)
            return current != latest
        }
        return false
    }

    /**
     * Retrieves the current app version name from the Android package manager.
     * This method is exposed to JavaScript via the @JavascriptInterface annotation.
     */
    @JavascriptInterface
    fun getCurrentVersion(): String {
        return try {
            mActivity.packageManager.getPackageInfo(mActivity.packageName, 0).versionName ?: "0.1.6"
        } catch (e: Exception) {
            Log.e(TAG, "Error getting package version name", e)
            "0.1.6"
        }
    }

    /**
     * Initiates a direct forced download of the APK from the repository's raw URL.
     * This method is exposed to JavaScript via the @JavascriptInterface annotation.
     */
    @JavascriptInterface
    fun downloadFromRepo() {
        Log.d(TAG, "Requesting direct download from repository...")
        mDownloadUrl = REPO_APK_URL
        downloadAndInstall()
    }

    /**
     * Directs the native downloader to download and install a package from a custom URL.
     * This method is exposed to JavaScript via the @JavascriptInterface annotation.
     */
    @JavascriptInterface
    fun downloadAndInstallForUrl(url: String) {
        Log.d(TAG, "Requesting custom download URL: $url")
        mDownloadUrl = url
        downloadAndInstall()
    }

    /**
     * Begins the background download of the latest APK file.
     * Automatically handles HTTP redirects, writes the stream to the cache, and publishes progress updates.
     */
    @JavascriptInterface
    fun downloadAndInstall() {
        val downloadUrl = mDownloadUrl
        if (downloadUrl == null) {
            Log.e(TAG, "No download URL available")
            notifyWebUpdateError()
            return
        }

        Log.d(TAG, "Starting download: $downloadUrl")
        mExecutor.execute {
            var conn: HttpURLConnection? = null
            var inputStream: InputStream? = null
            var fos: FileOutputStream? = null
            try {
                notifyWebUpdateStatus("downloading")

                var finalUrl = downloadUrl
                var redirectCount = 0
                val maxRedirects = 5

                while (redirectCount < maxRedirects) {
                    conn = URI.create(finalUrl).toURL().openConnection() as HttpURLConnection
                    conn.setRequestProperty("User-Agent", "IVIDS-Music-Android-App")
                    conn.instanceFollowRedirects = false
                    conn.connectTimeout = 15000
                    conn.readTimeout = 30000
                    conn.connect()

                    val responseCode = conn.responseCode
                    Log.d(TAG, "Response code: $responseCode for URL: $finalUrl")

                    if (responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                        responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                        responseCode == 307 || responseCode == 308
                    ) {
                        val location = conn.getHeaderField("Location")
                        if (location == null) {
                            Log.e(TAG, "Redirect with no Location header")
                            notifyWebUpdateError()
                            return@execute
                        }
                        Log.d(TAG, "Redirecting to: $location")
                        finalUrl = location
                        conn.disconnect()
                        redirectCount++
                    } else if (responseCode == HttpURLConnection.HTTP_OK) {
                        break
                    } else {
                        Log.e(TAG, "HTTP error: $responseCode")
                        notifyWebUpdateError()
                        return@execute
                    }
                }

                if (redirectCount >= maxRedirects) {
                    Log.e(TAG, "Too many redirects")
                    notifyWebUpdateError()
                    return@execute
                }

                val downloadDir = File(mActivity.externalCacheDir, "updates")
                if (!downloadDir.exists()) {
                    downloadDir.mkdirs()
                }

                val apkFile = File(downloadDir, "IVIDSMusic-update.apk")
                if (apkFile.exists()) {
                    apkFile.delete()
                }

                inputStream = BufferedInputStream(conn!!.inputStream)
                fos = FileOutputStream(apkFile)

                val data = ByteArray(8192)
                var count: Int
                var total: Long = 0
                val fileLength = conn.contentLength
                while (inputStream.read(data).also { count = it } != -1) {
                    total += count
                    if (fileLength > 0) {
                        val progress = (total * 100 / fileLength).toInt()
                        mActivity.runOnUiThread {
                            mWebView.evaluateJavascript(
                                "if(typeof onUpdateProgress === 'function') onUpdateProgress($progress);",
                                null
                            )
                        }
                    }
                    fos.write(data, 0, count)
                }

                Log.d(TAG, "Download complete: ${apkFile.absolutePath}")
                installApk(apkFile)
            } catch (e: IOException) {
                Log.e(TAG, "Error downloading update", e)
                notifyWebUpdateError()
            } finally {
                try {
                    fos?.close()
                    inputStream?.close()
                } catch (e: IOException) {
                    Log.e(TAG, "Error closing streams", e)
                }
                conn?.disconnect()
            }
        }
    }

    /**
     * Triggers the Android system package installer to install the downloaded APK.
     * Uses FileProvider to securely grant the installer read access to the downloaded file.
     */
    private fun installApk(apkFile: File) {
        try {
            notifyWebUpdateStatus("installing")
            val apkUri = FileProvider.getUriForFile(
                mActivity,
                mActivity.packageName + ".fileprovider", apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            mActivity.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting installation", e)
            notifyWebUpdateError()
        }
    }

    /**
     * Checks if the device currently has an active network connection (Wi-Fi or Cellular).
     * Returns true if a network connection is available, false otherwise.
     */
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = mActivity
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
            ?: return false
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

    /**
     * Shuts down the background executor service to prevent memory leaks.
     * Should be called when the hosting activity is destroyed.
     */
    fun shutdown() {
        mExecutor.shutdown()
    }

    /**
     * Sends a JavaScript callback to the WebView notifying that a new update is available.
     * Evaluates the JavaScript onUpdateFound callback on the UI thread.
     */
    private fun notifyWebFoundUpdate(version: String) {
        mActivity.runOnUiThread {
            mWebView.evaluateJavascript(
                "if(typeof onUpdateFound === 'function') onUpdateFound('$version');",
                null
            )
        }
    }

    /**
     * Sends a JavaScript callback to the WebView notifying that no new updates were found.
     * Evaluates the JavaScript onNoUpdateFound callback on the UI thread.
     */
    private fun notifyWebNoUpdateFound() {
        mActivity.runOnUiThread {
            mWebView.evaluateJavascript(
                "if(typeof onNoUpdateFound === 'function') onNoUpdateFound();",
                null
            )
        }
    }

    /**
     * Sends a JavaScript callback to the WebView to update the UI on the current stage of the update process.
     * Evaluates the JavaScript onUpdateStatus callback on the UI thread.
     */
    private fun notifyWebUpdateStatus(statusKey: String) {
        mActivity.runOnUiThread {
            mWebView.evaluateJavascript(
                "if(typeof onUpdateStatus === 'function') onUpdateStatus('$statusKey');",
                null
            )
        }
    }

    /**
     * Sends a JavaScript callback to the WebView notifying that an error occurred during the update process.
     * Evaluates the JavaScript onUpdateCheckError callback on the UI thread.
     */
    private fun notifyWebUpdateError() {
        mActivity.runOnUiThread {
            mWebView.evaluateJavascript(
                "if(typeof onUpdateCheckError === 'function') onUpdateCheckError();",
                null
            )
        }
    }
}
