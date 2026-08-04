import { BaseService } from '../core/BaseService.js';
import { DeezerProvider } from './DeezerProvider.js';
import { iTunesProvider } from './iTunesProvider.js';
import { InvidiousResolver } from './InvidiousResolver.js';

/**
 * MusicRepositoryService provides a unified facade for metadata searching, charts, and stream resolution.
 */
export class MusicRepositoryService extends BaseService {
    #deezer = new DeezerProvider();
    #itunes = new iTunesProvider();
    #invidious = new InvidiousResolver();

    /**
     * Universal metadata search across music providers.
     */
    async search(query = 'top hits', limit = 20, type = 'all', yearRange = null, offset = 0, unique = false, signal = null) {
        return await this.#deezer.search(query, limit, type, yearRange, offset, unique, signal);
    }

    /**
     * Retrieves top chart tracks.
     */
    async getChart(limit = 20) {
        return await this.#deezer.getChart(limit);
    }

    /**
     * Resolves audio stream URL for playback.
     */
    async getAudioStreamUrl(videoId) {
        return await this.#invidious.getAudioStreamUrl(videoId);
    }
}

/** MusicRepository singleton instance */
export const MusicRepository = new MusicRepositoryService();
