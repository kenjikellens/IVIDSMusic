# Compose Multiplatform Screen & UI Specifications

All screens run from shared Kotlin composables in `composeApp/src/commonMain/kotlin/com/kenjigames/ividsmusic/ui/` across Windows Desktop, Android Mobile, and Android TV.

## Design System & Shell Architecture

- **Pure Neutral Palette (`IvidsColorTokens.kt`)**: Zero-tint neutral black/charcoal (`#000000` / `#090909` ObsidianStage, `#0A0A0A` AbyssRail, `#141414` GlassSurface, `#1C1C1C` ElevatedDeck, `#4DD0E1` PrimaryCyan for active controls, `#FFFFFF` ChalkInk).
- **Strictly Borderless UI**: Cards, pills, switches, segmented chips, and dialogs avoid borders/outlines and rely on tonal surface contrast (only TV D-pad focus uses `tvFocusable` focus rings).
- **Orthogonal Composition (`IvidsInteraction.kt`, `IvidsControls.kt`, `IvidsLoaders.kt`)**:
  - Style roles (`IvidsHoverStyle`: `NEUTRAL`, `ON_LIGHT`, `PRIMARY_ACCENT`, `DESTRUCTIVE`) stay decoupled from `Modifier.ividsHoverZoom`, `Modifier.ividsTouchFeedback`, and press-only `ividsPressRipple`.
  - Shared primitives (`IvidsIconButton`, `IvidsPrimaryButton`, `IvidsPlayCircleButton`, `IvidsTonalPill`, `IvidsSegmentedBar`, `IvidsSwitch`, `IvidsScrubber`, `IvidsVinylLoader`, `IvidsVinylLoadingBox`) are reused everywhere.
- **Adaptive Shell (`ui/shell/`)**:
  - **Mobile (`< 600dp`, `MobileShellChrome.kt`)**: Top header (`MobileTopHeader`), 5-tab bottom navigation (`MobileBottomBar`), and swipeable `MiniPlayerBar` opening `NowPlayingSheet`.
  - **Desktop & TV (`DesktopShellChrome.kt`, `DesktopPlayerChrome.kt`)**: Collapsible `SidebarNavigationRail` (236dp expanded / 78dp compact), centered search pill `DesktopTopBar` with left-pinned back button, persistent 88dp `DesktopPlayerDeck`, and toggleable `QueueSideSheet`.

## Navigation Routes (`AppRoute.kt`)

1. **Home (`HomeScreen`, `HomeViewModel`)**: Curated charts, genre rows, top artists, top albums, and "Recommended for You" hero (`LivingAlbumHero`, `MusicCard`). Uses `IvidsVinylLoadingBox` during initial load.
2. **Recommended (`RecommendedScreen`, `RecommendedViewModel`)**: Algorithmic Discovery Mix, Genre Fusion, and Taste Match sections driven by `RecommendationEngine` local scores, with top interest chips and manual refresh.
3. **Search (`SearchScreen`, `SearchViewModel`)**: Category filters (`IvidsSegmentedBar`: All, Songs, Artists, Albums), release-year range panel, browse genre cards, pagination, and `TrackRowItem` results.
4. **Library (`LibraryScreen`, `LibraryViewModel`)**: Tabs for Liked Songs, Saved Albums, Playlists, and History, with live local search filtering, batch Play/Shuffle pills, and playlist creation dialog.
5. **Profile (`ProfileScreen`, `ProfileViewModel`)**: Listener stats cards (Liked Songs, Playlists, Minutes Played), editable local profile modal, recent listening carousel, and top genres progress breakdown.
6. **Downloader (`DownloaderScreen`, `DownloaderViewModel`)**: Direct YouTube URL/playlist downloader with per-download bitrate selector (`BitrateSelector`: 128/196/320 kbps), active progress controls, custom save directory picker, and offline track library.
7. **Settings (`SettingsScreen`, `SettingsViewModel`)**: Flat settings surface (`#090909`) linking via right-push sub-pages (`AppRoute.SettingsDetail`) for Language (16 languages), UI Scale (`75%..150%`), Default Audio Quality, and Update Mode (`AUTO`, `MANUAL`, `OFF`, `DEVELOPER`), plus cache clearing, score reset, and reset-defaults confirmations.
8. **Settings Detail (`AppRoute.SettingsDetail(SettingsSection)`)**: Push sub-page rendering hover-reactive flat option lists (`IvidsSelectableDialogItem`) instead of nested cards or modal popups.
9. **Artist Detail (`ArtistDetailScreen`, `ArtistViewModel`)**: `LivingAlbumHero`, top tracks list, full discography `MusicCard` row, and related artists.
10. **Album Detail (`AlbumDetailScreen`, `AlbumViewModel`)**: Cover hero with Play All / Shuffle / Save Album actions and ordered `TrackRowItem` tracklist.
11. **Song Detail (`SongDetailScreen`, `SongDetailViewModel`)**: Artwork hero, queue/playlist/download/like/artist/album action pills, audio stream quality readout, and similar tracks.
12. **Playlist Detail (`PlaylistDetailScreen`, `PlaylistDetailViewModel`)**: Custom playlist hero, inline title/description rename, move up/down or remove track controls, batch playback, and playlist deletion.
