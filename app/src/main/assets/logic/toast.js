/**
 * IVIDS Music - Premium Glassmorphic Toast Notification System
 * 
 * Description: Replaces standard blocking browser alerts with a beautiful, mobile-friendly
 *              physical UI popup notification (Toast) at the bottom center of the viewport.
 */

export const Toast = {
    /**
     * Method: show
     * Description: Spawns a physical glassmorphic toast notification popup at the bottom center
     *              of the viewport. Fades in smoothly, stands, and transitions out after a duration.
     * @param {string} message - Text or HTML content to display.
     * @param {number} duration - Milliseconds to show the toast (default 3000ms).
     */
    show(message, duration = 3000) {
        // Remove any existing toast first to prevent overlap
        const existing = document.getElementById('ivids-toast-notification');
        if (existing) existing.remove();

        const toast = document.createElement('div');
        toast.id = 'ivids-toast-notification';
        toast.className = 'glassmorphism ivids-toast';
        
        // Replace newlines with HTML breaks for formatting
        toast.innerHTML = `<span class="toast-message">${message.replace(/\n/g, '<br>')}</span>`;

        document.body.appendChild(toast);

        // Force a layout reflow to allow CSS transitions to trigger
        toast.offsetHeight;

        // Fade in
        toast.classList.add('visible');

        // Setup removal timers
        const hideTimeout = setTimeout(() => {
            toast.classList.remove('visible');
            setTimeout(() => {
                toast.remove();
            }, 300); // Match CSS transition duration
        }, duration);

        // Allow immediate manual dismissal on click
        toast.onclick = () => {
            clearTimeout(hideTimeout);
            toast.classList.remove('visible');
            setTimeout(() => {
                toast.remove();
            }, 300);
        };
    },

    /**
     * Method: init
     * Description: Overrides the standard browser window.alert method with our premium 
     *              glassmorphic Toast notification system.
     */
    init() {
        window.alert = (msg) => this.show(msg);
        window.showToast = (msg, dur) => this.show(msg, dur);
        console.log('[Toast] Global alert() successfully overridden with physical popups.');
    }
};

// Auto-initialize when module is loaded
Toast.init();
