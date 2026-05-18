---
description: Version and release update
---

# Version and Release Update Workflow

This document defines the strict, standardized protocol for launching new release builds, compiling the Android installation package, and configuring distribution binaries for IVIDS Music.

---

## 🛠️ Step-by-Step Release Protocol

### 1. Inquire the Target Version (Mandatory Step)
- **ACTION**: The AI Agent MUST ask the developer/user explicitly what version name the new release should be (e.g., `v1.1.0`). Do NOT assume, auto-select, or hardcode the version bump without developer confirmation.
- **Title and Description Generation**: Upon receiving the version name, the Agent must formulate a high-quality, professional release title (e.g., `Release v1.1.0 (Prerelease)`) and a detailed, feature-rich release description highlighting all visual, audio, performance, and core logic improvements.

### 2. Update Android Version Config
- **ACTION**: Modify the Android build configuration in [build.gradle.kts](file:///c:/Users/kenji/AndroidStudioProjects/IVIDSMusic/app/build.gradle.kts):
  - Bump `versionName` to match the target version precisely (e.g., `versionName = "1.1"` or `versionName = "v1.1.0"` depending on developer preference).
  - Increment the integer `versionCode` by 1.
- **CHANGELOG HOOK**: Immediately document this gradle modification in [CHANGELOG.md](file:///c:/Users/kenji/AndroidStudioProjects/IVIDSMusic/documentation/CHANGELOG.md) in the exact format:
  `[YYYY-MM-DD] EDITED app/build.gradle.kts: Bumped versionName to vX.Y.Z and incremented versionCode to N.`

### 3. Compile the Application Package (APK)
- **ACTION**: Execute the Gradle wrapper command in PowerShell to compile the application and produce the APK:
  ```powershell
  .\gradlew assembleDebug
  ```
  *(Or execute `.\gradlew assembleRelease` if building a signed production-ready package).*

### 4. Relocate and Rename the Package to the Workspace Root
- **ACTION**: Copy the compiled APK to the root workspace directory and rename it to `IVIDSMusic.apk`.
  - **Source Path**: `app/build/outputs/apk/debug/app-debug.apk` (or matching release path).
  - **Destination Path**: [IVIDSMusic.apk](file:///c:/Users/kenji/AndroidStudioProjects/IVIDSMusic/IVIDSMusic.apk) (root).
  - **PowerShell Copy Command**:
    ```powershell
    Copy-Item -Path "app/build/outputs/apk/debug/app-debug.apk" -Destination "IVIDSMusic.apk" -Force
    ```
- **CHANGELOG HOOK**: Immediately document the copying and creation of the root APK in [CHANGELOG.md](file:///c:/Users/kenji/AndroidStudioProjects/IVIDSMusic/documentation/CHANGELOG.md) in the exact format:
  `[YYYY-MM-DD] ADDED IVIDSMusic.apk: Placed compiled prerelease APK at workspace root directory for distribution.`

### 5. Tag and Push the Release
- **ACTION**: Tag the release commit and push it to the remote GitHub repository:
  ```powershell
  git tag -a vX.Y.Z -m "Release vX.Y.Z"
  git push origin vX.Y.Z
  ```
- **GitHub Release Entry**: Draft a new release on GitHub using the pushed tag, applying the high-quality title and description formulated in Step 1.

### 6. Reset Changelog Post-Release
- **ACTION**: Immediately after the release version tag is pushed, clear the `CHANGELOG.md` file (making it completely blank or starting fresh), commit this change, and push it to `main` to prepare for the next release cycle:
  ```powershell
  Clear-Content -Path "documentation/CHANGELOG.md"
  git add documentation/CHANGELOG.md
  git commit -m "chore: clear changelog post-release vX.Y.Z"
  git push origin main
  ```

