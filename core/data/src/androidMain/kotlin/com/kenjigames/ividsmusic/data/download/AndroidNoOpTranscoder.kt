package com.kenjigames.ividsmusic.data.download

import com.kenjigames.ividsmusic.domain.model.AudioQuality
import java.io.File

/**
 * Android no-op [AudioTranscoder] because external `ffmpeg` transcoding is disabled on Android builds.
 * Causes [NativeDownloadManager] to fall back cleanly to the closest native `.m4a` stream bitrate.
 */
class AndroidNoOpTranscoder : AudioTranscoder {
    /** Always returns `false` on Android so downloads keep the native stream container. */
    override fun isAvailable(): Boolean = false

    /** Always returns `null` cleanly without modifying [sourcePartFile]. */
    override suspend fun transcodeIfNeeded(
        sourcePartFile: File,
        sourceExtension: String,
        sourceQuality: AudioQuality,
        targetQuality: AudioQuality
    ): TranscodeOutcome? = null
}
