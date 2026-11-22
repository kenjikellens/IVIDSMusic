package com.kenjigames.ividsmusic.`data`.db

import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.kenjigames.ividsmusic.`data`.db.`data`.newInstance
import com.kenjigames.ividsmusic.`data`.db.`data`.schema
import kotlin.Unit

public interface IvidsDatabase : Transacter {
  public val ividsMusicQueries: IvidsMusicQueries

  public companion object {
    public val Schema: SqlSchema<QueryResult.Value<Unit>>
      get() = IvidsDatabase::class.schema

    public operator fun invoke(driver: SqlDriver): IvidsDatabase =
        IvidsDatabase::class.newInstance(driver)
  }
}
