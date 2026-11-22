package com.kenjigames.ividsmusic.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.kenjigames.ividsmusic.resources.Res
import com.kenjigames.ividsmusic.resources.inter_medium
import com.kenjigames.ividsmusic.resources.inter_regular
import com.kenjigames.ividsmusic.resources.inter_semibold
import com.kenjigames.ividsmusic.resources.plus_jakarta_sans_bold
import com.kenjigames.ividsmusic.resources.plus_jakarta_sans_semibold
import org.jetbrains.compose.resources.Font

/**
 * Builds and remembers the Compose Material 3 Typography backed by bundled Plus Jakarta Sans
 * display fonts and Inter body fonts, ensuring consistent visual hierarchy across platforms.
 */
@Composable
fun rememberIvidsTypography(): Typography {
    val displayFamily = FontFamily(
        Font(Res.font.plus_jakarta_sans_semibold, weight = FontWeight.SemiBold),
        Font(Res.font.plus_jakarta_sans_bold, weight = FontWeight.Bold)
    )
    val bodyFamily = FontFamily(
        Font(Res.font.inter_regular, weight = FontWeight.Normal),
        Font(Res.font.inter_medium, weight = FontWeight.Medium),
        Font(Res.font.inter_semibold, weight = FontWeight.SemiBold)
    )
    return Typography(
        displaySmall = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 38.sp,
            letterSpacing = (-0.5).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            letterSpacing = (-0.3).sp
        ),
        headlineSmall = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            lineHeight = 26.sp,
            letterSpacing = (-0.2).sp
        ),
        titleLarge = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            lineHeight = 24.sp
        ),
        titleMedium = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            lineHeight = 20.sp
        ),
        titleSmall = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            lineHeight = 18.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 22.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 18.sp
        ),
        bodySmall = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp
        ),
        labelLarge = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            lineHeight = 16.sp
        ),
        labelMedium = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 14.sp
        ),
        labelSmall = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            lineHeight = 13.sp
        )
    )
}

/**
 * Provides tabular numeric TextStyles (`tnum`) for playback timers, track numbers,
 * storage metrics, and audio quality bitrate labels to prevent layout jitter.
 */
object IvidsTextStyles {

    /**
     * Returns a tabular monospace-aligned TextStyle derived from the active body typography
     * for live playback scrubbers and track duration counters.
     */
    @Composable
    fun tabularTime(): TextStyle = androidx.compose.material3.MaterialTheme.typography.bodySmall.copy(
        fontFeatureSettings = "tnum"
    )

    /**
     * Returns a tabular numeric TextStyle for track indices, fan counts, and bitrate badges
     * such as 4–320 kbps selectors.
     */
    @Composable
    fun tabularBadge(): TextStyle = androidx.compose.material3.MaterialTheme.typography.labelMedium.copy(
        fontFeatureSettings = "tnum"
    )
}
