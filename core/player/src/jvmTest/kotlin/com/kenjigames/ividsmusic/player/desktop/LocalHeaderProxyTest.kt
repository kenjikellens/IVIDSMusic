package com.kenjigames.ividsmusic.player.desktop

import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.ServerSocket
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Verifies [LocalHeaderProxy] forwards custom HTTP headers and `Range` headers and releases its socket cleanly.
 * Uses an ephemeral loopback upstream socket with zero external network dependencies.
 */
class LocalHeaderProxyTest {

    /**
     * Starts an upstream loopback stub, fetches via [LocalHeaderProxy], and verifies headers and cleanup.
     * Ensures `proxy.close()` tears down the listening socket so subsequent connects fail.
     */
    @Test
    fun forwardsHeadersAndRangeThenClosesCleanly() {
        val capturedHeaders = LinkedHashMap<String, String>()
        val payload = "0123456789ABCDEF".toByteArray(Charsets.US_ASCII)

        val upstreamServer = ServerSocket(0, 2, InetAddress.getLoopbackAddress())
        val upstreamThread = Thread {
            try {
                upstreamServer.accept().use { client ->
                    val reader = client.getInputStream().bufferedReader(Charsets.US_ASCII)
                    reader.readLine()
                    while (true) {
                        val line = reader.readLine() ?: break
                        if (line.isEmpty()) break
                        val colon = line.indexOf(':')
                        if (colon > 0) {
                            capturedHeaders[line.substring(0, colon).trim().lowercase()] =
                                line.substring(colon + 1).trim()
                        }
                    }
                    val out = client.getOutputStream()
                    val responseHeader = buildString {
                        append("HTTP/1.1 206 Partial Content\r\n")
                        append("Content-Type: audio/mp4\r\n")
                        append("Content-Range: bytes 0-15/16\r\n")
                        append("Content-Length: ").append(payload.size).append("\r\n")
                        append("Connection: close\r\n\r\n")
                    }
                    out.write(responseHeader.toByteArray(Charsets.US_ASCII))
                    out.write(payload)
                    out.flush()
                }
            } catch (_: Exception) {
                // Upstream server stopped after verification.
            }
        }.apply {
            isDaemon = true
            start()
        }

        val proxy = LocalHeaderProxy(
            targetUrl = "http://127.0.0.1:${upstreamServer.localPort}/audio.m4a",
            headers = mapOf(
                "User-Agent" to "IvidsTestAgent/1.0",
                "Referer" to "https://music.example.com/",
            ),
        )

        try {
            val conn = (URI(proxy.localUrl).toURL().openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Range", "bytes=0-15")
            }
            assertEquals(206, conn.responseCode)
            assertEquals("audio/mp4", conn.contentType)
            val received = conn.inputStream.use { it.readBytes() }.toString(Charsets.US_ASCII)
            assertEquals("0123456789ABCDEF", received)
            assertEquals("IvidsTestAgent/1.0", capturedHeaders["user-agent"])
            assertEquals("https://music.example.com/", capturedHeaders["referer"])
            assertEquals("bytes=0-15", capturedHeaders["range"])
            conn.disconnect()
        } finally {
            proxy.close()
            upstreamServer.close()
            upstreamThread.join(1_000L)
        }

        val connectAfterCloseFailed = try {
            val postConn = URI(proxy.localUrl).toURL().openConnection() as HttpURLConnection
            postConn.connectTimeout = 300
            postConn.readTimeout = 300
            postConn.responseCode
            false
        } catch (_: Exception) {
            true
        }
        assertTrue(connectAfterCloseFailed)
    }
}
