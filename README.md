# 🎵 IVIDS Music — Ad-Free Music for Android

**IVIDS Music** is a sleek, native Android music streaming app that lets you search for artists, albums, and songs — and play them **completely ad-free**. No subscriptions, no interruptions, just music.

Built with a premium dark glassmorphism UI, it combines the power of the **Deezer API** for music discovery with **Invidious** (a privacy-friendly YouTube proxy) for audio streaming. Everything runs natively on your phone — no external server required.

---

## ✨ Features

- 🔍 **Smart Search** — Search for songs, artists, and albums powered by Deezer
- 🎵 **Ad-Free Streaming** — Audio is streamed via Invidious with zero ads
- 🎨 **Premium Dark UI** — Glassmorphism design with smooth animations and vibrant colors
- 📱 **Fully Native** — Runs entirely on your Android device, no PC or server needed
- 🏠 **Home Page** — Curated genre rows (Pop, Rock, Hip-Hop, Hardcore, 90's, Electronic)
- 🔎 **Browse & Discover** — Search by artist, song, or album with category filtering
- 💾 **Track Persistence** — Remembers your last played song and your listen history
- 🎤 **Artist Pages** — Dedicated artist views with real images from Deezer
- 🎛️ **Persistent Player Bar** — Full playback controls with progress slider and volume
- 🌙 **Screensaver Mode** — Auto-activates after 60 seconds of inactivity
- ⚙️ **Settings** — UI scale factor (75% to 150%) for accessibility
- 👤 **You Page** — Your profile with listen stats, recently played, and top genres

---

## 🏗️ Architecture

IVIDS Music uses a **hybrid architecture**: a native Android shell (Kotlin) wrapping a high-quality web-based UI (HTML/CSS/JS).

### How It Works

```
┌─────────────────────────────────────────────┐
│              Android App (Kotlin)            │
│                                              │
│  ┌────────────────────────────────────────┐  │
│  │           WebView (Full Screen)        │  │
│  │   HTML/CSS/JS UI loaded from assets/   │  │
│  │                                        │  │
│  │   ┌──────────┐    ┌────────────────┐   │  │
│  │   │  Search   │    │  Play a Song   │   │  │
│  │   └─────┬─────┘    └───────┬────────┘   │  │
│  └─────────┼───────────────────┼────────────┘  │
│            │ fetch()           │ fetch()        │
│            ▼                   ▼                │
│  ┌──────────────────────────────────────────┐  │
│  │    shouldInterceptRequest() (Kotlin)      │  │
│  │  • /proxy?url=...  → Deezer API (OkHttp) │  │
│  │  • /play?videoId=  → Invidious (OkHttp)  │  │
│  └──────────────────────────────────────────┘  │
│            │                   │                │
│            ▼                   ▼                │
│     Deezer API            Invidious API         │
│   (Music Metadata)      (Audio Stream URL)      │
└─────────────────────────────────────────────┘
```

### The Key Trick: Native Request Interception

The JavaScript code thinks it's talking to a local server at `http://localhost:3000`. But there is no server — Android's `WebViewClient.shouldInterceptRequest()` catches these requests **before they leave the device** and handles them natively in Kotlin:

1. **`/proxy?url=...`** — Fetches Deezer API requests via OkHttp, bypassing CORS entirely.
2. **`/play?videoId=...`** — Queries Invidious for an audio-only stream URL, returned to the HTML5 `<audio>` element.

---

## 🛠️ Tech Stack

| Layer | Technology | Purpose |
|---|---|---|
| **Native Shell** | Kotlin + Android WebView | App container, request interception |
| **Networking** | OkHttp | Fast, reliable HTTP from Kotlin |
| **Frontend UI** | HTML5, Vanilla CSS, Vanilla JS (ES Modules) | The entire user interface |
| **Music Discovery** | Deezer API | Search, album art, genre browsing |
| **Audio Playback** | Invidious API + HTML5 `<audio>` | YouTube audio extraction & playback |
| **Persistence** | localStorage (WebView) | Last played track, listen history, settings |

---

## 📁 Project Structure

```
IVIDSMusic/
├── app/
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/kenjigames/ividsmusic/
│       │   └── MainActivity.kt          # WebView setup + request interception
│       └── assets/
│           ├── gui/
│           │   ├── index.html           # App shell (sidebar, header, player bar)
│           │   ├── index.css            # Full design system (dark, glassmorphism)
│           │   ├── loader.css           # Spinner animations
│           │   └── pages/
│           │       ├── home.html        # Genre rows
│           │       ├── search.html      # Search results
│           │       ├── artist.html      # Artist detail
│           │       ├── library.html     # User library
│           │       ├── profile.html     # 'You' page (stats, history, genres)
│           │       └── settings.html    # App settings (scale factor, etc.)
│           └── logic/
│               ├── config.js            # Server URL config
│               ├── api.js               # MusicAPI (Deezer + Invidious)
│               ├── player.js            # Audio playback + listen history
│               ├── router.js            # SPA page router
│               ├── pages.js             # Page initializers
│               ├── cards.js             # Music card components
│               ├── loader.js            # Spinner injection
│               └── settings-manager.js  # UI scale persistence
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 🔧 Key Components

### `MainActivity.kt`
The single Activity powering the entire app. Sets up a full-screen WebView, loads the UI from assets, and intercepts all `localhost:3000` requests via `shouldInterceptRequest()`.

### `api.js`
Handles all music data: Deezer search and metadata, iTunes fallback, Invidious YouTube video ID lookup, and dynamic color extraction from album art.

### `player.js`
Manages the HTML5 `<audio>` element. Loads tracks, controls playback, saves the last track and listen history to localStorage.

### `settings-manager.js`
Reads and writes the `--ui-scale` CSS variable, persisted to localStorage. Applies automatically on app load.

### `router.js`
SPA-style navigation — fetches page HTML from `pages/`, injects it into the main view, and calls the matching `PageSystem.init*()` method.

### `cards.js`
Builds reusable music cards (song, album, artist), card rows, and handles dynamic color tinting from album art.

---

## 📱 Requirements

- **Android 7.0+** (API level 24)
- **Internet connection**
- No servers, API keys, or extra tools needed

---

## 🚀 Getting Started

1. **Clone the repo**
   ```bash
   git clone https://github.com/kenjikellens/IVIDSMusic.git
   ```

2. **Open in Android Studio**
   Select *File → Open* and navigate to the cloned folder.

3. **Run**
   Connect a device or start an emulator and press **Run ▶**.

That's it — no setup required.

---

## 🎨 Design Philosophy

- **Glassmorphism**: Semi-transparent surfaces with backdrop blur
- **Dynamic Colors**: Album art colors bleed into the background
- **Smooth Animations**: CSS transitions on every interaction
- **Scalable UI**: `--ui-scale` CSS variable adapts to all screen sizes (settable from 75%–150%)
- **Skeleton Loading**: Shimmer placeholders while data loads

---

## 🔒 Privacy

- No user accounts or login
- No tracking or analytics
- No ads, ever
- Audio via Invidious (privacy-respecting YouTube frontend)
- All data stored locally on-device (localStorage)

---

## 📄 License

MIT — free to use and modify.
