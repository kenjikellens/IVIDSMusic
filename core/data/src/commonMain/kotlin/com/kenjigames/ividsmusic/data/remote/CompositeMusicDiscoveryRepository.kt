package com.kenjigames.ividsmusic.data.remote

import com.kenjigames.ividsmusic.domain.model.Album
import com.kenjigames.ividsmusic.domain.model.AlbumDetails
import com.kenjigames.ividsmusic.domain.model.AppResult
import com.kenjigames.ividsmusic.domain.model.Artist
import com.kenjigames.ividsmusic.domain.model.ArtistDetails
import com.kenjigames.ividsmusic.domain.model.CategoryRow
import com.kenjigames.ividsmusic.domain.model.DomainError
import com.kenjigames.ividsmusic.domain.model.SearchOverview
import com.kenjigames.ividsmusic.domain.model.Track
import com.kenjigames.ividsmusic.domain.port.MusicDiscoveryRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Unified [MusicDiscoveryRepository] orchestrating Deezer primary calls, automatic iTunes fallback, and 15-minute TTL caching.
 * Thread-safe bounded LRU cache prevents redundant remote API requests across rapid screen switches.
 */
class CompositeMusicDiscoveryRepository(
    private val deezerClient: DeezerApiClient,
    private val itunesClient: ItunesApiClient,
    private val clockMs: () -> Long = { System.currentTimeMillis() },
    private val cacheTtlMs: Long = DEFAULT_CACHE_TTL_MS
) : MusicDiscoveryRepository {

    private val cacheMutex = Mutex()
    private val cache = object : LinkedHashMap<String, CacheEntry>(MAX_CACHE_ENTRIES + 1, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CacheEntry>?): Boolean =
            size > MAX_CACHE_ENTRIES
    }

    /** Searches songs via Deezer with automatic iTunes fallback and 15-minute TTL caching. */
    override suspend fun searchTracks(
        query: String,
        limit: Int,
        offset: Int,
        yearRange: IntRange?
    ): AppResult<List<Track>> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return AppResult.Failure(DomainError.Validation("Search query cannot be empty"))
        }
        val key = "tracks:$trimmed:$limit:$offset:${yearRange?.first}-${yearRange?.last}"
        return cached(key) {
            val primary = deezerClient.searchTracks(trimmed, limit, offset, yearRange)
            if (primary is AppResult.Success && primary.data.isNotEmpty()) {
                primary
            } else {
                val fallback = itunesClient.searchTracks(trimmed, limit, offset, yearRange)
                when {
                    primary is AppResult.Success -> primary
                    fallback is AppResult.Success -> fallback
                    else -> primary
                }
            }
        }
    }

    /** Searches artists via Deezer with automatic iTunes fallback and 15-minute TTL caching. */
    override suspend fun searchArtists(
        query: String,
        limit: Int,
        offset: Int
    ): AppResult<List<Artist>> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return AppResult.Failure(DomainError.Validation("Search query cannot be empty"))
        }
        val key = "artists:$trimmed:$limit:$offset"
        return cached(key) {
            val primary = deezerClient.searchArtists(trimmed, limit, offset)
            if (primary is AppResult.Success && primary.data.isNotEmpty()) {
                primary
            } else {
                val fallback = itunesClient.searchArtists(trimmed, limit, offset)
                when {
                    primary is AppResult.Success -> primary
                    fallback is AppResult.Success -> fallback
                    else -> primary
                }
            }
        }
    }

    /** Searches albums via Deezer with automatic iTunes fallback and 15-minute TTL caching. */
    override suspend fun searchAlbums(
        query: String,
        limit: Int,
        offset: Int,
        yearRange: IntRange?
    ): AppResult<List<Album>> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return AppResult.Failure(DomainError.Validation("Search query cannot be empty"))
        }
        val key = "albums:$trimmed:$limit:$offset:${yearRange?.first}-${yearRange?.last}"
        return cached(key) {
            val primary = deezerClient.searchAlbums(trimmed, limit, offset, yearRange)
            if (primary is AppResult.Success && primary.data.isNotEmpty()) {
                primary
            } else {
                val fallback = itunesClient.searchAlbums(trimmed, limit, offset, yearRange)
                when {
                    primary is AppResult.Success -> primary
                    fallback is AppResult.Success -> fallback
                    else -> primary
                }
            }
        }
    }

    /** Concurrently executes artist, song, and album searches for unfiltered Search results. */
    override suspend fun searchOverview(
        query: String,
        limitPerSection: Int,
        yearRange: IntRange?
    ): AppResult<SearchOverview> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return AppResult.Failure(DomainError.Validation("Search query cannot be empty"))
        }
        return coroutineScope {
            val artistsDef = async { searchArtists(trimmed, limitPerSection, 0) }
            val songsDef = async { searchTracks(trimmed, limitPerSection, 0, yearRange) }
            val albumsDef = async { searchAlbums(trimmed, limitPerSection, 0, yearRange) }

            val artistsRes = artistsDef.await()
            val songsRes = songsDef.await()
            val albumsRes = albumsDef.await()

            val firstError = listOf(artistsRes, songsRes, albumsRes)
                .filterIsInstance<AppResult.Failure>()
                .firstOrNull()?.error

            val artists = (artistsRes as? AppResult.Success)?.data.orEmpty()
            val songs = (songsRes as? AppResult.Success)?.data.orEmpty()
            val albums = (albumsRes as? AppResult.Success)?.data.orEmpty()

            if (artists.isEmpty() && songs.isEmpty() && albums.isEmpty() && firstError != null) {
                AppResult.Failure(firstError)
            } else {
                AppResult.Success(SearchOverview(artists = artists, songs = songs, albums = albums))
            }
        }
    }

    /** Loads curated genre chart rows with iTunes fallback per genre if Deezer is degraded. */
    override suspend fun getGenreCharts(genres: List<String>): AppResult<List<CategoryRow>> {
        val key = "genreCharts:${genres.joinToString(",")}"
        return cached(key) {
            val primary = deezerClient.getGenreCharts(genres)
            if (primary is AppResult.Success && primary.data.isNotEmpty()) {
                primary
            } else {
                val fallbackRows = coroutineScope {
                    genres.map { genre ->
                        async {
                            val tracks = (itunesClient.searchTracks(genre, limit = 12) as? AppResult.Success)
                                ?.data.orEmpty()
                            CategoryRow(
                                id = genre.lowercase().replace(WHITESPACE_REGEX, "-"),
                                title = genre,
                                tracks = tracks
                            )
                        }
                    }.awaitAll().filter { it.tracks.isNotEmpty() }
                }
                if (fallbackRows.isNotEmpty()) AppResult.Success(fallbackRows) else primary
            }
        }
    }

    /** Retrieves top global chart tracks with iTunes fallback when Deezer fails. */
    override suspend fun getChartTracks(limit: Int): AppResult<List<Track>> {
        val key = "chart:$limit"
        return cached(key) {
            val primary = deezerClient.getChartTracks(limit)
            if (primary is AppResult.Success && primary.data.isNotEmpty()) {
                primary
            } else {
                val fallback = itunesClient.searchTracks("top hits", limit = limit)
                if (fallback is AppResult.Success && fallback.data.isNotEmpty()) fallback else primary
            }
        }
    }

    /** Retrieves detailed track metadata with automatic iTunes lookup fallback. */
    override suspend fun getTrackDetails(trackId: String): AppResult<Track> {
        if (trackId.isBlank()) {
            return AppResult.Failure(DomainError.Validation("Track ID cannot be blank"))
        }
        return cached("track:$trackId") {
            val primary = deezerClient.getTrackDetails(trackId)
            if (primary is AppResult.Success) primary
            else itunesClient.getTrackDetails(trackId).let { fb -> if (fb is AppResult.Success) fb else primary }
        }
    }

    /** Retrieves artist profile, top tracks, albums, and peers with iTunes fallback. */
    override suspend fun getArtistDetails(
        artistId: String,
        artistNameHint: String
    ): AppResult<ArtistDetails> {
        if (artistId.isBlank() && artistNameHint.isBlank()) {
            return AppResult.Failure(DomainError.Validation("Artist ID or name hint is required"))
        }
        return cached("artist:$artistId:${artistNameHint.lowercase()}") {
            val primary = deezerClient.getArtistDetails(artistId, artistNameHint)
            if (primary is AppResult.Success) primary
            else itunesClient.getArtistDetails(artistId, artistNameHint).let { fb ->
                if (fb is AppResult.Success) fb else primary
            }
        }
    }

    /** Retrieves album details and tracklist with iTunes lookup fallback. */
    override suspend fun getAlbumDetails(albumId: String): AppResult<AlbumDetails> {
        if (albumId.isBlank()) {
            return AppResult.Failure(DomainError.Validation("Album ID cannot be blank"))
        }
        return cached("album:$albumId") {
            val primary = deezerClient.getAlbumDetails(albumId)
            if (primary is AppResult.Success) primary
            else itunesClient.getAlbumDetails(albumId).let { fb -> if (fb is AppResult.Success) fb else primary }
        }
    }

    /** Retrieves radio/related tracks for [trackId], falling back to track artist search when empty. */
    override suspend fun getRelatedTracks(trackId: String, limit: Int): AppResult<List<Track>> {
        if (trackId.isBlank()) {
            return AppResult.Failure(DomainError.Validation("Track ID cannot be blank"))
        }
        return cached("related:$trackId:$limit") {
            val primary = deezerClient.getRelatedTracks(trackId, limit)
            if (primary is AppResult.Success && primary.data.isNotEmpty()) {
                primary
            } else {
                val track = (getTrackDetails(trackId) as? AppResult.Success)?.data
                if (track != null && track.artist.isNotBlank()) {
                    searchTracks(track.artist, limit = limit)
                } else {
                    primary
                }
            }
        }
    }

    /** Retrieves genre chart tracks with automatic iTunes search fallback. */
    override suspend fun getGenreTracks(genreName: String, limit: Int): AppResult<List<Track>> {
        if (genreName.isBlank()) {
            return AppResult.Failure(DomainError.Validation("Genre name cannot be blank"))
        }
        return cached("genre:$genreName:$limit") {
            val primary = deezerClient.getGenreTracks(genreName, limit)
            if (primary is AppResult.Success && primary.data.isNotEmpty()) {
                primary
            } else {
                val fallback = itunesClient.searchTracks(genreName, limit = limit)
                if (fallback is AppResult.Success && fallback.data.isNotEmpty()) fallback else primary
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend inline fun <T> cached(
        key: String,
        crossinline loader: suspend () -> AppResult<T>
    ): AppResult<T> {
        val now = clockMs()
        val hit = cacheMutex.withLock {
            val entry = cache[key]
            if (entry != null && (now - entry.storedAtMs) < cacheTtlMs) {
                entry.value as? T
            } else {
                cache.remove(key)
                null
            }
        }
        if (hit != null) return AppResult.Success(hit)

        val result = loader()
        if (result is AppResult.Success) {
            cacheMutex.withLock {
                cache[key] = CacheEntry(value = result.data as Any, storedAtMs = clockMs())
            }
        }
        return result
    }

    private data class CacheEntry(
        val value: Any,
        val storedAtMs: Long
    )

    companion object {
        const val DEFAULT_CACHE_TTL_MS = 15 * 60 * 1000L
        private const val MAX_CACHE_ENTRIES = 200
        private val WHITESPACE_REGEX = Regex("\\s+")
    }
}
