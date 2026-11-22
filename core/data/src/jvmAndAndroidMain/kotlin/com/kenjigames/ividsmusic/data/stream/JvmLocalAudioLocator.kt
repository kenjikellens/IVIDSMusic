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
        private val INVALID_FILENAME_CHARS = Regex("[/\\\\?%*:|\"<>]")

        /** Strips filesystem-illegal characters from artist or track title segments deterministically. */
        internal fun sanitizeFilenamePart(raw: String): String =
            raw.trim().replace(INVALID_FILENAME_CHARS, "").ifEmpty { "Unknown" }
    }
}
