import { Config } from '../config.js';
import { ProxyService } from './ProxyService.js';

/**
 * InvidiousResolver resolves YouTube playback stream endpoints via parallel instance probing for ultra-fast audio loading.
 */
export class InvidiousResolver {
    #invidiousInstances = [
        'https://invidious.flokinet.to',
        'https://iv.melmac.space',
        'https://invidious.drgns.space'
    ];

    /**
     * Resolves playable audio stream URL for a given YouTube video ID using parallel race probing.
     * @param {string} videoId
     * @returns {Promise<string|null>} Audio stream URL or null.
     */
    async getAudioStreamUrl(videoId) {
        if (Config.isNative) {
            return `/api/play?videoId=${encodeURIComponent(videoId)}`;
        }
        if (Config.isElectron && window.ElectronAPI?.playTrack) {
            try {
                const res = await window.ElectronAPI.playTrack(videoId);
                if (res && res.status === 'ready' && res.url) {
                    return res.url;
                }
            } catch (e) {
                console.error('[InvidiousResolver] Electron playTrack error:', e);
            }
        }

        const resolveInstance = async (instance) => {
            const controller = new AbortController();
            const timeoutId = setTimeout(() => controller.abort(), 3500);
            try {
                const url = `${instance}/api/v1/videos/${videoId}`;
                const response = await ProxyService.fetch(url, { signal: controller.signal });
                clearTimeout(timeoutId);
                const data = await response.json();

                if (data.adaptiveFormats) {
                    const audioFormat = data.adaptiveFormats.find(f => f.type && f.type.includes('audio'));
                    if (audioFormat && audioFormat.url) {
                        return audioFormat.url;
                    }
                }
                throw new Error('No audio format found');
            } catch (err) {
                clearTimeout(timeoutId);
                throw err;
            }
        };

        try {
            return await Promise.any(this.#invidiousInstances.map(inst => resolveInstance(inst)));
        } catch (err) {
            console.error(`[InvidiousResolver] Parallel resolution failed for video ${videoId}:`, err);
            return null;
        }
    }
}
