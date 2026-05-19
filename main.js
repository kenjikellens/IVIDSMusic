const { app, BrowserWindow, ipcMain, protocol, net } = require('electron');
const path = require('path');
const fs = require('fs');
const { exec } = require('child_process');
const { pathToFileURL } = require('url');

// Determine local directories for cached/downloaded files inside User Data
const savedDir = path.join(app.getPath('userData'), 'saved');
if (!fs.existsSync(savedDir)) {
    fs.mkdirSync(savedDir, { recursive: true });
}

/**
 * Custom protocol handler for secure local media loading.
 * Registers the 'saved-media' protocol to stream saved tracks from AppData.
 * Uses net.fetch to natively support HTTP byte-range seeking requests in Chromium.
 */
function registerMediaProtocol() {
    protocol.handle('saved-media', (request) => {
        const decodedPath = decodeURIComponent(request.url.replace('saved-media://', ''));
        const filePath = path.join(savedDir, decodedPath);
        return net.fetch(pathToFileURL(filePath).toString());
    });
}

/**
 * Main application window instance.
 */
let mainWindow = null;

/**
 * Creates the primary browser window, disables the native menu bar,
 * configures secure webPreferences, and loads the HTML5 GUI.
 */
function createWindow() {
    mainWindow = new BrowserWindow({
        width: 1280,
        height: 720,
        webPreferences: {
            preload: path.join(__dirname, 'preload.js'),
            contextIsolation: true,
            nodeIntegration: false,
            webSecurity: false
        },
        icon: path.join(__dirname, 'app/src/main/ic_launcher-playstore.png')
    });

    // Hide default menu bar for standard app look
    mainWindow.setMenuBarVisibility(false);

    // Load local index.html
    const indexPath = path.join(__dirname, 'app/src/main/assets/gui/index.html');
    mainWindow.loadFile(indexPath);

    mainWindow.on('closed', () => {
        mainWindow = null;
    });
}

// App lifecycle listeners
app.whenReady().then(() => {
    registerMediaProtocol();
    createWindow();

    app.on('activate', () => {
        if (BrowserWindow.getAllWindows().length === 0) createWindow();
    });
});

app.on('window-all-closed', () => {
    if (process.platform !== 'darwin') app.quit();
});

// IPC Handler implementations for Native Desktop Logic

/**
 * Scans the local user directory for saved audio files (.mp3 and .m4a)
 * and returns metadata for rendering on the library screen.
 */
ipcMain.handle('get-saved-tracks', async () => {
    try {
        const files = fs.readdirSync(savedDir);
        const results = [];
        files.forEach(file => {
            if (file.endsWith('.m4a') || file.endsWith('.mp3')) {
                const nameWithoutExt = path.parse(file).name;
                const parts = nameWithoutExt.split(' - ');
                const artist = parts.length > 1 ? parts[0] : 'Unknown Artist';
                const title = parts.length > 1 ? parts[1] : nameWithoutExt;

                results.push({
                    filename: file,
                    artist: artist.trim(),
                    title: title.trim(),
                    url: `saved-media://${encodeURIComponent(file)}`
                });
            }
        });
        return results;
    } catch (e) {
        console.error('Failed to read saved tracks', e);
        return [];
    }
});

/**
 * Resolves a YouTube stream URL using yt-dlp.
 */
ipcMain.handle('play-track', async (event, videoId) => {
    const ytUrl = `https://www.youtube.com/watch?v=${videoId}`;
    return new Promise((resolve) => {
        exec(`yt-dlp -f bestaudio -g "${ytUrl}"`, (err, stdout, stderr) => {
            if (err || !stdout) {
                console.error(`yt-dlp failed to stream: ${stderr || err?.message}`);
                resolve({ status: 'error', message: 'yt-dlp failed to resolve streaming URL' });
            } else {
                resolve({ status: 'ready', url: stdout.trim() });
            }
        });
    });
});

/**
 * Downloads and saves a track locally using yt-dlp.
 */
ipcMain.handle('save-track', async (event, videoId, artist, title) => {
    const cleanArtist = artist.replace(/[/\\?%*:|"<>]/g, '').trim();
    const cleanTitle = title.replace(/[/\\?%*:|"<>]/g, '').trim();
    const filename = `${cleanArtist} - ${cleanTitle}.m4a`;
    const outputPath = path.join(savedDir, filename);
    const ytUrl = `https://www.youtube.com/watch?v=${videoId}`;

    return new Promise((resolve) => {
        exec(`yt-dlp -f bestaudio -o "${outputPath}" "${ytUrl}"`, (err, stdout, stderr) => {
            if (err) {
                console.error(`yt-dlp failed to download: ${stderr || err?.message}`);
                resolve({ status: 'error', message: 'yt-dlp download failed' });
            } else {
                resolve({
                    status: 'saved',
                    message: 'Saved successfully',
                    url: `saved-media://${encodeURIComponent(filename)}`
                });
            }
        });
    });
});

/**
 * Deletes a saved track from disk.
 */
ipcMain.handle('delete-track', async (event, filename) => {
    const filePath = path.join(savedDir, filename);
    return new Promise((resolve) => {
        fs.unlink(filePath, (err) => {
            if (err) {
                console.error(`Failed to delete file: ${err}`);
                resolve({ status: 'error', message: 'Delete failed' });
            } else {
                resolve({ status: 'deleted' });
            }
        });
    });
});
