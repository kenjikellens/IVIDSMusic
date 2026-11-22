package com.kenjigames.ividsmusic.`data`.db

import kotlin.Long
import kotlin.String

public data class Playlist_tracks(
  public val playlist_id: Long,
  public val track_id: String,
  public val position_index: Long,
)
