package com.kenjigames.ividsmusic.`data`.db.`data`

import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.kenjigames.ividsmusic.`data`.db.IvidsDatabase
import com.kenjigames.ividsmusic.`data`.db.IvidsMusicQueries
import kotlin.Long
import kotlin.Unit
import kotlin.reflect.KClass

internal val KClass<IvidsDatabase>.schema: SqlSchema<QueryResult.Value<Unit>>
  get() = IvidsDatabaseImpl.Schema

internal fun KClass<IvidsDatabase>.newInstance(driver: SqlDriver): IvidsDatabase =
    IvidsDatabaseImpl(driver)

private class IvidsDatabaseImpl(
  driver: SqlDriver,
) : TransacterImpl(driver), IvidsDatabase {
  override val ividsMusicQueries: IvidsMusicQueries = IvidsMusicQueries(driver)

  public object Schema : SqlSchema<QueryResult.Value<Unit>> {
    override val version: Long
      get() = 1

    override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
      driver.execute(null, """
          |CREATE TABLE tracks (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    title TEXT NOT NULL,
          |    artist_name TEXT NOT NULL,
          |    artist_id TEXT NOT NULL DEFAULT '',
          |    album_title TEXT NOT NULL DEFAULT '',
          |    album_id TEXT NOT NULL DEFAULT '',
          |    cover_url TEXT NOT NULL DEFAULT '',
          |    duration_seconds INTEGER NOT NULL DEFAULT 0,
          |    preview_url TEXT NOT NULL DEFAULT '',
          |    genres_csv TEXT NOT NULL DEFAULT '',
          |    release_year INTEGER,
          |    rank INTEGER NOT NULL DEFAULT 0,
          |    video_id TEXT,
          |    is_liked INTEGER NOT NULL DEFAULT 0,
          |    is_downloaded INTEGER NOT NULL DEFAULT 0,
          |    local_file_path TEXT,
          |    bitrate_kbps INTEGER,
          |    added_timestamp_ms INTEGER NOT NULL DEFAULT 0
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE playlists (
          |    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
          |    title TEXT NOT NULL,
          |    description TEXT NOT NULL DEFAULT '',
          |    cover_gradient_index INTEGER NOT NULL DEFAULT 0,
          |    created_timestamp_ms INTEGER NOT NULL
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE playlist_tracks (
          |    playlist_id INTEGER NOT NULL REFERENCES playlists(id) ON DELETE CASCADE,
          |    track_id TEXT NOT NULL REFERENCES tracks(id) ON DELETE CASCADE,
          |    position_index INTEGER NOT NULL,
          |    PRIMARY KEY (playlist_id, track_id)
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE listen_history (
          |    track_id TEXT NOT NULL PRIMARY KEY,
          |    title TEXT NOT NULL,
          |    artist_name TEXT NOT NULL,
          |    artist_id TEXT NOT NULL DEFAULT '',
          |    album_title TEXT NOT NULL DEFAULT '',
          |    album_id TEXT NOT NULL DEFAULT '',
          |    cover_url TEXT NOT NULL DEFAULT '',
          |    duration_seconds INTEGER NOT NULL DEFAULT 0,
          |    played_timestamp_ms INTEGER NOT NULL
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE discovery_scores (
          |    entity_key TEXT NOT NULL PRIMARY KEY,
          |    entity_type TEXT NOT NULL,
          |    entity_id TEXT NOT NULL,
          |    display_name TEXT NOT NULL,
          |    score INTEGER NOT NULL DEFAULT 0 CHECK (score >= 0 AND score <= 100),
          |    last_updated_ms INTEGER NOT NULL
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE saved_albums (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    title TEXT NOT NULL,
          |    artist_name TEXT NOT NULL,
          |    artist_id TEXT NOT NULL DEFAULT '',
          |    cover_url TEXT NOT NULL DEFAULT '',
          |    track_count INTEGER NOT NULL DEFAULT 0,
          |    release_year INTEGER,
          |    saved_timestamp_ms INTEGER NOT NULL
          |)
          """.trimMargin(), 0)
      driver.execute(null,
          "CREATE INDEX idx_playlist_tracks_order ON playlist_tracks(playlist_id, position_index)",
          0)
      return QueryResult.Unit
    }

    override fun migrate(
      driver: SqlDriver,
      oldVersion: Long,
      newVersion: Long,
      vararg callbacks: AfterVersion,
    ): QueryResult.Value<Unit> = QueryResult.Unit
  }
}
