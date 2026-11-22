package com.kenjigames.ividsmusic.data.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Platform-resolved filesystem root directories injected into `:core:data` Koin bindings at startup.
 * Keeps Android `Context` paths and Windows desktop storage paths explicit and testable.
 */
data class AppStorageDirectories(
    val cacheDir: String,
    val filesDir: String,
    val savedDir: String,
    val bundledToolsDir: String? = null
)

/**
 * Shared thread-safe holder tracking the currently active offline `saved/` directory path without DI cycles.
 * Mutated by download directory preferences and observed by both [com.kenjigames.ividsmusic.data.stream.CompositeStreamResolver] and [com.kenjigames.ividsmusic.data.download.NativeDownloadManager].
 */
class ActiveSaveDirectoryHolder(initialPath: String) {
    private val _path = MutableStateFlow(initialPath)

    /** Reactive stream of the active saved directory path. */
    val path: StateFlow<String> = _path.asStateFlow()

    /** Returns the active saved directory path synchronously. */
    fun current(): String = _path.value

    /** Updates the active saved directory path to [newPath]. */
    fun update(newPath: String) {
        _path.value = newPath
    }
}
