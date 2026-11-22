package com.kenjigames.ividsmusic.player.android

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import okhttp3.OkHttpClient

/**
 * Builds ExoPlayer [DataSource] instances that route HTTP(S) streams through the 500 MB [SimpleCache]
 * and inject per-URI headers while streaming `file://` and `content://` tracks directly without caching.
 */
@OptIn(UnstableApi::class)
internal class PlaybackDataSourceFactory(
    context: Context,
    cache: SimpleCache,
    okHttpClient: OkHttpClient,
    private val headerRegistry: StreamHeaderRegistry = StreamHeaderRegistry,
) : DataSource.Factory {

    private val appContext = context.applicationContext

    private val resolvingHttpFactory: DataSource.Factory = ResolvingDataSource.Factory(
        OkHttpDataSource.Factory(okHttpClient),
    ) { dataSpec ->
        val customHeaders = headerRegistry.getHeaders(dataSpec.uri.toString())
        if (customHeaders.isEmpty()) dataSpec else dataSpec.withAdditionalHeaders(customHeaders)
    }

    private val cacheDataSourceFactory: CacheDataSource.Factory = CacheDataSource.Factory()
        .setCache(cache)
        .setUpstreamDataSourceFactory(resolvingHttpFactory)
        .setCacheWriteDataSinkFactory(CacheDataSink.Factory().setCache(cache))
        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

    private val localDataSourceFactory: DataSource.Factory = DefaultDataSource.Factory(
        appContext,
        resolvingHttpFactory,
    )

    /**
     * Creates a scheme-aware [DataSource] that caches remote HTTP(S) audio and bypasses disk cache for local URIs.
     * Prevents redundant copies of offline downloaded files inside the 500 MB LRU audio cache.
     */
    override fun createDataSource(): DataSource {
        return SchemeRoutingDataSource(
            cacheDataSourceFactory = cacheDataSourceFactory,
            localDataSourceFactory = localDataSourceFactory,
        )
    }

    /**
     * Delegates `open`, `read`, and `close` calls to either [cacheDataSourceFactory] or [localDataSourceFactory].
     * Attaches registered [TransferListener]s to whichever delegate opens the current [DataSpec].
     */
    private class SchemeRoutingDataSource(
        private val cacheDataSourceFactory: CacheDataSource.Factory,
        private val localDataSourceFactory: DataSource.Factory,
    ) : DataSource {

        private val transferListeners = mutableListOf<TransferListener>()
        private var activeDelegate: DataSource? = null

        /**
         * Registers [transferListener] to be forwarded onto the active delegate when [open] runs.
         * Also attaches immediately if a delegate is already open.
         */
        override fun addTransferListener(transferListener: TransferListener) {
            transferListeners += transferListener
            activeDelegate?.addTransferListener(transferListener)
        }

        /**
         * Selects cached HTTP or direct local [DataSource] based on [dataSpec] URI scheme and opens it.
         * Closes any leftover active delegate first if [open] is invoked twice without [close].
         */
        override fun open(dataSpec: DataSpec): Long {
            close()
            val scheme = dataSpec.uri.scheme?.lowercase()
            val delegate = if (scheme == "http" || scheme == "https") {
                cacheDataSourceFactory.createDataSource()
            } else {
                localDataSourceFactory.createDataSource()
            }
            for (listener in transferListeners) {
                delegate.addTransferListener(listener)
            }
            activeDelegate = delegate
            return delegate.open(dataSpec)
        }

        /**
         * Reads up to [length] bytes from the currently open [activeDelegate] into [buffer].
         * Returns [C.RESULT_END_OF_INPUT] if no stream is open or end-of-stream is reached.
         */
        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            val delegate = activeDelegate ?: return C.RESULT_END_OF_INPUT
            return delegate.read(buffer, offset, length)
        }

        /**
         * Returns the resolved [Uri] of the currently open delegate, or null when closed.
         * Delegates directly to [activeDelegate].
         */
        override fun getUri(): Uri? = activeDelegate?.uri

        /**
         * Returns HTTP response headers from the active delegate, or an empty map for local streams.
         * Delegates directly to [activeDelegate].
         */
        override fun getResponseHeaders(): Map<String, List<String>> =
            activeDelegate?.responseHeaders ?: emptyMap()

        /**
         * Closes the active delegate stream deterministically and clears the reference in `finally`.
         * Ensures native file descriptors and OkHttp response bodies are never leaked.
         */
        override fun close() {
            val delegate = activeDelegate ?: return
            try {
                delegate.close()
            } finally {
                activeDelegate = null
            }
        }
    }
}
