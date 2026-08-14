import { BasePageController } from './BasePageController.js';
import { SettingsManager } from '../settings-manager.js';
import { LanguageManager } from '../language-manager.js';


/**
 * SettingsPageController manages application settings, theme toggles, and i18n language options.
 */
export class SettingsPageController extends BasePageController {
    /**
     * Renders settings view.
     * @param {Object} params
     */
    async render(params = {}) {
        this.resetAbortController();
        this.bindEvents();
        this.updateLayoutModeUIState(SettingsManager.getLayoutMode());
        this.updateModeUIState(SettingsManager.getUpdateMode());
        this.updateAboutVersion();
    }

    /** Binds settings UI interaction handlers */
    bindEvents() {
        SettingsManager.bindScaleUI();
        LanguageManager.bindLanguageUI();
        this.bindLayoutModeUI();
        this.bindUpdateModeUI();
    }

    /** Updates the displayed application version in the About section */
    updateAboutVersion() {
        const verDisplay = document.getElementById('about-version-display');
        if (verDisplay) {
            const ver = (window.Updater && window.Updater.currentVersion) ? window.Updater.currentVersion : '0.2.5';
            verDisplay.textContent = ver;
        }
    }

    /** Binds Layout Mode modal interactions */
    bindLayoutModeUI() {
        const layoutModeBtn = document.getElementById('layout-mode-btn');
        const layoutModeModalOverlay = document.getElementById('layout-mode-modal-overlay');
        const layoutModeModalClose = document.getElementById('layout-mode-modal-close');
        const layoutModeOptions = document.querySelectorAll('.layout-mode-option-btn');

        if (layoutModeBtn && layoutModeModalOverlay) {
            layoutModeBtn.onclick = () => {
                layoutModeModalOverlay.style.display = 'flex';
            };
        }

        if (layoutModeModalClose && layoutModeModalOverlay) {
            layoutModeModalClose.onclick = () => {
                layoutModeModalOverlay.style.display = 'none';
            };
        }

        layoutModeOptions.forEach(btn => {
            btn.onclick = () => {
                const layout = btn.getAttribute('data-layout');
                const setLayout = SettingsManager.setLayoutMode(layout);
                this.updateLayoutModeUIState(setLayout);
                if (layoutModeModalOverlay) layoutModeModalOverlay.style.display = 'none';
            };
        });
    }

    /** Updates UI state based on active layout mode */
    updateLayoutModeUIState(mode) {
        const display = document.getElementById('current-layout-mode-display');
        const desc = document.getElementById('layout-mode-desc');
        const options = document.querySelectorAll('.layout-mode-option-btn');

        const t = (key, fallback) => (window.LanguageManager && typeof window.LanguageManager.t === 'function') 
            ? window.LanguageManager.t(key) 
            : fallback;

        const modeLabels = {
            auto: t('layout_auto', 'Auto (Responsive)'),
            mobile: t('layout_mobile', 'Mobile (Phone)'),
            desktop: t('layout_desktop', 'Desktop Mode'),
            tv: t('layout_tv', 'TV Mode')
        };

        const modeDescs = {
            auto: t('layout_auto_desc', 'Automatically adjusts to screen size and orientation.'),
            mobile: t('layout_mobile_desc', 'Forces bottom navigation dock and floating mini player.'),
            desktop: t('layout_desktop_desc', 'Forces full sidebar navigation and wide desktop player.'),
            tv: t('layout_tv_desc', 'Optimized for TV with D-pad spatial navigation focus rings.')
        };

        if (display) {
            display.textContent = modeLabels[mode] || modeLabels.auto;
        }
        if (desc) {
            desc.textContent = modeDescs[mode] || modeDescs.auto;
        }

        options.forEach(opt => {
            if (opt.getAttribute('data-layout') === mode) {
                opt.style.borderColor = 'var(--primary-color)';
                opt.style.background = 'rgba(var(--primary-rgb), 0.15)';
            } else {
                opt.style.borderColor = '';
                opt.style.background = '';
            }
        });
    }


    /** Binds Update Mode and Developer Versions modal interactions */
    bindUpdateModeUI() {
        const updateModeBtn = document.getElementById('update-mode-btn');
        const updateModeModalOverlay = document.getElementById('update-mode-modal-overlay');
        const updateModeModalClose = document.getElementById('update-mode-modal-close');
        const updateModeOptions = document.querySelectorAll('.update-mode-option-btn');

        const showVersionsBtn = document.getElementById('show-versions-btn');
        const versionsModalOverlay = document.getElementById('versions-modal-overlay');
        const versionsModalClose = document.getElementById('versions-modal-close');

        if (updateModeBtn && updateModeModalOverlay) {
            updateModeBtn.onclick = () => {
                updateModeModalOverlay.style.display = 'flex';
            };
        }

        if (updateModeModalClose && updateModeModalOverlay) {
            updateModeModalClose.onclick = () => {
                updateModeModalOverlay.style.display = 'none';
            };
        }

        updateModeOptions.forEach(btn => {
            btn.onclick = () => {
                const mode = btn.getAttribute('data-mode');
                const setMode = SettingsManager.setUpdateMode(mode);
                this.updateModeUIState(setMode);
                if (updateModeModalOverlay) updateModeModalOverlay.style.display = 'none';
            };
        });

        if (showVersionsBtn && versionsModalOverlay) {
            showVersionsBtn.onclick = async () => {
                if (window.Updater && typeof window.Updater.openVersionsDialog === 'function') {
                    await window.Updater.openVersionsDialog();
                } else {
                    versionsModalOverlay.style.display = 'flex';
                }
            };
        }

        if (versionsModalClose && versionsModalOverlay) {
            versionsModalClose.onclick = () => {
                versionsModalOverlay.style.display = 'none';
            };
        }
    }

    /** Updates UI state based on active update mode */
    updateModeUIState(mode) {
        const display = document.getElementById('current-update-mode-display');
        const desc = document.getElementById('update-mode-desc');
        const actionItem = document.getElementById('update-action-item');
        const actionLabel = document.getElementById('action-item-label');
        const actionDesc = document.getElementById('action-item-desc');
        const checkUpdatesBtn = document.getElementById('check-updates-btn');
        const showVersionsBtn = document.getElementById('show-versions-btn');
        const modeOptions = document.querySelectorAll('.update-mode-option-btn');

        if (display) display.textContent = mode;

        // Highlight selected option in modal
        modeOptions.forEach(opt => {
            if (opt.getAttribute('data-mode') === mode) {
                opt.style.borderColor = 'var(--primary-color)';
                opt.style.background = 'rgba(var(--primary-rgb), 0.15)';
            } else {
                opt.style.borderColor = '';
                opt.style.background = '';
            }
        });

        if (mode === 'Off') {
            if (desc) desc.textContent = 'Update checking is disabled.';
            if (actionItem) actionItem.style.display = 'none';
        } else if (mode === 'Auto') {
            if (desc) desc.textContent = 'Automatically check every 24 hours. You can also check manually.';
            if (actionItem) actionItem.style.display = 'flex';
            if (actionLabel) actionLabel.textContent = 'Software Updates';
            if (actionDesc) actionDesc.textContent = 'Check for official application updates.';
            if (checkUpdatesBtn) checkUpdatesBtn.style.display = 'inline-flex';
            if (showVersionsBtn) showVersionsBtn.style.display = 'none';
        } else if (mode === 'Manual') {
            if (desc) desc.textContent = 'No automatic background checking. Check for updates manually.';
            if (actionItem) actionItem.style.display = 'flex';
            if (actionLabel) actionLabel.textContent = 'Software Updates';
            if (actionDesc) actionDesc.textContent = 'Check for official application updates.';
            if (checkUpdatesBtn) checkUpdatesBtn.style.display = 'inline-flex';
            if (showVersionsBtn) showVersionsBtn.style.display = 'none';
        } else if (mode === 'Developer Mode') {
            if (desc) desc.textContent = 'Developer Mode active. Select custom release versions or Main branch builds.';
            if (actionItem) actionItem.style.display = 'flex';
            if (actionLabel) actionLabel.textContent = 'Developer Version Selection';
            if (actionDesc) actionDesc.textContent = 'Choose a specific release version or Main branch file.';
            if (checkUpdatesBtn) checkUpdatesBtn.style.display = 'none'; // NO check for updates button in Dev Mode
            if (showVersionsBtn) showVersionsBtn.style.display = 'inline-flex'; // ONLY Show Versions button
        }
    }
}
