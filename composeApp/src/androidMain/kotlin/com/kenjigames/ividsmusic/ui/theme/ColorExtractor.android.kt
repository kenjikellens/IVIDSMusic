package com.kenjigames.ividsmusic.ui.theme

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import com.kenjigames.ividsmusic.ui.components.fetchBoundedAndroidImageBytes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/**
 * Android implementation of [ColorExtractor] backed by AndroidX Palette and a bounded 120-entry LRU cache.
 */
actual object ColorExtractor {

    private const val MAX_CACHE_ENTRIES = 120
    private const val MAX_BYTES = 2 * 1024 * 1024

    private val fetchSemaphore = Semaphore(4)
    private val cacheLock = Any()
    private val colorCache = object : LinkedHashMap<String, Color>(MAX_CACHE_ENTRIES, 0.75f, true) {
        /** Evicts the least-recently-used artwork swatch once [colorCache] exceeds 120 entries. */
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Color>?): Boolean =
            size > MAX_CACHE_ENTRIES
    }

    /**
     * Downloads and downsamples artwork at [imageUrl] on IO dispatcher, extracts a vibrant
     * Palette swatch, recycles the temporary Bitmap deterministically, and caches the result.
     */
    actual suspend fun extractDominantColor(imageUrl: String?, fallback: Color): Color {
        val tint = extractCardTintColor(imageUrl)
        return tint ?: fallback
    }

    /**
     * Samples the true dominant color of [imageUrl] for card and backdrop tinting, returning `null`
     * for blank URLs, network failures, or monochrome covers so neutral surfaces stay uncolored.
     */
    actual suspend fun extractCardTintColor(imageUrl: String?): Color? {
        if (imageUrl.isNullOrBlank()) return null
        synchronized(cacheLock) {
            if (colorCache.containsKey(imageUrl)) {
                val cached = colorCache[imageUrl]
                return cached?.takeIf { it != Color.Unspecified }
            }
        }
        return withContext(Dispatchers.IO) {
            fetchSemaphore.withPermit {
                synchronized(cacheLock) {
                    if (colorCache.containsKey(imageUrl)) {
                        return@withPermit colorCache[imageUrl]?.takeIf { it != Color.Unspecified }
                    }
                }
                var bitmap: Bitmap? = null
                try {
                    val bytes = fetchBoundedAndroidImageBytes(imageUrl, MAX_BYTES)
                        ?: run {
                            synchronized(cacheLock) { colorCache[imageUrl] = Color.Unspecified }
                            return@withPermit null
                        }

                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                    val maxDim = maxOf(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
                    var sample = 1
                    while (maxDim / sample > 96) sample *= 2

                    val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
                    bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
                        ?: run {
                            synchronized(cacheLock) { colorCache[imageUrl] = Color.Unspecified }
                            return@withPermit null
                        }

                    val palette = Palette.from(bitmap).generate()
                    val rawRgb = palette.vibrantSwatch?.rgb
                        ?: palette.lightVibrantSwatch?.rgb
                        ?: palette.dominantSwatch?.rgb
                        ?: run {
                            synchronized(cacheLock) { colorCache[imageUrl] = Color.Unspecified }
                            return@withPermit null
                        }

                    val clamped = clampAccentContrast(rawRgb, Color.Unspecified)
                    synchronized(cacheLock) {
                        colorCache[imageUrl] = clamped
                    }
                    clamped.takeIf { it != Color.Unspecified }
                } catch (cancel: CancellationException) {
                    throw cancel
                } catch (_: Exception) {
                    synchronized(cacheLock) { colorCache[imageUrl] = Color.Unspecified }
                    null
                } finally {
                    bitmap?.recycle()
                }
            }
        }
    }
}
