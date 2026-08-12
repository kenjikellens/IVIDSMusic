import { BaseService } from '../core/BaseService.js';
import { DeezerProvider } from './DeezerProvider.js';
import { iTunesProvider } from './iTunesProvider.js';
import { InvidiousResolver } from './InvidiousResolver.js';

/**
 * MusicRepositoryService provides a unified facade for metadata searching, charts, genres, and stream resolution.
 * Utilizes high-performance TTL in-memory caching for category rows and search queries.
 */
export class MusicRepositoryService extends BaseService {
    #deezer = new DeezerProvider();
    #itunes = new iTunesProvider();
    #invidious = new InvidiousResolver();
    #cache = new Map();
    #ttlMs = 15 * 60 * 1000; // 15 minutes TTL

    #getCache(key) {
        const entry = this.#cache.get(key);
        if (!entry) return null;
        if (Date.now() - entry.timestamp > this.#ttlMs) {
            this.#cache.delete(key);
            return null;
        }
        return entry.data;
    }

    #setCache(key, data) {
        if (this.#cache.size > 200) {
            const oldestKey = this.#cache.keys().next().value;
            this.#cache.delete(oldestKey);
        }
        this.#cache.set(key, { data, timestamp: Date.now() });
    }

    /**
     * Universal metadata search across music providers with in-memory TTL caching.
     */
    async search(query = 'top hits', limit = 20, type = 'all', yearRange = null, offset = 0, unique = false, signal = null) {
        const cacheKey = `search:${query}:${limit}:${type}:${yearRange}:${offset}:${unique}`;
        const cached = this.#getCache(cacheKey);
        if (cached) return cached;

        const results = await this.#deezer.search(query, limit, type, yearRange, offset, unique, signal);
        if (results && results.length > 0) {
            this.#setCache(cacheKey, results);
        }
        return results;
    }

    /**
     * Fetch category rows using genre chart endpoints directly with TTL caching.
     */
    async getCategories(genres, signal = null) {
        const genreKey = genres ? genres.join(',') : 'default';
        const cacheKey = `categories:${genreKey}`;
        const cached = this.#getCache(cacheKey);
        if (cached) return cached;

        const categories = await this.#deezer.getCategories(genres, signal);
        if (categories && categories.length > 0) {
            this.#setCache(cacheKey, categories);
        }
        return categories;
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
        const cacheKey = `chart:${limit}`;
        const cached = this.#getCache(cacheKey);
        if (cached) return cached;

        const chart = await this.#deezer.getChart(limit);
        if (chart && chart.length > 0) {
            this.#setCache(cacheKey, chart);
        }
        return chart;
    }

    /**
     * Resolves audio stream URL for playback.
     */
    async getAudioStreamUrl(videoId) {
        return await this.#invidious.getAudioStreamUrl(videoId);
    }

    #colorCache = new Map();

    /**
     * Calculates dominant cover artwork color with fast in-memory Map caching.
     */
    async getAverageColor(imageUrl) {
        if (!imageUrl) return 'rgba(255,255,255,0.05)';
        if (this.#colorCache.has(imageUrl)) return this.#colorCache.get(imageUrl);

        const color = await new Promise((resolve) => {
            const img = new Image();
            img.crossOrigin = "Anonymous";
            img.src = imageUrl;
            img.onload = () => {
                try {
                    const canvas = document.createElement("canvas");
                    canvas.width = 8;
                    canvas.height = 8;
                    const ctx = canvas.getContext("2d", { willReadFrequently: true });
                    ctx.drawImage(img, 0, 0, 8, 8);
                    const data = ctx.getImageData(0, 0, 8, 8).data;
                    let r = 0, g = 0, b = 0;
                    for (let i = 0; i < data.length; i += 4) {
                        r += data[i]; g += data[i + 1]; b += data[i + 2];
                    }
                    const count = data.length / 4;
                    resolve(`rgb(${Math.round(r / count)}, ${Math.round(g / count)}, ${Math.round(b / count)})`);
                } catch (e) {
                    resolve("rgba(255,255,255,0.05)");
                }
            };
            img.onerror = () => resolve("rgba(255,255,255,0.05)");
        });

        if (this.#colorCache.size > 300) {
            const firstKey = this.#colorCache.keys().next().value;
            this.#colorCache.delete(firstKey);
        }
        this.#colorCache.set(imageUrl, color);
        return color;
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

