package com.kenjigames.ividsmusic.player.desktop

import javafx.application.Platform

/**
 * Coordinates one-time idempotent JavaFX toolkit initialization for standalone Windows playback.
 * Protects against duplicate `Platform.startup` calls and captures headless runtime failures safely.
 */
internal object JavaFxRuntime {

    private val lock = Any()

    @Volatile
    private var initialized: Boolean = false

    @Volatile
    private var initializationFailure: Throwable? = null

    /**
     * Ensures the JavaFX toolkit is started once per JVM with implicit exit disabled.
     * Returns [Result.success] when ready or [Result.failure] if headless or native JavaFX libraries fail.
     */
    fun ensureStarted(): Result<Unit> {
        if (initialized) return Result.success(Unit)
        initializationFailure?.let { return Result.failure(it) }

        return synchronized(lock) {
            if (initialized) return@synchronized Result.success(Unit)
            initializationFailure?.let { return@synchronized Result.failure(it) }

            try {
                Platform.startup {
                    Platform.setImplicitExit(false)
                }
                initialized = true
                Result.success(Unit)
            } catch (_: IllegalStateException) {
                verifyToolkitAlive()
            } catch (throwable: Throwable) {
                initializationFailure = throwable
                Result.failure(throwable)
            }
        }
    }

    /**
     * Executes [action] directly on the JavaFX Application Thread or dispatches it via [Platform.runLater].
     * Ignores [IllegalStateException] only if the JavaFX toolkit has already terminated on shutdown.
     */
    fun runOnFxThread(action: () -> Unit) {
        if (Platform.isFxApplicationThread()) {
            action()
            return
        }
        try {
            Platform.runLater(action)
        } catch (_: IllegalStateException) {
            // Toolkit has already exited during JVM teardown; caller disposal continues.
        }
    }

    /**
     * Verifies whether an already-initialized JavaFX toolkit can accept `Platform.runLater` work.
     * Records an error if the toolkit previously exited instead of being active.
     */
    private fun verifyToolkitAlive(): Result<Unit> {
        return try {
            Platform.runLater {
                Platform.setImplicitExit(false)
            }
            initialized = true
            Result.success(Unit)
        } catch (stateError: IllegalStateException) {
            initializationFailure = stateError
            Result.failure(stateError)
        }
    }
}
