package com.kenjigames.ividsmusic.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kenjigames.ividsmusic.domain.model.LayoutMode
import com.kenjigames.ividsmusic.domain.model.UpdateMode
import com.kenjigames.ividsmusic.platform.AppUpdateState
import com.kenjigames.ividsmusic.ui.components.BitrateSelector
import com.kenjigames.ividsmusic.ui.components.ConfirmationDialog
import com.kenjigames.ividsmusic.ui.i18n.LocalStrings
import com.kenjigames.ividsmusic.ui.i18n.SupportedLanguage
import com.kenjigames.ividsmusic.ui.theme.IvidsColorTokens
import com.kenjigames.ividsmusic.ui.theme.IvidsTextStyles
import com.kenjigames.ividsmusic.ui.theme.LocalDynamicAccent
import com.kenjigames.ividsmusic.ui.theme.tvFocusable
import kotlin.math.roundToInt

/**
 * Renders the Settings screen with UI scale selector (75%..150%), 16-language instant picker,
 * default download bitrate selector (4..320 kbps), dynamic tint toggle, layout override, native
 * app update checker, and cache/score data management.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val strings = LocalStrings.current
    val accent = LocalDynamicAccent.current

    if (state.isLanguageModalOpen) {
        AlertDialog(
            onDismissRequest = { viewModel.setLanguageModalOpen(false) },
            containerColor = IvidsColorTokens.VinylSurface,
            title = { Text(strings["language"], color = IvidsColorTokens.ChalkInk) },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(SupportedLanguage.entries, key = { it.code }) { lang ->
                        val selected = lang == state.language
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .tvFocusable(shape = RoundedCornerShape(10.dp))
                                .clickable { viewModel.selectLanguage(lang) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) accent.copy(alpha = 0.16f) else IvidsColorTokens.GrooveElevated,
                            border = BorderStroke(
                                1.dp,
                                if (selected) accent else IvidsColorTokens.HairlineBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(lang.displayName, color = IvidsColorTokens.ChalkInk)
                                if (selected) {
                                    Icon(Icons.Rounded.Check, null, tint = accent)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.setLanguageModalOpen(false) }) {
                    Text(strings["close"], color = IvidsColorTokens.SlateMuted)
                }
            }
        )
    }

    if (state.showResetScoresConfirm) {
        ConfirmationDialog(
            title = strings["clear_scores"],
            message = strings["clear_scores_desc"],
            confirmLabel = strings["confirm"],
            isDestructive = true,
            onConfirm = viewModel::confirmResetScores,
            onDismiss = { viewModel.setResetScoresConfirmOpen(false) }
        )
    }

    if (state.showResetDefaultsConfirm) {
        ConfirmationDialog(
            title = strings["reset_defaults"],
            message = strings["reset_defaults_desc"],
            confirmLabel = strings["confirm"],
            isDestructive = true,
            onConfirm = viewModel::confirmResetDefaults,
            onDismiss = { viewModel.setResetDefaultsConfirmOpen(false) }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item(key = "settings_title") {
            Text(strings["settings"], style = MaterialTheme.typography.headlineMedium, color = IvidsColorTokens.ChalkInk)
        }

        item(key = "appearance_card") {
            SettingsCard(title = strings["appearance"]) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings["interface_scale"], style = MaterialTheme.typography.titleSmall, color = IvidsColorTokens.ChalkInk)
                            Text(strings["interface_scale_desc"], style = MaterialTheme.typography.bodySmall, color = IvidsColorTokens.SlateMuted)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.setUiScale(state.uiScale - 0.10f) },
                                enabled = state.uiScale > 0.76f,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) { Text("-", color = IvidsColorTokens.ChalkInk) }
                            Text(
                                text = "${(state.uiScale * 100).roundToInt()}%",
                                style = IvidsTextStyles.tabularBadge(),
                                color = accent
                            )
                            OutlinedButton(
                                onClick = { viewModel.setUiScale(state.uiScale + 0.10f) },
                                enabled = state.uiScale < 1.49f,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) { Text("+", color = IvidsColorTokens.ChalkInk) }
                        }
                    }

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0.75f, 0.90f, 1.00f, 1.10f, 1.25f, 1.50f).forEach { factor ->
                            val selected = kotlin.math.abs(state.uiScale - factor) < 0.03f
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.setUiScale(factor) },
                                label = { Text("${(factor * 100).roundToInt()}%", style = IvidsTextStyles.tabularBadge()) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = accent,
                                    selectedLabelColor = IvidsColorTokens.OnAccentDark,
                                    containerColor = IvidsColorTokens.GrooveElevated,
                                    labelColor = IvidsColorTokens.ChalkInk
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings["language"], style = MaterialTheme.typography.titleSmall, color = IvidsColorTokens.ChalkInk)
                            Text(strings["language_desc"], style = MaterialTheme.typography.bodySmall, color = IvidsColorTokens.SlateMuted)
                        }
                        OutlinedButton(
                            onClick = { viewModel.setLanguageModalOpen(true) },
                            border = BorderStroke(1.dp, accent),
                            modifier = Modifier.tvFocusable(shape = RoundedCornerShape(10.dp))
                        ) {
                            Icon(Icons.Rounded.Language, null, tint = accent)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(state.language.displayName, color = IvidsColorTokens.ChalkInk)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings["dynamic_tint"], style = MaterialTheme.typography.titleSmall, color = IvidsColorTokens.ChalkInk)
                            Text(strings["dynamic_tint_desc"], style = MaterialTheme.typography.bodySmall, color = IvidsColorTokens.SlateMuted)
                        }
                        Switch(
                            checked = state.dynamicTintEnabled,
                            onCheckedChange = viewModel::setDynamicTintEnabled,
                            colors = SwitchDefaults.colors(checkedThumbColor = accent)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(strings["layout_mode"], style = MaterialTheme.typography.titleSmall, color = IvidsColorTokens.ChalkInk)
                        Text(strings["layout_mode_desc"], style = MaterialTheme.typography.bodySmall, color = IvidsColorTokens.SlateMuted)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val modes = listOf(
                                LayoutMode.AUTO to strings["layout_auto"],
                                LayoutMode.MOBILE to strings["layout_mobile"],
                                LayoutMode.DESKTOP to strings["layout_desktop"],
                                LayoutMode.TV to strings["layout_tv"]
                            )
                            modes.forEach { (mode, label) ->
                                val selected = state.layoutMode == mode
                                FilterChip(
                                    selected = selected,
                                    onClick = { viewModel.setLayoutMode(mode) },
                                    label = { Text(label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = accent,
                                        selectedLabelColor = IvidsColorTokens.OnAccentDark,
                                        containerColor = IvidsColorTokens.GrooveElevated,
                                        labelColor = IvidsColorTokens.ChalkInk
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        item(key = "quality_card") {
            SettingsCard(title = strings["default_quality"]) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(strings["default_quality_desc"], style = MaterialTheme.typography.bodySmall, color = IvidsColorTokens.SlateMuted)
                    BitrateSelector(
                        selected = state.defaultQuality,
                        onSelect = viewModel::setDefaultDownloadQuality
                    )
                }
            }
        }

        item(key = "update_card") {
            SettingsCard(title = strings["update"]) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val updateModes = listOf(
                            UpdateMode.AUTO to strings["update_mode_auto"],
                            UpdateMode.MANUAL to strings["update_mode_manual"],
                            UpdateMode.OFF to strings["update_mode_off"],
                            UpdateMode.DEVELOPER to strings["update_mode_developer"]
                        )
                        updateModes.forEach { (mode, label) ->
                            val selected = state.updateMode == mode
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.setUpdateMode(mode) },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = accent,
                                    selectedLabelColor = IvidsColorTokens.OnAccentDark,
                                    containerColor = IvidsColorTokens.GrooveElevated,
                                    labelColor = IvidsColorTokens.ChalkInk
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${strings["version"]}: ${state.appVersion}", style = IvidsTextStyles.tabularBadge(), color = IvidsColorTokens.ChalkInk)
                            when (val upd = state.updateState) {
                                is AppUpdateState.Idle -> Unit
                                is AppUpdateState.Checking -> Text(strings[upd.statusKey], style = MaterialTheme.typography.bodySmall, color = accent)
                                is AppUpdateState.UpToDate -> Text(strings["up_to_date"], style = MaterialTheme.typography.bodySmall, color = IvidsColorTokens.SuccessMint)
                                is AppUpdateState.Available -> Text("${strings["update_title"]} (${upd.version})", style = MaterialTheme.typography.bodySmall, color = accent)
                                is AppUpdateState.Downloading -> LinearProgressIndicator(progress = { upd.progress }, color = accent, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
                                is AppUpdateState.ReadyToInstall -> Text(upd.message, style = MaterialTheme.typography.bodySmall, color = IvidsColorTokens.SuccessMint)
                                is AppUpdateState.Error -> Text(upd.message, style = MaterialTheme.typography.bodySmall, color = IvidsColorTokens.DangerCoral)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        if (state.updateState is AppUpdateState.Available) {
                            Button(
                                onClick = viewModel::downloadAndInstallUpdate,
                                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = IvidsColorTokens.OnAccentDark)
                            ) {
                                Text(strings["update_now"])
                            }
                        } else {
                            OutlinedButton(
                                onClick = viewModel::checkForUpdates,
                                border = BorderStroke(1.dp, accent),
                                modifier = Modifier.tvFocusable(shape = RoundedCornerShape(10.dp))
                            ) {
                                Icon(Icons.Rounded.SystemUpdate, null, tint = accent)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings["check_updates"], color = IvidsColorTokens.ChalkInk)
                            }
                        }
                    }
                }
            }
        }

        item(key = "data_card") {
            SettingsCard(title = strings["data_management"]) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings["clear_cache"], style = MaterialTheme.typography.titleSmall, color = IvidsColorTokens.ChalkInk)
                            Text(strings["clear_cache_desc"], style = MaterialTheme.typography.bodySmall, color = IvidsColorTokens.SlateMuted)
                        }
                        OutlinedButton(onClick = viewModel::clearAudioCache) {
                            Text(strings["clear_cache"], color = IvidsColorTokens.ChalkInk)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings["clear_scores"], style = MaterialTheme.typography.titleSmall, color = IvidsColorTokens.ChalkInk)
                            Text(strings["clear_scores_desc"], style = MaterialTheme.typography.bodySmall, color = IvidsColorTokens.SlateMuted)
                        }
                        OutlinedButton(
                            onClick = { viewModel.setResetScoresConfirmOpen(true) },
                            border = BorderStroke(1.dp, IvidsColorTokens.DangerCoral.copy(alpha = 0.5f))
                        ) {
                            Text(strings["clear_scores"], color = IvidsColorTokens.DangerCoral)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings["reset_defaults"], style = MaterialTheme.typography.titleSmall, color = IvidsColorTokens.ChalkInk)
                            Text(strings["reset_defaults_desc"], style = MaterialTheme.typography.bodySmall, color = IvidsColorTokens.SlateMuted)
                        }
                        OutlinedButton(
                            onClick = { viewModel.setResetDefaultsConfirmOpen(true) },
                            border = BorderStroke(1.dp, IvidsColorTokens.DangerCoral.copy(alpha = 0.5f))
                        ) {
                            Text(strings["reset_defaults"], color = IvidsColorTokens.DangerCoral)
                        }
                    }
                }
            }
        }

        item(key = "about_card") {
            SettingsCard(title = strings["about"]) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("IVIDS Music", style = MaterialTheme.typography.titleMedium, color = accent)
                    Text("${strings["publisher"]}: KenjiGames", style = MaterialTheme.typography.bodySmall, color = IvidsColorTokens.SlateMuted)
                    Text("${strings["version"]}: ${state.appVersion}", style = IvidsTextStyles.tabularBadge(), color = IvidsColorTokens.SlateMuted)
                    Text("${strings["license"]}: ISC License", style = MaterialTheme.typography.bodySmall, color = IvidsColorTokens.SlateMuted)
                }
            }
        }
    }
}

/**
 * Renders a titled elevated section card grouping related settings controls inside [SettingsScreen].
 */
@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = IvidsColorTokens.VinylSurface,
        border = BorderStroke(1.dp, IvidsColorTokens.HairlineBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = IvidsColorTokens.ChalkInk)
            content()
        }
    }
}
