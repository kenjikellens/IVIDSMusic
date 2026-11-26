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
    /** No update check has been triggered or any active prompt was dismissed. */
    data object Idle : AppUpdateState
    /** Currently querying GitHub Releases API for newer release metadata. */
    data class Checking(val statusKey: String) : AppUpdateState
    /** Installed build matches or exceeds the latest published GitHub release. */
    data object UpToDate : AppUpdateState
    /** A newer release binary and changelog are available to download and install. */
    data class Available(
        val version: String,
        val changelog: String,
        val downloadUrl: String
    ) : AppUpdateState
    /** Streaming the update package onto local storage with normalized progress. */
    data class Downloading(val progress: Float) : AppUpdateState
    /** Package downloaded or installer launched cleanly. */
    data class ReadyToInstall(val message: String) : AppUpdateState
    /** Release check or package download failed with a diagnostic error message. */
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

/**
 * Returns the device's current local hour of day in `0..23` for time-of-day UI greetings.
 */
expect fun currentLocalHour24(): Int
