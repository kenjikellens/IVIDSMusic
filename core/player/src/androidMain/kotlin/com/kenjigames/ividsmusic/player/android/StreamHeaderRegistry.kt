package com.kenjigames.ividsmusic.player.android

import android.os.Bundle

/**
 * Stores per-stream HTTP request headers across Media3 IPC boundaries and data-source lookups.
 * Maintains a bounded 64-entry access-ordered LRU map guarded for multi-threaded ExoPlayer access.
 */
internal object StreamHeaderRegistry {

    const val EXTRA_MIME_TYPE: String = "ivids_mime_type"
    private const val EXTRA_HTTP_HEADERS_BUNDLE: String = "ivids_http_headers"
    private const val MAX_ENTRIES: Int = 64

    private val lock = Any()
    private val headersByUri: LinkedHashMap<String, Map<String, String>> =
        object : LinkedHashMap<String, Map<String, String>>(16, 0.75f, true) {
            /**
             * Evicts the least-recently-used stream header entry once capacity exceeds [MAX_ENTRIES].
             * Keeps bounded memory footprint in [StreamHeaderRegistry].
             */
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<String, Map<String, String>>?,
            ): Boolean = size > MAX_ENTRIES
        }

    /**
     * Associates custom HTTP [headers] with [uri], or clears any existing entry if [headers] is empty.
     * Mutates the bounded in-memory registry consumed by [PlaybackDataSourceFactory].
     */
    fun putHeaders(uri: String, headers: Map<String, String>) {
        synchronized(lock) {
            if (headers.isEmpty()) {
                headersByUri.remove(uri)
            } else {
                headersByUri[uri] = headers.toMap()
            }
        }
    }

    /**
     * Returns an immutable copy of custom HTTP headers registered for [uri], or an empty map.
     * Thread-safe read against the internal header registry.
     */
    fun getHeaders(uri: String): Map<String, String> {
        synchronized(lock) {
            return headersByUri[uri]?.toMap() ?: emptyMap()
        }
    }

    /**
     * Clears all stored per-URI HTTP header mappings when [AndroidPlaybackService] is destroyed.
     * Prevents stale header entries from persisting across service lifecycles.
     */
    fun clear() {
        synchronized(lock) {
            headersByUri.clear()
        }
    }

    /**
     * Serializes [headers] and optional [mimeType] into an Android [Bundle] for `MediaMetadata.extras`.
     * Enables `MediaController` to pass custom HTTP headers across Media3 session IPC.
     */
    fun encodeExtras(headers: Map<String, String>, mimeType: String?): Bundle {
        val extras = Bundle()
        if (!mimeType.isNullOrBlank()) {
            extras.putString(EXTRA_MIME_TYPE, mimeType)
        }
        if (headers.isNotEmpty()) {
            val headersBundle = Bundle().apply {
                for ((key, value) in headers) {
                    putString(key, value)
                }
            }
            extras.putBundle(EXTRA_HTTP_HEADERS_BUNDLE, headersBundle)
        }
        return extras
    }

    /**
     * Extracts custom HTTP header key-value pairs from a Media3 `MediaMetadata.extras` [Bundle].
     * Returns an empty map when [extras] or its header sub-bundle is absent.
     */
    fun decodeHeadersBundle(extras: Bundle?): Map<String, String> {
        val headersBundle = extras?.getBundle(EXTRA_HTTP_HEADERS_BUNDLE) ?: return emptyMap()
        val result = LinkedHashMap<String, String>(headersBundle.size())
        for (key in headersBundle.keySet()) {
            val value = headersBundle.getString(key)
            if (value != null) {
                result[key] = value
            }
        }
        return result
    }
}
