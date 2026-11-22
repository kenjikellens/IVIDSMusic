package com.kenjigames.ividsmusic.data.download

import com.kenjigames.ividsmusic.domain.model.AudioQuality
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

/**
 * Windows Desktop [AudioTranscoder] invoking local or PATH-installed `ffmpeg.exe` when bitrate conversion is needed.
 * Enforces deterministic timeout termination via `destroyForcibly()` and returns `null` when `ffmpeg` is unavailable.
 */
class DesktopFfmpegTranscoder(
    private val bundledToolsDir: String?,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val timeoutSeconds: Long = DEFAULT_TIMEOUT_SECONDS
) : AudioTranscoder {

    /** Returns `true` if `ffmpeg.exe` is found locally, in WinGet Links, or on the system PATH. */
    override fun isAvailable(): Boolean = findFfmpegExecutable() != null

    /**
     * Transcodes [sourcePartFile] to `.mp3` at [targetQuality] when `ffmpeg` exists and bitrate differs from [sourceQuality].
     * Returns `null` cleanly if `ffmpeg` is absent or conversion fails, allowing fallback to the raw stream.
     */
    override suspend fun transcodeIfNeeded(
        sourcePartFile: File,
        sourceExtension: String,
        sourceQuality: AudioQuality,
        targetQuality: AudioQuality
    ): TranscodeOutcome? = withContext(ioDispatcher) {
        if (sourceQuality == targetQuality && sourceExtension == "m4a") {
            return@withContext null
        }
        val ffmpeg = findFfmpegExecutable() ?: return@withContext null
        val outputPart = File(sourcePartFile.parentFile, "${sourcePartFile.name}.mp3.part")
        if (outputPart.exists()) outputPart.delete()

        val command = listOf(
            ffmpeg,
            "-y",
            "-loglevel",
            "error",
            "-i",
            sourcePartFile.absolutePath,
            "-vn",
            "-c:a",
            "libmp3lame",
            "-b:a",
            "${targetQuality.kbps}k",
            "-f",
            "mp3",
            outputPart.absolutePath
        )

        var process: Process? = null
        try {
            process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()
            val drainThread = Thread {
                process.inputStream.bufferedReader().use { it.readText() }
            }.apply { isDaemon = true; start() }

            val finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                outputPart.delete()
                return@withContext null
            }
            drainThread.join(1_000L)
            if (process.exitValue() == 0 && outputPart.exists() && outputPart.length() > 0L) {
                TranscodeOutcome(
                    outputFile = outputPart,
                    extension = "mp3",
                    effectiveQuality = targetQuality
                )
            } else {
                outputPart.delete()
                null
            }
        } catch (cancel: CancellationException) {
            process?.destroyForcibly()
            outputPart.delete()
            throw cancel
        } catch (_: Exception) {
            process?.destroyForcibly()
            outputPart.delete()
            null
        } finally {
            if (process?.isAlive == true) {
                process.destroyForcibly()
            }
        }
    }

    /** Locates `ffmpeg.exe` from tools folder, working directory, WinGet links, or PATH environment variable. */
    internal fun findFfmpegExecutable(): String? {
        val candidates = mutableListOf<File>()
        if (!bundledToolsDir.isNullOrBlank()) {
            candidates.add(File(bundledToolsDir, "ffmpeg.exe"))
            candidates.add(File(bundledToolsDir, "ffmpeg"))
        }
        System.getProperty("user.dir")?.takeIf { it.isNotBlank() }?.let { dir ->
            candidates.add(File(dir, "ffmpeg.exe"))
        }
        System.getenv("LOCALAPPDATA")?.takeIf { it.isNotBlank() }?.let { localAppData ->
            candidates.add(File(localAppData, "Microsoft/WinGet/Links/ffmpeg.exe"))
        }
        val direct = candidates.firstOrNull { it.exists() && it.isFile }
        if (direct != null) return direct.absolutePath

        val pathEnv = System.getenv("PATH").orEmpty()
        for (dir in pathEnv.split(File.pathSeparatorChar)) {
            if (dir.isBlank()) continue
            val exe = File(dir.trim(), "ffmpeg.exe")
            if (exe.exists() && exe.isFile) return exe.absolutePath
            val bin = File(dir.trim(), "ffmpeg")
            if (bin.exists() && bin.isFile) return bin.absolutePath
        }
        return null
    }

    companion object {
        private const val DEFAULT_TIMEOUT_SECONDS = 90L
    }
}
