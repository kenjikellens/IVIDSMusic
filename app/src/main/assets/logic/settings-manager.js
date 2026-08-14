import { LanguageManager } from './language-manager.js';

/**
 * IVIDS Music - Settings Manager
 * Handles persistence and application of user settings like UI scale.
 */
export const SettingsManager = {
    defaults: {
        scale: 1.0
    },

    /**
     * Get the current scale factor from localStorage or default
     */
    getScale() {
        const saved = localStorage.getItem('iv_ui_scale');
        return saved ? parseFloat(saved) : this.defaults.scale;
    },

    /**
     * Sets and saves the scale factor
     * @param {number} value - Scale factor (e.g., 0.75, 1.1)
     */
    setScale(value) {
        // Clamp value between 0.5 (50%) and 2.0 (200%)
        let clampedValue = Math.max(0.5, Math.min(2.0, value));
        // Round to 1 decimal place to prevent floating point issues (e.g. 1.1000000000000001)
        clampedValue = Math.round(clampedValue * 10) / 10;

        localStorage.setItem('iv_ui_scale', clampedValue);
        this.applyScale(clampedValue);
        this.updateScaleDisplay(clampedValue);
    },

    /**
     * Applies the scale factor to the document root
     * @param {number} value 
     */
    applyScale(value) {
        // Apply user scale multiplier and update --ui-user-scale & --ui-scale
        document.documentElement.style.setProperty('--ui-user-scale', String(value));
        
        try {
            const baseScaleStr = getComputedStyle(document.documentElement).getPropertyValue('--ui-base-scale');
            const baseScale = parseFloat(baseScaleStr) || 1;
            document.documentElement.style.setProperty('--ui-scale', String(value * baseScale));
        } catch (e) {}

        // Dispatch event for components that might need manual adjustment
        window.dispatchEvent(new CustomEvent('iv-scale-changed', { detail: { userScale: value } }));
    },

    /**
     * Update the scale value display in the settings UI
     */
    updateScaleDisplay(value) {
        try {
            const display = document.getElementById('current-scale-display');
            if (display) {
                display.textContent = `${Math.round(value * 100)}%`;
            }
        } catch (e) {
            // ignore if DOM not available
        }
    },

    /**
     * Bind click handlers for the scale stepper in settings UI
     */
    bindScaleUI() {
        try {
            if (!this._scaleListenerBound) {
                document.body.addEventListener('click', (ev) => {
                    const target = ev.target;
                    if (!target || !target.closest) return;

                    const decBtn = target.closest('#scale-decrease');
                    const incBtn = target.closest('#scale-increase');

                    if (decBtn) {
                        this.setScale(this.getScale() - 0.1);
                    } else if (incBtn) {
                        this.setScale(this.getScale() + 0.1);
                    }
                });
                this._scaleListenerBound = true;
            }

            // set initial display if possible
            this.updateScaleDisplay(this.getScale());
        } catch (e) {
            // ignore if DOM not available
        }
    },

    /**
     * Get active layout mode ('auto' | 'mobile' | 'desktop' | 'tv')
     */
    getLayoutMode() {
        const saved = localStorage.getItem('iv_layout_mode');
        return saved || 'auto';
    },

    /**
     * Set and persist active layout mode
     * @param {string} mode 
     */
    setLayoutMode(mode) {
        const validModes = ['auto', 'mobile', 'desktop', 'tv'];
        const modeToSet = validModes.includes(mode) ? mode : 'auto';
        localStorage.setItem('iv_layout_mode', modeToSet);
        this.applyLayoutMode(modeToSet);
        window.dispatchEvent(new CustomEvent('iv-layout-mode-changed', { detail: { layoutMode: modeToSet } }));
        return modeToSet;
    },

    /**
     * Applies layout mode classes to the document body
     * @param {string} mode 
     */
    applyLayoutMode(mode) {
        if (typeof document === 'undefined' || !document.body) return;

        document.body.classList.remove('layout-mobile', 'is-mobile-layout', 'layout-desktop', 'layout-tv', 'tv-mode');

        if (mode === 'mobile') {
            document.body.classList.add('layout-mobile', 'is-mobile-layout');
        } else if (mode === 'desktop') {
            document.body.classList.add('layout-desktop');
        } else if (mode === 'tv') {
            document.body.classList.add('layout-tv', 'tv-mode');
        } else {
            // Auto mode: sync is-mobile-layout dynamically
            const mql = window.matchMedia && window.matchMedia('(orientation: portrait), (max-width: 768px)');
            if (mql && mql.matches) {
                document.body.classList.add('is-mobile-layout');
            }
        }
    },

    /**
     * Initialize settings on app load
     */
    async init() {
        const scale = this.getScale();
        this.applyScale(scale);

        const layoutMode = this.getLayoutMode();
        this.applyLayoutMode(layoutMode);

        // Listen for screen orientation / resize in auto mode
        if (typeof window !== 'undefined' && window.matchMedia) {
            const mql = window.matchMedia('(orientation: portrait), (max-width: 768px)');
            mql.addEventListener('change', () => {
                if (this.getLayoutMode() === 'auto') {
                    this.applyLayoutMode('auto');
                }
            });
        }

        // Initialize LanguageManager
        await LanguageManager.init();

        // If DOM ready, bind UI controls
        if (typeof document !== 'undefined') {
            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', () => {
                    this.bindScaleUI();
                    LanguageManager.bindLanguageUI();
                });
            } else {
                this.bindScaleUI();
                LanguageManager.bindLanguageUI();
            }
        }
        console.log(`[SettingsManager] UI Scale initialized to: ${scale}, Layout mode: ${layoutMode}`);
    },

    /**
     * Get active update mode ('Off' | 'Auto' | 'Manual' | 'Developer Mode')
     */
    getUpdateMode() {
        const saved = localStorage.getItem('iv_update_mode');
        return saved || 'Auto';
    },

    /**
     * Set and persist active update mode
     * @param {string} mode 
     */
    setUpdateMode(mode) {
        const validModes = ['Off', 'Auto', 'Manual', 'Developer Mode'];
        const modeToSet = validModes.includes(mode) ? mode : 'Auto';
        localStorage.setItem('iv_update_mode', modeToSet);
        window.dispatchEvent(new CustomEvent('iv-update-mode-changed', { detail: { updateMode: modeToSet } }));
        return modeToSet;
    }
};

// Auto-init if in browser context
if (typeof window !== 'undefined') {
    window.SettingsManager = SettingsManager;
}
