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
 * Ephemeral loopback HTTP proxy attaching custom headers to remote audio streams for JavaFX `Media`.
 * Bridges `Range` and `HEAD` requests over `127.0.0.1` and closes sockets deterministically on [close].
 */
internal class LocalHeaderProxy(
    private val targetUrl: String,
    private val headers: Map<String, String>,
) : Closeable {

    private val closed = AtomicBoolean(false)
    private val token = "ivids-${UUID.randomUUID()}"
    private val serverSocket = ServerSocket(0, 8, InetAddress.getLoopbackAddress())
    private val activeSockets: MutableSet<Socket> = Collections.synchronizedSet(mutableSetOf())

    /**
     * Loopback URL passed to JavaFX `Media` so all audio fetches include [headers].
     * Bound dynamically to an ephemeral localhost port until [close] is invoked.
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

    /**
     * Opens an upstream [HttpURLConnection], attaches [headers] and range headers, and relays the response to [output].
     * Follows up to 5 HTTP/HTTPS redirects manually so cross-scheme redirects retain custom headers.
     */
    private fun forwardToUpstream(request: ParsedRequest, output: BufferedOutputStream) {
        var currentUrl = targetUrl
        var connection: HttpURLConnection? = null
        try {
            repeat(MAX_REDIRECTS + 1) { hop ->
                val conn = openConfiguredConnection(currentUrl, request)
                val status = conn.responseCode
                if (status in REDIRECT_CODES && hop < MAX_REDIRECTS) {
                    val location = conn.getHeaderField("Location")
                    conn.disconnect()
                    if (location.isNullOrBlank()) {
                        writeStatusResponse(output, 502, "Bad Gateway")
                        return
                    }
                    currentUrl = URI(currentUrl).resolve(location).toString()
                    return@repeat
                }
                connection = conn
                relayResponse(conn, request.method == "HEAD", output)
                return
            }
            writeStatusResponse(output, 502, "Too Many Redirects")
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Creates and configures an [HttpURLConnection] to [urlString] with custom [headers] and client range headers.
     * Disables automatic redirects so protocol-crossing redirects keep configured headers intact.
     */
    private fun openConfiguredConnection(urlString: String, request: ParsedRequest): HttpURLConnection {
        val conn = (URI(urlString).toURL().openConnection() as HttpURLConnection).apply {
            requestMethod = request.method
            instanceFollowRedirects = false
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            for ((name, value) in headers) {
                setRequestProperty(name, value)
            }
            request.range?.let { setRequestProperty("Range", it) }
            request.ifRange?.let { setRequestProperty("If-Range", it) }
        }
        return conn
    }

    /**
     * Writes upstream HTTP status, content headers, and streamed body bytes directly to [output].
     * Streams via an 8 KB buffer so large audio files are never buffered wholly in heap memory.
     */
    private fun relayResponse(
        conn: HttpURLConnection,
        isHead: Boolean,
        output: BufferedOutputStream,
    ) {
        val statusCode = conn.responseCode
        val reason = conn.responseMessage?.takeIf { it.isNotBlank() } ?: "OK"
        val headerBlock = buildString {
            append("HTTP/1.1 ").append(statusCode).append(' ').append(reason).append("\r\n")
            for (headerName in FORWARDED_RESPONSE_HEADERS) {
                val value = conn.getHeaderField(headerName)
                if (!value.isNullOrBlank()) {
                    append(headerName).append(": ").append(value).append("\r\n")
                }
            }
            append("Connection: close\r\n\r\n")
        }
        output.write(headerBlock.toByteArray(Charsets.US_ASCII))
        output.flush()
        if (isHead) return

        val upstreamStream: InputStream = if (statusCode in 200..399) {
            conn.inputStream
        } else {
            conn.errorStream ?: return
        }
        upstreamStream.use { stream ->
            val buffer = ByteArray(STREAM_BUFFER_BYTES)
            while (!closed.get()) {
                val read = stream.read(buffer)
                if (read < 0) break
                output.write(buffer, 0, read)
            }
            output.flush()
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
        const val MAX_HEADER_LINE_BYTES: Int = 8_192
        const val MAX_REDIRECTS: Int = 5
        val REDIRECT_CODES = setOf(301, 302, 303, 307, 308)
        val FORWARDED_RESPONSE_HEADERS = listOf(
            "Content-Type",
            "Content-Length",
            "Content-Range",
            "Accept-Ranges",
        )
    }
}
