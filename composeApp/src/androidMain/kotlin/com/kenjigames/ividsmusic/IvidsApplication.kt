package com.kenjigames.ividsmusic

import android.app.Application
import com.kenjigames.ividsmusic.data.local.AppStorageDirectories
import com.kenjigames.ividsmusic.di.initKoin
import org.koin.android.ext.koin.androidContext
import java.io.File

/**
 * Android Application class initializing Koin dependency injection across `:core:data`,
 * `:core:player`, and `:composeApp` with internal cache, database, and `saved/` paths.
 */
class IvidsApplication : Application() {

    /**
     * Initializes local storage directories and starts the global Koin dependency container.
     */
    override fun onCreate() {
        super.onCreate()
        val savedDir = File(filesDir, "saved").apply { mkdirs() }
        val directories = AppStorageDirectories(
            cacheDir = cacheDir.absolutePath,
            filesDir = filesDir.absolutePath,
            savedDir = savedDir.absolutePath,
            bundledToolsDir = null
        )
        initKoin(directories) {
            androidContext(this@IvidsApplication)
        }
    }
}
