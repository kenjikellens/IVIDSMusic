# 🎵 IVIDS Music — Architecture & Documentation

**IVIDS Music** is a high-performance, ad-free music streaming application featuring a **dual-target architecture**:
1. **Native Android App (Mobile & Android TV)**: Built with **Kotlin**, **Jetpack Compose**, **Jetpack Media3 (ExoPlayer)**, and **Room Database**.
2. **PC Desktop & Web App (Electron & Browser)**: Built with standalone HTML5/CSS3/JS, housed in the [`pc/`](file:///c:/Users/kenji/AndroidStudioProjects/IVIDSMusic/pc/) directory and launched via `run_pc.py` or Electron (`main.js`).

---

## 📚 Documentation Index

### 🚀 Migration & Architecture Guide
- 📖 [**Migration Roadmap**](file:///c:/Users/kenji/AndroidStudioProjects/IVIDSMusic/documentation/migration_plan.md) — Phased architecture status and PC web asset separation guide.

### 🎨 UI & Screens Specifications
- 📱 [**Native UI & Screen Specifications**](file:///c:/Users/kenji/AndroidStudioProjects/IVIDSMusic/documentation/ui/screens.md) — Detailed layout, state management, and Jetpack Compose component specs for all application screens:
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

## 🛠️ Project Structure & Target Tech Stack

| Layer / Target | Technology | Location & Purpose |
|---|---|---|
| **Native Mobile App** | Kotlin 2.x + Jetpack Compose | [`app/src/mobile/`](file:///c:/Users/kenji/AndroidStudioProjects/IVIDSMusic/app/src/mobile/) — Full Compose Single-Activity UI |
| **Shared Android Logic** | Room + Media3 ExoPlayer + Retrofit | [`app/src/main/`](file:///c:/Users/kenji/AndroidStudioProjects/IVIDSMusic/app/src/main/) — Domain models, DAOs, repositories, player service |
| **PC Desktop / Dev Server** | Electron + Python (`run_pc.py`) | [`pc/`](file:///c:/Users/kenji/AndroidStudioProjects/IVIDSMusic/pc/) — Web UI assets (`pc/gui/`, `pc/logic/`) for PC executable |
| **UI Components** | OOP BaseTile Hierarchy | [`ui/component/tile/`](file:///c:/Users/kenji/AndroidStudioProjects/IVIDSMusic/app/src/main/java/com/kenjigames/ividsmusic/ui/component/tile/) — `BaseTile` -> `SongTile`, `ArtistTile`, `AlbumTile` |
| **Audio Engine** | Jetpack Media3 Session + SimpleCache | [`player/`](file:///c:/Users/kenji/AndroidStudioProjects/IVIDSMusic/app/src/main/java/com/kenjigames/ividsmusic/player/) — Background playback, 500MB LRU disk cache |
| **Persistence** | Room DB + DataStore | [`data/`](file:///c:/Users/kenji/AndroidStudioProjects/IVIDSMusic/app/src/main/java/com/kenjigames/ividsmusic/data/) — Liked tracks, history (20 cap), playlists, scores, preferences |

---

## 🔒 Privacy & Core Principles

- 🚫 **No User Accounts or Login**
- 🚫 **Zero Analytics or Telemetry Tracking**
- 🚫 **No Advertisements Ever**
- 🔐 **100% On-Device Local Data Storage**
