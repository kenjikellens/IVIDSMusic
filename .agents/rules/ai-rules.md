---
trigger: always_on
---

# AI Rules for IVIDS Music

When working on this project, you must strictly follow these rules to maintain the integrity and quality of the application:

## 1. Responsive Design & Breakpoints
You must maintain, respect, and design for the granular layout hierarchy. This system ensures the UI scales correctly from small phones to large monitors.
- **The 5 Width Breakpoints**: You must design for 1200px, 1024px, 768px, 700px, and 600px. The `--ui-base-scale` variable changes at each of these points.
- **Portrait Mode**: You must ensure the layout shifts to a vertical orientation (sidebar disappears, bottom navigation appears) when the device is in portrait mode. This is critical for the mobile experience and is independent of the width scale.

## 2. Platform-Specific Implementations & Compatibility
You must ensure the codebase respects and handles the four distinct runtimes:
- **PC Desktop (Electron)**: Run natively inside Electron. Uses `main.js` and `preload.js` IPC handlers to call `yt-dlp` for streaming and saving, and loads saved media via the custom `saved-media://` protocol.
- **Android TV (WebView)**: Compiled under the `tv` Gradle flavor. Loads the embedded `gui` and `logic` assets into an Android WebView, utilizing Kotlin `shouldInterceptRequest` hooks for playing/saving.
- **Android Phone (Native Java)**: Fully native Java application under the `mobile` Gradle flavor. Uses Android XML layouts, Room DB for library persistence, Jetpack Media3 ExoPlayer in a background service, and Retrofit/OkHttp for networking.
- **Static Web / GitHub Pages**: Served as static assets. Resolves playback URLs via public Invidious API instances, and caches downloaded audio streams as binary Blobs in browser IndexedDB.
- **Unified Logic Integration**: Frontend files (e.g., `api.js`, `player.js`) must query `Config.isElectron`, `Config.isNative` (Android WebView), and `Config.isWeb` to dynamically call the correct platform API wrapper.


## 3. Git & GitHub Protocol
- **No Autonomous Pushing**: You must NEVER push changes to the GitHub repository on your own initiative.
- **Explicit Permission Required**: You must always ask for explicit permission from the user before using `git push`. Keep your changes in the local workspace until you are instructed to push them to the cloud.
- **Obligatory Release Metadata**: When creating, tagging, or pushing a new release, you are strictly OBLIGATED to formulate a professional, high-quality release title (e.g., `Release v0.1.6 (Beta)`) and a detailed, feature-rich release description highlighting all visual, architectural, compilation, and core improvements included in this build.

if the user says **"push to main"**, you may directly push ALL changes to main, if the user says **"push [file] to main"**, you only push this file.

## 4. Change Logging
For every file you **edit**, **add**, or **delete**, you must append a new line to the bottom of the project's `CHANGELOG.md` file (located at the "./documentation/" of the project). Each log entry must follow this format:

[YYYY-MM-DD] [ACTION] <filename>: <brief description of what was changed/added/deleted>

- **ACTION** must be one of: `EDITED`, `ADDED`, or `DELETED`.
- The description must be concise but specific enough to understand what changed and why.
- **Chronological Order**: Always append new log entries to the bottom of the file (newest changes at the bottom).
- **Reset on Release**: The changelog only contains changes since the last release. As soon as a new release is published, tagged, or pushed, the `CHANGELOG.md` file must be cleared (started fresh), and this cleared state must be pushed to `main` immediately to prepare for the next release cycle.
- **Excluded Files**: Do NOT log modifications to agent-internal files (e.g., `.agents/` folder, workflows, system rules) or documentation files (e.g., `documentation/` folder, `CHANGELOG.md` itself) in `CHANGELOG.md`. The changelog tracks codebase/application changes only.
- This log entry must be written **every time** a codebase file operation occurs, without exception.

Always follow these rules for every task you perform.

P.S.: NEVER use translate X or Y unless specifically asked for, use preferbly a thick(er) border for buttons on hover/focus or backgroudn change instead!