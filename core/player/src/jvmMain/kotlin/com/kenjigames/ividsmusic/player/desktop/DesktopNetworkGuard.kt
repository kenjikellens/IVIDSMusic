package com.kenjigames.ividsmusic.player.desktop

import java.net.InetAddress
import java.net.URI

/**
 * Validates HTTP(S) redirect hops and strips CRLF/NUL characters from headers in desktop playback.
 * Prevents SSRF into loopback/private networks from public redirects and blocks HTTP header splitting.
 */
internal object DesktopNetworkGuard {

    private val CONTROL_OR_CRLF_REGEX = Regex("[\\r\\n\\u0000]")

    /**
     * Validates a redirect hop from [currentUrl] to [location], blocking public-to-private/loopback downgrades.
     * Allows loopback redirects only when [currentUrl] itself targeted a loopback/private address.
     */
    fun validateRedirectTarget(currentUrl: String, location: String): URI? {
        val cleanLocation = location.trim()
        if (cleanLocation.isEmpty() || CONTROL_OR_CRLF_REGEX.containsMatchIn(cleanLocation)) return null
        val currentUri = runCatching { URI(currentUrl) }.getOrNull() ?: return null
        val resolved = runCatching { currentUri.resolve(cleanLocation) }.getOrNull() ?: return null
        val scheme = resolved.scheme?.lowercase() ?: return null
        if (scheme != "http" && scheme != "https") return null
        if (!resolved.rawUserInfo.isNullOrEmpty()) return null
        val host = resolved.host?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: return null
        val currentIsPrivate = currentUri.host?.let(::isPrivateOrLoopbackHost) == true
        if (!currentIsPrivate && isPrivateOrLoopbackHost(host)) return null
        return resolved
    }

    /**
     * Removes CR, LF, and NUL characters from HTTP header names or values to prevent header injection.
     * Returns the sanitized single-line header string.
     */
    fun sanitizeHeaderValue(raw: String): String = raw.replace(CONTROL_OR_CRLF_REGEX, "")

    /**
     * Checks whether [host] is `localhost` or resolves to a loopback, link-local, site-local, or any-local IP.
     * Fails closed on security exceptions.
     */
    private fun isPrivateOrLoopbackHost(host: String): Boolean {
        val normalized = host.trim().lowercase().trim('[', ']')
        if (normalized == "localhost" || normalized.endsWith(".localhost")) return true
        return try {
            InetAddress.getAllByName(normalized).any { addr ->
                addr.isAnyLocalAddress ||
                    addr.isLoopbackAddress ||
                    addr.isLinkLocalAddress ||
                    addr.isSiteLocalAddress ||
                    addr.isMulticastAddress
            }
        } catch (_: SecurityException) {
            true
        } catch (_: Exception) {
            false
        }
    }
}

