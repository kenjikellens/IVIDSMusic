package com.kenjigames.ividsmusic.data.stream

import com.kenjigames.ividsmusic.domain.model.AppResult
import com.kenjigames.ividsmusic.domain.model.AudioQuality
import com.kenjigames.ividsmusic.domain.model.DomainError
import com.kenjigames.ividsmusic.domain.model.StreamSource
import com.kenjigames.ividsmusic.domain.model.Track
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

/**
 * Windows Desktop Tier-4b [PlatformStreamFallback] invoking local `yt-dlp.exe` with a strict 10-second timeout.
 * Guarantees deterministic process termination via `destroyForcibly()` on timeout, error, or coroutine cancellation.
 */
class YtDlpDesktopResolver(
    private val bundledToolsDir: String?,
    private val json: Json,
    private val videoUrlLookup: (suspend (Track) -> String?)? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clockMs: () -> Long = { System.currentTimeMillis() },
    private val timeoutSeconds: Long = DEFAULT_TIMEOUT_SECONDS
) : PlatformStreamFallback {

    /**
     * Resolves a progressive MP4/M4A stream URL via `yt-dlp.exe` using `track.videoId`, fast NewPipe watch-URL lookup, or `ytsearch1:`.
     */
    override suspend fun resolveTrack(
        track: Track,
        preferredQuality: AudioQuality
    ): AppResult<StreamSource.RemoteUrl> {
        val knownWatchUrl = track.videoId?.takeIf { it.isNotBlank() }?.let { "https://www.youtube.com/watch?v=$it" }
            ?: videoUrlLookup?.invoke(track)
        val resolvedVideoId = track.videoId?.takeIf { it.isNotBlank() }
            ?: knownWatchUrl?.let(::extractYouTubeVideoId)
        val target = knownWatchUrl ?: "ytsearch1:${buildYouTubeSearchQuery(track)}"
        return executeYtDlpUrlLookup(target, resolvedVideoId, preferredQuality)
    }

    /**
     * Resolves a direct M4A stream URL from a YouTube URL or video ID via `yt-dlp.exe`.
     */
    override suspend fun resolveDirectUrl(
        videoUrlOrId: String,
        preferredQuality: AudioQuality
    ): AppResult<StreamSource.RemoteUrl> {
        val videoId = extractYouTubeVideoId(videoUrlOrId)
            ?: return AppResult.Failure(DomainError.Validation("Invalid YouTube URL or ID: $videoUrlOrId"))
        return executeYtDlpUrlLookup("https://www.youtube.com/watch?v=$videoId", videoId, preferredQuality)
    }

    /**
     * Queries flat JSON metadata lines via `yt-dlp.exe --flat-playlist --dump-json` as a preview fallback on Desktop.
     */
    suspend fun previewPlaylistOrVideo(youtubeUrl: String): AppResult<List<Track>> = withContext(ioDispatcher) {
        val executable = findYtDlpExecutable()
            ?: return@withContext AppResult.Failure(DomainError.ProcessFailure("yt-dlp.exe not found on Desktop"))

        val command = listOf(
            executable,
            "--flat-playlist",
            "--dump-json",
            "--no-warnings",
            youtubeUrl.trim()
        )
        when (val result = runProcess(command, timeoutSeconds)) {
            is ProcessOutcome.Success -> {
                val tracks = result.stdout.lineSequence()
                    .map { it.trim() }
                    .filter { it.startsWith("{") }
                    .mapNotNull { line ->
                        runCatching { json.decodeFromString<YtDlpDumpItemDto>(line) }.getOrNull()
                    }
                    .filter { !it.id.isNullOrBlank() && !it.title.isNullOrBlank() }
                    .map { item ->
                        Track(
                            id = "yt_${item.id}",
                            title = item.title.orEmpty(),
                            artist = item.channel ?: item.uploader ?: "YouTube",
                            coverUrl = item.thumbnail.orEmpty(),
                            durationSeconds = item.duration?.toInt()?.coerceAtLeast(0) ?: 0,
                            videoId = item.id
                        )
                    }
                    .toList()
                if (tracks.isEmpty()) {
                    AppResult.Failure(DomainError.StreamNotFound("yt-dlp returned no metadata for $youtubeUrl"))
                } else {
                    AppResult.Success(tracks)
                }
            }
            is ProcessOutcome.Failure -> AppResult.Failure(result.error)
        }
    }

    /**
     * Executes `yt-dlp.exe` to extract a direct audio stream URL for [target] and resolves its metadata.
     * Enforces [timeoutSeconds] and prioritizes compact M4A audio streams before progressive MP4 fallbacks.
     */
    private suspend fun executeYtDlpUrlLookup(
        target: String,
        videoId: String?,
        preferredQuality: AudioQuality
    ): AppResult<StreamSource.RemoteUrl> = withContext(ioDispatcher) {
        val executable = findYtDlpExecutable()
            ?: return@withContext AppResult.Failure(DomainError.ProcessFailure("yt-dlp.exe not found"))

        val command = listOf(
            executable,
            "--no-playlist",
            "--no-warnings",
            "-f",
            "bestaudio[ext=m4a]/140/bestaudio/18/best",
            "-g",
            target
        )
        when (val outcome = runProcess(command, timeoutSeconds)) {
            is ProcessOutcome.Success -> {
                val resolvedUrl = outcome.stdout.lineSequence()
                    .map { it.trim() }
                    .firstOrNull { it.startsWith("http://") || it.startsWith("https://") }
                if (resolvedUrl == null) {
                    AppResult.Failure(DomainError.StreamNotFound("yt-dlp returned no playable URL"))
                } else {
                    AppResult.Success(
                        StreamSource.RemoteUrl(
                            url = resolvedUrl,
                            mimeType = "audio/mp4",
                            videoId = videoId,
                            quality = preferredQuality,
                            resolvedAtMs = clockMs()
                        )
                    )
                }
            }
            is ProcessOutcome.Failure -> AppResult.Failure(outcome.error)
        }
    }

    private fun runProcess(command: List<String>, waitSeconds: Long): ProcessOutcome {
        var process: Process? = null
        return try {
            process = ProcessBuilder(command)
                .redirectErrorStream(false)
                .start()

            // Read stdout and stderr before/while waiting so pipe buffers never deadlock
            var stdoutText = ""
            var stderrText = ""
            val outThread = Thread {
                stdoutText = process.inputStream.bufferedReader().use { it.readText() }
            }.apply { isDaemon = true; start() }
            val errThread = Thread {
                stderrText = process.errorStream.bufferedReader().use { it.readText() }
            }.apply { isDaemon = true; start() }

            val finished = process.waitFor(waitSeconds, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                return ProcessOutcome.Failure(
                    DomainError.ProcessFailure("yt-dlp timed out after ${waitSeconds}s")
                )
            }

            outThread.join(1_000L)
            errThread.join(1_000L)
            val exitCode = process.exitValue()
            if (exitCode != 0) {
                ProcessOutcome.Failure(
                    DomainError.ProcessFailure(
                        stderrText.trim().ifEmpty { "yt-dlp exited with code $exitCode" },
                        exitCode = exitCode
                    )
                )
            } else {
                ProcessOutcome.Success(stdoutText)
            }
        } catch (cancel: CancellationException) {
            process?.destroyForcibly()
            throw cancel
        } catch (error: Exception) {
            process?.destroyForcibly()
            ProcessOutcome.Failure(
                DomainError.ProcessFailure(error.message ?: "Failed to execute yt-dlp")
            )
        } finally {
            if (process?.isAlive == true) {
                process.destroyForcibly()
            }
        }
    }

    /** Resolves `yt-dlp.exe` from the configured tools folder, working/parent resource directories, or system PATH. */
    internal fun findYtDlpExecutable(): String? {
        val candidates = buildList {
            if (!bundledToolsDir.isNullOrBlank()) {
                add(File(bundledToolsDir, "yt-dlp.exe"))
                add(File(bundledToolsDir, "yt-dlp"))
            }
            val appResourcesDir = System.getProperty("compose.application.resources.dir")
            if (!appResourcesDir.isNullOrBlank()) {
                add(File(appResourcesDir, "yt-dlp.exe"))
            }
            val userDir = System.getProperty("user.dir")
            if (!userDir.isNullOrBlank()) {
                val current = File(userDir)
                add(File(current, "yt-dlp.exe"))
                add(File(current, "resources/windows/yt-dlp.exe"))
                add(File(current, "composeApp/resources/windows/yt-dlp.exe"))
                current.parentFile?.let { parent ->
                    add(File(parent, "yt-dlp.exe"))
                    add(File(parent, "composeApp/resources/windows/yt-dlp.exe"))
                }
            }
        }
        val direct = candidates.firstOrNull { it.exists() && it.isFile }
        if (direct != null) return direct.absolutePath

        val pathEnv = System.getenv("PATH").orEmpty()
        val pathSeparator = File.pathSeparatorChar
        for (dir in pathEnv.split(pathSeparator)) {
            if (dir.isBlank()) continue
            val exe = File(dir.trim(), "yt-dlp.exe")
            if (exe.exists() && exe.isFile) return exe.absolutePath
            val unixBin = File(dir.trim(), "yt-dlp")
            if (unixBin.exists() && unixBin.isFile) return unixBin.absolutePath
        }
        return null
    }

    private sealed interface ProcessOutcome {
        data class Success(val stdout: String) : ProcessOutcome
        data class Failure(val error: DomainError) : ProcessOutcome
    }

    @Serializable
    private data class YtDlpDumpItemDto(
        val id: String? = null,
        val title: String? = null,
        val channel: String? = null,
        val uploader: String? = null,
        val duration: Double? = null,
        val thumbnail: String? = null
    )

    companion object {
        const val DEFAULT_TIMEOUT_SECONDS = 10L
    }
}
