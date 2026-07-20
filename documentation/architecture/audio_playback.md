# 🎶 Audio Playback Architecture (Jetpack Media3 & ExoPlayer)

This document details the background audio service, ExoPlayer stream caching, media notification system, and Invidious audio stream resolution for **IVIDS Music**.

---

## 🏗️ Architectural Overview

Audio playback is managed by a background service extending **Jetpack Media3 `MediaSessionService`**. This ensures audio continues playing seamlessly when the user leaves the app, locks their screen, or switches to other applications.

```
┌───────────────────────────────────────────────────────────────────┐
│                      Jetpack Compose UI                           │
│     (PlayerBottomBar / SongDetailScreen / Notification)           │
└─────────────────────────────────┬─────────────────────────────────┘
                                  │ (StateFlow / PlayerController)
                                  ▼
┌───────────────────────────────────────────────────────────────────┐
│                      PlayerRepository                             │
│     • Interacts with MediaController                              │
│     • Manages playback queue & history persistence                │
└─────────────────────────────────┬─────────────────────────────────┘
                                  │ (IPC / Binder)
                                  ▼
┌───────────────────────────────────────────────────────────────────┐
│                      PlaybackService                              │
│             (Extends MediaSessionService)                         │
│  ┌─────────────────────────────────────────────────────────────┐  │
│  │                    MediaSession                             │  │
│  │  • System Notifications (Play/Pause/Skip/Artwork)           │  │
│  │  • Android Auto / Bluetooth / Lockscreen Metadata           │  │
│  └──────────────────────────────┬──────────────────────────────┘  │
│                                 │                                 │
│  ┌──────────────────────────────▼──────────────────────────────┐  │
│  │                    ExoPlayer Instance                       │  │
│  │  • SimpleCache (LRU Disk Audio Cache)                       │  │
│  │  • AudioAttributes (CONTENT_TYPE_MUSIC, USAGE_MEDIA)        │  │
│  │  • Stream Extractor (Invidious / Local MP3 Files)           │  │
│  └─────────────────────────────────────────────────────────────┘  │
└───────────────────────────────────────────────────────────────────┘
```

---

## 🔑 Key Components

### 1. `PlaybackService.kt`
- Subclasses `MediaSessionService`.
- Instantiates `ExoPlayer` and binds it to a `MediaSession`.
- Handles foreground service notifications so Android does not kill playback.
- Listens to audio focus changes (e.g. pauses when a phone call comes in, lowers volume for notifications).

### 2. Stream Resolution Pipeline (`InvidiousStreamResolver.kt`)
Before ExoPlayer can play a track, the app resolves the audio URL:
1. **Query Invidious API**: Request YouTube Video ID for `"$trackName $artistName"`.
2. **Fetch Stream Metadata**: Obtain raw MP3 / AAC audio stream URL from Invidious / Piped API.
3. **Fallback Mechanism**: If Invidious is unreachable, fallback to OkHttp proxy scraping of YouTube search HTML.
4. **Offline Direct Playback**: If the track is downloaded locally, bypass the network entirely and pass the local file `Uri` directly to ExoPlayer.

### 3. ExoPlayer Disk Caching (`AudioCacheManager.kt`)
- Implements `SimpleCache` using `LeastRecentlyUsedCacheEvictor` (e.g., 500 MB max cache).
- Caches streaming audio data automatically during playback to minimize bandwidth and allow offline re-listening.

### 4. Background Playback & Lockscreen Integration
- **MediaNotificationProvider**: Generates custom lockscreen notifications featuring track title, artist, high-res album cover, play/pause toggle, and skip controls.
- **Audio Attributes**: Configured with `USAGE_MEDIA` and `CONTENT_TYPE_MUSIC` to properly request system audio focus.

---

## 🔄 Playback State Flow

```kotlin
data class PlayerUiState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isBuffering: Boolean = false,
    val isRepeatOne: Boolean = false,
    val isShuffleEnabled: Boolean = false,
    val errorMessage: String? = null
)
```
- `PlayerRepository` exposes `StateFlow<PlayerUiState>` to ViewModels.
- UI components (such as the persistent bottom bar and `SongDetailScreen`) collect `PlayerUiState` reactively.
