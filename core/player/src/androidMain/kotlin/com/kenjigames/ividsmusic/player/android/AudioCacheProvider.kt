package com.kenjigames.ividsmusic.player.android

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

/**
 * Owns the process-wide 500 MB Media3 [SimpleCache] instance under `cacheDir/audio_cache`.
 * Prevents concurrent [SimpleCache] directory lock failures across Android service restarts.
 */
@OptIn(UnstableApi::class)
internal object AudioCacheProvider {

    private const val MAX_CACHE_BYTES: Long = 500L * 1024L * 1024L
    private const val CACHE_DIR_NAME: String = "audio_cache"

    @Volatile
    private var cachedInstance: SimpleCache? = null

    /**
     * Returns the singleton [SimpleCache] bound to `cacheDir/audio_cache`, initializing it once on demand.
     * Uses double-checked locking against application [context] without leaking activity contexts.
     */
    fun getCache(context: Context): SimpleCache {
        cachedInstance?.let { return it }
        return synchronized(this) {
            cachedInstance?.let { return@synchronized it }
            val appContext = context.applicationContext
            val cacheDir = File(appContext.cacheDir, CACHE_DIR_NAME).apply { mkdirs() }
            val evictor = LeastRecentlyUsedCacheEvictor(MAX_CACHE_BYTES)
            val databaseProvider = StandaloneDatabaseProvider(appContext)
            SimpleCache(cacheDir, evictor, databaseProvider).also { created ->
                cachedInstance = created
            }
        }
    }
}
