# IVIDS Music

Ad-free, privacy-first music streaming and offline download application built with Kotlin Multiplatform and Compose Multiplatform (`1.7.1`) for Windows Desktop, Android Mobile, and Android TV.

## Key Features

- **Unified Compose UI**: Adaptive borderless pure-black (`#000000` / `#090909`) interface supporting Desktop, Mobile (`< 600dp`), and TV focus navigation, with cyan (`#4DD0E1`) active indicators.
- **Discovery & Search**: Curated charts, artists, albums, personalized recommendation feeds, category filters, and release-year filtering backed by Deezer + iTunes fallback (`CompositeMusicDiscoveryRepository`).
- **Multi-Tier Audio Streaming**: Background audio resolution pipeline combining local offline lookup, NewPipeExtractor, parallel Invidious mirror racing, and portable `yt-dlp.exe` + `ffmpeg.exe` fallback on Windows.
- **Native Playback Engines**:
  - **Windows Desktop**: `DesktopAudioEngine` (JavaFX Media `17.0.12`) with `LocalHeaderProxy`, next-track prefetching, and M4A-to-MP3 disk caching (`DesktopAudioCacheTranscoder`).
  - **Android Mobile & TV**: `AndroidAudioEngine` + `AndroidPlaybackService` via Jetpack Media3 (`1.5.1`) ExoPlayer with `SimpleCache`, background service notifications, and lockscreen controls.
- **Offline Library & Storage**: SQLDelight (`IvidsMusic.sq`) for liked tracks, playlists, saved albums, listen history, and local discovery scores; DataStore Preferences for settings; atomic `session_state.json` queue restore.
- **16-Language Localization**: Instant runtime switching across 16 bundled JSON locales (`composeResources/files/lang/*.json`).

## Modules

| Module | Purpose |
|---|---|
| `:composeApp` | Shared Compose Multiplatform UI, ViewModels, shell chrome, i18n, desktop/Android entrypoints |
| `:core:domain` | Pure Kotlin domain models, repository ports, `PlaybackQueueController`, `RecommendationEngine`, `ResolveTrackStreamUseCase` |
| `:core:data` | SQLDelight database, DataStore preferences, session state persistence, Ktor API clients, stream resolvers, native downloader |
| `:core:player` | Multiplatform `AudioEngine` state machine + Media3 (`androidMain`) and JavaFX (`jvmMain`) implementations |

## Quick Start

### Run Windows Desktop
- Launch prebuilt/Gradle runner: `run_pc.bat` (or `run_pc.bat --dev` to force Gradle `:composeApp:run`)

### Run Tests & Build Binaries
```powershell
.\gradlew.bat :composeApp:desktopTest
.\gradlew.bat :composeApp:createDistributable
.\gradlew.bat :composeApp:assembleMobileDebug
.\gradlew.bat :composeApp:assembleTvDebug
```

For full release packaging (`IVIDSMusic_Mobile.apk`, `IVIDSMusic_TV.apk`, `IVIDSMusic_PC.exe`, and `dist\IVIDSMusic_PC`), run `build.bat`.

## Documentation Index

- [Documentation Overview](docs/README.md)
- [Open TODOs](docs/TODO.md) & [Feature Roadmap](FEATURES_ROADMAP.md)
- [Screen & UI Specifications](docs/ui/screens.md)
- [Audio Playback Architecture](docs/architecture/audio_playback.md)
- [Data & Local Storage](docs/architecture/data_storage.md)
- [API & Stream Integration](docs/architecture/api_integration.md)
- [Recommendation Algorithm](docs/algorithm.md)
- [Source File Inventory](docs/file_list.md)

## Core Privacy Principles

- Zero user accounts or logins
- Zero analytics or telemetry tracking
- 100% local SQLite, DataStore, and filesystem storage
