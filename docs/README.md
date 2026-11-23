# IVIDS Music - Architecture & Documentation

**IVIDS Music** is a Compose Multiplatform (`1.7.1`) / Kotlin Multiplatform (`2.0.21`) ad-free music player and offline downloader sharing domain, data, playback state, and UI across Windows Desktop, Android Mobile, and Android TV.

## Documentation Index

### UI & Planning
- [Open TODOs](TODO.md): Active P1-P2 UX backlog (legacy web/Electron tasks removed).
- [Feature & Improvement Roadmap](../FEATURES_ROADMAP.md): Full P0 (done) through P3 feature roadmap.
- [Compose Multiplatform Screen Specifications](ui/screens.md): Adaptive shell architecture, orthogonal design tokens, and all 12 navigation routes.

### Architecture & Backend Services
- [Audio Playback Engine](architecture/audio_playback.md): Multiplatform `EngineStateMachine`, Android Media3 `ExoPlayer` + `AndroidPlaybackService`, and Desktop JavaFX Media `17.0.12` + `LocalHeaderProxy`.
- [Local Data & Storage](architecture/data_storage.md): SQLDelight schema (`IvidsMusic.sq`), DataStore Preferences (`DataStorePreferencesRepository`), and atomic `session_state.json`.
- [Networking & API Integration](architecture/api_integration.md): Ktor `3.0.1` clients (`DeezerApiClient`, `ItunesApiClient`), `CompositeStreamResolver` (Local -> NewPipe -> Invidious -> `yt-dlp`), and `ColorExtractor`.
- [Recommendation Algorithm](algorithm.md): Pure-domain local interest scoring (`0..100`) and cosine-similarity feed generation (`RecommendationEngine`).
- [Source File Inventory](file_list.md): Up-to-date map of `:composeApp`, `:core:domain`, `:core:data`, and `:core:player`.

## Project Module Stack

| Module / Layer | Technology | Location & Purpose |
|---|---|---|
| `:composeApp` | Kotlin + Compose Multiplatform `1.7.1` + Coil 3 + Koin | `composeApp/src/commonMain` - Adaptive shell, borderless dark UI, 12 screens, ViewModels, 16 JSON locales |
| `:composeApp` (`desktopMain` / `androidMain`) | AWT/Swing (`#090909` host) / Android Activity (`mobile` + `tv` flavors) | Window management, `PlatformServices`, `BlurredArtworkBackdrop`, `ColorExtractor` |
| `:core:domain` | Pure Kotlin Multiplatform + Coroutines | `core/domain/src/commonMain` - Domain models, repository ports, `PlaybackQueueController`, `RecommendationEngine` |
| `:core:data` | SQLDelight `2.0.2` + DataStore `1.1.1` + Ktor `3.0.1` + NewPipeExtractor | `core/data/src/` - SQLite tables, preferences, `CompositeMusicDiscoveryRepository`, `CompositeStreamResolver`, `NativeDownloadManager` |
| `:core:player` | Media3 ExoPlayer `1.5.1` (`androidMain`) + JavaFX Media `17.0.12` (`jvmMain`) | `core/player/src/` - `EngineStateMachine`, background playback, disk caching, prefetching |

## Core Principles

- Zero user accounts or logins
- Zero analytics or telemetry tracking
- Zero advertisements
- 100% local persistence (SQLDelight + DataStore + user filesystem)
