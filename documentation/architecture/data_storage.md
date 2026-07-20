# 💾 Local Data & Storage Architecture (Room & DataStore)

This document defines the database entities, DAOs, and DataStore preference schemas for **IVIDS Music** in Kotlin.

---

## 🏛️ Storage Stack Overview

| Data Type | Storage Solution | Scope |
|---|---|---|
| **Liked Songs, Saved Albums, Custom Playlists** | **Room Database (SQLite)** | Persistent relational storage |
| **Listening History (Last 20 tracks)** | **Room Database (SQLite)** | Historical playback log |
| **Recommendation Interest Scores** | **Room Database (SQLite)** | Local discovery scoring matrix |
| **UI Scale Factor, Theme Accent, Language** | **Jetpack DataStore** | Async Key-Value preferences |
| **Downloaded Offline Audio Files** | **Android App Storage (`Context.filesDir`)** | Offline MP3 file storage |

---

## 🗄️ Room Database Schema (`AppDatabase.kt`)

### 1. `TrackEntity`
Stores metadata for tracks saved, liked, or downloaded locally.
```kotlin
@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: String, // Deezer or YouTube track ID
    val title: String,
    val artistName: String,
    val artistId: String,
    val albumTitle: String,
    val albumId: String,
    val coverUrl: String,
    val durationSeconds: Int,
    val isLiked: Boolean = false,
    val isDownloaded: Boolean = false,
    val localFilePath: String? = null,
    val addedTimestamp: Long = System.currentTimeMillis()
)
```

### 2. `PlaylistEntity` & `PlaylistTrackCrossRef`
Custom user playlists and many-to-many relationship mapping.
```kotlin
@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val playlistId: Long = 0,
    val title: String,
    val description: String? = null,
    val coverImagePath: String? = null,
    val createdTimestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_track_cross_ref",
    primaryKeys = ["playlistId", "trackId"]
)
data class PlaylistTrackCrossRef(
    val playlistId: Long,
    val trackId: String,
    val positionInPlaylist: Int
)
```

### 3. `HistoryEntity`
Maintains the last 20 played tracks.
```kotlin
@Entity(tableName = "listen_history")
data class HistoryEntity(
    @PrimaryKey val trackId: String,
    val title: String,
    val artistName: String,
    val coverUrl: String,
    val playedTimestamp: Long = System.currentTimeMillis()
)
```

### 4. `DiscoveryScoreEntity`
Tracks local interest scores for artists, tracks, and genres.
```kotlin
@Entity(tableName = "discovery_scores")
data class DiscoveryScoreEntity(
    @PrimaryKey val entityKey: String, // e.g. "artist:123" or "genre:pop"
    val entityType: String, // "TRACK", "ARTIST", "GENRE"
    val score: Int = 0, // Clamped between 0 and 100
    val lastUpdated: Long = System.currentTimeMillis()
)
```

---

## 🔑 DataAccessObjects (DAOs)

- `TrackDao`: CRUD operations for liked songs, downloaded tracks, and search indexing.
- `PlaylistDao`: Create, update, delete playlists; insert/remove tracks; reorder track positions.
- `HistoryDao`: Insert history items, enforce 20-item cap via `@Transaction`, query recently played.
- `RecommendationDao`: Increment/decrement interest scores, retrieve top-scoring artists/genres for recommendation expansion.

---

## ⚙️ DataStore Preferences (`UserPreferencesRepository.kt`)

Stored asynchronously using `PreferencesDataStore`:

```kotlin
object PreferencesKeys {
    val UI_SCALE_FACTOR = floatPreferencesKey("ui_scale_factor") // Default: 1.0f (100%)
    val LANGUAGE_CODE = stringPreferencesKey("language_code") // Default: "en"
    val THEME_ACCENT_COLOR = intPreferencesKey("theme_accent_color") // Default: HSL Tint
    val AUTO_UPDATE_ENABLED = booleanPreferencesKey("auto_update_enabled") // Default: true
}
```
Exposed via `Flow<UserPreferences>` for instant UI updates when settings change.
