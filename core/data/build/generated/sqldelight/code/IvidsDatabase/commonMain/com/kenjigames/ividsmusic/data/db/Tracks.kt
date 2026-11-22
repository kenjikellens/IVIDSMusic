package com.kenjigames.ividsmusic.`data`.db

import kotlin.Long
import kotlin.String

public data class Tracks(
  public val id: String,
  public val title: String,
  public val artist_name: String,
  public val artist_id: String,
  public val album_title: String,
  public val album_id: String,
  public val cover_url: String,
  public val duration_seconds: Long,
  public val preview_url: String,
  public val genres_csv: String,
  public val release_year: Long?,
  public val rank: Long,
  public val video_id: String?,
  public val is_liked: Long,
  public val is_downloaded: Long,
  public val local_file_path: String?,
  public val bitrate_kbps: Long?,
  public val added_timestamp_ms: Long,
)
