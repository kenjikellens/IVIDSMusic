# Audio Playback Architecture

**IVIDS Music** shares queue management, stream resolution, session restoration, and playback state rules in `:core:domain` and `:core:player`, backed by platform audio engines on Android and Desktop.

```
Compose UI (DesktopPlayerDeck / MiniPlayerBar / NowPlayingSheet)
        | observes StateFlow<PlayerUiState> (250ms position polling)
PlayerViewModel
        | coordinates PlaybackQueueController + ResolveTrackStreamUseCase + SessionRepo
AudioEngine (core:domain port, backed by EngineStateMachine)
   +------------------------------+------------------------------+
   | androidMain                  | jvmMain (Windows Desktop)    |
   v                              v                              |
AndroidAudioEngine             DesktopAudioEngine                |
   | IPC via MediaController      | LocalHeaderProxy (127.0.0.1) |
   v                              | + JavaFxMediaBackend (17.0.12)
AndroidPlaybackService         DesktopAudioPrefetcher            |
   | Media3 ExoPlayer 1.5.1       | + DesktopAudioCacheTranscoder|
   | SimpleCache (500 MB LRU)     v (optional ffmpeg -> MP3 cache)|
   +------------------------------+------------------------------+
```

## Shared Domain & State Machine (`:core:domain`, `:core:player`)

- **`PlaybackQueueController`**: Pure-domain queue operations (play list, enqueue next/last, jump, remove, shuffle order preservation, `RepeatMode.OFF` / `ALL` / `ONE`).
- **`ResolveTrackStreamUseCase`**: Resolves offline local `.mp3`/`.m4a` files first (`LocalAudioLocator`), otherwise races/falls through remote stream extractors (`CompositeStreamResolver`).
- **`EngineStateMachine`**: Generation-tokenized, lock-free state machine emitting distinct `EngineSnapshot` updates and ignoring stale callbacks after track switches or engine release.
- **`PlayerViewModel`**: Syncs playback state, volume/mute persistence, 2.5-second delayed next-track prefetching, early-skip/completion interest scoring, error retry banners in player decks, and atomic session snapshots (`session_state.json`).

## Android Engine (`core/player/src/androidMain`)

- **`AndroidAudioEngine`**: Connects to `AndroidPlaybackService` via Media3 `MediaController`, binds volume/seek/transport commands, and forwards notification/lockscreen external commands.
- **`AndroidPlaybackService`**: Subclasses `MediaSessionService`, hosts `ExoPlayer` with `AudioAttributes(CONTENT_TYPE_MUSIC, USAGE_MEDIA)`, notification metadata/artwork, and automatic focus handling.
- **`AudioCacheProvider` & `PlaybackDataSourceFactory`**: Configures 500 MB `SimpleCache` (LRU) with `StreamHeaderRegistry` custom headers for authenticated/proxied YouTube streams.

## Desktop Engine (`core/player/src/jvmMain`)

- **`DesktopAudioEngine` & `JavaFxMediaBackend`**: Uses OpenJFX Media (`17.0.12`) with `Mutex`-serialized commands and deterministic `stop()` / `dispose()` on every track switch.
- **`LocalHeaderProxy`**: Lightweight local loopback HTTP proxy injecting required `User-Agent` and stream headers so JavaFX `MediaPlayer` can stream signed remote audio URLs reliably.
- **`DesktopAudioPrefetcher` & `DesktopAudioCacheTranscoder`**: Prefetches upcoming tracks after 2.5s of playback and transcodes streams into local MP3 cache (`fx_audio_cache`) via `ffmpeg.exe` when available.

## UI State Model (`PlayerUiState` in `PlayerViewModel.kt`)

```kotlin
@Immutable
data class PlayerUiState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isResolving: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val volume: Float = 1.0f,
    val isMuted: Boolean = false,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val queue: List<Track> = emptyList(),
    val currentIndex: Int = -1,
    val isCurrentLiked: Boolean = false,
    val isCurrentDownloaded: Boolean = false,
    val activeQuality: AudioQuality = AudioQuality.Q196,
    ...
)
```
