# Changelog

All notable changes to the **IVIDS Music** project will be documented in this file.

---

## [2.0.0] - 2026-05-19

### Added
- **Native OTA Updater**:
  - Implemented a background Kotlin `UpdateManager` for downloading APK updates and prompting the Android system Package Installer safely.
  - Declared `filepaths.xml` provider directory configurations and registered `FileProvider` permissions in `AndroidManifest.xml`.
  - Added a high-fidelity visual download progress bar overlay to the app header styled with neon-green themes and smooth CSS cubic-bezier transitions.
  - Implemented `window.Updater.initAutoCheck()` to trigger a silent boot check after a 3-second delay on application startup.
- **Song Details Expansion**:
  - Added visual action buttons for `Play Next` (⏭), `Add to Queue` (➕), and `Add to Playlist` (📂) to the song details layout (`song.html`).
  - Updated `pages.js:initSong()` controller to parse custom parameterized navigation parameters (`params.track`), allowing full metadata presentation and queue setups for any target track.
  - Integrated offline downloader actions via `window.AndroidAPI.downloadTrack` when executing download commands outside playback.
- **Unified Search Bar**:
  - Created a root-level `unified-search-bar` container in `search.html` that remains always visible at the top of the viewport on Mobile/TV screens.
  - Integrated a tap-friendly `✕` clear button that displays dynamically as characters are typed and resets the view state when clicked.

### Changed
- **Mobile & TV Card Click Interception**:
  - Modified card click listeners in `cards.js` to detect Mobile and TV viewports. Song card clicks now route the user to the Song Details page (`song.html`) instead of immediately starting playback, matching standard Android/TV media browser patterns.
- **UI Localization Refactoring**:
  - Audited all views and removed hardcoded UI texts across Settings, Artist, Album, Playlist, and Recommended pages, replacing them with dynamic `data-i18n` translations.
  - Refactored `pages.js` search text and `player.js` playback state updates (e.g. `"Searching YouTube..."`, `"Downloading MP3..."`) to pull translations from the active language file.
  - Synchronized translation updates across all 16 supported languages (`en`, `nl`, `fr`, `de`, `es`, `pt`, `it`, `zh`, `hi`, `ar`, `ru`, `ro`, `ja`, `tr`, `ko`, `pl`) while maintaining BOM UTF-8 compliance.

### Fixed
- **Mobile Tap Highlights**:
  - Resolved ugly default browser tap highlights on Android touch screens by applying global `-webkit-tap-highlight-color: transparent !important` CSS rules.
- **TV Auto-Popping Keyboards**:
  - Solved spatial navigation keyboard triggers in `tv-nav.js` by enforcing `readOnly = true` constraints on all inputs during D-pad navigation. Clicking `Enter` on the focused input unlocks the write mode and opens the soft keyboard.
- **Focus Rings and Glow Styling**:
  - Corrected border overlaps and glow highlights for settings cards, sidebar links, and song cover thumbnails under TV focus and PC hover states.

[2026-05-19] [EDITED] app/src/main/assets/gui/pages/search.html: Fixed invalid duplicate closing div tag in the search promo layout.
[2026-05-19] [EDITED] app/src/main/assets/gui/index.css: Added matching bottom border radii to card info box to eliminate bottom corner bleeding.
[2026-05-19] [EDITED] app/src/main/assets/gui/index.css: Refactored music card play button overlays and library track item buttons to strip rule-violating scale and rotate hover transitions.
[2026-05-19] [EDITED] app/src/main/assets/gui/index.css: Added header search container auto-hiding on the search page and enabled page-level unified search bar visibility on all platforms.
[2026-05-19] [EDITED] app/src/main/assets/gui/index.css: Refactored search promo close button and search icon transitions to strip rule-violating hover/focus scale values and center-aligned search input wrapper.
[2026-05-19] [EDITED] app/src/main/assets/gui/tv-nav.css: Added TV mode D-pad spatial navigation focus styles with thick flush white border outline for the search page input.
[2026-05-19] [EDITED] app/src/main/assets/gui/index.css: Redesigned the search landing page layout, centered the onboarding promo card, adjusted hero padding, and added a premium grid layout for browse categories.
[2026-05-19] [EDITED] app/src/main/assets/gui/pages/search.html: Redesigned the search landing page structure by grouping the title, tagline, onboarding promo card, and browse categories grid inside the browse-hero container for unified toggling.
[2026-05-19] [EDITED] app/src/main/assets/logic/tv-nav.js: Added auto-detection for TV mode to disable spatial navigation and locked readOnly inputs on PC/Mobile.
[2026-05-19] [EDITED] app/src/main/assets/gui/index.css: Scoped portrait media query search page header styles to only apply when not hidden, preventing empty state results display on mobile.
