# 🚀 Native Android Migration & Architecture Status

This document outlines the architectural evolution of **IVIDS Music** from its legacy hybrid web shell to a native **Android application** written in **Kotlin** using **Jetpack Compose**, **Jetpack Media3 (ExoPlayer)**, and **Room Database**, while preserving the web UI for PC Desktop execution in the [`pc/`](file:///c:/Users/kenji/AndroidStudioProjects/IVIDSMusic/pc/) directory.

---

## 🎯 Architecture Strategy

1. **PC Desktop Isolation**: Web UI assets (`gui/` and `logic/`) are located in `pc/`. Running `python run_pc.py` or building `IVIDSMusic_PC.exe` via Electron uses these files seamlessly.
2. **Native Mobile Experience**: The `mobile` flavor (`app/src/mobile/`) runs a 100% native **Jetpack Compose** Single-Activity interface (`MainActivity.kt`).
3. **Robust Background Audio**: Android `PlaybackService` extends `MediaSessionService` with ExoPlayer and a 500MB LRU disk cache (`AudioCacheManager`).
4. **Structured Persistence**: SQLite Room database (`AppDatabase`) manages liked tracks, custom playlists, listening history (20-item cap), and discovery scores. Settings are stored via Jetpack `DataStore`.

---

## 📅 Implementation Status

### Phase 1: Build Configuration & Dependencies ⚙️ [COMPLETED]
- Enabled Jetpack Compose in `app/build.gradle.kts` (`buildFeatures { compose = true }`).
- Added Compose BOM, Material3, Navigation Compose, Coil Compose, DataStore, and Kotlin Compose compiler plugin.

### Phase 2: Core Data Layer & Domain Models 💾 [COMPLETED]
- **Domain Models**: Defined `MusicItem` sealed class base with `Song`, `Artist`, and `Album` subtypes. Created `Playlist`, `Genre`, and `PlayerState` data classes.
- **Room Database (`AppDatabase`)**:
  - Entities: `TrackEntity`, `PlaylistEntity`, `PlaylistTrackCrossRef`, `HistoryEntity`, `DiscoveryScoreEntity`.
  - DAOs: `TrackDao`, `PlaylistDao`, `HistoryDao` (with automatic 20-item transaction cap), `DiscoveryScoreDao`.
- **DataStore**: Implemented `UserPreferencesRepository` for UI scale factor (75%-150%), active language code, and auto-update toggles.

### Phase 3: Audio Playback Engine (Media3 Service) 🎶 [COMPLETED]
- Implemented `PlaybackService` extending `MediaSessionService`.
- Configured `AudioCacheManager` using `SimpleCache` + `LeastRecentlyUsedCacheEvictor` (500MB disk cache).
- Refactored `PlaybackManager` singleton exposing a reactive `StateFlow<PlayerState>` to ViewModels and Composables.

### Phase 4: Network & Stream Resolution Strategy 🌐 [COMPLETED]
- Defined `DeezerApiService` Retrofit interface for search, charts, artist, and album queries.
- Defined `StreamResolver` strategy interface implemented by:
  - `InvidiousStreamResolver`: Rotates across a pool of public Invidious instances.
  - `YouTubeHtmlScraper`: Tertiary regex fallback on raw YouTube HTML.
- Centralized singleton network providers in `NetworkModule`.

### Phase 5: OOP Component Hierarchy & UI Design System 🎨 [COMPLETED]
- Defined `IVIDSMusicTheme` dark glassmorphism theme, typography scale, and rounded corner shapes.
- Created `BaseTile` abstract card composable extended by:
  - `SongTile`: Includes play overlay icon and duration.
  - `ArtistTile`: Clips artwork to `CircleShape` avatar with listener count.
  - `AlbumTile`: Includes track count subtitle.
- Implemented `HorizontalTileRow`, `TrackListItem`, `PlayerBottomBar`, `TopSearchBar`, and shimmer loading skeletons.

### Phase 6: Navigation & Screen Composables 📱 [COMPLETED]
- Implemented type-safe `Screen` sealed class and `NavHost` routing in `NavGraph.kt`.
- Built `BottomNavBar` with 5 primary tabs: **Home**, **Search**, **Library**, **You**, **Settings**.
- Implemented `HomeScreen`, `SearchScreen`, `LibraryScreen`, `ProfileScreen`, `SettingsScreen` and their corresponding ViewModels.

### Phase 7: PC Web Assets Separation 💻 [COMPLETED]
- Moved web files from `app/src/main/assets/` to `pc/gui/` and `pc/logic/`.
- Updated `run_pc.py` assets directory path to point to `pc/`.
- Updated `main.js` and `package.json` for Electron PC build packaging.
