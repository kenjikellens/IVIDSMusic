package com.kenjigames.ividsmusic.data.remote

import com.kenjigames.ividsmusic.data.remote.dto.ItunesItemDto
import com.kenjigames.ividsmusic.data.remote.dto.ItunesSearchResponse
import com.kenjigames.ividsmusic.domain.model.Album
import com.kenjigames.ividsmusic.domain.model.AlbumDetails
import com.kenjigames.ividsmusic.domain.model.AppResult
import com.kenjigames.ividsmusic.domain.model.Artist
import com.kenjigames.ividsmusic.domain.model.ArtistDetails
import com.kenjigames.ividsmusic.domain.model.DomainError
import com.kenjigames.ividsmusic.domain.model.Track
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException

/**
 * Ktor fallback client querying Apple iTunes Search and Lookup endpoints (`https://itunes.apple.com/`).
 * Parses `text/javascript` JSON bodies safely and upgrades 100px artwork thumbnails to 600px covers.
 */
class ItunesApiClient(
    private val httpClient: HttpClient,
    private val json: Json,
    private val baseUrl: String = BASE_URL
) {
    /** Searches iTunes song items matching [query] with offset pagination and optional [yearRange] filter. */
    suspend fun searchTracks(
        query: String,
        limit: Int = 24,
        offset: Int = 0,
        yearRange: IntRange? = null
    ): AppResult<List<Track>> = safeRequest {
        val response = executeSearch(term = query, entity = "song", limit = limit, offset = offset)
        val mapped = response.results
            .filter { it.trackId != null && !it.trackName.isNullOrBlank() }
            .map(::mapTrack)
            .let { tracks ->
                if (yearRange == null) tracks
                else tracks.filter { it.releaseYear == null || it.releaseYear in yearRange }
            }
        AppResult.Success(mapped)
    }

    /** Searches iTunes artist entries matching [query] with offset pagination. */
    suspend fun searchArtists(
        query: String,
        limit: Int = 24,
        offset: Int = 0
    ): AppResult<List<Artist>> = safeRequest {
        val response = executeSearch(term = query, entity = "musicArtist", limit = limit, offset = offset)
        val artists = response.results
            .filter { it.artistId != null && !it.artistName.isNullOrBlank() }
            .map(::mapArtist)
        AppResult.Success(artists)
    }

    /** Searches iTunes album entries matching [query] with optional [yearRange] filter and offset pagination. */
    suspend fun searchAlbums(
        query: String,
        limit: Int = 24,
        offset: Int = 0,
        yearRange: IntRange? = null
    ): AppResult<List<Album>> = safeRequest {
        val response = executeSearch(term = query, entity = "album", limit = limit, offset = offset)
        val albums = response.results
            .filter { it.collectionId != null && !it.collectionName.isNullOrBlank() }
            .map(::mapAlbum)
            .let { list ->
                if (yearRange == null) list
                else list.filter { it.releaseYear == null || it.releaseYear in yearRange }
            }
        AppResult.Success(albums)
    }

    /** Fetches a single track by iTunes [trackId] via `/lookup`. */
    suspend fun getTrackDetails(trackId: String): AppResult<Track> = safeRequest {
        val response = executeLookup(id = trackId, entity = "song")
        val item = response.results.firstOrNull { it.trackId != null }
            ?: return@safeRequest AppResult.Failure(DomainError.StreamNotFound("iTunes track not found: $trackId"))
        AppResult.Success(mapTrack(item))
    }

    /** Fetches artist top songs and albums via iTunes `/lookup` or name fallback. */
    suspend fun getArtistDetails(artistId: String, artistNameHint: String): AppResult<ArtistDetails> = safeRequest {
        val songsResponse = if (artistId.isNotBlank() && artistId.all { it.isDigit() }) {
            executeLookup(id = artistId, entity = "song", limit = 25)
        } else {
            executeSearch(term = artistNameHint.ifBlank { artistId }, entity = "song", limit = 25, offset = 0)
        }
        val albumsResponse = if (artistId.isNotBlank() && artistId.all { it.isDigit() }) {
            executeLookup(id = artistId, entity = "album", limit = 25)
        } else {
            executeSearch(term = artistNameHint.ifBlank { artistId }, entity = "album", limit = 25, offset = 0)
        }

        val tracks = songsResponse.results.filter { it.trackId != null }.map(::mapTrack)
        val albums = albumsResponse.results.filter { it.collectionId != null }.map(::mapAlbum)
        val artistItem = songsResponse.results.firstOrNull { it.artistName != null }
            ?: albumsResponse.results.firstOrNull { it.artistName != null }
        val artist = Artist(
            id = artistItem?.artistId?.toString() ?: artistId,
            name = artistItem?.artistName ?: artistNameHint.ifBlank { "Artist" },
            coverUrl = upgradeArtwork(artistItem?.artworkUrl100),
            genre = artistItem?.primaryGenreName.orEmpty(),
            albumCount = albums.size
        )
        AppResult.Success(ArtistDetails(artist = artist, topTracks = tracks, albums = albums))
    }

    /** Fetches album metadata and track listing by iTunes collection [albumId] via `/lookup`. */
    suspend fun getAlbumDetails(albumId: String): AppResult<AlbumDetails> = safeRequest {
        val response = executeLookup(id = albumId, entity = "song", limit = 50)
        val albumItem = response.results.firstOrNull { it.wrapperType == "collection" || it.collectionId != null }
            ?: return@safeRequest AppResult.Failure(DomainError.StreamNotFound("iTunes album not found: $albumId"))
        val album = mapAlbum(albumItem)
        val tracks = response.results.filter { it.trackId != null }.map(::mapTrack)
        AppResult.Success(AlbumDetails(album = album, tracks = tracks))
    }

    private suspend fun executeSearch(
        term: String,
        entity: String,
        limit: Int,
        offset: Int
    ): ItunesSearchResponse {
        val normalizedBase = baseUrl.trimEnd('/')
        val response = httpClient.get("$normalizedBase/search") {
            parameter("term", term.trim())
            parameter("entity", entity)
            parameter("limit", limit)
            parameter("offset", offset)
            parameter("media", "music")
        }
        if (!response.status.isSuccess()) {
            throw ItunesHttpException(response.status.value, "HTTP ${response.status.value} from iTunes")
        }
        return json.decodeFromString(response.bodyAsText())
    }

    private suspend fun executeLookup(
        id: String,
        entity: String,
        limit: Int? = null
    ): ItunesSearchResponse {
        val normalizedBase = baseUrl.trimEnd('/')
        val response = httpClient.get("$normalizedBase/lookup") {
            parameter("id", id)
            parameter("entity", entity)
            if (limit != null) parameter("limit", limit)
        }
        if (!response.status.isSuccess()) {
            throw ItunesHttpException(response.status.value, "HTTP ${response.status.value} from iTunes")
        }
        return json.decodeFromString(response.bodyAsText())
    }

    private fun mapTrack(dto: ItunesItemDto): Track = Track(
        id = (dto.trackId ?: 0L).toString(),
        title = dto.trackName.orEmpty(),
        artist = dto.artistName?.ifBlank { "Unknown Artist" } ?: "Unknown Artist",
        artistId = dto.artistId?.toString().orEmpty(),
        album = dto.collectionName.orEmpty(),
        albumId = dto.collectionId?.toString().orEmpty(),
        coverUrl = upgradeArtwork(dto.artworkUrl100),
        durationSeconds = ((dto.trackTimeMillis ?: 0L) / 1000L).toInt().coerceAtLeast(0),
        previewUrl = dto.previewUrl.orEmpty(),
        genres = listOfNotNull(dto.primaryGenreName?.takeIf { it.isNotBlank() }),
        releaseYear = parseYear(dto.releaseDate)
    )

    private fun mapArtist(dto: ItunesItemDto): Artist = Artist(
        id = (dto.artistId ?: 0L).toString(),
        name = dto.artistName.orEmpty(),
        coverUrl = upgradeArtwork(dto.artworkUrl100),
        genre = dto.primaryGenreName.orEmpty()
    )

    private fun mapAlbum(dto: ItunesItemDto): Album = Album(
        id = (dto.collectionId ?: 0L).toString(),
        title = dto.collectionName.orEmpty(),
        artist = dto.artistName?.ifBlank { "Unknown Artist" } ?: "Unknown Artist",
        artistId = dto.artistId?.toString().orEmpty(),
        coverUrl = upgradeArtwork(dto.artworkUrl100),
        trackCount = dto.trackCount ?: 0,
        releaseYear = parseYear(dto.releaseDate)
    )

    private fun upgradeArtwork(url: String?): String =
        url?.replace("100x100bb", "600x600bb") ?: ""

    private fun parseYear(rawDate: String?): Int? =
        rawDate?.trim()?.take(4)?.toIntOrNull()

    private suspend inline fun <T> safeRequest(crossinline block: suspend () -> AppResult<T>): AppResult<T> = try {
        block()
    } catch (cancel: CancellationException) {
        throw cancel
    } catch (http: ItunesHttpException) {
        AppResult.Failure(DomainError.Network(http.message ?: "iTunes HTTP error", http.statusCode))
    } catch (error: Exception) {
        AppResult.Failure(DomainError.Network(error.message ?: "iTunes request failed"))
    }

    private class ItunesHttpException(val statusCode: Int, message: String) : Exception(message)

    companion object {
        const val BASE_URL = "https://itunes.apple.com"
    }
}
