import { Config } from './config.js';
import { IndexedDBStorage } from './indexeddb-storage.js';
import { MusicAPI } from './api.js';

let loadedTracks = [];
let activeAbortController = null;
let electronUnsubscribeCallbacks = [];

export const DownloaderPage = {
    /**
     * Extracts videoId or playlistId from a YouTube URL.
     * It handles various URL patterns like youtu.be, shorts, embeds, and raw IDs.
     * 
     * @param {string} url - The YouTube input URL.
     * @returns {Object} Object containing resolved videoId and/or playlistId.
     */
    extractYoutubeId(url) {
        let videoId = null;
        let playlistId = null;
        
        try {
            const u = new URL(url);
            if (u.hostname.includes('youtube.com') || u.hostname.includes('youtu.be')) {
                playlistId = u.searchParams.get('list');
                if (u.hostname.includes('youtu.be')) {
                    videoId = u.pathname.substring(1).split('&')[0];
                } else if (u.pathname.includes('/watch')) {
                    videoId = u.searchParams.get('v');
                } else if (u.pathname.includes('/embed/') || u.pathname.includes('/v/')) {
                    videoId = u.pathname.split('/')[2].split('&')[0];
                } else if (u.pathname.includes('/shorts/')) {
                    videoId = u.pathname.split('/shorts/')[1].split('&')[0];
                }
            }
        } catch (e) {
            if (url.startsWith('PL') && url.length >= 18) {
                playlistId = url;
            } else if (url.length === 11) {
                videoId = url;
            }
        }
        return { videoId, playlistId };
    },

    /**
     * Formats a track duration value in seconds to a human-readable MM:SS string.
     * Returns a localized unknown string if duration is invalid or not available.
     * 
     * @param {number} durationSeconds - The total duration in seconds.
     * @returns {string} Formatted time string.
     */
    formatDuration(durationSeconds) {
        if (!durationSeconds || isNaN(durationSeconds)) return this.getTranslation('preview_unknown', 'Unknown');
        const minutes = Math.floor(durationSeconds / 60);
        const seconds = Math.floor(durationSeconds % 60);
        return `${minutes}:${seconds < 10 ? '0' : ''}${seconds}`;
    },

    /**
     * Helper to get a translation key or fallback default text.
     * This relies on window.LanguageManager's active translations mapping.
     * 
     * @param {string} key - Translation dictionary key.
     * @param {string} defaultValue - Default text fallback.
     * @returns {string} Localized string.
     */
    getTranslation(key, defaultValue) {
        if (window.LanguageManager && window.LanguageManager.translations[key] !== undefined) {
            return window.LanguageManager.translations[key];
        }
        return defaultValue;
    },

    /**
     * Appends a log line inside the scrolling console box in the UI.
     * Automatically scrolls the container to show the latest entries.
     * 
     * @param {string} msg - The log text to add.
     */
    addLog(msg) {
        const consoleContainer = document.getElementById('console');
        if (!consoleContainer) return;

        const div = document.createElement('div');
        div.className = 'log-line';

        if (msg.indexOf('[Success]') !== -1) {
            div.className += ' log-success';
        } else if (msg.indexOf('[Error]') !== -1) {
            div.className += ' log-error';
        } else if (msg.indexOf('[Warning]') !== -1) {
            div.className += ' log-warn';
        }

        div.textContent = msg;
        consoleContainer.appendChild(div);
        consoleContainer.scrollTop = consoleContainer.scrollHeight;
    },

    /**
     * Sets the width fill percentage on the HTML progress bar.
     * This visually reports download progress.
     * 
     * @param {number} percent - Completion value between 0 and 100.
     */
    setProgress(percent) {
        const fill = document.getElementById('progress-fill');
        if (fill) {
            fill.style.width = `${percent}%`;
        }
    },

    /**
     * Updates the status bar header text with formatted translations.
     * This dynamically replaces current and total counts in the download labels.
     * 
     * @param {string} status - Current status text.
     * @param {string} track - Active track title or details.
     */
    setStatus(status, track) {
        const statusText = document.getElementById('status-text');
        const trackText = document.getElementById('track-text');
        
        let translatedStatus = status;
        if (status === 'Idle') {
            translatedStatus = this.getTranslation('status_idle', 'Idle');
        } else if (status === 'Querying URL...') {
            translatedStatus = this.getTranslation('status_querying', 'Querying URL...');
        } else if (status === 'Setup...') {
            translatedStatus = this.getTranslation('status_setup', 'Setup...');
        } else if (status === 'Completed') {
            translatedStatus = this.getTranslation('status_completed', 'Completed');
        } else if (status === 'Failed') {
            translatedStatus = this.getTranslation('status_failed', 'Failed');
        } else if (status && status.startsWith('Downloading track ')) {
            const match = status.match(/Downloading track (\d+) of (\d+)/);
            if (match) {
                const current = match[1];
                const total = match[2];
                const template = this.getTranslation('status_downloading_track', 'Downloading track {current} of {total}');
                translatedStatus = template.replace('{current}', current).replace('{total}', total);
            } else {
                translatedStatus = this.getTranslation('status_downloading', 'Downloading');
            }
        }
        
        let translatedTrack = track;
        if (track === 'Finished!') {
            translatedTrack = this.getTranslation('status_finished', 'Finished!');
        } else if (track === 'Downloading yt-dlp') {
            translatedTrack = this.getTranslation('status_downloading_ytdlp', 'Downloading yt-dlp');
        } else if (track === 'Downloading FFmpeg') {
            translatedTrack = this.getTranslation('status_downloading_ffmpeg', 'Downloading FFmpeg');
        }
        
        const prefix = this.getTranslation('status_prefix', 'Status: ');
        if (statusText) statusText.textContent = prefix + translatedStatus;
        if (trackText) trackText.textContent = translatedTrack || '';
    },

    /**
     * Toggles the display state of the preview panel card layout.
     * This chooses between empty, single track, or scrollable checklist lists.
     * 
     * @param {string} state - The target layout state to activate.
     */
    showPreviewState(state) {
        document.getElementById('state-empty').style.display = 'none';
        document.getElementById('state-single').style.display = 'none';
        document.getElementById('state-playlist').style.display = 'none';

        if (state === 'empty') {
            document.getElementById('state-empty').style.display = 'flex';
        } else if (state === 'single') {
            document.getElementById('state-single').style.display = 'flex';
        } else if (state === 'playlist') {
            document.getElementById('state-playlist').style.display = 'flex';
        }
    },

    /**
     * Fetches metadata for a YouTube URL client-side by querying public Invidious instances.
     * It handles single video information or entire playlist tracks arrays.
     * 
     * @param {string} url - YouTube URL.
     * @returns {Promise<Array<Object>>} Resolved track metadata list.
     */
    async fetchClientMetadata(url) {
        const { videoId, playlistId } = this.extractYoutubeId(url);
        if (!playlistId && !videoId) {
            throw new Error('Invalid YouTube URL');
        }
        
        const instances = MusicAPI.invidiousInstances;
        let lastError = null;
        
        if (playlistId) {
            for (const instance of instances) {
                try {
                    const fetchUrl = `${instance}/api/v1/playlists/${playlistId}`;
                    const response = await fetch(fetchUrl);
                    if (!response.ok) continue;
                    const json = await response.json();
                    if (json.videos) {
                        return json.videos.map(v => ({
                            title: v.title,
                            id: v.videoId,
                            duration: v.lengthSeconds,
                            channel: v.author
                        }));
                    }
                } catch (e) {
                    lastError = e;
                }
            }
        } else if (videoId) {
            for (const instance of instances) {
                try {
                    const fetchUrl = `${instance}/api/v1/videos/${videoId}`;
                    const response = await fetch(fetchUrl);
                    if (!response.ok) continue;
                    const json = await response.json();
                    return [{
                        title: json.title,
                        id: json.videoId,
                        duration: json.lengthSeconds,
                        channel: json.author
                    }];
                } catch (e) {
                    lastError = e;
                }
            }
        }
        throw lastError || new Error('All Invidious instances failed to load metadata');
    },

    /**
     * Resolves the target directory paths and updates UI display.
     * Reads custom default configurations from local storage or falls back to system defaults.
     */
    async initOutputDirectory() {
        if (!Config.isElectron) return;

        let mode = 'standard';
        try {
            mode = localStorage.getItem('dir-mode') || 'standard';
        } catch (e) {}

        let activeDir = null;
        if (mode === 'custom') {
            try {
                activeDir = localStorage.getItem('custom-dir');
            } catch (e) {}
        } else if (mode === 'last') {
            try {
                activeDir = localStorage.getItem('last-dir');
            } catch (e) {}
        }

        if (activeDir) {
            this.setOutputDir(activeDir);
        } else {
            try {
                const standardPath = await window.ElectronAPI.getDefaultDir();
                if (standardPath) {
                    this.setOutputDir(standardPath);
                }
            } catch (e) {
                this.setOutputDir('C:\\Downloads');
            }
        }
    },

    /**
     * Configures the save location directory path label element.
     * 
     * @param {string} folderPath - Target OS directory location.
     */
    setOutputDir(folderPath) {
        const dirElement = document.getElementById('dir-path');
        if (dirElement) {
            dirElement.textContent = folderPath;
        }
    },

    /**
     * Opens the native folder picker to select the downloads directory.
     * This saves choice to local storage and reports outcomes.
     */
    async browseDirectory() {
        try {
            const folderPath = await window.ElectronAPI.selectDirectory();
            if (folderPath) {
                this.setOutputDir(folderPath);
                try {
                    localStorage.setItem('last-dir', folderPath);
                } catch (e) {}
                this.addLog(this.getTranslation('log_output_folder_set', 'Output folder set to: ') + folderPath);
            }
        } catch (err) {
            this.addLog(`[Error] ${this.getTranslation('log_failed_select_dir', 'Failed to select directory: ')}${err.message}`);
        }
    },

    /**
     * Triggers URL input metadata loading and populates preview cards.
     * Validates input links and handles UI loading states.
     */
    async loadMetadata() {
        const urlInput = document.getElementById('url-input');
        const url = urlInput ? urlInput.value.trim() : '';
        if (!url) return;

        const btnLoad = document.getElementById('btn-load-info');
        if (btnLoad) {
            btnLoad.disabled = true;
            btnLoad.textContent = this.getTranslation('loading', 'Loading...');
        }

        this.addLog(this.getTranslation('log_fetching_metadata', 'Fetching metadata details for URL...'));
        this.showPreviewState('empty');
        document.getElementById('state-empty').innerHTML = `<p>${this.getTranslation('preview_loading', 'Loading tracks metadata...')}</p>`;

        try {
            let tracks = [];
            if (Config.isElectron) {
                const res = await window.ElectronAPI.fetchMetadata(url);
                if (res.error) throw new Error(res.error);
                tracks = res.tracks || [];
            } else {
                tracks = await this.fetchClientMetadata(url);
            }

            loadedTracks = tracks;
            this.renderPreview(tracks);
        } catch (err) {
            this.addLog(`[Error] ${this.getTranslation('log_failed_metadata', 'Failed loading metadata: ')}${err.message}`);
            document.getElementById('state-empty').innerHTML = `
                <svg class="preview-empty-icon" viewBox="0 0 24 24" width="44" height="44">
                    <path d="M21 3H3c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h18c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16H3V5h18v14zM9.5 16.5l7-4.5-7-4.5v9z"/>
                </svg>
                <p style="color:var(--accent-red);">[Error] ${err.message}</p>
            `;
        } finally {
            if (btnLoad) {
                btnLoad.disabled = false;
                btnLoad.textContent = this.getTranslation('preview_load', 'Load Info');
            }
        }
    },

    /**
     * Renders loaded track results inside visual cards or scrollable checklists.
     * Hooks change elements to synchronize playlist selections.
     * 
     * @param {Array<Object>} tracks - Metadatas list.
     */
    renderPreview(tracks) {
        if (!tracks || tracks.length === 0) {
            this.showPreviewState('empty');
            document.getElementById('state-empty').innerHTML = `
                <svg class="preview-empty-icon" viewBox="0 0 24 24" width="44" height="44">
                    <path d="M21 3H3c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h18c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16H3V5h18v14zM9.5 16.5l7-4.5-7-4.5v9z"/>
                </svg>
                <p>${this.getTranslation('preview_no_tracks', 'No tracks or video information found.')}</p>
            `;
            return;
        }

        if (tracks.length === 1) {
            this.showPreviewState('single');
            const track = tracks[0];
            const container = document.getElementById('single-video-card');
            const thumbUrl = `https://img.youtube.com/vi/${track.id}/mqdefault.jpg`;
            const durationText = track.duration ? this.formatDuration(track.duration) : '';
            const initials = track.channel ? track.channel.substring(0, 2).toUpperCase() : 'YT';

            container.innerHTML = `
                <div class="video-thumb">
                    <img src="${thumbUrl}" alt="Thumbnail">
                    ${durationText ? `<span class="video-duration">${durationText}</span>` : ''}
                </div>
                <div class="video-info">
                    <div class="video-info-title">${track.title}</div>
                    <div class="video-info-channel">
                        <div class="channel-avatar">${initials}</div>
                        <span>${track.channel || 'YouTube'}</span>
                    </div>
                    <div style="font-size: 11px; color: var(--text-muted); margin-top: 4px;">
                        ${this.getTranslation('preview_length', 'Length: ')}${durationText}
                    </div>
                </div>
            `;
        } else {
            this.showPreviewState('playlist');
            const tracksFoundTemplate = this.getTranslation('preview_tracks_found', 'tracks found');
            document.getElementById('track-count-text').textContent = `${tracks.length} ${tracksFoundTemplate}`;

            const list = document.getElementById('track-list');
            list.innerHTML = '';

            tracks.forEach((t, idx) => {
                const label = document.createElement('label');
                label.className = 'track-item';
                const thumbUrl = `https://img.youtube.com/vi/${t.id}/mqdefault.jpg`;
                const durationText = t.duration ? this.formatDuration(t.duration) : this.getTranslation('preview_unknown', 'Unknown');

                label.innerHTML = `
                    <input type="checkbox" class="track-cb" checked data-id="${t.id}">
                    <div class="track-thumb">
                        <img src="${thumbUrl}" alt="">
                    </div>
                    <div class="track-info">
                        <div class="track-title">${idx + 1}. ${t.title}</div>
                        <div class="track-artist">${t.channel || 'YouTube'}</div>
                        <div class="track-length">${this.getTranslation('preview_length', 'Length: ')}${durationText}</div>
                    </div>
                `;
                list.appendChild(label);
            });
        }
    },

    /**
     * Toggles select state across all playlist checkbox checklist items.
     * 
     * @param {boolean} checked - Selection boolean.
     */
    toggleSelectAll(checked) {
        document.querySelectorAll('.track-cb').forEach(cb => {
            cb.checked = checked;
        });
    },

    /**
     * Gathers configuration settings and initiates background download executions.
     * Routes task running either to Electron native IPC or client-side fetches.
     */
    startDownload() {
        const urlInput = document.getElementById('url-input');
        const url = urlInput ? urlInput.value.trim() : '';
        if (!url) {
            this.addLog(`[Warning] ${this.getTranslation('log_warning_enter_url', 'Please enter a YouTube URL.')}`);
            return;
        }

        const selectedIds = [];
        document.querySelectorAll('.track-cb').forEach(cb => {
            if (cb.checked) {
                const tid = cb.getAttribute('data-id');
                if (tid) selectedIds.push(tid);
            }
        });

        if (loadedTracks.length > 1 && selectedIds.length === 0) {
            this.addLog(`[Warning] ${this.getTranslation('log_warning_no_tracks', 'No playlist tracks are selected for download.')}`);
            return;
        }

        const btnDownload = document.getElementById('btn-download');
        const btnCancel = document.getElementById('btn-cancel');
        if (btnDownload) btnDownload.disabled = true;
        if (btnCancel) btnCancel.disabled = false;

        document.getElementById('console').innerHTML = '';
        this.setProgress(0);
        this.addLog(this.getTranslation('log_init_job', 'Initializing download job...'));

        const mediaTypeSelect = document.getElementById('media-type-select');
        const mediaType = mediaTypeSelect ? mediaTypeSelect.value : 'audio';

        const formatSelect = document.getElementById(mediaType === 'audio' ? 'format-select' : 'video-format');
        const qualitySelect = document.getElementById(mediaType === 'audio' ? 'quality-select' : 'video-quality');
        const format = formatSelect ? formatSelect.value : 'mp3';
        const quality = qualitySelect ? qualitySelect.value : '192k';

        const subfolderSelect = document.getElementById('subfolder-select');
        const subfolder = subfolderSelect ? subfolderSelect.value : 'none';

        if (Config.isElectron) {
            const dirElement = document.getElementById('dir-path');
            const outputDir = dirElement ? dirElement.textContent.trim() : '';
            let savedConcurrency = 1;
            try {
                savedConcurrency = parseInt(localStorage.getItem('app-concurrency')) || 1;
            } catch (e) {}

            const options = {
                url,
                outputDir,
                mediaType,
                subfolder,
                format,
                quality,
                startIdx: 1,
                endIdx: -1,
                selectedIds,
                concurrency: savedConcurrency
            };
            window.ElectronAPI.startDownload(options);
        } else {
            this.runClientSideDownload(selectedIds);
        }
    },

    /**
     * Cancels the active downloads.
     * Aborts Electron queues or triggers abort triggers on client-side requests.
     */
    cancelDownload() {
        this.addLog(this.getTranslation('log_cancel_request', 'Sending cancel request...'));
        if (Config.isElectron) {
            window.electronAPI.cancelDownload();
        } else {
            if (activeAbortController) {
                activeAbortController.abort();
                activeAbortController = null;
            }
        }
    },

    /**
     * Executes the download process client-side for Web/Android.
     * It maps chunks to progress percentages, downloads, and caches tracks into IndexedDB.
     * 
     * @param {Array<string>} selectedIds - Target video IDs to download.
     */
    async runClientSideDownload(selectedIds) {
        activeAbortController = new AbortController();
        const signal = activeAbortController.signal;

        let queue = [];
        if (loadedTracks.length === 1) {
            queue = [...loadedTracks];
        } else {
            queue = loadedTracks.filter(t => selectedIds.includes(t.id));
        }

        const totalTracks = queue.length;
        let completed = 0;

        for (let i = 0; i < totalTracks; i++) {
            if (signal.aborted) break;

            const track = queue[i];
            this.setStatus(`Downloading track ${i + 1} of ${totalTracks}`, track.title);
            this.addLog(`Downloading [${i + 1}/${totalTracks}]: ${track.title}`);

            try {
                const resolution = await MusicAPI.playTrack(track.id, track.channel, track.title);
                if (signal.aborted) break;

                if (resolution && resolution.status === 'ready' && resolution.url) {
                    const response = await fetch(resolution.url, { signal });
                    const reader = response.body.getReader();
                    const contentLength = +response.headers.get('Content-Length') || 10000000; // fallback if missing
                    let receivedLength = 0;
                    const chunks = [];

                    while (true) {
                        const { done, value } = await reader.read();
                        if (done) break;
                        if (signal.aborted) throw new Error('Download aborted');

                        chunks.push(value);
                        receivedLength += value.length;
                        const percent = Math.round((receivedLength / contentLength) * 100);
                        this.setProgress(Math.round(((completed + (receivedLength / contentLength)) / totalTracks) * 100));
                    }

                    const blob = new Blob(chunks);
                    const db = await IndexedDBStorage.initDb();
                    const cleanArtist = (track.channel || 'Unknown Artist').replace(/[/\\?%*:|"<>]/g, '').trim();
                    const cleanTitle = (track.title || 'Unknown Title').replace(/[/\\?%*:|"<>]/g, '').trim();
                    const filename = `${cleanArtist} - ${cleanTitle}.m4a`;

                    const trackData = {
                        filename,
                        videoId: track.id,
                        artist: cleanArtist,
                        title: cleanTitle,
                        blob,
                        timestamp: Date.now()
                    };

                    await new Promise((resolve, reject) => {
                        const transaction = db.transaction(IndexedDBStorage.storeName, 'readwrite');
                        const store = transaction.objectStore(IndexedDBStorage.storeName);
                        const putRequest = store.put(trackData);
                        putRequest.onsuccess = () => resolve();
                        putRequest.onerror = (e) => reject(e.target.error);
                    });

                    completed++;
                    this.addLog(`[Success] Saved to Library: ${track.title}`);
                } else {
                    throw new Error(resolution.message || 'Stream resolution failed');
                }
            } catch (err) {
                if (signal.aborted) {
                    this.addLog('[Warning] Download cancelled by user.');
                    break;
                }
                this.addLog(`[Warning] Track failed: ${track.title}. Reason: ${err.message}`);
            }
        }

        activeAbortController = null;
        const btnDownload = document.getElementById('btn-download');
        const btnCancel = document.getElementById('btn-cancel');
        if (btnDownload) btnDownload.disabled = false;
        if (btnCancel) btnCancel.disabled = true;

        if (signal.aborted) {
            this.setStatus('Failed', '');
            this.addLog('[Warning] Job was cancelled.');
        } else if (completed === 0 && totalTracks > 0) {
            this.setStatus('Failed', '');
            this.setProgress(0);
            this.addLog('[Error] All selected tracks failed to download.');
        } else {
            this.setProgress(100);
            this.setStatus('Completed', 'Finished!');
            this.addLog('[Success] All tasks finished successfully!');
        }
    },

    /**
     * Cleans up the active IPC process observers and bindings when navigating away.
     */
    cleanup() {
        if (activeAbortController) {
            activeAbortController.abort();
            activeAbortController = null;
        }
        electronUnsubscribeCallbacks.forEach(unsubscribe => unsubscribe());
        electronUnsubscribeCallbacks = [];
    },

    /**
     * Initializes the downloader page layout triggers, inputs, and environment configurations.
     * 
     * @param {Object} params - Routing parameters.
     */
    async init(params) {
        this.cleanup();

        const urlInput = document.getElementById('url-input');
        const btnBrowse = document.getElementById('btn-browse');
        const advancedToggle = document.getElementById('advanced-toggle');
        const btnDownload = document.getElementById('btn-download');
        const btnCancel = document.getElementById('btn-cancel');
        const btnLoadInfo = document.getElementById('btn-load-info');
        const btnSelectAll = document.getElementById('btn-select-all');
        const btnDeselectAll = document.getElementById('btn-deselect-all');
        const mediaTypeSelect = document.getElementById('media-type-select');

        // Dynamic environment rendering
        if (Config.isElectron) {
            document.getElementById('save-dir-row').style.display = 'block';
            document.getElementById('storage-info-row').style.display = 'none';
            document.getElementById('subfolder-organization-group').style.display = 'block';
            
            // Subscriptions to Electron IPC
            electronUnsubscribeCallbacks.push(
                window.ElectronAPI.onDownloaderLog(msg => this.addLog(msg)),
                window.ElectronAPI.onDownloaderProgress(percent => this.setProgress(percent)),
                window.ElectronAPI.onDownloaderStatus(data => this.setStatus(data.status, data.track)),
                window.ElectronAPI.onDownloaderComplete(data => {
                    const btnDownloadEl = document.getElementById('btn-download');
                    const btnCancelEl = document.getElementById('btn-cancel');
                    if (btnDownloadEl) btnDownloadEl.disabled = false;
                    if (btnCancelEl) btnCancelEl.disabled = true;

                    if (data.success) {
                        this.setProgress(100);
                        this.setStatus('Completed', 'Finished!');
                        this.addLog(`[Success] ${this.getTranslation('log_success_finished', 'All tasks finished successfully!')}`);
                    } else {
                        this.setStatus('Failed', '');
                        if (data.errorMsg) {
                            this.addLog(`[Error] ${data.errorMsg}`);
                        } else {
                            this.addLog(`[Warning] ${this.getTranslation('log_warning_cancelled', 'Job was cancelled.')}`);
                        }
                    }
                })
            );

            await this.initOutputDirectory();
        } else {
            // Android APK / Web fallback storage notice
            document.getElementById('save-dir-row').style.display = 'none';
            document.getElementById('storage-info-row').style.display = 'block';
            document.getElementById('subfolder-organization-group').style.display = 'none';
        }

        // Checklist selectors
        if (btnSelectAll) btnSelectAll.onclick = () => this.toggleSelectAll(true);
        if (btnDeselectAll) btnDeselectAll.onclick = () => this.toggleSelectAll(false);

        // URL load info
        if (btnLoadInfo) btnLoadInfo.onclick = () => this.loadMetadata();

        // Advanced panel toggle
        if (advancedToggle) {
            advancedToggle.onclick = () => {
                const panel = document.getElementById('advanced-panel');
                if (panel) {
                    panel.classList.toggle('visible');
                }
            };
        }

        // Browse directories
        if (btnBrowse) {
            btnBrowse.onclick = () => this.browseDirectory();
        }

        // Start / Cancel download triggers
        if (btnDownload) btnDownload.onclick = () => this.startDownload();
        if (btnCancel) btnCancel.onclick = () => this.cancelDownload();

        // Media Type selection toggles
        if (mediaTypeSelect) {
            const toggleOptions = () => {
                if (mediaTypeSelect.value === 'audio') {
                    document.getElementById('audio-options').style.display = 'contents';
                    document.getElementById('video-options').style.display = 'none';
                } else {
                    document.getElementById('audio-options').style.display = 'none';
                    document.getElementById('video-options').style.display = 'contents';
                }
            };
            mediaTypeSelect.onchange = toggleOptions;
            toggleOptions();
        }

        // Debounce automatic metadata fetch
        let autoLoadTimer = null;
        if (urlInput) {
            urlInput.oninput = () => {
                clearTimeout(autoLoadTimer);
                autoLoadTimer = setTimeout(() => this.loadMetadata(), 600);
            };
            urlInput.onpaste = () => {
                clearTimeout(autoLoadTimer);
                autoLoadTimer = setTimeout(() => this.loadMetadata(), 100);
            };
        }

        // Prepopulate URL from router parameters if provided
        if (params && params.url) {
            if (urlInput) {
                urlInput.value = params.url;
                this.loadMetadata();
            }
        }
    }
};
