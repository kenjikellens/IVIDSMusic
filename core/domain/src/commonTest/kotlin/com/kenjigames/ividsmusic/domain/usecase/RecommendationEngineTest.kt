package com.kenjigames.ividsmusic.domain.usecase

import com.kenjigames.ividsmusic.domain.model.Album
import com.kenjigames.ividsmusic.domain.model.AlbumDetails
import com.kenjigames.ividsmusic.domain.model.AppResult
import com.kenjigames.ividsmusic.domain.model.Artist
import com.kenjigames.ividsmusic.domain.model.ArtistDetails
import com.kenjigames.ividsmusic.domain.model.CategoryRow
import com.kenjigames.ividsmusic.domain.model.DomainError
import com.kenjigames.ividsmusic.domain.model.InterestScore
import com.kenjigames.ividsmusic.domain.model.ScoreEntityType
import com.kenjigames.ividsmusic.domain.model.SearchOverview
import com.kenjigames.ividsmusic.domain.model.Track
import com.kenjigames.ividsmusic.domain.port.MusicDiscoveryRepository
import com.kenjigames.ividsmusic.domain.port.ScoreRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * In-memory fake implementation of [ScoreRepository] for pure domain [RecommendationEngine] tests.
 */
private class FakeScoreRepository : ScoreRepository {
    private val store = LinkedHashMap<String, InterestScore>()
    private val flow = MutableStateFlow<List<InterestScore>>(emptyList())

    /** Exposes reactive updates to the in-memory score table. */
    override fun observeAllScores(): Flow<List<InterestScore>> = flow.asStateFlow()

    /** Returns all recorded [InterestScore] entries from the in-memory map. */
    override suspend fun getAllScores(): List<InterestScore> = store.values.toList()

    /** Returns the score entry for [entityKey], or `null` when absent. */
    override suspend fun getScore(entityKey: String): InterestScore? = store[entityKey]

    /** Persists [score] into the in-memory map and emits the updated list. */
    override suspend fun upsertScore(score: InterestScore): AppResult<Unit> {
        store[score.entityKey] = score
        flow.value = store.values.toList()
        return AppResult.Success(Unit)
    }

    /** Clears all stored entries and notifies observers. */
    override suspend fun clearAllScores(): AppResult<Unit> {
        store.clear()
        flow.value = emptyList()
        return AppResult.Success(Unit)
    }
}

/**
 * Configurable fake [MusicDiscoveryRepository] supplying deterministic track, artist, genre, and chart candidates.
 */
private class FakeDiscoveryRepository(
    private val trackDetailsById: Map<String, Track> = emptyMap(),
    private val relatedByTrackId: Map<String, List<Track>> = emptyMap(),
    private val artistTopById: Map<String, List<Track>> = emptyMap(),
    private val genreTracksByName: Map<String, List<Track>> = emptyMap(),
    private val chartTracks: List<Track> = emptyList()
) : MusicDiscoveryRepository {

    /** Returns empty search track list for stubbed discovery queries. */
    override suspend fun searchTracks(query: String, limit: Int, offset: Int, yearRange: IntRange?): AppResult<List<Track>> =
        AppResult.Success(emptyList())

    /** Returns empty artist list for stubbed discovery queries. */
    override suspend fun searchArtists(query: String, limit: Int, offset: Int): AppResult<List<Artist>> =
        AppResult.Success(emptyList())

    /** Returns empty album list for stubbed discovery queries. */
    override suspend fun searchAlbums(query: String, limit: Int, offset: Int, yearRange: IntRange?): AppResult<List<Album>> =
        AppResult.Success(emptyList())

    /** Returns empty search overview for stubbed queries. */
    override suspend fun searchOverview(query: String, limitPerSection: Int, yearRange: IntRange?): AppResult<SearchOverview> =
        AppResult.Success(SearchOverview(emptyList(), emptyList(), emptyList()))

    /** Returns empty genre chart shelves for stubbed queries. */
    override suspend fun getGenreCharts(genres: List<String>): AppResult<List<CategoryRow>> =
        AppResult.Success(emptyList())

    /** Returns configured [chartTracks] baseline candidates. */
    override suspend fun getChartTracks(limit: Int): AppResult<List<Track>> =
        AppResult.Success(chartTracks.take(limit))

    /** Returns stubbed detailed track metadata or [DomainError.StreamNotFound] when unknown. */
    override suspend fun getTrackDetails(trackId: String): AppResult<Track> =
        trackDetailsById[trackId]?.let { AppResult.Success(it) }
            ?: AppResult.Failure(DomainError.StreamNotFound("Not found: $trackId"))

    /** Returns configured artist top tracks inside [ArtistDetails]. */
    override suspend fun getArtistDetails(artistId: String, artistNameHint: String): AppResult<ArtistDetails> =
        AppResult.Success(
            ArtistDetails(
                artist = Artist(id = artistId, name = artistNameHint),
                topTracks = artistTopById[artistId].orEmpty(),
                albums = emptyList()
            )
        )

    /** Returns empty album details for stubbed calls. */
    override suspend fun getAlbumDetails(albumId: String): AppResult<AlbumDetails> =
        AppResult.Failure(DomainError.StreamNotFound("Not used"))

    /** Returns configured related tracks for [trackId]. */
    override suspend fun getRelatedTracks(trackId: String, limit: Int): AppResult<List<Track>> =
        AppResult.Success(relatedByTrackId[trackId].orEmpty().take(limit))

    /** Returns configured genre tracks for [genreName]. */
    override suspend fun getGenreTracks(genreName: String, limit: Int): AppResult<List<Track>> =
        AppResult.Success(genreTracksByName[genreName].orEmpty().take(limit))
}

/**
 * Verifies every rule from `docs/algorithm.md` inside [RecommendationEngine].
 */
class RecommendationEngineTest {

    private var clock = 1_000L

    /** Validates click, completion, like, dislike, early-skip threshold, and 0..100 clamping. */
    @Test
    fun scoringRulesClampToZeroAndHundredAndRespectThresholds() = runTest {
        val scoreRepo = FakeScoreRepository()
        val track = Track(
            id = "t1",
            title = "Neon Skyline",
            artist = "Kenji",
            artistId = "a1",
            genres = listOf("Electronic")
        )
        val engine = RecommendationEngine(scoreRepo, FakeDiscoveryRepository(), clockMs = { clock++ })

        // Click track: +1 track, +1 artist, +1 genre
        engine.recordTrackClick(track)
        assertEquals(1, scoreRepo.getScore("track:t1")?.score)
        assertEquals(1, scoreRepo.getScore("artist:a1")?.score)
        assertEquals(1, scoreRepo.getScore("genre:electronic")?.score)

        // Completion (>90%): +1 track
        engine.recordTrackCompletion("t1")
        assertEquals(2, scoreRepo.getScore("track:t1")?.score)

        // Like track: +3 track (total 5), +2 artist (total 3), +2 genre (total 3)
        engine.recordLikeTrack(track)
        assertEquals(5, scoreRepo.getScore("track:t1")?.score)
        assertEquals(3, scoreRepo.getScore("artist:a1")?.score)
        assertEquals(3, scoreRepo.getScore("genre:electronic")?.score)

        // Early skip <20s applies -1, >=20s does not
        engine.recordEarlySkip("t1", 19_999L)
        assertEquals(4, scoreRepo.getScore("track:t1")?.score)
        engine.recordEarlySkip("t1", 20_000L)
        assertEquals(4, scoreRepo.getScore("track:t1")?.score)

        // Dislike track: halves track (4 / 2 = 2), -2 artist (3 - 2 = 1), -1 genre (3 - 1 = 2)
        engine.recordDislikeTrack(track)
        assertEquals(2, scoreRepo.getScore("track:t1")?.score)
        assertEquals(1, scoreRepo.getScore("artist:a1")?.score)
        assertEquals(2, scoreRepo.getScore("genre:electronic")?.score)

        // Artist click +1 & artist like +3, plus upper bound clamp at 100
        engine.recordArtistClick("a1", "Kenji")
        engine.recordLikeArtist(Artist(id = "a1", name = "Kenji"))
        assertEquals(5, scoreRepo.getScore("artist:a1")?.score)

        repeat(40) {
            engine.recordLikeTrack(track)
        }
        assertEquals(100, scoreRepo.getScore("track:t1")?.score)
    }

    /** Verifies missing track genres are enriched lazily via discovery details before scoring. */
    @Test
    fun missingGenresAreEnrichedFromDiscoveryDetails() = runTest {
        val scoreRepo = FakeScoreRepository()
        val discovery = FakeDiscoveryRepository(
            trackDetailsById = mapOf(
                "t2" to Track(id = "t2", title = "Jazz Café", artist = "Blue", artistId = "a2", genres = listOf("Jazz", "Soul"))
            )
        )
        val engine = RecommendationEngine(scoreRepo, discovery, clockMs = { clock++ })

        engine.recordLikeTrack(Track(id = "t2", title = "Jazz Café", artist = "Blue", artistId = "a2"))
        assertEquals(1, scoreRepo.getScore("genre:jazz")?.score)
        assertEquals(1, scoreRepo.getScore("genre:soul")?.score)
    }

    /** Verifies cosine similarity ranks matching genre/artist candidates ahead of unrelated tracks and excludes saved items. */
    @Test
    fun recommendationsRankRelevantCandidatesFirstAndExcludeSavedTracks() = runTest {
        val scoreRepo = FakeScoreRepository()
        val seedTrack = Track(
            id = "seed",
            title = "Cyber Pulse",
            artist = "Daft",
            artistId = "art_daft",
            genres = listOf("Electronic"),
            releaseYear = 2022,
            rank = 800_000
        )
        val highMatch = Track(
            id = "match_1",
            title = "Midnight Grid",
            artist = "Daft",
            artistId = "art_daft",
            genres = listOf("Electronic"),
            releaseYear = 2023,
            rank = 750_000
        )
        val unrelated = Track(
            id = "other_1",
            title = "Country Road",
            artist = "Cowboy",
            artistId = "art_cowboy",
            genres = listOf("Country"),
            releaseYear = 1985,
            rank = 100_000
        )
        val savedAlready = Track(
            id = "saved_1",
            title = "Already Downloaded",
            artist = "Daft",
            artistId = "art_daft",
            genres = listOf("Electronic")
        )

        val discovery = FakeDiscoveryRepository(
            relatedByTrackId = mapOf("seed" to listOf(unrelated, highMatch, savedAlready)),
            artistTopById = mapOf("art_daft" to listOf(highMatch)),
            genreTracksByName = mapOf("electronic" to listOf(highMatch)),
            chartTracks = listOf(unrelated)
        )
        val engine = RecommendationEngine(scoreRepo, discovery, clockMs = { clock++ })

        val emptyFeed = engine.generateRecommendations().let { (it as AppResult.Success).data }
        assertFalse(emptyFeed.hasHistory)

        repeat(8) {
            engine.recordTrackClick(seedTrack)
            engine.recordLikeTrack(seedTrack)
        }

        val feed = engine.generateRecommendations(savedTrackIds = setOf("saved_1")).let { (it as AppResult.Success).data }
        assertTrue(feed.hasHistory)
        assertTrue(feed.discoveryMix.none { it.id == "saved_1" })
        assertEquals("match_1", feed.discoveryMix.firstOrNull()?.id)
        assertTrue(feed.tasteMatchTracks.any { it.id == "match_1" })
        assertTrue(feed.genreFusionTracks.any { it.id == "match_1" })
    }

    /** Verifies low positive scores (`1..14`) produce positive user-vector weights and rank matching tracks first. */
    @Test
    fun lowPositiveScoresProducePositiveSimilarityAndOutrankUnrelated() = runTest {
        val scoreRepo = FakeScoreRepository()
        val clicked = Track(
            id = "s1",
            title = "Fresh Track",
            artist = "NewArtist",
            artistId = "art_new",
            genres = listOf("Synthwave")
        )
        val matchingCandidate = Track(
            id = "m_new",
            title = "Neon Horizon",
            artist = "NewArtist",
            artistId = "art_new",
            genres = listOf("Synthwave")
        )
        val unrelated = Track(
            id = "u_other",
            title = "Banjo Picking",
            artist = "Other",
            artistId = "art_other",
            genres = listOf("Country")
        )
        val engine = RecommendationEngine(
            scoreRepo,
            FakeDiscoveryRepository(
                relatedByTrackId = mapOf("s1" to listOf(unrelated, matchingCandidate)),
                chartTracks = listOf(unrelated, matchingCandidate)
            ),
            clockMs = { clock }
        )

        engine.recordTrackClick(clicked)
        val userVector = engine.buildUserProfileVector(scoreRepo.getAllScores(), nowMs = clock)
        assertTrue((userVector["artist_art_new"] ?: 0.0) > 0.0)
        assertTrue((userVector["genre_synthwave"] ?: 0.0) > 0.0)

        val feed = (engine.generateRecommendations() as AppResult.Success).data
        assertEquals("m_new", feed.discoveryMix.firstOrNull()?.id)
    }

    /** Verifies recency decay downweights stale entries and active era/popularity axes populate from seeds. */
    @Test
    fun recencyDecayDownweightsOldEntriesAndDiscoveryMixCapsArtistDominance() = runTest {
        val nowMs = 60L * 86_400_000L
        val scoreRepo = FakeScoreRepository()
        val engine = RecommendationEngine(scoreRepo, FakeDiscoveryRepository(), clockMs = { nowMs })

        assertTrue(engine.recencyWeight(lastUpdatedMs = 0L, nowMs = nowMs) < 1.0001)
        assertTrue(engine.recencyWeight(lastUpdatedMs = nowMs - 21L * 86_400_000L, nowMs = nowMs) in 0.49..0.51)
        assertEquals(0.25, engine.recencyWeight(lastUpdatedMs = 1L, nowMs = nowMs), 0.0001)

        scoreRepo.upsertScore(
            InterestScore("genre:rock", ScoreEntityType.GENRE, "rock", "Rock", score = 10, lastUpdatedMs = 1L)
        )
        scoreRepo.upsertScore(
            InterestScore("genre:jazz", ScoreEntityType.GENRE, "jazz", "Jazz", score = 10, lastUpdatedMs = nowMs)
        )
        val seedTrack = Track(
            id = "seed_j",
            title = "Blue Note",
            artist = "Miles",
            artistId = "a_miles",
            genres = listOf("Jazz"),
            releaseYear = 2023,
            rank = 600_000
        )
        val vector = engine.buildUserProfileVector(scoreRepo.getAllScores(), nowMs = nowMs, seedTracks = listOf(seedTrack))
        assertTrue((vector["genre_jazz"] ?: 0.0) > (vector["genre_rock"] ?: 0.0) * 3.0)
        assertTrue((vector["era_recent"] ?: 0.0) > 0.0)
        assertTrue((vector["popularity"] ?: 0.0) > 0.0)

        val skewedPool = (1..8).map { i ->
            Track(id = "dom_$i", title = "Dom $i", artist = "TopArtist", artistId = "top_a", genres = listOf("Pop"))
        } + (1..4).map { i ->
            Track(id = "alt_$i", title = "Alt $i", artist = "AltArtist $i", artistId = "alt_$i", genres = listOf("Jazz"))
        }
        val mix = engine.diversifyDiscoveryMix(skewedPool, savedTrackIds = emptySet(), limit = 6)
        assertEquals(6, mix.size)
        assertEquals(2, mix.count { it.artistId == "top_a" })
    }
}
