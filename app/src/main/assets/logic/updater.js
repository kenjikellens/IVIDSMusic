import { Config } from './config.js';

const CURRENT_VERSION = '0.2.0';
const REPO = 'kenjikellens/IVIDSMusic';
const API_URL = `https://api.github.com/repos/${REPO}/releases/latest`;
const STORAGE_KEY = 'iv_last_update_check';

export const Updater = {
    currentVersion: CURRENT_VERSION,
    isManualCheck: false,

    /**
     * Proxy-aware fetch helper to resolve CORS/security blocks in Native WebView.
     * It intercepts requests when native and redirects to local API proxy endpoints.
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
            if (isExternal && !url.includes(':3000') && !url.includes('api.github.com')) {
                finalUrl = `https://corsproxy.io/?url=${encodeURIComponent(url)}`;
            }
        }

        return await fetch(finalUrl, options);
    },

    /**
     * Retrieves the translated text for a given key from the active language translations, falling back to a default value.
     * Integrates with the global LanguageManager to provide localized UI text elements.
     * 
     * @param {string} key - The translation key to look up.
     * @param {string} defaultVal - The default fallback string if key is not found.
     * @returns {string} The localized string.
     */
    translate(key, defaultVal) {
        if (window.LanguageManager && window.LanguageManager.translations) {
            return window.LanguageManager.translations[key] || defaultVal;
        }
        return defaultVal;
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
     * Performs the network request to GitHub's Releases API to fetch release metadata.
     * Hits GitHub API or localized fallback proxy to return latest release tag info.
     * 
     * @returns {Promise<Object|null>} Latest release object or null if failed.
     */
    async fetchLatestRelease() {
        try {
            const response = await this._fetch(API_URL, {
                headers: { 'Accept': 'application/vnd.github.v3+json' }
            });
            if (response.status === 404) {
                return { tag_name: CURRENT_VERSION, name: 'No Releases Published', body: 'No remote releases found.' };
            }
            if (!response.ok) throw new Error(`HTTP Error ${response.status}`);
            return await response.json();
        } catch (e) {
            console.error('[Updater] Failed to fetch latest release:', e);
            return null;
        }
    },

    /**
     * Triggers a manual update check from the Settings page.
     * Delegates checking to the native Android updater if available, otherwise runs fallback web fetch.
     */
    async checkManual() {
        const btn = document.getElementById('check-updates-btn');
        if (btn) {
            btn.disabled = true;
            btn.textContent = 'Checking...';
        }

        if (window.AndroidUpdate) {
            console.log('[Updater] Triggering native manual check...');
            Updater.isManualCheck = true;
            window.AndroidUpdate.checkForUpdates();
        } else {
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
        }
    },

    /**
     * Runs the automated daily update check on app boot.
     * Utilizes local storage timestamp to check native or web release status once every 24 hours.
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
            
            if (window.AndroidUpdate) {
                console.log('[Updater] Triggering native auto-check...');
                Updater.isManualCheck = false;
                window.AndroidUpdate.checkForUpdates();
            } else {
                const release = await this.fetchLatestRelease();
                if (release && this.isNewer(CURRENT_VERSION, release.tag_name)) {
                    this.showUpdateDialog(release);
                }
            }
        } catch (e) {
            console.error('[Updater] Auto-check error:', e);
        }
    },

    /**
     * Renders a gorgeous glassmorphic overlay dialog displaying release details
     * and triggers native updater download and installation flows if confirmed.
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
        
        const downloadUrl = apkAsset ? apkAsset.browser_download_url : `https://github.com/${REPO}/raw/main/IVIDSMusic.apk`;

        // Format raw markdown bodies to simple bullet points
        const cleanBody = release.body 
            ? release.body.replace(/[#*`]/g, '').trim().split('\n').slice(0, 8).join('<br>• ') 
            : 'Performance and core interface enhancements.';

        const translatedTitle = this.translate('update_title', 'New Version Available!');
        const translatedChangelogTitle = this.translate('update_changelog_title', "What's New:");
        const translatedLater = this.translate('update_later', 'Later');
        const translatedNow = this.translate('update_now', 'Update Now');

        overlay.innerHTML = `
            <div class="update-modal glassmorphism">
                <div class="update-header">
                    <div class="update-rocket">🚀</div>
                    <h2 class="update-title">${translatedTitle}</h2>
                </div>
                <div class="update-info-block">
                    <div class="version-badge-container">
                        <span class="version-badge current">v${CURRENT_VERSION}</span>
                        <span class="version-arrow">→</span>
                        <span class="version-badge remote">${release.tag_name}</span>
                    </div>
                    <div class="update-release-name">${release.name || 'Prerelease Update'}</div>
                    <div class="update-changelog-title">${translatedChangelogTitle}</div>
                    <div class="update-changelog-body">• ${cleanBody}</div>
                </div>
                <div class="update-progress-container" id="update-progress-container">
                    <div class="update-progress-bar-bg">
                        <div class="update-progress-bar" id="update-progress-bar"></div>
                    </div>
                    <span class="update-progress-text" id="update-progress-text"></span>
                </div>
                <div class="update-footer-actions" id="update-actions">
                    <button class="btn secondary" id="update-dismiss-btn">${translatedLater}</button>
                    <button class="btn primary" id="update-download-btn">${translatedNow}</button>
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
        const progressContainer = overlay.querySelector('#update-progress-container');
        const progressBar = overlay.querySelector('#update-progress-bar');
        const progressText = overlay.querySelector('#update-progress-text');
        const actionsContainer = overlay.querySelector('#update-actions');

        if (downloadBtn) {
            downloadBtn.onclick = () => {
                if (window.AndroidUpdate) {
                    if (actionsContainer) actionsContainer.style.display = 'none';
                    if (progressContainer) progressContainer.style.display = 'flex';
                    if (progressBar) progressBar.style.width = '0%';
                    if (progressText) progressText.textContent = 'Downloading (0%)...';

                    try {
                        window.AndroidUpdate.downloadAndInstallForUrl(downloadUrl);
                    } catch (e) {
                        console.error('[Updater] Failed to trigger native download:', e);
                        if (progressText) {
                            progressText.textContent = 'Failed to start download.';
                            progressText.style.color = '#ef4444';
                        }
                        if (actionsContainer) actionsContainer.style.display = 'flex';
                    }
                } else {
                    window.open(downloadUrl, '_blank');
                    overlay.remove();
                }
            };
        }
    }
};

if (typeof window !== 'undefined') {
    window.Updater = Updater;

    /**
     * Global bridge callback called by Android native code when a new release version is discovered.
     * Resets the checking indicators and launches the update dialog with changelog details.
     * 
     * @param {string} version - The version tag of the remote release found.
     */
    window.onUpdateFound = async (version) => {
        console.log('[Updater] Native callback: Update found:', version);
        
        const btn = document.getElementById('check-updates-btn');
        if (btn) {
            btn.disabled = false;
            btn.textContent = 'Check for Updates';
        }

        // Fetch the latest release to show the rich changelog dialog
        let release = await Updater.fetchLatestRelease();
        if (!release || release.tag_name !== version) {
            release = {
                tag_name: version,
                name: `Release ${version}`,
                body: 'Performance and stability improvements.'
            };
        }
        
        Updater.isManualCheck = false;
        Updater.showUpdateDialog(release);
    };

    /**
     * Global bridge callback called by Android native code when no updates are found.
     * Resets checking button indicators and alerts the user if they checked manually.
     */
    window.onNoUpdateFound = () => {
        console.log('[Updater] Native callback: No update found');
        const btn = document.getElementById('check-updates-btn');
        if (btn) {
            btn.disabled = false;
            btn.textContent = 'Check for Updates';
        }
        if (Updater.isManualCheck) {
            alert(`You are up to date!\n\nCurrent Version: ${CURRENT_VERSION}`);
        }
        Updater.isManualCheck = false;
    };

    /**
     * Global bridge callback called by Android native code to publish stage status key transitions.
     * Dynamically resolves localized updates and writes state messages inside the progress text element.
     * 
     * @param {string} statusKey - Key representing the current process state.
     */
    window.onUpdateStatus = (statusKey) => {
        console.log('[Updater] Native callback: Status changed to:', statusKey);
        const progressText = document.getElementById('update-progress-text');
        if (progressText) {
            if (statusKey === 'downloading') {
                progressText.textContent = Updater.translate('update_status_downloading', 'Downloading update...');
            } else if (statusKey === 'installing') {
                progressText.textContent = Updater.translate('update_status_installing', 'Preparing installation...');
            } else if (statusKey === 'connecting-api') {
                progressText.textContent = Updater.translate('update_status_connecting', 'Connecting to server...');
            } else if (statusKey === 'fetching-releases') {
                progressText.textContent = Updater.translate('update_status_checking', 'Checking releases...');
            } else if (statusKey === 'comparing-versions') {
                progressText.textContent = Updater.translate('update_status_comparing', 'Comparing versions...');
            } else {
                progressText.textContent = statusKey;
            }
        }
    };

    /**
     * Global bridge callback called by Android native code to stream active download progress.
     * Translates integer percent values into styling modifications on the dialog progress bar.
     * 
     * @param {number} progress - Value ranging from 0 to 100 indicating percentage downloaded.
     */
    window.onUpdateProgress = (progress) => {
        const progressBar = document.getElementById('update-progress-bar');
        const progressText = document.getElementById('update-progress-text');
        if (progressBar) {
            progressBar.style.width = `${progress}%`;
        }
        if (progressText) {
            progressText.textContent = `${Updater.translate('update_status_downloading', 'Downloading update')} (${progress}%)...`;
        }
    };

    /**
     * Global bridge callback called by Android native code when update operations encounter exceptions.
     * Displays descriptive alert texts inside the progress container and reveals action retry triggers.
     */
    window.onUpdateCheckError = () => {
        console.error('[Updater] Native callback: Error checking or downloading');
        const btn = document.getElementById('check-updates-btn');
        if (btn) {
            btn.disabled = false;
            btn.textContent = 'Check for Updates';
        }
        const progressText = document.getElementById('update-progress-text');
        if (progressText) {
            progressText.textContent = Updater.translate('update_status_failed', 'Update failed. Please check connection and try again.');
            progressText.style.color = '#ef4444';
        }
        const actionsContainer = document.getElementById('update-actions');
        if (actionsContainer) {
            actionsContainer.style.display = 'flex';
        }
        Updater.isManualCheck = false;
    };
}
