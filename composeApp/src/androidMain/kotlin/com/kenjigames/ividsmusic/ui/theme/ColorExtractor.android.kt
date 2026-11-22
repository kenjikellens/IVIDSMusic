package com.kenjigames.ividsmusic.ui.theme

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI

/**
 * Android implementation of [ColorExtractor] backed by AndroidX Palette and a bounded LRU cache.
 */
actual object ColorExtractor {

    private const val MAX_CACHE_ENTRIES = 40
    private const val MAX_BYTES = 2 * 1024 * 1024

    private val cacheLock = Any()
    private val colorCache = object : LinkedHashMap<String, Color>(MAX_CACHE_ENTRIES, 0.75f, true) {
        /** Evicts the least-recently-used artwork swatch once [colorCache] exceeds 40 entries. */
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Color>?): Boolean =
            size > MAX_CACHE_ENTRIES
    }

    /**
     * Downloads and downsamples artwork at [imageUrl] on IO dispatcher, extracts a vibrant
     * Palette swatch, recycles the temporary Bitmap deterministically, and caches the result.
     */
    actual suspend fun extractDominantColor(imageUrl: String?, fallback: Color): Color {
        if (imageUrl.isNullOrBlank()) return fallback
        synchronized(cacheLock) {
            colorCache[imageUrl]?.let { return it }
        }
        return withContext(Dispatchers.IO) {
            var bitmap: Bitmap? = null
            var connection: HttpURLConnection? = null
            try {
                connection = (URI.create(imageUrl).toURL().openConnection() as HttpURLConnection).apply {
                    connectTimeout = 4_000
                    readTimeout = 4_000
                    setRequestProperty("User-Agent", "IVIDS-Music-Android")
                }
                if (connection.responseCode !in 200..299) return@withContext fallback
                val bytes = connection.inputStream.use { input ->
                    val buffer = ByteArrayOutputStream()
                    val chunk = ByteArray(8192)
                    var total = 0
                    while (true) {
                        val read = input.read(chunk)
                        if (read <= 0) break
                        total += read
                        if (total > MAX_BYTES) break
                        buffer.write(chunk, 0, read)
                    }
                    buffer.toByteArray()
                }
                if (bytes.isEmpty()) return@withContext fallback

                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                val maxDim = maxOf(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
                var sample = 1
                while (maxDim / sample > 96) sample *= 2

                val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
                    ?: return@withContext fallback

                val palette = Palette.from(bitmap).generate()
                val rawRgb = palette.vibrantSwatch?.rgb
                    ?: palette.lightVibrantSwatch?.rgb
                    ?: palette.dominantSwatch?.rgb
                    ?: return@withContext fallback

                val clamped = clampAccentContrast(rawRgb, fallback)
                synchronized(cacheLock) {
                    colorCache[imageUrl] = clamped
                }
                clamped
            } catch (_: Exception) {
                fallback
            } finally {
                bitmap?.recycle()
                connection?.disconnect()
            }
        }
    }
}
