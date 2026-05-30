const { contextBridge, ipcRenderer } = require('electron');

/**
 * ElectronAPI context bridge interface.
 * Exposes secure communication APIs to the frontend browser window,
 * shielding the renderer from direct Node.js system access.
 */
contextBridge.exposeInMainWorld('ElectronAPI', {
    /**
     * Fetches the list of offline tracks saved locally.
     *
     * @returns {Promise<Array<Object>>} List of saved track objects containing metadata and local URLs.
     */
    getSavedTracks: () => ipcRenderer.invoke('get-saved-tracks'),

    /**
     * Resolves a YouTube stream URL natively using yt-dlp.
     *
     * @param {string} videoId The YouTube video identifier.
     * @returns {Promise<Object>} Direct streaming URL payload.
     */
    playTrack: (videoId) => ipcRenderer.invoke('play-track', videoId),

    /**
     * Downloads and caches a YouTube audio track locally.
     *
     * @param {string} videoId The YouTube video identifier.
     * @param {string} artist The name of the artist.
     * @param {string} title The title of the track.
     * @returns {Promise<Object>} Save status and final saved-media URL.
     */
    saveTrack: (videoId, artist, title) => ipcRenderer.invoke('save-track', videoId, artist, title),

    /**
     * Deletes a saved track from local disk storage.
     *
     * @param {string} filename The name of the file to delete.
     * @returns {Promise<Object>} Deletion operation status.
     */
    deleteTrack: (filename) => ipcRenderer.invoke('delete-track', filename),

    /**
     * Fetches the latest GitHub release metadata from the Electron main process.
     *
     * @returns {Promise<Object>} Latest release status payload.
     */
    checkPcUpdate: () => ipcRenderer.invoke('check-pc-update'),

    /**
     * Downloads a portable PC update executable.
     *
     * @param {string} downloadUrl The selected GitHub asset or raw-main URL.
     * @param {string} version The release version being downloaded.
     * @returns {Promise<Object>} Download status and saved executable path.
     */
    downloadPcUpdate: (downloadUrl, version) => ipcRenderer.invoke('download-pc-update', downloadUrl, version),

    /**
     * Launches a downloaded portable PC update executable and quits this app.
     *
     * @param {string} filePath Downloaded update executable path.
     * @returns {Promise<Object>} Launch status payload.
     */
    installPcUpdate: (filePath) => ipcRenderer.invoke('install-pc-update', filePath),

    /**
     * Subscribes to PC update download progress events.
     *
     * @param {Function} callback Receives an integer percentage.
     * @returns {Function} Unsubscribe function.
     */
    onPcUpdateProgress: (callback) => {
        const listener = (event, progress) => callback(progress);
        ipcRenderer.on('pc-update-progress', listener);
        return () => ipcRenderer.removeListener('pc-update-progress', listener);
    },

    /**
     * Opens the native OS directory picker dialog window.
     * This allows the user to select an output folder for their files.
     *
     * @returns {Promise<string|null>} The chosen directory path, or null if cancelled.
     */
    selectDirectory: () => ipcRenderer.invoke('select-directory'),

    /**
     * Retrieves the default OS downloads directory path.
     * This provides a sensible default fallback target for local file downloads.
     *
     * @returns {Promise<string>} The path to the Downloads directory.
     */
    getDefaultDir: () => ipcRenderer.invoke('get-default-dir'),

    /**
     * Submits download parameters to initiate a queue run in the Electron main process.
     * This initiates downloading and metadata processing using the selected options.
     *
     * @param {Object} options - Parameter options.
     */
    startDownload: (options) => ipcRenderer.send('start-download', options),

    /**
     * Aborts the active downloader subprocess.
     * This terminates any ongoing download and transcoding operations immediately.
     */
    cancelDownload: () => ipcRenderer.send('cancel-download'),

    /**
     * Queries playlist/video metadata via Electron IPC.
     * This returns lists of tracks found at the target YouTube URL.
     *
     * @param {string} url - YouTube URL.
     * @returns {Promise<Object>} List of video metadata details or error messages.
     */
    fetchMetadata: (url) => ipcRenderer.invoke('fetch-metadata', url),

    /**
     * Registers a callback listener to print backend log strings.
     * This updates the download console in the UI with status log messages.
     *
     * @param {Function} callback - Receives standard console log outputs.
     * @returns {Function} Unsubscribe function.
     */
    onDownloaderLog: (callback) => {
        const listener = (event, msg) => callback(msg);
        ipcRenderer.on('downloader-log', listener);
        return () => ipcRenderer.removeListener('downloader-log', listener);
    },

    /**
     * Registers a callback listener for progress percentage changes.
     * This updates the visual progress bar on the downloader page.
     *
     * @param {Function} callback - Receives progress values from 0 to 100.
     * @returns {Function} Unsubscribe function.
     */
    onDownloaderProgress: (callback) => {
        const listener = (event, percent) => callback(percent);
        ipcRenderer.on('downloader-progress', listener);
        return () => ipcRenderer.removeListener('downloader-progress', listener);
    },

    /**
     * Registers a callback listener for download track title shifts.
     * This updates the active track status text on the UI screen.
     *
     * @param {Function} callback - Receives dict with status and track details.
     * @returns {Function} Unsubscribe function.
     */
    onDownloaderStatus: (callback) => {
        const listener = (event, data) => callback(data);
        ipcRenderer.on('downloader-status', listener);
        return () => ipcRenderer.removeListener('downloader-status', listener);
    },

    /**
     * Registers a callback listener for download task terminations.
     * This updates the UI to reflect a completed or failed download process.
     *
     * @param {Function} callback - Receives dict with success flag and error details.
     * @returns {Function} Unsubscribe function.
     */
    onDownloaderComplete: (callback) => {
        const listener = (event, data) => callback(data);
        ipcRenderer.on('downloader-complete', listener);
        return () => ipcRenderer.removeListener('downloader-complete', listener);
    }
});
