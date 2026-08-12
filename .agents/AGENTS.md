# Master Project Rules for IVIDS Music

Every AI agent and developer working on IVIDS Music MUST strictly follow these unified rules without exception.

---

## 1. Platform Architecture & Responsive Design

### Responsive Design & Width Breakpoints
- **The 5 Width Breakpoints**: Design strictly within the 5 responsive width breakpoints: `1200px`, `1024px`, `768px`, `700px`, and `600px`. The `--ui-base-scale` variable scales automatically across these points.
- **Portrait Mode Orientation Shift**: When the device is in portrait mode (or width <= 768px), the layout MUST shift to vertical orientation (sidebar hides, bottom navigation bar appears).
- **Dynamic Sizing**: Prefer percentage `%` or viewport units (`vw`, `vh`) over fixed `px` values. Use `calc(N * var(--ui-scale))` for scaling UI dimensions and font sizes.

### 4 Multi-Platform Runtimes
- **Android Mobile WebView (`IVIDSMusic_Mobile.apk`)**: Mobile WebView loading embedded assets from `app/src/main/assets` with Kotlin `shouldInterceptRequest` hooks.
- **Android TV WebView (`IVIDSMusic_TV.apk`)**: Android TV WebView loading embedded assets with TV spatial navigation.
- **PC Desktop Electron (`IVIDSMusic_PC.exe`)**: Electron WebView running embedded assets, IPC handlers via `preload.js` calling `yt-dlp` and custom `saved-media://` protocol.
- **Static Web / GitHub Pages**: Static assets, resolving URLs via public Invidious API instances, caching audio streams in browser IndexedDB.
- **Unified Logic Wrappers**: Frontend logic (`api.js`, `player.js`) must query `Config.isElectron`, `Config.isNative`, and `Config.isWeb` dynamically.

---

## 2. CSS Architecture, Styling & Animation Rules

- **Modular CSS Architecture**: Styles MUST be organized into modular stylesheets under `gui/css/` (`variables.css`, `base.css`, `components.css`, `pages.css`, `responsive.css`). Master `index.css` or `index.html` links these modules. NEVER create 5,000+ line monolithic CSS files.
- **Strict `!important` Ban**: Do NOT use `!important` unless overriding third-party injected styles or state utility classes. Use CSS selector specificity.
- **Mandatory Cleanup on Refactor**: Whenever a UI component, HTML structure, or page layout is refactored, renamed, or deleted, search for and delete all corresponding obsolete CSS class selectors in the exact same turn.
- **Accent Color Rule**: Always use CSS accent color tokens (`var(--primary-color)`, `var(--accent-color)`). If the user mentions "colored" or "green" elements, ask if they mean the active accent color.
- **No Translate Animations**: NEVER use `translateX` or `translateY` for hover/focus effects unless explicitly requested.
- **Hover/Focus Animation Standard**: Standard hover/focus effects are a white thicker border, background color change, or glassmorphic adjustment. Interactive elements MUST NOT scale or move on hover.
- **CSS Utility Classes & DRY Styling**:
  - Define single utility classes (e.g. `.container-hover-effect`) in CSS rather than chaining long element selectors.
  - Overlay utilities must use `border-radius: inherit` and relative positioning.
  - Always compose with pre-existing base layout classes (`class="page-container foryou-container"`).

---

## 3. JavaScript OOP, Performance & Domain Modeling

### Object-Oriented Architecture (OOP)
- **ES6 Classes & Encapsulation**: Use ES6 `class` syntax for controllers, managers, and services. State must be encapsulated in private/instance properties.
- **Strict Base Class Inheritance**: Page controllers MUST extend `BasePageController`. Overlays and modals MUST extend standard modal base abstractions.
- **Single Responsibility Principle**: Decompose functions >50 lines into small, focused helper methods.

### Event Delegation & Memory Safety
- **Container Event Delegation**: NEVER bind individual event listeners in loops or arrays (`items.forEach(item => item.onclick = ...)`). ALWAYS attach a single listener to parent containers using `e.target.closest('.item-selector')`.
- **AbortController Lifecycle**: Controllers must call `this.resetAbortController()` on render/destroy to prevent memory leaks across page transitions.

### Speed & DOM Optimization
- **Batch DOM Operations**: Do not interleave DOM reads (`getBoundingClientRect`, `offsetHeight`) with DOM writes in loops. Batch all reads first, then execute single-pass DOM writes.
- **Template Fragment Rendering**: Construct UI elements using `DocumentFragment` or HTML template literal strings rendered in a single `innerHTML` assignment.
- **Zero Code Duplication (DRY)**: Abstract repeated functionality into shared utility classes (`Config`, `LanguageManager`, `SettingsManager`).

### Domain Modeling
- **First-Class Domain Entities**: Core concepts (`Song`, `Album`, `Artist`) must be first-class domain models with typed object relationships.
- **DTO Mappers**: Use dedicated Data Transfer Object (DTO) mappers for converting raw API responses into domain entities.

---

## 4. Translation & Localization Rules

- **No Hardcoded UI Strings**: Never hardcode user-facing text in HTML or JS templates.
- **16 Supported Languages**: All user-facing strings must be added to all 16 JSON translation files in `app/src/main/assets/gui/lang/` (`en.json`, `nl.json`, `de.json`, `fr.json`, `es.json`, `it.json`, `pt.json`, `ro.json`, `ru.json`, `zh.json`, `hi.json`, `ar.json`, `ja.json`, `ko.json`, `pl.json`, `tr.json`).
- **Batch Translation Script Mandate**: ALWAYS write and run a script (Node.js or Python) to update all 16 translation JSON files simultaneously in a single automated step.

---

## 5. UI Icon Rules

- **NO EMOJIS IN UI**: Never use raw Unicode emojis (🎧, 📂, ⚙️, 🎬, 🎵) in HTML templates, JavaScript strings, or CSS content.
- **SVG ICONS ONLY**: All UI icons must be SVG files loaded from `gui/svg/` (or inline SVG matching component standards). Every SVG must be its own `.svg` file.

---

## 6. Git & GitHub Protocol

- **No Autonomous Commits or Pushes**: NEVER run `git commit` or `git push` on your own initiative. All edits remain in the working tree for user review.
- **Explicit Permission Required**: Always request explicit permission before using `git push`. Contextual commands like "push changes" authorize pushing the active branch for that turn only.
- **Obligatory Release Metadata**: Releases must feature a professional release title (e.g. `Release v0.1.6 (Beta)`) and a detailed release description highlighting visual, architectural, and compilation updates.
- **Release Authorization**: Direct request to create/publish a release authorizes pushing the release tag and publishing the GitHub release within that workflow.

---

## 7. Multi-Platform Release Packaging

- **Release Target Isolation**: Every target release build packages ONLY the three compiled binaries at root:
  - `IVIDSMusic_Mobile.apk`
  - `IVIDSMusic_TV.apk`
  - `IVIDSMusic_PC.exe`
- **Orphan Release Execution**: Coordinated via `.agents/scripts/build-release.js` utility isolating builds in an orphan tag.

---

## 8. Implementation Plans, Mockups & Documentation

- **Implementation Plans are Artifacts ONLY**: Implementation plans MUST strictly be generated as an IDE artifact (`implementation_plan.md` in conversation artifact directory).
- **No Implementation Plan Files in Workspace**: NEVER create `implementation_plan.md` inside `mockup/` or any workspace folder.
- **Mockup Rules**: Standalone UI prototypes go into a dedicated `mockup/` folder with offline HTML, JS, CSS, and SVG assets.
- **Documentation Reference**: Consult `app/../documentation/file_list.md` for information about key project files.
