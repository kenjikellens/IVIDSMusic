package com.kenjigames.ividsmusic.`data`.db

import kotlin.Long
import kotlin.String

public data class Playlists(
  public val id: Long,
  public val title: String,
  public val description: String,
  public val cover_gradient_index: Long,
  public val created_timestamp_ms: Long,
)
