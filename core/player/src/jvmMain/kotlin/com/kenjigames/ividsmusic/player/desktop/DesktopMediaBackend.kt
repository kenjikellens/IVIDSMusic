package com.kenjigames.ividsmusic.player.desktop

import javafx.beans.value.ChangeListener
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.util.Duration
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/**
 * Abstraction over desktop media player creation enabling deterministic JVM tests without audio hardware.
 * Implemented in production by [JavaFxMediaBackend] using OpenJFX 17.0.12.
 */
internal interface DesktopMediaBackend {

    /**
     * Creates a native playback handle for [mediaUri] with optional HTTP [headers] and [initialVolume].
     * Binds lifecycle [callbacks] and begins playback, returning [Result.failure] on initialization errors.
     */
    suspend fun createAndStart(
        mediaUri: String,
        headers: Map<String, String>,
        initialVolume: Float,
        callbacks: MediaHandleCallbacks,
    ): Result<DesktopMediaHandle>
}

/**
 * Lifecycle-managed handle to an active desktop audio player instance.
 * Guarantees deterministic teardown via [stop] followed by [dispose].
 */
internal interface DesktopMediaHandle {

    /**
     * Starts or resumes audio output on the underlying desktop player.
     * Safe no-op once [dispose] has completed.
     */
    fun play()

    /**
     * Pauses audio output on the underlying desktop player without releasing native resources.
     * Safe no-op once [dispose] has completed.
     */
    fun pause()

    /**
     * Seeks playback cursor to [positionMs] milliseconds on the underlying desktop player.
     * Safe no-op once [dispose] has completed.
     */
    fun seekTo(positionMs: Long)

    /**
     * Applies normalized [volume] (`0.0f..1.0f`) to the underlying desktop player.
     * Safe no-op once [dispose] has completed.
     */
    fun setVolume(volume: Float)

    /**
     * Stops active audio output before [dispose] releases native resources.
     * Safe no-op once [dispose] has completed.
     */
    fun stop()

    /**
     * Detaches listeners, disposes native GStreamer/JavaFX media resources, and closes auxiliary sockets.
     * Must be idempotent and safe to invoke in `finally` blocks even if [stop] throws.
     */
    fun dispose()

    /**
     * Returns current playback position in milliseconds, or `0L` when unknown or disposed.
     * Used by the 250ms position polling coroutine in [DesktopAudioEngine].
     */
    fun currentPositionMs(): Long

    /**
     * Returns total track duration in milliseconds, or `0L` when unknown or disposed.
     * Guards against JavaFX `Duration.UNKNOWN` and `Duration.INDEFINITE`.
     */
    fun durationMs(): Long
}

/**
 * Callback bundle forwarded from a [DesktopMediaHandle] into [DesktopAudioEngine].
 * All callbacks are generation-checked inside [DesktopAudioEngine] to reject stale events.
 */
internal data class MediaHandleCallbacks(
    val onReady: (durationMs: Long) -> Unit,
    val onPlaying: (durationMs: Long) -> Unit,
    val onPaused: () -> Unit,
    val onStalled: () -> Unit,
    val onEndOfMedia: () -> Unit,
    val onError: (Throwable) -> Unit,
)

/**
 * Production [DesktopMediaBackend] powered by OpenJFX 17.0.12 `Media` and `MediaPlayer`.
 * Starts [JavaFxRuntime] idempotently and bridges custom HTTP headers via [LocalHeaderProxy] when needed.
 */
internal class JavaFxMediaBackend : DesktopMediaBackend {

    /**
     * Initializes the JavaFX toolkit, optionally opens a [LocalHeaderProxy], and starts a JavaFX `MediaPlayer`.
     * Immediately disposes created native resources if the calling coroutine is cancelled mid-startup.
     */
    override suspend fun createAndStart(
        mediaUri: String,
        headers: Map<String, String>,
        initialVolume: Float,
        callbacks: MediaHandleCallbacks,
    ): Result<DesktopMediaHandle> {
        JavaFxRuntime.ensureStarted().getOrElse { failure ->
            return Result.failure(failure)
        }

        val isRemoteHttp = mediaUri.startsWith("http://", ignoreCase = true) ||
            mediaUri.startsWith("https://", ignoreCase = true)
        val proxy = if (isRemoteHttp || headers.isNotEmpty()) {
            try {
                LocalHeaderProxy(targetUrl = mediaUri, headers = headers)
            } catch (throwable: Throwable) {
                return Result.failure(throwable)
            }
        } else {
            null
        }
        val effectiveUri = proxy?.localUrl ?: mediaUri

        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation {
                runCatching { proxy?.close() }
            }
            JavaFxRuntime.runOnFxThread {
                var media: Media? = null
                var player: MediaPlayer? = null
                try {
                    val createdMedia = Media(effectiveUri)
                    media = createdMedia
                    val createdPlayer = MediaPlayer(createdMedia)
                    player = createdPlayer
                    val handle = JavaFxMediaHandle(
                        media = createdMedia,
                        player = createdPlayer,
                        proxy = proxy,
                        callbacks = callbacks,
                    )
                    if (!continuation.isActive) {
                        handle.dispose()
                        return@runOnFxThread
                    }
                    createdPlayer.volume = initialVolume.toDouble()
                    createdPlayer.isAutoPlay = true
                    createdPlayer.play()
                    continuation.resume(Result.success(handle))
                } catch (throwable: Throwable) {
                    runCatching { player?.dispose() }
                    runCatching { proxy?.close() }
                    media?.onError = null
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(throwable))
                    }
                }
            }
        }
    }

    /**
     * Wraps a single JavaFX `Media` / `MediaPlayer` pair and optional [LocalHeaderProxy] for deterministic disposal.
     * Executes JavaFX control calls on the FX Application Thread and guards against post-disposal invocations.
     */
    private class JavaFxMediaHandle(
        private val media: Media,
        private val player: MediaPlayer,
        private val proxy: LocalHeaderProxy?,
        private val callbacks: MediaHandleCallbacks,
    ) : DesktopMediaHandle {

        private val disposed = AtomicBoolean(false)

        private val statusListener = ChangeListener<MediaPlayer.Status> { _, _, newStatus ->
            if (disposed.get()) return@ChangeListener
            if (newStatus == MediaPlayer.Status.HALTED) {
                val error = player.error ?: IllegalStateException("JavaFX MediaPlayer halted unexpectedly")
                callbacks.onError(error)
            }
        }

        init {
            media.onError = Runnable {
                if (disposed.get()) return@Runnable
                val error = media.error ?: IllegalStateException("JavaFX Media error")
                callbacks.onError(error)
            }
            player.onError = Runnable {
                if (disposed.get()) return@Runnable
                val error = player.error ?: IllegalStateException("JavaFX MediaPlayer error")
                callbacks.onError(error)
            }
            player.onReady = Runnable {
                if (!disposed.get()) callbacks.onReady(durationMs())
            }
            player.onPlaying = Runnable {
                if (!disposed.get()) callbacks.onPlaying(durationMs())
            }
            player.onPaused = Runnable {
                if (!disposed.get()) callbacks.onPaused()
            }
            player.onStalled = Runnable {
                if (!disposed.get()) callbacks.onStalled()
            }
            player.onEndOfMedia = Runnable {
                if (!disposed.get()) callbacks.onEndOfMedia()
            }
            player.statusProperty().addListener(statusListener)
        }

        /**
         * Resumes playback on the JavaFX Application Thread when handle is still active.
         * Safely ignored after [dispose].
         */
        override fun play() {
            if (disposed.get()) return
            JavaFxRuntime.runOnFxThread {
                if (!disposed.get()) player.play()
            }
        }

        /**
         * Pauses playback on the JavaFX Application Thread when handle is still active.
         * Safely ignored after [dispose].
         */
        override fun pause() {
            if (disposed.get()) return
            JavaFxRuntime.runOnFxThread {
                if (!disposed.get()) player.pause()
            }
        }

        /**
         * Seeks the JavaFX player cursor to [positionMs] milliseconds on the FX thread.
         * Safely ignored after [dispose].
         */
        override fun seekTo(positionMs: Long) {
            if (disposed.get()) return
            JavaFxRuntime.runOnFxThread {
                if (!disposed.get()) player.seek(Duration.millis(positionMs.toDouble()))
            }
        }

        /**
         * Sets JavaFX player volume on the FX thread.
         * Safely ignored after [dispose].
         */
        override fun setVolume(volume: Float) {
            if (disposed.get()) return
            JavaFxRuntime.runOnFxThread {
                if (!disposed.get()) player.volume = volume.toDouble()
            }
        }

        /**
         * Stops the underlying JavaFX `MediaPlayer` on the FX thread prior to disposal.
         * Safely ignored after [dispose].
         */
        override fun stop() {
            if (disposed.get()) return
            JavaFxRuntime.runOnFxThread {
                if (!disposed.get()) player.stop()
            }
        }

        /**
         * Idempotently closes [LocalHeaderProxy], detaches all JavaFX listeners, and invokes `stop(); dispose()`.
         * Guarantees native GStreamer handles are never leaked even if `stop()` raises an exception.
         */
        override fun dispose() {
            if (!disposed.compareAndSet(false, true)) return
            try {
                proxy?.close()
            } catch (_: Exception) {
                // Loopback proxy socket already closed.
            }
            JavaFxRuntime.runOnFxThread {
                media.onError = null
                player.onError = null
                player.onReady = null
                player.onPlaying = null
                player.onPaused = null
                player.onStalled = null
                player.onEndOfMedia = null
                try {
                    player.statusProperty().removeListener(statusListener)
                } catch (_: Exception) {
                    // Status listener already detached during player teardown.
                }
                try {
                    try {
                        player.stop()
                    } catch (_: Exception) {
                        // Proceed unconditionally to player.dispose() below.
                    }
                } finally {
                    player.dispose()
                }
            }
        }

        /**
         * Reads current JavaFX player cursor position safely as non-negative milliseconds.
         * Returns `0L` if disposed or duration is unknown/NaN.
         */
        override fun currentPositionMs(): Long {
            if (disposed.get()) return 0L
            return toValidMillis(player.currentTime)
        }

        /**
         * Reads total JavaFX media duration safely as non-negative milliseconds.
         * Returns `0L` if disposed or duration is unknown/indefinite.
         */
        override fun durationMs(): Long {
            if (disposed.get()) return 0L
            return toValidMillis(player.totalDuration)
        }

        /**
         * Converts a JavaFX [Duration] to non-negative milliseconds, guarding against unknown/indefinite/NaN values.
         * Returns `0L` when duration metadata has not yet loaded.
         */
        private fun toValidMillis(duration: Duration?): Long {
            if (duration == null || duration.isUnknown || duration.isIndefinite) return 0L
            val millis = duration.toMillis()
            if (millis.isNaN() || millis < 0.0) return 0L
            return millis.toLong()
        }
    }
}
