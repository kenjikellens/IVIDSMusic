# 🎵 IVIDS Music — Ad-Free Music for Android

**IVIDS Music** is a sleek, native Android music streaming app that lets you search for artists, albums, and songs — and play them **completely ad-free**. No subscriptions, no interruptions, just music.

Built with a premium dark glassmorphism UI, it combines the power of the **Deezer API** for music discovery with **Invidious** (a privacy-friendly YouTube proxy) for audio streaming. Everything runs natively on your phone — no external server required.

---

## ✨ Features

- 🔍 **Smart Search** — Integrated search experience syncing header input and a premium centered frosted-glass search bar
- 🎵 **Ad-Free Streaming** — Audio is streamed directly from YouTube via Invidious, with zero ads
- 🖥️ **True Cross-Platform** — Works flawlessly natively on **Android Mobile**, **Android TV** (with full D-pad remote navigation), and in any **Web Browser**
- 🎨 **Premium Dark UI** — Glassmorphism design with dynamic blurred backgrounds, custom typographic branding, and smooth CSS interactions
- 🏠 **Home Page** — Curated genre rows (Pop, Rock, Hip-Hop, Hardcore, 90's, Electronic) with album art
- 🔎 **Browse & Discover** — Filter results by category (Artists, Songs, Albums) and refine with dynamic Year filtering, plus a dismissible mini-hero promo
- 💾 **Track Persistence** — Remembers your last played song across sessions using localStorage
- 📜 **Listen History** — Automatically tracks your recently played songs (stored locally, max 20 tracks, no duplicates)
- 🎤 **Artist Pages** — Dedicated artist views with real artist images from Deezer
- 🎛️ **Persistent Player Bar** — Full playback controls with progress slider, volume, and track info — slides in when active
- 🖼️ **Dynamic Colors** — Album artwork colors are extracted and used to tint the UI in real-time
- 🌙 **Screensaver Mode** — Auto-activates after 60 seconds of inactivity with a smooth overlay transition
- ⚙️ **Settings Page** — Configurable UI scale factor (75%, 90%, 100%, 110%, 125%, 150%) with instant preview and persistence
- 👤 **You Page** — Personal profile showing listen stats (liked songs, playlists, minutes played), recently played tracks, and top genres
- 📚 **Your Library** — Collection of liked songs and saved albums
- 🦴 **Skeleton Loading** — Animated shimmer placeholders while content is being fetched

---

## 🏗️ Architecture

IVIDS Music uses a **hybrid architecture**: a native Android shell (Kotlin) wrapping a high-quality web-based UI (HTML/CSS/JS). This approach gives the best of both worlds — a beautiful, responsive web UI with native Android networking capabilities.

When running natively, the app intercepts requests and uses a custom local proxy to bypass CORS. When running in a web browser, the app detects the environment and automatically switches to a public CORS proxy (`api.allorigins.win`) so the UI continues to function perfectly without the Android backend.

### How It Works (Android Native)

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

---

## 🛠️ Tech Stack

| Layer | Technology | Purpose |
|---|---|---|
| **Native Shell** | Kotlin + Android WebView | App container, request interception, native networking |
| **Networking** | OkHttp | Fast, reliable HTTP requests from Kotlin |
| **Frontend UI** | HTML5, Vanilla CSS, Vanilla JS (ES Modules) | The entire user interface |
| **Music Discovery** | Deezer API | Search, album art, artist images, genre browsing |
| **Audio Playback** | Invidious API + HTML5 `<audio>` | YouTube audio stream extraction and playback |
| **Persistence** | localStorage (WebView/Browser) | Last played track, listen history, UI scale settings |

---

## � Project Structure

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
│       │   │   ├── svg/                 # SVG icons (brand, nav, player controls)
│       │   │   └── pages/               # Dynamic page templates
│       │   │       ├── home.html        # Genre rows with album cards
│       │   │       ├── search.html      # Search results (artists, songs, albums)
│       │   │       ├── artist.html      # Artist detail page
│       │   │       ├── library.html     # User library (liked songs, saved albums)
│       │   │       ├── profile.html     # 'You' page (stats, recently played, genres)
│       │   │       └── settings.html    # App settings (UI scale factor)
│       │   └── logic/                   # Frontend JavaScript (ES Modules)
│       │       ├── config.js            # Server URL configuration
│       │       ├── api.js               # MusicAPI — Deezer search, Invidious video lookup
│       │       ├── player.js            # YouTubePlayer — audio playback + listen history
│       │       ├── router.js            # Client-side page router (SPA navigation)
│       │       ├── pages.js             # Page initialization logic (Home, Search, Artist, Profile, Settings)
│       │       ├── cards.js             # Music card component system
│       │       ├── loader.js            # Loading spinner injection
│       │       └── settings-manager.js  # UI scale factor persistence & application
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
- Saves recently played tracks to localStorage (max 20, no duplicates) for the "You" page
- Updates the player bar UI (cover art, title, artist, progress)

### `router.js` — Single Page Application Router
Enables SPA-like navigation without page reloads:
- Fetches page templates from the `pages/` directory
- Injects HTML into the main view container
- Dynamically imports and initializes page-specific JavaScript via `PageSystem.init*()`
- Manages sidebar active states and scroll position

### `cards.js` — Card Component System
Creates reusable music card elements:
- Robust click handler that safely plays tracks or navigates to albums/artists
- Song cards with interactive play overlay
- Album cards that automatically generate a search query for their tracks
- Artist cards with circular layouts and lazy-loaded imagery
- Scrollable card rows with arrow navigation
- Dynamic color tinting based on album art

### `settings-manager.js` — UI Scale & Settings Persistence
Manages user preferences:
- Reads and writes the `--ui-scale` CSS custom property registered via `@property`
- Saves the chosen scale factor to localStorage (`iv_ui_scale`)
- Automatically applies the saved scale on every page load
- Dispatches `iv-scale-changed` events for components that need manual adjustment
- Supported values: 75%, 90%, 100% (default), 110%, 125%, 150%

### `pages.js` — Page Initialization System
Centralizes all page-specific logic:
- `initHome()` — Loads genre-based recommendations, renders skeleton loaders, populates album card rows
- `initSearch()` — Handles exact state synchronization between search inputs, applies robust year filtering, manages grid/row displays, and implements "Load More" pagination
- `initArtist()` — Renders individual artist detail views
- `initProfile()` — Loads real listen history from localStorage, displays empty states when no data exists
- `initSettings()` — Populates scale factor buttons, highlights the active selection, wires up click handlers

### `loader.js` — Adaptive Loader System
Injects SVG spinner animations into `.ivids-loader` elements:
- Auto-initializes on DOM load
- Supports programmatic creation with size variants (small, medium, large)
- Used throughout the app for skeleton loaders and status indicators

---

## 📄 Page Breakdown

### 🏠 Home (`home.html`)
The landing page displays curated genre rows — Pop, Rock, Hip-Hop, Hardcore, 90's, and Electronic — each populated with album cards from the Deezer API. Skeleton loaders with spinners appear while data loads, then smoothly transition to real content.

### 🔍 Search (`search.html`)
The comprehensive search hub. It begins with a hero browse section featuring a premium dark glassmorphic input, a dismissible "Start Your Experience" promo box, and syncs seamlessly with the sidebar header search. Results appear in real-time categorized by Artists, Songs, and Albums. Users can refine lists using a specific Year filter and infinitely load more via pagination.

### 🎤 Artist (`artist.html`)
A dedicated detail page for individual artists, showing artist name and image with further content to be expanded.

### 📚 Library (`library.html`)
Your personal library — a collection of liked songs and saved albums. Currently features a clean empty state with a link to create your first playlist.

### 👤 You (`profile.html`)
Your personal profile page featuring:
- **Avatar** — With an edit button for future customization
- **Name & bio** — Defaults to "Listener" / "No bio yet." until customized
- **Stats grid** — Liked Songs, Playlists, and Minutes Played (all start at 0, populated by real usage)
- **Recently Played** — Loaded from localStorage, shows real tracks you've listened to or an empty state
- **Top Genres** — Displays genres based on listening habits or an empty state message

### ⚙️ Settings (`settings.html`)
Currently features:
- **Appearance section** — Interface Scale with a segmented button control for 75%, 90%, 100%, 110%, 125%, 150%
- **About section** — Version info and attribution
- Mobile-friendly layout that stacks controls vertically on smaller screens

---

## �🚀 Getting Started

IVIDS Music can be run in **two ways**: natively on an Android device/TV, or directly in a web browser.

### Option 1: Native Android / Android TV
1. **Clone the repository**
   ```bash
   git clone https://github.com/kenjikellens/IVIDSMusic.git
   ```
2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an Existing Project"
   - Navigate to the cloned `IVIDSMusic` folder
3. **Build & Run**
   - Connect an Android phone, Android TV, or start an emulator
   - Click the **Run ▶** button
   - The app will automatically configure the correct scaling and navigation mode (TV or Touch).

### Option 2: Web Browser (Live Server)
Because the UI is built entirely with web technologies, you can run the app directly in Firefox, Chrome, or Safari.
1. Navigate to the UI source folder: `app/src/main/assets/gui/`
2. Open `index.html` using an extension like **VS Code Live Server**.
3. The app will automatically detect it is running on the web, bypass the Android-specific logic, and use public CORS proxies to ensure Deezer features work perfectly.

---

## 🎨 Design Philosophy

IVIDS Music follows a **premium dark UI** aesthetic inspired by modern music streaming apps:

- **Glassmorphism**: Semi-transparent surfaces with backdrop blur effects for depth
- **Dynamic Backgrounds**: Album art colors are extracted and bleed into the background for an immersive feel
- **Smooth Animations**: CSS transitions and micro-animations on every interaction (hover, focus, page load)
- **Responsive Scaling**: Uses a registered CSS `@property --ui-scale` variable to adapt to different screen sizes and user preferences, settable from 75% to 150%
- **TV Support**: Fully custom spatial navigation engine (`tv-nav.js`) enables perfect D-pad control on Android TV interfaces
- **Persistent Player Bar**: Slides in when music starts playing, stays hidden when inactive
- **Mobile-First**: All pages and settings use media queries to adapt gracefully to smaller screens
- **Accent Color System**: Uses CSS custom properties (`--primary-color`, `--accent-color`) with HSL-based tinting for a cohesive color palette

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
