import { Config } from '../config.js';

/**
 * ProxyService handles CORS proxy resolution across Native, Electron, and Web environments.
 */
export class ProxyService {
    /** Returns proxy URL for local server */
    static get proxyUrl() {
        return `${Config.SERVER_URL}/proxy?url=`;
    }

    /**
     * Routes external URLs through appropriate proxy if needed.
     * @param {string} url - Target URL.
     * @param {Object} [options] - Fetch options.
     * @returns {Promise<Response>}
     */
    static async fetch(url, options = {}) {
        let finalUrl = url;
        const isExternal = url.startsWith('http') && typeof window !== 'undefined' && !url.startsWith(window.location.origin);

        if (Config.isNative) {
            if (isExternal && !url.includes('appassets.androidplatform.net')) {
                finalUrl = this.proxyUrl + encodeURIComponent(url);
            }
        } else {
            if (isExternal && !url.includes(':3000') && !Config.isElectron) {
                finalUrl = `https://corsproxy.io/?url=${encodeURIComponent(url)}`;
            }
        }

        const fetchOptions = {
            cache: 'no-store',
            ...options
        };

        const response = await fetch(finalUrl, fetchOptions);
        if (!response.ok) throw new Error(`HTTP ${response.status} for ${url}`);
        return response;
    }
}
