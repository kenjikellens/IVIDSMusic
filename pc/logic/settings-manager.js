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
        // Apply as user multiplier; base scale is controlled by media queries
        document.documentElement.style.setProperty('--ui-user-scale', String(value));
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

                    if (target.id === 'scale-decrease') {
                        this.setScale(this.getScale() - 0.1);
                    } else if (target.id === 'scale-increase') {
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
     * Initialize settings on app load
     */
    async init() {
        const scale = this.getScale();
        this.applyScale(scale);

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
        console.log(`[SettingsManager] UI Scale initialized to: ${scale}`);
    }
};

// Auto-init if in browser context
if (typeof window !== 'undefined') {
    window.SettingsManager = SettingsManager;
}
