# 🎵 IVIDS Music — Ad-Free Music for Android

**IVIDS Music** is a sleek, native Android music streaming app that lets you search for artists, albums, and songs — and play them **completely ad-free**. No subscriptions, no interruptions, just music.

Built with a premium dark glassmorphism UI, it combines the power of the **Deezer API** for music discovery with **Invidious** (a privacy-friendly YouTube proxy) for audio streaming. Everything runs natively on your phone — no external server required.

---

## ✨ Features

- 🔍 **Smart Search** — Search for songs, artists, and albums with real-time results powered by Deezer
- 🎵 **Ad-Free Streaming** — Audio is streamed directly from YouTube via Invidious, with zero ads
- 🎨 **Premium Dark UI** — Glassmorphism design with dynamic blurred backgrounds, smooth animations, and vibrant colors
- 📱 **Fully Native** — Runs entirely on your Android device, no PC or external server needed
- 🏠 **Home Page** — Curated genre rows (Pop, Rock, Hip-Hop, Hardcore, 90's, Electronic) with album art
- 🔎 **Browse & Discover** — Search by artist, song, or album with category filtering
- 💾 **Track Persistence** — Remembers your last played song across sessions using localStorage
- 🎤 **Artist Pages** — Dedicated artist views with real artist images from Deezer
- 🎛️ **Persistent Player Bar** — Full playback controls with progress slider, volume, and track info
- 🖼️ **Dynamic Colors** — Album artwork colors are extracted and used to tint the UI in real-time
- 🌙 **Screensaver Mode** — Auto-activates after 60 seconds of inactivity

---

## 🏗️ Architecture

IVIDS Music uses a **hybrid architecture**: a native Android shell (Kotlin) wrapping a high-quality web-based UI (HTML/CSS/JS). This approach gives the best of both worlds — a beautiful, responsive web UI with native Android networking capabilities.

### How It Works

```
┌─────────────────────────────────────────────┐
│              Android App (Kotlin)            │
│                                              │
│  ┌────────────────────────────────────────┐  │
│  │           WebView (Full Screen)        │  │
│  │                                        │  │
│  │   HTML/CSS/JS UI loaded from assets/   │  │
│  │                                        │  │
│  │   ┌──────────┐    ┌────────────────┐   │  │
│  │   │  Search   │    │  Play a Song   │   │  │
│  │   └─────┬─────┘    └───────┬────────┘   │  │
│  │         │                   │            │  │
│  └─────────┼───────────────────┼────────────┘  │
│            │ fetch()           │ fetch()        │
│            ▼                   ▼                │
│  ┌──────────────────────────────────────────┐  │
│  │    shouldInterceptRequest() (Kotlin)      │  │
│  │                                           │  │
│  │  • /proxy?url=...  → Deezer API (OkHttp)  │  │
│  │  • /play?videoId=  → Invidious API (OkHttp)│  │
│  └──────────────────────────────────────────┘  │
│            │                   │                │
│            ▼                   ▼                │
│     Deezer API            Invidious API         │
│   (Music Metadata)      (Audio Stream URL)      │
└─────────────────────────────────────────────┘
```

### The Key Trick: Native Request Interception

The JavaScript code thinks it's talking to a local server at `http://localhost:3000`. But there is no server — Android's `WebViewClient.shouldInterceptRequest()` catches these requests **before they leave the device** and handles them natively in Kotlin using OkHttp:

1. **`/proxy?url=...`** — The JS code sends Deezer API requests through this "proxy" (originally needed to bypass CORS in a browser). On Android, the Kotlin code simply fetches the URL directly with OkHttp and returns the response. No CORS issues exist in a native context.

2. **`/play?videoId=...`** — When a user taps a song, the JS code requests the audio URL for a YouTube video. The Kotlin code queries the Invidious API to find an audio-only stream URL and returns it. The HTML5 `<audio>` element then plays the stream directly.

---

## 🛠️ Tech Stack

| Layer | Technology | Purpose |
|---|---|---|
| **Native Shell** | Kotlin + Android WebView | App container, request interception, native networking |
| **Networking** | OkHttp | Fast, reliable HTTP requests from Kotlin |
| **Frontend UI** | HTML5, Vanilla CSS, Vanilla JS (ES Modules) | The entire user interface |
| **Music Discovery** | Deezer API | Search, album art, artist images, genre browsing |
| **Audio Playback** | Invidious API + HTML5 `<audio>` | YouTube audio stream extraction and playback |
| **Persistence** | localStorage (WebView) | Remembers last played track |

---

## 📁 Project Structure

```
IVIDSMusic/
├── app/
│   └── src/main/
│       ├── AndroidManifest.xml          # Permissions (INTERNET) & Activity declaration
│       ├── java/com/kenjigames/ividsmusic/
│       │   └── MainActivity.kt          # WebView setup + request interception logic
│       ├── assets/
│       │   ├── gui/                     # Frontend UI
│       │   │   ├── index.html           # Main app shell (sidebar, header, player bar)
│       │   │   ├── index.css            # Complete design system (glassmorphism, dark theme)
│       │   │   ├── loader.css           # Loading spinner animations
│       │   │   └── pages/               # Dynamic page templates
│       │   │       ├── home.html        # Genre rows with album cards
│       │   │       ├── search.html      # Search results (artists, songs, albums)
│       │   │       ├── artist.html      # Artist detail page
│       │   │       ├── library.html     # User library
│       │   │       ├── playlists.html   # Playlists
│       │   │       └── settings.html    # App settings
│       │   └── logic/                   # Frontend JavaScript (ES Modules)
│       │       ├── config.js            # Server URL configuration
│       │       ├── api.js               # MusicAPI — Deezer search, Invidious video lookup
│       │       ├── player.js            # YouTubePlayer — audio playback controller
│       │       ├── router.js            # Client-side page router (SPA navigation)
│       │       ├── pages.js             # Page initialization logic (Home, Search, Artist)
│       │       ├── cards.js             # Music card component system
│       │       └── loader.js            # Loading spinner injection
│       └── res/
│           ├── layout/activity_main.xml # Full-screen WebView layout
│           └── values/themes.xml        # NoActionBar dark theme
├── IVIDSMusic/                          # Original web app source (reference)
│   ├── gui/                             # Original frontend files
│   ├── logic/                           # Original JavaScript modules
│   └── server/                          # Node.js server (not needed for Android)
├── build.gradle.kts                     # Root build config
├── gradle/libs.versions.toml           # Dependency version catalog
└── settings.gradle.kts                  # Project settings
```

---

## 🔧 Key Components Explained

### `MainActivity.kt` — The Native Brain
The single Activity that powers the entire app. It:
- Sets up a full-screen WebView with JavaScript, DOM storage, and file access enabled
- Loads the UI from `file:///android_asset/gui/index.html`
- Intercepts all HTTP requests to `localhost:3000` via `shouldInterceptRequest()`
- Handles Deezer proxy requests natively (bypassing CORS)
- Extracts YouTube audio stream URLs via the Invidious API
- Manages Android back button navigation within the WebView

### `api.js` — Music Discovery Engine
Handles all music data retrieval:
- **Primary**: Deezer API for search, genre browsing, artist images, and album metadata
- **Fallback**: iTunes API if Deezer is unavailable
- **YouTube Search**: Queries multiple Invidious instances to find video IDs for songs
- **Color Extraction**: Analyzes album art to generate dynamic UI accent colors

### `player.js` — Audio Playback Controller
Manages the HTML5 `<audio>` element:
- Loads tracks by searching YouTube → getting audio URL → playing the stream
- Controls play/pause, progress scrubbing, and volume
- Persists the last played track to localStorage
- Updates the player bar UI (cover art, title, artist, progress)

### `router.js` — Single Page Application Router
Enables SPA-like navigation without page reloads:
- Fetches page templates from the `pages/` directory
- Injects HTML into the main view container
- Dynamically imports and initializes page-specific JavaScript
- Manages sidebar active states

### `cards.js` — Card Component System
Creates reusable music card elements:
- Song cards with play overlay
- Album cards that navigate to search results
- Artist cards with lazy-loaded images
- Scrollable card rows with arrow navigation
- Dynamic color tinting based on album art

---

## 📱 Requirements

- **Android 7.0+** (API level 24)
- **Internet connection** (for Deezer API and audio streaming)
- No additional tools, servers, or dependencies needed

---

## 🚀 Getting Started

1. **Clone the repository**
   ```bash
   git clone https://github.com/kenjikellens/IVIDSMusic.git
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an Existing Project"
   - Navigate to the cloned `IVIDSMusic` folder

3. **Build & Run**
   - Connect an Android device or start an emulator
   - Click the **Run ▶** button
   - The app will install and launch automatically

That's it — no server setup, no API keys, no configuration needed.

---

## 🎨 Design Philosophy

IVIDS Music follows a **premium dark UI** aesthetic inspired by modern music streaming apps:

- **Glassmorphism**: Semi-transparent surfaces with backdrop blur effects
- **Dynamic Backgrounds**: Album art colors bleed into the background for an immersive feel
- **Smooth Animations**: CSS transitions and micro-animations on every interaction
- **Responsive Scaling**: Uses a CSS `--ui-scale` variable to adapt to different screen sizes
- **Skeleton Loading**: Content placeholders with animated shimmer effects while data loads
- **Persistent Player Bar**: Slides in when music plays, stays hidden when not active

---

## 🔒 Privacy

- No user accounts or login required
- No tracking or analytics
- No ads, ever
- Audio is streamed via Invidious (a privacy-respecting YouTube frontend)
- All data stays on your device (localStorage only)

---

## 📄 License

MIT — feel free to use and modify.
