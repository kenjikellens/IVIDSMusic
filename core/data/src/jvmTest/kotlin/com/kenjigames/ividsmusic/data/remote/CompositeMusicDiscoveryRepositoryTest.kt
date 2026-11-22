package com.kenjigames.ividsmusic.data.remote

import com.kenjigames.ividsmusic.domain.model.AppResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration tests verifying Deezer parsing, pagination, year filtering, genre enrichment, and iTunes fallback.
 */
class CompositeMusicDiscoveryRepositoryTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    /** Confirms Deezer responses parse pagination, year filtering, genre charts, album details, and track radio. */
    @Test
    fun deezerPrimaryEndpointsParseAndEnrichMetadata() = runTest {
        var requestCount = 0
        val mockEngine = MockEngine { request ->
            requestCount++
            val path = request.url.encodedPath
            val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            when {
                path.endsWith("/search") -> {
                    val q = request.url.parameters["q"].orEmpty()
                    assertTrue(q.contains("year:\"2018-2024\""))
                    assertEquals("10", request.url.parameters["index"])
                    respond(
                        content = """
                        {
                          "data": [
                            {
                              "id": 101,
                              "title": "Cyan Drift",
                              "duration": 210,
                              "rank": 500000,
                              "release_date": "2022-04-01",
                              "artist": { "id": 77, "name": "Nova" },
                              "album": { "id": 88, "title": "Obsidian", "cover_big": "https://img/obs.jpg" }
                            },
                            {
                              "id": 102,
                              "title": "Old Track",
                              "duration": 180,
                              "release_date": "1999-01-01",
                              "artist": { "id": 78, "name": "Retro" }
                            }
                          ]
                        }
                        """.trimIndent(),
                        headers = jsonHeaders
                    )
                }
                path.endsWith("/track/101") -> respond(
                    content = """
                    {
                      "id": 101,
                      "title": "Cyan Drift",
                      "duration": 210,
                      "artist": { "id": 77, "name": "Nova" },
                      "album": { "id": 88, "title": "Obsidian" }
                    }
                    """.trimIndent(),
                    headers = jsonHeaders
                )
                path.endsWith("/album/88") -> respond(
                    content = """
                    {
                      "id": 88,
                      "title": "Obsidian",
                      "release_date": "2022-04-01",
                      "nb_tracks": 1,
                      "genres": { "data": [{ "id": 106, "name": "Electronic" }] },
                      "artist": { "id": 77, "name": "Nova" },
                      "tracks": { "data": [{ "id": 101, "title": "Cyan Drift", "duration": 210, "artist": { "id": 77, "name": "Nova" } }] }
                    }
                    """.trimIndent(),
                    headers = jsonHeaders
                )
                path.endsWith("/track/101/radio") -> respond(
                    content = """{ "data": [{ "id": 202, "title": "Echoes", "duration": 195, "artist": { "id": 79, "name": "Pulse" } }] }""",
                    headers = jsonHeaders
                )
                else -> respond("{}", status = HttpStatusCode.NotFound, headers = jsonHeaders)
            }
        }

        val http = HttpClient(mockEngine)
        val repo = CompositeMusicDiscoveryRepository(
            deezerClient = DeezerApiClient(http, json),
            itunesClient = ItunesApiClient(http, json)
        )

        val tracksRes = repo.searchTracks("synth", limit = 10, offset = 10, yearRange = 2018..2024)
        assertTrue(tracksRes is AppResult.Success)
        assertEquals(listOf("101"), tracksRes.data.map { it.id })

        // Second identical search hits 15-min TTL cache without hitting HTTP engine
        val countBeforeCache = requestCount
        repo.searchTracks("synth", limit = 10, offset = 10, yearRange = 2018..2024)
        assertEquals(countBeforeCache, requestCount)

        val trackDetails = (repo.getTrackDetails("101") as AppResult.Success).data
        assertEquals(listOf("Electronic"), trackDetails.genres)
        assertEquals(2022, trackDetails.releaseYear)

        val radio = (repo.getRelatedTracks("101") as AppResult.Success).data
        assertEquals("202", radio.first().id)
    }

    /** Verifies Deezer HTTP 200 quota errors automatically fall back to Apple iTunes Search (text/javascript). */
    @Test
    fun deezerQuotaErrorFallsBackAutomaticallyToItunes() = runTest {
        val mockEngine = MockEngine { request ->
            when (request.url.host) {
                "api.deezer.com" -> respond(
                    content = """{ "error": { "type": "QuotaException", "message": "Quota limit exceeded", "code": 4 } }""",
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
                "itunes.apple.com" -> respond(
                    content = """
                    {
                      "resultCount": 1,
                      "results": [
                        {
                          "wrapperType": "track",
                          "kind": "song",
                          "trackId": 9001,
                          "trackName": "Midnight Horizon",
                          "artistId": 42,
                          "artistName": "Koda",
                          "collectionId": 55,
                          "collectionName": "Starlight",
                          "artworkUrl100": "https://is1.mzstatic.com/image/thumb/100x100bb.jpg",
                          "trackTimeMillis": 240000,
                          "primaryGenreName": "Electronic",
                          "releaseDate": "2023-08-15T07:00:00Z"
                        }
                      ]
                    }
                    """.trimIndent(),
                    headers = headersOf(HttpHeaders.ContentType, "text/javascript; charset=utf-8")
                )
                else -> respond("{}", status = HttpStatusCode.BadRequest)
            }
        }

        val http = HttpClient(mockEngine)
        val repo = CompositeMusicDiscoveryRepository(
            deezerClient = DeezerApiClient(http, json),
            itunesClient = ItunesApiClient(http, json)
        )

        val result = repo.searchTracks("Midnight", yearRange = 2020..2025)
        assertTrue(result is AppResult.Success)
        val track = result.data.single()
        assertEquals("9001", track.id)
        assertEquals("Midnight Horizon", track.title)
        assertEquals("Koda", track.artist)
        assertEquals(240, track.durationSeconds)
        assertEquals(2023, track.releaseYear)
        assertEquals(listOf("Electronic"), track.genres)
        assertTrue(track.coverUrl.contains("600x600bb"))
    }
}
