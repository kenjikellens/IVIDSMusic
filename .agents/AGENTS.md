# Master Project Rules for IVIDS Music

Every AI agent and developer working on IVIDS Music must follow these rules strictly without exception.

## 1. Platform Architecture & Build Parity

Build across the shared Kotlin Multiplatform modules (`:composeApp`, `:core:domain`, `:core:data`, `:core:player`) targeting three runtimes: Windows Desktop (`IVIDSMusic_PC.exe`), Android Mobile (`IVIDSMusic_Mobile.apk`), and Android TV (`IVIDSMusic_TV.apk`).

Switch the adaptive UI cleanly via `ShellMode`: render Mobile chrome (`< 600.dp`) with top header, bottom bar, and swipeable `MiniPlayerBar`, and Desktop/TV chrome (`>= 600.dp`) with `SidebarNavigationRail` (collapsible below `880.dp`), `DesktopTopBar`, `DesktopPlayerDeck`, and `QueueSideSheet`. Keep Desktop reading width constrained inside ergonomic containers (`880.dp` to `1360.dp`).

Run `:composeApp:desktopTest`, `:composeApp:createDistributable`, and `:composeApp:assembleMobileDebug` after UI or core changes so Windows Desktop and Android builds stay verified in sync.

## 2. Clean Kotlin Architecture & OOP Composition

Maintain strict module boundaries: `:core:domain` contains pure Kotlin models, use cases, and repository/engine ports with zero UI or framework imports; `:core:data` implements SQLDelight, DataStore, Ktor, and stream extraction; `:core:player` encapsulates playback behind `AudioEngine` and `EngineStateMachine`; `:composeApp` holds Compose UI and ViewModels.

Follow OOP Composition over Duplication in Compose UI. Keep semantic color tokens (`IvidsHoverStyle`, `resolveHoverColor`, `resolveRippleColor`) decoupled from behavior modifiers (`ividsHoverZoom`, `ividsTouchFeedback`, pointer `ividsPressRipple`), combine them once inside shared controls (`IvidsTonalPill`, `IvidsPrimaryButton`, `IvidsPlayCircleButton`, `IvidsIconButton`, `IvidsVinylLoader`, `MusicCard`, `TrackRowItem`), and reuse those components across all screens and player decks.

Keep functions and files focused with single responsibility. Split oversized UI files when needed and remove unused parameters, imports, and dead composables in the same change.

## 3. Compose Multiplatform UI, Color & Interaction Standards

Use pure `R = G = B` black and neutral charcoal surfaces (`#000000`, `#090909`, `#141414`, `#1C1C1C`) with zero blue-gray undertone. Set Desktop `window.background` and `window.contentPane.background` to `#090909`.

Use signature cyan (`PrimaryCyan` `#4DD0E1`) strictly on active or main controls such as the `"IVIDS"` wordmark, opened navigation destination, active Shuffle and Repeat toggles, open Queue toggle, unmuted Volume icon, and vinyl loader center label.

Never draw borders (`BorderStroke` or `.border()`) on cards, pills, switches, segmented chips, or dialogs, except TV `tvFocusable` focus rings. Use dark overlays (`IvidsHoverStyle.ON_LIGHT`) for hover and ripple on white primary buttons, and subtle transparent red (`IvidsHoverStyle.DESTRUCTIVE`) on destructive actions.

Render Settings flat on `#090909` without nested cards or a `Layout Mode` row. Push multi-option pickers from the right via `AppRoute.SettingsDetail` instead of modal popups, keep click feedback on full-width rows limited to hover zoom and ripple without shrinking the row, and keep the Desktop search bar centered with Back pinned far left.

## 4. Localization (16 Languages)

Never hardcode user-facing UI strings in Kotlin composables or ViewModels. Resolve text via `LocalStrings` and add every new key simultaneously across all 16 JSON files in `composeApp/src/commonMain/composeResources/files/lang/` (`en.json`, `nl.json`, `de.json`, `fr.json`, `es.json`, `it.json`, `pt.json`, `ro.json`, `ru.json`, `zh.json`, `hi.json`, `ar.json`, `ja.json`, `ko.json`, `pl.json`, `tr.json`) using a single automated script.

## 5. Icons & Media Rules

Never use raw Unicode emojis inside UI components or strings. Use Material Rounded vector icons (`Icons.Rounded.*`, `Icons.AutoMirrored.Rounded.*`), `composeResources/drawable/` assets, or custom Compose `Canvas` graphics (`IvidsVinylLoader`).

## 6. Git, GitHub & Release Packaging

Never run `git commit` or `git push` autonomously; leave all edits in the working tree unless explicitly instructed to commit, push, or publish a release.

Release workflows (`build.bat`, `.agents/scripts/build-release.js`, `.agents/scripts/publish-release.js`) package only `IVIDSMusic_Mobile.apk`, `IVIDSMusic_TV.apk`, and `IVIDSMusic_PC.exe` at root with descriptive release titles and changelogs.

## 7. Implementation Plans & Documentation

Create implementation plans exclusively as IDE artifacts (`implementation_plan.md` in the conversation artifact directory), never inside workspace folders. Consult [docs/README.md](../docs/README.md) and [docs/file_list.md](../docs/file_list.md) for current project structure and technical specifications.
