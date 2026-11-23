# Local Data & Storage Architecture (SQLDelight & DataStore)

**IVIDS Music** stores library metadata, preferences, playback sessions, recommendation scores, and downloaded audio locally via `:core:data`.

## Storage Stack Overview

| Data Type | Storage Mechanism | Implementation |
|---|---|---|
| Tracks, Likes, Playlists, Saved Albums | SQLDelight (`2.0.2`) SQLite DB (`IvidsMusic.sq`) | `SqlLibraryRepository` |
| Listen History | SQLDelight (`listen_history`) | `SqlLibraryRepository` |
| Discovery Interest Scores | SQLDelight (`discovery_scores`, `0..100` clamped) | `SqlScoreRepository` |
| User Settings & Local Profile | AndroidX DataStore Preferences (`1.1.1`) | `DataStorePreferencesRepository` |
| Playback Queue & Cursor Restore | Atomic JSON file (`session_state.json`) | `FilePlaybackSessionRepository` |
| Offline Audio Files | Local app/music directories (`AppStorageDirectories`) | `NativeDownloadManager` |

## SQLDelight Schema (`IvidsMusic.sq`)

Located at `core/data/src/commonMain/sqldelight/com/kenjigames/ividsmusic/data/db/IvidsMusic.sq`:

- **`tracks`**: Stores `id`, `title`, `artist_name`, `artist_id`, `album_title`, `album_id`, `cover_url`, `duration_seconds`, `preview_url`, `genres_csv`, `release_year`, `rank`, `video_id`, `is_liked`, `is_downloaded`, `local_file_path`, `bitrate_kbps`, and `added_timestamp_ms`.
- **`playlists` & `playlist_tracks`**: User playlists (`title`, `description`, `cover_gradient_index`) linked to tracks with ordered `position_index` and cascading deletes.
- **`saved_albums`**: Saved album cards with cover, track count, and release year.
- **`listen_history`**: Persistent playback history sorted by `played_timestamp_ms`.
- **`discovery_scores`**: Tracks entity weights (`entity_key`, `entity_type`, `entity_id`, `display_name`, `score` constrained `CHECK (score >= 0 AND score <= 100)`, `last_updated_ms`).

Uses SQLDelight Android driver (`sqldelight-android-driver`) on Android and JVM SQLite driver (`sqldelight-sqlite-driver`) on Desktop.

## DataStore Preferences (`DataStorePreferencesRepository.kt`)

Persists reactive `Flow<UserPreferences>` and `Flow<UserProfile>` without blocking the UI thread:

- **`download_quality_kbps`**: Preferred download bitrate (`128`, `196`, or `320` kbps; default `196`).
- **`ui_scale_factor`**: UI scaling clamped within `0.75f..1.50f` in 5% increments.
- **`language_code`**: Active locale (`en`, `nl`, `de`, `fr`, `es`, `it`, `pt`, `ro`, `ru`, `zh`, `hi`, `ar`, `ja`, `ko`, `pl`, `tr`).
- **`dynamic_tint_enabled`**: Ambient cover-art tint toggle.
- **`layout_mode`**: Persisted `LayoutMode` enum (`AUTO`, `MOBILE`, `DESKTOP`, `TV`).
- **`update_mode` & `auto_update_enabled`**: Self-updater mode (`AUTO`, `MANUAL`, `OFF`, `DEVELOPER`).
- **`playback_volume` & `playback_muted`**: Restored to `AudioEngine` on app launch.
- **`profile_display_name`, `profile_bio`, `total_listening_seconds`**: Local listener profile stats.
- **`custom_save_directory` & `ytdlp_last_check_ms`**: Offline download destination override and native tool update check timestamp.

## Playback Session Persistence (`FilePlaybackSessionRepository.kt`)

- Atomically writes/reads `session_state.json` (queue, `currentIndex`, `positionMs`, `isShuffleEnabled`, `repeatMode`, `updatedAtMs`).
- Ignores missing or corrupt JSON safely so startup restores paused playback without auto-playing unprompted.
