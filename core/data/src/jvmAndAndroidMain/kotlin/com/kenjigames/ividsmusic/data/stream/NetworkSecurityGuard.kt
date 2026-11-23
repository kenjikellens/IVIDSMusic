package com.kenjigames.ividsmusic.data.stream

import java.net.InetAddress
import java.net.URI

/**
 * Validates outbound HTTP(S) URLs, redirect targets, and header values against SSRF and CRLF injection.
 * Blocks redirects from public hosts into loopback, link-local, or private network address ranges.
 */
internal object NetworkSecurityGuard {

    private val CONTROL_OR_CRLF_REGEX = Regex("[\\r\\n\\u0000]")

    /**
     * Parses [rawUrl], verifies an `http`/`https` scheme without userinfo or control chars, and optionally blocks private IPs.
     * Returns the validated [URI] or `null` when the target is malformed or violates network boundaries.
     */
    fun validateHttpUri(rawUrl: String, allowPrivateHosts: Boolean = false): URI? {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty() || CONTROL_OR_CRLF_REGEX.containsMatchIn(trimmed)) return null
        val uri = runCatching { URI(trimmed) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase() ?: return null
        if (scheme != "http" && scheme != "https") return null
        if (!uri.rawUserInfo.isNullOrEmpty()) return null
        val host = uri.host?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: return null

        if (!allowPrivateHosts && isPrivateOrLoopbackHost(host)) {
            return null
        }
        return uri
    }

    /**
     * Validates a redirect hop from [currentUrl] to [location], blocking downgrades into private/loopback networks.
     * Preserves loopback redirects only when the initial [currentUrl] itself was already a loopback address.
     */
    fun validateRedirectTarget(currentUrl: String, location: String): URI? {
        val cleanLocation = location.trim()
        if (cleanLocation.isEmpty() || CONTROL_OR_CRLF_REGEX.containsMatchIn(cleanLocation)) return null
        val currentUri = runCatching { URI(currentUrl) }.getOrNull() ?: return null
        val resolved = runCatching { currentUri.resolve(cleanLocation) }.getOrNull() ?: return null
        val currentHostIsPrivate = currentUri.host?.let { isPrivateOrLoopbackHost(it) } == true
        return validateHttpUri(resolved.toString(), allowPrivateHosts = currentHostIsPrivate)
    }

    /**
     * Strips CR, LF, and NUL bytes from HTTP header names or values to prevent HTTP response/request splitting.
     * Returns the sanitized single-line header string.
     */
    fun sanitizeHeaderValue(raw: String): String =
        raw.replace(CONTROL_OR_CRLF_REGEX, "")

    /**
     * Returns `true` when [host] is `localhost` or resolves to loopback, link-local, site-local, or any-local IP.
     * Fails closed (`true`) on unresolvable hostnames only when DNS lookup throws a security error.
     */
    internal fun isPrivateOrLoopbackHost(host: String): Boolean {
        val normalized = host.trim().lowercase().trim('[', ']')
        if (normalized == "localhost" || normalized.endsWith(".localhost")) return true
        return try {
            val addresses = InetAddress.getAllByName(normalized)
            addresses.any { addr ->
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

