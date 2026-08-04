import { AbstractStorageEngine } from './AbstractStorageEngine.js';

/**
 * ElectronStorageEngine handles track saving via Electron IPC handlers and saved-media:// protocol.
 */
export class ElectronStorageEngine extends AbstractStorageEngine {
    /**
     * Triggers Electron IPC download/save handler.
     * @param {string} videoId
     * @param {string} artist
     * @param {string} title
     * @param {string} audioUrl
     * @returns {Promise<Object>}
     */
    async saveTrack(videoId, artist, title, audioUrl) {
        try {
            if (typeof window !== 'undefined' && window.electronAPI && window.electronAPI.saveTrack) {
                return await window.electronAPI.saveTrack({ videoId, artist, title, audioUrl });
            }
            return { status: 'error', message: 'Electron IPC unavailable' };
        } catch (error) {
            console.error('[ElectronStorage] Failed to save track:', error);
            return { status: 'error', message: error.message };
        }
    }

    /**
     * Retrieves saved tracks from Electron IPC handler mapped to saved-media:// protocol URLs.
     * @returns {Promise<Array<Object>>}
     */
    async getSavedTracks() {
        try {
            if (typeof window !== 'undefined' && window.electronAPI && window.electronAPI.getSavedTracks) {
                return await window.electronAPI.getSavedTracks();
            }
            return [];
        } catch (error) {
            console.error('[ElectronStorage] Failed to get saved tracks:', error);
            return [];
        }
    }

    /**
     * Deletes a saved track file via Electron IPC.
     * @param {string} filename
     * @returns {Promise<Object>}
     */
    async deleteTrack(filename) {
        try {
            if (typeof window !== 'undefined' && window.electronAPI && window.electronAPI.deleteTrack) {
                return await window.electronAPI.deleteTrack(filename);
            }
            return { status: 'error', message: 'Electron IPC unavailable' };
        } catch (error) {
            console.error('[ElectronStorage] Failed to delete track:', error);
            return { status: 'error', message: error.message };
        }
    }
}
