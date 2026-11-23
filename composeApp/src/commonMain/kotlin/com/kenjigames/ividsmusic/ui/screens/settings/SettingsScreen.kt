package com.kenjigames.ividsmusic.ui.screens.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.ViewQuilt
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ZoomOutMap
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kenjigames.ividsmusic.domain.model.AudioQuality
import com.kenjigames.ividsmusic.domain.model.LayoutMode
import com.kenjigames.ividsmusic.domain.model.UpdateMode
import com.kenjigames.ividsmusic.platform.AppUpdateState
import com.kenjigames.ividsmusic.ui.components.ConfirmationDialog
import com.kenjigames.ividsmusic.ui.components.FramedScreenContainer
import com.kenjigames.ividsmusic.ui.components.IvidsSelectableDialogItem
import com.kenjigames.ividsmusic.ui.components.IvidsSwitch
import com.kenjigames.ividsmusic.ui.components.ividsTouchFeedback
import com.kenjigames.ividsmusic.ui.i18n.AppStrings
import com.kenjigames.ividsmusic.ui.i18n.LocalStrings
import com.kenjigames.ividsmusic.ui.i18n.SupportedLanguage
import com.kenjigames.ividsmusic.ui.theme.IvidsColorTokens
import com.kenjigames.ividsmusic.ui.theme.LocalLayoutMetrics
import com.kenjigames.ividsmusic.ui.theme.tvFocusable
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class SettingsPickerKind {
    SCALE,
    LANGUAGE,
    LAYOUT,
    QUALITY,
    UPDATE_MODE
}

private val UI_SCALE_PRESETS = listOf(0.75f, 0.90f, 1.00f, 1.10f, 1.25f, 1.50f)

/**
 * Renders the flat, borderless YouTube/Spotify-style Settings list inside [FramedScreenContainer]
 * with single-tap modal pickers (`Interface Scale`, `Language`, `Layout Mode`, `Audio Quality`,
 * `Update Mode`), switches, and minimal dividers directly on the black canvas.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val strings = LocalStrings.current
    val metrics = LocalLayoutMetrics.current
    var activePicker by remember { mutableStateOf<SettingsPickerKind?>(null) }

    val resolvedPicker = if (state.isLanguageModalOpen) SettingsPickerKind.LANGUAGE else activePicker
    when (resolvedPicker) {
        SettingsPickerKind.LANGUAGE -> {
            SettingsPickerDialog(
                title = strings["language"],
                items = SupportedLanguage.entries,
                selected = state.language,
                labelProvider = { it.displayName },
                keyProvider = { it.code },
                onSelect = {
                    activePicker = null
                    viewModel.selectLanguage(it)
                },
                onDismiss = {
                    activePicker = null
                    viewModel.setLanguageModalOpen(false)
                }
            )
        }
        SettingsPickerKind.SCALE -> {
            val closestScale = UI_SCALE_PRESETS.minByOrNull { abs(it - state.uiScale) } ?: 1.0f
            SettingsPickerDialog(
                title = strings["interface_scale"],
                items = UI_SCALE_PRESETS,
                selected = closestScale,
                labelProvider = { "${(it * 100).roundToInt()}%" },
                keyProvider = { it },
                onSelect = {
                    activePicker = null
                    viewModel.setUiScale(it)
                },
                onDismiss = { activePicker = null }
            )
        }
        SettingsPickerKind.LAYOUT -> {
            SettingsPickerDialog(
                title = strings["layout_mode"],
                items = LayoutMode.entries,
                selected = state.layoutMode,
                labelProvider = { formatLayoutModeLabel(it, strings) },
                keyProvider = { it.name },
                onSelect = {
                    activePicker = null
                    viewModel.setLayoutMode(it)
                },
                onDismiss = { activePicker = null }
            )
        }
        SettingsPickerKind.QUALITY -> {
            SettingsPickerDialog(
                title = strings["default_quality"],
                items = AudioQuality.entries,
                selected = state.defaultQuality,
                labelProvider = { it.label },
                keyProvider = { it.name },
                onSelect = {
                    activePicker = null
                    viewModel.setDefaultDownloadQuality(it)
                },
                onDismiss = { activePicker = null }
            )
        }
        SettingsPickerKind.UPDATE_MODE -> {
            SettingsPickerDialog(
                title = strings["update_mode"],
                items = UpdateMode.entries,
                selected = state.updateMode,
                labelProvider = { formatUpdateModeLabel(it, strings) },
                keyProvider = { it.name },
                onSelect = {
                    activePicker = null
                    viewModel.setUpdateMode(it)
                },
                onDismiss = { activePicker = null }
            )
        }
        null -> Unit
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

    val rowHPad = if (metrics.isCompactMobile) 16.dp else 20.dp

    FramedScreenContainer(
        maxWidth = 760.dp,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = if (metrics.isCompactMobile) 14.dp else 24.dp,
                bottom = metrics.contentBottomClearance
            )
        ) {
            item(key = "settings_title") {
                Text(
                    text = strings["settings"],
                    style = MaterialTheme.typography.headlineMedium,
                    color = IvidsColorTokens.ChalkInk,
                    modifier = Modifier.padding(horizontal = rowHPad, vertical = 8.dp)
                )
            }

            item(key = "sec_appearance") {
                SettingsSectionHeader(title = strings["appearance"], horizontalPadding = rowHPad)
            }
            item(key = "row_language") {
                SettingsListRow(
                    icon = Icons.Rounded.Language,
                    title = strings["language"],
                    subtitle = state.language.displayName,
                    horizontalPadding = rowHPad,
                    onClick = { viewModel.setLanguageModalOpen(true) }
                )
            }
            item(key = "row_scale") {
                SettingsListRow(
                    icon = Icons.Rounded.ZoomOutMap,
                    title = strings["interface_scale"],
                    subtitle = "${(state.uiScale * 100).roundToInt()}%",
                    horizontalPadding = rowHPad,
                    onClick = { activePicker = SettingsPickerKind.SCALE }
                )
            }
            item(key = "row_layout") {
                SettingsListRow(
                    icon = Icons.AutoMirrored.Rounded.ViewQuilt,
                    title = strings["layout_mode"],
                    subtitle = formatLayoutModeLabel(state.layoutMode, strings),
                    horizontalPadding = rowHPad,
                    onClick = { activePicker = SettingsPickerKind.LAYOUT }
                )
            }
            item(key = "row_tint") {
                SettingsListRow(
                    icon = Icons.Rounded.AutoAwesome,
                    title = strings["dynamic_tint"],
                    horizontalPadding = rowHPad,
                    trailing = {
                        IvidsSwitch(
                            checked = state.dynamicTintEnabled,
                            onCheckedChange = viewModel::setDynamicTintEnabled
                        )
                    },
                    onClick = { viewModel.setDynamicTintEnabled(!state.dynamicTintEnabled) }
                )
            }

            item(key = "div_audio") {
                HorizontalDivider(color = IvidsColorTokens.HairlineSubtle, modifier = Modifier.padding(vertical = 8.dp))
            }
            item(key = "sec_audio") {
                SettingsSectionHeader(title = strings["default_quality"], horizontalPadding = rowHPad)
            }
            item(key = "row_quality") {
                SettingsListRow(
                    icon = Icons.Rounded.GraphicEq,
                    title = strings["default_quality"],
                    subtitle = state.defaultQuality.label,
                    horizontalPadding = rowHPad,
                    onClick = { activePicker = SettingsPickerKind.QUALITY }
                )
            }

            item(key = "div_updates") {
                HorizontalDivider(color = IvidsColorTokens.HairlineSubtle, modifier = Modifier.padding(vertical = 8.dp))
            }
            item(key = "sec_updates") {
                SettingsSectionHeader(title = strings["update"], horizontalPadding = rowHPad)
            }
            item(key = "row_update_mode") {
                SettingsListRow(
                    icon = Icons.Rounded.Tune,
                    title = strings["update_mode"],
                    subtitle = formatUpdateModeLabel(state.updateMode, strings),
                    horizontalPadding = rowHPad,
                    onClick = { activePicker = SettingsPickerKind.UPDATE_MODE }
                )
            }
            item(key = "row_check_updates") {
                val (statusText, statusColor) = when (val upd = state.updateState) {
                    is AppUpdateState.Idle -> "${strings["version"]} ${state.appVersion}" to IvidsColorTokens.SlateMuted
                    is AppUpdateState.Checking -> strings[upd.statusKey] to IvidsColorTokens.ChalkInk
                    is AppUpdateState.UpToDate -> strings["up_to_date"] to IvidsColorTokens.SuccessMint
                    is AppUpdateState.Available -> "${strings["update_title"]} (${upd.version})" to IvidsColorTokens.ChalkInk
                    is AppUpdateState.Downloading -> "${(upd.progress * 100).roundToInt()}%" to IvidsColorTokens.ChalkInk
                    is AppUpdateState.ReadyToInstall -> upd.message to IvidsColorTokens.SuccessMint
                    is AppUpdateState.Error -> upd.message to IvidsColorTokens.DangerCoral
                }
                SettingsListRow(
                    icon = Icons.Rounded.SystemUpdate,
                    title = strings["check_updates"],
                    subtitle = statusText,
                    subtitleColor = statusColor,
                    horizontalPadding = rowHPad,
                    trailing = if (state.updateState is AppUpdateState.Available) {
                        {
                            Button(
                                onClick = viewModel::downloadAndInstallUpdate,
                                shape = CircleShape,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = IvidsColorTokens.ChalkInk,
                                    contentColor = IvidsColorTokens.ObsidianStage
                                )
                            ) {
                                Text(strings["update_now"], style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    } else {
                        null
                    },
                    extraContent = if (state.updateState is AppUpdateState.Downloading) {
                        val downloading = state.updateState as AppUpdateState.Downloading
                        {
                            LinearProgressIndicator(
                                progress = { downloading.progress },
                                color = IvidsColorTokens.ChalkInk,
                                trackColor = IvidsColorTokens.RecessedTrough,
                                modifier = Modifier.fillMaxWidth().padding(top = 6.dp).clip(CircleShape)
                            )
                        }
                    } else {
                        null
                    },
                    onClick = if (state.updateState is AppUpdateState.Available) {
                        viewModel::downloadAndInstallUpdate
                    } else {
                        viewModel::checkForUpdates
                    }
                )
            }

            item(key = "div_data") {
                HorizontalDivider(color = IvidsColorTokens.HairlineSubtle, modifier = Modifier.padding(vertical = 8.dp))
            }
            item(key = "sec_data") {
                SettingsSectionHeader(title = strings["data_management"], horizontalPadding = rowHPad)
            }
            item(key = "row_clear_cache") {
                SettingsListRow(
                    icon = Icons.Rounded.DeleteSweep,
                    title = strings["clear_cache"],
                    horizontalPadding = rowHPad,
                    onClick = viewModel::clearAudioCache
                )
            }
            item(key = "row_clear_scores") {
                SettingsListRow(
                    icon = Icons.Rounded.DeleteSweep,
                    title = strings["clear_scores"],
                    isDestructive = true,
                    horizontalPadding = rowHPad,
                    onClick = { viewModel.setResetScoresConfirmOpen(true) }
                )
            }
            item(key = "row_reset_defaults") {
                SettingsListRow(
                    icon = Icons.Rounded.RestartAlt,
                    title = strings["reset_defaults"],
                    isDestructive = true,
                    horizontalPadding = rowHPad,
                    onClick = { viewModel.setResetDefaultsConfirmOpen(true) }
                )
            }

            item(key = "div_about") {
                HorizontalDivider(color = IvidsColorTokens.HairlineSubtle, modifier = Modifier.padding(vertical = 8.dp))
            }
            item(key = "sec_about") {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = rowHPad, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = strings["about"],
                        style = MaterialTheme.typography.labelLarge,
                        color = IvidsColorTokens.SlateMuted
                    )
                    Text(
                        text = "IVIDS Music • v${state.appVersion} • KenjiGames • ISC License",
                        style = MaterialTheme.typography.bodySmall,
                        color = IvidsColorTokens.SlateSubtle
                    )
                }
            }
        }
    }
}

/**
 * Renders a compact YouTube-style section header label aligned to the list row gutter.
 */
@Composable
private fun SettingsSectionHeader(
    title: String,
    horizontalPadding: Dp
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = IvidsColorTokens.SlateMuted,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = horizontalPadding, end = horizontalPadding, top = 8.dp, bottom = 4.dp)
    )
}

/**
 * Renders a single borderless, flat full-width settings list row directly on [ObsidianStage] with
 * leading icon, title, optional current-value subtitle, hover/touch feedback, and trailing widget.
 */
@Composable
private fun SettingsListRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    subtitleColor: Color = IvidsColorTokens.SlateMuted,
    isDestructive: Boolean = false,
    horizontalPadding: Dp = 16.dp,
    trailing: (@Composable () -> Unit)? = null,
    extraContent: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val rowBg by animateColorAsState(
        targetValue = if (isHovered) Color(0xFF181818) else Color.Transparent,
        animationSpec = tween(140, easing = FastOutSlowInEasing),
        label = "SettingsRowBg"
    )
    val contentTint = if (isDestructive) IvidsColorTokens.DangerCoral else IvidsColorTokens.ChalkInk
    val iconTint = if (isDestructive) IvidsColorTokens.DangerCoral else IvidsColorTokens.SlateMuted

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .background(rowBg)
            .ividsTouchFeedback(interactionSource = interactionSource, pressedScale = 0.99f)
            .pointerHoverIcon(PointerIcon.Hand)
            .hoverable(interactionSource)
            .tvFocusable(shape = RoundedCornerShape(8.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = horizontalPadding, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = contentTint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = subtitleColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (extraContent != null) {
                extraContent()
            }
        }
        if (trailing != null) {
            trailing()
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = if (isDestructive) IvidsColorTokens.DangerCoral.copy(alpha = 0.7f) else IvidsColorTokens.SlateSubtle,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Displays a flat borderless single-selection dialog list backing [SettingsScreen] pickers without
 * nested cards or duplicate buttons.
 */
@Composable
private fun <T> SettingsPickerDialog(
    title: String,
    items: List<T>,
    selected: T,
    labelProvider: (T) -> String,
    keyProvider: (T) -> Any,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = IvidsColorTokens.GlassSurface,
        title = {
            Text(title, style = MaterialTheme.typography.titleLarge, color = IvidsColorTokens.ChalkInk)
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(items, key = keyProvider) { item ->
                    val isSelected = item == selected
                    IvidsSelectableDialogItem(
                        selected = isSelected,
                        onClick = { onSelect(item) }
                    ) {
                        Text(
                            text = labelProvider(item),
                            style = MaterialTheme.typography.titleSmall,
                            color = IvidsColorTokens.ChalkInk
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = IvidsColorTokens.ChalkInk,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
            ) {
                Text(strings["close"], color = IvidsColorTokens.SlateMuted)
            }
        }
    )
}

/** Maps a [LayoutMode] enum value to its localized label in [AppStrings]. */
private fun formatLayoutModeLabel(mode: LayoutMode, strings: AppStrings): String = when (mode) {
    LayoutMode.AUTO -> strings["layout_auto"]
    LayoutMode.MOBILE -> strings["layout_mobile"]
    LayoutMode.DESKTOP -> strings["layout_desktop"]
    LayoutMode.TV -> strings["layout_tv"]
}

/** Maps an [UpdateMode] enum value to its localized label in [AppStrings]. */
private fun formatUpdateModeLabel(mode: UpdateMode, strings: AppStrings): String = when (mode) {
    UpdateMode.AUTO -> strings["update_mode_auto"]
    UpdateMode.MANUAL -> strings["update_mode_manual"]
    UpdateMode.OFF -> strings["update_mode_off"]
    UpdateMode.DEVELOPER -> strings["update_mode_developer"]
}
