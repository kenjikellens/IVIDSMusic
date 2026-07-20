package com.kenjigames.ividsmusic.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.kenjigames.ividsmusic.data.dao.DiscoveryScoreDao
import com.kenjigames.ividsmusic.data.dao.HistoryDao
import com.kenjigames.ividsmusic.data.dao.PlaylistDao
import com.kenjigames.ividsmusic.data.dao.TrackDao
import com.kenjigames.ividsmusic.data.entity.DiscoveryScoreEntity
import com.kenjigames.ividsmusic.data.entity.HistoryEntity
import com.kenjigames.ividsmusic.data.entity.PlaylistEntity
import com.kenjigames.ividsmusic.data.entity.PlaylistTrackCrossRef
import com.kenjigames.ividsmusic.data.entity.TrackEntity

/**
 * Main Room database singleton for IVIDS Music.
 * Manages SQLite persistence for tracks, history, playlists, and interest scores.
 */
@Database(
    entities = [
        TrackEntity::class,
        HistoryEntity::class,
        PlaylistEntity::class,
        PlaylistTrackCrossRef::class,
        DiscoveryScoreEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun trackDao(): TrackDao
    abstract fun historyDao(): HistoryDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun discoveryScoreDao(): DiscoveryScoreDao

    companion object {
        private const val DATABASE_NAME = "ivids_music_db"

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                .fallbackToDestructiveMigration()
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
                .also { instance = it }
            }
        }
    }
}
