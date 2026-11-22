package com.kenjigames.ividsmusic.`data`.db

import kotlin.Long
import kotlin.String

public data class Saved_albums(
  public val id: String,
  public val title: String,
  public val artist_name: String,
  public val artist_id: String,
  public val cover_url: String,
  public val track_count: Long,
  public val release_year: Long?,
  public val saved_timestamp_ms: Long,
)
