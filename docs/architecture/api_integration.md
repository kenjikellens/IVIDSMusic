# Networking & API Integration Architecture

**IVIDS Music** uses **Ktor (`3.0.1`)** with **OkHttp (`4.12.0`)** engine, **Kotlinx Serialization (`1.7.3`)**, **NewPipeExtractor (`v0.24.4`)**, and native fallbacks for catalog discovery and stream extraction.

## Networking Stack

| Purpose | Library / Component | Role |
|---|---|---|
| **HTTP Client** | Ktor `3.0.1` (`ktor-client-okhttp`) + Coroutines | Async JSON requests, content negotiation, timeouts |
| **JSON Parsing** | `kotlinx.serialization.json` | DTO deserialization (`DeezerDtos.kt`, `ItunesDtos.kt`, `InvidiousDtos.kt`) |
| **Catalog Primary** | `DeezerApiClient` | Track/artist/album search, charts, artist top tracks, related tracks |
| **Catalog Fallback** | `ItunesApiClient` | Automatic catalog fallback when Deezer returns empty/errors |
| **YouTube Streams** | `CompositeStreamResolver` | Tiered resolution + 30-minute bounded in-memory URL cache (`InMemoryStreamTtlCache`) |
| **Desktop Tools** | `DesktopNativeToolsManager` | Bundles/updates `yt-dlp.exe` and `ffmpeg.exe` on Windows |
| **Image & Color** | Coil 3 (`coil-compose` + `coil-network-ktor3`) + `ColorExtractor` | Album artwork caching and `expect/actual` background color/blur sampling |

## Catalog Discovery (`CompositeMusicDiscoveryRepository.kt`)

- Wraps `DeezerApiClient` and `ItunesApiClient` behind `MusicDiscoveryRepository` (`:core:domain`).
- Applies a thread-safe bounded LRU cache with a **15-minute TTL** to eliminate duplicate API calls across rapid navigation.
- Falls back cleanly to iTunes when Deezer calls return empty or fail.

## Multi-Tier Stream Resolution (`CompositeStreamResolver`)

`CompositeStreamResolver` executes ordered resolution tiers protected by `NetworkSecurityGuard` and `InMemoryStreamTtlCache` (30-min TTL):

1. **Local Storage (`JvmLocalAudioLocator` / `LocalAudioLocator`)**: Immediately returns `StreamSource.LocalFile` if downloaded `.mp3`/`.m4a` already exists.
2. **In-Memory TTL Cache (`InMemoryStreamTtlCache`)**: Reuses valid remote URLs across replays, downloads, and seeks (`invalidate(trackId)` clears stale URLs on playback failure).
3. **NewPipeExtractor (`NewPipeStreamResolver`)**: Extracts direct audio stream metadata in-process on JVM and Android without external proxies.
4. **Invidious Parallel Race (`InvidiousFallbackResolver`)**: Races configured public Invidious mirrors in parallel with a `3.5s` timeout, cancelling slower requests as soon as one succeeds.
5. **Platform Final Fallback (`PlatformStreamFallback`)**:
   - **Desktop (`YtDlpDesktopResolver`)**: Invokes bundled/updated `yt-dlp.exe` (and `DesktopFfmpegTranscoder` for offline downloads).
   - **Android (`AndroidStreamFallback`)**: Returns structured `DomainError.StreamNotFound` when earlier extractors fail.

## Artwork Blur & Ambient Tinting (`ColorExtractor`, `BlurredArtworkBackdrop`)

- **`BlurredArtworkBackdrop` (`expect/actual`)**: Downloads/downsamples artwork off the main thread into a pre-blurred `ImageBitmap` on Android and Desktop, rendering backdrops with a 420ms crossfade and contrast scrim at zero per-frame blur cost.
- **`ColorExtractor`**: Samples artwork tint via `androidx.palette.ktx` on Android (`ColorExtractor.android.kt`) and AWT pixel quantization on Desktop (`ColorExtractor.desktop.kt`).
