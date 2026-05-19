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

## 4. Multi-Platform Release Packaging
- **Release Target Isolation**: Every target release build must package ONLY the three compiled distribution binaries (`IVIDSMusic_Mobile.apk`, `IVIDSMusic_TV.apk`, and `IVIDSMusic_PC.exe`) at its root. The source codebase must remain entirely on the `main` branch.
- **Orphan Release Execution**: Release updates must be coordinated by executing the `.agents/scripts/build-release.js` utility, which compiles the three targets and isolates them in an orphan tag. Do NOT run manual tag or checkout actions for releases unless instructed.

Always follow these rules for every task you perform.

P.S.: NEVER use translate X or Y unless specifically asked for, use preferbly a thick(er) border for buttons on hover/focus or backgroudn change instead!