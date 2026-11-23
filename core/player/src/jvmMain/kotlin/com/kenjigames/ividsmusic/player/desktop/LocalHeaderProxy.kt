package com.kenjigames.ividsmusic.player.desktop

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.Closeable
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URI
import java.util.Collections
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Ephemeral loopback HTTP proxy bridging remote audio streams (such as YouTube `googlevideo.com`
 * DASH `audio/mp4`) into OpenJFX `Media`. Normalizes `audio/mp4` to OpenJFX-compatible `audio/x-m4a`,
 * translates rejected `HEAD` probes into 1-byte `GET` range probes, and attaches required headers.
 */
internal class LocalHeaderProxy(
    private val targetUrl: String,
    private val headers: Map<String, String>,
) : Closeable {

    private val closed = AtomicBoolean(false)
    private val token = "ivids-${UUID.randomUUID()}.m4a"
    private val serverSocket = ServerSocket(0, 16, InetAddress.getLoopbackAddress())
    private val activeSockets: MutableSet<Socket> = Collections.synchronizedSet(mutableSetOf())

    /**
     * Loopback `.m4a` URL passed to JavaFX `Media` so OpenJFX `Locator` recognizes the M4A container
     * and routes all `HEAD` and byte-range `GET` fetches through this proxy.
     */
    val localUrl: String = "http://127.0.0.1:${serverSocket.localPort}/$token"

    private val acceptThread = Thread(
        { runAcceptLoop() },
        "ivids-header-proxy-${serverSocket.localPort}",
    ).apply {
        isDaemon = true
        start()
    }

    /**
     * Stops accepting new requests, closes active client sockets, and frees the listening [serverSocket].
     * Idempotent across multiple invocations during track switches and engine teardown.
     */
    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        try {
            serverSocket.close()
        } catch (_: Exception) {
            // Socket already closed or shutting down during teardown.
        }
        val snapshot = synchronized(activeSockets) { activeSockets.toList() }
        for (socket in snapshot) {
            try {
                socket.close()
            } catch (_: Exception) {
                // Client socket already closed during teardown.
            }
        }
        acceptThread.interrupt()
    }

    /**
     * Accepts incoming loopback connections from JavaFX GStreamer until [close] shuts down [serverSocket].
     * Spawns lightweight daemon handlers per socket so concurrent range seeks never block.
     */
    private fun runAcceptLoop() {
        while (!closed.get()) {
            val client = try {
                serverSocket.accept()
            } catch (_: Exception) {
                break
            }
            if (closed.get()) {
                try {
                    client.close()
                } catch (_: Exception) {
                    // Proxy already shutting down.
                }
                break
            }
            activeSockets.add(client)
            Thread({ handleClient(client) }, "ivids-proxy-conn-${client.port}").apply {
                isDaemon = true
                start()
            }
        }
    }

    /**
     * Parses a single HTTP request from [client], proxies it upstream with [headers], and streams the response.
     * Guarantees [client] removal from [activeSockets] and deterministic closure in `finally`.
     */
    private fun handleClient(client: Socket) {
        try {
            client.soTimeout = READ_TIMEOUT_MS
            val input = BufferedInputStream(client.getInputStream())
            val output = BufferedOutputStream(client.getOutputStream())
            val request = readHttpRequest(input) ?: return
            if (request.path != "/$token") {
                writeStatusResponse(output, 404, "Not Found")
                return
            }
            if (request.method != "GET" && request.method != "HEAD") {
                writeStatusResponse(output, 405, "Method Not Allowed")
                return
            }
            forwardToUpstream(request, output)
        } catch (_: Exception) {
            // Client aborted or upstream stream closed; socket closes deterministically in finally.
        } finally {
            activeSockets.remove(client)
            try {
                client.close()
            } catch (_: Exception) {
                // Socket already closed.
            }
        }
    }

    @Volatile
    private var cachedTotalLength: Long = -1L

    /**
     * Streams upstream audio in bounded 512 KB byte-range chunks while normalizing `audio/mp4` to `audio/x-m4a`.
     * Prevents YouTube `googlevideo.com` (`rqh=1`) from rejecting un-ranged or full-file GET/HEAD requests with HTTP 403.
     */
    private fun forwardToUpstream(request: ParsedRequest, output: BufferedOutputStream) {
        if (request.method == "HEAD") {
            handleHeadRequest(output)
            return
        }

        val parsedRange = parseRawRangeSpec(request.range)
        val startByte = parsedRange?.first ?: 0L
        val requestedEndByte = parsedRange?.second
        val knownTotal = cachedTotalLength

        // Determine the end byte of the first upstream chunk (never exceeding 512 KB per request).
        val firstChunkMaxEnd = startByte + UPSTREAM_CHUNK_BYTES - 1L
        val firstChunkEnd = when {
            requestedEndByte != null && requestedEndByte >= startByte -> minOf(requestedEndByte, firstChunkMaxEnd)
            knownTotal > startByte -> minOf(knownTotal - 1L, firstChunkMaxEnd)
            else -> firstChunkMaxEnd
        }

        val firstConn = followRedirects(
            rangeHeader = "bytes=$startByte-$firstChunkEnd",
            ifRangeHeader = request.ifRange,
        ) ?: run {
            writeStatusResponse(output, 502, "Bad Gateway")
            return
        }

        val totalLength: Long
        val finalEndByte: Long
        val normalizedContentType: String
        try {
            val firstStatus = firstConn.responseCode
            normalizedContentType = normalizeJavaFxContentType(firstConn.getHeaderField("Content-Type"))
            if (firstStatus !in 200..299) {
                writeStatusResponse(output, firstStatus, firstConn.responseMessage ?: "Upstream Error")
                return
            }

            val contentRange = firstConn.getHeaderField("Content-Range")
            val totalFromRange = parseTotalFromContentRange(contentRange)
            val firstContentLen = firstConn.getHeaderField("Content-Length")?.trim()?.toLongOrNull() ?: -1L
            totalLength = when {
                totalFromRange != null && totalFromRange > 0L -> totalFromRange.also { cachedTotalLength = it }
                knownTotal > 0L -> knownTotal
                firstContentLen > 0L -> startByte + firstContentLen
                else -> -1L
            }

            if (totalLength <= 0L) {
                // Non-rangeable upstream fallback
                relaySingleConnection(firstConn, normalizedContentType, output, startByte == 0L)
                return
            }

            finalEndByte = when {
                requestedEndByte != null && requestedEndByte >= startByte ->
                    minOf(requestedEndByte, totalLength - 1L)
                else -> totalLength - 1L
            }

            val isPartial = request.range != null
            val responseLength = finalEndByte - startByte + 1L
            val headerBlock = buildString {
                if (isPartial) {
                    append("HTTP/1.1 206 Partial Content\r\n")
                    append("Content-Range: bytes ").append(startByte).append('-').append(finalEndByte)
                        .append('/').append(totalLength).append("\r\n")
                } else {
                    append("HTTP/1.1 200 OK\r\n")
                }
                append("Content-Type: ").append(normalizedContentType).append("\r\n")
                append("Content-Length: ").append(responseLength).append("\r\n")
                append("Accept-Ranges: bytes\r\n")
                append("Connection: close\r\n\r\n")
            }
            output.write(headerBlock.toByteArray(Charsets.US_ASCII))
            output.flush()

            val firstActualEnd = minOf(firstChunkEnd, finalEndByte)
            val firstExpectedBytes = firstActualEnd - startByte + 1L
            val firstWritten = streamChunkBody(
                conn = firstConn,
                expectedBytes = firstExpectedBytes,
                patchFtypBrandAtZero = (startByte == 0L),
                output = output,
            )
            if (firstWritten <= 0L) return
            var cursor = startByte + firstWritten

            while (cursor <= finalEndByte && !closed.get()) {
                val chunkEnd = minOf(finalEndByte, cursor + UPSTREAM_CHUNK_BYTES - 1L)
                val chunkConn = followRedirects(
                    rangeHeader = "bytes=$cursor-$chunkEnd",
                    ifRangeHeader = request.ifRange,
                ) ?: break
                try {
                    val code = chunkConn.responseCode
                    if (code !in 200..299) break
                    val expected = chunkEnd - cursor + 1L
                    val written = streamChunkBody(
                        conn = chunkConn,
                        expectedBytes = expected,
                        patchFtypBrandAtZero = false,
                        output = output,
                    )
                    if (written <= 0L) break
                    cursor += written
                } finally {
                    chunkConn.disconnect()
                }
            }
        } finally {
            firstConn.disconnect()
        }
    }

    /**
     * Responds to JavaFX `HEAD` probes using a bounded 1 KB `GET` range probe so YouTube CDNs never return 403.
     */
    private fun handleHeadRequest(output: BufferedOutputStream) {
        val conn = followRedirects(rangeHeader = "bytes=0-1023", ifRangeHeader = null) ?: run {
            writeStatusResponse(output, 502, "Bad Gateway")
            return
        }
        try {
            val status = conn.responseCode
            if (status !in 200..299) {
                writeStatusResponse(output, status, conn.responseMessage ?: "Upstream Error")
                return
            }
            val normalizedContentType = normalizeJavaFxContentType(conn.getHeaderField("Content-Type"))
            val totalFromRange = parseTotalFromContentRange(conn.getHeaderField("Content-Range"))
            val contentLen = conn.getHeaderField("Content-Length")?.trim()?.toLongOrNull() ?: -1L
            runCatching { conn.inputStream.use { it.readBytes() } }
            val totalLength = when {
                totalFromRange != null && totalFromRange > 0L -> totalFromRange.also { cachedTotalLength = it }
                contentLen > 0L -> contentLen
                else -> cachedTotalLength
            }
            val headHeaders = buildString {
                append("HTTP/1.1 200 OK\r\n")
                append("Content-Type: ").append(normalizedContentType).append("\r\n")
                if (totalLength > 0L) {
                    append("Content-Length: ").append(totalLength).append("\r\n")
                }
                append("Accept-Ranges: bytes\r\n")
                append("Connection: close\r\n\r\n")
            }
            output.write(headHeaders.toByteArray(Charsets.US_ASCII))
            output.flush()
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Reads up to [expectedBytes] from [conn], rewriting `ftypdash` at bytes `4..11` to `ftypM4A ` when [patchFtypBrandAtZero] is true.
     */
    private fun streamChunkBody(
        conn: HttpURLConnection,
        expectedBytes: Long,
        patchFtypBrandAtZero: Boolean,
        output: BufferedOutputStream,
    ): Long {
        conn.inputStream.use { stream ->
            val buffer = ByteArray(STREAM_BUFFER_BYTES)
            var totalReceived = 0L
            var patchedHeader = !patchFtypBrandAtZero
            while (!closed.get() && totalReceived < expectedBytes) {
                val toRead = minOf(buffer.size.toLong(), expectedBytes - totalReceived).toInt()
                val read = stream.read(buffer, 0, toRead)
                if (read < 0) break
                if (!patchedHeader && totalReceived == 0L && read >= 12) {
                    patchDashFtypInPlace(buffer, read)
                    patchedHeader = true
                }
                output.write(buffer, 0, read)
                totalReceived += read
            }
            output.flush()
            return totalReceived
        }
    }

    /**
     * Relays a single non-rangeable upstream connection directly to [output].
     */
    private fun relaySingleConnection(
        conn: HttpURLConnection,
        normalizedContentType: String,
        output: BufferedOutputStream,
        patchFtypBrandAtZero: Boolean,
    ) {
        val rawStatus = conn.responseCode
        val reason = conn.responseMessage?.takeIf { it.isNotBlank() } ?: "OK"
        val headerBlock = buildString {
            append("HTTP/1.1 ").append(rawStatus).append(' ').append(reason).append("\r\n")
            append("Content-Type: ").append(normalizedContentType).append("\r\n")
            append("Accept-Ranges: bytes\r\n")
            append("Connection: close\r\n\r\n")
        }
        output.write(headerBlock.toByteArray(Charsets.US_ASCII))
        output.flush()
        conn.inputStream.use { stream ->
            val buffer = ByteArray(STREAM_BUFFER_BYTES)
            var firstChunk = patchFtypBrandAtZero
            while (!closed.get()) {
                val read = stream.read(buffer)
                if (read < 0) break
                if (firstChunk && read >= 12) {
                    patchDashFtypInPlace(buffer, read)
                    firstChunk = false
                }
                output.write(buffer, 0, read)
            }
            output.flush()
        }
    }

    /**
     * Rewrites MP4 major brand `ftypdash` at offset `4..11` to `ftypM4A ` so OpenJFX `MediaUtils` accepts the header.
     */
    private fun patchDashFtypInPlace(buffer: ByteArray, length: Int) {
        if (length < 12) return
        val isFtyp = buffer[4] == 'f'.code.toByte() &&
            buffer[5] == 't'.code.toByte() &&
            buffer[6] == 'y'.code.toByte() &&
            buffer[7] == 'p'.code.toByte()
        val isDash = buffer[8] == 'd'.code.toByte() &&
            buffer[9] == 'a'.code.toByte() &&
            buffer[10] == 's'.code.toByte() &&
            buffer[11] == 'h'.code.toByte()
        if (isFtyp && isDash) {
            buffer[8] = 'M'.code.toByte()
            buffer[9] = '4'.code.toByte()
            buffer[10] = 'A'.code.toByte()
            buffer[11] = ' '.code.toByte()
        }
    }

    /**
     * Extracts the total byte length from an HTTP `Content-Range: bytes start-end/total` response header.
     */
    private fun parseTotalFromContentRange(contentRange: String?): Long? {
        if (contentRange.isNullOrBlank()) return null
        return contentRange
            .substringAfter('/', "")
            .trim()
            .takeIf { it.isNotEmpty() && it.all(Char::isDigit) }
            ?.toLongOrNull()
    }

    /**
     * Parses a client `Range: bytes=start-end` header into `(startByte, optionalEndByte)`.
     */
    private fun parseRawRangeSpec(rangeHeader: String?): Pair<Long, Long?>? {
        if (rangeHeader.isNullOrBlank()) return null
        val trimmed = rangeHeader.trim()
        if (!trimmed.startsWith("bytes=", ignoreCase = true)) return null
        val spec = trimmed.substringAfter('=').substringBefore(',').trim()
        val dash = spec.indexOf('-')
        if (dash < 0) return null
        val startPart = spec.substring(0, dash).trim()
        val endPart = spec.substring(dash + 1).trim()
        val start = startPart.toLongOrNull() ?: return null
        val end = if (endPart.isEmpty()) null else endPart.toLongOrNull()
        return start to end
    }

    /**
     * Follows up to [MAX_REDIRECTS] HTTP/HTTPS redirects manually so cross-scheme redirects retain custom headers.
     */
    private fun followRedirects(rangeHeader: String?, ifRangeHeader: String?): HttpURLConnection? {
        var currentUrl = targetUrl
        repeat(MAX_REDIRECTS + 1) { hop ->
            val conn = openConfiguredConnection(currentUrl, rangeHeader, ifRangeHeader)
            val status = conn.responseCode
            if (status in REDIRECT_CODES && hop < MAX_REDIRECTS) {
                val location = conn.getHeaderField("Location")
                conn.disconnect()
                if (location.isNullOrBlank()) return null
                currentUrl = URI(currentUrl).resolve(location).toString()
                return@repeat
            }
            return conn
        }
        return null
    }

    /**
     * Creates and configures a `GET` [HttpURLConnection] to [urlString] with custom [headers] and bounded [rangeHeader].
     */
    private fun openConfiguredConnection(
        urlString: String,
        rangeHeader: String?,
        ifRangeHeader: String?,
    ): HttpURLConnection {
        val conn = (URI(urlString).toURL().openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            instanceFollowRedirects = false
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("User-Agent", DEFAULT_USER_AGENT)
            for ((name, value) in headers) {
                setRequestProperty(name, value)
            }
            if (!rangeHeader.isNullOrBlank()) {
                setRequestProperty("Range", rangeHeader)
            }
            if (!ifRangeHeader.isNullOrBlank()) {
                setRequestProperty("If-Range", ifRangeHeader)
            }
        }
        return conn
    }

    /**
     * Maps YouTube/CDN MIME types like `audio/mp4` onto OpenJFX `MediaManager`-supported types (`audio/x-m4a`).
     */
    private fun normalizeJavaFxContentType(upstreamType: String?): String {
        val clean = upstreamType?.substringBefore(';')?.trim()?.lowercase().orEmpty()
        return when {
            clean == "audio/mpeg" || clean == "audio/mp3" -> "audio/mpeg"
            clean == "audio/x-wav" || clean == "audio/wav" -> "audio/x-wav"
            clean == "audio/x-aiff" || clean == "audio/aiff" -> "audio/x-aiff"
            else -> "audio/x-m4a"
        }
    }

    /**
     * Reads the HTTP request line and relevant `Range` / `If-Range` headers from [input].
     * Returns null when the connection closes prematurely or sends a malformed request line.
     */
    private fun readHttpRequest(input: InputStream): ParsedRequest? {
        val requestLine = readAsciiLine(input) ?: return null
        val parts = requestLine.split(' ')
        if (parts.size < 2) return null
        val method = parts[0].uppercase()
        val path = parts[1].substringBefore('?')

        var range: String? = null
        var ifRange: String? = null
        while (true) {
            val line = readAsciiLine(input) ?: break
            if (line.isEmpty()) break
            val colon = line.indexOf(':')
            if (colon <= 0) continue
            val name = line.substring(0, colon).trim()
            val value = line.substring(colon + 1).trim()
            if (name.equals("Range", ignoreCase = true)) {
                range = value
            } else if (name.equals("If-Range", ignoreCase = true)) {
                ifRange = value
            }
        }
        return ParsedRequest(method = method, path = path, range = range, ifRange = ifRange)
    }

    /**
     * Reads a single CRLF-terminated ASCII header line up to 8 KB without blocking beyond line end.
     * Prevents buffered readers from over-reading past the HTTP header boundary.
     */
    private fun readAsciiLine(input: InputStream): String? {
        val bytes = ArrayList<Byte>(128)
        while (bytes.size < MAX_HEADER_LINE_BYTES) {
            val next = input.read()
            if (next == -1) {
                return if (bytes.isEmpty()) null else bytes.toByteArray().toString(Charsets.US_ASCII)
            }
            if (next == '\n'.code) break
            if (next != '\r'.code) {
                bytes.add(next.toByte())
            }
        }
        return bytes.toByteArray().toString(Charsets.US_ASCII)
    }

    /**
     * Writes a minimal terminal HTTP error response to [output] and flushes immediately.
     * Used when a local client hits an invalid proxy token or unsupported HTTP method.
     */
    private fun writeStatusResponse(output: BufferedOutputStream, code: Int, message: String) {
        val payload = "HTTP/1.1 $code $message\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
        output.write(payload.toByteArray(Charsets.US_ASCII))
        output.flush()
    }

    /**
     * Parsed HTTP request metadata needed to proxy a JavaFX audio fetch upstream.
     * Holds HTTP method, request path, and optional byte-range headers.
     */
    private data class ParsedRequest(
        val method: String,
        val path: String,
        val range: String?,
        val ifRange: String?,
    )

    private companion object {
        const val CONNECT_TIMEOUT_MS: Int = 15_000
        const val READ_TIMEOUT_MS: Int = 30_000
        const val STREAM_BUFFER_BYTES: Int = 8_192
        const val UPSTREAM_CHUNK_BYTES: Long = 524_288L
        const val MAX_HEADER_LINE_BYTES: Int = 8_192
        const val MAX_REDIRECTS: Int = 5
        const val DEFAULT_USER_AGENT: String =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36"
        val REDIRECT_CODES = setOf(301, 302, 303, 307, 308)
    }
}
