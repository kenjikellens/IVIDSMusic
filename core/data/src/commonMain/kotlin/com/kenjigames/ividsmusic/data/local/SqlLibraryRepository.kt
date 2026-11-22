package com.kenjigames.ividsmusic.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.kenjigames.ividsmusic.data.db.IvidsDatabase
import com.kenjigames.ividsmusic.data.db.Listen_history
import com.kenjigames.ividsmusic.data.db.Playlists
import com.kenjigames.ividsmusic.data.db.Saved_albums
import com.kenjigames.ividsmusic.data.db.Tracks
import com.kenjigames.ividsmusic.domain.model.Album
import com.kenjigames.ividsmusic.domain.model.AppResult
import com.kenjigames.ividsmusic.domain.model.AudioQuality
import com.kenjigames.ividsmusic.domain.model.DomainError
import com.kenjigames.ividsmusic.domain.model.HistoryEntry
import com.kenjigames.ividsmusic.domain.model.Playlist
import com.kenjigames.ividsmusic.domain.model.Track
import com.kenjigames.ividsmusic.domain.port.LibraryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * SQLDelight-backed implementation of [LibraryRepository] managing tracks, playlists, saved albums, and 20-item history.
 * Runs write transactions on [ioDispatcher] while exposing reactive SQLDelight flows to presentation ViewModels.
 */
class SqlLibraryRepository(
    private val database: IvidsDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clockMs: () -> Long = { System.currentTimeMillis() }
) : LibraryRepository {

    private val queries = database.ividsMusicQueries

    /** Observes all tracks marked as liked, ordered newest-first. */
    override fun observeLikedTracks(): Flow<List<Track>> =
        queries.selectLikedTracks()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { rows -> rows.map(::mapTrack) }

    /** Observes all tracks marked as downloaded locally, ordered newest-first. */
    override fun observeDownloadedTracks(): Flow<List<Track>> =
        queries.selectDownloadedTracks()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { rows -> rows.map(::mapTrack) }

    /** Observes all albums saved to the local library, ordered newest-first. */
    override fun observeSavedAlbums(): Flow<List<Album>> =
        queries.selectSavedAlbums()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { rows -> rows.map(::mapSavedAlbum) }

    /** Observes all user playlists hydrated with their position-ordered [Track] lists. */
    override fun observePlaylists(): Flow<List<Playlist>> {
        val playlistsFlow = queries.selectAllPlaylists().asFlow().mapToList(ioDispatcher)
        val relationsFlow = queries.selectAllPlaylistTrackRelations().asFlow().mapToList(ioDispatcher)
        return combine(playlistsFlow, relationsFlow) { playlistRows, relationRows ->
            val tracksByPlaylist = LinkedHashMap<Long, MutableList<Track>>()
            for (rel in relationRows) {
                val track = Track(
                    id = rel.id,
                    title = rel.title,
                    artist = rel.artist_name,
                    artistId = rel.artist_id,
                    album = rel.album_title,
                    albumId = rel.album_id,
                    coverUrl = rel.cover_url,
                    durationSeconds = rel.duration_seconds.toInt(),
                    previewUrl = rel.preview_url,
                    genres = parseGenresCsv(rel.genres_csv),
                    releaseYear = rel.release_year?.toInt(),
                    rank = rel.rank.toInt(),
                    videoId = rel.video_id,
                    isLiked = rel.is_liked != 0L,
                    isDownloaded = rel.is_downloaded != 0L,
                    localFilePath = rel.local_file_path,
                    bitrateKbps = rel.bitrate_kbps?.toInt(),
                    addedTimestampMs = rel.added_timestamp_ms
                )
                tracksByPlaylist.getOrPut(rel.playlist_id) { mutableListOf() }.add(track)
            }
            playlistRows.map { row ->
                mapPlaylist(row, tracksByPlaylist[row.id].orEmpty())
            }
        }
    }

    /** Observes a single playlist matching [playlistId] alongside its ordered tracks, or `null` if deleted. */
    override fun observePlaylist(playlistId: Long): Flow<Playlist?> {
        val playlistFlow = queries.selectPlaylistById(playlistId).asFlow().mapToOneOrNull(ioDispatcher)
        val tracksFlow = queries.selectTracksForPlaylist(playlistId).asFlow().mapToList(ioDispatcher)
        return combine(playlistFlow, tracksFlow) { playlistRow, trackRows ->
            playlistRow?.let { mapPlaylist(it, trackRows.map(::mapTrack)) }
        }
    }

    /** Observes up to 20 most recently played [HistoryEntry] items, newest first. */
    override fun observeHistory(): Flow<List<HistoryEntry>> =
        queries.selectHistory()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { rows -> rows.map(::mapHistoryEntry) }

    /** Looks up a single persisted [Track] by [trackId] on [ioDispatcher], returning `null` if absent. */
    override suspend fun getTrack(trackId: String): Track? = withContext(ioDispatcher) {
        if (trackId.isBlank()) return@withContext null
        queries.selectTrackById(trackId).executeAsOneOrNull()?.let(::mapTrack)
    }

    /** Returns every row currently stored in `tracks` for local startup reconciliation. */
    suspend fun getAllPersistedTracks(): List<Track> = withContext(ioDispatcher) {
        queries.selectAllTracks().executeAsList().map(::mapTrack)
    }

    /** Inserts or updates [track] metadata in SQLite while preserving existing like and download flags. */
    override suspend fun upsertTrack(track: Track): AppResult<Unit> = runStorageWrite {
        if (track.id.isBlank() || track.title.isBlank()) {
            return@runStorageWrite AppResult.Failure(DomainError.Validation("Track id and title cannot be blank"))
        }
        database.transaction {
            upsertTrackInTransaction(track)
        }
        AppResult.Success(Unit)
    }

    /** Updates the liked status for [track], inserting the track metadata row if not yet persisted. */
    override suspend fun setTrackLiked(track: Track, liked: Boolean): AppResult<Unit> = runStorageWrite {
        if (track.id.isBlank()) {
            return@runStorageWrite AppResult.Failure(DomainError.Validation("Track id cannot be blank"))
        }
        val now = clockMs()
        database.transaction {
            upsertTrackInTransaction(track.copy(addedTimestampMs = if (track.addedTimestampMs > 0L) track.addedTimestampMs else now))
            queries.updateTrackLiked(
                is_liked = if (liked) 1L else 0L,
                added_timestamp_ms = now,
                id = track.id
            )
        }
        AppResult.Success(Unit)
    }

    /** Saves or removes [album] from the local `saved_albums` table. */
    override suspend fun setAlbumSaved(album: Album, saved: Boolean): AppResult<Unit> = runStorageWrite {
        if (album.id.isBlank()) {
            return@runStorageWrite AppResult.Failure(DomainError.Validation("Album id cannot be blank"))
        }
        if (saved) {
            queries.upsertSavedAlbum(
                id = album.id,
                title = album.title,
                artist_name = album.artist,
                artist_id = album.artistId,
                cover_url = album.coverUrl,
                track_count = album.trackCount.toLong(),
                release_year = album.releaseYear?.toLong(),
                saved_timestamp_ms = clockMs()
            )
        } else {
            queries.deleteSavedAlbum(album.id)
        }
        AppResult.Success(Unit)
    }

    /** Persists the local offline [filePath] and [quality] bitrate for [track] inside a transaction. */
    override suspend fun markTrackDownloaded(
        track: Track,
        filePath: String,
        quality: AudioQuality
    ): AppResult<Unit> = runStorageWrite {
        if (track.id.isBlank() || filePath.isBlank()) {
            return@runStorageWrite AppResult.Failure(DomainError.Validation("Track id and filePath are required"))
        }
        val now = clockMs()
        database.transaction {
            upsertTrackInTransaction(
                track.copy(
                    isDownloaded = true,
                    localFilePath = filePath,
                    bitrateKbps = quality.kbps,
                    addedTimestampMs = if (track.addedTimestampMs > 0L) track.addedTimestampMs else now
                )
            )
            queries.updateTrackDownloaded(
                local_file_path = filePath,
                bitrate_kbps = quality.kbps.toLong(),
                added_timestamp_ms = now,
                id = track.id
            )
        }
        AppResult.Success(Unit)
    }

    /** Clears the offline download path and downloaded flag for [trackId] without removing likes or playlists. */
    override suspend fun removeDownloadedTrack(trackId: String): AppResult<Unit> = runStorageWrite {
        if (trackId.isBlank()) {
            return@runStorageWrite AppResult.Failure(DomainError.Validation("Track id cannot be blank"))
        }
        queries.clearTrackDownloaded(trackId)
        AppResult.Success(Unit)
    }

    /** Atomically records [track] into `listen_history` and enforces the 20-track cap inside one transaction. */
    override suspend fun recordHistory(track: Track, playedTimestampMs: Long): AppResult<Unit> = runStorageWrite {
        if (track.id.isBlank() || track.title.isBlank()) {
            return@runStorageWrite AppResult.Failure(DomainError.Validation("History track requires non-blank id and title"))
        }
        database.transaction {
            val latestTs = queries.selectLatestHistoryTimestamp().executeAsOne()
            val monotonicTs = maxOf(playedTimestampMs, latestTs + 1L)
            queries.upsertHistory(
                track_id = track.id,
                title = track.title,
                artist_name = track.artist,
                artist_id = track.artistId,
                album_title = track.album,
                album_id = track.albumId,
                cover_url = track.coverUrl,
                duration_seconds = track.durationSeconds.toLong(),
                played_timestamp_ms = monotonicTs
            )
            queries.trimHistoryToTwenty()
        }
        AppResult.Success(Unit)
    }

    /** Removes all rows from `listen_history`. */
    override suspend fun clearHistory(): AppResult<Unit> = runStorageWrite {
        queries.clearHistory()
        AppResult.Success(Unit)
    }

    /** Creates a new playlist with sanitized [title] and deterministic cover gradient index. */
    override suspend fun createPlaylist(title: String, description: String): AppResult<Playlist> = runStorageWrite {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) {
            return@runStorageWrite AppResult.Failure(DomainError.Validation("Playlist title cannot be empty"))
        }
        val cleanDesc = description.trim()
        val now = clockMs()
        val created = database.transactionWithResult {
            val count = queries.selectAllPlaylists().executeAsList().size
            val gradientIndex = count % PLAYLIST_GRADIENT_COUNT
            queries.insertPlaylist(
                title = cleanTitle,
                description = cleanDesc,
                cover_gradient_index = gradientIndex.toLong(),
                created_timestamp_ms = now
            )
            val insertedId = queries.lastInsertRowId().executeAsOne()
            Playlist(
                id = insertedId,
                title = cleanTitle,
                description = cleanDesc,
                coverGradientIndex = gradientIndex,
                tracks = emptyList(),
                createdTimestampMs = now
            )
        }
        AppResult.Success(created)
    }

    /** Renames an existing playlist and optionally updates its description. */
    override suspend fun renamePlaylist(
        playlistId: Long,
        newTitle: String,
        newDescription: String?
    ): AppResult<Unit> = runStorageWrite {
        val cleanTitle = newTitle.trim()
        if (cleanTitle.isEmpty()) {
            return@runStorageWrite AppResult.Failure(DomainError.Validation("Playlist title cannot be empty"))
        }
        database.transaction {
            val existing = queries.selectPlaylistById(playlistId).executeAsOneOrNull()
                ?: error("Playlist $playlistId not found")
            queries.updatePlaylist(
                title = cleanTitle,
                description = newDescription?.trim() ?: existing.description,
                id = playlistId
            )
        }
        AppResult.Success(Unit)
    }

    /** Deletes playlist [playlistId], relying on SQLite `ON DELETE CASCADE` to clean `playlist_tracks`. */
    override suspend fun deletePlaylist(playlistId: Long): AppResult<Unit> = runStorageWrite {
        queries.deletePlaylistById(playlistId)
        AppResult.Success(Unit)
    }

    /** Appends [track] at the next contiguous `position_index` inside playlist [playlistId] without duplicating entries. */
    override suspend fun addTrackToPlaylist(playlistId: Long, track: Track): AppResult<Unit> = runStorageWrite {
        if (track.id.isBlank()) {
            return@runStorageWrite AppResult.Failure(DomainError.Validation("Track id cannot be blank"))
        }
        database.transaction {
            val playlist = queries.selectPlaylistById(playlistId).executeAsOneOrNull()
                ?: error("Playlist $playlistId not found")
            upsertTrackInTransaction(track)
            val nextPosition = queries.selectMaxPositionInPlaylist(playlist.id).executeAsOne() + 1L
            queries.insertPlaylistTrackIgnore(
                playlist_id = playlist.id,
                track_id = track.id,
                position_index = nextPosition
            )
        }
        AppResult.Success(Unit)
    }

    /** Removes [trackId] from playlist [playlistId] and compacts remaining `position_index` values sequentially. */
    override suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: String): AppResult<Unit> = runStorageWrite {
        database.transaction {
            queries.deleteTrackFromPlaylist(playlistId, trackId)
            val remaining = queries.selectTracksForPlaylist(playlistId).executeAsList()
            remaining.forEachIndexed { index, trackRow ->
                queries.updatePlaylistTrackPosition(
                    position_index = index.toLong(),
                    playlist_id = playlistId,
                    track_id = trackRow.id
                )
            }
        }
        AppResult.Success(Unit)
    }

    /** Rewrites `position_index` values for playlist [playlistId] according to [orderedTrackIds] inside one transaction. */
    override suspend fun reorderPlaylistTracks(
        playlistId: Long,
        orderedTrackIds: List<String>
    ): AppResult<Unit> = runStorageWrite {
        database.transaction {
            val existingIds = queries.selectTracksForPlaylist(playlistId).executeAsList().map { it.id }
            val existingSet = existingIds.toSet()
            val sanitizedOrder = buildList {
                for (id in orderedTrackIds) {
                    if (id in existingSet && id !in this) add(id)
                }
                for (id in existingIds) {
                    if (id !in this) add(id)
                }
            }
            sanitizedOrder.forEachIndexed { index, trackId ->
                queries.updatePlaylistTrackPosition(
                    position_index = index.toLong(),
                    playlist_id = playlistId,
                    track_id = trackId
                )
            }
        }
        AppResult.Success(Unit)
    }

    private fun upsertTrackInTransaction(track: Track) {
        val genresCsv = track.genres.joinToString(",") { it.trim() }.trim(',')
        val timestamp = if (track.addedTimestampMs > 0L) track.addedTimestampMs else clockMs()
        queries.insertTrackIgnore(
            id = track.id,
            title = track.title,
            artist_name = track.artist,
            artist_id = track.artistId,
            album_title = track.album,
            album_id = track.albumId,
            cover_url = track.coverUrl,
            duration_seconds = track.durationSeconds.toLong(),
            preview_url = track.previewUrl,
            genres_csv = genresCsv,
            release_year = track.releaseYear?.toLong(),
            rank = track.rank.toLong(),
            video_id = track.videoId,
            is_liked = if (track.isLiked) 1L else 0L,
            is_downloaded = if (track.isDownloaded) 1L else 0L,
            local_file_path = track.localFilePath,
            bitrate_kbps = track.bitrateKbps?.toLong(),
            added_timestamp_ms = timestamp
        )
        queries.updateTrackMetadataPreserveFlags(
            title = track.title,
            artistName = track.artist,
            artistId = track.artistId,
            albumTitle = track.album,
            albumId = track.albumId,
            coverUrl = track.coverUrl,
            durationSeconds = track.durationSeconds.toLong(),
            previewUrl = track.previewUrl,
            genresCsv = genresCsv,
            releaseYear = track.releaseYear?.toLong(),
            rank = track.rank.toLong(),
            videoId = track.videoId,
            id = track.id
        )
    }

    private suspend inline fun <T> runStorageWrite(
        crossinline block: () -> AppResult<T>
    ): AppResult<T> = withContext(ioDispatcher) {
        try {
            block()
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (error: Exception) {
            AppResult.Failure(DomainError.Storage(error.message ?: "SQLite storage operation failed"))
        }
    }

    private fun mapTrack(row: Tracks): Track = Track(
        id = row.id,
        title = row.title,
        artist = row.artist_name,
        artistId = row.artist_id,
        album = row.album_title,
        albumId = row.album_id,
        coverUrl = row.cover_url,
        durationSeconds = row.duration_seconds.toInt(),
        previewUrl = row.preview_url,
        genres = parseGenresCsv(row.genres_csv),
        releaseYear = row.release_year?.toInt(),
        rank = row.rank.toInt(),
        videoId = row.video_id,
        isLiked = row.is_liked != 0L,
        isDownloaded = row.is_downloaded != 0L,
        localFilePath = row.local_file_path,
        bitrateKbps = row.bitrate_kbps?.toInt(),
        addedTimestampMs = row.added_timestamp_ms
    )

    private fun mapSavedAlbum(row: Saved_albums): Album = Album(
        id = row.id,
        title = row.title,
        artist = row.artist_name,
        artistId = row.artist_id,
        coverUrl = row.cover_url,
        trackCount = row.track_count.toInt(),
        releaseYear = row.release_year?.toInt(),
        isSaved = true
    )

    private fun mapPlaylist(row: Playlists, tracks: List<Track>): Playlist = Playlist(
        id = row.id,
        title = row.title,
        description = row.description,
        coverGradientIndex = row.cover_gradient_index.toInt(),
        tracks = tracks,
        createdTimestampMs = row.created_timestamp_ms
    )

    private fun mapHistoryEntry(row: Listen_history): HistoryEntry = HistoryEntry(
        track = Track(
            id = row.track_id,
            title = row.title,
            artist = row.artist_name,
            artistId = row.artist_id,
            album = row.album_title,
            albumId = row.album_id,
            coverUrl = row.cover_url,
            durationSeconds = row.duration_seconds.toInt()
        ),
        playedTimestampMs = row.played_timestamp_ms
    )

    private fun parseGenresCsv(csv: String): List<String> =
        csv.split(',').map { it.trim() }.filter { it.isNotEmpty() }

    companion object {
        private const val PLAYLIST_GRADIENT_COUNT = 6
    }
}
