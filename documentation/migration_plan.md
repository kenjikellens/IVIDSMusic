# 🚀 Native Android Migration Roadmap

This document outlines the step-by-step roadmap for migrating **IVIDS Music** from its legacy hybrid web shell (HTML/CSS/JS in WebView) to a pure, native **Android application** written in **Kotlin** using **Jetpack Compose**, **Jetpack Media3 (ExoPlayer)**, and **Room Database**.

---

## 🎯 Migration Goals
1. **Remove Web Overhead**: Eliminate the `WebView`, HTML5 templates, vanilla JavaScript modules, and CSS files.
2. **Native Performance**: Achieve 60/120 FPS UI rendering, smooth animations, instant startup times, and reduced memory usage.
3. **Robust Background Audio**: Replace HTML5 `<audio>` playback with a dedicated Android `MediaSessionService` using Jetpack Media3 ExoPlayer for seamless background listening and system notification controls.
4. **Structured Persistence**: Replace `localStorage` and `IndexedDB` with typed Room SQLite entities and DataStore preferences.
5. **Unified Codebase**: Focus exclusively on Android (Mobile & Android TV flavors).

---

## 📅 Migration Phases

### Phase 1: Architecture & Dependencies Setup 🏗️
- Add Kotlin dependencies to `build.gradle.kts` (libs.versions.toml):
  - Jetpack Compose & Compose Material3 / Compose TV
  - Navigation Compose
  - Jetpack Media3 (ExoPlayer, MediaSession)
  - Room DB (Runtime, KSP compiler, Kotlin Coroutines extensions)
  - DataStore Preferences
  - Retrofit + OkHttp + Kotlinx Serialization
  - Coil Compose (for image loading & palette extraction)
  - Hilt / Koin for Dependency Injection
- Establish clean architectural layers:
  - `data/`: Repositories, DAOs, DTOs, DataSources (Network & Local)
  - `domain/`: Use cases, Model data classes, Business logic
  - `ui/`: Compose screens, ViewModels, UI state definitions, Theme/Design System

### Phase 2: Core Data Layer & Network Models 💾
- **Data Models**: Define Kotlin `@Serializable` data classes for `Track`, `Artist`, `Album`, `Playlist`, `Genre`.
- **Network Services**:
  - Build `DeezerApiService` interface using Retrofit.
  - Build `InvidiousStreamService` for resolving YouTube video IDs and audio stream URLs.
- **Room Database (`AppDatabase`)**:
  - Create entities: `TrackEntity`, `PlaylistEntity`, `PlaylistTrackCrossRef`, `HistoryEntity`, `DiscoveryScoreEntity`.
  - Create DAOs: `TrackDao`, `PlaylistDao`, `HistoryDao`, `RecommendationDao`.
- **DataStore**: Implement `UserPreferencesRepository` for UI Scale Factor, Theme Accent, and Selected Language.

### Phase 3: Audio Playback Engine (Media3 Service) 🎶
- Implement `PlaybackService` extending `MediaSessionService`.
- Configure `ExoPlayer` instance with caching (`SimpleCache` + `LeastRecentlyUsedCacheEvictor`) for audio streams.
- Connect `MediaSession` to publish system notifications (play, pause, next, prev, artwork, lockscreen controls).
- Build `PlayerRepository` and `PlayerViewModel` exposing a unified `PlayerUiState` StateFlow to Compose UI components.

### Phase 4: Jetpack Compose UI & Screen Implementation 🎨
- Build the **Design System**:
  - `Theme.kt`: Dark glassmorphism theme, dynamic palette colors, custom fonts.
  - Reusable Components: `MusicCard`, `ArtistCard`, `TrackListItem`, `HorizontalCardRow`, `ShimmerSkeleton`, `PlayerBottomBar`, `TopSearchBar`.
- Build the 11 Screen Composables:
  1. `HomeScreen`
  2. `SearchScreen`
  3. `ArtistDetailScreen`
  4. `AlbumDetailScreen`
  5. `SongDetailScreen`
  6. `LibraryScreen`
  7. `PlaylistDetailScreen`
  8. `DownloaderScreen`
  9. `ProfileScreen`
  10. `RecommendedScreen`
  11. `SettingsScreen`
- Set up `NavHost` for seamless Single-Activity navigation with arguments and transitions.

### Phase 5: Android TV D-Pad Navigation Support 📺
- Integrate `androidx.tv.material3` components for Android TV builds.
- Configure `FocusRequester` and `FocusRestorer` for D-pad spatial navigation.
- Ensure TV-specific focus highlighting and remote control button handling (Play/Pause key, Back key, Select key).

### Phase 6: Asset Deletion & Cleanup 🧹
- Delete legacy web directory: `app/src/main/assets/gui/` and `app/src/main/assets/logic/`.
- Remove Electron files (`main.js`, `preload.js`, `run_pc.py`, `package.json`, `node_modules`).
- Clean up WebView interception code in `MainActivity.kt` and replace with `setContent { IVIDSMusicApp() }`.
- Update Android Manifest permissions and services declarations (`PlaybackService`).
