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
    }
});
