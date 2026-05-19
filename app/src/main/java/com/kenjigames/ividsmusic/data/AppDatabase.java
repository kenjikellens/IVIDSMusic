package com.kenjigames.ividsmusic.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * Main Room database class for the application.
 * Manages the SQLite database instance and provides DAO accessors.
 */
@Database(entities = {Track.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "ivids_music_db";
    private static volatile AppDatabase instance;

    /**
     * Exposes the Track Data Access Object (DAO).
     *
     * @return The TrackDao instance.
     */
    public abstract TrackDao trackDao();

    /**
     * Retrieves the thread-safe singleton instance of the Room database.
     * Uses double-checked locking to ensure single instance instantiation.
     *
     * @param context The Android context.
     * @return The AppDatabase singleton instance.
     */
    public static AppDatabase getInstance(final Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME
                    )
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return instance;
    }
}
