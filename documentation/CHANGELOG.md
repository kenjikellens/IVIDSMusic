[2026-03-03] EDITED app/src/main/assets/logic/api.js: Fixed critical syntax error causing UI loading failure
[2026-03-03] EDITED app/src/main/assets/logic/recommendations.js: Moved initRecommended logic into DiscoveryEngine module
[2026-03-03] EDITED app/src/main/assets/logic/router.js: Updated dynamic imports for recommendations page to use DiscoveryEngine
[2026-03-03] EDITED app/src/main/assets/logic/pages.js: Removed initRecommended logic, delegating to recommendations.js
[2026-03-03] ADDED app/src/main/assets/logic/recommendations.js: New local recommendation engine with scoring for tracks, artists, and genres
[2026-03-03] ADDED app/src/main/assets/gui/pages/recommended.html: New "For You" page layout
[2026-03-03] ADDED app/src/main/assets/gui/svg/like.svg: New heart icon for liking tracks
[2026-03-03] ADDED app/src/main/assets/gui/svg/dislike.svg: New thumbs-down icon for disliking tracks
[2026-03-03] EDITED app/src/main/assets/logic/player.js: Integrated Like/Dislike controls and automated scoring triggers (+1 on click)
[2026-03-03] EDITED app/src/main/assets/logic/cards.js: Added scoring trigger for artist clicks
[2026-03-03] EDITED app/src/main/assets/logic/api.js: Added getArtistTopTracks and getGenreTracks methods for recommendation engine
[2026-03-03] EDITED app/src/main/assets/logic/pages.js: Implemented initRecommended logic for personalized content rendering
[2026-03-03] EDITED app/src/main/assets/gui/index.html: Added "For You" link to sidebar and Like/Dislike buttons to player bar
[2026-03-03] EDITED app/src/main/assets/gui/lang/*.json: Added localization keys for recommendation engine and "For You" page
[2026-03-03] EDITED documentation/algorithm.md: Refined scoring rules (immediate +1 for track clicks)
[2026-03-01] EDITED app/src/main/assets/gui/index.css: Added standard background-clip property for text compatibility
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
[2026-02-26] EDITED app/src/main/assets/gui/index.css: Made header search bar permanently wide and removed focus expansion
[2026-02-26] EDITED app/src/main/assets/logic/player.js: implemented smooth progress updates via requestAnimationFrame and unified slider interaction
[2026-02-26] EDITED app/src/main/assets/gui/index.html: added step="any" to progress slider for high-precision smooth movement
[2026-02-27] ADDED gui/lang/: Created directory for internationalization files (EN, NL, FR)
[2026-02-27] ADDED logic/language-manager.js: New module for multi-language support and dynamic UI translation
[2026-02-27] EDITED logic/settings-manager.js: Integrated LanguageManager for language persistence
[2026-02-27] EDITED logic/router.js: Added automatic translation of dynamic pages on load
[2026-02-27] EDITED index.html: Added data-i18n attributes for main navigation and player controls
[2026-02-27] EDITED pages/settings.html: Added language selection UI and translation keys
[2026-02-27] EDITED logic/language-manager.js: Refined translation logic to protect icons using child-check and spans; disabled query params for Android compatibility
[2026-02-27] EDITED gui/index.html: Wrapped sidebar and player labels in spans for non-destructive translation
[2026-02-27] EDITED gui/pages/home.html: Wrapped hero buttons and titles in spans for safe translation
[2026-02-27] EDITED gui/pages/search.html: Wrapped results labels and category titles in spans for localization
[2026-02-27] EDITED logic/cards.js: Added `data-i18n` support for dynamic row headers on the home page
[2026-02-27] EDITED gui/lang/*.json: Added missing keys for home page row headers and dynamic UI elements
[2026-02-27] EDITED logic/config.js: Improved environment detection and added explicit `isNative` property
[2026-02-27] EDITED logic/api.js: Fixed typos in `isNative` check and converted `proxyUrl` to dynamic getter to resolve 'failed to fetch' errors
[2026-02-27] EDITED logic/player.js: Switched to `MusicAPI._fetch` for playback and save requests to ensure consistent CORS/proxy handling
[2026-02-27] EDITED logic/config.js: Simplified `Config` to static properties for better cross-module reliability
[2026-02-27] EDITED MainActivity.kt: Refactored `shouldInterceptRequest` to use robust path-based matching and improved logging
[2026-02-27] EDITED logic/api.js: Implemented centralized proxy-aware `_fetch` helper to solve CORS 'failed to fetch' errors in Android WebView
[2026-02-27] EDITED logic/player.js: Fixed critical syntax error (misplaced `handleError` method) that broke script parsing
[2026-02-27] EDITED logic/api.js: Refined `isExternal` detection to use `window.location.origin` for better CORS handling in Web Mode
[2026-02-27] EDITED gui/temp_server.js: Implemented robust CORS support (including `OPTIONS` preflight) and improved proxy logging
[2026-02-27] EDITED logic/config.js: Updated `SERVER_URL` to dynamically match localhost/127.0.0.1 origins for web compatibility
[2026-02-27] EDITED logic/config.js: Fixed URL parsing issue when app is loaded via `file:///` protocol in Web Mode
[2026-02-27] EDITED logic/api.js: Restored `corsproxy.io` for external requests (Deezer) in Web Mode to fix cards failing to load
[2026-02-27] ADDED startUp.py: Created a Python script to automatically run the local Node server and open the browser in 1 click
[2026-02-27] EDITED gui/pages/settings.html, logic/settings-manager.js, gui/index.css: Redesigned the Interface Scale UI into a stepper control (`- 100% +`) allowing 10% increments from 50% to 200%.
[2026-02-27] EDITED gui/tv-nav.css: Removed empty `body.tv-mode` ruleset to fix CSS lint warning
[2026-02-27] ADDED gui/svg/edit.svg: New pencil/edit SVG icon for the language edit button
[2026-02-27] EDITED gui/pages/settings.html: Replaced inline language buttons with display + edit icon and added language selection modal popup
[2026-02-27] EDITED gui/index.css: Added language display row, modal overlay, modal card, and language option styles with glassmorphism and animations
[2026-02-27] EDITED logic/language-manager.js: Rewrote with modal open/close logic, LANGUAGE_NAMES map, and document-level event delegation
[2026-02-27] EDITED logic/pages.js: Added LanguageManager.updateLanguageDisplay() call to initSettings
[2026-02-27] EDITED gui/tv-nav.css: Added TV focus ring styles for language edit button, modal close, and language options
[2026-02-27] ADDED gui/lang/de.json, gui/lang/es.json, gui/lang/pt.json, gui/lang/it.json: Added German, Spanish, Portuguese, and Italian translations
[2026-02-27] EDITED logic/language-manager.js: Registered 4 new supported languages to auto-populate the language modal

[2026-02-27] EDITED gui/index.css: Refined language option hover/focus styles to avoid layout shift while maintaining high visibility
[2026-02-27] ADDED documentation/future_plans.md: Created a roadmap of feature ideas for the app's future evolution
[2026-02-27] ADDED gui/lang/zh.json, gui/lang/hi.json, gui/lang/ar.json: Added Mandarin Chinese, Hindi, and Arabic translations (top most spoken languages)
[2026-02-27] EDITED logic/language-manager.js: Registered 3 new languages: Chinese, Hindi, and Arabic
[2026-02-27] ADDED gui/lang/ru.json, gui/lang/ro.json, gui/lang/ja.json: Added Russian, Romanian, and Japanese translations
[2026-02-27] EDITED logic/language-manager.js: Registered 3 more languages: Russian, Romanian, and Japanese
[2026-02-27] EDITED gui/index.css, logic/language-manager.js: Implemented responsive column-based grid for language selector; reduced padding and font size for a more compact UI
[2026-02-28] EDITED app/src/main/assets/gui/index.css: Reduced standard card sizes and skeleton widths for a more compact layout across all breakpoints.
[2026-02-28] EDITED app/src/main/assets/gui/index.css: Implemented white glassy background, backdrop blur, and refined borders for cards and track items in the Library section.
[2026-02-28] EDITED documentation/future_plans.md: Expanded all future feature ideas with more detailed descriptions and specific technical/UX implementations.
[2026-02-28] ADDED app/src/main/assets/gui/svg/info.svg: Info circle icon for the player's More Info button
[2026-02-28] EDITED app/src/main/assets/gui/index.html: Added More Info button to the player bar
[2026-02-28] EDITED app/src/main/assets/logic/player.js: Enabled More Info button on track load and playback
[2026-02-28] EDITED app/src/main/assets/logic/api.js: Added getTrackDetails and getRelatedTracks for rich song metadata
[2026-02-28] EDITED app/src/main/assets/logic/pages.js: Implemented initSong for the new Song Detail page
[2026-02-28] ADDED app/src/main/assets/gui/pages/song.html: Created template for Song Detail page with hero and metadata
[2026-02-28] EDITED app/src/main/assets/gui/index.css: Added comprehensive styles for the Song Detail page
[2026-02-28] EDITED app/src/main/assets/gui/lang/*.json: Added internationalization keys for the Song Detail page in all 13 languages
[2026-02-28] EDITED app/src/main/assets/logic/pages.js: Fixed bug where album metadata wasn't updating from the detailed API fetch.
[2026-02-28] EDITED app/src/main/assets/logic/player.js: Fixed bug where the Song Detail page wouldn't refresh when a new track was played from within the page.
[2026-02-28] EDITED app/src/main/assets/gui/temp_server.js: Updated static root to `assets/` and set default `/` route to `gui/index.html`.
[2026-02-28] EDITED StartUp.py: Updated browser URL to `gui/index.html` for out-of-the-box project startup.

[2026-02-28] EDITED StartUp.py: Updated startup script for correct URL and other tweaks
[2026-02-28] EDITED app/src/main/assets/gui/index.css: Minor style adjustments after recent UI tweaks
[2026-02-28] EDITED app/src/main/assets/gui/index.html: Updated More Info button integration
[2026-02-28] EDITED app/src/main/assets/gui/lang/ar.json: Updated translation keys for Song Detail page
[2026-02-28] EDITED app/src/main/assets/gui/lang/de.json: Updated translation keys for Song Detail page
[2026-02-28] EDITED app/src/main/assets/gui/lang/en.json: Updated translation keys for Song Detail page
[2026-02-28] EDITED app/src/main/assets/gui/lang/fr.json: Updated translation keys for Song Detail page
[2026-02-28] EDITED app/src/main/assets/gui/lang/hi.json: Updated translation keys for Song Detail page
[2026-02-28] EDITED app/src/main/assets/gui/lang/it.json: Updated translation keys for Song Detail page
[2026-02-28] EDITED app/src/main/assets/gui/lang/ja.json: Updated translation keys for Song Detail page
[2026-02-28] EDITED app/src/main/assets/gui/lang/nl.json: Updated translation keys for Song Detail page
[2026-02-28] EDITED app/src/main/assets/gui/lang/pt.json: Updated translation keys for Song Detail page
[2026-02-28] EDITED app/src/main/assets/gui/lang/ro.json: Updated translation keys for Song Detail page
[2026-02-28] EDITED app/src/main/assets/gui/lang/ru.json: Updated translation keys for Song Detail page
[2026-02-28] EDITED app/src/main/assets/gui/lang/zh.json: Updated translation keys for Song Detail page
[2026-02-28] EDITED app/src/main/assets/gui/temp_server.js: Updated static root and default route
[2026-02-28] EDITED app/src/main/assets/logic/api.js: Added getTrackDetails and getRelatedTracks
[2026-02-28] EDITED app/src/main/assets/logic/pages.js: Implemented initSong and bug fixes
[2026-02-28] EDITED app/src/main/assets/logic/player.js: Fixed song detail refresh bug
[2026-02-28] EDITED documentation/future_plans.md: Expanded roadmap with detailed descriptions
[2026-02-28] ADDED app/src/main/assets/gui/pages/song.html: Created Song Detail page template
[2026-02-28] ADDED app/src/main/assets/gui/svg/info.svg: Added info icon for player More Info button
[2026-02-28] EDITED documentation/CHANGELOG.md: Added changelog entries for UI tweaks and song detail page implementation

[2026-03-02] EDITED app/src/main/assets/logic/config.js: Added AI API key placeholders and endpoints for Smart Mixes
[2026-03-02] EDITED app/src/main/assets/logic/api.js: Added generateAIPlaylist and getSmartMixes methods with multi-AI fallback
[2026-03-02] ADDED app/src/main/assets/logic/smart-mixes.js: New module for AI-powered playlist orchestration
[2026-03-02] EDITED app/src/main/assets/gui/pages/home.html: Added Smart Mixes section above genre rows
[2026-03-02] EDITED app/src/main/assets/gui/index.css: Added premium glassmorphism styles and animations for Smart Mix cards
[2026-03-02] EDITED app/src/main/assets/logic/pages.js: Integrated SmartMixes.refresh() into home page and Library playlist view
[2026-03-02] EDITED app/src/main/assets/gui/lang/*.json: Added localization keys for Smart Mixes
[2026-03-02] EDITED app/src/main/assets/logic/api.js: Removed search-based 90's genre section from Home page; enforced chart-only results
[2026-03-02] EDITED app/src/main/assets/gui/pages/home.html: Updated genre rows to match official charts (added Jazz, Dance; removed 90's)
[2026-03-02] EDITED app/src/main/assets/logic/player.js: Added playTracks method for AI playlist support
[2026-03-02] EDITED app/src/main/assets/logic/smart-mixes.js: Added debug logs and forced section visibility
[2026-03-02] EDITED app/src/main/assets/gui/index.html: Ensured PageSystem is imported and exposed globally
[2026-03-03] DELETED app/src/main/assets/logic/smart-mixes.js: Removed AI-powered Smart Mixes feature completely
[2026-03-03] EDITED app/src/main/assets/gui/pages/home.html: Removed Smart Mixes section
[2026-03-03] EDITED app/src/main/assets/logic/pages.js: Removed SmartMixes integration and Library playlist rendering
[2026-03-03] EDITED app/src/main/assets/logic/config.js: Removed AI API keys and endpoints
[2026-03-03] EDITED app/src/main/assets/logic/player.js: Removed playTracks method
[2026-03-03] EDITED app/src/main/assets/gui/lang/*.json: Removed smart_mixes localization key
