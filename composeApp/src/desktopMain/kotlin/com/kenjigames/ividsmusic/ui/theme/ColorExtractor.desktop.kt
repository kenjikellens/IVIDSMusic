package com.kenjigames.ividsmusic.ui.theme

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Rect
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max
import kotlin.math.min

/**
 * Desktop JVM implementation of [ColorExtractor] using Skiko/Skia pixel downsampling and an LRU cache.
 */
actual object ColorExtractor {

    private const val MAX_CACHE_ENTRIES = 40
    private const val MAX_BYTES = 2 * 1024 * 1024
    private const val SAMPLE_SIZE = 24

    private val cacheLock = Any()
    private val colorCache = object : LinkedHashMap<String, Color>(MAX_CACHE_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Color>?): Boolean =
            size > MAX_CACHE_ENTRIES
    }

    /**
     * Fetches [imageUrl] bytes on IO dispatcher, decodes with Skia Image, downsamples onto a 24x24
     * Bitmap to pick the most vibrant dominant color, and releases all native Skia handles cleanly.
     */
    actual suspend fun extractDominantColor(imageUrl: String?, fallback: Color): Color {
        if (imageUrl.isNullOrBlank()) return fallback
        synchronized(cacheLock) {
            colorCache[imageUrl]?.let { return it }
        }
        return withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                connection = (URL(imageUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 4_000
                    readTimeout = 4_000
                    setRequestProperty("User-Agent", "IVIDS-Music-Desktop")
                }
                if (connection.responseCode !in 200..299) return@withContext fallback
                val bytes = connection.inputStream.use { input ->
                    val out = ByteArrayOutputStream()
                    val buf = ByteArray(8192)
                    var total = 0
                    while (true) {
                        val read = input.read(buf)
                        if (read <= 0) break
                        total += read
                        if (total > MAX_BYTES) break
                        out.write(buf, 0, read)
                    }
                    out.toByteArray()
                }
                if (bytes.isEmpty()) return@withContext fallback

                val extracted = Image.makeFromEncoded(bytes).use { image ->
                    Bitmap().use { bitmap ->
                        bitmap.allocPixels(ImageInfo.makeN32Premul(SAMPLE_SIZE, SAMPLE_SIZE))
                        Canvas(bitmap).use { canvas ->
                            canvas.drawImageRect(
                                image = image,
                                src = Rect.makeWH(image.width.toFloat(), image.height.toFloat()),
                                dst = Rect.makeWH(SAMPLE_SIZE.toFloat(), SAMPLE_SIZE.toFloat())
                            )
                        }
                        sampleDominantArgb(bitmap)
                    }
                } ?: return@withContext fallback

                val clamped = clampAccentContrast(extracted, fallback)
                synchronized(cacheLock) {
                    colorCache[imageUrl] = clamped
                }
                clamped
            } catch (_: Exception) {
                fallback
            } finally {
                connection?.disconnect()
            }
        }
    }

    /**
     * Scans the downsampled Skia [bitmap] and selects the color bucket with highest vibrancy score
     * while ignoring near-black or near-white background pixels.
     */
    private fun sampleDominantArgb(bitmap: Bitmap): Int? {
        val buckets = HashMap<Int, Pair<Double, Int>>()
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val argb = bitmap.getColor(x, y)
                val a = (argb ushr 24) and 0xFF
                if (a < 128) continue
                val r = (argb ushr 16) and 0xFF
                val g = (argb ushr 8) and 0xFF
                val b = argb and 0xFF
                val maxC = max(r, max(g, b))
                val minC = min(r, min(g, b))
                if (maxC < 32 || minC > 230) continue
                val sat = (maxC - minC).toDouble() / maxC.toDouble()
                val key = ((r shr 5) shl 6) or ((g shr 5) shl 3) or (b shr 5)
                val weight = 1.0 + sat * 3.0
                val prev = buckets[key]
                if (prev == null) {
                    buckets[key] = weight to argb
                } else {
                    buckets[key] = (prev.first + weight) to prev.second
                }
            }
        }
        return buckets.values.maxByOrNull { it.first }?.second
    }
}
