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
    },

    /**
     * Applies the scale factor to the document root
     * @param {number} value 
     */
    applyScale(value) {
        document.documentElement.style.setProperty('--ui-scale', value);
        // Dispatch event for components that might need manual adjustment
        window.dispatchEvent(new CustomEvent('iv-scale-changed', { detail: { scale: value } }));
    },

    /**
     * Initialize settings on app load
     */
    init() {
        const scale = this.getScale();
        this.applyScale(scale);
        console.log(`[SettingsManager] UI Scale initialized to: ${scale}`);
    }
};

// Auto-init if in browser context
if (typeof window !== 'undefined') {
    window.SettingsManager = SettingsManager;
}
