package com.kenjigames.ividsmusic.player

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

/**
 * Singleton managing ExoPlayer LRU audio stream disk caching to optimize network usage.
 */
object AudioCacheManager {
    
    private var simpleCache: SimpleCache? = null

    /** Max cache size: 500 MB */
    private const val MAX_CACHE_SIZE: Long = 500 * 1024 * 1024

    /** Gets or creates thread-safe SimpleCache instance */
    @Synchronized
    fun getCache(context: Context): SimpleCache {
        if (simpleCache == null) {
            val cacheDir = File(context.cacheDir, "audio_cache")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val evictor = LeastRecentlyUsedCacheEvictor(MAX_CACHE_SIZE)
            val databaseProvider = StandaloneDatabaseProvider(context)
            simpleCache = SimpleCache(cacheDir, evictor, databaseProvider)
        }
        return simpleCache!!
    }
}
