package com.kenjigames.ividsmusic.data.stream

import com.kenjigames.ividsmusic.domain.model.AppResult
import com.kenjigames.ividsmusic.domain.model.AudioQuality
import com.kenjigames.ividsmusic.domain.model.DomainError
import com.kenjigames.ividsmusic.domain.model.StreamSource
import com.kenjigames.ividsmusic.domain.model.Track
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs

/**
 * Pure-JVM OkHttp adapter implementing NewPipeExtractor's [Downloader] contract with deterministic body cleanup.
 * Maps HTTP methods, headers, payloads, and redirects between NewPipe and the shared [OkHttpClient].
 */
class OkHttpNewPipeDownloader(
    private val client: OkHttpClient
) : Downloader() {

    /**
     * Executes a synchronous NewPipe HTTP [request] over [client] and closes the underlying OkHttp response cleanly.
     */
    @Throws(IOException::class)
    override fun execute(request: Request): Response {
        val bodyBytes = request.dataToSend()
        val requestBody = if (bodyBytes != null) {
            val contentType = request.headers()["Content-Type"]?.firstOrNull()?.toMediaTypeOrNull()
            bodyBytes.toRequestBody(contentType)
        } else {
            null
        }

        val okBuilder = okhttp3.Request.Builder()
            .url(request.url())
            .method(request.httpMethod(), requestBody)
            .header("User-Agent", USER_AGENT)

        for ((headerName, values) in request.headers()) {
            for (value in values) {
                okBuilder.addHeader(headerName, value)
            }
        }

        client.newCall(okBuilder.build()).execute().use { okResponse ->
            val responseHeaders = LinkedHashMap<String, MutableList<String>>()
            for ((name, value) in okResponse.headers) {
                responseHeaders.getOrPut(name) { mutableListOf() }.add(value)
            }
            val bodyString = okResponse.body?.string() ?: ""
            return Response(
                okResponse.code,
                okResponse.message,
                responseHeaders,
                bodyString,
                okResponse.request.url.toString()
            )
        }
    }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
    }
}

/**
 * Tier-3 [StreamTierResolver] and [YouTubeMetadataPreviewer] powered by pure-JVM NewPipeExtractor `v0.24.4`.
 * Runs blocking extractions on [ioDispatcher], enforces <= 15s duration tolerance, and prefers `audio/mp4` M4A streams.
 */
class NewPipeStreamResolver(
    okHttpClient: OkHttpClient,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clockMs: () -> Long = { System.currentTimeMillis() }
) : StreamTierResolver, YouTubeMetadataPreviewer {

    private val downloader = OkHttpNewPipeDownloader(okHttpClient)

    /**
     * Searches YouTube (unless [track.videoId] is present) and selects the M4A audio stream closest to [preferredQuality].
     */
    override suspend fun resolveTrack(
        track: Track,
        preferredQuality: AudioQuality
    ): AppResult<StreamSource.RemoteUrl> = withContext(ioDispatcher) {
        try {
            ensureInitialized()
            val videoUrl = track.videoId?.takeIf { it.isNotBlank() }?.let { id ->
                "https://www.youtube.com/watch?v=$id"
            } ?: searchBestMatchingVideoUrl(track)
            ?: return@withContext AppResult.Failure(
                DomainError.StreamNotFound("NewPipe found no matching video for '${track.artist} - ${track.title}'")
            )
            extractStreamFromWatchUrl(videoUrl, preferredQuality)
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (error: Exception) {
            AppResult.Failure(
                DomainError.StreamNotFound(error.message ?: "NewPipe stream extraction failed")
            )
        }
    }

    /**
     * Extracts an audio stream directly for [videoUrlOrId] using NewPipe's YouTube stream extractor.
     */
    override suspend fun resolveDirectUrl(
        videoUrlOrId: String,
        preferredQuality: AudioQuality
    ): AppResult<StreamSource.RemoteUrl> = withContext(ioDispatcher) {
        val videoId = extractYouTubeVideoId(videoUrlOrId)
            ?: return@withContext AppResult.Failure(
                DomainError.Validation("Invalid YouTube URL or ID: $videoUrlOrId")
            )
        try {
            ensureInitialized()
            extractStreamFromWatchUrl("https://www.youtube.com/watch?v=$videoId", preferredQuality)
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (error: Exception) {
            AppResult.Failure(
                DomainError.StreamNotFound(error.message ?: "NewPipe direct URL extraction failed")
            )
        }
    }

    /**
     * Extracts single-video or playlist track metadata from [youtubeUrl] for DownloaderScreen previews.
     */
    override suspend fun previewUrl(youtubeUrl: String): AppResult<List<Track>> = withContext(ioDispatcher) {
        val trimmed = youtubeUrl.trim()
        if (trimmed.isEmpty()) {
            return@withContext AppResult.Failure(DomainError.Validation("YouTube URL cannot be blank"))
        }
        try {
            ensureInitialized()
            val isPlaylist = trimmed.contains("list=", ignoreCase = true) && !trimmed.contains("v=", ignoreCase = true)
            if (isPlaylist || trimmed.contains("/playlist", ignoreCase = true)) {
                val playlistExtractor = ServiceList.YouTube.getPlaylistExtractor(trimmed)
                playlistExtractor.fetchPage()
                val tracks = playlistExtractor.initialPage.items
                    .filterIsInstance<StreamInfoItem>()
                    .mapNotNull(::mapStreamInfoItem)
                if (tracks.isEmpty()) {
                    AppResult.Failure(DomainError.StreamNotFound("Playlist contains no extractable audio tracks"))
                } else {
                    AppResult.Success(tracks)
                }
            } else {
                val extractor = ServiceList.YouTube.getStreamExtractor(trimmed)
                extractor.fetchPage()
                val videoId = extractYouTubeVideoId(extractor.url) ?: extractYouTubeVideoId(trimmed)
                val cover = extractor.thumbnails.maxByOrNull { it.width * it.height }?.url.orEmpty()
                val track = Track(
                    id = "yt_${videoId ?: extractor.id}",
                    title = extractor.name.ifBlank { "YouTube Audio" },
                    artist = extractor.uploaderName.ifBlank { "YouTube" },
                    coverUrl = cover,
                    durationSeconds = extractor.length.toInt().coerceAtLeast(0),
                    videoId = videoId ?: extractor.id
                )
                AppResult.Success(listOf(track))
            }
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (error: Exception) {
            AppResult.Failure(DomainError.Network(error.message ?: "Failed to preview YouTube URL"))
        }
    }

    private fun ensureInitialized() {
        if (initialized.compareAndSet(false, true)) {
            try {
                NewPipe.init(downloader, Localization("US", "en"))
            } catch (error: Exception) {
                initialized.set(false)
                throw error
            }
        }
    }

    private fun searchBestMatchingVideoUrl(track: Track): String? {
        val query = buildYouTubeSearchQuery(track)
        if (query.isBlank()) return null

        val searchExtractor = ServiceList.YouTube.getSearchExtractor(query)
        searchExtractor.fetchPage()

        val candidates = searchExtractor.initialPage.items
            .filterIsInstance<StreamInfoItem>()
            .filter { !it.url.isNullOrBlank() && it.duration > 0L }

        if (candidates.isEmpty()) {
            return searchExtractor.initialPage.items
                .filterIsInstance<StreamInfoItem>()
                .firstOrNull { !it.url.isNullOrBlank() }
                ?.url
        }

        val tolerantMatch = candidates.firstOrNull { item ->
            isDurationWithinTolerance(item.duration.toInt(), track.durationSeconds)
        }
        return (tolerantMatch ?: candidates.first()).url
    }

    private fun extractStreamFromWatchUrl(
        videoUrl: String,
        preferredQuality: AudioQuality
    ): AppResult<StreamSource.RemoteUrl> {
        val extractor = ServiceList.YouTube.getStreamExtractor(videoUrl)
        extractor.fetchPage()

        val streams = extractor.audioStreams.filter { !it.content.isNullOrBlank() }
        if (streams.isEmpty()) {
            return AppResult.Failure(DomainError.StreamNotFound("NewPipe returned no audio streams for $videoUrl"))
        }

        val m4aStreams = streams.filter { stream ->
            stream.format == MediaFormat.M4A ||
                stream.format?.mimeType?.contains("audio/mp4", ignoreCase = true) == true
        }
        val pool = m4aStreams.ifEmpty { streams }
        val chosen: AudioStream = pool.minByOrNull { stream ->
            val kbps = normalizeBitrateKbps(stream.averageBitrate).takeIf { it > 0 } ?: AudioQuality.Q128.kbps
            abs(kbps - preferredQuality.kbps)
        } ?: return AppResult.Failure(DomainError.StreamNotFound("No selectable audio stream for $videoUrl"))

        val resolvedKbps = normalizeBitrateKbps(chosen.averageBitrate).takeIf { it > 0 } ?: preferredQuality.kbps
        val mime = chosen.format?.mimeType ?: "audio/mp4"
        return AppResult.Success(
            StreamSource.RemoteUrl(
                url = chosen.content,
                mimeType = mime,
                videoId = extractYouTubeVideoId(videoUrl) ?: extractor.id,
                quality = AudioQuality.fromKbps(resolvedKbps),
                resolvedAtMs = clockMs()
            )
        )
    }

    private fun mapStreamInfoItem(item: StreamInfoItem): Track? {
        val videoId = extractYouTubeVideoId(item.url.orEmpty()) ?: return null
        val cover = item.thumbnails.maxByOrNull { it.width * it.height }?.url.orEmpty()
        return Track(
            id = "yt_$videoId",
            title = item.name.orEmpty().ifBlank { "YouTube Track" },
            artist = item.uploaderName.orEmpty().ifBlank { "YouTube" },
            coverUrl = cover,
            durationSeconds = item.duration.toInt().coerceAtLeast(0),
            videoId = videoId
        )
    }

    companion object {
        private val initialized = AtomicBoolean(false)
    }
}
