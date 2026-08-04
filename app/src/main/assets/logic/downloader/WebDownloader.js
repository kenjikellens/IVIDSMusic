import { AbstractDownloader } from './AbstractDownloader.js';
import { IndexedDBStorage } from '../indexeddb-storage.js';

/**
 * WebDownloader caches track audio streams inside browser IndexedDB as Blobs.
 */
export class WebDownloader extends AbstractDownloader {
    #abortController = null;

    /**
     * Downloads audio stream to IndexedDB Blob storage.
     * @param {Object} trackInfo
     * @returns {Promise<Object>}
     */
    async downloadTrack(trackInfo) {
        this.#abortController = new AbortController();
        const { videoId, artist, title, audioUrl } = trackInfo;
        try {
            this.emit('progress', { videoId, progress: 10, status: 'fetching' });
            const result = await IndexedDBStorage.saveTrack(videoId, artist, title, audioUrl);
            this.emit('progress', { videoId, progress: 100, status: 'complete' });
            return result;
        } catch (error) {
            this.emit('error', { videoId, error: error.message });
            return { status: 'error', message: error.message };
        }
    }

    /**
     * Aborts current download request.
     */
    cancelDownload() {
        if (this.#abortController) {
            this.#abortController.abort();
            this.#abortController = null;
        }
    }
}
