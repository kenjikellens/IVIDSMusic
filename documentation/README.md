# 🎵 IVIDS Music — Native Android Music Architecture & Blueprint

**IVIDS Music** is a high-performance, ad-free native music streaming and discovery application built exclusively for **Android (Mobile & Android TV)** using **Jetpack Compose**, **Jetpack Media3 (ExoPlayer)**, and **Room Database**.

This documentation repository serves as the complete architectural blueprint and specifications guide for migrating from the legacy web/hybrid shell to a 100% pure native Kotlin Android application.

---

## 📚 Documentation Index

### 🚀 Migration Guide
- 📖 [**Migration Roadmap**](file:///c:/Users/kenji/AndroidStudioProjects/IVIDSMusic/documentation/migration_plan.md) — Phased plan for removing legacy web assets (HTML/CSS/JS/Electron) and transitioning to native Kotlin components.

### 🎨 UI & Screens Specifications
- 📱 [**Native UI & Screen Specifications**](file:///c:/Users/kenji/AndroidStudioProjects/IVIDSMusic/documentation/ui/screens.md) — Detailed layout, state management, and Jetpack Compose component specs for all 11 application screens:
  1. **Home Screen** — Curated genre rows, shimmer skeletons, hero cards, recommendations.
  2. **Search Screen** — Hero search, category tabs (Songs, Artists, Albums), release year range filtering, pagination.
  3. **Artist Detail Screen** — Header banner, top songs, full album discography.
  4. **Album Detail Screen** — Tracklist, batch controls ("Play All", "Shuffle"), artist links.
  5. **Song Detail Screen** — Full-screen cover art, playback controls, playlist actions, download button.
  6. **Library Screen** — Collection tabs (Liked Songs, Saved Albums, Playlists), client-side search.
  7. **Playlist Detail Screen** — Custom playlist management, cover editing, reordering, track removal.
  8. **Downloader & Saved Media Screen** — Direct audio download manager, offline track browser, storage diagnostics.
  9. **Profile ("You") Screen** — User stats, avatar customization, last 20 listened tracks history, top genres.
  10. **Recommended Screen** — Algorithmic discovery feed based on local interest scores.
  11. **Settings Screen** — UI scale factor, 13-language i18n picker, native app self-updater, cache clear.

### 🏗️ Architecture & Backend Services
- 🎶 [**Audio Playback Engine**](file:///c:/Users/kenji/AndroidStudioProjects/IVIDSMusic/documentation/architecture/audio_playback.md) — Jetpack Media3 ExoPlayer integration, background audio service, media notifications, lockscreen controls, and Invidious audio stream resolution.
- 💾 [**Local Data & Storage**](file:///c:/Users/kenji/AndroidStudioProjects/IVIDSMusic/documentation/architecture/data_storage.md) — Room DB entities and DAOs (Playlists, History, Liked Tracks, Interest Scores) and DataStore preferences.
- 🌐 [**Networking & API Integration**](file:///c:/Users/kenji/AndroidStudioProjects/IVIDSMusic/documentation/architecture/api_integration.md) — Retrofit/OkHttp network layer for Deezer API discovery, iTunes fallback, and Invidious proxy scraping.
- 📊 [**Recommendation Algorithm**](file:///c:/Users/kenji/AndroidStudioProjects/IVIDSMusic/documentation/algorithm.md) — On-device local interest scoring system for personalized recommendations.

---

## 🛠️ Target Tech Stack

| Component | Technology | Purpose |
|---|---|---|
| **Language** | Kotlin 2.x | Primary language for all app logic & UI |
| **UI Framework** | Jetpack Compose | Declarative UI for Mobile & Android TV |
| **Navigation** | Navigation Compose | Type-safe single-activity screen navigation |
| **Audio Playback** | Jetpack Media3 (ExoPlayer) | Foreground/background service, audio streaming, caching |
| **Database** | Room (SQLite) | Local persistence for playlists, history, liked tracks & scoring |
| **Preferences** | Jetpack DataStore | Async settings persistence (UI scale, language, theme) |
| **Networking** | Retrofit + OkHttp + Kotlinx Serialization | Deezer metadata & Invidious stream extraction |
| **Image Loading** | Coil Compose | Async image loading & canvas palette extraction |
| **Dependency Injection** | Hilt / Koin | Modular architecture & viewmodel injection |
| **TV Spatial Navigation** | Compose TV / FocusRestorer | D-pad remote navigation for Android TV |

---

## 🔒 Privacy & Core Principles

- 🚫 **No User Accounts or Login**
- 🚫 **Zero Analytics or Telemetry Tracking**
- 🚫 **No Advertisements Ever**
- 🔐 **100% On-Device Local Data Storage**
