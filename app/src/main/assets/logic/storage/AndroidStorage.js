import { AbstractStorageEngine } from './AbstractStorageEngine.js';

/**
 * AndroidStorageEngine handles track saving via native Kotlin WebView bridge hooks.
 */
export class AndroidStorageEngine extends AbstractStorageEngine {
    /**
     * Delegates track saving to Kotlin Android interface or local endpoint.
     * @param {string} videoId
     * @param {string} artist
     * @param {string} title
     * @param {string} audioUrl
     * @returns {Promise<Object>}
     */
    async saveTrack(videoId, artist, title, audioUrl) {
        try {
            if (typeof window !== 'undefined' && window.AndroidBridge && window.AndroidBridge.saveTrack) {
                const res = window.AndroidBridge.saveTrack(videoId, artist, title, audioUrl);
                return JSON.parse(res);
            }
            const endpoint = `/api/save?videoId=${encodeURIComponent(videoId)}&artist=${encodeURIComponent(artist)}&title=${encodeURIComponent(title)}&url=${encodeURIComponent(audioUrl)}`;
            const response = await fetch(endpoint);
            return await response.json();
        } catch (error) {
            console.error('[AndroidStorage] Failed to save track:', error);
            return { status: 'error', message: error.message };
        }
    }

    /**
     * Retrieves saved tracks from Android app asset / local storage endpoint.
     * @returns {Promise<Array<Object>>}
     */
    async getSavedTracks() {
        try {
            if (typeof window !== 'undefined' && window.AndroidBridge && window.AndroidBridge.getSavedTracks) {
                const res = window.AndroidBridge.getSavedTracks();
                return JSON.parse(res);
            }
            const response = await fetch('/api/saved');
            return await response.json();
        } catch (error) {
            console.error('[AndroidStorage] Failed to get saved tracks:', error);
            return [];
        }
    }

    /**
     * Deletes a saved track from Android local storage.
     * @param {string} filename
     * @returns {Promise<Object>}
     */
    async deleteTrack(filename) {
        try {
            if (typeof window !== 'undefined' && window.AndroidBridge && window.AndroidBridge.deleteTrack) {
                const res = window.AndroidBridge.deleteTrack(filename);
                return JSON.parse(res);
            }
            const response = await fetch(`/api/delete?filename=${encodeURIComponent(filename)}`, { method: 'DELETE' });
            return await response.json();
        } catch (error) {
            console.error('[AndroidStorage] Failed to delete track:', error);
            return { status: 'error', message: error.message };
        }
    }
}
