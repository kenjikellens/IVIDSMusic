[2026-02-26] EDITED .agents/rules/ai-rules.md: Updated AI rules file per user request
[2026-02-26] DELETED AI_RULES.md: Removed duplicate rules file
[2026-02-26] ADDED CHANGELOG.md: Created initial changelog with entries for recent changes
[2026-02-26] EDITED CHANGELOG.md: Added changelog entries for recent changes
[2026-02-26] EDITED app/src/main/assets/gui/pages/home.html: Added close (X) button to hero section
[2026-02-26] EDITED app/src/main/assets/logic/pages.js: Implemented hero dismissal logic with localStorage persistence
[2026-02-26] EDITED app/src/main/assets/gui/pages/home.html: Fixed close button positioning within hero section
[2026-02-26] EDITED app/src/main/assets/logic/pages.js: Changed hero dismissal to session-bound (resets on reload)
[2026-02-26] EDITED app/src/main/assets/logic/api.js: Added `getArtistByName`, `getArtistTopTracks`, and `getArtistAlbums` methods
[2026-02-26] EDITED app/src/main/assets/gui/pages/artist.html: Replaced placeholder with full blob-animated hero, top tracks, and albums layout
[2026-02-26] EDITED app/src/main/assets/gui/index.css: Added styles for new artist page hero, avatar, and stats
[2026-02-26] EDITED app/src/main/assets/logic/pages.js: Implemented `initArtist` to fetch and render full artist profile, tracks, and albums
[2026-02-26] EDITED app/src/main/assets/logic/api.js: Fixed CORS/fetch error on Artist page by enforcing `proxyUrl` routing
[2026-02-26] EDITED app/src/main/assets/logic/tv-nav.js: Fixed bug where Backspace/Delete was blocked in text inputs
[2026-02-26] EDITED app/src/main/assets/gui/pages/search.html: Removed real-time input mirroring from browse search bar to top search bar
[2026-02-26] EDITED app/src/main/assets/logic/pages.js: Cleared deprecated `handleBrowseInput` mirror function
[2026-02-26] EDITED app/src/main/assets/logic/api.js: Fixed inaccurate fan/album counts by fetching direct `/artist/{id}` endpoint; fixed album artist showing 'Unknown'
[2026-02-26] EDITED app/src/main/assets/logic/pages.js: Pass artist name to `getArtistAlbums` for correct album card labels
[2026-02-26] EDITED app/src/main/assets/logic/api.js: Filter albums to `record_type === 'album'` only (excludes singles/EPs/compilations)
[2026-02-26] EDITED app/src/main/assets/logic/pages.js: Album count now shows actual studio albums instead of Deezer's inflated `nb_album`
[2026-02-26] EDITED app/src/main/assets/logic/cards.js: Removed sub-labels for Artist cards to match minimalist circular redesign
[2026-02-26] EDITED app/src/main/assets/gui/index.css: Implemented minimalist circular look for Artist cards (removed glass background, centered name)
[2026-02-26] EDITED app/src/main/assets/logic/api.js: Added `getAlbumDetails` method to fetch album metadata and tracks
[2026-02-26] ADDED app/src/main/assets/gui/pages/album.html: Created initial template for the album page with hero section and tracklist
[2026-02-26] EDITED app/src/main/assets/logic/pages.js: Implemented `initAlbum` for dynamic rendering of album details and tracks
[2026-02-26] EDITED app/src/main/assets/gui/index.css: Added responsive styles for the album hero and tracklist rows
[2026-02-26] EDITED app/src/main/assets/logic/cards.js: Fixed syntax error in album card click handler
