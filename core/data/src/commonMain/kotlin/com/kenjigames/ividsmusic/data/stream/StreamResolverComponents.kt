package com.kenjigames.ividsmusic.data.stream

import com.kenjigames.ividsmusic.data.remote.dto.InvidiousAdaptiveFormatDto
import com.kenjigames.ividsmusic.data.remote.dto.InvidiousSearchVideoDto
import com.kenjigames.ividsmusic.data.remote.dto.InvidiousVideoDetailsDto
import com.kenjigames.ividsmusic.domain.model.AppResult
import com.kenjigames.ividsmusic.domain.model.AudioQuality
import com.kenjigames.ividsmusic.domain.model.DomainError
import com.kenjigames.ividsmusic.domain.model.StreamSource
import com.kenjigames.ividsmusic.domain.model.Track
import com.kenjigames.ividsmusic.domain.port.StreamResolver
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs

/**
 * Abstraction for an individual remote audio stream extraction strategy such as NewPipe, Invidious, or yt-dlp.
 * Invoked sequentially or in races by [CompositeStreamResolver].
 */
interface StreamTierResolver {
    /** Resolves a remote [StreamSource.RemoteUrl] matching [track] and [preferredQuality]. */
    suspend fun resolveTrack(
        track: Track,
        preferredQuality: AudioQuality
    ): AppResult<StreamSource.RemoteUrl>

    /** Resolves a remote [StreamSource.RemoteUrl] directly from a YouTube watch URL or video ID. */
    suspend fun resolveDirectUrl(
        videoUrlOrId: String,
        preferredQuality: AudioQuality
    ): AppResult<StreamSource.RemoteUrl>
}

/**
 * Platform-specific final fallback resolver used after NewPipe and Invidious fail (desktop `yt-dlp.exe`, Android no-op).
 */
interface PlatformStreamFallback : StreamTierResolver

/**
 * Abstraction for extracting preview track metadata from a YouTube video or playlist URL prior to downloading.
 */
interface YouTubeMetadataPreviewer {
    /** Fetches one or more candidate [Track] records described by [youtubeUrl]. */
    suspend fun previewUrl(youtubeUrl: String): AppResult<List<Track>>
}

/**
 * Locates valid pre-downloaded `.m4a` or `.mp3` files on disk before performing any remote stream lookups.
 */
interface LocalAudioLocator {
    /** Checks [track.localFilePath] and matching filenames inside [savedDirPath], returning a readable file path or `null`. */
    fun findLocalAudioPath(track: Track, savedDirPath: String): String?

    /** Returns `true` only when [filePath] points to a non-empty existing regular file. */
    fun isValidLocalFile(filePath: String?): Boolean
}

/**
 * Thread-safe bounded 30-minute TTL in-memory cache for resolved remote audio stream URLs.
 * Prevents redundant YouTube extractions across replays, downloads, and rapid seeks.
 */
class InMemoryStreamTtlCache(
    private val clockMs: () -> Long = { System.currentTimeMillis() },
    private val ttlMs: Long = DEFAULT_TTL_MS
) {
    private val mutex = Mutex()
    private val entries = object : LinkedHashMap<String, CachedStream>(MAX_ENTRIES + 1, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CachedStream>?): Boolean =
            size > MAX_ENTRIES
    }

    /** Returns a non-expired [StreamSource.RemoteUrl] for [cacheKey], evicting stale entries transparently. */
    suspend fun get(cacheKey: String): StreamSource.RemoteUrl? {
        val now = clockMs()
        return mutex.withLock {
            val cached = entries[cacheKey] ?: return@withLock null
            if (now - cached.cachedAtMs >= ttlMs) {
                entries.remove(cacheKey)
                null
            } else {
                cached.source
            }
        }
    }

    /** Stores [source] under [cacheKey] tagged with [trackId] and the current clock timestamp. */
    suspend fun put(cacheKey: String, trackId: String, source: StreamSource.RemoteUrl) {
        val now = clockMs()
        mutex.withLock {
            entries[cacheKey] = CachedStream(
                trackId = trackId,
                source = source,
                cachedAtMs = now
            )
        }
    }

    /** Removes all cached stream entries associated with [trackId]. */
    suspend fun invalidateTrack(trackId: String) {
        if (trackId.isBlank()) return
        mutex.withLock {
            val iterator = entries.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (entry.value.trackId == trackId || entry.key.startsWith("track:$trackId:")) {
                    iterator.remove()
                }
            }
        }
    }

    /** Synchronously requests removal of entries keyed to [trackId] on the next cache cleanup or non-blocking lock. */
    fun invalidateTrackNonBlocking(trackId: String) {
        if (trackId.isBlank()) return
        if (mutex.tryLock()) {
            try {
                val iterator = entries.entries.iterator()
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    if (entry.value.trackId == trackId || entry.key.startsWith("track:$trackId:")) {
                        iterator.remove()
                    }
                }
            } finally {
                mutex.unlock()
            }
        }
    }

    /** Clears every cached stream entry, typically invoked when the user clears audio cache. */
    suspend fun clear() {
        mutex.withLock {
            entries.clear()
        }
    }

    private data class CachedStream(
        val trackId: String,
        val source: StreamSource.RemoteUrl,
        val cachedAtMs: Long
    )

    companion object {
        const val DEFAULT_TTL_MS = 30 * 60 * 1000L
        private const val MAX_ENTRIES = 200
    }
}

/**
 * Tier-4a [StreamTierResolver] racing public Invidious instances in parallel with a 3.5-second timeout.
 * Cancels slower instances immediately once any mirror yields a matching audio stream URL.
 */
class InvidiousFallbackResolver(
    private val httpClient: HttpClient,
    private val json: Json,
    private val clockMs: () -> Long = { System.currentTimeMillis() },
    private val instances: List<String> = DEFAULT_INSTANCES,
    private val raceTimeoutMs: Long = RACE_TIMEOUT_MS
) : StreamTierResolver {

    /** Searches and extracts an audio stream for [track] by racing configured Invidious instances in parallel. */
    override suspend fun resolveTrack(
        track: Track,
        preferredQuality: AudioQuality
    ): AppResult<StreamSource.RemoteUrl> {
        val knownVideoId = track.videoId?.takeIf { it.isNotBlank() }
        if (knownVideoId != null) {
            return resolveVideoIdAcrossMirrors(knownVideoId, preferredQuality)
        }
        val searchQuery = buildYouTubeSearchQuery(track)
        if (searchQuery.isBlank()) {
            return AppResult.Failure(DomainError.Validation("Track query cannot be blank"))
        }
        return raceMirrors { instance ->
            val videoId = searchVideoIdOnMirror(instance, searchQuery, track.durationSeconds)
                ?: return@raceMirrors AppResult.Failure(
                    DomainError.StreamNotFound("No Invidious video match within tolerance on $instance")
                )
            fetchAudioStreamFromMirror(instance, videoId, preferredQuality)
        }
    }

    /** Extracts a playable stream directly for a YouTube URL or 11-character video ID across Invidious mirrors. */
    override suspend fun resolveDirectUrl(
        videoUrlOrId: String,
        preferredQuality: AudioQuality
    ): AppResult<StreamSource.RemoteUrl> {
        val videoId = extractYouTubeVideoId(videoUrlOrId)
            ?: return AppResult.Failure(DomainError.Validation("Invalid YouTube video URL or ID: $videoUrlOrId"))
        return resolveVideoIdAcrossMirrors(videoId, preferredQuality)
    }

    private suspend fun resolveVideoIdAcrossMirrors(
        videoId: String,
        preferredQuality: AudioQuality
    ): AppResult<StreamSource.RemoteUrl> = raceMirrors { instance ->
        fetchAudioStreamFromMirror(instance, videoId, preferredQuality)
    }

    private suspend fun raceMirrors(
        attempt: suspend (instance: String) -> AppResult<StreamSource.RemoteUrl>
    ): AppResult<StreamSource.RemoteUrl> {
        if (instances.isEmpty()) {
            return AppResult.Failure(DomainError.StreamNotFound("No Invidious instances configured"))
        }
        return coroutineScope {
            val channel = Channel<AppResult<StreamSource.RemoteUrl>>(capacity = instances.size)
            val jobs = instances.map { instance ->
                launch {
                    val result = try {
                        attempt(instance.trimEnd('/'))
                    } catch (cancel: CancellationException) {
                        throw cancel
                    } catch (error: Exception) {
                        AppResult.Failure(DomainError.Network(error.message ?: "Invidious error on $instance"))
                    }
                    channel.trySend(result)
                }
            }

            var lastFailure: DomainError = DomainError.StreamNotFound("Invidious race timed out after ${raceTimeoutMs}ms")
            val winner = withTimeoutOrNull(raceTimeoutMs) {
                repeat(instances.size) {
                    val outcome = channel.receive()
                    if (outcome is AppResult.Success) {
                        return@withTimeoutOrNull outcome
                    }
                    if (outcome is AppResult.Failure) {
                        lastFailure = outcome.error
                    }
                }
                null
            }

            jobs.forEach { it.cancel() }
            channel.close()
            winner ?: AppResult.Failure(lastFailure)
        }
    }

    private suspend fun searchVideoIdOnMirror(
        instance: String,
        query: String,
        expectedDurationSeconds: Int
    ): String? {
        val response = httpClient.get("$instance/api/v1/search") {
            parameter("q", query)
            parameter("type", "video")
        }
        if (!response.status.isSuccess()) return null
        val items = json.decodeFromString<List<InvidiousSearchVideoDto>>(response.bodyAsText())
        val validCandidates = items.filter { it.videoId.isNotBlank() && it.lengthSeconds > 0 }
        if (validCandidates.isEmpty()) return items.firstOrNull { it.videoId.isNotBlank() }?.videoId

        return validCandidates.firstOrNull {
            isDurationWithinTolerance(it.lengthSeconds, expectedDurationSeconds)
        }?.videoId ?: validCandidates.firstOrNull()?.videoId
    }

    private suspend fun fetchAudioStreamFromMirror(
        instance: String,
        videoId: String,
        preferredQuality: AudioQuality
    ): AppResult<StreamSource.RemoteUrl> {
        val response = httpClient.get("$instance/api/v1/videos/$videoId")
        if (!response.status.isSuccess()) {
            return AppResult.Failure(
                DomainError.Network("Invidious HTTP ${response.status.value} from $instance", response.status.value)
            )
        }
        val details = json.decodeFromString<InvidiousVideoDetailsDto>(response.bodyAsText())
        val selected = selectBestAdaptiveFormat(details.adaptiveFormats, preferredQuality)
            ?: return AppResult.Failure(DomainError.StreamNotFound("No audio stream in Invidious response on $instance"))

        val streamUrl = selected.url.orEmpty()
        val parsedKbps = parseInvidiousBitrateKbps(selected.bitrate) ?: preferredQuality.kbps
        val mime = selected.type?.substringBefore(';')?.trim()?.ifEmpty { "audio/mp4" } ?: "audio/mp4"
        return AppResult.Success(
            StreamSource.RemoteUrl(
                url = streamUrl,
                mimeType = mime,
                videoId = videoId,
                quality = AudioQuality.fromKbps(parsedKbps),
                resolvedAtMs = clockMs()
            )
        )
    }

    private fun selectBestAdaptiveFormat(
        formats: List<InvidiousAdaptiveFormatDto>,
        preferredQuality: AudioQuality
    ): InvidiousAdaptiveFormatDto? {
        val audioOnly = formats.filter { fmt ->
            !fmt.url.isNullOrBlank() && (fmt.type?.contains("audio/", ignoreCase = true) == true)
        }
        if (audioOnly.isEmpty()) return null

        val m4aPool = audioOnly.filter { fmt ->
            fmt.type?.contains("audio/mp4", ignoreCase = true) == true ||
                fmt.container.equals("m4a", ignoreCase = true)
        }
        val pool = m4aPool.ifEmpty { audioOnly }
        return pool.minByOrNull { fmt ->
            val kbps = parseInvidiousBitrateKbps(fmt.bitrate) ?: AudioQuality.Q128.kbps
            abs(kbps - preferredQuality.kbps)
        }
    }

    private fun parseInvidiousBitrateKbps(rawBitrate: String?): Int? {
        val numeric = rawBitrate?.toIntOrNull() ?: return null
        return if (numeric > 1_000) numeric / 1_000 else numeric
    }

    companion object {
        const val RACE_TIMEOUT_MS = 3_500L
        val DEFAULT_INSTANCES = listOf(
            "https://invidious.flokinet.to",
            "https://iv.melmac.space",
            "https://invidious.drgns.space",
            "https://invidious.perennialte.chs.org",
            "https://yt.artemislena.eu"
        )
    }
}

/**
 * Reliable 4-tier [StreamResolver]:
 * 1) Existing local offline file in `saved/`,
 * 2) 30-minute in-memory TTL cache,
 * 3) Pure-JVM NewPipeExtractor (`audio/mp4` M4A within <= 15s duration tolerance),
 * 4) Parallel 3.5s Invidious race followed by Desktop `yt-dlp.exe` fallback (10s timeout).
 */
class CompositeStreamResolver(
    private val localAudioLocator: LocalAudioLocator,
    private val savedDirectoryProvider: () -> String,
    private val streamTtlCache: InMemoryStreamTtlCache,
    private val primaryExtractor: StreamTierResolver,
    private val invidiousFallback: StreamTierResolver,
    private val platformFallback: PlatformStreamFallback
) : StreamResolver {

    /** Resolves a local or remote audio stream for [track] across all 4 ordered tiers. */
    override suspend fun resolve(
        track: Track,
        preferredQuality: AudioQuality
    ): AppResult<StreamSource> {
        if (track.title.isBlank() && track.videoId.isNullOrBlank() && track.localFilePath.isNullOrBlank()) {
            return AppResult.Failure(DomainError.Validation("Track lacks title, videoId, and local file path"))
        }

        // Tier 1: Existing local offline audio file
        val localPath = localAudioLocator.findLocalAudioPath(track, savedDirectoryProvider())
        if (localPath != null) {
            val localQuality = AudioQuality.fromKbps(track.bitrateKbps ?: preferredQuality.kbps)
            return AppResult.Success(StreamSource.LocalFile(filePath = localPath, quality = localQuality))
        }

        // Tier 2: In-memory 30-minute TTL cache
        val cacheKey = "track:${track.id}:${preferredQuality.kbps}"
        streamTtlCache.get(cacheKey)?.let { cached ->
            return AppResult.Success(cached)
        }

        // Tier 3: NewPipeExtractor
        val tier3 = primaryExtractor.resolveTrack(track, preferredQuality)
        if (tier3 is AppResult.Success) {
            streamTtlCache.put(cacheKey, track.id, tier3.data)
            return tier3
        }

        // Tier 4a: Parallel 3.5s Invidious race
        val tier4a = invidiousFallback.resolveTrack(track, preferredQuality)
        if (tier4a is AppResult.Success) {
            streamTtlCache.put(cacheKey, track.id, tier4a.data)
            return tier4a
        }

        // Tier 4b: Desktop yt-dlp.exe fallback (no-op on Android)
        val tier4b = platformFallback.resolveTrack(track, preferredQuality)
        if (tier4b is AppResult.Success) {
            streamTtlCache.put(cacheKey, track.id, tier4b.data)
            return tier4b
        }

        val failureMessage = buildString {
            append("Stream resolution failed for '${track.artist} - ${track.title}'. ")
            append("NewPipe: ${(tier3 as AppResult.Failure).error.message}; ")
            append("Invidious: ${(tier4a as AppResult.Failure).error.message}; ")
            append("Platform: ${(tier4b as AppResult.Failure).error.message}")
        }
        return AppResult.Failure(DomainError.StreamNotFound(failureMessage))
    }

    /** Resolves a direct YouTube watch URL or video ID using TTL cache, NewPipe, Invidious, and Desktop yt-dlp. */
    override suspend fun resolveUrl(
        videoUrlOrId: String,
        preferredQuality: AudioQuality
    ): AppResult<StreamSource> {
        val videoId = extractYouTubeVideoId(videoUrlOrId)
            ?: return AppResult.Failure(DomainError.Validation("Invalid YouTube URL or video ID: $videoUrlOrId"))

        val cacheKey = "url:$videoId:${preferredQuality.kbps}"
        streamTtlCache.get(cacheKey)?.let { cached ->
            return AppResult.Success(cached)
        }

        val tier3 = primaryExtractor.resolveDirectUrl(videoId, preferredQuality)
        if (tier3 is AppResult.Success) {
            streamTtlCache.put(cacheKey, videoId, tier3.data)
            return tier3
        }

        val tier4a = invidiousFallback.resolveDirectUrl(videoId, preferredQuality)
        if (tier4a is AppResult.Success) {
            streamTtlCache.put(cacheKey, videoId, tier4a.data)
            return tier4a
        }

        val tier4b = platformFallback.resolveDirectUrl(videoId, preferredQuality)
        if (tier4b is AppResult.Success) {
            streamTtlCache.put(cacheKey, videoId, tier4b.data)
            return tier4b
        }

        return AppResult.Failure(
            DomainError.StreamNotFound(
                "Could not resolve URL '$videoUrlOrId': ${(tier4b as AppResult.Failure).error.message}"
            )
        )
    }

    /** Invalidates cached stream entries for [trackId] so subsequent playback fetches fresh stream URLs. */
    override fun invalidate(trackId: String) {
        streamTtlCache.invalidateTrackNonBlocking(trackId)
    }
}

/**
 * Formats a clean YouTube search query from [track] artist and title metadata without mutating state.
 */
internal fun buildYouTubeSearchQuery(track: Track): String {
    val cleanArtist = track.artist.trim()
    val cleanTitle = track.title.trim()
    return when {
        cleanArtist.isNotEmpty() && cleanTitle.isNotEmpty() -> "$cleanArtist - $cleanTitle audio"
        cleanTitle.isNotEmpty() -> "$cleanTitle audio"
        else -> cleanArtist
    }
}

/**
 * Verifies whether [candidateSeconds] lies within 15 seconds of [expectedSeconds], allowing any match when unknown (`<= 0`).
 */
internal fun isDurationWithinTolerance(candidateSeconds: Int, expectedSeconds: Int): Boolean {
    if (expectedSeconds <= 0 || candidateSeconds <= 0) return true
    return abs(candidateSeconds - expectedSeconds) <= DURATION_TOLERANCE_SECONDS
}

/**
 * Normalizes bitrates reported in either kbps (`4..512`) or bps (`4_000..512_000`) into integer kbps.
 */
internal fun normalizeBitrateKbps(rawBitrate: Int): Int {
    if (rawBitrate <= 0) return 0
    return if (rawBitrate > 1_000) rawBitrate / 1_000 else rawBitrate
}

/**
 * Extracts an 11-character YouTube video ID from standard watch, short `youtu.be`, `shorts`, or raw ID inputs.
 */
internal fun extractYouTubeVideoId(raw: String): String? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null
    if (VIDEO_ID_REGEX.matches(trimmed)) return trimmed

    val watchMatch = WATCH_V_REGEX.find(trimmed)?.groupValues?.getOrNull(1)
    if (watchMatch != null) return watchMatch

    val shortMatch = SHORT_OR_SHORTS_REGEX.find(trimmed)?.groupValues?.getOrNull(1)
    if (shortMatch != null) return shortMatch

    return null
}

private const val DURATION_TOLERANCE_SECONDS = 15
private val VIDEO_ID_REGEX = Regex("^[A-Za-z0-9_-]{11}$")
private val WATCH_V_REGEX = Regex("[?&]v=([A-Za-z0-9_-]{11})")
private val SHORT_OR_SHORTS_REGEX = Regex("(?:youtu\\.be/|/shorts/|/embed/)([A-Za-z0-9_-]{11})")
