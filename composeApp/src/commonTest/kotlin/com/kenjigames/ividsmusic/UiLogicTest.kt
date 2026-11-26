package com.kenjigames.ividsmusic

import androidx.compose.ui.unit.dp
import com.kenjigames.ividsmusic.domain.model.CategoryRow
import com.kenjigames.ividsmusic.domain.model.LayoutMode
import com.kenjigames.ividsmusic.domain.model.Track
import com.kenjigames.ividsmusic.domain.model.TrackSortOption
import com.kenjigames.ividsmusic.ui.components.formatByteSize
import com.kenjigames.ividsmusic.ui.components.formatDurationMillis
import com.kenjigames.ividsmusic.ui.components.formatDurationSeconds
import com.kenjigames.ividsmusic.ui.components.formatPlaylistTotalDuration
import com.kenjigames.ividsmusic.ui.i18n.AppStrings
import com.kenjigames.ividsmusic.ui.i18n.SupportedLanguage
import com.kenjigames.ividsmusic.ui.navigation.AppNavigator
import com.kenjigames.ividsmusic.ui.navigation.AppRoute
import com.kenjigames.ividsmusic.ui.screens.home.HomeQuickPick
import com.kenjigames.ividsmusic.ui.screens.home.HomeUiState
import com.kenjigames.ividsmusic.ui.screens.home.buildHomeFeedProjection
import com.kenjigames.ividsmusic.ui.screens.library.sortAndFilterTracks
import com.kenjigames.ividsmusic.ui.shell.ShellMode
import com.kenjigames.ividsmusic.ui.shell.resolveShellMode
import com.kenjigames.ividsmusic.ui.theme.IvidsColorTokens
import com.kenjigames.ividsmusic.ui.theme.clampAccentContrast
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Verifies pure presentation helpers including all 16 locales, RTL detection, backstack
 * navigation, shell mode breakpoints, tabular duration formatting, and accent contrast clamping.
 */
class UiLogicTest {

    /**
     * Ensures all 16 supported language codes resolve deterministically and Arabic marks RTL.
     */
    @Test
    fun supportedLanguagesCoverAll16Locales() {
        assertEquals(16, SupportedLanguage.entries.size)
        assertTrue(SupportedLanguage.fromCode("ar").isRtl)
        assertFalse(SupportedLanguage.fromCode("en").isRtl)
        assertEquals(SupportedLanguage.NL, SupportedLanguage.fromCode("nl"))
        assertEquals(SupportedLanguage.EN, SupportedLanguage.fromCode("unknown"))
    }

    /**
     * Ensures [AppStrings] interpolates template placeholders and falls back cleanly through English.
     */
    @Test
    fun appStringsFormatsAndFallsBackToEnglish() {
        val strings = AppStrings(
            language = SupportedLanguage.NL,
            entries = mapOf("home" to "Startpagina"),
            englishFallback = mapOf("albums_from_artist" to "Albums from {artist}")
        )
        assertEquals("Startpagina", strings["home"])
        assertEquals("Albums from Daft Punk", strings.format("albums_from_artist", "artist" to "Daft Punk"))
    }

    /**
     * Verifies [AppNavigator] deduplicates top routes, switches root tabs cleanly, and pops backstack.
     */
    @Test
    fun navigatorManagesBackstackAndSingleTop() {
        val nav = AppNavigator()
        nav.navigateTo(AppRoute.Search("rock"))
        nav.navigateTo(AppRoute.Search("rock"), singleTop = true)
        assertEquals(2, nav.backStack.value.size)

        assertTrue(nav.navigateBack())
        assertEquals(AppRoute.Home, nav.currentRoute)
        assertFalse(nav.navigateBack())

        nav.navigateTo(AppRoute.AlbumDetail("123"))
        nav.switchRootTab(AppRoute.Library)
        assertEquals(listOf(AppRoute.Library), nav.backStack.value)
    }

    /**
     * Confirms [resolveShellMode] chooses MOBILE `<600dp`, DESKTOP `>=600dp`, TV on Leanback, and obeys overrides.
     */
    @Test
    fun shellModeResolvesResponsiveBreakpointsAndOverrides() {
        assertEquals(ShellMode.MOBILE, resolveShellMode(LayoutMode.AUTO, isTvDevice = false, widthDp = 420.dp))
        assertEquals(ShellMode.DESKTOP, resolveShellMode(LayoutMode.AUTO, isTvDevice = false, widthDp = 960.dp))
        assertEquals(ShellMode.TV, resolveShellMode(LayoutMode.AUTO, isTvDevice = true, widthDp = 960.dp))
        assertEquals(ShellMode.MOBILE, resolveShellMode(LayoutMode.MOBILE, isTvDevice = false, widthDp = 1280.dp))
        assertEquals(ShellMode.TV, resolveShellMode(LayoutMode.TV, isTvDevice = false, widthDp = 1280.dp))
    }

    /**
     * Verifies duration formatting and dark-stage accent color contrast clamping.
     */
    @Test
    fun formatsDurationAndClampsDarkAccent() {
        assertEquals("3:05", formatDurationSeconds(185))
        assertEquals("1:01:05", formatDurationSeconds(3665))
        assertEquals("0:42", formatDurationMillis(42_500L))
        assertEquals("512 B", formatByteSize(512L))
        assertEquals("2.0 MB", formatByteSize(2L * 1024L * 1024L))
        assertEquals("45m 0s", formatPlaylistTotalDuration(45 * 60))
        assertEquals("1h 15m", formatPlaylistTotalDuration(75 * 60))
        assertEquals(IvidsColorTokens.PrimaryCyan, clampAccentContrast(0xFF050505.toInt(), IvidsColorTokens.PrimaryCyan))
    }

    /**
     * Verifies library track sorting across Title, Artist, Duration, text query, and Offline-only filtering.
     */
    @Test
    fun sortAndFilterTracksOrdersAndFiltersOffline() {
        val tracks = listOf(
            Track(id = "1", title = "Zeta", artist = "Beta", durationSeconds = 200, isDownloaded = false),
            Track(id = "2", title = "Alpha", artist = "Gamma", durationSeconds = 120, isDownloaded = true),
            Track(id = "3", title = "Mid", artist = "Alpha", durationSeconds = 300, isDownloaded = false)
        )
        assertEquals(listOf("2", "3", "1"), sortAndFilterTracks(tracks, "", TrackSortOption.TITLE, false, emptySet()).map { it.id })
        assertEquals(listOf("3", "1", "2"), sortAndFilterTracks(tracks, "", TrackSortOption.ARTIST, false, emptySet()).map { it.id })
        assertEquals(listOf("3", "1", "2"), sortAndFilterTracks(tracks, "", TrackSortOption.DURATION, false, emptySet()).map { it.id })
        assertEquals(listOf("2", "3"), sortAndFilterTracks(tracks, "", TrackSortOption.TITLE, true, setOf("3")).map { it.id })
        assertEquals(listOf("1"), sortAndFilterTracks(tracks, "beta", TrackSortOption.RECENT, false, emptySet()).map { it.id })
    }

    /**
     * Verifies Home feed projection fills all 6 quick-pick slots, heals blank history cover URLs from live
     * charts, selects a distinct Hero track, and excludes already-displayed quick-pick tracks from Recently Listened.
     */
    @Test
    fun homeFeedProjectionDeduplicatesQuickPicksHeroAndRecent() {
        val popTracks = (1..4).map { idx ->
            Track(id = "pop_$idx", title = "Pop $idx", artist = "Artist $idx", coverUrl = "https://img/$idx.jpg")
        }
        val rockTracks = (1..4).map { idx ->
            Track(id = "rock_$idx", title = "Rock $idx", artist = "Band $idx", coverUrl = "https://img/r$idx.jpg")
        }
        val staleRecent = listOf(
            Track(id = "pop_1", title = "Pop 1", artist = "Artist 1", coverUrl = ""),
            Track(id = "r_custom", title = "Custom", artist = "Solo", coverUrl = "https://img/c.jpg")
        )
        val projection = buildHomeFeedProjection(
            state = HomeUiState(
                heroTrack = popTracks.first(),
                continueListening = staleRecent,
                genreSections = listOf(
                    CategoryRow("pop", "Pop", popTracks),
                    CategoryRow("rock", "Rock", rockTracks)
                ),
                isLoading = false
            ),
            playlists = emptyList(),
            likedTracks = emptyList()
        )
        assertEquals(6, projection.quickPicks.size)
        val pickTracks = projection.quickPicks.filterIsInstance<HomeQuickPick.TrackPick>().map { it.track }
        assertEquals("https://img/1.jpg", pickTracks.first().coverUrl)
        assertTrue(projection.remainingRecent.isEmpty())
        val hero = assertNotNull(projection.heroTrack)
        assertFalse(pickTracks.any { it.id == hero.id })

        val overflowRecent = staleRecent + (1..6).map { idx ->
            Track(id = "extra_$idx", title = "Extra $idx", artist = "Solo $idx", coverUrl = "https://img/e$idx.jpg")
        }
        val overflowProjection = buildHomeFeedProjection(
            state = HomeUiState(
                heroTrack = popTracks.first(),
                continueListening = overflowRecent,
                genreSections = listOf(CategoryRow("pop", "Pop", popTracks)),
                isLoading = false
            ),
            playlists = emptyList(),
            likedTracks = emptyList()
        )
        assertEquals(6, overflowProjection.quickPicks.size)
        assertEquals(2, overflowProjection.remainingRecent.size)
        assertEquals(listOf("extra_5", "extra_6"), overflowProjection.remainingRecent.map { it.id })
    }
}
