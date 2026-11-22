package com.kenjigames.ividsmusic.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kenjigames.ividsmusic.domain.model.LayoutMode
import com.kenjigames.ividsmusic.domain.model.UpdateMode
import com.kenjigames.ividsmusic.platform.AppUpdateState
import com.kenjigames.ividsmusic.ui.components.BitrateSelector
import com.kenjigames.ividsmusic.ui.components.ConfirmationDialog
import com.kenjigames.ividsmusic.ui.components.FramedScreenContainer
import com.kenjigames.ividsmusic.ui.components.IvidsSegmentedBar
import com.kenjigames.ividsmusic.ui.components.IvidsSwitch
import com.kenjigames.ividsmusic.ui.i18n.LocalStrings
import com.kenjigames.ividsmusic.ui.i18n.SupportedLanguage
import com.kenjigames.ividsmusic.ui.theme.IvidsColorTokens
import com.kenjigames.ividsmusic.ui.theme.IvidsTextStyles
import com.kenjigames.ividsmusic.ui.theme.LocalDynamicAccent
import com.kenjigames.ividsmusic.ui.theme.tvFocusable
import kotlin.math.roundToInt

/**
 * Renders the centered Settings screen (max 860dp) featuring borderless tonal panels, recessed
 * segmented selectors, custom [IvidsSwitch] toggles, 16-language picker, and 4–320 kbps selector.
 */
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
            containerColor = IvidsColorTokens.GlassSurface,
            title = { Text(strings["language"], style = MaterialTheme.typography.titleLarge, color = IvidsColorTokens.ChalkInk) },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(SupportedLanguage.entries, key = { it.code }) { lang ->
                        val selected = lang == state.language
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .tvFocusable(shape = RoundedCornerShape(12.dp))
                                .clickable { viewModel.selectLanguage(lang) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) accent.copy(alpha = 0.16f) else IvidsColorTokens.RecessedTrough
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(lang.displayName, style = MaterialTheme.typography.titleSmall, color = IvidsColorTokens.ChalkInk)
                                if (selected) {
                                    Icon(Icons.Rounded.Check, null, tint = accent, modifier = Modifier.size(20.dp))
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

    FramedScreenContainer(
        maxWidth = 860.dp,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            item(key = "settings_title") {
                Text(
                    text = strings["settings"],
                    style = MaterialTheme.typography.headlineMedium,
                    color = IvidsColorTokens.ChalkInk
                )
            }

            item(key = "appearance_card") {
                SettingsGroupPanel(title = strings["appearance"]) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                                Text(strings["interface_scale"], style = MaterialTheme.typography.titleMedium, color = IvidsColorTokens.ChalkInk)
                                Text(strings["interface_scale_desc"], style = MaterialTheme.typography.bodySmall, color = IvidsColorTokens.SlateMuted)
                            }
                            Surface(
                                shape = CircleShape,
                                color = IvidsColorTokens.RecessedTrough
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    TextButton(
                                        onClick = { viewModel.setUiScale(state.uiScale - 0.10f) },
                                        enabled = state.uiScale > 0.76f,
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) { Text("-", color = IvidsColorTokens.ChalkInk) }
                                    Text(
                                        text = "${(state.uiScale * 100).roundToInt()}%",
                                        style = IvidsTextStyles.tabularBadge(),
                                        color = accent
                                    )
                                    TextButton(
                                        onClick = { viewModel.setUiScale(state.uiScale + 0.10f) },
                                        enabled = state.uiScale < 1.49f,
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) { Text("+", color = IvidsColorTokens.ChalkInk) }
                                }
                            }
                        }

                        val scalePresets = listOf(0.75f, 0.90f, 1.00f, 1.10f, 1.25f, 1.50f)
                        val activeScalePreset = scalePresets.minByOrNull { kotlin.math.abs(it - state.uiScale) } ?: 1.00f
                        IvidsSegmentedBar(
                            items = scalePresets,
                            selected = activeScalePreset,
                            labelProvider = { "${(it * 100).roundToInt()}%" },
                            onSelect = viewModel::setUiScale,
                            tabular = true
                        )
                    }

                    HorizontalDivider(color = IvidsColorTokens.HairlineSubtle)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                            Text(strings["language"], style = MaterialTheme.typography.titleMedium, color = IvidsColorTokens.ChalkInk)
                            Text(strings["language_desc"], style = MaterialTheme.typography.bodySmall, color = IvidsColorTokens.SlateMuted)
                        }
                        Surface(
                            onClick = { viewModel.setLanguageModalOpen(true) },
                            shape = CircleShape,
                            color = IvidsColorTokens.RecessedTrough,
                            border = BorderStroke(1.dp, IvidsColorTokens.HairlineSubtle),
                            modifier = Modifier.tvFocusable(shape = CircleShape)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Rounded.Language, null, tint = accent, modifier = Modifier.size(18.dp))
                                Text(state.language.displayName, style = MaterialTheme.typography.labelLarge, color = IvidsColorTokens.ChalkInk)
                            }
                        }
                    }

                    HorizontalDivider(color = IvidsColorTokens.HairlineSubtle)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                            Text(strings["dynamic_tint"], style = MaterialTheme.typography.titleMedium, color = IvidsColorTokens.ChalkInk)
                            Text(strings["dynamic_tint_desc"], style = MaterialTheme.typography.bodySmall, color = IvidsColorTokens.SlateMuted)
                        }
                        IvidsSwitch(
                            checked = state.dynamicTintEnabled,
                            onCheckedChange = viewModel::setDynamicTintEnabled
                        )
                    }

                    HorizontalDivider(color = IvidsColorTokens.HairlineSubtle)

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column {
                            Text(strings["layout_mode"], style = MaterialTheme.typography.titleMedium, color = IvidsColorTokens.ChalkInk)
                            Text(strings["layout_mode_desc"], style = MaterialTheme.typography.bodySmall, color = IvidsColorTokens.SlateMuted)
                        }
                        val layoutOptions = listOf(
                            LayoutMode.AUTO to strings["layout_auto"],
                            LayoutMode.MOBILE to strings["layout_mobile"],
                            LayoutMode.DESKTOP to strings["layout_desktop"],
                            LayoutMode.TV to strings["layout_tv"]
                        )
                        IvidsSegmentedBar(
                            items = layoutOptions.map { it.first },
                            selected = state.layoutMode,
                            labelProvider = { mode -> layoutOptions.first { it.first == mode }.second },
                            onSelect = viewModel::setLayoutMode
                        )
                    }
                }
            }

            item(key = "quality_card") {
                SettingsGroupPanel(title = strings["default_quality"]) {
                    Text(strings["default_quality_desc"], style = MaterialTheme.typography.bodySmall, color = IvidsColorTokens.SlateMuted)
                    BitrateSelector(
                        selected = state.defaultQuality,
                        onSelect = viewModel::setDefaultDownloadQuality
                    )
                }
            }

            item(key = "update_card") {
                SettingsGroupPanel(title = strings["update"]) {
                    val updateOptions = listOf(
                        UpdateMode.AUTO to strings["update_mode_auto"],
                        UpdateMode.MANUAL to strings["update_mode_manual"],
                        UpdateMode.OFF to strings["update_mode_off"],
                        UpdateMode.DEVELOPER to strings["update_mode_developer"]
                    )
                    IvidsSegmentedBar(
                        items = updateOptions.map { it.first },
                        selected = state.updateMode,
                        labelProvider = { mode -> updateOptions.first { it.first == mode }.second },
                        onSelect = viewModel::setUpdateMode
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
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
                        if (state.updateState is AppUpdateState.Available) {
                            Button(
                                onClick = viewModel::downloadAndInstallUpdate,
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = IvidsColorTokens.OnAccentDark)
                            ) {
                                Text(strings["update_now"], style = MaterialTheme.typography.labelLarge)
                            }
                        } else {
                            Surface(
                                onClick = viewModel::checkForUpdates,
                                shape = CircleShape,
                                color = IvidsColorTokens.RecessedTrough,
                                border = BorderStroke(1.dp, IvidsColorTokens.HairlineSubtle),
                                modifier = Modifier.tvFocusable(shape = CircleShape)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Rounded.SystemUpdate, null, tint = accent, modifier = Modifier.size(18.dp))
                                    Text(strings["check_updates"], style = MaterialTheme.typography.labelLarge, color = IvidsColorTokens.ChalkInk)
                                }
                            }
                        }
                    }
                }
            }

            item(key = "data_card") {
                SettingsGroupPanel(title = strings["data_management"]) {
                    SettingsActionRow(
                        title = strings["clear_cache"],
                        subtitle = strings["clear_cache_desc"],
                        actionLabel = strings["clear_cache"],
                        isDestructive = false,
                        onClick = viewModel::clearAudioCache
                    )
                    HorizontalDivider(color = IvidsColorTokens.HairlineSubtle)
                    SettingsActionRow(
                        title = strings["clear_scores"],
                        subtitle = strings["clear_scores_desc"],
                        actionLabel = strings["clear_scores"],
                        isDestructive = true,
                        onClick = { viewModel.setResetScoresConfirmOpen(true) }
                    )
                    HorizontalDivider(color = IvidsColorTokens.HairlineSubtle)
                    SettingsActionRow(
                        title = strings["reset_defaults"],
                        subtitle = strings["reset_defaults_desc"],
                        actionLabel = strings["reset_defaults"],
                        isDestructive = true,
                        onClick = { viewModel.setResetDefaultsConfirmOpen(true) }
                    )
                }
            }

            item(key = "about_card") {
                SettingsGroupPanel(title = strings["about"]) {
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
}

/**
 * Renders a single label + pill action row inside a [SettingsGroupPanel].
 */
@Composable
private fun SettingsActionRow(
    title: String,
    subtitle: String,
    actionLabel: String,
    isDestructive: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = IvidsColorTokens.ChalkInk)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = IvidsColorTokens.SlateMuted)
        }
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = if (isDestructive) IvidsColorTokens.DangerCoral.copy(alpha = 0.14f) else IvidsColorTokens.RecessedTrough,
            modifier = Modifier.tvFocusable(shape = CircleShape)
        ) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = if (isDestructive) IvidsColorTokens.DangerCoral else IvidsColorTokens.ChalkInk,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
            )
        }
    }
}

/**
 * Renders a borderless elevated tonal group card for logically related settings rows.
 */
@Composable
private fun SettingsGroupPanel(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = IvidsColorTokens.ChalkInk,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = IvidsColorTokens.GlassSurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                content()
            }
        }
    }
}
