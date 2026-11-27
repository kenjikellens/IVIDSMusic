package com.kenjigames.ividsmusic.platform

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.awt.Desktop
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import javax.swing.JFileChooser
import javax.swing.SwingUtilities

/**
 * Windows Desktop implementation of [PlatformBridge] providing Swing directory selection and
 * system browser launching.
 */
actual class PlatformBridge {

    actual val isTvDevice: Boolean = false
    actual val appVersion: String = "0.2.5"

    /**
     * Opens a Swing folder chooser dialog on the Event Dispatch Thread and invokes [onPicked] with
     * the chosen directory path or null if cancelled.
     */
    actual fun pickDirectory(currentPath: String?, onPicked: (String?) -> Unit) {
        SwingUtilities.invokeLater {
            val selected = runCatching {
                val chooser = JFileChooser(currentPath?.let(::File)).apply {
                    fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                    isAcceptAllFileFilterUsed = false
                    dialogTitle = "Select Download Folder"
                }
                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    chooser.selectedFile?.absolutePath
                } else {
                    null
                }
            }.getOrNull()
            onPicked(selected)
        }
    }

    /**
     * Opens [url] in the host desktop browser after validating an `http://` or `https://` scheme.
     */
    actual fun openExternalUrl(url: String) {
        if (!isValidExternalHttpUrl(url)) return
        runCatching {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(url.trim()))
            }
        }
    }
}

/**
 * Desktop [AppUpdateChecker] querying GitHub Releases for Windows binaries or opening the
 * release download page.
 */
actual class AppUpdateChecker(
    private val platformBridge: PlatformBridge,
    private val json: Json = DefaultPlatformJson
) {
    private val _state = MutableStateFlow<AppUpdateState>(AppUpdateState.Idle)
    actual val state: StateFlow<AppUpdateState> = _state.asStateFlow()

    /**
     * Queries GitHub Releases API to detect newer desktop builds and surfaces release notes.
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
                    setRequestProperty("User-Agent", "IVIDS-Music-Desktop")
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
                val htmlUrl = root["html_url"]?.jsonPrimitive?.content
                    ?: "https://github.com/kenjikellens/IVIDSMusic/releases/latest"

                if (!isNewerVersion(platformBridge.appVersion, tag)) {
                    _state.value = AppUpdateState.UpToDate
                    return@withContext
                }

                val assets = root["assets"]?.jsonArray.orEmpty()
                val winAssetUrl = assets.firstNotNullOfOrNull { element ->
                    val obj = element.jsonObject
                    val name = obj["name"]?.jsonPrimitive?.content?.lowercase().orEmpty()
                    if (name.endsWith(".exe") || name.endsWith(".zip")) {
                        obj["browser_download_url"]?.jsonPrimitive?.content
                    } else {
                        null
                    }
                } ?: htmlUrl

                _state.value = AppUpdateState.Available(tag, changelog, winAssetUrl)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (e: Exception) {
                _state.value = AppUpdateState.Error(e.message ?: "Update check failed")
            } finally {
                connection?.disconnect()
            }
        }
    }

    /**
     * Opens the Windows installer download URL or release page in the default system browser.
     */
    actual suspend fun downloadAndInstallUpdate() {
        val current = _state.value as? AppUpdateState.Available ?: return
        platformBridge.openExternalUrl(current.downloadUrl)
        _state.value = AppUpdateState.ReadyToInstall("Opened download in browser")
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
 * Returns the desktop host's current local hour of day (`0..23`).
 */
actual fun currentLocalHour24(): Int = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
