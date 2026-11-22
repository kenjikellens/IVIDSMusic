package com.kenjigames.ividsmusic.player.desktop

import com.kenjigames.ividsmusic.domain.model.DomainError
import javafx.scene.media.MediaException
import java.io.FileNotFoundException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.nio.file.AccessDeniedException

/**
 * Maps JavaFX [MediaException] types, IO failures, and toolkit errors to canonical [DomainError] values.
 * Pure stateless mapper used by [DesktopAudioEngine].
 */
internal object DesktopErrorMapper {

    /**
     * Converts [throwable] encountered while loading or playing [trackId] into a [DomainError].
     * Uses [isLocalFile] to distinguish missing offline files from remote network or HTTP failures.
     */
    fun map(throwable: Throwable, trackId: String, isLocalFile: Boolean): DomainError {
        val message = throwable.message ?: "Desktop audio playback error"
        val lowerMessage = message.lowercase()

        if (throwable is FileNotFoundException) {
            return DomainError.StreamNotFound("Local audio file not found for track '$trackId': $message")
        }
        if (throwable is AccessDeniedException || throwable is SecurityException) {
            return DomainError.Storage("Storage permission error for track '$trackId': $message")
        }
        if (throwable is UnknownHostException || throwable is ConnectException || throwable is SocketTimeoutException) {
            return DomainError.Network(message, statusCode = null)
        }
        if (lowerMessage.contains("429")) {
            return DomainError.Network("Stream rate-limited (HTTP 429)", statusCode = 429)
        }
        if (lowerMessage.contains("403") || lowerMessage.contains("404") || lowerMessage.contains("410")) {
            return DomainError.StreamNotFound("Remote stream unavailable for track '$trackId': $message")
        }

        if (throwable is MediaException) {
            return mapMediaException(throwable, trackId, isLocalFile, message)
        }

        return DomainError.Unknown(
            message = message,
            causeName = throwable.javaClass.simpleName,
        )
    }

    /**
     * Classifies JavaFX [MediaException] subtypes into storage, stream-not-found, network, or unknown errors.
     * Distinguishes `MEDIA_UNAVAILABLE` between local file streams and remote network streams.
     */
    private fun mapMediaException(
        exception: MediaException,
        trackId: String,
        isLocalFile: Boolean,
        message: String,
    ): DomainError {
        return when (exception.type) {
            MediaException.Type.MEDIA_UNAVAILABLE -> {
                if (isLocalFile) {
                    DomainError.StreamNotFound("Local audio file unavailable for track '$trackId': $message")
                } else {
                    DomainError.Network("Remote audio stream unavailable: $message", statusCode = null)
                }
            }
            MediaException.Type.MEDIA_INACCESSIBLE -> {
                if (isLocalFile) {
                    DomainError.Storage("Local audio file is inaccessible: $message")
                } else {
                    DomainError.StreamNotFound("Remote audio stream inaccessible for track '$trackId': $message")
                }
            }
            MediaException.Type.MEDIA_CORRUPTED,
            MediaException.Type.MEDIA_UNSUPPORTED,
            MediaException.Type.MEDIA_UNSPECIFIED,
            MediaException.Type.OPERATION_UNSUPPORTED,
            MediaException.Type.PLAYBACK_ERROR,
            MediaException.Type.PLAYBACK_HALTED,
            MediaException.Type.UNKNOWN,
            null -> DomainError.Unknown(
                message = message,
                causeName = exception.type?.name ?: MediaException::class.simpleName,
            )
        }
    }
}
