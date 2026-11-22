package com.kenjigames.ividsmusic.player

import com.kenjigames.ividsmusic.domain.model.DomainError
import com.kenjigames.ividsmusic.domain.model.StreamSource

/**
 * Validates audio stream sources and clamps numeric playback controls before reaching native engines.
 * Pure domain-adjacent helper with zero mutable state or platform service dependencies.
 */
internal object StreamSourceValidator {

    /**
     * Validates that [source] contains a playable local path or well-formed HTTP/HTTPS URL.
     * Returns a [DomainError.Validation] on malformed input, or null when valid without mutating state.
     */
    fun validate(source: StreamSource): DomainError.Validation? {
        return when (source) {
            is StreamSource.LocalFile -> validateLocalFile(source.filePath)
            is StreamSource.RemoteUrl -> validateRemoteUrl(source.url)
        }
    }

    /**
     * Clamps [volume] into the normalized `0.0f..1.0f` range expected by native audio engines.
     * Maps `NaN` values safely to `0.0f` without side effects.
     */
    fun clampVolume(volume: Float): Float {
        if (volume.isNaN()) return 0.0f
        return volume.coerceIn(0.0f, 1.0f)
    }

    /**
     * Clamps [positionMs] to a non-negative seek target bounded by [durationMs] when known.
     * Prevents out-of-range seek calls from reaching ExoPlayer or JavaFX MediaPlayer.
     */
    fun clampSeekPosition(positionMs: Long, durationMs: Long): Long {
        val nonNegative = positionMs.coerceAtLeast(0L)
        if (durationMs <= 0L) return nonNegative
        return nonNegative.coerceAtMost(durationMs)
    }

    /**
     * Checks that a local audio file path is non-blank and free of control characters.
     * Returns a [DomainError.Validation] on failure without inspecting filesystem state.
     */
    private fun validateLocalFile(filePath: String): DomainError.Validation? {
        if (filePath.isBlank()) {
            return DomainError.Validation("Local audio file path cannot be blank")
        }
        if (filePath.any { it.isISOControl() }) {
            return DomainError.Validation("Local audio file path contains invalid control characters")
        }
        return null
    }

    /**
     * Checks that a remote stream URL is non-blank, uses HTTP or HTTPS, and contains no whitespace or control characters.
     * Returns a [DomainError.Validation] when malformed without opening network connections.
     */
    private fun validateRemoteUrl(url: String): DomainError.Validation? {
        if (url.isBlank()) {
            return DomainError.Validation("Remote stream URL cannot be blank")
        }
        if (url.any { it.isWhitespace() || it.isISOControl() }) {
            return DomainError.Validation("Remote stream URL contains invalid whitespace or control characters")
        }
        val lower = url.lowercase()
        val hasSupportedScheme = lower.startsWith("http://") || lower.startsWith("https://")
        if (!hasSupportedScheme) {
            return DomainError.Validation("Remote stream URL must use http:// or https:// scheme")
        }
        return null
    }
}
