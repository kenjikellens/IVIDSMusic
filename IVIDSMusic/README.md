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
