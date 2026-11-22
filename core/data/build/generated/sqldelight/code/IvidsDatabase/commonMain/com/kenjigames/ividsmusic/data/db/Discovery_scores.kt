package com.kenjigames.ividsmusic.`data`.db

import kotlin.Long
import kotlin.String

public data class Discovery_scores(
  public val entity_key: String,
  public val entity_type: String,
  public val entity_id: String,
  public val display_name: String,
  public val score: Long,
  public val last_updated_ms: Long,
)
