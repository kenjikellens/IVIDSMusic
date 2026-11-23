package com.kenjigames.ividsmusic.ui.shell

import androidx.compose.ui.unit.dp
import com.kenjigames.ividsmusic.domain.model.LayoutMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MobileLayoutMetricsTest {

    @Test
    fun resolveLayoutMetricsAdaptsMobileWhilePreservingDesktopAndTvDefaults() {
        val mobile = resolveLayoutMetrics(ShellMode.MOBILE)
        assertTrue(mobile.isCompactMobile)
        assertEquals(16.dp, mobile.screenHorizontalPadding)
        assertEquals(136.dp, mobile.contentBottomClearance)
        assertEquals(148.dp, mobile.cardWidth)
        assertTrue(mobile.showTouchPlayFab)

        val desktop = resolveLayoutMetrics(ShellMode.DESKTOP)
        assertFalse(desktop.isCompactMobile)
        assertEquals(24.dp, desktop.screenHorizontalPadding)
        assertEquals(104.dp, desktop.contentBottomClearance)
        assertEquals(176.dp, desktop.cardWidth)
        assertFalse(desktop.showTouchPlayFab)

        assertEquals(desktop, resolveLayoutMetrics(ShellMode.TV))
    }

    @Test
    fun horizontalSwipeActionRespectsThresholdsAndAvailableDirections() {
        assertEquals(
            HorizontalSwipeAction.SKIP_NEXT,
            resolveHorizontalSwipeAction(-60f, 56f, canSkipPrevious = true, canSkipNext = true)
        )
        assertEquals(
            HorizontalSwipeAction.SKIP_PREVIOUS,
            resolveHorizontalSwipeAction(60f, 56f, canSkipPrevious = true, canSkipNext = true)
        )
        assertEquals(
            HorizontalSwipeAction.NONE,
            resolveHorizontalSwipeAction(-40f, 56f, canSkipPrevious = true, canSkipNext = true)
        )
        assertEquals(
            HorizontalSwipeAction.NONE,
            resolveHorizontalSwipeAction(-80f, 56f, canSkipPrevious = true, canSkipNext = false)
        )
        assertEquals(
            HorizontalSwipeAction.NONE,
            resolveHorizontalSwipeAction(80f, 56f, canSkipPrevious = false, canSkipNext = true)
        )
    }

    @Test
    fun verticalDismissAndShellResolutionHonorThresholds() {
        assertTrue(shouldDismissVerticalSwipe(120f))
        assertFalse(shouldDismissVerticalSwipe(85f))

        assertEquals(ShellMode.MOBILE, resolveShellMode(LayoutMode.AUTO, isTvDevice = false, widthDp = 390.dp))
        assertEquals(ShellMode.DESKTOP, resolveShellMode(LayoutMode.AUTO, isTvDevice = false, widthDp = 600.dp))
        assertEquals(ShellMode.TV, resolveShellMode(LayoutMode.AUTO, isTvDevice = true, widthDp = 390.dp))
    }
}
