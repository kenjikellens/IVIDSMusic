import { AbstractDownloader } from './AbstractDownloader.js';
import { ElectronStorageEngine } from '../storage/ElectronStorage.js';

/**
 * ElectronDownloader triggers yt-dlp PC Desktop IPC downloads.
 */
export class ElectronDownloader extends AbstractDownloader {
    #storage = new ElectronStorageEngine();

    /**
     * Initiates Electron IPC yt-dlp download task.
     * @param {Object} trackInfo
     * @returns {Promise<Object>}
     */
    async downloadTrack(trackInfo) {
        return await this.#storage.saveTrack(
            trackInfo.videoId,
            trackInfo.artist,
            trackInfo.title,
            trackInfo.audioUrl
        );
    }

    /**
     * Cancels active yt-dlp download process via Electron IPC.
     */
    cancelDownload() {
        if (typeof window !== 'undefined' && window.electronAPI && window.electronAPI.cancelDownload) {
            window.electronAPI.cancelDownload();
        }
    }
}
