package com.kenjigames.ividsmusic.`data`.db

import kotlin.Long
import kotlin.String

public data class Listen_history(
  public val track_id: String,
  public val title: String,
  public val artist_name: String,
  public val artist_id: String,
  public val album_title: String,
  public val album_id: String,
  public val cover_url: String,
  public val duration_seconds: Long,
  public val played_timestamp_ms: Long,
)
