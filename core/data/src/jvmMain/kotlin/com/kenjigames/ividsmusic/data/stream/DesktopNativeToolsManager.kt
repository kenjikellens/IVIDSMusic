package com.kenjigames.ividsmusic.data.stream

import com.kenjigames.ividsmusic.data.local.AppStorageDirectories
import com.kenjigames.ividsmusic.data.local.DataStorePreferencesRepository
import com.kenjigames.ividsmusic.domain.model.UpdateMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

/**
 * Manages user-writable `yt-dlp.exe` updates (weekly check) and portable `ffmpeg.exe` provisioning on Windows.
 * Writes tools atomically under `%LOCALAPPDATA%\IVIDSMusic\tools\` so Program Files permissions never block updates.
 */
class DesktopNativeToolsManager(
    private val directories: AppStorageDirectories,
    private val preferencesRepository: DataStorePreferencesRepository,
    private val okHttpClient: OkHttpClient,
    private val json: Json,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clockMs: () -> Long = { System.currentTimeMillis() },
) {

    /**
     * Resolves and ensures existence of the user-writable desktop tools folder (`.../IVIDSMusic/tools`).
     * Uses the parent of [AppStorageDirectories.filesDir] so cache clears never delete managed executables.
     */
    fun resolveManagedToolsDir(): File {
        val dataParent = File(directories.filesDir).parentFile ?: File(directories.filesDir)
        return File(dataParent, "tools").apply { mkdirs() }
    }

    /**
     * Returns the path to the managed user-writable `yt-dlp.exe` if present and non-empty, or `null`.
     */
    fun findManagedYtDlp(): String? =
        File(resolveManagedToolsDir(), "yt-dlp.exe")
            .takeIf { it.isFile && it.length() > MIN_EXECUTABLE_BYTES }
            ?.absolutePath

    /**
     * Returns the path to the managed user-writable `ffmpeg.exe` if present and non-empty, or `null`.
     */
    fun findManagedFfmpeg(): String? =
        File(resolveManagedToolsDir(), "ffmpeg.exe")
            .takeIf { it.isFile && it.length() > MIN_EXECUTABLE_BYTES }
            ?.absolutePath

    /**
     * Performs silent background tool readiness checks: provisions portable `ffmpeg.exe` when completely absent
     * and checks weekly for newer `yt-dlp.exe` releases unless [UpdateMode.OFF] is active.
     */
    suspend fun ensureToolsReady(
        ytDlpCurrentPath: String?,
        ffmpegCurrentPath: String?,
    ) = withContext(ioDispatcher) {
        val toolsDir = resolveManagedToolsDir()
        if (ffmpegCurrentPath.isNullOrBlank() && findManagedFfmpeg() == null) {
            runCatching { provisionPortableFfmpeg(toolsDir) }
        }
        val prefs = preferencesRepository.preferences.first()
        if (prefs.updateMode == UpdateMode.OFF) return@withContext
        val now = clockMs()
        val lastCheck = preferencesRepository.getLastYtDlpCheckMs()
        if (now - lastCheck < YTDLP_CHECK_INTERVAL_MS) {
            return@withContext
        }
        runCatching { checkAndUpdateYtDlp(toolsDir, ytDlpCurrentPath) }
        preferencesRepository.setLastYtDlpCheckMs(now)
    }

    /**
     * Queries GitHub's latest `yt-dlp` release and atomically downloads `yt-dlp.exe` when local version lags behind.
     */
    private suspend fun checkAndUpdateYtDlp(toolsDir: File, currentPath: String?) {
        val currentVersion = currentPath?.let(::readToolVersionLine).orEmpty()
        val releaseJson = executeValidatedGetText(YTDLP_LATEST_RELEASE_API) ?: return
        val release = runCatching { json.decodeFromString<GitHubReleaseDto>(releaseJson) }.getOrNull() ?: return
        val latestTag = release.tag_name.trim()
        if (latestTag.isEmpty() || latestTag == currentVersion) return
        val assetUrl = release.assets
            .firstOrNull { it.name.equals("yt-dlp.exe", ignoreCase = true) }
            ?.browser_download_url ?: return
        if (NetworkSecurityGuard.validateHttpUri(assetUrl, allowPrivateHosts = false) == null) return

        val targetExe = File(toolsDir, "yt-dlp.exe")
        val partExe = File(toolsDir, "yt-dlp.exe.part")
        if (downloadToFile(assetUrl, partExe) && partExe.length() > MIN_EXECUTABLE_BYTES) {
            moveAtomically(partExe, targetExe)
        } else {
            partExe.delete()
        }
    }

    /**
     * Downloads an official portable Windows `ffmpeg.exe` archive and safely extracts only `ffmpeg.exe`.
     */
    private suspend fun provisionPortableFfmpeg(toolsDir: File) {
        if (NetworkSecurityGuard.validateHttpUri(PORTABLE_FFMPEG_ZIP_URL, allowPrivateHosts = false) == null) return
        val zipPart = File(toolsDir, "ffmpeg-portable.zip.part")
        val exeTarget = File(toolsDir, "ffmpeg.exe")
        try {
            if (!downloadToFile(PORTABLE_FFMPEG_ZIP_URL, zipPart)) return
            val extracted = extractFfmpegFromZip(zipPart, exeTarget)
            if (!extracted && exeTarget.exists() && exeTarget.length() < MIN_EXECUTABLE_BYTES) {
                exeTarget.delete()
            }
        } finally {
            if (zipPart.exists()) zipPart.delete()
        }
    }

    /**
     * Extracts exclusively the single `ffmpeg.exe` binary from [zipFile] into [destExecutable] without Zip Slip risk.
     * Never writes arbitrary zip directory trees; writes via a `.part` staging file and verifies non-empty output.
     */
    internal fun extractFfmpegFromZip(zipFile: File, destExecutable: File): Boolean {
        if (!zipFile.exists() || !zipFile.isFile) return false
        val stagingFile = File(destExecutable.parentFile, "${destExecutable.name}.part")
        if (stagingFile.exists()) stagingFile.delete()
        try {
            ZipInputStream(zipFile.inputStream().buffered()).use { zis ->
                while (true) {
                    val entry = zis.nextEntry ?: break
                    val entryName = entry.name.replace('\\', '/')
                    if (!entry.isDirectory &&
                        !entryName.contains("..") &&
                        (entryName.equals("ffmpeg.exe", ignoreCase = true) ||
                            entryName.endsWith("/ffmpeg.exe", ignoreCase = true))
                    ) {
                        FileOutputStream(stagingFile, false).use { out ->
                            val buffer = ByteArray(16_384)
                            while (true) {
                                val read = zis.read(buffer)
                                if (read < 0) break
                                out.write(buffer, 0, read)
                            }
                            out.flush()
                        }
                        if (stagingFile.exists() && stagingFile.length() > 0L) {
                            moveAtomically(stagingFile, destExecutable)
                            return true
                        }
                    }
                    zis.closeEntry()
                }
            }
            return false
        } catch (cancel: CancellationException) {
            stagingFile.delete()
            throw cancel
        } catch (_: Exception) {
            stagingFile.delete()
            return false
        }
    }

    private fun executeValidatedGetText(url: String): String? {
        return executeValidatedRequest(url) { resp ->
            val len = resp.body?.contentLength() ?: -1L
            if (len > MAX_API_RESPONSE_BYTES) return@executeValidatedRequest null
            resp.body?.string()?.takeIf { it.length <= MAX_API_RESPONSE_BYTES }
        }
    }

    private fun downloadToFile(url: String, destination: File): Boolean {
        destination.parentFile?.mkdirs()
        return executeValidatedRequest(url) { resp ->
            val body = resp.body ?: return@executeValidatedRequest false
            val declaredLength = body.contentLength()
            if (declaredLength > MAX_TOOL_DOWNLOAD_BYTES) return@executeValidatedRequest false
            var totalRead = 0L
            val buffer = ByteArray(16_384)
            body.byteStream().use { input ->
                FileOutputStream(destination, false).use { out ->
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        totalRead += read
                        if (totalRead > MAX_TOOL_DOWNLOAD_BYTES) {
                            return@executeValidatedRequest false
                        }
                        out.write(buffer, 0, read)
                    }
                    out.flush()
                }
            }
            true
        } ?: run {
            destination.delete()
            false
        }
    }

    /**
     * Executes an HTTP(S) `GET` manually validating every redirect hop via [NetworkSecurityGuard] to block SSRF.
     */
    private inline fun <T> executeValidatedRequest(
        startUrl: String,
        crossinline handler: (okhttp3.Response) -> T?
    ): T? {
        val noRedirectClient = okHttpClient.newBuilder()
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
        var currentUrl = startUrl
        repeat(MAX_REDIRECTS + 1) { hop ->
            if (NetworkSecurityGuard.validateHttpUri(currentUrl, allowPrivateHosts = false) == null) return null
            val req = Request.Builder()
                .url(currentUrl)
                .header("User-Agent", "IVIDSMusic-Desktop")
                .build()
            noRedirectClient.newCall(req).execute().use { resp ->
                if (resp.code in REDIRECT_CODES && hop < MAX_REDIRECTS) {
                    val loc = resp.header("Location") ?: return null
                    val next = NetworkSecurityGuard.validateRedirectTarget(currentUrl, loc) ?: return null
                    currentUrl = next.toString()
                    return@repeat
                }
                if (!resp.isSuccessful) return null
                return handler(resp)
            }
        }
        return null
    }

    private fun readToolVersionLine(executablePath: String): String? {
        val file = File(executablePath)
        if (!file.isFile) return null
        return runCatching {
            val proc = ProcessBuilder(executablePath, "--version").start()
            val finished = proc.waitFor(4L, TimeUnit.SECONDS)
            if (!finished) {
                proc.destroyForcibly()
                return@runCatching null
            }
            proc.inputStream.bufferedReader().readLine()?.trim()
        }.getOrNull()
    }

    private fun moveAtomically(src: File, dst: File) {
        dst.parentFile?.mkdirs()
        try {
            Files.move(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: Exception) {
            Files.move(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    @Serializable
    private data class GitHubReleaseDto(
        val tag_name: String = "",
        val assets: List<GitHubAssetDto> = emptyList(),
    )

    @Serializable
    private data class GitHubAssetDto(
        val name: String = "",
        val browser_download_url: String = "",
    )

    companion object {
        private const val YTDLP_LATEST_RELEASE_API = "https://api.github.com/repos/yt-dlp/yt-dlp/releases/latest"
        private const val PORTABLE_FFMPEG_ZIP_URL =
            "https://github.com/BtbN/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-win64-gpl.zip"
        private const val YTDLP_CHECK_INTERVAL_MS = 7L * 24L * 60L * 60L * 1_000L
        private const val MIN_EXECUTABLE_BYTES = 100_000L
        private const val MAX_TOOL_DOWNLOAD_BYTES = 160L * 1024L * 1024L
        private const val MAX_API_RESPONSE_BYTES = 2L * 1024L * 1024L
        private const val MAX_REDIRECTS = 5
        private val REDIRECT_CODES = setOf(301, 302, 303, 307, 308)
    }
}
