package com.kenjigames.ividsmusic.platform

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.FileProvider
import com.kenjigames.ividsmusic.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI

/**
 * Android implementation of [PlatformBridge] using PackageManager Leanback detection and Action Intents.
 */
actual class PlatformBridge(private val context: Context) {

    actual val isTvDevice: Boolean
        get() = BuildConfig.IS_TV ||
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)

    actual val appVersion: String
        get() = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
                ?: BuildConfig.VERSION_NAME
        }.getOrDefault(BuildConfig.VERSION_NAME)

    /**
     * Android stores offline files in app-scoped internal storage, so directory picking returns null.
     */
    actual fun pickDirectory(currentPath: String?, onPicked: (String?) -> Unit) {
        onPicked(null)
    }

    /**
     * Opens [url] in an external Android browser activity using FLAG_ACTIVITY_NEW_TASK.
     */
    actual fun openExternalUrl(url: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}

/**
 * Android OTA [AppUpdateChecker] querying GitHub Releases for Mobile or TV APKs and launching
 * the package installer via FileProvider.
 */
actual class AppUpdateChecker(
    private val context: Context,
    private val platformBridge: PlatformBridge,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    private val _state = MutableStateFlow<AppUpdateState>(AppUpdateState.Idle)
    actual val state: StateFlow<AppUpdateState> = _state.asStateFlow()

    /**
     * Fetches the latest GitHub release tag and flavor-matched APK URL, comparing semantic versions
     * against the installed app version.
     */
    actual suspend fun checkForUpdates() {
        _state.value = AppUpdateState.Checking("update_status_checking")
        withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                connection = (URI.create(GITHUB_RELEASE_URL).toURL().openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8_000
                    readTimeout = 8_000
                    setRequestProperty("Accept", "application/vnd.github.v3+json")
                    setRequestProperty("User-Agent", "IVIDS-Music-Android")
                }
                if (connection.responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    _state.value = AppUpdateState.UpToDate
                    return@withContext
                }
                if (connection.responseCode !in 200..299) {
                    _state.value = AppUpdateState.Error("HTTP ${connection.responseCode}")
                    return@withContext
                }
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val root = json.parseToJsonElement(body).jsonObject
                val tag = root["tag_name"]?.jsonPrimitive?.content.orEmpty()
                val changelog = root["body"]?.jsonPrimitive?.content.orEmpty()
                if (!isNewerVersion(platformBridge.appVersion, tag)) {
                    _state.value = AppUpdateState.UpToDate
                    return@withContext
                }

                val assets = root["assets"]?.jsonArray.orEmpty()
                val preferTv = platformBridge.isTvDevice
                var chosenUrl: String? = null
                for (asset in assets) {
                    val obj = asset.jsonObject
                    val name = obj["name"]?.jsonPrimitive?.content?.lowercase().orEmpty()
                    val url = obj["browser_download_url"]?.jsonPrimitive?.content ?: continue
                    if (!name.endsWith(".apk")) continue
                    chosenUrl = url
                    val matchesFlavor = if (preferTv) name.contains("tv") else name.contains("mobile")
                    if (matchesFlavor) break
                }

                _state.value = if (chosenUrl != null) {
                    AppUpdateState.Available(tag, changelog, chosenUrl)
                } else {
                    AppUpdateState.UpToDate
                }
            } catch (e: Exception) {
                _state.value = AppUpdateState.Error(e.message ?: "Update check failed")
            } finally {
                connection?.disconnect()
            }
        }
    }

    /**
     * Streams the selected APK asset into `cacheDir/updates` and launches Android's package
     * installer intent through FileProvider.
     */
    actual suspend fun downloadAndInstallUpdate() {
        val current = _state.value as? AppUpdateState.Available ?: return
        withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                _state.value = AppUpdateState.Downloading(0f)
                val updateDir = File(context.cacheDir, "updates").apply { mkdirs() }
                val apkFile = File(updateDir, "IVIDSMusic-update.apk")
                connection = (URI.create(current.downloadUrl).toURL().openConnection() as HttpURLConnection).apply {
                    connectTimeout = 12_000
                    readTimeout = 20_000
                    instanceFollowRedirects = true
                }
                if (connection.responseCode !in 200..299) {
                    _state.value = AppUpdateState.Error("HTTP ${connection.responseCode}")
                    return@withContext
                }
                val total = connection.contentLengthLong.coerceAtLeast(1L)
                connection.inputStream.use { input ->
                    FileOutputStream(apkFile).use { output ->
                        val buffer = ByteArray(8192)
                        var downloaded = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            _state.value = AppUpdateState.Downloading(
                                (downloaded.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                            )
                        }
                    }
                }
                val contentUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )
                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(contentUri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(installIntent)
                _state.value = AppUpdateState.ReadyToInstall("Installer launched")
            } catch (e: Exception) {
                _state.value = AppUpdateState.Error(e.message ?: "Update download failed")
            } finally {
                connection?.disconnect()
            }
        }
    }

    /**
     * Resets the update UI state back to [AppUpdateState.Idle].
     */
    actual fun dismiss() {
        _state.value = AppUpdateState.Idle
    }

    private companion object {
        const val GITHUB_RELEASE_URL =
            "https://api.github.com/repos/kenjikellens/IVIDSMusic/releases/latest"
    }
}

/**
 * Compares dot-separated semantic version tags such as `0.2.5` and `v0.3.0`.
 */
internal fun isNewerVersion(current: String, latest: String): Boolean {
    val cParts = current.trim().removePrefix("v").split(".")
    val lParts = latest.trim().removePrefix("v").split(".")
    val maxLen = maxOf(cParts.size, lParts.size)
    for (i in 0 until maxLen) {
        val cVal = cParts.getOrNull(i)?.filter(Char::isDigit)?.toIntOrNull() ?: 0
        val lVal = lParts.getOrNull(i)?.filter(Char::isDigit)?.toIntOrNull() ?: 0
        if (lVal > cVal) return true
        if (cVal > lVal) return false
    }
    return false
}
