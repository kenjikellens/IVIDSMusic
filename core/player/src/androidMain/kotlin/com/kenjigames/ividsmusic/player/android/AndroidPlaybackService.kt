package com.kenjigames.ividsmusic.player.android

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import com.google.common.util.concurrent.ListenableFuture
import okhttp3.OkHttpClient
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

/**
 * Background Media3 [MediaSessionService] hosting [ExoPlayer], 500 MB LRU caching, and system notifications.
 * Bridges hardware, lockscreen, and notification next/previous controls back to [AndroidAudioEngine].
 */
@OptIn(UnstableApi::class)
class AndroidPlaybackService : MediaSessionService() {

    private var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var okHttpClient: OkHttpClient? = null

    /**
     * Initializes [OkHttpClient], singleton [AudioCacheProvider], [ExoPlayer], [MediaSession], and notifications.
     * Configures music audio attributes, noisy-output auto-pause, and network wake lock.
     */
    override fun onCreate() {
        super.onCreate()
        val client = OkHttpClient.Builder()
            .connectTimeout(15L, TimeUnit.SECONDS)
            .readTimeout(30L, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
        okHttpClient = client

        val cache = AudioCacheProvider.getCache(applicationContext)
        val dataSourceFactory = PlaybackDataSourceFactory(
            context = applicationContext,
            cache = cache,
            okHttpClient = client,
            headerRegistry = StreamHeaderRegistry,
        )

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
        exoPlayer = player

        val sessionPlayer = ExternalSkipForwardingPlayer(
            wrapped = player,
            onSkipNext = { broadcastCustomSessionCommand(ACTION_COMMAND_NEXT) },
            onSkipPrevious = { broadcastCustomSessionCommand(ACTION_COMMAND_PREVIOUS) },
        )

        val sessionBuilder = MediaSession.Builder(this, sessionPlayer)
            .setCallback(IvidsMediaSessionCallback())
        createLaunchActivityPendingIntent()?.let { pendingIntent ->
            sessionBuilder.setSessionActivity(pendingIntent)
        }

        mediaSession = sessionBuilder.build()
        setMediaNotificationProvider(DefaultMediaNotificationProvider.Builder(this).build())
    }

    /**
     * Returns the active [MediaSession] for incoming MediaController and system media-button connections.
     * Returns null only before [onCreate] or after [onDestroy].
     */
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    /**
     * Stops this foreground service when the user dismisses the app task while playback is paused or empty.
     * Keeps active background playback alive if the player is currently playing.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = exoPlayer
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    /**
     * Releases [mediaSession], [exoPlayer], [StreamHeaderRegistry], and OkHttp executor pools deterministically.
     * Prevents player, binder, and HTTP dispatcher leaks when Android tears down the service.
     */
    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        exoPlayer?.release()
        exoPlayer = null
        StreamHeaderRegistry.clear()
        okHttpClient?.dispatcher?.executorService?.shutdown()
        okHttpClient = null
        super.onDestroy()
    }

    /**
     * Broadcasts a custom Media3 [SessionCommand] to connected controllers for external skip controls.
     * Safely no-ops if [mediaSession] has already been released.
     */
    private fun broadcastCustomSessionCommand(action: String) {
        mediaSession?.broadcastCustomCommand(SessionCommand(action, Bundle.EMPTY), Bundle.EMPTY)
    }

    /**
     * Builds a launch [PendingIntent] targeting the host application's launcher activity when available.
     * Avoids coupling `:core:player` directly to `:composeApp` activity class names.
     */
    private fun createLaunchActivityPendingIntent(): PendingIntent? {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: return null
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /**
     * Reconstructs playable URIs, MIME types, and HTTP header mappings for MediaItems arriving over IPC.
     * Preserves custom request headers inside [StreamHeaderRegistry] prior to ExoPlayer data-source opening.
     */
    private class IvidsMediaSessionCallback : MediaSession.Callback {

        /**
         * Rebuilds [mediaItems] with resolved request URIs and registers custom HTTP headers in [StreamHeaderRegistry].
         * Returns an immediate [ListenableFuture] so Media3 accepts IPC items without stripping stream configuration.
         */
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> {
            val resolved = mediaItems.map { item ->
                val uri = item.requestMetadata.mediaUri ?: item.localConfiguration?.uri
                val extras = item.mediaMetadata.extras
                val mimeType = extras?.getString(StreamHeaderRegistry.EXTRA_MIME_TYPE)
                    ?: item.localConfiguration?.mimeType
                val headers = StreamHeaderRegistry.decodeHeadersBundle(extras)
                if (uri != null) {
                    StreamHeaderRegistry.putHeaders(uri.toString(), headers)
                }
                item.buildUpon()
                    .apply {
                        if (uri != null) setUri(uri)
                        if (!mimeType.isNullOrBlank()) setMimeType(mimeType)
                    }
                    .build()
            }.toMutableList()
            return ImmediateListenableFuture(resolved)
        }
    }

    /**
     * Wraps [ExoPlayer] to advertise next/previous commands on single-item sessions and forward them upstream.
     * Enables lockscreen, notification, and Bluetooth skip buttons to drive [AndroidAudioEngine.externalCommands].
     */
    private class ExternalSkipForwardingPlayer(
        wrapped: Player,
        private val onSkipNext: () -> Unit,
        private val onSkipPrevious: () -> Unit,
    ) : ForwardingPlayer(wrapped) {

        /**
         * Adds skip-next and skip-previous commands onto the underlying player's [Player.Commands].
         * Ensures Media3 renders transport next/previous actions even with a single active [MediaItem].
         */
        override fun getAvailableCommands(): Player.Commands {
            return super.getAvailableCommands().buildUpon()
                .addAll(
                    COMMAND_SEEK_TO_NEXT,
                    COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                    COMMAND_SEEK_TO_PREVIOUS,
                    COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                )
                .build()
        }

        /**
         * Reports next and previous commands as available so system media controllers invoke [seekToNext] / [seekToPrevious].
         * Delegates all other command queries to the wrapped [ExoPlayer].
         */
        override fun isCommandAvailable(command: Int): Boolean {
            return when (command) {
                COMMAND_SEEK_TO_NEXT,
                COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                COMMAND_SEEK_TO_PREVIOUS,
                COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> true
                else -> super.isCommandAvailable(command)
            }
        }

        /**
         * Emits the skip-next command to connected controllers instead of seeking within a single-item queue.
         * Allows the domain queue controller to resolve and play the next track.
         */
        override fun seekToNext() {
            onSkipNext()
        }

        /**
         * Emits the skip-next command for media-item-scoped next requests from Media3 notifications.
         * Keeps headset and lockscreen controls consistent with [seekToNext].
         */
        override fun seekToNextMediaItem() {
            onSkipNext()
        }

        /**
         * Emits the skip-previous command to connected controllers instead of restarting single-item timelines.
         * Allows the domain queue controller to handle previous-track navigation.
         */
        override fun seekToPrevious() {
            onSkipPrevious()
        }

        /**
         * Emits the skip-previous command for media-item-scoped previous requests from Media3 notifications.
         * Keeps headset and lockscreen controls consistent with [seekToPrevious].
         */
        override fun seekToPreviousMediaItem() {
            onSkipPrevious()
        }
    }

    /**
     * Minimal completed [ListenableFuture] avoiding direct compile dependency on Guava's `Futures` helper.
     * Immediately invokes listeners on the provided [Executor] with [result].
     */
    private class ImmediateListenableFuture<T>(
        private val result: T,
    ) : ListenableFuture<T> {

        /** Reports cancellation as unsupported because [result] is already resolved. */
        override fun cancel(mayInterruptIfRunning: Boolean): Boolean = false

        /** Always returns false because immediate results cannot be cancelled. */
        override fun isCancelled(): Boolean = false

        /** Always returns true because [result] is available immediately on construction. */
        override fun isDone(): Boolean = true

        /** Returns the precomputed [result] immediately without blocking. */
        override fun get(): T = result

        /** Returns the precomputed [result] immediately regardless of timeout parameters. */
        override fun get(timeout: Long, unit: TimeUnit): T = result

        /**
         * Executes [listener] immediately via [executor] because [result] is already computed.
         * Satisfies Media3's asynchronous callback contract without background threads.
         */
        override fun addListener(listener: Runnable, executor: Executor) {
            executor.execute(listener)
        }
    }

    companion object {
        const val ACTION_COMMAND_NEXT: String = "com.kenjigames.ividsmusic.ACTION_NEXT"
        const val ACTION_COMMAND_PREVIOUS: String = "com.kenjigames.ividsmusic.ACTION_PREVIOUS"
    }
}
