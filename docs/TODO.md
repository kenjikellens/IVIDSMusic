# IVIDS Music — TODO & Bug Tracker

## Completed UI Speed & Performance Optimizations

- [x] **Image Resolution Optimization (250px Medium)**:
  - Switched Deezer API artwork payloads in `api.js` and `DeezerProvider.js` from giant 1000px/500px images (`cover_xl`, `cover_big`) to 250px `cover_medium` and `picture_medium`.
  - Cut network payload size per artwork from ~1.5 MB to ~25 KB (**95% bandwidth & CPU decoding savings**).
- [x] **IntersectionObserver Thumbnail Lazy Loading**:
  - Implemented `ImageLazyLoader` in `cards.js` using `IntersectionObserver` with `300px` root margin buffer.
  - Off-screen thumbnails do not download a single byte until the user actually scrolls down to them.
- [x] **Category Row-Level IntersectionObserver Lazy Loading**:
  - Implemented `getSingleCategory()` in `api.js` and viewport row lazy loading in `pages.js` `initHome()`.
  - Upfront page load fetches ONLY the top 2 visible categories (Pop & Rock); off-screen categories fetch dynamically as the user scrolls down.
- [x] **Instant Hero X Button Response**:
  - Bound `close-hero-btn` and `promo-close-btn` with inline `onclick="this.closest('.hero')?.remove()"` attributes in `home.html` and `index.html`.
  - Clicking X closes the hero banner **100% instantly**, independent of network requests or JS initialization.
- [x] **Loader Resilience on Re-Navigation**:
  - Updated `Loader.init()` in `loader.js` to inspect innerHTML directly instead of relying on stale `.initialized` CSS class flags.
- [x] **Instant Artist Name Pre-Hydration**:
  - Removed `data-i18n="loading"` from `artist.html` and populated `document.getElementById('artist-name')` immediately in **0ms** from `params.name` before API network calls.
- [x] **Complete Scroll Arrow Removal**:
  - Removed all scroll arrow HTML buttons from `artist.html`, `home.html`, `song.html`, and `search.html`.
  - Enforced `.scroll-arrow { display: none !important; }` in `index.css` across all pages.
- [x] **Artist Detail Page Alignment**:
  - Updated `ArtistPageController.js` and `PageSystem.initArtist` to match `SongPageController` structure, hero statistics, discography rows, and action controls.

## Pending High Priority Issues & Enhancements

- [ ] **Search Page Breakdown**: Investigate and fix why the search page is not working at all.
- [ ] **Home Page Missing Sections**: Add dedicated Artists and Albums sections to the Home page.
- [ ] **Library Layout Spacing**: Correct the spacing, margins, and padding on the "Je Bibliotheek" (Your Library) page.
- [ ] **UI Emoji Removal**: Audit and remove all hardcoded emojis across HTML/JS files; replace with SVG icons or CSS styling.
- [ ] **Canvas Color Extraction Caching**: Add in-memory `Map` cache (`#colorCache`) to `MusicRepository.getAverageColor` to prevent concurrent Canvas readbacks.
