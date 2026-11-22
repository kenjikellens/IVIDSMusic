package com.kenjigames.ividsmusic.player

import com.kenjigames.ividsmusic.domain.model.AudioQuality
import com.kenjigames.ividsmusic.domain.model.StreamSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Verifies stream input validation and numeric range clamping in [StreamSourceValidator].
 * Covers malformed URLs, blank paths, NaN volumes, and negative/oversized seek positions.
 */
class StreamSourceValidatorTest {

    /**
     * Verifies valid local paths and HTTP/HTTPS remote URLs pass validation with null error.
     * Confirms supported inputs are accepted without mutation.
     */
    @Test
    fun validSourcesPassValidation() {
        assertNull(StreamSourceValidator.validate(StreamSource.LocalFile("C:\\Music\\song.m4a", AudioQuality.Q196)))
        assertNull(StreamSourceValidator.validate(StreamSource.RemoteUrl("https://cdn.example.com/audio.m4a")))
        assertNull(StreamSourceValidator.validate(StreamSource.RemoteUrl("HTTP://127.0.0.1:8080/stream")))
    }

    /**
     * Verifies blank paths, blank URLs, whitespace URLs, and non-HTTP schemes yield [DomainError.Validation].
     * Ensures malformed inputs are rejected before reaching native media backends.
     */
    @Test
    fun invalidSourcesReturnValidationErrors() {
        assertNotNull(StreamSourceValidator.validate(StreamSource.LocalFile("   ")))
        assertNotNull(StreamSourceValidator.validate(StreamSource.LocalFile("bad\npath.mp3")))
        assertNotNull(StreamSourceValidator.validate(StreamSource.RemoteUrl("")))
        assertNotNull(StreamSourceValidator.validate(StreamSource.RemoteUrl("ftp://example.com/file.mp3")))
        assertNotNull(StreamSourceValidator.validate(StreamSource.RemoteUrl("https://example.com/with space.mp3")))
    }

    /**
     * Verifies [StreamSourceValidator.clampVolume] and [StreamSourceValidator.clampSeekPosition] enforce bounds.
     * Confirms `NaN` volume maps to `0.0f` and negative seeks map to `0L`.
     */
    @Test
    fun clampingEnforcesSafeRanges() {
        assertEquals(0.0f, StreamSourceValidator.clampVolume(-0.4f))
        assertEquals(1.0f, StreamSourceValidator.clampVolume(1.8f))
        assertEquals(0.0f, StreamSourceValidator.clampVolume(Float.NaN))
        assertEquals(0.65f, StreamSourceValidator.clampVolume(0.65f))

        assertEquals(0L, StreamSourceValidator.clampSeekPosition(-500L, 120_000L))
        assertEquals(120_000L, StreamSourceValidator.clampSeekPosition(150_000L, 120_000L))
        assertEquals(45_000L, StreamSourceValidator.clampSeekPosition(45_000L, 0L))
    }
}
