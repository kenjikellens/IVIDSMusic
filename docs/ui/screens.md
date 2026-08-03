# 📱 Native Jetpack Compose Screen Specifications

This document defines the layout, state management, and Jetpack Compose component requirements for all **11 screens** in the native **IVIDS Music** Android application.

---

## 🎨 Design System & Theme Principles

- **Theme**: Dark Glassmorphism aesthetic using semi-transparent surfaces (`Surface` with alpha), subtle blur effects, and dynamic gradients.
- **Dynamic Accent Tinting**: Uses `Coil` image loader's palette extraction to sample dominant colors from current album art and apply smooth color transitions across backgrounds and player UI.
- **Responsive Layout**:
  - **Mobile**: Single pane, bottom navigation bar (`NavigationBar`), sliding bottom player bar.
  - **Tablet / TV**: Dual-pane / sidebar navigation (`NavigationRail` or TV drawer), high-visibility focus borders for D-pad navigation.

---

## 📄 Screen Breakdown & Compose Architecture

### 🏠 1. Home Screen (`HomeScreen.kt`)
- **ViewModel**: `HomeViewModel`
- **UI State**: `HomeUiState` (Loading, Success(categories, recommendations), Error)
- **Components**:
  - `HeroRecommendationCard`: Featured track/album banner with glass blur overlay and play button.
  - `HorizontalCardRow`: Scrollable row of `MusicCard` elements with title and "See All" action.
  - `GenreSection`: Categorized rows (Pop, Rock, Hip-Hop, Hardcore, 90's, Electronic).
  - `ShimmerSkeletonRow`: Animated placeholders while `HomeUiState.Loading`.
- **Interactions**: Tapping a card launches playback or navigates to album/artist detail.

### 🔍 2. Search Screen (`SearchScreen.kt`)
- **ViewModel**: `SearchViewModel`
- **UI State**: `SearchUiState` (query, categoryFilter, yearRange, results(artists, songs, albums), isLoading)
- **Components**:
  - `GlassSearchBar`: Centered text input with clear button and voice search support.
  - `CategoryFilterChips`: Scrollable row of filter chips (**All**, **Songs**, **Artists**, **Albums**).
  - `YearRangeSlider`: Expandable filter sheet with a dual-thumb slider for filtering release years.
  - `SearchResultsGrid`: LazyVerticalGrid or LazyColumn displaying search results.
  - `PaginationLoader`: Bottom shimmer indicator during infinite scrolling.

### 🎤 3. Artist Detail Screen (`ArtistDetailScreen.kt`)
- **ViewModel**: `ArtistViewModel(artistId)`
- **UI State**: `ArtistUiState` (artistInfo, topTracks, albums, isLoading)
- **Components**:
  - `ArtistHeaderBanner`: Large header image with dynamic gradient overlay, artist avatar, name, and total fans/listeners count.
  - `PopularTracksList`: Top 5-10 tracks with track numbers, titles, durations, and play counts.
  - `DiscographyGrid`: Scrollable grid of album cards released by the artist.

### 💿 4. Album Detail Screen (`AlbumDetailScreen.kt`)
- **ViewModel**: `AlbumViewModel(albumId)`
- **UI State**: `AlbumUiState` (albumDetails, tracks, isLoading)
- **Components**:
  - `AlbumHeader`: High-res cover image, title, artist link, release date, and genre chips.
  - `BatchActionButtons`: Prominent "Play All" button (primary accent) and "Shuffle" button.
  - `TrackList`: `LazyColumn` of track items showing track number, title, artist, duration, and overflow menu (Add to Playlist, Download).

### 🎵 5. Song Detail Screen (`SongDetailScreen.kt`)
- **ViewModel**: `SongDetailViewModel(trackId)`
- **UI State**: `SongDetailUiState` (track, isLiked, isDownloaded, isPlaying)
- **Components**:
  - `ExpandedCoverArt`: Full-width rounded cover image with dynamic shadow glow.
  - `TrackMetadataHeader`: Track title, clickable artist name, and album title.
  - `InteractiveScrubber`: Smooth slider showing current playback position and total track duration.
  - `PlaybackControls`: Prominent Play/Pause toggle, Previous, Next, Shuffle, Repeat.
  - `ActionRow`: Like button (heart toggle), Add to Playlist button, Download button.

### 📚 6. Library Screen (`LibraryScreen.kt`)
- **ViewModel**: `LibraryViewModel`
- **UI State**: `LibraryUiState` (likedSongs, savedAlbums, customPlaylists, searchQuery)
- **Components**:
  - `LibraryTabRow`: Tabs for **Liked Songs**, **Saved Albums**, and **Playlists**.
  - `LocalSearchFilter`: Sticky search bar to filter library content locally in real-time.
  - `BatchLibraryActions`: "Play All Saved" and "Shuffle Saved" header buttons.
  - `EmptyStatePlaceholder`: Illustration and quick link to explore music when empty.

### 📜 7. Playlist Detail Screen (`PlaylistDetailScreen.kt`)
- **ViewModel**: `PlaylistDetailViewModel(playlistId)`
- **UI State**: `PlaylistDetailUiState` (playlist, tracks, isEditing)
- **Components**:
  - `PlaylistHeader`: Editable cover photo, editable title, track count, and total duration.
  - `ReorderableTrackList`: Drag-and-drop track reordering list items with swipe-to-delete.
  - `BatchPlayHeader`: Play and Shuffle controls for the custom playlist.

### 💾 8. Downloader & Saved Media Screen (`DownloaderScreen.kt`)
- **ViewModel**: `DownloaderViewModel`
- **UI State**: `DownloaderUiState` (activeDownloads, savedTracks, storageInfo)
- **Components**:
  - `DirectDownloadBar`: Input field for pasting YouTube/stream links to trigger manual downloads.
  - `ActiveDownloadsCard`: Progress bars, download speed metrics, and cancellation controls.
  - `OfflineTrackList`: List of downloaded MP3 files with playback and delete options.
  - `StorageUsageIndicator`: Visual bar graph displaying used vs available storage.

### 👤 9. Profile Screen (`ProfileScreen.kt`)
- **ViewModel**: `ProfileViewModel`
- **UI State**: `ProfileUiState` (userProfile, stats, recentlyPlayed, topGenres)
- **Components**:
  - `ProfileHeader`: Avatar image picker, display name, and bio text editor.
  - `StatsGrid`: Metric cards for **Liked Songs**, **Playlists**, and **Minutes Played**.
  - `RecentlyPlayedRow`: Horizontal scrollable row of the last 20 played tracks.
  - `TopGenresChart`: Visual breakdown of top listened music genres.

### ✨ 10. Recommended Screen (`RecommendedScreen.kt`)
- **ViewModel**: `RecommendedViewModel`
- **UI State**: `RecommendedUiState` (recommendedTracks, recommendedArtists, topInterests)
- **Components**:
  - `AlgorithmicFeedGrid`: Personalized track recommendations based on local interest scores.
  - `InterestTopicsRow`: Chips showing top interests powering current recommendations.
  - `RefreshFeedButton`: Manual refresh trigger to recalculate recommendations.

### ⚙️ 11. Settings Screen (`SettingsScreen.kt`)
- **ViewModel**: `SettingsViewModel`
- **UI State**: `SettingsUiState` (uiScale, languageCode, appVersion, updateStatus)
- **Components**:
  - `ScaleFactorSelector`: Segmented button control for UI scale (75%, 90%, 100%, 110%, 125%, 150%).
  - `LanguageDropdown`: Language picker supporting 13 languages with instant app relocalization.
  - `AppUpdateCard`: Update check status, changelog viewer, and "Download Update" action.
  - `DataManagementSection`: Clear cache, purge recommendation scores, reset application defaults.
