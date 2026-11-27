package com.kenjigames.ividsmusic.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
 * Builds and remembers the crisp Compose Material 3 Typography backed by bundled Plus Jakarta Sans
 * display fonts and Inter body fonts with clear desktop and mobile typographic hierarchy.
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
    return remember(displayFamily, bodyFamily) {
        Typography(
            displaySmall = TextStyle(
                fontFamily = displayFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp,
                lineHeight = 42.sp,
                letterSpacing = (-0.75).sp
            ),
            headlineMedium = TextStyle(
                fontFamily = displayFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                lineHeight = 34.sp,
                letterSpacing = (-0.45).sp
            ),
            headlineSmall = TextStyle(
                fontFamily = displayFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                letterSpacing = (-0.3).sp
            ),
            titleLarge = TextStyle(
                fontFamily = displayFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 19.sp,
                lineHeight = 25.sp,
                letterSpacing = (-0.15).sp
            ),
            titleMedium = TextStyle(
                fontFamily = bodyFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 22.sp
            ),
            titleSmall = TextStyle(
                fontFamily = bodyFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                lineHeight = 20.sp
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
                fontSize = 14.sp,
                lineHeight = 20.sp
            ),
            bodySmall = TextStyle(
                fontFamily = bodyFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                lineHeight = 18.sp
            ),
            labelLarge = TextStyle(
                fontFamily = bodyFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 18.sp
            ),
            labelMedium = TextStyle(
                fontFamily = bodyFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp
            ),
            labelSmall = TextStyle(
                fontFamily = bodyFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
        )
    }
}

/**
 * Provides tabular numeric TextStyles (`tnum`) for playback timers, track numbers,
 * storage metrics, and audio quality bitrate labels to prevent layout jitter.
 */
object IvidsTextStyles {

    /**
     * Returns a tabular numeric TextStyle derived from bodySmall for live playback scrubbers
     * and track duration counters.
     */
    @Composable
    fun tabularTime(): TextStyle = MaterialTheme.typography.bodySmall.copy(
        fontFeatureSettings = "tnum"
    )

    /**
     * Returns a tabular numeric TextStyle for track indices, fan counts, and bitrate badges
     * such as 4–320 kbps selectors.
     */
    @Composable
    fun tabularBadge(): TextStyle = MaterialTheme.typography.labelMedium.copy(
        fontFeatureSettings = "tnum"
    )
}
