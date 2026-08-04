import { BaseService } from '../core/BaseService.js';
import { DeezerProvider } from './DeezerProvider.js';
import { iTunesProvider } from './iTunesProvider.js';
import { InvidiousResolver } from './InvidiousResolver.js';

/**
 * MusicRepositoryService provides a unified facade for metadata searching, charts, genres, and stream resolution.
 * All memory caching has been removed to ensure real-time, live data on every query.
 */
export class MusicRepositoryService extends BaseService {
    #deezer = new DeezerProvider();
    #itunes = new iTunesProvider();
    #invidious = new InvidiousResolver();

    /**
     * Universal metadata search across music providers without in-memory caching.
     */
    async search(query = 'top hits', limit = 20, type = 'all', yearRange = null, offset = 0, unique = false, signal = null) {
        return await this.#deezer.search(query, limit, type, yearRange, offset, unique, signal);
    }

    /**
     * Fetch category rows using genre chart endpoints directly.
     */
    async getCategories(genres, signal = null) {
        return await this.#deezer.getCategories(genres, signal);
    }

    /**
     * Alias for getCategories (recommendations feed).
     */
    async getRecommendations(signal = null) {
        return await this.getCategories(undefined, signal);
    }

    /**
     * Retrieves top chart tracks directly.
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

    /**
     * Calculates dominant cover artwork color without memory caching.
     */
    async getAverageColor(imageUrl) {
        if (!imageUrl) return 'rgba(255,255,255,0.05)';

        return new Promise((resolve) => {
            const img = new Image();
            img.crossOrigin = "Anonymous";
            img.src = imageUrl;
            img.onload = () => {
                try {
                    const canvas = document.createElement("canvas");
                    canvas.width = 16;
                    canvas.height = 16;
                    const ctx = canvas.getContext("2d");
                    ctx.drawImage(img, 0, 0, 16, 16);
                    const data = ctx.getImageData(0, 0, 16, 16).data;
                    let r = 0, g = 0, b = 0;
                    for (let i = 0; i < data.length; i += 4) {
                        r += data[i]; g += data[i + 1]; b += data[i + 2];
                    }
                    const count = data.length / 4;
                    const color = `rgb(${Math.round(r / count)}, ${Math.round(g / count)}, ${Math.round(b / count)})`;
                    resolve(color);
                } catch (e) {
                    resolve("rgba(255,255,255,0.05)");
                }
            };
            img.onerror = () => resolve("rgba(255,255,255,0.05)");
        });
    }

    /**
     * Gets artist profile artwork.
     */
    async getArtistImage(name) {
        const data = await this.search(name, 1, 'artist');
        return data.length ? data[0].cover : null;
    }
}

/** MusicRepository singleton instance */
export const MusicRepository = new MusicRepositoryService();
export const MusicAPI = MusicRepository;
