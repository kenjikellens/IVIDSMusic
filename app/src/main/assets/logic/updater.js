import { Config } from './config.js';

const CURRENT_VERSION = '0.1.4';
const REPO = 'kenjikellens/IVIDSMusic';
const API_URL = `https://api.github.com/repos/${REPO}/releases/latest`;
const STORAGE_KEY = 'iv_last_update_check';

export const Updater = {
    currentVersion: CURRENT_VERSION,

    /**
     * Proxy-aware fetch helper to resolve CORS/security blocks in Native WebView.
     * 
     * @param {string} url - Target URL.
     * @param {Object} options - Fetch options.
     * @returns {Promise<Response>} Fetch Response object.
     */
    async _fetch(url, options = {}) {
        let finalUrl = url;
        const isExternal = url.startsWith('http') && typeof window !== 'undefined' && !url.startsWith(window.location.origin);

        if (Config.isNative) {
            if (isExternal && !url.includes('appassets.androidplatform.net')) {
                finalUrl = `/api/proxy?url=` + encodeURIComponent(url);
            }
        } else {
            if (isExternal && !url.includes(':3000')) {
                finalUrl = `https://corsproxy.io/?url=${encodeURIComponent(url)}`;
            }
        }

        return await fetch(finalUrl, options);
    },

    /**
     * Parses a version string into an array of integers for comparison.
     * Handles formats like "v1.1.0", "1.0", "2.0.1-beta".
     * 
     * @param {string} versionStr - The version string to parse.
     * @returns {Array<number>} Numeric parts of the version.
     */
    parseVersion(versionStr) {
        const clean = versionStr.replace(/^v/i, '').split('-')[0];
        const parts = clean.split('.').map(p => parseInt(p, 10) || 0);
        while (parts.length < 3) parts.push(0);
        return parts;
    },

    /**
     * Compares two semantic version strings.
     * 
     * @param {string} local - Local version (e.g., "1.0.0")
     * @param {string} remote - Remote version (e.g., "1.1.0")
     * @returns {boolean} True if remote is strictly newer than local.
     */
    isNewer(local, remote) {
        const localParts = this.parseVersion(local);
        const remoteParts = this.parseVersion(remote);

        for (let i = 0; i < 3; i++) {
            if (remoteParts[i] > localParts[i]) return true;
            if (remoteParts[i] < localParts[i]) return false;
        }
        return false;
    },

    /**
     * Performs the network request to GitHub's Releases API.
     * 
     * @returns {Promise<Object|null>} Latest release object or null if failed.
     */
    async fetchLatestRelease() {
        try {
            const response = await this._fetch(API_URL, {
                headers: { 'Accept': 'application/vnd.github.v3+json' }
            });
            if (!response.ok) throw new Error(`HTTP Error ${response.status}`);
            return await response.json();
        } catch (e) {
            console.error('[Updater] Failed to fetch latest release:', e);
            return null;
        }
    },

    /**
     * Triggers a manual update check from the Settings page.
     * Shows feedback loading spinners and localized alerts.
     */
    async checkManual() {
        const btn = document.getElementById('check-updates-btn');
        if (btn) {
            btn.disabled = true;
            btn.textContent = 'Checking...';
        }

        const release = await this.fetchLatestRelease();

        if (btn) {
            btn.disabled = false;
            btn.textContent = 'Check for Updates';
        }

        if (!release) {
            alert('Unable to contact the update server. Please check your internet connection.');
            return;
        }

        const remoteVer = release.tag_name;
        if (this.isNewer(CURRENT_VERSION, remoteVer)) {
            this.showUpdateDialog(release);
        } else {
            alert(`You are up to date!\n\nCurrent Version: ${CURRENT_VERSION}\nLatest Version: ${remoteVer}`);
        }
    },

    /**
     * Runs the automated daily update check on app boot.
     * Utilizes local storage timestamps to limit API checks to once per 24 hours.
     */
    async initAutoCheck() {
        try {
            const now = Date.now();
            const lastCheck = localStorage.getItem(STORAGE_KEY);
            
            // 24 hours = 86400000 ms
            if (lastCheck && (now - parseInt(lastCheck, 10)) < 86400000) {
                console.log('[Updater] Skipping auto-check: last check was less than 24 hours ago.');
                return;
            }

            localStorage.setItem(STORAGE_KEY, now.toString());
            
            const release = await this.fetchLatestRelease();
            if (release && this.isNewer(CURRENT_VERSION, release.tag_name)) {
                this.showUpdateDialog(release);
            }
        } catch (e) {
            console.error('[Updater] Auto-check error:', e);
        }
    },

    /**
     * Renders a gorgeous glassmorphic overlay dialog displaying release details
     * and direct installation/download actions.
     * 
     * @param {Object} release - The GitHub release data object.
     */
    showUpdateDialog(release) {
        // Remove existing if any
        const existing = document.getElementById('update-overlay');
        if (existing) existing.remove();

        const overlay = document.createElement('div');
        overlay.id = 'update-overlay';
        overlay.className = 'update-modal-overlay';
        
        let apkAsset = null;
        if (release.assets && Array.isArray(release.assets)) {
            apkAsset = release.assets.find(a => a.name.endsWith('.apk'));
        }
        
        const downloadUrl = apkAsset ? apkAsset.browser_download_url : release.html_url;

        // Format raw markdown bodies to simple bullet points
        const cleanBody = release.body 
            ? release.body.replace(/[#*`]/g, '').trim().split('\n').slice(0, 8).join('<br>• ') 
            : 'Performance and core interface enhancements.';

        overlay.innerHTML = `
            <div class="update-modal glassmorphism">
                <div class="update-header">
                    <div class="update-rocket">🚀</div>
                    <h2 class="update-title">New Version Available!</h2>
                </div>
                <div class="update-info-block">
                    <div class="version-badge-container">
                        <span class="version-badge current">v${CURRENT_VERSION}</span>
                        <span class="version-arrow">→</span>
                        <span class="version-badge remote">${release.tag_name}</span>
                    </div>
                    <div class="update-release-name">${release.name || 'Prerelease Update'}</div>
                    <div class="update-changelog-title">What's New:</div>
                    <div class="update-changelog-body">• ${cleanBody}</div>
                </div>
                <div class="update-footer-actions">
                    <button class="btn secondary" id="update-dismiss-btn">Later</button>
                    <a href="${downloadUrl}" class="btn primary" id="update-download-btn" target="_blank">Update Now</a>
                </div>
            </div>
        `;

        document.body.appendChild(overlay);

        // Bind actions
        const dismissBtn = overlay.querySelector('#update-dismiss-btn');
        if (dismissBtn) {
            dismissBtn.onclick = () => {
                overlay.remove();
            };
        }

        const downloadBtn = overlay.querySelector('#update-download-btn');
        if (downloadBtn) {
            downloadBtn.onclick = () => {
                overlay.remove();
            };
        }
    }
};

if (typeof window !== 'undefined') {
    window.Updater = Updater;
}
