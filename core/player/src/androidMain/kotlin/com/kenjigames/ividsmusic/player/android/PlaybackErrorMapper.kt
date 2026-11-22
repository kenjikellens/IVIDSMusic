package com.kenjigames.ividsmusic.player.android

import androidx.annotation.OptIn
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import com.kenjigames.ividsmusic.domain.model.DomainError
import java.io.FileNotFoundException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Translates Media3 [PlaybackException] instances and underlying HTTP/IO causes into [DomainError].
 * Pure mapper with no mutable state or Android service references.
 */
@OptIn(UnstableApi::class)
internal object PlaybackErrorMapper {

    private const val MAX_CAUSE_DEPTH: Int = 8

    /**
     * Converts [error] emitted for [activeTrackId] into a canonical [DomainError] subtype.
     * Inspects Media3 error codes, HTTP status codes, and nested IO causes before falling back to [DomainError.Unknown].
     */
    fun map(error: PlaybackException, activeTrackId: String): DomainError {
        val httpError = findCause<HttpDataSource.InvalidResponseCodeException>(error)
        if (httpError != null) {
            return mapHttpStatus(httpError.responseCode, activeTrackId)
        }
        return when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            PlaybackException.ERROR_CODE_IO_UNSPECIFIED -> mapIoError(error)
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                DomainError.Network(error.message ?: "Bad HTTP status during playback", statusCode = null)
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
                DomainError.StreamNotFound("Local audio file not found for track '$activeTrackId'")
            PlaybackException.ERROR_CODE_IO_NO_PERMISSION,
            PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE ->
                DomainError.Storage(error.message ?: error.errorCodeName)
            else -> {
                val fileNotFound = findCause<FileNotFoundException>(error)
                if (fileNotFound != null) {
                    DomainError.StreamNotFound("Audio stream file missing for track '$activeTrackId'")
                } else {
                    DomainError.Unknown(
                        message = error.message ?: "Playback error (${error.errorCodeName})",
                        causeName = error.cause?.javaClass?.simpleName,
                    )
                }
            }
        }
    }

    /**
     * Maps HTTP status codes from [HttpDataSource.InvalidResponseCodeException] to domain errors.
     * Maps `429` to rate-limited [DomainError.Network] and `403`/`404`/`410` to [DomainError.StreamNotFound].
     */
    private fun mapHttpStatus(statusCode: Int, activeTrackId: String): DomainError {
        return when (statusCode) {
            429 -> DomainError.Network("Stream rate-limited (HTTP 429)", statusCode = 429)
            403, 404, 410 ->
                DomainError.StreamNotFound("Remote stream unavailable (HTTP $statusCode) for track '$activeTrackId'")
            else -> DomainError.Network("HTTP playback error (HTTP $statusCode)", statusCode = statusCode)
        }
    }

    /**
     * Classifies generic IO playback failures into [DomainError.Network] or [DomainError.Unknown].
     * Checks cause chain for DNS, connection, and timeout exceptions.
     */
    private fun mapIoError(error: PlaybackException): DomainError {
        val hasNetworkCause = findCause<UnknownHostException>(error) != null ||
            findCause<ConnectException>(error) != null ||
            findCause<SocketTimeoutException>(error) != null ||
            error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
            error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
        if (hasNetworkCause) {
            return DomainError.Network(error.message ?: "Network unavailable during playback", statusCode = null)
        }
        return DomainError.Unknown(
            message = error.message ?: "I/O playback failure (${error.errorCodeName})",
            causeName = error.cause?.javaClass?.simpleName,
        )
    }

    /**
     * Walks up to [MAX_CAUSE_DEPTH] exception causes to locate a cause of type [T].
     * Guards against cyclic cause graphs while keeping lookup shallow.
     */
    private inline fun <reified T : Throwable> findCause(root: Throwable): T? {
        var current: Throwable? = root
        repeat(MAX_CAUSE_DEPTH) {
            if (current == null) return null
            if (current is T) return current
            current = current?.cause
        }
        return null
    }
}
