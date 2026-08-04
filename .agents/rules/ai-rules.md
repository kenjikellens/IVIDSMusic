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
- **Android Phone (Android WebView)**: `IVIDSMusic_Mobile.apk` compiled under the `mobile` Gradle flavor (`app` module). WebView-based Android Mobile application loading embedded `gui` and `logic` web assets from `app/src/main/assets` into an Android WebView, utilizing Kotlin `shouldInterceptRequest` hooks for playing/saving.
- **Android TV (Android WebView)**: `IVIDSMusic_TV.apk` compiled under the `tv` Gradle flavor (`app` module). WebView-based Android TV application loading embedded `gui` and `logic` web assets into an Android WebView, utilizing Kotlin `shouldInterceptRequest` hooks for playing/saving.
- **PC Desktop (Electron WebView)**: `IVIDSMusic_PC.exe` compiled from `app/`. WebView-based desktop application running embedded `gui` and `logic` web assets inside Electron. Uses `app/main.js` and `app/preload.js` IPC handlers to call `yt-dlp` for streaming and saving, loading saved media via custom `saved-media://` protocol.
- **Static Web / GitHub Pages**: Served as static assets. Resolves playback URLs via public Invidious API instances, and caches downloaded audio streams as binary Blobs in browser IndexedDB.
- **Unified Logic Integration**: Frontend files (e.g., `api.js`, `player.js`) must query `Config.isElectron`, `Config.isNative` (Android WebView), and `Config.isWeb` to dynamically call the correct platform API wrapper.


## 3. Git & GitHub Protocol
- **No Autonomous Pushing**: You must NEVER push changes to the GitHub repository on your own initiative.
- **Explicit Permission Required**: You must always ask for explicit permission from the user before using `git push`. Keep your changes in the local workspace until the user clearly asks to push, upload, sync, publish, or otherwise put the local branch/ref on GitHub.
- **Obligatory Release Metadata**: When creating, tagging, or pushing a new release, you are strictly OBLIGATED to formulate a professional, high-quality release title (e.g., `Release v0.1.6 (Beta)`) and a detailed, feature-rich release description highlighting all visual, architectural, compilation, and core improvements included in this build.

If the user clearly asks to push/upload/sync the current branch, you may push the current branch and its local commits. If the user limits the push to a specific file, commit, branch, tag, or ref, push only that requested scope.

## 4. Multi-Platform Release Packaging
- **Release Target Isolation**: Every target release build must package ONLY the three compiled distribution binaries at its root:
  - `IVIDSMusic_Mobile.apk` (Native Kotlin/Java Android Mobile app)
  - `IVIDSMusic_TV.apk` (Android TV WebView app)
  - `IVIDSMusic_PC.exe` (PC Desktop Electron WebView app)
  The source codebase must remain entirely on the `main` branch.
- **Orphan Release Execution**: Release updates must be coordinated by executing the `.agents/scripts/build-release.js` utility, which compiles the three targets and isolates them in an orphan tag. Do NOT run manual tag or checkout actions for releases unless instructed.

Always follow these rules for every task you perform.

P.S.: NEVER use translate X or Y unless specifically asked for, use preferbly a thick(er) border for buttons on hover/focus or backgroudn change instead!
