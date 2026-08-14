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
        this.updateModeUIState(SettingsManager.getUpdateMode());
    }

    /** Binds settings UI interaction handlers */
    bindEvents() {
        SettingsManager.bindScaleUI();
        LanguageManager.bindLanguageUI();
        this.bindUpdateModeUI();
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
