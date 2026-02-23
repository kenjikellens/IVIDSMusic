const http = require('http');
const fs = require('fs');
const path = require('path');
const { exec } = require('child_process');
const url = require('url');

const downloadsDir = path.join(__dirname, 'downloads');
const savedDir = path.join(downloadsDir, 'saved');
if (!fs.existsSync(downloadsDir)) fs.mkdirSync(downloadsDir);
if (!fs.existsSync(savedDir)) fs.mkdirSync(savedDir);

const server = http.createServer(async (req, res) => {
    // Basic CORS for API testing
    res.setHeader('Access-Control-Allow-Origin', '*');

    const parsedUrl = url.parse(req.url, true);
    let pathname = parsedUrl.pathname;

    // 1. Proxy
    if (pathname === '/proxy') {
        const targetUrl = parsedUrl.query.url;
        if (!targetUrl) return res.end(JSON.stringify({ error: "missing url" }));
        try {
            const fetchReq = await fetch(targetUrl, {
                headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)' }
            });
            const data = await fetchReq.text();
            res.writeHead(200, { 'Content-Type': 'application/json' });
            return res.end(data);
        } catch (e) {
            res.writeHead(500);
            return res.end(JSON.stringify({ error: e.message }));
        }
    }

    // 2. Play
    if (pathname === '/play' || pathname === '/api/play') {
        const videoId = parsedUrl.query.videoId;
        if (!videoId) return res.end(JSON.stringify({ status: 'error', message: 'No videoId' }));

        const ytUrl = `https://www.youtube.com/watch?v=${videoId}`;
        console.log(`[Play] Getting URL for ${videoId}...`);

        // Using yt-dlp to get the highest quality audio URL (-f 140 is m4a audio, or bestaudio)
        exec(`yt-dlp -f bestaudio -g "${ytUrl}"`, (err, stdout, stderr) => {
            if (err || !stdout) {
                console.error(`[Play Error]`, stderr || err?.message);
                res.writeHead(500, { 'Content-Type': 'application/json' });
                return res.end(JSON.stringify({ status: 'error', message: 'yt-dlp extraction failed' }));
            }
            res.writeHead(200, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ status: 'ready', url: stdout.trim() }));
        });
        return;
    }

    // 3. Save
    if (pathname === '/save' || pathname === '/api/save') {
        const videoId = parsedUrl.query.videoId;
        const artist = parsedUrl.query.artist || 'Unknown Artist';
        const title = parsedUrl.query.title || 'Unknown Title';

        if (!videoId) return res.end(JSON.stringify({ status: 'error', message: 'No videoId' }));

        const filename = `${artist} - ${title}.m4a`.replace(/[/\\?%*:|"<>]/g, '');
        const outputPath = path.join(savedDir, filename);

        console.log(`[Save] Downloading ${filename}...`);

        // Using yt-dlp to download directly
        const ytUrl = `https://www.youtube.com/watch?v=${videoId}`;
        exec(`yt-dlp -f bestaudio -o "${outputPath}" "${ytUrl}"`, (err, stdout, stderr) => {
            if (err) {
                console.error(`[Save Error]`, stderr || err.message);
                res.writeHead(500, { 'Content-Type': 'application/json' });
                return res.end(JSON.stringify({ status: 'error', message: 'yt-dlp download failed' }));
            }
            res.writeHead(200, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({
                status: 'saved',
                message: 'Saved successfully',
                url: `/saved/${encodeURIComponent(filename)}`
            }));
        });
        return;
    }

    // 4. API Saved list
    if (pathname === '/api/saved') {
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
                        url: `/saved/${encodeURIComponent(file)}`
                    });
                }
            });
            res.writeHead(200, { 'Content-Type': 'application/json' });
            return res.end(JSON.stringify(results));
        } catch (e) {
            res.writeHead(500);
            return res.end(JSON.stringify([]));
        }
    }

    // 5. Serve Static Files
    let filePath = path.join(process.cwd(), pathname === '/' ? 'index.html' : pathname);

    // Serve from "saved" directory dynamically
    if (pathname.startsWith('/saved/')) {
        const decoded = decodeURIComponent(pathname.replace('/saved/', ''));
        filePath = path.join(savedDir, decoded);
    }

    const extname = path.extname(filePath).toLowerCase();
    const mimeTypes = {
        '.js': 'text/javascript',
        '.css': 'text/css',
        '.json': 'application/json',
        '.png': 'image/png',
        '.jpg': 'image/jpg',
        '.svg': 'image/svg+xml',
        '.html': 'text/html',
        '.m4a': 'audio/mp4',
        '.mp3': 'audio/mpeg'
    };

    const contentType = mimeTypes[extname] || 'application/octet-stream';

    fs.readFile(filePath, (error, content) => {
        if (error) {
            res.writeHead(404);
            res.end('Not found: ' + req.url);
        } else {
            res.writeHead(200, { 'Content-Type': contentType });
            res.end(content, 'utf-8');
        }
    });
});

const PORT = 3000;
server.listen(PORT, () => {
    console.log(`Server running at http://localhost:${PORT}/`);
    console.log(`Open http://localhost:${PORT}/ in your web browser to test.`);
});
