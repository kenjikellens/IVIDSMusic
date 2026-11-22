package com.kenjigames.ividsmusic.data.stream

import com.kenjigames.ividsmusic.domain.model.AppResult
import com.kenjigames.ividsmusic.domain.model.AudioQuality
import com.kenjigames.ividsmusic.domain.model.DomainError
import com.kenjigames.ividsmusic.domain.model.StreamSource
import com.kenjigames.ividsmusic.domain.model.Track
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Fake [StreamTierResolver] tracking invocation counts for verifying tier fallback order and TTL caching.
 */
private class FakeTierResolver(
    private val response: AppResult<StreamSource.RemoteUrl>
) : StreamTierResolver, PlatformStreamFallback {
    var calls = 0

    /** Increments invocation count and returns configured [response]. */
    override suspend fun resolveTrack(track: Track, preferredQuality: AudioQuality): AppResult<StreamSource.RemoteUrl> {
        calls++
        return response
    }

    /** Increments invocation count and returns configured [response] for direct URL calls. */
    override suspend fun resolveDirectUrl(
        videoUrlOrId: String,
        preferredQuality: AudioQuality
    ): AppResult<StreamSource.RemoteUrl> {
        calls++
        return response
    }
}

/**
 * Tests 4-tier stream resolution ordering, 30-min TTL expiration, duration tolerance, and Invidious parallel racing.
 */
class CompositeStreamResolverTest {

    private lateinit var tempSavedDir: File
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
    private var clock = 1_000_000L

    /** Prepares an isolated temporary `saved/` folder before each test. */
    @BeforeTest
    fun setup() {
        tempSavedDir = Files.createTempDirectory("ivids_stream_test").toFile()
    }

    /** Cleans up temporary saved audio fixtures after each test. */
    @AfterTest
    fun tearDown() {
        tempSavedDir.deleteRecursively()
    }

    /** Confirms Tier 1 local file detection short-circuits cache and remote tiers immediately. */
    @Test
    fun tier1LocalOfflineFileShortCircuitsRemoteTiers() = runTest {
        val localFile = File(tempSavedDir, "Artist - Local Song.m4a").apply { writeText("audio-bytes") }
        val primary = FakeTierResolver(AppResult.Failure(DomainError.StreamNotFound("unused")))
        val invidious = FakeTierResolver(AppResult.Failure(DomainError.StreamNotFound("unused")))
        val desktop = FakeTierResolver(AppResult.Failure(DomainError.StreamNotFound("unused")))

        val resolver = CompositeStreamResolver(
            localAudioLocator = JvmLocalAudioLocator(),
            savedDirectoryProvider = { tempSavedDir.absolutePath },
            streamTtlCache = InMemoryStreamTtlCache(clockMs = { clock }),
            primaryExtractor = primary,
            invidiousFallback = invidious,
            platformFallback = desktop
        )

        val result = resolver.resolve(Track(id = "t1", title = "Local Song", artist = "Artist"))
        val source = assertIs<StreamSource.LocalFile>((result as AppResult.Success).data)
        assertEquals(localFile.absolutePath, source.filePath)
        assertEquals(0, primary.calls)
        assertEquals(0, invidious.calls)
        assertEquals(0, desktop.calls)
    }

    /** Confirms Tier 2 caches remote streams for 30 minutes, expires cleanly after 30m, and supports invalidation. */
    @Test
    fun tier2TtlCacheServesHitsAndExpiresAfterThirtyMinutes() = runTest {
        val primary = FakeTierResolver(
            AppResult.Success(StreamSource.RemoteUrl("https://cdn.example/stream.m4a", quality = AudioQuality.Q196))
        )
        val resolver = CompositeStreamResolver(
            localAudioLocator = JvmLocalAudioLocator(),
            savedDirectoryProvider = { tempSavedDir.absolutePath },
            streamTtlCache = InMemoryStreamTtlCache(clockMs = { clock }),
            primaryExtractor = primary,
            invidiousFallback = FakeTierResolver(AppResult.Failure(DomainError.StreamNotFound("unused"))),
            platformFallback = FakeTierResolver(AppResult.Failure(DomainError.StreamNotFound("unused")))
        )

        val track = Track(id = "t_cache", title = "Cached", artist = "Band")
        resolver.resolve(track, AudioQuality.Q196)
        resolver.resolve(track, AudioQuality.Q196)
        assertEquals(1, primary.calls)

        // Advance clock past 30 min TTL
        clock += 30 * 60 * 1000L + 1L
        resolver.resolve(track, AudioQuality.Q196)
        assertEquals(2, primary.calls)

        resolver.invalidate("t_cache")
        resolver.resolve(track, AudioQuality.Q196)
        assertEquals(3, primary.calls)
    }

    /** Confirms Tier 3 failure falls through to Tier 4a Invidious parallel race selecting closest audio/mp4 format. */
    @Test
    fun invidiousParallelRacePicksFastestHealthyMirrorWithinDurationTolerance() = runTest {
        val mockEngine = MockEngine { request ->
            val host = request.url.host
            val path = request.url.encodedPath
            val headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            when {
                host == "slow.mirror" -> {
                    delay(5_000L)
                    respond("[]", headers = headers)
                }
                host == "broken.mirror" -> respond("error", status = HttpStatusCode.InternalServerError, headers = headers)
                host == "fast.mirror" && path == "/api/v1/search" -> respond(
                    content = """
                    [
                      { "type": "video", "videoId": "wrong_len", "title": "10 hour loop", "lengthSeconds": 36000 },
                      { "type": "video", "videoId": "good_vid", "title": "Exact Track", "lengthSeconds": 205 }
                    ]
                    """.trimIndent(),
                    headers = headers
                )
                host == "fast.mirror" && path == "/api/v1/videos/good_vid" -> respond(
                    content = """
                    {
                      "videoId": "good_vid",
                      "adaptiveFormats": [
                        { "url": "https://audio.example/48k.m4a", "type": "audio/mp4; codecs=\"mp4a.40.5\"", "bitrate": "48000" },
                        { "url": "https://audio.example/256k.m4a", "type": "audio/mp4; codecs=\"mp4a.40.2\"", "bitrate": "256000" }
                      ]
                    }
                    """.trimIndent(),
                    headers = headers
                )
                else -> respond("{}", status = HttpStatusCode.NotFound, headers = headers)
            }
        }

        val invidious = InvidiousFallbackResolver(
            httpClient = HttpClient(mockEngine),
            json = json,
            clockMs = { clock },
            instances = listOf("https://slow.mirror", "https://broken.mirror", "https://fast.mirror"),
            raceTimeoutMs = 2_000L
        )
        val desktop = FakeTierResolver(
            AppResult.Success(StreamSource.RemoteUrl("https://ytdlp.example/fallback.m4a"))
        )
        val resolver = CompositeStreamResolver(
            localAudioLocator = JvmLocalAudioLocator(),
            savedDirectoryProvider = { tempSavedDir.absolutePath },
            streamTtlCache = InMemoryStreamTtlCache(clockMs = { clock }),
            primaryExtractor = FakeTierResolver(AppResult.Failure(DomainError.StreamNotFound("NewPipe down"))),
            invidiousFallback = invidious,
            platformFallback = desktop
        )

        val result = resolver.resolve(
            Track(id = "t_race", title = "Exact Track", artist = "Artist", durationSeconds = 200),
            preferredQuality = AudioQuality.Q256
        )
        val remote = assertIs<StreamSource.RemoteUrl>((result as AppResult.Success).data)
        assertEquals("https://audio.example/256k.m4a", remote.url)
        assertEquals("good_vid", remote.videoId)
        assertEquals(AudioQuality.Q256, remote.quality)
        assertEquals(0, desktop.calls)
    }

    /** Verifies Tier 4b Desktop fallback runs when both Tier 3 and Tier 4a fail, plus duration/ID utilities. */
    @Test
    fun tier4bDesktopFallbackTriggersAndHelpersMatchSpec() = runTest {
        assertTrue(isDurationWithinTolerance(215, 200))
        assertFalse(isDurationWithinTolerance(216, 200))
        assertEquals(128, normalizeBitrateKbps(128_000))
        assertEquals(128, normalizeBitrateKbps(128))
        assertEquals("dQw4w9WgXcQ", extractYouTubeVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", extractYouTubeVideoId("https://youtu.be/dQw4w9WgXcQ?t=10"))

        val desktop = FakeTierResolver(
            AppResult.Success(StreamSource.RemoteUrl("https://ytdlp.example/stream.m4a", quality = AudioQuality.Q128))
        )
        val resolver = CompositeStreamResolver(
            localAudioLocator = JvmLocalAudioLocator(),
            savedDirectoryProvider = { tempSavedDir.absolutePath },
            streamTtlCache = InMemoryStreamTtlCache(clockMs = { clock }),
            primaryExtractor = FakeTierResolver(AppResult.Failure(DomainError.StreamNotFound("NewPipe fail"))),
            invidiousFallback = FakeTierResolver(AppResult.Failure(DomainError.StreamNotFound("Invidious fail"))),
            platformFallback = desktop
        )

        val result = resolver.resolve(Track(id = "t_ytdlp", title = "Fallback", artist = "Band"), AudioQuality.Q128)
        val source = assertIs<StreamSource.RemoteUrl>((result as AppResult.Success).data)
        assertEquals("https://ytdlp.example/stream.m4a", source.url)
        assertEquals(1, desktop.calls)
    }
}
