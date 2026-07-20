/**
 * IVIDS Adaptive Loader Helper
 * Automatically injects the SVG structure into elements with class 'ivids-loader'
 */
export const Loader = {
    /**
     * Injects the loader SVG into any element with the 'ivids-loader' class
     * that hasn't been initialized yet.
     */
    init() {
        const loaders = document.querySelectorAll('.ivids-loader:not(.initialized)');
        loaders.forEach(loader => {
            loader.innerHTML = `
                <svg class="spinner" viewBox="0 0 50 50">
                    <circle cx="25" cy="25" r="20"></circle>
                </svg>
            `;
            loader.classList.add('initialized');
        });
    },

    /**
     * Creates a loader element programmatically
     * @param {string} size - 'small', 'medium', 'large'
     * @returns {HTMLElement}
     */
    create(size = 'medium') {
        const div = document.createElement('div');
        div.className = `ivids-loader ${size}`;
        div.innerHTML = `
            <svg class="spinner" viewBox="0 0 50 50">
                <circle cx="25" cy="25" r="20"></circle>
            </svg>
        `;
        div.classList.add('initialized');
        return div;
    }
};

// Auto-init on DOM load
window.addEventListener('DOMContentLoaded', () => Loader.init());
window.Loader = Loader;
