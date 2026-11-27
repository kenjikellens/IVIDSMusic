package com.kenjigames.ividsmusic.data.remote

import com.kenjigames.ividsmusic.data.remote.dto.DeezerAlbumDto
import com.kenjigames.ividsmusic.data.remote.dto.DeezerArtistDto
import com.kenjigames.ividsmusic.data.remote.dto.DeezerListResponse
import com.kenjigames.ividsmusic.data.remote.dto.DeezerTrackDto
import com.kenjigames.ividsmusic.domain.model.Album
import com.kenjigames.ividsmusic.domain.model.AlbumDetails
import com.kenjigames.ividsmusic.domain.model.AppResult
import com.kenjigames.ividsmusic.domain.model.Artist
import com.kenjigames.ividsmusic.domain.model.ArtistDetails
import com.kenjigames.ividsmusic.domain.model.CategoryRow
import com.kenjigames.ividsmusic.domain.model.DomainError
import com.kenjigames.ividsmusic.domain.model.Track
import com.kenjigames.ividsmusic.domain.model.getOrNull
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException

/**
 * Ktor REST client querying the public Deezer catalog API (`https://api.deezer.com/`).
 * Maps JSON responses and API-level error objects into immutable domain models wrapped in [AppResult].
 */
class DeezerApiClient(
    private val httpClient: HttpClient,
    private val json: Json,
    private val baseUrl: String = BASE_URL
) {
    /** Searches Deezer tracks matching [query] with optional [yearRange] filter and [offset] pagination. */
    suspend fun searchTracks(
        query: String,
        limit: Int = 24,
        offset: Int = 0,
        yearRange: IntRange? = null
    ): AppResult<List<Track>> = safeRequest {
        val formattedQuery = appendYearFilter(query.trim(), yearRange)
        val payload = getJson<DeezerListResponse<DeezerTrackDto>>("search") {
            parameter("q", formattedQuery)
            parameter("limit", limit)
            parameter("index", offset)
        }
        payload.error?.let { err ->
            return@safeRequest AppResult.Failure(
                DomainError.Network(err.message ?: "Deezer search error", err.code)
            )
        }
        val mapped = payload.data.map { mapTrack(it) }.let { tracks ->
            if (yearRange == null) tracks
            else tracks.filter { it.releaseYear == null || it.releaseYear in yearRange }
        }
        AppResult.Success(mapped)
    }

    /** Searches Deezer artists matching [query] with pagination parameters [limit] and [offset]. */
    suspend fun searchArtists(
        query: String,
        limit: Int = 24,
        offset: Int = 0
    ): AppResult<List<Artist>> = safeRequest {
        val payload = getJson<DeezerListResponse<DeezerArtistDto>>("search/artist") {
            parameter("q", query.trim())
            parameter("limit", limit)
            parameter("index", offset)
        }
        payload.error?.let { err ->
            return@safeRequest AppResult.Failure(
                DomainError.Network(err.message ?: "Deezer artist search error", err.code)
            )
        }
        AppResult.Success(payload.data.map(::mapArtist))
    }

    /** Searches Deezer albums matching [query] with optional [yearRange] filter and [offset] pagination. */
    suspend fun searchAlbums(
        query: String,
        limit: Int = 24,
        offset: Int = 0,
        yearRange: IntRange? = null
    ): AppResult<List<Album>> = safeRequest {
        val formattedQuery = appendYearFilter(query.trim(), yearRange)
        val payload = getJson<DeezerListResponse<DeezerAlbumDto>>("search/album") {
            parameter("q", formattedQuery)
            parameter("limit", limit)
            parameter("index", offset)
        }
        payload.error?.let { err ->
            return@safeRequest AppResult.Failure(
                DomainError.Network(err.message ?: "Deezer album search error", err.code)
            )
        }
        val mapped = payload.data.map(::mapAlbum).let { albums ->
            if (yearRange == null) albums
            else albums.filter { it.releaseYear == null || it.releaseYear in yearRange }
        }
        AppResult.Success(mapped)
    }

    /** Fetches top global Deezer chart tracks up to [limit] items. */
    suspend fun getChartTracks(limit: Int = 20): AppResult<List<Track>> = safeRequest {
        val payload = getJson<DeezerListResponse<DeezerTrackDto>>("chart/0/tracks") {
            parameter("limit", limit)
        }
        payload.error?.let { err ->
            return@safeRequest AppResult.Failure(
                DomainError.Network(err.message ?: "Deezer chart error", err.code)
            )
        }
        AppResult.Success(payload.data.map { mapTrack(it) })
    }

    /** Loads curated genre chart rows concurrently, limiting each shelf to one track per artist (max 12). */
    suspend fun getGenreCharts(genres: List<String>): AppResult<List<CategoryRow>> = safeRequest {
        val rows = coroutineScope {
            genres.map { genre ->
                async { fetchSingleGenreRow(genre) }
            }.awaitAll()
        }
        val nonEmpty = rows.filter { it.tracks.isNotEmpty() }
        if (nonEmpty.isEmpty() && genres.isNotEmpty()) {
            AppResult.Failure(DomainError.Network("All Deezer genre chart requests returned empty"))
        } else {
            AppResult.Success(nonEmpty)
        }
    }

    /** Retrieves tracks from a single Deezer genre chart or genre search fallback up to [limit] items. */
    suspend fun getGenreTracks(genreName: String, limit: Int = 20): AppResult<List<Track>> = safeRequest {
        val genreId = GENRE_IDS[genreName] ?: GENRE_IDS.entries
            .firstOrNull { it.key.equals(genreName.trim(), ignoreCase = true) }?.value

        if (genreId != null) {
            val payload = getJson<DeezerListResponse<DeezerTrackDto>>("chart/$genreId/tracks") {
                parameter("limit", maxOf(limit, 30))
            }
            if (payload.error == null && payload.data.isNotEmpty()) {
                val tracks = payload.data.map { mapTrack(it, listOf(genreName)) }.take(limit)
                return@safeRequest AppResult.Success(tracks)
            }
        }
        searchTracks(genreName, limit = limit)
    }

    /** Fetches full track metadata and enriches genre tags via the track's parent Deezer album when available. */
    suspend fun getTrackDetails(trackId: String): AppResult<Track> = safeRequest {
        val dto = getJson<DeezerTrackDto>("track/$trackId")
        dto.error?.let { err ->
            return@safeRequest AppResult.Failure(
                DomainError.Network(err.message ?: "Deezer track details error", err.code)
            )
        }
        val albumId = dto.album?.id?.takeIf { it > 0L }?.toString()
        val albumDto = if (albumId != null) {
            runCatchingNonCancel { getJson<DeezerAlbumDto>("album/$albumId") }
        } else {
            null
        }
        val genres = albumDto?.genres?.data
            ?.map { it.name.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()
        val releaseYear = parseYear(dto.releaseDate ?: dto.album?.releaseDate ?: albumDto?.releaseDate)
        AppResult.Success(
            mapTrack(dto, genres).let { mapped ->
                if (mapped.releaseYear == null && releaseYear != null) {
                    mapped.copy(releaseYear = releaseYear)
                } else {
                    mapped
                }
            }
        )
    }

    /** Fetches artist details, top tracks, albums, and related artists, resolving by [artistNameHint] when needed. */
    suspend fun getArtistDetails(
        artistId: String,
        artistNameHint: String = ""
    ): AppResult<ArtistDetails> = safeRequest {
        val resolvedId = resolveNumericArtistId(artistId, artistNameHint)
            ?: return@safeRequest AppResult.Failure(
                DomainError.StreamNotFound("Artist not found on Deezer: $artistId / $artistNameHint")
            )

        coroutineScope {
            val detailsDeferred = async { getJson<DeezerArtistDto>("artist/$resolvedId") }
            val topDeferred = async {
                runCatchingNonCancel {
                    getJson<DeezerListResponse<DeezerTrackDto>>("artist/$resolvedId/top") {
                        parameter("limit", 25)
                    }
                }
            }
            val albumsDeferred = async {
                runCatchingNonCancel {
                    getJson<DeezerListResponse<DeezerAlbumDto>>("artist/$resolvedId/albums") {
                        parameter("limit", 25)
                    }
                }
            }
            val relatedDeferred = async {
                runCatchingNonCancel {
                    getJson<DeezerListResponse<DeezerArtistDto>>("artist/$resolvedId/related") {
                        parameter("limit", 15)
                    }
                }
            }

            val artistDto = detailsDeferred.await()
            artistDto.error?.let { err ->
                return@coroutineScope AppResult.Failure(
                    DomainError.Network(err.message ?: "Deezer artist details error", err.code)
                )
            }
            val artist = mapArtist(artistDto)
            val topTracks = topDeferred.await()?.data.orEmpty().map { mapTrack(it) }
            val albums = albumsDeferred.await()?.data.orEmpty().map { mapAlbum(it, artist) }
            val related = relatedDeferred.await()?.data.orEmpty().map(::mapArtist)

            AppResult.Success(
                ArtistDetails(
                    artist = artist,
                    topTracks = topTracks,
                    albums = albums,
                    relatedArtists = related
                )
            )
        }
    }

    /** Fetches album metadata, genres, release year, and ordered track listing for [albumId]. */
    suspend fun getAlbumDetails(albumId: String): AppResult<AlbumDetails> = safeRequest {
        val dto = getJson<DeezerAlbumDto>("album/$albumId")
        dto.error?.let { err ->
            return@safeRequest AppResult.Failure(
                DomainError.Network(err.message ?: "Deezer album details error", err.code)
            )
        }
        val album = mapAlbum(dto)
        val genres = dto.genres?.data?.map { it.name.trim() }?.filter { it.isNotEmpty() }.orEmpty()
        val tracks = dto.tracks?.data.orEmpty().map { rawTrack ->
            val mapped = mapTrack(rawTrack, genres)
            mapped.copy(
                album = mapped.album.ifBlank { album.title },
                albumId = mapped.albumId.ifBlank { album.id },
                coverUrl = mapped.coverUrl.ifBlank { album.coverUrl },
                releaseYear = mapped.releaseYear ?: album.releaseYear
            )
        }
        AppResult.Success(AlbumDetails(album = album, tracks = tracks))
    }

    /** Retrieves radio/related tracks seeded by [trackId] up to [limit] items. */
    suspend fun getRelatedTracks(trackId: String, limit: Int = 15): AppResult<List<Track>> = safeRequest {
        val payload = getJson<DeezerListResponse<DeezerTrackDto>>("track/$trackId/radio") {
            parameter("limit", limit)
        }
        payload.error?.let { err ->
            return@safeRequest AppResult.Failure(
                DomainError.Network(err.message ?: "Deezer radio error", err.code)
            )
        }
        AppResult.Success(payload.data.map { mapTrack(it) }.take(limit))
    }

    private suspend fun fetchSingleGenreRow(genre: String): CategoryRow {
        val tracks = getGenreTracks(genre, limit = 30).getOrNull().orEmpty()
        val seenArtists = HashSet<String>()
        val uniqueTracks = tracks.filter { track ->
            val artistKey = track.artist.trim().lowercase()
            if (artistKey.isEmpty()) true else seenArtists.add(artistKey)
        }.take(CATEGORY_ROW_LIMIT)
        val id = genre.lowercase().replace(WHITESPACE_REGEX, "-").replace("'", "")
        return CategoryRow(id = id, title = genre, tracks = uniqueTracks)
    }

    private suspend fun resolveNumericArtistId(artistId: String, nameHint: String): String? {
        if (artistId.isNotBlank() && artistId.all { it.isDigit() }) {
            return artistId
        }
        val lookupTerm = nameHint.ifBlank { artistId }.trim()
        if (lookupTerm.isEmpty()) return null
        return searchArtists(lookupTerm, limit = 1, offset = 0).getOrNull()?.firstOrNull()?.id
    }

    private fun appendYearFilter(query: String, yearRange: IntRange?): String {
        if (yearRange == null) return query
        return if (yearRange.first == yearRange.last) {
            "$query year:${yearRange.first}"
        } else {
            "$query year:\"${yearRange.first}-${yearRange.last}\""
        }
    }

    private fun mapTrack(dto: DeezerTrackDto, genres: List<String> = emptyList()): Track {
        val cover = dto.album?.coverXl
            ?: dto.album?.coverBig
            ?: dto.album?.coverMedium
            ?: dto.artist?.pictureBig
            ?: ""
        return Track(
            id = dto.id.toString(),
            title = dto.title,
            artist = dto.artist?.name?.ifBlank { "Unknown Artist" } ?: "Unknown Artist",
            artistId = dto.artist?.id?.takeIf { it > 0L }?.toString().orEmpty(),
            album = dto.album?.title.orEmpty(),
            albumId = dto.album?.id?.takeIf { it > 0L }?.toString().orEmpty(),
            coverUrl = cover,
            durationSeconds = dto.duration.coerceAtLeast(0),
            previewUrl = dto.preview.orEmpty(),
            genres = genres,
            releaseYear = parseYear(dto.releaseDate ?: dto.album?.releaseDate),
            rank = dto.rank.coerceAtLeast(0)
        )
    }

    private fun mapArtist(dto: DeezerArtistDto): Artist = Artist(
        id = dto.id.toString(),
        name = dto.name,
        coverUrl = dto.pictureXl ?: dto.pictureBig ?: dto.pictureMedium ?: "",
        fanCount = dto.nbFan.coerceAtLeast(0L),
        albumCount = dto.nbAlbum.coerceAtLeast(0)
    )

    private fun mapAlbum(dto: DeezerAlbumDto, fallbackArtist: Artist? = null): Album = Album(
        id = dto.id.toString(),
        title = dto.title,
        artist = dto.artist?.name?.ifBlank { fallbackArtist?.name ?: "Unknown Artist" }
            ?: fallbackArtist?.name
            ?: "Unknown Artist",
        artistId = dto.artist?.id?.takeIf { it > 0L }?.toString() ?: fallbackArtist?.id.orEmpty(),
        coverUrl = dto.coverXl ?: dto.coverBig ?: dto.coverMedium ?: "",
        trackCount = dto.nbTracks.coerceAtLeast(0),
        releaseYear = parseYear(dto.releaseDate)
    )

    private fun parseYear(rawDate: String?): Int? =
        rawDate?.trim()?.take(4)?.toIntOrNull()

    private suspend inline fun <reified T> getJson(
        path: String,
        crossinline builder: io.ktor.client.request.HttpRequestBuilder.() -> Unit = {}
    ): T {
        val normalizedBase = baseUrl.trimEnd('/')
        val response = httpClient.get("$normalizedBase/$path") {
            builder()
        }
        if (!response.status.isSuccess()) {
            throw DeezerHttpException(response.status.value, "HTTP ${response.status.value} from Deezer")
        }
        val body = response.bodyAsText()
        return json.decodeFromString(body)
    }

    private suspend inline fun <T> runCatchingNonCancel(crossinline block: suspend () -> T): T? = try {
        block()
    } catch (cancel: CancellationException) {
        throw cancel
    } catch (_: Exception) {
        null
    }

    private suspend inline fun <T> safeRequest(crossinline block: suspend () -> AppResult<T>): AppResult<T> = try {
        block()
    } catch (cancel: CancellationException) {
        throw cancel
    } catch (http: DeezerHttpException) {
        AppResult.Failure(DomainError.Network(http.message ?: "Deezer HTTP error", http.statusCode))
    } catch (error: Exception) {
        AppResult.Failure(DomainError.Network(error.message ?: "Deezer request failed"))
    }

    private class DeezerHttpException(val statusCode: Int, message: String) : Exception(message)

    companion object {
        const val BASE_URL = "https://api.deezer.com"
        private const val CATEGORY_ROW_LIMIT = 12
        private val WHITESPACE_REGEX = Regex("\\s+")
        val GENRE_IDS: Map<String, Int> = mapOf(
            "Pop" to 132,
            "Rock" to 152,
            "Hip-Hop" to 116,
            "Electronic" to 106,
            "Hardcore" to 464,
            "R&B" to 165,
            "Jazz" to 129,
            "Dance" to 113,
            "Alternative" to 85
        )
    }
}
