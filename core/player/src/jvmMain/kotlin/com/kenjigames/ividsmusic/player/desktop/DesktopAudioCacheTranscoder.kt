package com.kenjigames.ividsmusic.player.desktop

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Prepares Windows-compatible cached `.mp3` audio files for OpenJFX `MediaPlayer` via `ffmpeg`.
 * Eliminates `CGstAVPlaybackPipeline` silent hangs on audio-only `.m4a` and YouTube DASH streams.
 */
internal object DesktopAudioCacheTranscoder {

    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 30_000
    private const val STREAM_BUFFER_BYTES = 16_384
    private const val RANGE_CHUNK_BYTES = 524_288L
    private const val CHUNK_MAX_ATTEMPTS = 3
    private const val FFMPEG_TIMEOUT_SECONDS = 45L
    private const val PROCESS_EXIT_WAIT_SECONDS = 2L
    private const val MAX_REDIRECTS = 5
    private const val MIN_VALID_MP3_BYTES = 1_024L
    private const val DEFAULT_CACHE_LIMIT = 80
    private const val DEFAULT_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36"
    private val REDIRECT_CODES = setOf(301, 302, 303, 307, 308)
    private val keyLocks = ConcurrentHashMap<String, Mutex>()

    /**
     * Locates `ffmpeg.exe` across managed user tools, packaged app resources, working/parent dirs, WinGet, and `PATH`.
     * Returns the absolute executable path or `null` when `ffmpeg` is not installed.
     */
    internal fun findFfmpegExecutable(): String? {
        val candidates = mutableListOf<File>()
        System.getenv("LOCALAPPDATA")?.takeIf { it.isNotBlank() }?.let { localAppData ->
            candidates.add(File(localAppData, "IVIDSMusic/tools/ffmpeg.exe"))
        }
        System.getProperty("compose.application.resources.dir")?.takeIf { it.isNotBlank() }?.let { dir ->
            candidates.add(File(dir, "ffmpeg.exe"))
            candidates.add(File(dir, "ffmpeg"))
        }
        System.getProperty("user.dir")?.takeIf { it.isNotBlank() }?.let { userDir ->
            val current = File(userDir)
            candidates.add(File(current, "ffmpeg.exe"))
            candidates.add(File(current, "resources/windows/ffmpeg.exe"))
            candidates.add(File(current, "composeApp/resources/windows/ffmpeg.exe"))
            current.parentFile?.let { parent ->
                candidates.add(File(parent, "ffmpeg.exe"))
                candidates.add(File(parent, "resources/windows/ffmpeg.exe"))
                candidates.add(File(parent, "composeApp/resources/windows/ffmpeg.exe"))
            }
        }
        System.getenv("LOCALAPPDATA")?.takeIf { it.isNotBlank() }?.let { localAppData ->
            candidates.add(File(localAppData, "Microsoft/WinGet/Links/ffmpeg.exe"))
        }
        candidates.firstOrNull { it.exists() && it.isFile }?.let { return it.absolutePath }

        val pathEnv = System.getenv("PATH").orEmpty()
        for (dir in pathEnv.split(File.pathSeparatorChar)) {
            val trimmed = dir.trim()
            if (trimmed.isEmpty()) continue
            File(trimmed, "ffmpeg.exe").takeIf { it.exists() && it.isFile }?.let { return it.absolutePath }
            File(trimmed, "ffmpeg").takeIf { it.exists() && it.isFile }?.let { return it.absolutePath }
        }
        return null
    }

    /**
     * Resolves and ensures existence of the desktop audio transcode cache directory (`fx_audio_cache`).
     * Uses [overrideDir] when provided for isolated tests, otherwise `%LOCALAPPDATA%\IVIDSMusic\cache\fx_audio_cache`.
     */
    internal fun resolveCacheDirectory(overrideDir: File? = null): File {
        val dir = overrideDir ?: run {
            val localAppData = System.getenv("LOCALAPPDATA")?.takeIf { it.isNotBlank() }
            if (localAppData != null) {
                File(localAppData, "IVIDSMusic/cache/fx_audio_cache")
            } else {
                File(System.getProperty("user.home"), ".ividsmusic/cache/fx_audio_cache")
            }
        }
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Derives a stable 24-hex-char SHA-256 cache key for [mediaUri] using [cacheKeyHint] when available.
     * Falls back to `file:` metadata or YouTube `(id, itag)` query parameters so replays hit disk cache cleanly.
     */
    internal fun buildCacheKey(mediaUri: String, cacheKeyHint: String? = null): String {
        val cleanHint = cacheKeyHint?.trim()?.takeIf { it.isNotEmpty() }
        val descriptor = when {
            mediaUri.startsWith("file:", ignoreCase = true) -> {
                val file = resolveLocalFile(mediaUri)
                if (file != null && file.exists()) {
                    "file:${file.absolutePath}|${file.length()}|${file.lastModified()}"
                } else {
                    mediaUri
                }
            }
            cleanHint != null -> "hint:$cleanHint"
            else -> extractYouTubeStreamIdentity(mediaUri) ?: mediaUri
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(descriptor.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }.take(24)
    }

    /**
     * Checks whether [mediaUri] is already a local `.mp3` or `.wav` file that JavaFX `Media` plays natively.
     * Avoids redundant `ffmpeg` transcoding for already-compatible local audio files.
     */
    internal fun isDirectlyPlayableByJavaFx(mediaUri: String): Boolean {
        if (!mediaUri.startsWith("file:", ignoreCase = true)) return false
        val path = runCatching { URI(mediaUri).path }.getOrNull() ?: mediaUri
        val clean = path.substringBefore('?').lowercase()
        return clean.endsWith(".mp3") || clean.endsWith(".wav")
    }

    /**
     * Transcodes [mediaUri] into a cached local `.mp3` `file:` URI using singleflight locking on [cacheKeyHint].
     * Returns `null` when [mediaUri] is already directly playable, `ffmpeg` is missing, or transcoding fails.
     */
    suspend fun prepareJavaFxCompatibleUri(
        mediaUri: String,
        headers: Map<String, String>,
        cacheKeyHint: String? = null,
        cacheDirOverride: File? = null,
    ): String? {
        if (isDirectlyPlayableByJavaFx(mediaUri)) return null
        val ffmpeg = findFfmpegExecutable() ?: return null
        val cacheDir = resolveCacheDirectory(cacheDirOverride)
        val key = buildCacheKey(mediaUri, cacheKeyHint)
        val targetFile = File(cacheDir, "$key.mp3")

        if (targetFile.exists() && targetFile.isFile && targetFile.length() > MIN_VALID_MP3_BYTES) {
            targetFile.setLastModified(System.currentTimeMillis())
            return targetFile.toURI().toASCIIString()
        }

        val lock = keyLocks.computeIfAbsent(key) { Mutex() }
        return lock.withLock {
            withContext(Dispatchers.IO) {
                if (targetFile.exists() && targetFile.isFile && targetFile.length() > MIN_VALID_MP3_BYTES) {
                    targetFile.setLastModified(System.currentTimeMillis())
                    return@withContext targetFile.toURI().toASCIIString()
                }

                val partFile = File(cacheDir, "$key.${UUID.randomUUID()}.part.mp3")
                try {
                    val success = when {
                        mediaUri.startsWith("file:", ignoreCase = true) -> {
                            val localFile = resolveLocalFile(mediaUri) ?: return@withContext null
                            if (!localFile.exists() || !localFile.isFile) return@withContext null
                            transcodeLocalFileToMp3(ffmpeg, localFile, partFile)
                        }
                        mediaUri.startsWith("http://", ignoreCase = true) ||
                            mediaUri.startsWith("https://", ignoreCase = true) -> {
                            transcodeRemoteStreamToMp3(ffmpeg, mediaUri, headers, partFile)
                        }
                        else -> false
                    }
                    if (success && partFile.exists() && partFile.length() > MIN_VALID_MP3_BYTES) {
                        moveAtomically(partFile, targetFile)
                        pruneOldCacheFiles(cacheDir, DEFAULT_CACHE_LIMIT)
                        targetFile.toURI().toASCIIString()
                    } else {
                        partFile.delete()
                        null
                    }
                } catch (cancel: CancellationException) {
                    partFile.delete()
                    throw cancel
                } catch (_: Exception) {
                    partFile.delete()
                    null
                }
            }
        }
    }

    /**
     * Retains the newest [maxFiles] `.mp3` entries in [cacheDir] by `lastModified()` and deletes older files.
     * Ignores active `.part.mp3` temporary files so in-flight transcodes are never removed prematurely.
     */
    internal fun pruneOldCacheFiles(cacheDir: File, maxFiles: Int = DEFAULT_CACHE_LIMIT) {
        if (!cacheDir.exists() || !cacheDir.isDirectory || maxFiles < 0) return
        val cached = cacheDir.listFiles { file ->
            file.isFile &&
                file.name.endsWith(".mp3", ignoreCase = true) &&
                !file.name.endsWith(".part.mp3", ignoreCase = true)
        }?.sortedByDescending { it.lastModified() } ?: return

        if (cached.size <= maxFiles) return
        for (stale in cached.drop(maxFiles)) {
            stale.delete()
        }
    }

    /**
     * Transcodes a local audio file via `ffmpeg` into [partFile] with deterministic Windows process release.
     * Waits briefly after `destroyForcibly()` so [partFile] is never left locked on cancellation or timeout.
     */
    private suspend fun transcodeLocalFileToMp3(
        ffmpeg: String,
        sourceFile: File,
        partFile: File,
    ): Boolean {
        val process = ProcessBuilder(
            ffmpeg,
            "-y",
            "-v",
            "error",
            "-i",
            sourceFile.absolutePath,
            "-vn",
            "-c:a",
            "libmp3lame",
            "-b:a",
            "192k",
            "-compression_level",
            "7",
            partFile.absolutePath,
        ).start()

        val stderrDrain = spawnDrainThread(process.errorStream)
        return try {
            currentCoroutineContext().ensureActive()
            val finished = process.waitFor(FFMPEG_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (!finished) {
                terminateAndAwait(process)
                return false
            }
            stderrDrain.join(1_000L)
            process.exitValue() == 0
        } catch (cancel: CancellationException) {
            terminateAndAwait(process)
            throw cancel
        } finally {
            if (process.isAlive) terminateAndAwait(process)
        }
    }

    /**
     * Streams a remote HTTP(S) track into `ffmpeg` `pipe:0` in 512 KB bounded `Range` chunks with chunk-level retries.
     * Awaits child process exit after `destroyForcibly()` so `.part.mp3` locks release cleanly on Windows.
     */
    private suspend fun transcodeRemoteStreamToMp3(
        ffmpeg: String,
        mediaUri: String,
        headers: Map<String, String>,
        partFile: File,
    ): Boolean {
        val process = ProcessBuilder(
            ffmpeg,
            "-y",
            "-v",
            "error",
            "-i",
            "pipe:0",
            "-vn",
            "-c:a",
            "libmp3lame",
            "-b:a",
            "192k",
            "-compression_level",
            "7",
            partFile.absolutePath,
        ).start()

        val stderrDrain = spawnDrainThread(process.errorStream)
        return try {
            BufferedOutputStream(process.outputStream, STREAM_BUFFER_BYTES * 2).use { ffmpegIn ->
                streamRemoteChunksInto(mediaUri, headers, ffmpegIn)
                ffmpegIn.flush()
            }
            val finished = process.waitFor(FFMPEG_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (!finished) {
                terminateAndAwait(process)
                return false
            }
            stderrDrain.join(1_000L)
            process.exitValue() == 0
        } catch (cancel: CancellationException) {
            terminateAndAwait(process)
            throw cancel
        } catch (_: Exception) {
            terminateAndAwait(process)
            false
        } finally {
            if (process.isAlive) terminateAndAwait(process)
        }
    }

    /**
     * Fetches remote audio in sequential 512 KB byte-range chunks into [sink] with up to 3 attempts per chunk.
     * Falls back to an un-ranged `GET` when a non-DASH server rejects the initial `Range` header (`403`/`416`).
     */
    private suspend fun streamRemoteChunksInto(
        mediaUri: String,
        headers: Map<String, String>,
        sink: BufferedOutputStream,
    ) {
        var cursor = 0L
        var totalBytes: Long = -1L

        while (totalBytes < 0L || cursor < totalBytes) {
            currentCoroutineContext().ensureActive()
            val endByte = if (totalBytes > 0L) {
                minOf(totalBytes - 1L, cursor + RANGE_CHUNK_BYTES - 1L)
            } else {
                cursor + RANGE_CHUNK_BYTES - 1L
            }
            val result = fetchChunkWithRetry(
                mediaUri = mediaUri,
                headers = headers,
                cursor = cursor,
                endByte = endByte,
                sink = sink,
            )
            if (totalBytes < 0L && result.discoveredTotalBytes > 0L) {
                totalBytes = result.discoveredTotalBytes
            }
            if (result.bytesWritten <= 0L) break
            cursor += result.bytesWritten
            if (result.isFullResponse && (totalBytes < 0L || cursor >= totalBytes)) break
        }
    }

    /**
     * Downloads a single byte-range chunk (`cursor..endByte`) into [sink], retrying transient HTTP/IO errors.
     * Throws immediately on coroutine cancellation or persistent upstream failure after [CHUNK_MAX_ATTEMPTS].
     */
    private suspend fun fetchChunkWithRetry(
        mediaUri: String,
        headers: Map<String, String>,
        cursor: Long,
        endByte: Long,
        sink: BufferedOutputStream,
    ): ChunkResult {
        val buffer = ByteArray(STREAM_BUFFER_BYTES)
        var lastError: Exception? = null
        repeat(CHUNK_MAX_ATTEMPTS) { attempt ->
            currentCoroutineContext().ensureActive()
            var conn: HttpURLConnection? = null
            var bytesWrittenThisAttempt = 0L
            try {
                conn = followRedirects(mediaUri, headers, "bytes=$cursor-$endByte")
                    ?: throw IOException("Too many or unsafe redirects fetching $mediaUri")
                var code = conn.responseCode
                if (cursor == 0L && (code == 403 || code == 416)) {
                    conn.disconnect()
                    conn = followRedirects(mediaUri, headers, rangeHeader = null)
                        ?: throw IOException("Too many or unsafe redirects fetching $mediaUri")
                    code = conn.responseCode
                }
                if (code !in 200..299) {
                    throw IOException("HTTP $code streaming $mediaUri")
                }
                val isFull = (code == 200)
                val discoveredTotal = parseTotalFromContentRange(conn.getHeaderField("Content-Range"))
                    ?: conn.getHeaderField("Content-Length")?.trim()?.toLongOrNull()
                    ?: -1L
                conn.inputStream.use { input ->
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        sink.write(buffer, 0, read)
                        bytesWrittenThisAttempt += read
                    }
                }
                return ChunkResult(
                    bytesWritten = bytesWrittenThisAttempt,
                    discoveredTotalBytes = discoveredTotal,
                    isFullResponse = isFull,
                )
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (ex: Exception) {
                lastError = ex
                // Only retry if no partial bytes from this chunk were already flushed to ffmpeg's pipe.
                if (bytesWrittenThisAttempt > 0L || attempt == CHUNK_MAX_ATTEMPTS - 1) {
                    throw ex
                }
            } finally {
                conn?.disconnect()
            }
        }
        throw lastError ?: IOException("Chunk transfer failed")
    }

    /**
     * Validates [startUrl] and opens an upstream HTTP `GET` connection following up to [MAX_REDIRECTS] hops via [DesktopNetworkGuard].
     * Strips CRLF characters from all forwarded headers and blocks public-to-private redirect hops.
     */
    private fun followRedirects(
        startUrl: String,
        headers: Map<String, String>,
        rangeHeader: String?,
    ): HttpURLConnection? {
        val initialUri = DesktopNetworkGuard.validateHttpUri(startUrl, allowPrivateHosts = true) ?: return null
        var currentUrl = initialUri.toString()
        repeat(MAX_REDIRECTS + 1) { hop ->
            val conn = (URI(currentUrl).toURL().openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                instanceFollowRedirects = false
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("User-Agent", DEFAULT_USER_AGENT)
                for ((name, value) in headers) {
                    val cleanName = DesktopNetworkGuard.sanitizeHeaderValue(name)
                    if (cleanName.isNotEmpty()) {
                        setRequestProperty(cleanName, DesktopNetworkGuard.sanitizeHeaderValue(value))
                    }
                }
                if (!rangeHeader.isNullOrBlank()) {
                    setRequestProperty("Range", DesktopNetworkGuard.sanitizeHeaderValue(rangeHeader))
                }
            }
            val status = conn.responseCode
            if (status in REDIRECT_CODES && hop < MAX_REDIRECTS) {
                val location = conn.getHeaderField("Location")
                conn.disconnect()
                if (location.isNullOrBlank()) return null
                val nextUri = DesktopNetworkGuard.validateRedirectTarget(currentUrl, location) ?: return null
                currentUrl = nextUri.toString()
                return@repeat
            }
            return conn
        }
        return null
    }

    /**
     * Forcibly terminates [process] and waits up to 2 seconds for Windows file handles to close cleanly.
     */
    private fun terminateAndAwait(process: Process) {
        process.destroyForcibly()
        runCatching { process.waitFor(PROCESS_EXIT_WAIT_SECONDS, TimeUnit.SECONDS) }
    }

    /**
     * Extracts stable `(id, itag)` query identity from signed YouTube `googlevideo.com` URLs.
     * Returns `null` for non-YouTube URLs or URLs missing the `id` query parameter.
     */
    private fun extractYouTubeStreamIdentity(mediaUri: String): String? {
        val uri = runCatching { URI(mediaUri) }.getOrNull() ?: return null
        val host = uri.host?.lowercase().orEmpty()
        if (!host.contains("googlevideo.com")) return null
        val query = uri.rawQuery ?: return null
        val params = query.split('&')
            .mapNotNull { pair ->
                val eq = pair.indexOf('=')
                if (eq <= 0) null else pair.substring(0, eq) to pair.substring(eq + 1)
            }
            .toMap()
        val id = params["id"]?.takeIf { it.isNotBlank() } ?: return null
        val itag = params["itag"].orEmpty()
        return "yt:$id:$itag"
    }

    /**
     * Parses total byte count from an HTTP `Content-Range: bytes start-end/total` response header.
     * Returns `null` when absent or when total length is `*`.
     */
    private fun parseTotalFromContentRange(contentRange: String?): Long? {
        if (contentRange.isNullOrBlank()) return null
        return contentRange.substringAfter('/', "")
            .trim()
            .takeIf { it.isNotEmpty() && it.all(Char::isDigit) }
            ?.toLongOrNull()
    }

    /**
     * Converts a `file:` URI string into a local [File] instance, falling back cleanly for malformed URIs.
     * Returns `null` if [fileUri] is blank.
     */
    private fun resolveLocalFile(fileUri: String): File? {
        if (fileUri.isBlank()) return null
        return runCatching { File(URI(fileUri)) }
            .getOrElse { File(fileUri.removePrefix("file://").removePrefix("file:")) }
    }

    /**
     * Starts a daemon thread that drains [stream] continuously so child `ffmpeg` processes never block on full pipes.
     * Ignores read errors once the child process terminates.
     */
    private fun spawnDrainThread(stream: InputStream): Thread {
        return Thread {
            runCatching { stream.use { it.readBytes() } }
        }.apply {
            isDaemon = true
            start()
        }
    }

    /**
     * Moves completed [partFile] into [targetFile] atomically where supported, falling back to overwrite move on Windows.
     * Leaves the existing cache invariant intact even on concurrent reads.
     */
    private fun moveAtomically(partFile: File, targetFile: File) {
        val src = partFile.toPath()
        val dst = targetFile.toPath()
        try {
            Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: Exception) {
            Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private data class ChunkResult(
        val bytesWritten: Long,
        val discoveredTotalBytes: Long,
        val isFullResponse: Boolean,
    )
}
