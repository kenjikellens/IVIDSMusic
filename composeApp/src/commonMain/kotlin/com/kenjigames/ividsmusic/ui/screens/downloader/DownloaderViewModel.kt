package com.kenjigames.ividsmusic.ui.screens.downloader

import androidx.compose.runtime.Immutable
import com.kenjigames.ividsmusic.domain.model.AppResult
import com.kenjigames.ividsmusic.domain.model.AudioQuality
import com.kenjigames.ividsmusic.domain.model.DownloadItem
import com.kenjigames.ividsmusic.domain.model.Track
import com.kenjigames.ividsmusic.domain.port.LibraryRepository
import com.kenjigames.ividsmusic.domain.port.OfflineDownloadManager
import com.kenjigames.ividsmusic.domain.port.PreferencesRepository
import com.kenjigames.ividsmusic.platform.PlatformBridge
import com.kenjigames.ividsmusic.ui.components.ToastController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Immutable UI state for [DownloaderScreen] covering URL preview tracks, per-download bitrate
 * selection (4..320 kbps), active download tasks, offline files, and storage metrics.
 */
@Immutable
data class DownloaderUiState(
    val urlInput: String = "",
    val saveDirectoryPath: String = "",
    val selectedQuality: AudioQuality = AudioQuality.Q196,
    val previewTracks: List<Track> = emptyList(),
    val selectedPreviewIds: Set<String> = emptySet(),
    val isLoadingPreview: Boolean = false,
    val activeDownloads: List<DownloadItem> = emptyList(),
    val savedTracks: List<Track> = emptyList(),
    val storageUsedBytes: Long = 0L,
    val errorMessage: String? = null
)

/**
 * Coordinates direct URL previews, per-download [AudioQuality] selection (`4..320` kbps),
 * active downloads, Desktop folder selection, and offline `saved/` directory scanning.
 */
class DownloaderViewModel(
    private val downloadManager: OfflineDownloadManager,
    private val libraryRepository: LibraryRepository,
    private val preferencesRepository: PreferencesRepository,
    private val platformBridge: PlatformBridge,
    private val toastController: ToastController
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _uiState = MutableStateFlow(
        DownloaderUiState(saveDirectoryPath = downloadManager.getSavedDirectoryPath())
    )

    /** Observable downloader screen state rendered by [DownloaderScreen]. */
    val uiState: StateFlow<DownloaderUiState> = _uiState.asStateFlow()

    private var userOverrodeQuality = false

    init {
        observeDownloadsAndSavedTracks()
        refreshStorageMetrics()
    }

    /**
     * Updates the YouTube or playlist URL input field.
     */
    fun onUrlChanged(url: String) {
        _uiState.update { it.copy(urlInput = url, errorMessage = null) }
    }

    /**
     * Overrides the per-download [AudioQuality] preset (`4`, `48`, `96`, `128`, `196`, `256`, `320` kbps)
     * directly on the Downloader screen.
     */
    fun selectQuality(quality: AudioQuality) {
        userOverrodeQuality = true
        _uiState.update { it.copy(selectedQuality = quality) }
    }

    /**
     * Fetches preview metadata for the entered YouTube track or playlist URL via [OfflineDownloadManager].
     */
    fun loadPreview() {
        val url = _uiState.value.urlInput.trim()
        if (url.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Please enter a YouTube URL.") }
            return
        }
        scope.launch {
            _uiState.update { it.copy(isLoadingPreview = true, errorMessage = null) }
            when (val res = downloadManager.previewUrl(url)) {
                is AppResult.Success -> {
                    val ids = res.data.map { it.id }.toSet()
                    _uiState.update {
                        it.copy(
                            previewTracks = res.data,
                            selectedPreviewIds = ids,
                            isLoadingPreview = false,
                            errorMessage = null
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(isLoadingPreview = false, errorMessage = res.error.message)
                    }
                }
            }
        }
    }

    /**
     * Toggles selection of an individual track in the URL preview checklist.
     */
    fun togglePreviewSelection(trackId: String) {
        _uiState.update { state ->
            val updated = if (trackId in state.selectedPreviewIds) {
                state.selectedPreviewIds - trackId
            } else {
                state.selectedPreviewIds + trackId
            }
            state.copy(selectedPreviewIds = updated)
        }
    }

    /**
     * Selects or deselects every track currently shown in the preview checklist.
     */
    fun selectAllPreview(selectAll: Boolean) {
        _uiState.update { state ->
            val ids = if (selectAll) state.previewTracks.map { it.id }.toSet() else emptySet()
            state.copy(selectedPreviewIds = ids)
        }
    }

    /**
     * Enqueues either the selected preview tracks or the raw URL input at the chosen [AudioQuality].
     */
    fun startDownload() {
        val state = _uiState.value
        val quality = state.selectedQuality
        val url = state.urlInput.trim()

        scope.launch {
            val selectedTracks = state.previewTracks.filter { it.id in state.selectedPreviewIds }
            if (selectedTracks.isNotEmpty()) {
                selectedTracks.forEach { track ->
                    downloadManager.enqueue(track, quality)
                }
                toastController.show("Queued ${selectedTracks.size} tracks (${quality.label})")
                return@launch
            }
            if (url.isEmpty()) {
                _uiState.update { it.copy(errorMessage = "Please enter a YouTube URL.") }
                return@launch
            }
            when (val res = downloadManager.enqueueUrl(url, quality)) {
                is AppResult.Success -> toastController.show("Download queued (${quality.label})")
                is AppResult.Failure -> {
                    _uiState.update { it.copy(errorMessage = res.error.message) }
                    toastController.show(res.error.message, isError = true)
                }
            }
        }
    }

    /**
     * Cancels an active or queued download job by [downloadId].
     */
    fun cancelDownload(downloadId: String) {
        scope.launch {
            downloadManager.cancel(downloadId)
        }
    }

    /**
     * Opens the platform folder chooser (Desktop) and updates [OfflineDownloadManager]'s output path.
     */
    fun browseOutputFolder() {
        platformBridge.pickDirectory(_uiState.value.saveDirectoryPath) { picked ->
            if (!picked.isNullOrBlank()) {
                scope.launch {
                    when (val res = downloadManager.setCustomSaveDirectory(picked)) {
                        is AppResult.Success -> {
                            toastController.show("Output folder set to $picked")
                            refreshStorageMetrics()
                        }
                        is AppResult.Failure -> toastController.show(res.error.message, isError = true)
                    }
                }
            }
        }
    }

    /**
     * Rescans the `saved/` directory for existing offline `.mp3`/`.m4a` tracks and updates storage metrics.
     */
    fun rescanSavedDirectory() {
        scope.launch {
            when (val res = downloadManager.scanSavedDirectory()) {
                is AppResult.Success -> toastController.show("Scanned ${res.data.size} offline tracks")
                is AppResult.Failure -> toastController.show(res.error.message, isError = true)
            }
            refreshStorageMetrics()
        }
    }

    /**
     * Deletes an offline track file on disk and clears its downloaded state in SQLite.
     */
    fun deleteOfflineTrack(trackId: String) {
        scope.launch {
            when (val res = downloadManager.deleteOfflineFile(trackId)) {
                is AppResult.Success -> {
                    toastController.show("Deleted offline file")
                    refreshStorageMetrics()
                }
                is AppResult.Failure -> toastController.show(res.error.message, isError = true)
            }
        }
    }

    /**
     * Cancels active coroutines when disposed.
     */
    fun dispose() {
        scope.cancel()
    }

    /**
     * Recomputes occupied disk space in bytes inside the active saved folder.
     */
    private fun refreshStorageMetrics() {
        scope.launch {
            val bytes = downloadManager.getSavedStorageBytes()
            _uiState.update { it.copy(storageUsedBytes = bytes) }
        }
    }

    /**
     * Observes download tasks, output path, saved offline tracks, and default bitrate preference.
     */
    private fun observeDownloadsAndSavedTracks() {
        scope.launch {
            combine(
                downloadManager.downloads,
                downloadManager.downloadDirectoryPath,
                libraryRepository.observeDownloadedTracks(),
                preferencesRepository.preferences
            ) { downloads, dirPath, savedTracks, prefs ->
                arrayOf<Any>(downloads, dirPath, savedTracks, prefs.downloadQuality)
            }.collectLatest { tuple ->
                @Suppress("UNCHECKED_CAST")
                val downloads = tuple[0] as List<DownloadItem>
                val dirPath = tuple[1] as String
                val savedTracks = tuple[2] as List<Track>
                val defaultQuality = tuple[3] as AudioQuality
                _uiState.update { current ->
                    current.copy(
                        activeDownloads = downloads,
                        saveDirectoryPath = dirPath,
                        savedTracks = savedTracks,
                        selectedQuality = if (userOverrodeQuality) current.selectedQuality else defaultQuality
                    )
                }
                refreshStorageMetrics()
            }
        }
    }
}
