package com.kenjigames.ividsmusic

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.core.content.FileProvider
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
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * UpdateManager handles automatic OTA (Over-The-Air) update downloads and installers specifically
 * for the Android TV app (WebView wrapper) version.
 */
class UpdateManager(private val mActivity: Activity, private val mWebView: WebView) {

    private val mExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var mDownloadUrl: String? = null
    private var mLatestVersion: String? = null

    companion object {
        private const val TAG = "UpdateManager"
        private const val GITHUB_API_URL = "https://api.github.com/repos/kenjikellens/IVIDSMusic/releases/latest"
        private const val REPO_APK_URL = "https://github.com/kenjikellens/IVIDSMusic/raw/main/IVIDSMusic_TV.apk"
    }

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

                    val latestRelease = JSONObject(response.toString())
                    mLatestVersion = latestRelease.getString("tag_name")

                    val currentVersion = getCurrentVersion()

                    Log.d(TAG, "Current version: $currentVersion, Latest version: $mLatestVersion")

                    if (isNewerVersion(currentVersion, mLatestVersion!!)) {
                        Log.d(TAG, "New update found: $mLatestVersion")
                        val assets = latestRelease.getJSONArray("assets")
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            // Look for the flavor-specific APK if named correctly, otherwise fallback to any APK
                            val assetName = asset.getString("name").lowercase()
                            if (assetName.endsWith(".apk")) {
                                mDownloadUrl = asset.getString("browser_download_url")
                                // Prefer APKs that specifically mention 'tv' for this flavor
                                if (assetName.contains("tv")) break
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
                } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    Log.d(TAG, "No releases found on GitHub.")
                    notifyWebNoUpdateFound()
                } else {
                    Log.e(TAG, "GitHub API returned error: $responseCode")
                    notifyWebUpdateError()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking for updates", e)
                notifyWebUpdateError()
            } finally {
                reader?.close()
                conn?.disconnect()
            }
        }
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        try {
            notifyWebUpdateStatus("comparing-versions")
            
            val c = current.trim().removePrefix("v")
            val l = latest.trim().removePrefix("v")
            
            if (c == l) return false

            val cParts = c.split(".")
            val lParts = l.split(".")

            val length = maxOf(cParts.size, lParts.size)
            for (i in 0 until length) {
                val cPart = if (i < cParts.size) cParts[i].filter { it.isDigit() }.toIntOrNull() ?: 0 else 0
                val lPart = if (i < lParts.size) lParts[i].filter { it.isDigit() }.toIntOrNull() ?: 0 else 0

                if (lPart > cPart) return true
                if (cPart > lPart) return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error comparing versions", e)
            return current != latest
        }
        return false
    }

    @JavascriptInterface
    fun getCurrentVersion(): String {
        return try {
            mActivity.packageManager.getPackageInfo(mActivity.packageName, 0).versionName ?: "0.2.0"
        } catch (e: Exception) {
            "0.2.0"
        }
    }

    @JavascriptInterface
    fun downloadFromRepo() {
        mDownloadUrl = REPO_APK_URL
        downloadAndInstall()
    }

    @JavascriptInterface
    fun downloadAndInstallForUrl(url: String) {
        mDownloadUrl = url
        downloadAndInstall()
    }

    @JavascriptInterface
    fun downloadAndInstall() {
        val downloadUrl = mDownloadUrl ?: return notifyWebUpdateError()

        mExecutor.execute {
            var conn: HttpURLConnection? = null
            var inputStream: InputStream? = null
            var fos: FileOutputStream? = null
            try {
                notifyWebUpdateStatus("downloading")

                var finalUrl = downloadUrl
                var redirectCount = 0
                while (redirectCount < 5) {
                    conn = URI.create(finalUrl).toURL().openConnection() as HttpURLConnection
                    conn.setRequestProperty("User-Agent", "IVIDS-Music-Android-App")
                    conn.instanceFollowRedirects = false
                    conn.connect()

                    if (conn.responseCode in 301..308) {
                        finalUrl = conn.getHeaderField("Location") ?: break
                        conn.disconnect()
                        redirectCount++
                    } else break
                }

                val downloadDir = File(mActivity.externalCacheDir, "updates")
                if (!downloadDir.exists()) downloadDir.mkdirs()

                // We keep the internal filename consistent so we can overwrite previous attempts
                val apkFile = File(downloadDir, "IVIDSMusic-update.apk")
                
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
                            mWebView.evaluateJavascript("if(typeof onUpdateProgress === 'function') onUpdateProgress($progress);", null)
                        }
                    }
                    fos.write(data, 0, count)
                }

                installApk(apkFile)
            } catch (e: IOException) {
                notifyWebUpdateError()
            } finally {
                fos?.close()
                inputStream?.close()
                conn?.disconnect()
            }
        }
    }

    private fun installApk(apkFile: File) {
        try {
            notifyWebUpdateStatus("installing")
            val apkUri = FileProvider.getUriForFile(mActivity, "${mActivity.packageName}.fileprovider", apkFile)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            mActivity.startActivity(intent)
        } catch (e: Exception) {
            notifyWebUpdateError()
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = mActivity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetworkInfo?.isConnected ?: false
    }

    fun shutdown() {
        mExecutor.shutdown()
    }

    private fun notifyWebFoundUpdate(version: String) {
        mActivity.runOnUiThread { mWebView.evaluateJavascript("if(typeof onUpdateFound === 'function') onUpdateFound('$version');", null) }
    }

    private fun notifyWebNoUpdateFound() {
        mActivity.runOnUiThread { mWebView.evaluateJavascript("if(typeof onNoUpdateFound === 'function') onNoUpdateFound();", null) }
    }

    private fun notifyWebUpdateStatus(statusKey: String) {
        mActivity.runOnUiThread { mWebView.evaluateJavascript("if(typeof onUpdateStatus === 'function') onUpdateStatus('$statusKey');", null) }
    }

    private fun notifyWebUpdateError() {
        mActivity.runOnUiThread { mWebView.evaluateJavascript("if(typeof onUpdateCheckError === 'function') onUpdateCheckError();", null) }
    }
}
