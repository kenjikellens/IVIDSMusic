import { Config } from '../core/Config.js';
import { ProxyService } from './ProxyService.js';

/**
 * InvidiousResolver resolves YouTube playback stream endpoints via public Invidious instances.
 */
export class InvidiousResolver {
    #invidiousInstances = [
        'https://invidious.flokinet.to',
        'https://iv.melmac.space',
        'https://invidious.drgns.space'
    ];

    /**
     * Resolves playable audio stream URL for a given YouTube video ID.
     * @param {string} videoId
     * @returns {Promise<string|null>} Audio stream URL or null.
     */
    async getAudioStreamUrl(videoId) {
        if (Config.isNative) {
            return `/api/play?videoId=${encodeURIComponent(videoId)}`;
        }
        if (Config.isElectron) {
            return `saved-media://${videoId}`;
        }

        for (const instance of this.#invidiousInstances) {
            try {
                const url = `${instance}/api/v1/videos/${videoId}`;
                const response = await ProxyService.fetch(url);
                const data = await response.json();

                if (data.adaptiveFormats) {
                    const audioFormat = data.adaptiveFormats.find(f => f.type && f.type.includes('audio'));
                    if (audioFormat && audioFormat.url) {
                        return audioFormat.url;
                    }
                }
            } catch (err) {
                console.warn(`[InvidiousResolver] Instance ${instance} failed for video ${videoId}:`, err);
            }
        }
        return null;
    }
}
