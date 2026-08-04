import { AbstractDownloader } from './AbstractDownloader.js';
import { AndroidStorageEngine } from '../storage/AndroidStorage.js';

/**
 * AndroidDownloader delegates track saving to Android Kotlin bridge interface.
 */
export class AndroidDownloader extends AbstractDownloader {
    #storage = new AndroidStorageEngine();

    /**
     * Downloads and saves track on Android filesystem via native bridge.
     * @param {Object} trackInfo
     * @returns {Promise<Object>}
     */
    async downloadTrack(trackInfo) {
        const { videoId, artist, title, audioUrl } = trackInfo;
        return await this.#storage.saveTrack(videoId, artist, title, audioUrl);
    }

    /**
     * Cancels active download task on Android.
     */
    cancelDownload() {
        // Native Android webview interceptor handles lifecycle
    }
}
