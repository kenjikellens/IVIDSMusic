/**
 * IVIDS Music - Configuration
 * 
 * Dynamically detects the environment.
 * In Native Android Mode, it uses the secure appassets domain intercepted by Kotlin.
 * In Web/Live Server Mode, it falls back to the local Node.js backend on port 3000.
 */
export const Config = {
    // Determine the environment based on the current hostname
    get SERVER_URL() {
        const hostname = window.location.hostname;

        // Native Android Mode (WebView intercepts requests to this domain)
        if (hostname.includes("appassets.androidplatform.net")) {
            return "https://appassets.androidplatform.net/api";
        }

        // Web / Live Server Mode (usually localhost or local IP)
        // Fallback to exactly http://127.0.0.1:3000 to avoid CORS mismatches between localhost/127.0.0.1
        return "http://127.0.0.1:3000";
    }
};
