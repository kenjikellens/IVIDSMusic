package com.kenjigames.ividsmusic.`data`.db

import app.cash.sqldelight.ExecutableQuery
import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import kotlin.Any
import kotlin.Long
import kotlin.String

public class IvidsMusicQueries(
  driver: SqlDriver,
) : TransacterImpl(driver) {
  public fun <T : Any> selectTrackById(id: String, mapper: (
    id: String,
    title: String,
    artist_name: String,
    artist_id: String,
    album_title: String,
    album_id: String,
    cover_url: String,
    duration_seconds: Long,
    preview_url: String,
    genres_csv: String,
    release_year: Long?,
    rank: Long,
    video_id: String?,
    is_liked: Long,
    is_downloaded: Long,
    local_file_path: String?,
    bitrate_kbps: Long?,
    added_timestamp_ms: Long,
  ) -> T): Query<T> = SelectTrackByIdQuery(id) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4)!!,
      cursor.getString(5)!!,
      cursor.getString(6)!!,
      cursor.getLong(7)!!,
      cursor.getString(8)!!,
      cursor.getString(9)!!,
      cursor.getLong(10),
      cursor.getLong(11)!!,
      cursor.getString(12),
      cursor.getLong(13)!!,
      cursor.getLong(14)!!,
      cursor.getString(15),
      cursor.getLong(16),
      cursor.getLong(17)!!
    )
  }

  public fun selectTrackById(id: String): Query<Tracks> = selectTrackById(id) { id_, title,
      artist_name, artist_id, album_title, album_id, cover_url, duration_seconds, preview_url,
      genres_csv, release_year, rank, video_id, is_liked, is_downloaded, local_file_path,
      bitrate_kbps, added_timestamp_ms ->
    Tracks(
      id_,
      title,
      artist_name,
      artist_id,
      album_title,
      album_id,
      cover_url,
      duration_seconds,
      preview_url,
      genres_csv,
      release_year,
      rank,
      video_id,
      is_liked,
      is_downloaded,
      local_file_path,
      bitrate_kbps,
      added_timestamp_ms
    )
  }

  public fun <T : Any> selectAllTracks(mapper: (
    id: String,
    title: String,
    artist_name: String,
    artist_id: String,
    album_title: String,
    album_id: String,
    cover_url: String,
    duration_seconds: Long,
    preview_url: String,
    genres_csv: String,
    release_year: Long?,
    rank: Long,
    video_id: String?,
    is_liked: Long,
    is_downloaded: Long,
    local_file_path: String?,
    bitrate_kbps: Long?,
    added_timestamp_ms: Long,
  ) -> T): Query<T> = Query(1_151_708_428, arrayOf("tracks"), driver, "IvidsMusic.sq",
      "selectAllTracks",
      "SELECT tracks.id, tracks.title, tracks.artist_name, tracks.artist_id, tracks.album_title, tracks.album_id, tracks.cover_url, tracks.duration_seconds, tracks.preview_url, tracks.genres_csv, tracks.release_year, tracks.rank, tracks.video_id, tracks.is_liked, tracks.is_downloaded, tracks.local_file_path, tracks.bitrate_kbps, tracks.added_timestamp_ms FROM tracks") {
      cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4)!!,
      cursor.getString(5)!!,
      cursor.getString(6)!!,
      cursor.getLong(7)!!,
      cursor.getString(8)!!,
      cursor.getString(9)!!,
      cursor.getLong(10),
      cursor.getLong(11)!!,
      cursor.getString(12),
      cursor.getLong(13)!!,
      cursor.getLong(14)!!,
      cursor.getString(15),
      cursor.getLong(16),
      cursor.getLong(17)!!
    )
  }

  public fun selectAllTracks(): Query<Tracks> = selectAllTracks { id, title, artist_name, artist_id,
      album_title, album_id, cover_url, duration_seconds, preview_url, genres_csv, release_year,
      rank, video_id, is_liked, is_downloaded, local_file_path, bitrate_kbps, added_timestamp_ms ->
    Tracks(
      id,
      title,
      artist_name,
      artist_id,
      album_title,
      album_id,
      cover_url,
      duration_seconds,
      preview_url,
      genres_csv,
      release_year,
      rank,
      video_id,
      is_liked,
      is_downloaded,
      local_file_path,
      bitrate_kbps,
      added_timestamp_ms
    )
  }

  public fun <T : Any> selectLikedTracks(mapper: (
    id: String,
    title: String,
    artist_name: String,
    artist_id: String,
    album_title: String,
    album_id: String,
    cover_url: String,
    duration_seconds: Long,
    preview_url: String,
    genres_csv: String,
    release_year: Long?,
    rank: Long,
    video_id: String?,
    is_liked: Long,
    is_downloaded: Long,
    local_file_path: String?,
    bitrate_kbps: Long?,
    added_timestamp_ms: Long,
  ) -> T): Query<T> = Query(-1_835_144_872, arrayOf("tracks"), driver, "IvidsMusic.sq",
      "selectLikedTracks",
      "SELECT tracks.id, tracks.title, tracks.artist_name, tracks.artist_id, tracks.album_title, tracks.album_id, tracks.cover_url, tracks.duration_seconds, tracks.preview_url, tracks.genres_csv, tracks.release_year, tracks.rank, tracks.video_id, tracks.is_liked, tracks.is_downloaded, tracks.local_file_path, tracks.bitrate_kbps, tracks.added_timestamp_ms FROM tracks WHERE is_liked = 1 ORDER BY added_timestamp_ms DESC") {
      cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4)!!,
      cursor.getString(5)!!,
      cursor.getString(6)!!,
      cursor.getLong(7)!!,
      cursor.getString(8)!!,
      cursor.getString(9)!!,
      cursor.getLong(10),
      cursor.getLong(11)!!,
      cursor.getString(12),
      cursor.getLong(13)!!,
      cursor.getLong(14)!!,
      cursor.getString(15),
      cursor.getLong(16),
      cursor.getLong(17)!!
    )
  }

  public fun selectLikedTracks(): Query<Tracks> = selectLikedTracks { id, title, artist_name,
      artist_id, album_title, album_id, cover_url, duration_seconds, preview_url, genres_csv,
      release_year, rank, video_id, is_liked, is_downloaded, local_file_path, bitrate_kbps,
      added_timestamp_ms ->
    Tracks(
      id,
      title,
      artist_name,
      artist_id,
      album_title,
      album_id,
      cover_url,
      duration_seconds,
      preview_url,
      genres_csv,
      release_year,
      rank,
      video_id,
      is_liked,
      is_downloaded,
      local_file_path,
      bitrate_kbps,
      added_timestamp_ms
    )
  }

  public fun <T : Any> selectDownloadedTracks(mapper: (
    id: String,
    title: String,
    artist_name: String,
    artist_id: String,
    album_title: String,
    album_id: String,
    cover_url: String,
    duration_seconds: Long,
    preview_url: String,
    genres_csv: String,
    release_year: Long?,
    rank: Long,
    video_id: String?,
    is_liked: Long,
    is_downloaded: Long,
    local_file_path: String?,
    bitrate_kbps: Long?,
    added_timestamp_ms: Long,
  ) -> T): Query<T> = Query(1_661_585_836, arrayOf("tracks"), driver, "IvidsMusic.sq",
      "selectDownloadedTracks",
      "SELECT tracks.id, tracks.title, tracks.artist_name, tracks.artist_id, tracks.album_title, tracks.album_id, tracks.cover_url, tracks.duration_seconds, tracks.preview_url, tracks.genres_csv, tracks.release_year, tracks.rank, tracks.video_id, tracks.is_liked, tracks.is_downloaded, tracks.local_file_path, tracks.bitrate_kbps, tracks.added_timestamp_ms FROM tracks WHERE is_downloaded = 1 ORDER BY added_timestamp_ms DESC") {
      cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4)!!,
      cursor.getString(5)!!,
      cursor.getString(6)!!,
      cursor.getLong(7)!!,
      cursor.getString(8)!!,
      cursor.getString(9)!!,
      cursor.getLong(10),
      cursor.getLong(11)!!,
      cursor.getString(12),
      cursor.getLong(13)!!,
      cursor.getLong(14)!!,
      cursor.getString(15),
      cursor.getLong(16),
      cursor.getLong(17)!!
    )
  }

  public fun selectDownloadedTracks(): Query<Tracks> = selectDownloadedTracks { id, title,
      artist_name, artist_id, album_title, album_id, cover_url, duration_seconds, preview_url,
      genres_csv, release_year, rank, video_id, is_liked, is_downloaded, local_file_path,
      bitrate_kbps, added_timestamp_ms ->
    Tracks(
      id,
      title,
      artist_name,
      artist_id,
      album_title,
      album_id,
      cover_url,
      duration_seconds,
      preview_url,
      genres_csv,
      release_year,
      rank,
      video_id,
      is_liked,
      is_downloaded,
      local_file_path,
      bitrate_kbps,
      added_timestamp_ms
    )
  }

  public fun <T : Any> selectSavedAlbums(mapper: (
    id: String,
    title: String,
    artist_name: String,
    artist_id: String,
    cover_url: String,
    track_count: Long,
    release_year: Long?,
    saved_timestamp_ms: Long,
  ) -> T): Query<T> = Query(1_086_595_086, arrayOf("saved_albums"), driver, "IvidsMusic.sq",
      "selectSavedAlbums",
      "SELECT saved_albums.id, saved_albums.title, saved_albums.artist_name, saved_albums.artist_id, saved_albums.cover_url, saved_albums.track_count, saved_albums.release_year, saved_albums.saved_timestamp_ms FROM saved_albums ORDER BY saved_timestamp_ms DESC") {
      cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4)!!,
      cursor.getLong(5)!!,
      cursor.getLong(6),
      cursor.getLong(7)!!
    )
  }

  public fun selectSavedAlbums(): Query<Saved_albums> = selectSavedAlbums { id, title, artist_name,
      artist_id, cover_url, track_count, release_year, saved_timestamp_ms ->
    Saved_albums(
      id,
      title,
      artist_name,
      artist_id,
      cover_url,
      track_count,
      release_year,
      saved_timestamp_ms
    )
  }

  public fun <T : Any> selectAllPlaylists(mapper: (
    id: Long,
    title: String,
    description: String,
    cover_gradient_index: Long,
    created_timestamp_ms: Long,
  ) -> T): Query<T> = Query(-249_444_899, arrayOf("playlists"), driver, "IvidsMusic.sq",
      "selectAllPlaylists",
      "SELECT playlists.id, playlists.title, playlists.description, playlists.cover_gradient_index, playlists.created_timestamp_ms FROM playlists ORDER BY created_timestamp_ms ASC, id ASC") {
      cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getLong(3)!!,
      cursor.getLong(4)!!
    )
  }

  public fun selectAllPlaylists(): Query<Playlists> = selectAllPlaylists { id, title, description,
      cover_gradient_index, created_timestamp_ms ->
    Playlists(
      id,
      title,
      description,
      cover_gradient_index,
      created_timestamp_ms
    )
  }

  public fun <T : Any> selectPlaylistById(id: Long, mapper: (
    id: Long,
    title: String,
    description: String,
    cover_gradient_index: Long,
    created_timestamp_ms: Long,
  ) -> T): Query<T> = SelectPlaylistByIdQuery(id) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getLong(3)!!,
      cursor.getLong(4)!!
    )
  }

  public fun selectPlaylistById(id: Long): Query<Playlists> = selectPlaylistById(id) { id_, title,
      description, cover_gradient_index, created_timestamp_ms ->
    Playlists(
      id_,
      title,
      description,
      cover_gradient_index,
      created_timestamp_ms
    )
  }

  public fun lastInsertRowId(): ExecutableQuery<Long> = Query(-122_466_811, driver, "IvidsMusic.sq",
      "lastInsertRowId", "SELECT last_insert_rowid()") { cursor ->
    cursor.getLong(0)!!
  }

  public fun <T : Any> selectTracksForPlaylist(playlist_id: Long, mapper: (
    id: String,
    title: String,
    artist_name: String,
    artist_id: String,
    album_title: String,
    album_id: String,
    cover_url: String,
    duration_seconds: Long,
    preview_url: String,
    genres_csv: String,
    release_year: Long?,
    rank: Long,
    video_id: String?,
    is_liked: Long,
    is_downloaded: Long,
    local_file_path: String?,
    bitrate_kbps: Long?,
    added_timestamp_ms: Long,
  ) -> T): Query<T> = SelectTracksForPlaylistQuery(playlist_id) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4)!!,
      cursor.getString(5)!!,
      cursor.getString(6)!!,
      cursor.getLong(7)!!,
      cursor.getString(8)!!,
      cursor.getString(9)!!,
      cursor.getLong(10),
      cursor.getLong(11)!!,
      cursor.getString(12),
      cursor.getLong(13)!!,
      cursor.getLong(14)!!,
      cursor.getString(15),
      cursor.getLong(16),
      cursor.getLong(17)!!
    )
  }

  public fun selectTracksForPlaylist(playlist_id: Long): Query<Tracks> =
      selectTracksForPlaylist(playlist_id) { id, title, artist_name, artist_id, album_title,
      album_id, cover_url, duration_seconds, preview_url, genres_csv, release_year, rank, video_id,
      is_liked, is_downloaded, local_file_path, bitrate_kbps, added_timestamp_ms ->
    Tracks(
      id,
      title,
      artist_name,
      artist_id,
      album_title,
      album_id,
      cover_url,
      duration_seconds,
      preview_url,
      genres_csv,
      release_year,
      rank,
      video_id,
      is_liked,
      is_downloaded,
      local_file_path,
      bitrate_kbps,
      added_timestamp_ms
    )
  }

  public fun <T : Any> selectAllPlaylistTrackRelations(mapper: (
    playlist_id: Long,
    position_index: Long,
    id: String,
    title: String,
    artist_name: String,
    artist_id: String,
    album_title: String,
    album_id: String,
    cover_url: String,
    duration_seconds: Long,
    preview_url: String,
    genres_csv: String,
    release_year: Long?,
    rank: Long,
    video_id: String?,
    is_liked: Long,
    is_downloaded: Long,
    local_file_path: String?,
    bitrate_kbps: Long?,
    added_timestamp_ms: Long,
  ) -> T): Query<T> = Query(1_108_040_674, arrayOf("playlist_tracks", "tracks"), driver,
      "IvidsMusic.sq", "selectAllPlaylistTrackRelations", """
  |SELECT pt.playlist_id, pt.position_index, t.id, t.title, t.artist_name, t.artist_id, t.album_title, t.album_id, t.cover_url, t.duration_seconds, t.preview_url, t.genres_csv, t.release_year, t.rank, t.video_id, t.is_liked, t.is_downloaded, t.local_file_path, t.bitrate_kbps, t.added_timestamp_ms
  |FROM playlist_tracks pt
  |INNER JOIN tracks t ON t.id = pt.track_id
  |ORDER BY pt.playlist_id ASC, pt.position_index ASC
  """.trimMargin()) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getLong(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4)!!,
      cursor.getString(5)!!,
      cursor.getString(6)!!,
      cursor.getString(7)!!,
      cursor.getString(8)!!,
      cursor.getLong(9)!!,
      cursor.getString(10)!!,
      cursor.getString(11)!!,
      cursor.getLong(12),
      cursor.getLong(13)!!,
      cursor.getString(14),
      cursor.getLong(15)!!,
      cursor.getLong(16)!!,
      cursor.getString(17),
      cursor.getLong(18),
      cursor.getLong(19)!!
    )
  }

  public fun selectAllPlaylistTrackRelations(): Query<SelectAllPlaylistTrackRelations> =
      selectAllPlaylistTrackRelations { playlist_id, position_index, id, title, artist_name,
      artist_id, album_title, album_id, cover_url, duration_seconds, preview_url, genres_csv,
      release_year, rank, video_id, is_liked, is_downloaded, local_file_path, bitrate_kbps,
      added_timestamp_ms ->
    SelectAllPlaylistTrackRelations(
      playlist_id,
      position_index,
      id,
      title,
      artist_name,
      artist_id,
      album_title,
      album_id,
      cover_url,
      duration_seconds,
      preview_url,
      genres_csv,
      release_year,
      rank,
      video_id,
      is_liked,
      is_downloaded,
      local_file_path,
      bitrate_kbps,
      added_timestamp_ms
    )
  }

  public fun selectMaxPositionInPlaylist(playlist_id: Long): Query<Long> =
      SelectMaxPositionInPlaylistQuery(playlist_id) { cursor ->
    cursor.getLong(0)!!
  }

  public fun <T : Any> selectHistory(mapper: (
    track_id: String,
    title: String,
    artist_name: String,
    artist_id: String,
    album_title: String,
    album_id: String,
    cover_url: String,
    duration_seconds: Long,
    played_timestamp_ms: Long,
  ) -> T): Query<T> = Query(1_907_324_119, arrayOf("listen_history"), driver, "IvidsMusic.sq",
      "selectHistory",
      "SELECT listen_history.track_id, listen_history.title, listen_history.artist_name, listen_history.artist_id, listen_history.album_title, listen_history.album_id, listen_history.cover_url, listen_history.duration_seconds, listen_history.played_timestamp_ms FROM listen_history ORDER BY played_timestamp_ms DESC") {
      cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4)!!,
      cursor.getString(5)!!,
      cursor.getString(6)!!,
      cursor.getLong(7)!!,
      cursor.getLong(8)!!
    )
  }

  public fun selectHistory(): Query<Listen_history> = selectHistory { track_id, title, artist_name,
      artist_id, album_title, album_id, cover_url, duration_seconds, played_timestamp_ms ->
    Listen_history(
      track_id,
      title,
      artist_name,
      artist_id,
      album_title,
      album_id,
      cover_url,
      duration_seconds,
      played_timestamp_ms
    )
  }

  public fun selectLatestHistoryTimestamp(): Query<Long> = Query(-1_250_971_162,
      arrayOf("listen_history"), driver, "IvidsMusic.sq", "selectLatestHistoryTimestamp",
      "SELECT COALESCE(MAX(played_timestamp_ms), 0) FROM listen_history") { cursor ->
    cursor.getLong(0)!!
  }

  public fun <T : Any> selectAllScores(mapper: (
    entity_key: String,
    entity_type: String,
    entity_id: String,
    display_name: String,
    score: Long,
    last_updated_ms: Long,
  ) -> T): Query<T> = Query(1_109_657_765, arrayOf("discovery_scores"), driver, "IvidsMusic.sq",
      "selectAllScores",
      "SELECT discovery_scores.entity_key, discovery_scores.entity_type, discovery_scores.entity_id, discovery_scores.display_name, discovery_scores.score, discovery_scores.last_updated_ms FROM discovery_scores ORDER BY score DESC, last_updated_ms DESC") {
      cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getLong(4)!!,
      cursor.getLong(5)!!
    )
  }

  public fun selectAllScores(): Query<Discovery_scores> = selectAllScores { entity_key, entity_type,
      entity_id, display_name, score, last_updated_ms ->
    Discovery_scores(
      entity_key,
      entity_type,
      entity_id,
      display_name,
      score,
      last_updated_ms
    )
  }

  public fun <T : Any> selectScoreByKey(entity_key: String, mapper: (
    entity_key: String,
    entity_type: String,
    entity_id: String,
    display_name: String,
    score: Long,
    last_updated_ms: Long,
  ) -> T): Query<T> = SelectScoreByKeyQuery(entity_key) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getLong(4)!!,
      cursor.getLong(5)!!
    )
  }

  public fun selectScoreByKey(entity_key: String): Query<Discovery_scores> =
      selectScoreByKey(entity_key) { entity_key_, entity_type, entity_id, display_name, score,
      last_updated_ms ->
    Discovery_scores(
      entity_key_,
      entity_type,
      entity_id,
      display_name,
      score,
      last_updated_ms
    )
  }

  public fun insertTrackIgnore(
    id: String,
    title: String,
    artist_name: String,
    artist_id: String,
    album_title: String,
    album_id: String,
    cover_url: String,
    duration_seconds: Long,
    preview_url: String,
    genres_csv: String,
    release_year: Long?,
    rank: Long,
    video_id: String?,
    is_liked: Long,
    is_downloaded: Long,
    local_file_path: String?,
    bitrate_kbps: Long?,
    added_timestamp_ms: Long,
  ) {
    driver.execute(-888_760_349, """
        |INSERT OR IGNORE INTO tracks(
        |    id, title, artist_name, artist_id, album_title, album_id,
        |    cover_url, duration_seconds, preview_url, genres_csv, release_year,
        |    rank, video_id, is_liked, is_downloaded, local_file_path, bitrate_kbps, added_timestamp_ms
        |) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 18) {
          bindString(0, id)
          bindString(1, title)
          bindString(2, artist_name)
          bindString(3, artist_id)
          bindString(4, album_title)
          bindString(5, album_id)
          bindString(6, cover_url)
          bindLong(7, duration_seconds)
          bindString(8, preview_url)
          bindString(9, genres_csv)
          bindLong(10, release_year)
          bindLong(11, rank)
          bindString(12, video_id)
          bindLong(13, is_liked)
          bindLong(14, is_downloaded)
          bindString(15, local_file_path)
          bindLong(16, bitrate_kbps)
          bindLong(17, added_timestamp_ms)
        }
    notifyQueries(-888_760_349) { emit ->
      emit("tracks")
    }
  }

  public fun updateTrackMetadataPreserveFlags(
    title: String,
    artistName: String,
    artistId: String,
    albumTitle: String,
    albumId: String,
    coverUrl: String,
    durationSeconds: Long,
    previewUrl: String,
    genresCsv: String,
    releaseYear: Long?,
    rank: Long,
    videoId: String?,
    id: String,
  ) {
    driver.execute(-1_876_763_509, """
        |UPDATE tracks SET
        |    title = ?,
        |    artist_name = ?,
        |    artist_id = ?,
        |    album_title = ?,
        |    album_id = ?,
        |    cover_url = CASE WHEN ? != '' THEN ? ELSE cover_url END,
        |    duration_seconds = CASE WHEN ? > 0 THEN ? ELSE duration_seconds END,
        |    preview_url = CASE WHEN ? != '' THEN ? ELSE preview_url END,
        |    genres_csv = CASE WHEN ? != '' THEN ? ELSE genres_csv END,
        |    release_year = COALESCE(?, release_year),
        |    rank = CASE WHEN ? > 0 THEN ? ELSE rank END,
        |    video_id = COALESCE(?, video_id)
        |WHERE id = ?
        """.trimMargin(), 18) {
          bindString(0, title)
          bindString(1, artistName)
          bindString(2, artistId)
          bindString(3, albumTitle)
          bindString(4, albumId)
          bindString(5, coverUrl)
          bindString(6, coverUrl)
          bindLong(7, durationSeconds)
          bindLong(8, durationSeconds)
          bindString(9, previewUrl)
          bindString(10, previewUrl)
          bindString(11, genresCsv)
          bindString(12, genresCsv)
          bindLong(13, releaseYear)
          bindLong(14, rank)
          bindLong(15, rank)
          bindString(16, videoId)
          bindString(17, id)
        }
    notifyQueries(-1_876_763_509) { emit ->
      emit("tracks")
    }
  }

  public fun updateTrackLiked(
    is_liked: Long,
    added_timestamp_ms: Long,
    id: String,
  ) {
    driver.execute(550_749_580,
        """UPDATE tracks SET is_liked = ?, added_timestamp_ms = ? WHERE id = ?""", 3) {
          bindLong(0, is_liked)
          bindLong(1, added_timestamp_ms)
          bindString(2, id)
        }
    notifyQueries(550_749_580) { emit ->
      emit("tracks")
    }
  }

  public fun updateTrackDownloaded(
    local_file_path: String?,
    bitrate_kbps: Long?,
    added_timestamp_ms: Long,
    id: String,
  ) {
    driver.execute(713_794_728,
        """UPDATE tracks SET is_downloaded = 1, local_file_path = ?, bitrate_kbps = ?, added_timestamp_ms = ? WHERE id = ?""",
        4) {
          bindString(0, local_file_path)
          bindLong(1, bitrate_kbps)
          bindLong(2, added_timestamp_ms)
          bindString(3, id)
        }
    notifyQueries(713_794_728) { emit ->
      emit("tracks")
    }
  }

  public fun clearTrackDownloaded(id: String) {
    driver.execute(-1_465_167_098,
        """UPDATE tracks SET is_downloaded = 0, local_file_path = NULL, bitrate_kbps = NULL WHERE id = ?""",
        1) {
          bindString(0, id)
        }
    notifyQueries(-1_465_167_098) { emit ->
      emit("tracks")
    }
  }

  public fun upsertSavedAlbum(
    id: String,
    title: String,
    artist_name: String,
    artist_id: String,
    cover_url: String,
    track_count: Long,
    release_year: Long?,
    saved_timestamp_ms: Long,
  ) {
    driver.execute(-691_633_800, """
        |INSERT OR REPLACE INTO saved_albums(
        |    id, title, artist_name, artist_id, cover_url, track_count, release_year, saved_timestamp_ms
        |) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 8) {
          bindString(0, id)
          bindString(1, title)
          bindString(2, artist_name)
          bindString(3, artist_id)
          bindString(4, cover_url)
          bindLong(5, track_count)
          bindLong(6, release_year)
          bindLong(7, saved_timestamp_ms)
        }
    notifyQueries(-691_633_800) { emit ->
      emit("saved_albums")
    }
  }

  public fun deleteSavedAlbum(id: String) {
    driver.execute(680_887_028, """DELETE FROM saved_albums WHERE id = ?""", 1) {
          bindString(0, id)
        }
    notifyQueries(680_887_028) { emit ->
      emit("saved_albums")
    }
  }

  public fun insertPlaylist(
    title: String,
    description: String,
    cover_gradient_index: Long,
    created_timestamp_ms: Long,
  ) {
    driver.execute(-1_717_339_476, """
        |INSERT INTO playlists(title, description, cover_gradient_index, created_timestamp_ms)
        |VALUES (?, ?, ?, ?)
        """.trimMargin(), 4) {
          bindString(0, title)
          bindString(1, description)
          bindLong(2, cover_gradient_index)
          bindLong(3, created_timestamp_ms)
        }
    notifyQueries(-1_717_339_476) { emit ->
      emit("playlists")
    }
  }

  public fun updatePlaylist(
    title: String,
    description: String,
    id: Long,
  ) {
    driver.execute(2_124_669_116,
        """UPDATE playlists SET title = ?, description = ? WHERE id = ?""", 3) {
          bindString(0, title)
          bindString(1, description)
          bindLong(2, id)
        }
    notifyQueries(2_124_669_116) { emit ->
      emit("playlists")
    }
  }

  public fun deletePlaylistById(id: Long) {
    driver.execute(-1_020_461_936, """DELETE FROM playlists WHERE id = ?""", 1) {
          bindLong(0, id)
        }
    notifyQueries(-1_020_461_936) { emit ->
      emit("playlist_tracks")
      emit("playlists")
    }
  }

  public fun insertPlaylistTrackIgnore(
    playlist_id: Long,
    track_id: String,
    position_index: Long,
  ) {
    driver.execute(-2_095_808_335, """
        |INSERT OR IGNORE INTO playlist_tracks(playlist_id, track_id, position_index)
        |VALUES (?, ?, ?)
        """.trimMargin(), 3) {
          bindLong(0, playlist_id)
          bindString(1, track_id)
          bindLong(2, position_index)
        }
    notifyQueries(-2_095_808_335) { emit ->
      emit("playlist_tracks")
    }
  }

  public fun deleteTrackFromPlaylist(playlist_id: Long, track_id: String) {
    driver.execute(-1_580_008_389,
        """DELETE FROM playlist_tracks WHERE playlist_id = ? AND track_id = ?""", 2) {
          bindLong(0, playlist_id)
          bindString(1, track_id)
        }
    notifyQueries(-1_580_008_389) { emit ->
      emit("playlist_tracks")
    }
  }

  public fun deleteAllTracksFromPlaylist(playlist_id: Long) {
    driver.execute(-945_109_639, """DELETE FROM playlist_tracks WHERE playlist_id = ?""", 1) {
          bindLong(0, playlist_id)
        }
    notifyQueries(-945_109_639) { emit ->
      emit("playlist_tracks")
    }
  }

  public fun updatePlaylistTrackPosition(
    position_index: Long,
    playlist_id: Long,
    track_id: String,
  ) {
    driver.execute(1_303_893_048,
        """UPDATE playlist_tracks SET position_index = ? WHERE playlist_id = ? AND track_id = ?""",
        3) {
          bindLong(0, position_index)
          bindLong(1, playlist_id)
          bindString(2, track_id)
        }
    notifyQueries(1_303_893_048) { emit ->
      emit("playlist_tracks")
    }
  }

  public fun upsertHistory(
    track_id: String,
    title: String,
    artist_name: String,
    artist_id: String,
    album_title: String,
    album_id: String,
    cover_url: String,
    duration_seconds: Long,
    played_timestamp_ms: Long,
  ) {
    driver.execute(1_704_657_220, """
        |INSERT OR REPLACE INTO listen_history(
        |    track_id, title, artist_name, artist_id, album_title, album_id,
        |    cover_url, duration_seconds, played_timestamp_ms
        |) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 9) {
          bindString(0, track_id)
          bindString(1, title)
          bindString(2, artist_name)
          bindString(3, artist_id)
          bindString(4, album_title)
          bindString(5, album_id)
          bindString(6, cover_url)
          bindLong(7, duration_seconds)
          bindLong(8, played_timestamp_ms)
        }
    notifyQueries(1_704_657_220) { emit ->
      emit("listen_history")
    }
  }

  public fun trimHistoryToTwenty() {
    driver.execute(-210_146_787, """
        |DELETE FROM listen_history
        |WHERE track_id NOT IN (
        |    SELECT track_id
        |    FROM listen_history
        |    ORDER BY played_timestamp_ms DESC
        |    LIMIT 20
        |)
        """.trimMargin(), 0)
    notifyQueries(-210_146_787) { emit ->
      emit("listen_history")
    }
  }

  public fun clearHistory() {
    driver.execute(772_861_128, """DELETE FROM listen_history""", 0)
    notifyQueries(772_861_128) { emit ->
      emit("listen_history")
    }
  }

  public fun upsertScore(
    entity_key: String,
    entity_type: String,
    entity_id: String,
    display_name: String,
    score: Long,
    last_updated_ms: Long,
  ) {
    driver.execute(-2_124_560_574, """
        |INSERT OR REPLACE INTO discovery_scores(
        |    entity_key, entity_type, entity_id, display_name, score, last_updated_ms
        |) VALUES (?, ?, ?, ?, ?, ?)
        """.trimMargin(), 6) {
          bindString(0, entity_key)
          bindString(1, entity_type)
          bindString(2, entity_id)
          bindString(3, display_name)
          bindLong(4, score)
          bindLong(5, last_updated_ms)
        }
    notifyQueries(-2_124_560_574) { emit ->
      emit("discovery_scores")
    }
  }

  public fun clearAllScores() {
    driver.execute(1_812_416_598, """DELETE FROM discovery_scores""", 0)
    notifyQueries(1_812_416_598) { emit ->
      emit("discovery_scores")
    }
  }

  private inner class SelectTrackByIdQuery<out T : Any>(
    public val id: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("tracks", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("tracks", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-2_008_764_608,
        """SELECT tracks.id, tracks.title, tracks.artist_name, tracks.artist_id, tracks.album_title, tracks.album_id, tracks.cover_url, tracks.duration_seconds, tracks.preview_url, tracks.genres_csv, tracks.release_year, tracks.rank, tracks.video_id, tracks.is_liked, tracks.is_downloaded, tracks.local_file_path, tracks.bitrate_kbps, tracks.added_timestamp_ms FROM tracks WHERE id = ?""",
        mapper, 1) {
      bindString(0, id)
    }

    override fun toString(): String = "IvidsMusic.sq:selectTrackById"
  }

  private inner class SelectPlaylistByIdQuery<out T : Any>(
    public val id: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("playlists", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("playlists", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_101_806_177,
        """SELECT playlists.id, playlists.title, playlists.description, playlists.cover_gradient_index, playlists.created_timestamp_ms FROM playlists WHERE id = ?""",
        mapper, 1) {
      bindLong(0, id)
    }

    override fun toString(): String = "IvidsMusic.sq:selectPlaylistById"
  }

  private inner class SelectTracksForPlaylistQuery<out T : Any>(
    public val playlist_id: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("tracks", "playlist_tracks", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("tracks", "playlist_tracks", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(2_134_391_958, """
    |SELECT t.id, t.title, t.artist_name, t.artist_id, t.album_title, t.album_id, t.cover_url, t.duration_seconds, t.preview_url, t.genres_csv, t.release_year, t.rank, t.video_id, t.is_liked, t.is_downloaded, t.local_file_path, t.bitrate_kbps, t.added_timestamp_ms FROM tracks t
    |INNER JOIN playlist_tracks pt ON t.id = pt.track_id
    |WHERE pt.playlist_id = ?
    |ORDER BY pt.position_index ASC
    """.trimMargin(), mapper, 1) {
      bindLong(0, playlist_id)
    }

    override fun toString(): String = "IvidsMusic.sq:selectTracksForPlaylist"
  }

  private inner class SelectMaxPositionInPlaylistQuery<out T : Any>(
    public val playlist_id: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("playlist_tracks", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("playlist_tracks", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(724_992_935,
        """SELECT COALESCE(MAX(position_index), -1) FROM playlist_tracks WHERE playlist_id = ?""",
        mapper, 1) {
      bindLong(0, playlist_id)
    }

    override fun toString(): String = "IvidsMusic.sq:selectMaxPositionInPlaylist"
  }

  private inner class SelectScoreByKeyQuery<out T : Any>(
    public val entity_key: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("discovery_scores", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("discovery_scores", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_718_212_621,
        """SELECT discovery_scores.entity_key, discovery_scores.entity_type, discovery_scores.entity_id, discovery_scores.display_name, discovery_scores.score, discovery_scores.last_updated_ms FROM discovery_scores WHERE entity_key = ?""",
        mapper, 1) {
      bindString(0, entity_key)
    }

    override fun toString(): String = "IvidsMusic.sq:selectScoreByKey"
  }
}
