/**
 * EventEmitter provides a lightweight, decoupled pub/sub event mechanism.
 */
export class EventEmitter {
    #events = new Map();

    /**
     * Registers a listener callback for a specific event.
     * @param {string} event - The event name to subscribe to.
     * @param {Function} listener - The callback function.
     */
    on(event, listener) {
        if (typeof listener !== 'function') return;
        if (!this.#events.has(event)) {
            this.#events.set(event, new Set());
        }
        this.#events.get(event).add(listener);
    }

    /**
     * Removes a registered listener for an event.
     * @param {string} event - The event name.
     * @param {Function} listener - The callback function to unregister.
     */
    off(event, listener) {
        if (!this.#events.has(event)) return;
        this.#events.get(event).delete(listener);
    }

    /**
     * Registers a listener that triggers at most once.
     * @param {string} event - The event name.
     * @param {Function} listener - The callback function.
     */
    once(event, listener) {
        const wrapper = (data) => {
            this.off(event, wrapper);
            listener(data);
        };
        this.on(event, wrapper);
    }

    /**
     * Emits an event with optional data payload to all registered listeners.
     * @param {string} event - The event name.
     * @param {*} [data] - Data payload passed to listeners.
     */
    emit(event, data) {
        if (!this.#events.has(event)) return;
        this.#events.get(event).forEach(listener => {
            try {
                listener(data);
            } catch (err) {
                console.error(`[EventEmitter] Error in listener for event '${event}':`, err);
            }
        });
    }

    /**
     * Removes all listeners for a given event or clears all events if omitted.
     * @param {string} [event] - Optional target event name.
     */
    removeAllListeners(event) {
        if (event) {
            this.#events.delete(event);
        } else {
            this.#events.clear();
        }
    }
}
