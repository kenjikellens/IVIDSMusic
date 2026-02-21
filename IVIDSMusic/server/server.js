const express = require('express');
const cors = require('cors');
const { spawn } = require('child_process');
const path = require('path');
const fs = require('fs');
const https = require('https');

const app = express();
const PORT = 3000;

// Config
const BIN_DIR = path.join(__dirname, 'bin');
const DOWNLOADS_DIR = path.join(process.env.USERPROFILE || process.env.HOME, '.ivids_music_downloads');
const YTDLP_PATH = path.join(BIN_DIR, 'yt-dlp.exe');
const FFMPEG_PATH = path.join(BIN_DIR, 'ffmpeg.exe');

// Ensure downloads directory exists
if (!fs.existsSync(DOWNLOADS_DIR)) {
    fs.mkdirSync(DOWNLOADS_DIR, { recursive: true });
}

app.use(cors());
app.use(express.json());

// Serve downloaded files statically
app.use('/tracks', express.static(DOWNLOADS_DIR));

/**
 * Proxy endpoint to bypass CORS for music APIs (like Deezer)
 */
app.get('/proxy', (req, res) => {
    const targetUrl = req.query.url;
    if (!targetUrl) return res.status(400).json({ error: 'URL required' });

    https.get(targetUrl, (proxyRes) => {
        let data = '';
        proxyRes.on('data', (chunk) => data += chunk);
        proxyRes.on('end', () => {
            try {
                res.setHeader('Content-Type', 'application/json');
                res.send(data);
            } catch (e) {
                res.status(500).json({ error: 'Failed to parse JSON' });
            }
        });
    }).on('error', (err) => {
        console.error('Proxy Error:', err);
        res.status(500).json({ error: 'Proxy request failed' });
    });
});

/**
 * Sanitize filename for Windows
 */
function sanitizeFilename(name) {
    return name.replace(/[\\/:*?"<>|]/g, '').trim();
}

/**
 * Endpoint: /play?videoId=...&artist=...&title=...
 * Finds or downloads the MP3 using proper filename
 */
app.get('/play', async (req, res) => {
    const { videoId, artist, title } = req.query;

    if (!videoId || !artist || !title) {
        return res.status(400).json({ error: 'Missing parameters (videoId, artist, title required)' });
    }

    const cleanArtist = sanitizeFilename(artist);
    const cleanTitle = sanitizeFilename(title);
    const fileName = `${cleanArtist} - ${cleanTitle}.mp3`;
    const filePath = path.join(DOWNLOADS_DIR, fileName);

    // 1. Check if already downloaded
    if (fs.existsSync(filePath)) {
        console.log(`[Cache Hit] Serving: ${fileName}`);
        return res.json({
            status: 'ready',
            url: `http://localhost:${PORT}/tracks/${encodeURIComponent(fileName)}`
        });
    }

    // 2. Not downloaded - Start Download
    console.log(`[Download] Starting download for: ${fileName}`);

    const args = [
        '--extract-audio',
        '--audio-format', 'mp3',
        '--ffmpeg-location', FFMPEG_PATH,
        '--output', path.join(DOWNLOADS_DIR, `${cleanArtist} - ${cleanTitle}.%(ext)s`),
        `https://www.youtube.com/watch?v=${videoId}`
    ];

    const ytdlp = spawn(YTDLP_PATH, args);

    ytdlp.on('close', (code) => {
        if (code === 0) {
            console.log(`[Success] Downloaded: ${fileName}`);
            res.json({
                status: 'ready',
                url: `http://localhost:${PORT}/tracks/${encodeURIComponent(fileName)}`
            });
        } else {
            console.error(`yt-dlp exited with code ${code}`);
            res.status(500).json({ error: 'Download failed' });
        }
    });
});

app.listen(PORT, '0.0.0.0', () => {
    console.log(`========================================`);
    console.log(`IVIDS Music Backend Running`);
    console.log(`Port: ${PORT}`);
    console.log(`Listening on: 0.0.0.0:${PORT}`);
    console.log(`Downloads: ${DOWNLOADS_DIR}`);
    console.log(`Format: Artist - Title.mp3`);
    console.log(`========================================`);
});
