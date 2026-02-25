# 🎵 IVIDS Music — Ad-Free Music for Android

**IVIDS Music** is a sleek, native Android music streaming app that lets you search for artists, albums, and songs — and play them **completely ad-free**. No subscriptions, no interruptions, just music.

Built with a premium dark glassmorphism UI, it combines the power of the **Deezer API** for music discovery with **Invidious** (a privacy-friendly YouTube proxy) for audio streaming. Everything runs natively on your phone — no external server required.

---

## ✨ Features

- 🔍 **Smart Search** — Integrated search experience syncing header input and a premium centered frosted-glass search bar
- 🎵 **Ad-Free Streaming** — Audio is streamed directly from YouTube via Invidious, with zero ads
- 🖥️ **Cross-Platform Compatibility** — Designed to run flawlessly in both **Native Android** (Mobile & TV) and standard **Web Browsers** (Chrome, Firefox, Safari)
- 📐 **Adaptive Design** — Fluidly adapts across 4 distinct layout breakpoints (Desktop, Tablet, Mobile, and Compact Mobile)
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

IVIDS Music uses a **hybrid architecture**: a native Android shell (Kotlin) wrapping a high-quality web-based UI (HTML/CSS/JS). This approach ensures a beautiful, responsive interface that functions as a robust native app while maintaining full compatibility with modern web browsers.

### Environment Support

The system is engineered to work in two primary environments, requiring zero configuration from the user:

- **Native Android Shell**: The Kotlin backend intercepts requests via `shouldInterceptRequest()` to bypass CORS and handle YouTube stream extraction natively using OkHttp.
- **Stand-alone Web**: When running in a browser (outside the Android shell), the app automatically detects the environment and switches to public CORS proxies (`api.allorigins.win`) to maintain full functionality.

### Layout Breakpoints

The UI follows a mobile-first philosophy with four primary layout states governed by fluid CSS scaling:

1.  **Desktop (> 1200px)**: The standard wide-screen experience with a persistent sidebar and expanded music grids.
2.  **Tablet / Large Mobile (≤ 1200px)**: The UI automatically scales down to 75% (`--ui-base-scale: 0.75`) to maintain layout density on smaller laptops and tablets.
3.  **Portrait / Phone (Orientation-based)**: A significant layout shift optimized for vertical screens. The sidebar transforms into a bottom navigation bar, margins are tightened, and font sizes/icons are adjusted for touch precision.
4.  **Small Mobile / Compact (≤ 600px)**: An ultra-compact mode with a 66% UI scale (`--ui-base-scale: 0.66`) to ensure everything remains readable and functional on very small displays.

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

## 📁 Asset File Reference

### `gui/`

- **[index.html](file:///c:/Users/kenji/AndroidStudioProjects/IVIDSMusic/app/src/main/assets/gui/index.html)**: The primary entry point and structural shell for the entire application interface. It defines the core DOM structure, including the sidebar container, global header, and the persistent footer player bar. The file acts as a host for the `main-view` element, where the SPA router dynamically injects page templates. It also includes the basic screensaver overlay and initializes core systems like `SettingsManager`, `TVNav`, and the `Router` upon page load. This setup ensures that the UI remains consistent while navigating between different functional sections of the app.
- **[index.css](file:///c:/Users/kenji/AndroidStudioProjects/IVIDSMusic/app/src/main/assets/gui/index.css)**: The comprehensive design system and master stylesheet for IVIDS Music. It implements a premium dark glassmorphism aesthetic using modern CSS features such as `backdrop-filter: blur()`, CSS variables for theme synchronization, and the `@property` rule for registering custom properties like `--ui-scale`. The stylesheet manages the entire responsive layout, utilizing sophisticated media queries to adapt the UI for mobile touchscreens and large-screen Android TV interfaces. It also defines common UI components like frosted-glass inputs, animated buttons, and the complex grid systems used for music cards.
- **[loader.css](file:///c:/Users/kenji/AndroidStudioProjects/IVIDSMusic/app/src/main/assets/gui/loader.css)**: A specialized stylesheet that handles all visual aspects of the application's loading states. It contains CSS keyframe definitions for the `ivids-spinner` animation and the animated shimmer effect used in skeleton placeholders. By decoupling loading styles from the main stylesheet, the app ensures that these critical feedback elements are lightweight and highly performant. The file provides standard classes like `.ivids-loader`, `.poster-loader`, and `.skeleton-card` to maintain visual consistency during asynchronous data fetching across all pages.
- **[tv-nav.css](file:///c:/Users/kenji/AndroidStudioProjects/IVIDSMusic/app/src/main/assets/gui/tv-nav.css)**: Provides specialized styling enhancements for the Android TV experience. It defines high-visibility focus states and interactive hints that only activate when the application detects it is running in TV mode. This includes modifications to the `--primary-color` highlighting and specific scale transitions that help users track their selection when using a D-pad remote. It ensures that the spatial navigation engine is physically represented with clear, intuitive visual cues that meet TV accessibility standards.
- **[temp_server.js](file:///c:/Users/kenji/AndroidStudioProjects/IVIDSMusic/app/src/main/assets/gui/temp_server.js)**: A standalone Node.js development server designed for testing the web interface outside of the Android environment. It implements a local Express-like architecture with endpoints to handle Deezer API proxying (`/proxy`), YouTube audio extraction (`/play`), and local track downloading (`/save`). The server utilizes `yt-dlp` via child process execution to fetch audio stream URLs directly from YouTube. This enables developers to fully test the application's music playback and history features in a standard web browser before deploying to a mobile device.

### `logic/`

- **[api.js](file:///c:/Users/kenji/AndroidStudioProjects/IVIDSMusic/app/src/main/assets/logic/api.js)**: The core music data engine responsible for discovery, metadata retrieval, and audio stream identification. It abstracts interactions with the Deezer and iTunes APIs, providing a unified search interface for artists, albums, and tracks. To ensure reliability, it employs a redundant lookup system that queries multiple public Invidious and Piped instances to find YouTube video IDs. It also features a bulletproof scraping fallback that parses raw YouTube search HTML via the local proxy if external APIs are unavailable. Beyond networking, it includes a canvas-based color extraction system that analyzes album artwork to determine dominant UI accent colors.
  - `search(query, limit, type, yearRange, offset, unique, signal)`: Performs a universal search using the Deezer API, with fallback to iTunes on failure. Supports filtering by type (artist, album, song), year range, and enforcing unique results.
  - `searchiTunes(query, limit, type, signal)`: Fallback method that directly queries the Apple iTunes Search API when Deezer is unavailable.
  - `getCategories(genres, signal)`: Fetches categorized track lists based on genre strings using Deezer's chart endpoints to populate the home page discovery rows.
  - `getRecommendations(signal)`: A wrapper around `getCategories` to load the default set of recommendations for the application.
  - `getYouTubeVideoId(query)`: A highly resilient method that attempts to resolve a track query to a YouTube video ID by chaining requests across Invidious instances, Piped APIs, and finally falling back to raw HTML proxy scraping.
  - `getArtistImage(name)`: Convenience method that searches for an artist and returns their high-resolution cover image URL.
  - `getAverageColor(imageUrl)`: Downloads an image to an off-screen HTML Canvas, samples the pixel data, and calculates the dominant RGB color to be used for dynamic UI tinting. Includes built-in caching.
  - `getSavedTracks(signal)`: Retrieves the user's library of saved tracks, automatically routing the request to the Native Android bridge (`window.AndroidAPI`) or the local web server depending on the environment.

- **[cards.js](file:///c:/Users/kenji/AndroidStudioProjects/IVIDSMusic/app/src/main/assets/logic/cards.js)**: A modular component system used for dynamically generating and managing music card elements. It provides the `CardSystem` object, which features methods like `createCard` and `hydrateCard` to efficiently render or update UI nodes with new track data. The system handles complex layout logic, such as circular formatting for artist cards and interactive play overlays for song cards. It also implements lazy-loading for artist imagery and integrates with the `MusicAPI` to apply dynamic color tinting to individual cards based on their associated artwork.
  - `createCard(track)`: Generates a new DOM element (`div.music-card`) from scratch and populates it using `hydrateCard`.
  - `hydrateCard(card, track)`: Populates an existing DOM node with track metadata (title, artist, cover image), sets up internal HTML structures based on the track type (song, album, artist), applies dynamic CSS color variables, and attaches click/keyboard event listeners for routing and playback.
  - `createRow(title, id, tracks)`: Constructs a fully functional, scrollable horizontal row containing a collection of dynamically generated music cards, complete with left/right navigation arrow buttons.

- **[config.js](file:///c:/Users/kenji/AndroidStudioProjects/IVIDSMusic/app/src/main/assets/logic/config.js)**: Manages global configuration and environment detection for the application's logic layer. It features a smart `SERVER_URL` getter that automatically distinguishes between a native Android environment and a standard web environment by checking the window hostname. On Android, it targets the secure `appassets.androidplatform.net` domain intercepted by Kotlin code, while in web mode, it falls back to a local IP or localhost. This centralized configuration ensures that API modules like `api.js` and `player.js` always communicate with the correct backend service regardless of where the app is running.
  - `get SERVER_URL()`: A dynamic getter that inspects `window.location.hostname` to return the appropriate backend URL (`appassets.androidplatform.net` for Android, `127.0.0.1:3000` for local dev).

- **[history.js](file:///c:/Users/kenji/AndroidStudioProjects/IVIDSMusic/app/src/main/assets/logic/history.js)**: A dedicated service for managing user playback history and persistence. It utilizes the `localStorage` API under the `iv_recent_tracks` key to store a list of recently played song objects. The `HistorySystem` handles data sanitization, ensuring that duplicate entries are removed and the list is capped at a maximum of 20 items to optimize performance. It provides simple `get()`, `add()`, and `clear()` methods that are consumed by the Profile and Library pages to display a personalized feed of the user's latest listening habits.
  - `get()`: Parses and retrieves the array of previously played tracks from browser `localStorage`.
  - `add(track)`: Sanitizes a track object, removes duplicates, unshifts it to the front of the history array, enforces the 20-item limit, and persists the updated array to storage.
  - `clear()`: Purges the entire playback history from `localStorage`.

- **[loader.js](file:///c:/Users/kenji/AndroidStudioProjects/IVIDSMusic/app/src/main/assets/logic/loader.js)**: A utility module that programmatically manages the injection of SVG loading spinners into the DOM. It scan elements with the `.ivids-loader` class and populates them with a consistent SVG spinner structure, marking them as initialized. The module also provides a `create()` method for generating loader elements on the fly during dynamic UI updates. By centralizing the SVG structure in JavaScript, the application ensures that all spinners across all pages have an identical appearance and behave predictably without duplicating HTML boilerplate code.
  - `init()`: Scans the DOM for any uninitialized elements matching `.ivids-loader:not(.initialized)` and injects the standard SVG spinner innerHTML.
  - `create(size)`: Programmatically constructs and returns a detached DOM element containing the loaded SVG structure, accepting an optional size class ('small', 'medium', 'large').

- **[pages.js](file:///c:/Users/kenji/AndroidStudioProjects/IVIDSMusic/app/src/main/assets/logic/pages.js)**: The central hub for view-specific initialization and data binding logic. It provides initialization methods for every major page (Home, Search, Artist, Profile, Library, Settings), coordinating with the `MusicAPI` and `CardSystem` to populate the UI. The module handles complex tasks like hydrating skeleton loaders with real data on the Home page, managing multi-category search results with pagination, and filtering the user's library in real-time. It acts as the glue between the application's data layer and the HTML templates injected by the router.
  - `initHome()`: Fetches category recommendations via `MusicAPI` and seamlessly replaces the hardcoded skeleton DOM elements on the home page with hydrated card data.
  - `initSearch(params)`: Manages the complex state of the search page, delegating initial skeleton rendering, resolving concurrent API requests for artists/songs/albums, and setting up the infinite-scroll "Load More" pagination logic.
  - `initArtist(params)`: Minimal initialization method specifically for binding the requested artist name to the detail view template header.
  - `initSettings()`: Binds interactive logic to the settings page, specifically handling the active states and click events for the UI scaling segmented control buttons.
  - `initProfile()`: Retrieves local playback history via `HistorySystem` and populatesthe profile dashboard, switching to an empty state UI if no history exists.
  - `initLibrary()`: Coordinates fetching saved tracks, instantly rendering the current history feed, and initializing the UI structures for the user's personal collection.
  - `renderLibrary(tracks)`: Takes an array of track data and renders the interactive list items for the saved tracks view.
  - `renderRecentTracks()`: Reusable helper method to draw the "Recently Played" horizontal card row from local storage.
  - `clearHistory()`: Prompts the user for confirmation and triggers a history purge via `HistorySystem`, automatically updating the UI instantly.
  - `filterLibrary(query)`: Performs a real-time, client-side, case-insensitive text search against the currently loaded library track list and re-renders the DOM.

- **[player.js](file:///c:/Users/kenji/AndroidStudioProjects/IVIDSMusic/app/src/main/assets/logic/player.js)**: The primary controller for audio playback logic and the HTML5 media interface. It maintains a global `YouTubePlayer` singleton that manages a persistent `Audio` object and tracks playback state. The module coordinates the multi-step process of resolving a track name to a YouTube video ID, fetching a playable stream URL, and initiating playback. It also manages the state of the player bar UI, updating progress sliders, volume controls, and track metadata while simultaneously persisting the last played song to ensure continuity across app restarts.
  - `init()`: Binds all native HTML5 `<audio>` events (play, pause, timeupdate, loadedmetadata) to the global UI elements (play button, progress slider, time counters) and restores the last played track from memory.
  - `setUI(track)`: Updates the persistent lower player bar with the title, artist, and cover art of the currently loaded track.
  - `formatTime(seconds)`: Helper method to convert raw seconds into a human-readable `M:SS` format.
  - `next()` / `previous()`: Placeholder methods intended for future queue management.
  - `loadTrack(track)`: The core playback entrypoint. It updates UI state, requests a YouTube video ID from `MusicAPI`, pings the local proxy server to obtain an MP3 stream URL, and finally triggers audio playback while unlocking the "Save" functionality.
  - `playSavedTrack(track)`: A faster alternative to `loadTrack` intended for offline or pre-downloaded files, bypassing the YouTube API lookup entirely and playing directly from a known local URL.
  - `toggle()`: Pauses or plays the currently loaded audio stream based on the internal state.
  - `saveTrack()`: Triggers a server-side download of the currently loaded YouTube video, saving it as an MP3 file via the local Node.js or Kotlin backend.
  - `playAllSaved()`: Loads the user's entire library into an internal playback queue and begins sequential playback from index 0.
  - `shuffleSaved()`: Randomizes the user's saved library into an internal playback queue before starting playback.

- **[router.js](file:///c:/Users/kenji/AndroidStudioProjects/IVIDSMusic/app/src/main/assets/logic/router.js)**: Implement a lightweight client-side Single Page Application (SPA) routing system. It enables seamless transitions between different views by fetching HTML templates from the `pages/` directory and injecting them into the primary `main-view` container. The router proactively manages an `AbortController` to cancel pending network requests from previous pages and includes logic to extract and clean up scripts to prevent memory leaks. After aSuccessful injection, it automatically calls the corresponding initialization method in `PageSystem` to wire up page-specific interactivity.
  - `loadPage(pageName, params)`: The heart of navigation. It aborts any running API requests, displays a visual loading overlay, fetches the raw HTML template string, safely sanitizes it by removing inline scripts, injects it into `<main id="main-view">`, updates sidebar highlighting, and finally dynamically imports `pages.js` to execute the corresponding constructor method (e.g., `initHome`, `initSearch`).

- **[settings-manager.js](file:///c:/Users/kenji/AndroidStudioProjects/IVIDSMusic/app/src/main/assets/logic/settings-manager.js)**: An essential module for managing user preferences and application-wide settings. It primarily focuses on the UI scaling feature, allowing users to adjust the interface size from 75% to 150%. The manager persists these choices to `localStorage` and applies them using the `--ui-user-scale` CSS custom property on the document root. It also handles event dispatching via `CustomEvent('iv-scale-changed')` to allow other components to react to scale changes, and manages the active state of UI buttons within the settings menu.
  - `getScale()`: Retrieves the user's saved numeric UI scale factor from `localStorage`, falling back to 1.0.
  - `setScale(value)`: Persists a new scale value, updates the UI, and updates the settings menu visual state.
  - `applyScale(value)`: Injects the value into the CSSOM by mutating the `--ui-user-scale` variable on `document.documentElement` and dispatches a global `iv-scale-changed` event.
  - `updateScaleButtons(value)`: Updates the `.active` class state of the scale selection buttons based on strict numeric matching to provide accurate visual feedback.
  - `bindScaleUI()`: Attaches global event listeners to the settings container to intercept button clicks and trigger `setScale`.
  - `init()`: Automatically runs on window load to retrieve and apply the saved scale instantly to prevent layout thrashing.

- **[tv-nav.js](file:///c:/Users/kenji/AndroidStudioProjects/IVIDSMusic/app/src/main/assets/logic/tv-nav.js)**: A sophisticated spatial navigation engine designed to provide a native-like experience on Android TV. It intercepts D-pad arrow key events and uses a geometric algorithm to calculate the best next element to focus based on proximity and alignment. The engine supports complex navigation flows, including overlap bonuses for overlapping elements and penalties for significant cross-axis gaps. It also handles automated scrolling to keep the focused element in view and manages specialized focus behavior for interactive elements like inputs and buttons.
  - `init()`: Activates TV mode by adding a global class to the `body`, binding global keyboard listeners, and triggering the first focus calculation.
  - `reinitFocus()`: Identifies the best element to focus on if the user somehow loses focus (e.g., due to a page load or DOM manipulation), gracefully defaulting to the sidebar 'Home' button.
  - `getFocusableElements()`: Queries the DOM for all interactable elements (`a, button, input, [tabindex="0"]`) that are currently visible on screen.
  - `isElementVisible(el)`: Performs strict checks to ensure an element has positive dimensions and valid CSS display rules before allowing it to be focused.
  - `setFocus(el)`: Programmatically grants focus to an element and forces the viewport to smoothly scroll it into the center.
  - `getCenter(rect)`: Mathematical helper to calculate the exact X/Y center coordinates of a bounding rectangle.
  - `navigate(direction)`: The core geometric engine. Given a direction (up, down, left, right), it iterates over all focusable elements, calculates distance penalties and overlap bonuses based on bounding boxes, and selects the absolute mathematically closest element in the desired directional path.
  - `handleKeyDown(e)`: Global event interceptor for Arrow keys, Enter, and Escape/Backspace, converting hardware signals into `navigate()` calls or synthetic click events while suppressing default browser behavior.

### `gui/pages/`

- **[home.html](file:///c:/Users/kenji/AndroidStudioProjects/IVIDSMusic/app/src/main/assets/gui/pages/home.html)**: The structural template for the application's landing page. It is pre-populated with skeleton loaders arranged in genre-based rows (Pop, Rock, Hip-Hop, etc.) to ensure a smooth "initial paint" experience while data is being fetched. The page features a prominent hero section for featured recommendations and provides the container IDs that `PageSystem.initHome()` targets for content injection. This file defines the visual hierarchy for the discovery experience, using the shared design system to present curated music rows to the user.
- **[search.html](file:///c:/Users/kenji/AndroidStudioProjects/IVIDSMusic/app/src/main/assets/gui/pages/search.html)**: A versatile template for the search and browse functionality. It includes a dedicated "Browse" hero section with a large centered search input, a promo box for first-time users, and structures for displaying results in a grid or row format. The template provides sections for Artists, Songs, and Albums, each with its own "Load More" capability. It is designed to work in tandem with the global header search, synchronizing states between the two inputs to provide a seamless search experience throughout the application.
- **[artist.html](file:///c:/Users/kenji/AndroidStudioProjects/IVIDSMusic/app/src/main/assets/gui/pages/artist.html)**: The template used for displaying detailed information about a single musician or band. It features a large artist header area intended for high-resolution imagery and provides the structural framework for listing the artist's most popular tracks and albums. The page is dynamically populated by `PageSystem.initArtist()`, which consumes artist metadata fetched via the Deezer API. It serves as an immersive "deep dive" view within the application's navigation stack.
- **[library.html](file:///c:/Users/kenji/AndroidStudioProjects/IVIDSMusic/app/src/main/assets/gui/pages/library.html)**: A structural layout for the user's personal music collection. It includes dedicated sections for browsing locally saved tracks and features a custom search input for filtering the library in real-time. The template is designed to display a mix of list-based views for songs and grid-based views for saved albums. It also manages "empty states" with helpful prompts if the user hasn't favorited any music yet, encouraging them to start building their library by browsing other sections of the app.
- **[profile.html](file:///c:/Users/kenji/AndroidStudioProjects/IVIDSMusic/app/src/main/assets/gui/pages/profile.html)**: The "You" page template, serving as a personal dashboard for the user. It contains structural elements for showing the user's avatar, bio, and a statistical grid highlighting metrics like "Minutes Played" and "Liked Songs". A significant portion of the page is dedicated to the "Recently Played" list, which is dynamically populated from local history records. The layout uses the application's glassmorphism styles to create a clean, modern interface for reviewing personal listening data and habits.
- **[settings.html](file:///c:/Users/kenji/AndroidStudioProjects/IVIDSMusic/app/src/main/assets/gui/pages/settings.html)**: The template for the application's configuration interface. It organizes settings into logical sections such as "Appearance" and "About," featuring a segmented control system for the UI scale factor. The page is designed to be highly responsive, stacking controls vertically on mobile devices while maintaining a clean horizontal layout on TV and desktop screens. It provides the interactive hooks for the `SettingsManager` to bind user preferences and apply them instantly across the entire application.

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
