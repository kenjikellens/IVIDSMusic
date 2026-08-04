/**
 * IVIDS Adaptive Loader Helper
 * Automatically injects the SVG structure into elements with class 'ivids-loader'
 */
export const Loader = {
    /**
     * Injects the animated SVG loader image into elements with the 'ivids-loader' class
     */
    init() {
        const loaders = document.querySelectorAll('.ivids-loader:not(.initialized)');
        loaders.forEach(loader => {
            loader.innerHTML = `<img src="svg/loader.svg" alt="Loading" class="ivids-loader-img">`;
            loader.classList.add('initialized');
        });
    },

    /**
     * Creates a loader DOM container programmatically with svg/loader.svg
     * @param {string} size - 'small', 'medium', 'large'
     * @returns {HTMLElement} The created loader element
     */
    create(size = 'medium') {
        const div = document.createElement('div');
        div.className = `ivids-loader ${size}`;
        div.innerHTML = `<img src="svg/loader.svg" alt="Loading" class="ivids-loader-img">`;
        div.classList.add('initialized');
        return div;
    }
};

// Auto-init on DOM load
window.addEventListener('DOMContentLoaded', () => Loader.init());
window.Loader = Loader;
