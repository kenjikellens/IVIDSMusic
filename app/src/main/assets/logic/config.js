/**
 * IVIDS Music - Configuration
 * 
 * Dynamically detects the environment.
 * In Native Android Mode, it uses the secure appassets domain intercepted by Kotlin.
 * In Web/Live Server Mode, it falls back to the local Node.js backend on port 3000.
 */
const isNative = typeof window !== 'undefined' && window.location.hostname.includes("appassets.androidplatform.net");

let serverUrl = "http://127.0.0.1:3000";
if (typeof window !== 'undefined' && (window.location.protocol === 'http:' || window.location.protocol === 'https:')) {
    serverUrl = `${window.location.protocol}//${window.location.hostname}:3000`;
}

export const Config = {
    isNative: isNative,
    SERVER_URL: isNative ? "/api" : serverUrl,

};

// Global access for easier debugging
if (typeof window !== 'undefined') {
    window.Config = Config;
}
