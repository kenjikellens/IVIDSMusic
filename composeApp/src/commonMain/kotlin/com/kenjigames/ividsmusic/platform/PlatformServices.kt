package com.kenjigames.ividsmusic.platform

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.StateFlow

/**
 * Platform bridge exposing device form factor, version info, directory chooser, and external
 * browser launching cleanly to shared Compose ViewModels.
 */
expect class PlatformBridge {
    /** True when running on Android TV Leanback hardware or TV flavor. */
    val isTvDevice: Boolean

    /** Current application semantic version string displayed in Settings and update checks. */
    val appVersion: String

    /** Opens a platform-native folder picker (Desktop) or returns null when unavailable. */
    fun pickDirectory(currentPath: String?, onPicked: (String?) -> Unit)

    /** Opens [url] in the OS default web browser without crashing if no handler exists. */
    fun openExternalUrl(url: String)
}

/**
 * Represents the native self-update lifecycle across GitHub Releases lookup and installation.
 */
@Immutable
sealed interface AppUpdateState {
    data object Idle : AppUpdateState
    data class Checking(val statusKey: String) : AppUpdateState
    data object UpToDate : AppUpdateState
    data class Available(
        val version: String,
        val changelog: String,
        val downloadUrl: String
    ) : AppUpdateState
    data class Downloading(val progress: Float) : AppUpdateState
    data class ReadyToInstall(val message: String) : AppUpdateState
    data class Error(val message: String) : AppUpdateState
}

/**
 * Platform self-updater checking GitHub Releases for newer IVIDS Music binaries and triggering
 * native Android APK installation or Desktop release download/launch.
 */
expect class AppUpdateChecker {
    /** Observable self-update progress and availability state displayed in SettingsScreen. */
    val state: StateFlow<AppUpdateState>

    /** Queries GitHub Releases API to determine if a newer binary exists for this device flavor. */
    suspend fun checkForUpdates()

    /** Downloads and launches the pending update package or browser installer for the active release. */
    suspend fun downloadAndInstallUpdate()

    /** Resets update state back to [AppUpdateState.Idle] when dismissed by the user. */
    fun dismiss()
}
