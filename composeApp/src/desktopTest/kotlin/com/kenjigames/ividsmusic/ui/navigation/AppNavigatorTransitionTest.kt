package com.kenjigames.ividsmusic.ui.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppNavigatorTransitionTest {

    @Test
    fun navigationMethodsEmitExpectedTransitionTypes() {
        val navigator = AppNavigator(AppRoute.Home)
        assertEquals(NavTransitionType.NONE, navigator.transitionType.value)

        navigator.navigateTo(AppRoute.SongDetail("t1"))
        assertEquals(NavTransitionType.PUSH, navigator.transitionType.value)
        assertEquals(2, navigator.backStack.value.size)

        // Duplicate singleTop push does not change stack or transition
        navigator.switchRootTab(AppRoute.Search())
        assertEquals(NavTransitionType.TAB, navigator.transitionType.value)
        assertEquals(1, navigator.backStack.value.size)

        navigator.navigateTo(AppRoute.ArtistDetail("a1", "Artist"))
        assertEquals(NavTransitionType.PUSH, navigator.transitionType.value)

        assertTrue(navigator.navigateBack())
        assertEquals(NavTransitionType.POP, navigator.transitionType.value)
        assertFalse(navigator.navigateBack())
    }

    /** Verifies push, singleTop deduplication, and back pop transitions for SettingsDetail sub-pages. */
    @Test
    fun settingsDetailPushDeduplicationAndPopTransitions() {
        val navigator = AppNavigator(AppRoute.Settings)
        val languageRoute = AppRoute.SettingsDetail(SettingsSection.LANGUAGE)

        navigator.navigateTo(languageRoute)
        assertEquals(NavTransitionType.PUSH, navigator.transitionType.value)
        assertEquals(2, navigator.backStack.value.size)

        navigator.navigateTo(languageRoute)
        assertEquals(2, navigator.backStack.value.size)

        assertTrue(navigator.navigateBack())
        assertEquals(NavTransitionType.POP, navigator.transitionType.value)
        assertEquals(AppRoute.Settings, navigator.currentRoute)
        assertFalse(navigator.navigateBack())
    }
}
