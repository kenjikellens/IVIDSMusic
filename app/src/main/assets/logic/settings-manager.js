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
        localStorage.setItem('iv_ui_scale', value);
        this.applyScale(value);
        this.updateScaleButtons(value);
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
     * Update active state for scale buttons in the settings UI
     */
    updateScaleButtons(value) {
        try {
            const buttons = document.querySelectorAll('.scale-btn[data-scale]');
            buttons.forEach(btn => {
                const btnVal = parseFloat(btn.getAttribute('data-scale'));
                if (Math.abs(btnVal - value) < 0.0001) {
                    btn.classList.add('active');
                } else {
                    btn.classList.remove('active');
                }
            });
        } catch (e) {
            // ignore if DOM not available
        }
    },

    /**
     * Bind click handlers for scale buttons in settings UI
     */
    bindScaleUI() {
        try {
            const container = document.getElementById('scale-options-container');
            if (!container) return;
            container.addEventListener('click', (ev) => {
                const btn = ev.target.closest && ev.target.closest('.scale-btn');
                if (!btn) return;
                const data = btn.getAttribute('data-scale');
                if (!data) return;
                const val = parseFloat(data);
                if (!isNaN(val)) this.setScale(val);
            });
            // set initial active state
            this.updateScaleButtons(this.getScale());
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
