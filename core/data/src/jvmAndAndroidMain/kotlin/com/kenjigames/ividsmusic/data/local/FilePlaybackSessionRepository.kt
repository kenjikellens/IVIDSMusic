package com.kenjigames.ividsmusic.data.local

import com.kenjigames.ividsmusic.domain.model.AppResult
import com.kenjigames.ividsmusic.domain.model.DomainError
import com.kenjigames.ividsmusic.domain.model.PlaybackSessionSnapshot
import com.kenjigames.ividsmusic.domain.model.RepeatMode
import com.kenjigames.ividsmusic.domain.model.Track
import com.kenjigames.ividsmusic.domain.port.PlaybackSessionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * JSON-backed [PlaybackSessionRepository] persisting the last queue, active track, and cursor atomically.
 * Ignores corrupt or missing files cleanly so startup never crashes on damaged local state.
 */
class FilePlaybackSessionRepository(
    private val sessionFile: File,
    private val json: Json,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : PlaybackSessionRepository {

    private val fileMutex = Mutex()

    /**
     * Reads and parses [sessionFile] into a [PlaybackSessionSnapshot], returning `null` if missing or invalid.
     * Drops empty queues and clamps stored track indices and cursor positions within bounds.
     */
    override suspend fun load(): PlaybackSessionSnapshot? = fileMutex.withLock {
        withContext(ioDispatcher) {
            if (!sessionFile.exists() || !sessionFile.isFile || sessionFile.length() !in 1L..MAX_SESSION_FILE_BYTES) {
                return@withContext null
            }
            val raw = runCatching { sessionFile.readText(Charsets.UTF_8) }.getOrNull()
                ?.takeIf { it.isNotBlank() } ?: return@withContext null
            val dto = runCatching { json.decodeFromString<SessionSnapshotDto>(raw) }.getOrNull()
                ?: return@withContext null
            val mappedTracks = dto.queue.mapNotNull { it.toDomainOrNull() }
            if (mappedTracks.isEmpty()) return@withContext null
            val repeatMode = RepeatMode.entries.firstOrNull { it.name == dto.repeatMode } ?: RepeatMode.OFF
            PlaybackSessionSnapshot(
                queue = mappedTracks,
                currentIndex = dto.currentIndex.coerceIn(0, mappedTracks.lastIndex),
                positionMs = dto.positionMs.coerceAtLeast(0L),
                isShuffleEnabled = dto.isShuffleEnabled,
                repeatMode = repeatMode,
                updatedAtMs = dto.updatedAtMs.coerceAtLeast(0L),
            )
        }
    }

    /**
     * Serializes [snapshot] via a `.tmp` staging file and atomically replaces [sessionFile].
     * Deletes empty snapshots cleanly instead of keeping blank session payloads on disk.
     */
    override suspend fun save(snapshot: PlaybackSessionSnapshot): AppResult<Unit> = fileMutex.withLock {
        withContext(ioDispatcher) {
            if (snapshot.queue.isEmpty()) {
                return@withContext clearLocked()
            }
            try {
                sessionFile.parentFile?.mkdirs()
                val dto = SessionSnapshotDto(
                    queue = snapshot.queue.take(MAX_PERSISTED_QUEUE).map(SessionTrackDto::fromDomain),
                    currentIndex = snapshot.currentIndex.coerceIn(0, snapshot.queue.lastIndex),
                    positionMs = snapshot.positionMs.coerceAtLeast(0L),
                    isShuffleEnabled = snapshot.isShuffleEnabled,
                    repeatMode = snapshot.repeatMode.name,
                    updatedAtMs = snapshot.updatedAtMs,
                )
                val encoded = json.encodeToString(dto)
                val tempFile = File(sessionFile.parentFile, "${sessionFile.name}.tmp")
                tempFile.writeText(encoded, Charsets.UTF_8)
                try {
                    Files.move(
                        tempFile.toPath(),
                        sessionFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE,
                    )
                } catch (_: AtomicMoveNotSupportedException) {
                    Files.move(
                        tempFile.toPath(),
                        sessionFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                    )
                }
                AppResult.Success(Unit)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                AppResult.Failure(DomainError.Storage(error.message ?: "Failed to save playback session"))
            }
        }
    }

    /**
     * Deletes [sessionFile] and any leftover temporary staging file under [fileMutex].
     */
    override suspend fun clear(): AppResult<Unit> = fileMutex.withLock {
        withContext(ioDispatcher) { clearLocked() }
    }

    private fun clearLocked(): AppResult<Unit> = try {
        val tempFile = File(sessionFile.parentFile, "${sessionFile.name}.tmp")
        if (tempFile.exists()) tempFile.delete()
        if (sessionFile.exists()) sessionFile.delete()
        AppResult.Success(Unit)
    } catch (error: Exception) {
        AppResult.Failure(DomainError.Storage(error.message ?: "Failed to clear playback session"))
    }

    @Serializable
    private data class SessionSnapshotDto(
        val queue: List<SessionTrackDto> = emptyList(),
        val currentIndex: Int = -1,
        val positionMs: Long = 0L,
        val isShuffleEnabled: Boolean = false,
        val repeatMode: String = RepeatMode.OFF.name,
        val updatedAtMs: Long = 0L,
    )

    @Serializable
    private data class SessionTrackDto(
        val id: String = "",
        val title: String = "",
        val artist: String = "",
        val artistId: String = "",
        val album: String = "",
        val albumId: String = "",
        val coverUrl: String = "",
        val durationSeconds: Int = 0,
        val previewUrl: String = "",
        val genres: List<String> = emptyList(),
        val releaseYear: Int? = null,
        val rank: Int = 0,
        val videoId: String? = null,
        val isLiked: Boolean = false,
        val isDownloaded: Boolean = false,
        val localFilePath: String? = null,
        val bitrateKbps: Int? = null,
        val addedTimestampMs: Long = 0L,
    ) {
        fun toDomainOrNull(): Track? {
            if (id.isBlank() || title.isBlank()) return null
            return Track(
                id = id,
                title = title,
                artist = artist,
                artistId = artistId,
                album = album,
                albumId = albumId,
                coverUrl = coverUrl,
                durationSeconds = durationSeconds.coerceAtLeast(0),
                previewUrl = previewUrl,
                genres = genres,
                releaseYear = releaseYear,
                rank = rank,
                videoId = videoId,
                isLiked = isLiked,
                isDownloaded = isDownloaded,
                localFilePath = localFilePath,
                bitrateKbps = bitrateKbps,
                addedTimestampMs = addedTimestampMs,
            )
        }

        companion object {
            fun fromDomain(track: Track): SessionTrackDto = SessionTrackDto(
                id = track.id,
                title = track.title,
                artist = track.artist,
                artistId = track.artistId,
                album = track.album,
                albumId = track.albumId,
                coverUrl = track.coverUrl,
                durationSeconds = track.durationSeconds,
                previewUrl = track.previewUrl,
                genres = track.genres,
                releaseYear = track.releaseYear,
                rank = track.rank,
                videoId = track.videoId,
                isLiked = track.isLiked,
                isDownloaded = track.isDownloaded,
                localFilePath = track.localFilePath,
                bitrateKbps = track.bitrateKbps,
                addedTimestampMs = track.addedTimestampMs,
            )
        }
    }

    private companion object {
        const val MAX_PERSISTED_QUEUE = 100
        const val MAX_SESSION_FILE_BYTES = 2L * 1024L * 1024L
    }
}
