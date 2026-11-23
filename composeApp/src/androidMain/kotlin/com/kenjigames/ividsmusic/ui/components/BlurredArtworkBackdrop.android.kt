package com.kenjigames.ividsmusic.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI

private const val ANDROID_BLUR_CACHE_SIZE = 32
private const val ANDROID_BLUR_TARGET_PX = 56
private const val ANDROID_BLUR_MAX_BYTES = 2 * 1024 * 1024

private val androidBlurSemaphore = Semaphore(4)
private val androidBlurLock = Any()
private val androidBlurCache = object : LinkedHashMap<String, ImageBitmap>(ANDROID_BLUR_CACHE_SIZE, 0.75f, true) {
    /** Evicts the least-recently-used pre-blurred artwork bitmap once the cache exceeds 32 entries. */
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>?): Boolean =
        size > ANDROID_BLUR_CACHE_SIZE
}

/**
 * Downloads [imageUrl] on IO dispatcher, downsamples to 56x56, applies a fast 2-pass box blur
 * compatible with Android API 24+, and caches the resulting [ImageBitmap] for smooth 60+ FPS rendering.
 */
actual suspend fun loadBlurredArtworkBitmap(imageUrl: String?): ImageBitmap? {
    if (imageUrl.isNullOrBlank()) return null
    synchronized(androidBlurLock) {
        androidBlurCache[imageUrl]?.let { return it }
    }
    return withContext(Dispatchers.IO) {
        androidBlurSemaphore.withPermit {
            synchronized(androidBlurLock) {
                androidBlurCache[imageUrl]?.let { return@withPermit it }
            }
            var decoded: Bitmap? = null
            var scaled: Bitmap? = null
            var connection: HttpURLConnection? = null
            try {
                connection = (URI.create(imageUrl).toURL().openConnection() as HttpURLConnection).apply {
                    connectTimeout = 4_000
                    readTimeout = 4_000
                    setRequestProperty("User-Agent", "IVIDS-Music-Android")
                }
                if (connection.responseCode !in 200..299 || connection.contentLengthLong > ANDROID_BLUR_MAX_BYTES) {
                    return@withPermit null
                }
                val bytes = connection.inputStream.use { input ->
                    val out = ByteArrayOutputStream()
                    val buf = ByteArray(8192)
                    var total = 0
                    while (true) {
                        val read = input.read(buf)
                        if (read <= 0) break
                        total += read
                        if (total > ANDROID_BLUR_MAX_BYTES) return@withPermit null
                        out.write(buf, 0, read)
                    }
                    out.toByteArray()
                }
                if (bytes.isEmpty()) return@withPermit null

                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                val maxDim = maxOf(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
                var sample = 1
                while (maxDim / sample > 128) sample *= 2

                decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply {
                    inSampleSize = sample
                }) ?: return@withPermit null

                scaled = Bitmap.createScaledBitmap(decoded, ANDROID_BLUR_TARGET_PX, ANDROID_BLUR_TARGET_PX, true)
                val blurred = boxBlurBitmap(scaled, radius = 5)
                val imageBitmap = blurred.asImageBitmap()

                synchronized(androidBlurLock) {
                    androidBlurCache[imageUrl] = imageBitmap
                }
                imageBitmap
            } catch (_: Exception) {
                null
            } finally {
                if (scaled != null && scaled !== decoded) scaled.recycle()
                decoded?.recycle()
                connection?.disconnect()
            }
        }
    }
}

/**
 * Applies a fast separable 2-pass box blur of [radius] pixels to a small [source] bitmap so
 * Android API 24-36 devices render creamy artwork backdrops without per-frame RenderEffect overhead.
 */
private fun boxBlurBitmap(source: Bitmap, radius: Int): Bitmap {
    val w = source.width
    val h = source.height
    val pixels = IntArray(w * h)
    val temp = IntArray(w * h)
    source.getPixels(pixels, 0, w, 0, 0, w, h)

    repeat(2) {
        for (y in 0 until h) {
            val rowOffset = y * w
            for (x in 0 until w) {
                var r = 0
                var g = 0
                var b = 0
                var count = 0
                for (dx in -radius..radius) {
                    val nx = (x + dx).coerceIn(0, w - 1)
                    val c = pixels[rowOffset + nx]
                    r += (c shr 16) and 0xFF
                    g += (c shr 8) and 0xFF
                    b += c and 0xFF
                    count++
                }
                temp[rowOffset + x] = (0xFF shl 24) or ((r / count) shl 16) or ((g / count) shl 8) or (b / count)
            }
        }
        for (x in 0 until w) {
            for (y in 0 until h) {
                var r = 0
                var g = 0
                var b = 0
                var count = 0
                for (dy in -radius..radius) {
                    val ny = (y + dy).coerceIn(0, h - 1)
                    val c = temp[ny * w + x]
                    r += (c shr 16) and 0xFF
                    g += (c shr 8) and 0xFF
                    b += c and 0xFF
                    count++
                }
                pixels[y * w + x] = (0xFF shl 24) or ((r / count) shl 16) or ((g / count) shl 8) or (b / count)
            }
        }
    }

    return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, w, 0, 0, w, h)
    }
}
