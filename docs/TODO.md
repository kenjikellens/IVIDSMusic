# IVIDS Music - TODO

All previous legacy web/Electron and initial KMP port items (Search screen functionality, Home Artists/Albums carousels, Library spacing, CSS/DOM virtualization, and P0 playback stability features) have been completed or retired with the Compose Multiplatform architecture.

The remaining active items below reflect open UX enhancements tracked in [FEATURES_ROADMAP.md](../FEATURES_ROADMAP.md):

## Completed P0 & Spotify-Style P1/P2 Tasks
- [x] **Search History**: Persist and display up to 10 recent search queries with quick replay, single remove, and clear actions inside `SearchScreen`.
- [x] **Library & Playlist Sorting / Offline Filter**: Sort modes (`Recent`, `Title`, `Artist`, `Duration`), `Offline only` filter, and Desktop track table headers in `LibraryScreen` and `PlaylistDetailScreen`.
- [x] **Queue Reordering**: Move up/down reordering and `Now Playing` / `Next Up` sections inside `QueueSideSheet` and `NowPlayingSheet`.
- [x] **Desktop Context Menus**: Right-click context menus on tracks and cards (`Play Next`, `Add to Queue`, `Add to Playlist`, `Download`, `View Artist`, `View Album`).
- [x] **Live Audio Cache Size Indicator**: Live `fx_audio_cache` MB counter in `SettingsScreen` refreshed on open and after clearing cache.
- [x] **Infinite Autoplay / Track Radio**: Auto-enqueue similar tracks via `MusicDiscoveryRepository` when the active queue finishes, toggleable in `QueueSideSheet`, `NowPlayingSheet`, and `SettingsScreen`.
- [x] **Desktop Window Bounds Restore**: Remember Desktop window size and position across restarts in `window_bounds.json` with monitor-bounds validation.

## Open P1 Tasks (Daily UX)
- [ ] **Windows System Media Transport Controls (SMTC)**: Handle global media keys while the Desktop window is unfocused.

## Open P2 Tasks (Quality of Life)
- [ ] **Offline Mode & Network Indicator**: Detect offline status cleanly and surface instant filtering to locally downloaded media.
- [ ] **YouTube Playlist Import**: Save a YouTube playlist directly into a local IVIDS playlist without requiring full downloads.
- [ ] **Library Backup Export / Import**: Export and import playlists, likes, and preferences as JSON or M3U.
- [ ] **Windows System Tray Minimize**: Allow background playback in the Windows system tray when closing or minimizing the Desktop window.
