package com.kenjigames.ividsmusic.network

import android.content.Context
import android.net.ConnectivityManager

/**
 * Utility object for monitoring network connectivity and identifying slow internet speed (< 0.8 Mbps).
 */
object NetworkMonitor {

    /**
     * Checks if the active network connection has a downstream speed lower than 0.8 Mbps (800 Kbps).
     *
     * @param context Application context used to query system ConnectivityManager.
     * @return True if connection speed is strictly below 800 Kbps.
     */
    fun isSlowConnection(context: Context?): Boolean {
        if (context == null) return false
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

        val downstreamKbps = capabilities.linkDownstreamBandwidthKbps
        return downstreamKbps in 1..800
    }

    /**
     * Downscales an image/cover URL to a low-resolution variant if connection speed is below 0.8 Mbps.
     *
     * @param url Original image URL.
     * @param context Application context to evaluate network speed automatically.
     * @return Low-resolution URL if network speed is < 0.8 Mbps, otherwise original URL.
     */
    fun optimizeCoverUrl(url: String, context: Context?): String {
        if (url.isEmpty()) return url
        if (!isSlowConnection(context)) return url

        return optimizeCoverUrlDirect(url)
    }

    /**
     * Directly optimizes cover URL if isSlow is true.
     *
     * @param url Original image URL.
     * @return Low-resolution image URL variant.
     */
    fun optimizeCoverUrlDirect(url: String): String {
        if (url.isEmpty()) return url

        // Deezer URL downscaling
        if (url.contains("deezer.com")) {
            return url.replace("1000x1000", "250x250")
                .replace("500x500", "250x250")
                .replace("/cover_xl", "/cover_small")
                .replace("/cover_big", "/cover_medium")
                .replace("/picture_xl", "/picture_small")
                .replace("/picture_big", "/picture_medium")
        }

        // iTunes URL downscaling
        if (url.contains("mzstatic.com") || url.contains("itunes.apple.com")) {
            return url.replace("600x600bb", "100x100bb")
                .replace("500x500bb", "100x100bb")
        }

        return url
    }
}
