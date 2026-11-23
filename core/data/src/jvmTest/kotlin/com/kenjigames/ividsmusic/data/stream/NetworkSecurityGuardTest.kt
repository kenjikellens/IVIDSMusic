package com.kenjigames.ividsmusic.data.stream

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Verifies [NetworkSecurityGuard] SSRF protection, CRLF stripping, and [JvmLocalAudioLocator] filename sanitization.
 */
class NetworkSecurityGuardTest {

    /**
     * Ensures public URLs pass while private/loopback hosts, userinfo, and non-HTTP schemes are rejected.
     */
    @Test
    fun validateHttpUriBlocksLoopbackPrivateAndUnsafeSchemes() {
        assertNotNull(NetworkSecurityGuard.validateHttpUri("https://www.youtube.com/watch?v=xy3AcmW0lrQ"))
        assertNull(NetworkSecurityGuard.validateHttpUri("http://127.0.0.1:8080/secret"))
        assertNull(NetworkSecurityGuard.validateHttpUri("http://localhost/admin"))
        assertNull(NetworkSecurityGuard.validateHttpUri("http://169.254.169.254/latest/meta-data"))
        assertNull(NetworkSecurityGuard.validateHttpUri("https://user:pass@example.com/"))
        assertNull(NetworkSecurityGuard.validateHttpUri("file:///C:/Windows/System32/cmd.exe"))
        assertNull(NetworkSecurityGuard.validateHttpUri("https://example.com/\r\nX-Injected: true"))
    }

    /**
     * Verifies public-to-loopback redirects are blocked while loopback-to-loopback test redirects stay allowed.
     */
    @Test
    fun validateRedirectTargetPreventsPublicToLoopbackDowngrade() {
        assertNull(
            NetworkSecurityGuard.validateRedirectTarget(
                currentUrl = "https://example.com/audio",
                location = "http://127.0.0.1:9000/internal",
            )
        )
        assertNotNull(
            NetworkSecurityGuard.validateRedirectTarget(
                currentUrl = "http://127.0.0.1:9000/start",
                location = "http://127.0.0.1:9000/next",
            )
        )
    }

    /**
     * Confirms CR, LF, and NUL characters are stripped from headers and Windows reserved filenames are prefixed.
     */
    @Test
    fun sanitizeHeaderValueAndWindowsReservedFilenames() {
        assertEquals("Mozilla/5.0X-Evil: 1", NetworkSecurityGuard.sanitizeHeaderValue("Mozilla/5.0\r\nX-Evil: 1\u0000"))
        assertEquals("_CON", JvmLocalAudioLocator.sanitizeFilenamePart("CON"))
        assertEquals("_nul.txt", JvmLocalAudioLocator.sanitizeFilenamePart("nul.txt"))
        assertEquals("Artist", JvmLocalAudioLocator.sanitizeFilenamePart("Artist... "))
    }
}
