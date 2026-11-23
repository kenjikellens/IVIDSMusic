package com.kenjigames.ividsmusic.ui.components

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI

private const val DESKTOP_BLUR_CACHE_SIZE = 40
private const val DESKTOP_BLUR_TARGET_PX = 72
private const val DESKTOP_BLUR_MAX_BYTES = 2 * 1024 * 1024

private val desktopBlurSemaphore = Semaphore(4)
private val desktopBlurLock = Any()
private val desktopBlurCache = object : LinkedHashMap<String, ImageBitmap>(DESKTOP_BLUR_CACHE_SIZE, 0.75f, true) {
    /** Evicts the least-recently-used pre-blurred artwork bitmap once the cache exceeds 40 entries. */
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>?): Boolean =
        size > DESKTOP_BLUR_CACHE_SIZE
}

/**
 * Downloads [imageUrl] on IO dispatcher, downsamples and blurs it once into a 72x72 Skia surface
 * with clamped edges, and caches the resulting [ImageBitmap] for zero-cost 60+ FPS GPU blitting.
 */
actual suspend fun loadBlurredArtworkBitmap(imageUrl: String?): ImageBitmap? {
    if (imageUrl.isNullOrBlank()) return null
    synchronized(desktopBlurLock) {
        desktopBlurCache[imageUrl]?.let { return it }
    }
    return withContext(Dispatchers.IO) {
        desktopBlurSemaphore.withPermit {
            synchronized(desktopBlurLock) {
                desktopBlurCache[imageUrl]?.let { return@withPermit it }
            }
            var connection: HttpURLConnection? = null
            try {
                connection = (URI.create(imageUrl).toURL().openConnection() as HttpURLConnection).apply {
                    connectTimeout = 4_000
                    readTimeout = 4_000
                    setRequestProperty("User-Agent", "IVIDS-Music-Desktop")
                }
                if (connection.responseCode !in 200..299 || connection.contentLengthLong > DESKTOP_BLUR_MAX_BYTES) {
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
                        if (total > DESKTOP_BLUR_MAX_BYTES) return@withPermit null
                        out.write(buf, 0, read)
                    }
                    out.toByteArray()
                }
                if (bytes.isEmpty()) return@withPermit null

                val bitmap = Image.makeFromEncoded(bytes).use { source ->
                    Surface.makeRasterN32Premul(DESKTOP_BLUR_TARGET_PX, DESKTOP_BLUR_TARGET_PX).use { surface ->
                        val blurFilter = ImageFilter.makeBlur(8.5f, 8.5f, FilterTileMode.CLAMP)
                        Paint().use { paint ->
                            paint.imageFilter = blurFilter
                            surface.canvas.drawImageRect(
                                image = source,
                                src = Rect.makeWH(source.width.toFloat(), source.height.toFloat()),
                                dst = Rect.makeWH(DESKTOP_BLUR_TARGET_PX.toFloat(), DESKTOP_BLUR_TARGET_PX.toFloat()),
                                paint = paint
                            )
                        }
                        blurFilter.close()
                        surface.makeImageSnapshot().use { snapshot ->
                            snapshot.toComposeImageBitmap()
                        }
                    }
                }

                synchronized(desktopBlurLock) {
                    desktopBlurCache[imageUrl] = bitmap
                }
                bitmap
            } catch (_: Exception) {
                null
            } finally {
                connection?.disconnect()
            }
        }
    }
}
