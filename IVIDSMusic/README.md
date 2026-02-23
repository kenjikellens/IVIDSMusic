# 🎵 IVIDSMusic

**Listen to music — without ads.**

IVIDSMusic is a sleek, modern music player that lets you search for artists, albums, and songs using the iTunes API, then streams them ad-free via YouTube. Built with a premium dark UI, responsive design, and a persistent player bar.

---

## ✨ Features

- 🔍 **Multi-functional Search** — Search for songs, artists, and albums with real-time results
- 🎨 **Premium Dark UI** — Glassmorphism, dynamic blurred backgrounds, and smooth animations
- 📱 **Fully Responsive** — Scales across desktop, tablet, and mobile via a `--ui-scale` CSS variable
- 🎵 **Ad-free Playback** — Streams audio via YouTube (through Invidious) and downloads as MP3
- 🏠 **Home Page** — Top charts, recommendations, and curated content
- 🔎 **Browse Page** — Popular genres, moods, trending artists, and popular songs
- 💾 **Track Persistence** — Remembers your last played song across sessions
- 🎤 **Artist Pages** — Dedicated artist views with real artist images
- 🎛️ **Persistent Player Bar** — Slides in when music plays, stays out of the way when not
- 📂 **Library View** — Save and download tracks to a local folder for offline playback
- 🔌 **Local Node.js API** — Background server handles fetching, downloading, and serving local tracks

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| **Frontend** | HTML, Vanilla CSS, Vanilla JS (ES Modules) |
| **Backend** | Node.js + Express |
| **Music Data** | iTunes Search API |
| **Streaming** | Invidious (YouTube proxy) + `yt-dlp` |
| **Audio** | HTML5 `<audio>` element |

---

## 🚀 Getting Started

### Prerequisites

- [Node.js](https://nodejs.org/) (v18+)
- [yt-dlp](https://github.com/yt-dlp/yt-dlp) (must be in PATH or placed in `server/bin/`)
- [ffmpeg](https://ffmpeg.org/) (for audio conversion)

### Installation

```bash
# Clone the repository
git clone https://github.com/kenjikellens/IVIDSMusic.git
cd IVIDSMusic

# Install server dependencies
cd server
npm install
```

### Running

```bash
# Start the backend server (from the /server directory)
node server.js
```

Then open `gui/index.html` in your browser (or serve it with a local static server).

> The server runs on `http://localhost:3000` by default.

---

## ⚠️ Important Technical Notes

**Do NOT change the Category/Genre API from iTunes.** 
We previously attempted to use the Deezer API for the Categories/Home page (e.g., searching for "Pop" or "Rock" genres). However, free-tier Deezer API searches return highly inaccurate tags (e.g., returning children's Cartoons for the "Pop" genre). The iTunes API *must* be kept for building the Home categories to ensure high-quality, expected results.

---

## 🆕 Recent Updates

- **Library Implementation:** Added a new Library view that dynamically loads downloaded MP3s from the local `/saved` folder.
- **Offline Playback:** The player now supports instantly playing locally saved tracks bypassing the YouTube/Invidious streaming network.
- **Node.js CORS Fixes:** Fixed CORS and Fetch rejection errors on the Node server, allowing the frontend Live Server (`127.0.0.1`) to communicate flawlessly with the backend download engine.
- **Android Native Bridge:** Added a `JavascriptInterface` to `MainActivity.kt` so the Android app can natively list and play downloaded MP3s.

---

## 📁 Project Structure

```
IVIDSMusic/
├── gui/                    # Frontend
│   ├── index.html          # Main app shell
│   ├── index.css           # Global styles + responsive scaling
│   ├── api.js              # MusicAPI (iTunes + Invidious)
│   ├── player.js           # Audio player logic
│   ├── router.js           # Client-side router
│   ├── loader.js           # Loading indicator
│   └── pages/              # Page templates
│       ├── home.html
│       ├── search.html
│       └── artist.html
└── server/                 # Backend
    ├── server.js           # Express server
    ├── package.json
    └── bin/                # yt-dlp / ffmpeg binaries (optional)
```

---

## 📸 Screenshots

> Coming soon

---

## 📄 License

MIT — feel free to use and modify.
