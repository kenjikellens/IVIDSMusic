package com.kenjigames.ividsmusic.data.stream

import com.kenjigames.ividsmusic.domain.model.Track
import java.io.File

/**
 * JVM/Android filesystem implementation of [LocalAudioLocator] verifying pre-saved `.m4a` and `.mp3` files.
 * Inspects explicit persisted paths first and falls back to normalized `"Artist - Title"` filenames in `saved/`.
 */
class JvmLocalAudioLocator : LocalAudioLocator {

    /**
     * Checks whether [track] already exists as a non-empty local audio file on disk under [savedDirPath].
     */
    override fun findLocalAudioPath(track: Track, savedDirPath: String): String? {
        if (isValidLocalFile(track.localFilePath)) {
            return track.localFilePath
        }
        val savedDir = File(savedDirPath)
        if (!savedDir.exists() || !savedDir.isDirectory) return null

        val cleanBase = "${sanitizeFilenamePart(track.artist)} - ${sanitizeFilenamePart(track.title)}"
        for (ext in SUPPORTED_EXTENSIONS) {
            val candidate = File(savedDir, "$cleanBase.$ext")
            if (candidate.isFile && candidate.length() > 0L) {
                return candidate.absolutePath
            }
        }
        return null
    }

    /**
     * Returns `true` when [filePath] refers to an existing non-empty regular file on disk.
     */
    override fun isValidLocalFile(filePath: String?): Boolean {
        if (filePath.isNullOrBlank()) return false
        val file = File(filePath)
        return file.exists() && file.isFile && file.length() > 0L
    }

    companion object {
        private val SUPPORTED_EXTENSIONS = listOf("m4a", "mp3")
        private val INVALID_FILENAME_CHARS = Regex("[/\\\\?%*:|\"<>\\u0000-\\u001F]")
        private val WINDOWS_RESERVED_NAMES = setOf(
            "CON", "PRN", "AUX", "NUL",
            "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
            "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
        )

        /**
         * Strips illegal, control, and trailing dot/space characters and guards Windows reserved device names.
         * Returns a safe single path segment defaulting to `"Unknown"` when empty.
         */
        internal fun sanitizeFilenamePart(raw: String): String {
            val cleaned = raw.trim()
                .replace(INVALID_FILENAME_CHARS, "")
                .trimEnd('.', ' ')
                .ifEmpty { "Unknown" }
            val stem = cleaned.substringBefore('.').uppercase()
            return if (stem in WINDOWS_RESERVED_NAMES) "_$cleaned" else cleaned
        }
    }
}
