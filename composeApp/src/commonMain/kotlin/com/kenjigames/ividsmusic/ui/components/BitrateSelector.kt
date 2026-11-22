package com.kenjigames.ividsmusic.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kenjigames.ividsmusic.domain.model.AudioQuality
import com.kenjigames.ividsmusic.ui.theme.IvidsColorTokens
import com.kenjigames.ividsmusic.ui.theme.IvidsTextStyles
import com.kenjigames.ividsmusic.ui.theme.LocalDynamicAccent
import com.kenjigames.ividsmusic.ui.theme.tvFocusable

/**
 * Shared 7-tier bitrate selector (`4`, `48`, `96`, `128`, `196`, `256`, `320` kbps) with tabular
 * numerals and cyan active pills, reused across both [DownloaderScreen] and [SettingsScreen].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BitrateSelector(
    selected: AudioQuality,
    onSelect: (AudioQuality) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = LocalDynamicAccent.current

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AudioQuality.entries.forEach { quality ->
            val isSelected = quality == selected
            Surface(
                modifier = Modifier
                    .tvFocusable(shape = RoundedCornerShape(10.dp))
                    .clickable { onSelect(quality) },
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) accent else IvidsColorTokens.GrooveElevated,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) accent else IvidsColorTokens.HairlineBorder
                )
            ) {
                Text(
                    text = quality.label,
                    style = IvidsTextStyles.tabularBadge(),
                    color = if (isSelected) IvidsColorTokens.OnAccentDark else IvidsColorTokens.ChalkInk,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}
