package com.kenjigames.ividsmusic.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kenjigames.ividsmusic.domain.model.AudioQuality

/**
 * Shared 7-tier bitrate selector (`4`, `48`, `96`, `128`, `196`, `256`, `320` kbps) rendered inside
 * a recessed pill trough with tabular numerals, reused in [DownloaderScreen] and [SettingsScreen].
 */
@Composable
fun BitrateSelector(
    selected: AudioQuality,
    onSelect: (AudioQuality) -> Unit,
    modifier: Modifier = Modifier
) {
    IvidsSegmentedBar(
        items = AudioQuality.entries,
        selected = selected,
        labelProvider = { it.label },
        onSelect = onSelect,
        tabular = true,
        modifier = modifier.fillMaxWidth()
    )
}
