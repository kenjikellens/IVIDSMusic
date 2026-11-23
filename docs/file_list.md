# IVIDS Music - Source File Inventory

Actual Kotlin Multiplatform / Compose Multiplatform project structure (`composeApp`, `core/domain`, `core/data`, `core/player`, and `docs`).

## Root & Build Scripts
- `README.md` - Main project overview & build instructions
- `FEATURES_ROADMAP.md` - P0-P3 feature roadmap
- `build.bat` / `run_pc.bat` - Release build & Desktop launcher scripts
- `settings.gradle.kts` / `build.gradle.kts` / `gradle/libs.versions.toml`

## `:composeApp` (`composeApp/src/`)

### `commonMain/composeResources/`
- `drawable/ic_ivids_brand.png` - App brand mark
- `files/lang/*.json` - 16 localization bundles (`ar`, `de`, `en`, `es`, `fr`, `hi`, `it`, `ja`, `ko`, `nl`, `pl`, `pt`, `ro`, `ru`, `tr`, `zh`)
- `font/*.ttf` - Bundled Inter & Plus Jakarta Sans fonts

### `commonMain/kotlin/com/kenjigames/ividsmusic/`
- `App.kt` - Root composable, navigation back-stack host, keyboard shortcuts, update modal, toast overlay
- `di/AppModule.kt` - Koin app & ViewModel bindings
- `platform/PlatformServices.kt` - Multiplatform platform bridge (`expect/actual`)
- `ui/navigation/AppRoute.kt` - `AppRoute`, `SettingsSection`, `AppNavigator`, `NavTransitionType`
- `ui/i18n/LocalizationManager.kt` - Instant runtime 16-language loader & `LocalStrings`
- `ui/input/TextInputShortcutGuard.kt` - Prevents global media shortcuts while typing
- `ui/player/PlayerViewModel.kt` & `NowPlayingSheet.kt` - Playback ViewModel & full-screen Now Playing sheet
- `ui/shell/`:
  - `AdaptiveAppShell.kt` - Root adaptive container (`MOBILE`, `DESKTOP`, `TV`), layout metrics & player hosts
  - `MobileShellChrome.kt` - `MobileTopHeader`, `MobileBottomBar`, swipeable `MiniPlayerBar`
  - `DesktopShellChrome.kt` - `SidebarNavigationRail`, `DesktopTopBar`
  - `DesktopPlayerChrome.kt` - `DesktopPlayerDeck`, `QueueSideSheet`
- `ui/components/`:
  - `IvidsInteraction.kt` - `IvidsHoverStyle`, `resolveHoverColor`, `resolveRippleColor`, `ividsPressRipple`, `ividsHoverZoom`, `ividsTouchFeedback`
  - `IvidsControls.kt` - `IvidsScrubber`, `IvidsSwitch`, `IvidsSegmentedBar`, `IvidsTonalPill`, `IvidsPrimaryButton`, `IvidsPlayCircleButton`, `IvidsIconButton`, `IvidsSelectableDialogItem`, `FramedScreenContainer`
  - `IvidsLoaders.kt` - `IvidsVinylLoader`, `IvidsVinylLoadingBox`
  - `IvidsCards.kt`, `TrackRowItem.kt`, `BitrateSelector.kt`, `BlurredArtworkBackdrop.kt`, `DialogsAndToasts.kt`, `TrackActionsModalHost.kt`
- `ui/theme/` - `IvidsColorTokens.kt`, `IvidsTheme.kt`, `IvidsTypography.kt`, `ColorExtractor.kt`, `TvFocusModifier.kt`
- `ui/screens/` - Screen & ViewModel pairs in `album/`, `artist/`, `downloader/`, `home/`, `library/`, `playlist/`, `profile/`, `recommended/`, `search/`, `settings/`, `song/`

### `androidMain/` & `desktopMain/`
- `androidMain/` - `MainActivity.kt`, `IvidsApplication.kt`, `AppModule.android.kt`, `PlatformServices.android.kt`, `BlurredArtworkBackdrop.android.kt`, `ColorExtractor.android.kt`
- `desktopMain/` - `Main.kt`, `AppModule.desktop.kt`, `PlatformServices.desktop.kt`, `BlurredArtworkBackdrop.desktop.kt`, `ColorExtractor.desktop.kt`, `resources/icon.ico|png`

### `:composeApp` Tests
- `commonTest/.../UiLogicTest.kt`
- `desktopTest/.../` - `DesktopIntegrationTest.kt`, `AppNavigatorTransitionTest.kt`, `PlayerViewModelP0Test.kt`, `MobileLayoutMetricsTest.kt`

## `:core:domain` (`core/domain/src/`)
- `commonMain/.../domain/model/` - `MusicModels.kt`, `PlaybackModels.kt`
- `commonMain/.../domain/port/Repositories.kt` - Repository, `AudioEngine`, `StreamResolver`, and `OfflineDownloadManager` interfaces
- `commonMain/.../domain/usecase/` - `PlaybackQueueController.kt`, `RecommendationEngine.kt`, `ResolveTrackStreamUseCase.kt`
- `commonTest/.../` - `AudioQualityTest.kt`, `PlaybackQueueControllerTest.kt`, `RecommendationEngineTest.kt`

## `:core:data` (`core/data/src/`)
- `commonMain/sqldelight/.../IvidsMusic.sq` - SQLite schema & queries
- `commonMain/.../data/di/DataModule.kt` - Koin data module
- `commonMain/.../data/local/` - `AppStorageDirectories.kt`, `DataStorePreferencesRepository.kt`, `SqlLibraryRepository.kt`, `SqlScoreRepository.kt`
- `commonMain/.../data/remote/` - `CompositeMusicDiscoveryRepository.kt`, `DeezerApiClient.kt`, `ItunesApiClient.kt`, `dto/`
- `commonMain/.../data/stream/StreamResolverComponents.kt` - `CompositeStreamResolver`, `InMemoryStreamTtlCache`, `InvidiousFallbackResolver`
- `jvmAndAndroidMain/.../data/` - `NativeDownloadManager.kt`, `FilePlaybackSessionRepository.kt`, `NewPipeStreamResolver.kt`, `NetworkSecurityGuard.kt`, `JvmLocalAudioLocator.kt`
- `jvmMain/.../data/` - `DesktopFfmpegTranscoder.kt`, `DesktopNativeToolsManager.kt`, `YtDlpDesktopResolver.kt`, `PlatformDataModule.jvm.kt`
- `androidMain/.../data/` - `AndroidNoOpTranscoder.kt`, `AndroidStreamFallback.kt`, `PlatformDataModule.android.kt`
- `jvmTest/.../` - Data module, download, SQLite, preferences, discovery, stream, and security unit tests

## `:core:player` (`core/player/src/`)
- `commonMain/.../player/` - `EngineStateMachine.kt`, `StreamSourceValidator.kt`, `di/PlayerModule.kt`
- `androidMain/.../player/android/` - `AndroidAudioEngine.kt`, `AndroidPlaybackService.kt`, `AudioCacheProvider.kt`, `PlaybackDataSourceFactory.kt`, `PlaybackErrorMapper.kt`, `StreamHeaderRegistry.kt`
- `jvmMain/.../player/desktop/` - `DesktopAudioEngine.kt`, `DesktopMediaBackend.kt`, `LocalHeaderProxy.kt`, `DesktopAudioPrefetcher.kt`, `DesktopAudioCacheTranscoder.kt`, `DesktopErrorMapper.kt`, `DesktopNetworkGuard.kt`, `JavaFxRuntime.kt`
- `jvmTest/.../` - `EngineStateMachineTest.kt`, `StreamSourceValidatorTest.kt`, `DesktopAudioCacheTranscoderTest.kt`, `DesktopAudioEngineTest.kt`, `LocalHeaderProxyTest.kt`, `PlayerModuleJvmTest.kt`
