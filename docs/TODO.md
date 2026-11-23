# IVIDS Music - TODO

All previous legacy web/Electron and initial KMP port items (Search screen functionality, Home Artists/Albums carousels, Library spacing, CSS/DOM virtualization, and P0 playback stability features) have been completed or retired with the Compose Multiplatform architecture.

The remaining active items below reflect open UX enhancements tracked in [FEATURES_ROADMAP.md](../FEATURES_ROADMAP.md):

## Open P1 Tasks (Daily UX)
- [ ] **Search History**: Persist and display recent search queries with quick replay and clear actions inside `SearchScreen`.
- [ ] **Library & Playlist Sorting / Offline Filter**: Add sort modes (title, artist, duration, date added) and an "Offline only" filter in `LibraryScreen` and `PlaylistDetailScreen`.
- [ ] **Queue Reordering**: Support drag-and-drop or move up/down actions inside `QueueSideSheet` and `NowPlayingSheet`.
- [ ] **Windows System Media Transport Controls (SMTC)**: Handle global media keys while the Desktop window is unfocused.
- [ ] **Desktop Context Menus**: Add right-click context menus on tracks and cards (`Play Next`, `Add to Queue`, `Add to Playlist`, `Download`, `View Artist`).
- [ ] **Live Audio Cache Size Indicator**: Display exact `fx_audio_cache` / exo cache disk usage in `SettingsScreen` before clearing cache.

## Open P2 Tasks (Quality of Life)
- [ ] **Infinite Autoplay / Track Radio**: Auto-enqueue similar tracks via `RecommendationEngine` when the active queue finishes.
- [ ] **Offline Mode & Network Indicator**: Detect offline status cleanly and surface instant filtering to locally downloaded media.
- [ ] **YouTube Playlist Import**: Save a YouTube playlist directly into a local IVIDS playlist without requiring full downloads.
- [ ] **Library Backup Export / Import**: Export and import playlists, likes, and preferences as JSON or M3U.
- [ ] **Windows System Tray Minimize & Window Bounds Restore**: Allow background playback in system tray and remember Desktop window size/position across restarts.
