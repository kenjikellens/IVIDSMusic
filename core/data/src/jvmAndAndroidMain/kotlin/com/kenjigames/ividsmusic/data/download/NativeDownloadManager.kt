package com.kenjigames.ividsmusic.data.download

import com.kenjigames.ividsmusic.data.local.ActiveSaveDirectoryHolder
import com.kenjigames.ividsmusic.data.local.AppStorageDirectories
import com.kenjigames.ividsmusic.data.local.DataStorePreferencesRepository
import com.kenjigames.ividsmusic.data.stream.InMemoryStreamTtlCache
import com.kenjigames.ividsmusic.data.stream.JvmLocalAudioLocator
import com.kenjigames.ividsmusic.data.stream.YouTubeMetadataPreviewer
import com.kenjigames.ividsmusic.data.stream.extractYouTubeVideoId
import com.kenjigames.ividsmusic.domain.model.AppResult
import com.kenjigames.ividsmusic.domain.model.AudioQuality
import com.kenjigames.ividsmusic.domain.model.DomainError
import com.kenjigames.ividsmusic.domain.model.DownloadItem
import com.kenjigames.ividsmusic.domain.model.DownloadState
import com.kenjigames.ividsmusic.domain.model.StreamSource
import com.kenjigames.ividsmusic.domain.model.Track
import com.kenjigames.ividsmusic.domain.port.LibraryRepository
import com.kenjigames.ividsmusic.domain.port.OfflineDownloadManager
import com.kenjigames.ividsmusic.domain.port.StreamResolver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException

/**
 * Outcome returned by [AudioTranscoder] when desktop transcoding converts a `.part` stream cleanly.
 */
data class TranscodeOutcome(
    val outputFile: File,
    val extension: String,
    val effectiveQuality: AudioQuality
)

/**
 * Optional platform audio transcoding hook invoked during offline downloads when target bitrate differs.
 * Desktop uses `ffmpeg` when installed; Android returns `null` to preserve native stream bitrate gracefully.
 */
interface AudioTranscoder {
    /** Returns `true` when an external encoder binary such as `ffmpeg` is available on the host. */
    fun isAvailable(): Boolean

    /** Transcodes [sourcePartFile] to [targetQuality] or returns `null` when skipped or unavailable. */
    suspend fun transcodeIfNeeded(
        sourcePartFile: File,
        sourceExtension: String,
        sourceQuality: AudioQuality,
        targetQuality: AudioQuality
    ): TranscodeOutcome?
}

/**
 * JVM/Android implementation of [OfflineDownloadManager] supporting selectable [AudioQuality], `.part` streaming,
 * atomic file promotion to `.m4a`/`.mp3`, optional Desktop `ffmpeg` transcoding, and `saved/` startup scanning.
 */
class NativeDownloadManager(
    private val directories: AppStorageDirectories,
    private val saveDirectoryHolder: ActiveSaveDirectoryHolder = ActiveSaveDirectoryHolder(directories.savedDir),
    private val streamResolver: StreamResolver,
    private val metadataPreviewer: YouTubeMetadataPreviewer,
    private val libraryRepository: LibraryRepository,
    private val preferencesRepository: DataStorePreferencesRepository,
    private val streamTtlCache: InMemoryStreamTtlCache,
    private val okHttpClient: OkHttpClient,
    private val audioTranscoder: AudioTranscoder,
    private val desktopPreviewFallback: (suspend (String) -> AppResult<List<Track>>)? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clockMs: () -> Long = { System.currentTimeMillis() }
) : OfflineDownloadManager {

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val activeCalls = ConcurrentHashMap<String, Call>()

    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())

    /** Observable list of queued, downloading, transcoding, completed, and failed download entries. */
    override val downloads: StateFlow<List<DownloadItem>> = _downloads.asStateFlow()

    /** Observable path to the active offline `saved/` destination directory. */
    override val downloadDirectoryPath: StateFlow<String> = saveDirectoryHolder.path

    /**
     * Loads track metadata previews for a YouTube video or playlist URL using NewPipe with Desktop yt-dlp fallback.
     */
    override suspend fun previewUrl(youtubeUrl: String): AppResult<List<Track>> {
        val trimmed = youtubeUrl.trim()
        if (trimmed.isEmpty()) {
            return AppResult.Failure(DomainError.Validation("YouTube URL cannot be empty"))
        }
        val primary = metadataPreviewer.previewUrl(trimmed)
        if (primary is AppResult.Success && primary.data.isNotEmpty()) {
            return primary
        }
        val fallback = desktopPreviewFallback?.invoke(trimmed)
        return if (fallback is AppResult.Success && fallback.data.isNotEmpty()) fallback else primary
    }

    /**
     * Enqueues [track] for offline download at [quality] and starts asynchronous `.part` streaming.
     */
    override suspend fun enqueue(track: Track, quality: AudioQuality): AppResult<String> {
        if (track.id.isBlank() || track.title.isBlank()) {
            return AppResult.Failure(DomainError.Validation("Track must have non-blank id and title"))
        }
        val itemId = UUID.randomUUID().toString()
        val item = DownloadItem(
            id = itemId,
            track = track,
            requestedQuality = quality,
            state = DownloadState.Queued(track, quality),
            createdAtMs = clockMs()
        )
        _downloads.update { current -> listOf(item) + current }
        launchDownloadJob(itemId, track, quality)
        return AppResult.Success(itemId)
    }

    /**
     * Enqueues a raw [youtubeUrl] download at [quality], extracting video ID and metadata safely.
     */
    override suspend fun enqueueUrl(
        youtubeUrl: String,
        quality: AudioQuality,
        customTitle: String?
    ): AppResult<String> {
        val videoId = extractYouTubeVideoId(youtubeUrl)
            ?: return AppResult.Failure(DomainError.Validation("Invalid YouTube URL or video ID: $youtubeUrl"))

        val previewTrack = (previewUrl(youtubeUrl) as? AppResult.Success)?.data?.firstOrNull()
        val effectiveTrack = Track(
            id = previewTrack?.id ?: "yt_$videoId",
            title = customTitle?.takeIf { it.isNotBlank() } ?: previewTrack?.title ?: "YouTube Track $videoId",
            artist = previewTrack?.artist?.ifBlank { "YouTube" } ?: "YouTube",
            coverUrl = previewTrack?.coverUrl.orEmpty(),
            durationSeconds = previewTrack?.durationSeconds ?: 0,
            videoId = videoId
        )
        return enqueue(effectiveTrack, quality)
    }

    /**
     * Cancels an active or queued download matching [downloadId], aborting HTTP calls and cleaning `.part` files.
     */
    override suspend fun cancel(downloadId: String) {
        activeCalls.remove(downloadId)?.cancel()
        activeJobs.remove(downloadId)?.cancel()
        updateItemState(downloadId, DownloadState.Cancelled)
    }

    /**
     * Removes an offline audio file on disk for [trackId] and clears its downloaded state in SQLite.
     */
    override suspend fun deleteOfflineFile(trackId: String): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            val persisted = libraryRepository.getTrack(trackId)
            val path = persisted?.localFilePath
            if (!path.isNullOrBlank()) {
                val file = File(path)
                if (file.exists() && file.isFile) {
                    file.delete()
                }
            }
            libraryRepository.removeDownloadedTrack(trackId)
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (error: Exception) {
            AppResult.Failure(DomainError.Storage(error.message ?: "Failed to delete offline file"))
        }
    }

    /**
     * Scans the active `saved/` directory for `.mp3`/`.m4a` files, registers unrecognized tracks, and prunes missing files.
     */
    override suspend fun scanSavedDirectory(): AppResult<List<Track>> = withContext(ioDispatcher) {
        try {
            val savedDir = resolveActiveSavedDir()
            if (!savedDir.exists()) {
                savedDir.mkdirs()
            }

            val persistedDownloads = libraryRepository.observeDownloadedTracks().first()
            for (existing in persistedDownloads) {
                val path = existing.localFilePath
                val file = path?.let(::File)
                if (file == null || !file.exists() || !file.isFile || file.length() <= 0L) {
                    libraryRepository.removeDownloadedTrack(existing.id)
                }
            }

            val audioFiles = savedDir.listFiles()
                ?.filter { it.isFile && it.length() > 0L && isSupportedAudioFile(it.name) }
                .orEmpty()

            val currentAfterPrune = libraryRepository.observeDownloadedTracks().first()
            val knownPaths = currentAfterPrune.mapNotNull { it.localFilePath }.toSet()

            for (file in audioFiles) {
                if (file.absolutePath in knownPaths) continue
                val (artist, title) = parseArtistAndTitle(file.nameWithoutExtension)
                val matchingRow = currentAfterPrune.firstOrNull { row ->
                    row.artist.equals(artist, ignoreCase = true) && row.title.equals(title, ignoreCase = true)
                }
                val track = matchingRow ?: Track(
                    id = "local_${sha256Short(file.name)}",
                    title = title,
                    artist = artist,
                    isDownloaded = true,
                    localFilePath = file.absolutePath,
                    bitrateKbps = AudioQuality.Q196.kbps,
                    addedTimestampMs = file.lastModified().takeIf { it > 0L } ?: clockMs()
                )
                libraryRepository.markTrackDownloaded(
                    track = track,
                    filePath = file.absolutePath,
                    quality = AudioQuality.fromKbps(track.bitrateKbps ?: AudioQuality.Q196.kbps)
                )
            }

            AppResult.Success(libraryRepository.observeDownloadedTracks().first())
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (error: Exception) {
            AppResult.Failure(DomainError.Storage(error.message ?: "Failed to scan saved directory"))
        }
    }

    /**
     * Sums total byte size of all `.mp3` and `.m4a` offline files stored in the active saved directory.
     */
    override suspend fun getSavedStorageBytes(): Long = withContext(ioDispatcher) {
        val dir = resolveActiveSavedDir()
        if (!dir.exists() || !dir.isDirectory) return@withContext 0L
        dir.listFiles()
            ?.filter { it.isFile && isSupportedAudioFile(it.name) }
            ?.sumOf { it.length() }
            ?: 0L
    }

    /**
     * Validates that [path] is writable, persists it as the custom save folder, updates [downloadDirectoryPath], and rescans.
     */
    override suspend fun setCustomSaveDirectory(path: String): AppResult<Unit> = withContext(ioDispatcher) {
        val trimmed = path.trim()
        if (trimmed.isEmpty()) {
            return@withContext AppResult.Failure(DomainError.Validation("Save directory path cannot be empty"))
        }
        val targetDir = File(trimmed)
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            return@withContext AppResult.Failure(DomainError.Storage("Cannot create save directory: $trimmed"))
        }
        if (!targetDir.isDirectory) {
            return@withContext AppResult.Failure(DomainError.Validation("Path is not a directory: $trimmed"))
        }
        val probe = File(targetDir, ".ivids_write_test_${clockMs()}")
        val writable = try {
            probe.writeText("ok")
            probe.delete()
            true
        } catch (_: Exception) {
            false
        }
        if (!writable) {
            return@withContext AppResult.Failure(DomainError.Storage("Save directory is not writable: $trimmed"))
        }

        val persistRes = preferencesRepository.setCustomSaveDirectory(targetDir.absolutePath)
        if (persistRes is AppResult.Failure) return@withContext persistRes

        saveDirectoryHolder.update(targetDir.absolutePath)
        scanSavedDirectory()
        AppResult.Success(Unit)
    }

    /**
     * Deletes temporary cached files under [AppStorageDirectories.cacheDir] and flushes [InMemoryStreamTtlCache].
     */
    override suspend fun clearAudioCache(): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            streamTtlCache.clear()
            val cacheRoot = File(directories.cacheDir)
            if (cacheRoot.exists() && cacheRoot.isDirectory) {
                cacheRoot.listFiles()?.forEach { child ->
                    deleteRecursivelySafe(child)
                }
            }
            AppResult.Success(Unit)
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (error: Exception) {
            AppResult.Failure(DomainError.Storage(error.message ?: "Failed to clear audio cache"))
        }
    }

    /**
     * Returns the current offline saved directory path synchronously.
     */
    override fun getSavedDirectoryPath(): String = saveDirectoryHolder.current()

    private fun launchDownloadJob(
        downloadId: String,
        track: Track,
        requestedQuality: AudioQuality
    ) {
        val job = scope.launch {
            val savedDir = resolveActiveSavedDir()
            if (!savedDir.exists()) savedDir.mkdirs()

            val baseName = "${JvmLocalAudioLocator.sanitizeFilenamePart(track.artist)} - " +
                JvmLocalAudioLocator.sanitizeFilenamePart(track.title)
            val partFile = File(savedDir, "$baseName.part")
            var transcodedFile: File? = null

            try {
                updateItemState(downloadId, DownloadState.Resolving(track, requestedQuality))
                val remoteOnlyTrack = track.copy(localFilePath = null, isDownloaded = false)
                val resolvedResult = streamResolver.resolve(remoteOnlyTrack, requestedQuality)
                if (resolvedResult is AppResult.Failure) {
                    updateItemState(downloadId, DownloadState.Failed(resolvedResult.error))
                    return@launch
                }

                val source = (resolvedResult as AppResult.Success).data
                val remoteSource = when (source) {
                    is StreamSource.RemoteUrl -> source
                    is StreamSource.LocalFile -> {
                        updateItemState(
                            downloadId,
                            DownloadState.Completed(source.filePath, source.quality)
                        )
                        return@launch
                    }
                }

                val sourceExtension = if (remoteSource.mimeType.contains("mpeg", ignoreCase = true) ||
                    remoteSource.mimeType.contains("mp3", ignoreCase = true)
                ) {
                    "mp3"
                } else {
                    "m4a"
                }

                streamToPartFile(downloadId, remoteSource, partFile)

                var finalExtension = sourceExtension
                var effectiveQuality = remoteSource.quality
                var candidateOutputFile = partFile

                if (audioTranscoder.isAvailable() && remoteSource.quality != requestedQuality) {
                    updateItemState(downloadId, DownloadState.Transcoding(track, requestedQuality))
                    val transcoded = audioTranscoder.transcodeIfNeeded(
                        sourcePartFile = partFile,
                        sourceExtension = sourceExtension,
                        sourceQuality = remoteSource.quality,
                        targetQuality = requestedQuality
                    )
                    if (transcoded != null && transcoded.outputFile.exists() && transcoded.outputFile.length() > 0L) {
                        transcodedFile = transcoded.outputFile
                        candidateOutputFile = transcoded.outputFile
                        finalExtension = transcoded.extension
                        effectiveQuality = transcoded.effectiveQuality
                    }
                }

                val finalFile = File(savedDir, "$baseName.$finalExtension")
                moveAtomically(candidateOutputFile, finalFile)
                if (candidateOutputFile != partFile && partFile.exists()) {
                    partFile.delete()
                }

                libraryRepository.markTrackDownloaded(
                    track = track,
                    filePath = finalFile.absolutePath,
                    quality = effectiveQuality
                )
                updateItemState(
                    downloadId,
                    DownloadState.Completed(finalFile.absolutePath, effectiveQuality)
                )
            } catch (cancel: CancellationException) {
                updateItemState(downloadId, DownloadState.Cancelled)
                throw cancel
            } catch (error: Exception) {
                val domainError = if (error is DownloadHttpException) {
                    DomainError.Network(error.message ?: "Download HTTP ${error.statusCode}", error.statusCode)
                } else {
                    DomainError.Storage(error.message ?: "Download failed")
                }
                updateItemState(downloadId, DownloadState.Failed(domainError))
            } finally {
                activeCalls.remove(downloadId)
                activeJobs.remove(downloadId)
                if (partFile.exists()) partFile.delete()
                if (transcodedFile?.exists() == true) transcodedFile.delete()
            }
        }
        activeJobs[downloadId] = job
    }

    private suspend fun streamToPartFile(
        downloadId: String,
        source: StreamSource.RemoteUrl,
        partFile: File
    ) = withContext(ioDispatcher) {
        val reqBuilder = Request.Builder().url(source.url)
        for ((k, v) in source.headers) {
            reqBuilder.addHeader(k, v)
        }
        val call = okHttpClient.newCall(reqBuilder.build())
        activeCalls[downloadId] = call

        call.execute().use { response ->
            if (!response.isSuccessful) {
                throw DownloadHttpException(response.code, "HTTP ${response.code} downloading audio stream")
            }
            val body = response.body ?: throw IllegalStateException("Empty HTTP body")
            val totalBytes = body.contentLength().takeIf { it > 0L }
            var downloadedBytes = 0L
            var windowBytes = 0L
            var windowStartMs = clockMs()
            var lastEmitMs = 0L
            val buffer = ByteArray(DEFAULT_BUFFER_BYTES)

            body.byteStream().use { input ->
                FileOutputStream(partFile, false).use { output ->
                    while (true) {
                        ensureActive()
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloadedBytes += read
                        windowBytes += read

                        val now = clockMs()
                        val elapsedWindowMs = (now - windowStartMs).coerceAtLeast(1L)
                        if (now - lastEmitMs >= PROGRESS_EMIT_INTERVAL_MS || (totalBytes != null && downloadedBytes >= totalBytes)) {
                            val bytesPerSec = (windowBytes * 1_000L) / elapsedWindowMs
                            val progress = if (totalBytes != null) {
                                (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                            } else {
                                0f
                            }
                            updateItemState(
                                downloadId,
                                DownloadState.Downloading(
                                    progress = progress,
                                    bytesPerSec = bytesPerSec,
                                    downloadedBytes = downloadedBytes,
                                    totalBytes = totalBytes
                                )
                            )
                            lastEmitMs = now
                            if (elapsedWindowMs >= SPEED_WINDOW_RESET_MS) {
                                windowBytes = 0L
                                windowStartMs = now
                            }
                        }
                    }
                    output.flush()
                }
            }

            if (!partFile.exists() || partFile.length() <= 0L) {
                throw IllegalStateException("Downloaded audio file is empty")
            }
        }
    }

    private fun moveAtomically(source: File, target: File) {
        val sourcePath = source.toPath()
        val targetPath = target.toPath()
        try {
            Files.move(
                sourcePath,
                targetPath,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private suspend fun resolveActiveSavedDir(): File {
        val custom = preferencesRepository.getCustomSaveDirectory()
        val effectivePath = if (!custom.isNullOrBlank()) custom else directories.savedDir
        if (saveDirectoryHolder.current() != effectivePath) {
            saveDirectoryHolder.update(effectivePath)
        }
        return File(effectivePath)
    }

    private fun updateItemState(downloadId: String, nextState: DownloadState) {
        _downloads.update { list ->
            list.map { item ->
                if (item.id == downloadId) item.copy(state = nextState) else item
            }
        }
    }

    private fun isSupportedAudioFile(filename: String): Boolean {
        val lower = filename.lowercase()
        return lower.endsWith(".m4a") || lower.endsWith(".mp3")
    }

    private fun parseArtistAndTitle(nameWithoutExtension: String): Pair<String, String> {
        val parts = nameWithoutExtension.split(" - ", limit = 2)
        return if (parts.size == 2) {
            parts[0].trim().ifEmpty { "Unknown Artist" } to parts[1].trim().ifEmpty { nameWithoutExtension }
        } else {
            "Unknown Artist" to nameWithoutExtension.trim().ifEmpty { "Unknown Title" }
        }
    }

    private fun sha256Short(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return digest.take(8).joinToString("") { byte -> "%02x".format(byte) }
    }

    private fun deleteRecursivelySafe(target: File) {
        if (target.isDirectory) {
            target.listFiles()?.forEach(::deleteRecursivelySafe)
        }
        target.delete()
    }

    private class DownloadHttpException(val statusCode: Int, message: String) : Exception(message)

    companion object {
        private const val DEFAULT_BUFFER_BYTES = 16 * 1024
        private const val PROGRESS_EMIT_INTERVAL_MS = 120L
        private const val SPEED_WINDOW_RESET_MS = 1_000L
    }
}
