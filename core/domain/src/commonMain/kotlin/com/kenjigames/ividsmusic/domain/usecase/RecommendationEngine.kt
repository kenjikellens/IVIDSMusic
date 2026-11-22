package com.kenjigames.ividsmusic.domain.usecase

import com.kenjigames.ividsmusic.domain.model.AppResult
import com.kenjigames.ividsmusic.domain.model.Artist
import com.kenjigames.ividsmusic.domain.model.DomainError
import com.kenjigames.ividsmusic.domain.model.InterestScore
import com.kenjigames.ividsmusic.domain.model.RecommendationFeed
import com.kenjigames.ividsmusic.domain.model.ScoreEntityType
import com.kenjigames.ividsmusic.domain.model.Track
import com.kenjigames.ividsmusic.domain.model.getOrNull
import com.kenjigames.ividsmusic.domain.port.MusicDiscoveryRepository
import com.kenjigames.ividsmusic.domain.port.ScoreRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Pure domain engine implementing IVIDS Music's local scoring rules (`0..100` clamped) and cosine-similarity recommendations.
 * Coordinates [ScoreRepository] mutations and [MusicDiscoveryRepository] candidate expansions without external telemetry.
 */
class RecommendationEngine(
    private val scoreRepository: ScoreRepository,
    private val discoveryRepository: MusicDiscoveryRepository,
    private val clockMs: () -> Long
) {
    /**
     * Applies listen scoring for clicking a track: `+1` track, `+1` artist, and `+1` distributed across track genres.
     * Mutates [ScoreRepository] and lazily enriches missing genres via [MusicDiscoveryRepository.getTrackDetails].
     */
    suspend fun recordTrackClick(track: Track) {
        if (track.id.isBlank()) return
        adjustScore(ScoreEntityType.TRACK, track.id, track.title, delta = 1)
        if (track.artistId.isNotBlank()) {
            adjustScore(ScoreEntityType.ARTIST, track.artistId, track.artist, delta = 1)
        }
        val genres = resolveGenresForTrack(track)
        applyGenreDelta(genres, totalDelta = 1)
    }

    /**
     * Awards a `+1` completion bonus to [trackId] when playback passes 90% of the track's duration.
     * Mutates the track's [InterestScore] clamped to `0..100` inside [ScoreRepository].
     */
    suspend fun recordTrackCompletion(trackId: String) {
        if (trackId.isBlank()) return
        adjustScore(ScoreEntityType.TRACK, trackId, displayName = trackId, delta = 1)
    }

    /**
     * Records a `+1` interest increment when a user opens an artist's profile or page.
     * Mutates the artist entry inside [ScoreRepository] clamped to `0..100`.
     */
    suspend fun recordArtistClick(artistId: String, artistName: String) {
        if (artistId.isBlank()) return
        adjustScore(ScoreEntityType.ARTIST, artistId, artistName.ifBlank { artistId }, delta = 1)
    }

    /**
     * Applies like scoring for [track]: `+3` to the track and `+2` distributed across its genres.
     * Mutates [ScoreRepository] and resolves genres lazily if missing from [track].
     */
    suspend fun recordLikeTrack(track: Track) {
        if (track.id.isBlank()) return
        adjustScore(ScoreEntityType.TRACK, track.id, track.title, delta = 3)
        val genres = resolveGenresForTrack(track)
        applyGenreDelta(genres, totalDelta = 2)
    }

    /**
     * Awards `+3` interest points when the user likes [artist].
     * Mutates the artist's entry in [ScoreRepository] clamped to `0..100`.
     */
    suspend fun recordLikeArtist(artist: Artist) {
        if (artist.id.isBlank()) return
        adjustScore(ScoreEntityType.ARTIST, artist.id, artist.name, delta = 3)
    }

    /**
     * Applies dislike penalties for [track]: halves the track's score (`score / 2`), subtracts `2` from its artist, and subtracts `1` from genres.
     * Mutates track, artist, and genre entries inside [ScoreRepository] clamped to `0..100`.
     */
    suspend fun recordDislikeTrack(track: Track) {
        if (track.id.isBlank()) return
        halveScore(ScoreEntityType.TRACK, track.id, track.title)
        if (track.artistId.isNotBlank()) {
            adjustScore(ScoreEntityType.ARTIST, track.artistId, track.artist, delta = -2)
        }
        val genres = resolveGenresForTrack(track)
        applyGenreDelta(genres, totalDelta = -1)
    }

    /**
     * Halves [artist]'s existing local interest score (`score / 2`) when explicitly disliked.
     * Mutates the artist entry in [ScoreRepository] clamped to `0..100`.
     */
    suspend fun recordDislikeArtist(artist: Artist) {
        if (artist.id.isBlank()) return
        halveScore(ScoreEntityType.ARTIST, artist.id, artist.name)
    }

    /**
     * Penalizes [trackId] by `-1` point only when skipped earlier than 20 seconds (`listenedMs in 0 until 20_000L`).
     * Leaves [ScoreRepository] untouched when listening duration meets or exceeds the 20-second threshold.
     */
    suspend fun recordEarlySkip(trackId: String, listenedMs: Long) {
        if (trackId.isBlank() || listenedMs < 0L || listenedMs >= EARLY_SKIP_THRESHOLD_MS) return
        adjustScore(ScoreEntityType.TRACK, trackId, displayName = trackId, delta = -1)
    }

    /**
     * Generates personalized Discovery Mix, Genre Fusion, and Taste Match rows from local scores and remote expansions.
     * Queries top 3–5 tracks/artists and top 3 genres in parallel, ranking candidates by cosine similarity against the user taste vector.
     */
    suspend fun generateRecommendations(
        savedTrackIds: Set<String> = emptySet()
    ): AppResult<RecommendationFeed> = try {
        val allScores = scoreRepository.getAllScores()
        val topTracks = allScores
            .filter { it.entityType == ScoreEntityType.TRACK && it.score > 0 }
            .sortedByDescending { it.score }
            .take(MAX_SEED_ITEMS)
        val topArtists = allScores
            .filter { it.entityType == ScoreEntityType.ARTIST && it.score > 0 }
            .sortedByDescending { it.score }
            .take(MAX_SEED_ITEMS)
        val topGenres = allScores
            .filter { it.entityType == ScoreEntityType.GENRE && it.score > 0 }
            .sortedByDescending { it.score }
            .take(MAX_GENRE_SEEDS)

        if (topTracks.isEmpty() && topArtists.isEmpty()) {
            AppResult.Success(
                RecommendationFeed(
                    discoveryMix = emptyList(),
                    genreFusionTitle = null,
                    genreFusionTracks = emptyList(),
                    tasteMatchTitle = null,
                    tasteMatchTracks = emptyList(),
                    hasHistory = false
                )
            )
        } else {
            val candidatePool = collectCandidates(topTracks, topArtists, topGenres)
            val trackScoreMap = allScores
                .filter { it.entityType == ScoreEntityType.TRACK }
                .associate { it.entityId to it.score }
            val userVector = buildUserProfileVector(allScores)

            val scoredPool = candidatePool
                .map { candidate ->
                    val similarity = calculateCosineSimilarity(userVector, createTrackVector(candidate))
                    val recordedScore = trackScoreMap[candidate.id]
                    val adjusted = if (recordedScore != null && recordedScore == 0) {
                        similarity - SKIP_PENALTY_WEIGHT
                    } else {
                        similarity
                    }
                    candidate to adjusted
                }
                .sortedByDescending { it.second }
                .map { it.first }

            val discoveryMix = scoredPool
                .filterNot { it.id in savedTrackIds }
                .take(DISCOVERY_MIX_LIMIT)

            val primaryGenres = topGenres.take(2)
            val genreFusionTitle = when {
                primaryGenres.isEmpty() -> null
                primaryGenres.size == 1 -> "${primaryGenres[0].displayName} Fusion"
                else -> "${primaryGenres[0].displayName} & ${primaryGenres[1].displayName} Fusion"
            }
            val genreFusionTracks = if (primaryGenres.isEmpty()) {
                emptyList()
            } else {
                val targets = primaryGenres.map { it.displayName.lowercase() }
                scoredPool.filter { track ->
                    track.genres.any { genre ->
                        targets.any { target -> genre.lowercase().contains(target) }
                    }
                }.take(SECTION_LIMIT)
            }

            val primaryArtists = topArtists.take(2)
            val tasteMatchTitle = when {
                primaryArtists.isEmpty() -> null
                primaryArtists.size == 1 -> "More from ${primaryArtists[0].displayName}"
                else -> "Best of ${primaryArtists[0].displayName} & ${primaryArtists[1].displayName}"
            }
            val tasteMatchTracks = if (primaryArtists.isEmpty()) {
                emptyList()
            } else {
                val targetIds = primaryArtists.map { it.entityId }.toSet()
                val targetNames = primaryArtists.map { it.displayName.lowercase() }.toSet()
                scoredPool.filter { track ->
                    track.artistId in targetIds || track.artist.lowercase() in targetNames
                }.take(SECTION_LIMIT)
            }

            AppResult.Success(
                RecommendationFeed(
                    discoveryMix = discoveryMix,
                    genreFusionTitle = genreFusionTitle,
                    genreFusionTracks = genreFusionTracks,
                    tasteMatchTitle = tasteMatchTitle,
                    tasteMatchTracks = tasteMatchTracks,
                    hasHistory = true
                )
            )
        }
    } catch (cancel: CancellationException) {
        throw cancel
    } catch (error: Exception) {
        AppResult.Failure(
            DomainError.Unknown(
                message = error.message ?: "Failed to generate recommendations",
                causeName = error::class.simpleName
            )
        )
    }

    /**
     * Constructs a Euclidean-normalized sparse feature vector over artist, genre, release-era, and popularity axes.
     * Pure mathematical transform with zero side effects used for cosine-similarity ranking.
     */
    fun createTrackVector(track: Track): Map<String, Double> {
        val raw = LinkedHashMap<String, Double>()
        if (track.artistId.isNotBlank()) {
            raw["artist_${track.artistId}"] = 1.0
        } else if (track.artist.isNotBlank()) {
            raw["artist_${track.artist.trim().lowercase()}"] = 1.0
        }

        track.genres
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .forEach { genre -> raw["genre_$genre"] = 1.0 }

        track.releaseYear?.let { year ->
            val eraKey = when {
                year < 1990 -> "era_classic"
                year < 2005 -> "era_90s_00s"
                year < 2018 -> "era_modern"
                else -> "era_recent"
            }
            raw[eraKey] = 1.0
        }

        raw["popularity"] = if (track.rank > 0) {
            (track.rank / 1_000_000.0).coerceIn(0.0, 1.0)
        } else {
            0.5
        }

        val magnitude = sqrt(raw.values.sumOf { it * it })
        if (magnitude <= 0.0) return raw
        return raw.mapValues { (_, weight) -> weight / magnitude }
    }

    /**
     * Projects stored genre and artist [InterestScore] entries into a sparse user taste coordinate vector.
     * Centers genre weights around score `10` and artist weights around score `15`.
     */
    fun buildUserProfileVector(scores: List<InterestScore>): Map<String, Double> {
        val userVector = LinkedHashMap<String, Double>()
        for (entry in scores) {
            when (entry.entityType) {
                ScoreEntityType.GENRE -> {
                    val key = "genre_${entry.entityId.trim().lowercase()}"
                    userVector[key] = (userVector[key] ?: 0.0) + ((entry.score - 10) / 10.0)
                }
                ScoreEntityType.ARTIST -> {
                    val key = "artist_${entry.entityId}"
                    userVector[key] = (userVector[key] ?: 0.0) + ((entry.score - 15) / 10.0)
                }
                ScoreEntityType.TRACK -> Unit
            }
        }
        return userVector
    }

    /**
     * Computes angular cosine similarity (`-1.0..1.0`) between two sparse feature vectors [vectorA] and [vectorB].
     * Returns `0.0` when either vector has zero magnitude.
     */
    fun calculateCosineSimilarity(
        vectorA: Map<String, Double>,
        vectorB: Map<String, Double>
    ): Double {
        if (vectorA.isEmpty() || vectorB.isEmpty()) return 0.0
        var dot = 0.0
        var sumSqA = 0.0
        var sumSqB = 0.0
        for ((key, valA) in vectorA) {
            sumSqA += valA * valA
            val valB = vectorB[key] ?: 0.0
            dot += valA * valB
        }
        for (valB in vectorB.values) {
            sumSqB += valB * valB
        }
        val denom = sqrt(sumSqA) * sqrt(sumSqB)
        return if (denom <= 0.0) 0.0 else dot / denom
    }

    private suspend fun collectCandidates(
        topTracks: List<InterestScore>,
        topArtists: List<InterestScore>,
        topGenres: List<InterestScore>
    ): List<Track> = coroutineScope {
        val relatedJobs = topTracks.map { seed ->
            async { discoveryRepository.getRelatedTracks(seed.entityId, limit = 10).getOrNull().orEmpty() }
        }
        val artistJobs = topArtists.map { seed ->
            async {
                discoveryRepository.getArtistDetails(seed.entityId, seed.displayName)
                    .getOrNull()?.topTracks.orEmpty()
            }
        }
        val genreJobs = topGenres.map { seed ->
            async {
                val tracks = discoveryRepository.getGenreTracks(seed.displayName, limit = 15)
                    .getOrNull().orEmpty()
                tracks.map { track ->
                    if (track.genres.isEmpty()) track.copy(genres = listOf(seed.displayName)) else track
                }
            }
        }
        val chartJob = async { discoveryRepository.getChartTracks(limit = 20).getOrNull().orEmpty() }

        val combined = buildList {
            addAll(chartJob.await())
            relatedJobs.awaitAll().forEach(::addAll)
            artistJobs.awaitAll().forEach(::addAll)
            genreJobs.awaitAll().forEach(::addAll)
        }
        combined.distinctBy { it.id }
    }

    private suspend fun resolveGenresForTrack(track: Track): List<String> {
        if (track.genres.isNotEmpty()) return track.genres
        return discoveryRepository.getTrackDetails(track.id).getOrNull()?.genres.orEmpty()
    }

    private suspend fun applyGenreDelta(genres: List<String>, totalDelta: Int) {
        val normalized = genres.map { it.trim() }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }
        if (normalized.isEmpty() || totalDelta == 0) return
        val absPerGenre = maxOf(1, abs(totalDelta) / normalized.size)
        val delta = if (totalDelta > 0) absPerGenre else -absPerGenre
        for (genre in normalized) {
            adjustScore(ScoreEntityType.GENRE, genre.lowercase(), genre, delta)
        }
    }

    private suspend fun adjustScore(
        type: ScoreEntityType,
        entityId: String,
        displayName: String,
        delta: Int
    ) {
        val key = buildEntityKey(type, entityId)
        val existing = scoreRepository.getScore(key)
        val currentScore = existing?.score ?: 0
        val nextScore = (currentScore + delta).coerceIn(MIN_SCORE, MAX_SCORE)
        val resolvedName = displayName.takeIf { it.isNotBlank() && it != entityId }
            ?: existing?.displayName
            ?: displayName
        scoreRepository.upsertScore(
            InterestScore(
                entityKey = key,
                entityType = type,
                entityId = entityId,
                displayName = resolvedName,
                score = nextScore,
                lastUpdatedMs = clockMs()
            )
        )
    }

    private suspend fun halveScore(
        type: ScoreEntityType,
        entityId: String,
        displayName: String
    ) {
        val key = buildEntityKey(type, entityId)
        val existing = scoreRepository.getScore(key)
        val currentScore = existing?.score ?: 0
        val nextScore = (currentScore / 2).coerceIn(MIN_SCORE, MAX_SCORE)
        val resolvedName = displayName.takeIf { it.isNotBlank() } ?: existing?.displayName ?: entityId
        scoreRepository.upsertScore(
            InterestScore(
                entityKey = key,
                entityType = type,
                entityId = entityId,
                displayName = resolvedName,
                score = nextScore,
                lastUpdatedMs = clockMs()
            )
        )
    }

    private fun buildEntityKey(type: ScoreEntityType, entityId: String): String = when (type) {
        ScoreEntityType.TRACK -> "track:$entityId"
        ScoreEntityType.ARTIST -> "artist:$entityId"
        ScoreEntityType.GENRE -> "genre:${entityId.lowercase()}"
    }

    companion object {
        const val MIN_SCORE = 0
        const val MAX_SCORE = 100
        const val EARLY_SKIP_THRESHOLD_MS = 20_000L
        private const val MAX_SEED_ITEMS = 5
        private const val MAX_GENRE_SEEDS = 3
        private const val DISCOVERY_MIX_LIMIT = 12
        private const val SECTION_LIMIT = 10
        private const val SKIP_PENALTY_WEIGHT = 0.4
    }
}
