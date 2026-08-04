/**
 * ToastComponent renders floating notification toasts in the UI.
 */
export class ToastComponent {
    #container = null;

    #getContainer() {
        if (!this.#container) {
            this.#container = document.getElementById('toast-container');
            if (!this.#container) {
                this.#container = document.createElement('div');
                this.#container.id = 'toast-container';
                document.body.appendChild(this.#container);
            }
        }
        return this.#container;
    }

    /**
     * Displays a toast notification.
     * @param {string} message - Text message.
     * @param {string} type - 'info', 'success', 'error', 'warning'
     * @param {number} duration - Duration in milliseconds.
     */
    show(message, type = 'info', duration = 3000) {
        const container = this.#getContainer();
        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;
        toast.textContent = message;

        container.appendChild(toast);

        setTimeout(() => {
            toast.classList.add('hide');
            toast.addEventListener('transitionend', () => toast.remove());
        }, duration);
    }
}

/** Toast singleton instance */
export const Toast = new ToastComponent();
