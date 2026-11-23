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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ZoomOutMap
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.kenjigames.ividsmusic.domain.model.UpdateMode
import com.kenjigames.ividsmusic.platform.AppUpdateState
import com.kenjigames.ividsmusic.ui.components.ConfirmationDialog
import com.kenjigames.ividsmusic.ui.components.FramedScreenContainer
import com.kenjigames.ividsmusic.ui.components.IvidsHoverStyle
import com.kenjigames.ividsmusic.ui.components.IvidsPrimaryButton
import com.kenjigames.ividsmusic.ui.components.IvidsSwitch
import com.kenjigames.ividsmusic.ui.components.ividsHoverZoom
import com.kenjigames.ividsmusic.ui.components.ividsPressRipple
import com.kenjigames.ividsmusic.ui.i18n.AppStrings
import com.kenjigames.ividsmusic.ui.i18n.LocalStrings
import com.kenjigames.ividsmusic.ui.i18n.SupportedLanguage
import com.kenjigames.ividsmusic.ui.navigation.SettingsSection
import com.kenjigames.ividsmusic.ui.theme.IvidsColorTokens
import com.kenjigames.ividsmusic.ui.theme.LocalLayoutMetrics
import com.kenjigames.ividsmusic.ui.theme.tvFocusable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private val UI_SCALE_PRESETS = listOf(0.75f, 0.90f, 1.00f, 1.10f, 1.25f, 1.50f)

/**
 * Renders the flat, borderless YouTube-style Settings list directly on the dark stage, navigating
 * via [onOpenSection] to right-sliding push sub-pages without popup dialogs.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onOpenSection: (SettingsSection) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val strings = LocalStrings.current
    val metrics = LocalLayoutMetrics.current

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
                    onClick = { onOpenSection(SettingsSection.LANGUAGE) }
                )
            }
            item(key = "row_scale") {
                SettingsListRow(
                    icon = Icons.Rounded.ZoomOutMap,
                    title = strings["interface_scale"],
                    subtitle = "${(state.uiScale * 100).roundToInt()}%",
                    horizontalPadding = rowHPad,
                    onClick = { onOpenSection(SettingsSection.INTERFACE_SCALE) }
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
                    onClick = { onOpenSection(SettingsSection.AUDIO_QUALITY) }
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
                    onClick = { onOpenSection(SettingsSection.UPDATE_MODE) }
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
                            IvidsPrimaryButton(
                                onClick = viewModel::downloadAndInstallUpdate,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(strings["update_now"], style = MaterialTheme.typography.labelMedium, color = IvidsColorTokens.ObsidianStage)
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
 * Renders a flat, native YouTube-style push sub-page for [section] directly on [ObsidianStage]
 * with animated back-navigation via [onNavigateBack] instead of a rounded modal popup.
 */
@Composable
fun SettingsDetailScreen(
    section: SettingsSection,
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val strings = LocalStrings.current
    val metrics = LocalLayoutMetrics.current
    val rowHPad = if (metrics.isCompactMobile) 16.dp else 20.dp
    val scope = rememberCoroutineScope()
    var pendingCommit by remember(section) { mutableStateOf(false) }

    val pageTitle = when (section) {
        SettingsSection.LANGUAGE -> strings["language"]
        SettingsSection.INTERFACE_SCALE -> strings["interface_scale"]
        SettingsSection.AUDIO_QUALITY -> strings["default_quality"]
        SettingsSection.UPDATE_MODE -> strings["update_mode"]
    }

    /** Commits [applySelection] immediately and slides back cleanly after 90ms visual feedback. */
    fun commitAndPop(applySelection: () -> Unit) {
        if (pendingCommit) return
        pendingCommit = true
        applySelection()
        scope.launch {
            delay(90L)
            onNavigateBack()
        }
    }

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
            item(key = "detail_title") {
                Text(
                    text = pageTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    color = IvidsColorTokens.ChalkInk,
                    modifier = Modifier.padding(horizontal = rowHPad, vertical = 10.dp)
                )
            }

            when (section) {
                SettingsSection.LANGUAGE -> {
                    items(SupportedLanguage.entries, key = { it.code }) { lang ->
                        SettingsRadioRow(
                            label = lang.displayName,
                            selected = lang == state.language,
                            horizontalPadding = rowHPad,
                            onClick = { commitAndPop { viewModel.selectLanguage(lang) } }
                        )
                    }
                }
                SettingsSection.INTERFACE_SCALE -> {
                    val closestScale = UI_SCALE_PRESETS.minByOrNull { abs(it - state.uiScale) } ?: 1.0f
                    items(UI_SCALE_PRESETS, key = { it }) { preset ->
                        SettingsRadioRow(
                            label = "${(preset * 100).roundToInt()}%",
                            selected = preset == closestScale,
                            horizontalPadding = rowHPad,
                            onClick = { commitAndPop { viewModel.setUiScale(preset) } }
                        )
                    }
                }
                SettingsSection.AUDIO_QUALITY -> {
                    items(AudioQuality.entries, key = { it.name }) { quality ->
                        SettingsRadioRow(
                            label = quality.label,
                            selected = quality == state.defaultQuality,
                            horizontalPadding = rowHPad,
                            onClick = { commitAndPop { viewModel.setDefaultDownloadQuality(quality) } }
                        )
                    }
                }
                SettingsSection.UPDATE_MODE -> {
                    items(UpdateMode.entries, key = { it.name }) { mode ->
                        SettingsRadioRow(
                            label = formatUpdateModeLabel(mode, strings),
                            selected = mode == state.updateMode,
                            horizontalPadding = rowHPad,
                            onClick = { commitAndPop { viewModel.setUpdateMode(mode) } }
                        )
                    }
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
    val contentTint = if (isDestructive) IvidsColorTokens.DangerCoral else IvidsColorTokens.ChalkInk
    val iconTint = if (isDestructive) IvidsColorTokens.DangerCoral else IvidsColorTokens.SlateMuted

    val hoverStyle = if (isDestructive) IvidsHoverStyle.DESTRUCTIVE else IvidsHoverStyle.NEUTRAL

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .clip(RoundedCornerShape(10.dp))
            .ividsHoverZoom(
                hovered = isHovered,
                shape = RoundedCornerShape(10.dp),
                style = hoverStyle
            )
            .pointerHoverIcon(PointerIcon.Hand)
            .hoverable(interactionSource)
            .tvFocusable(shape = RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ividsPressRipple(hoverStyle),
                onClick = onClick
            )
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
 * Renders a full-width, borderless option row inside [SettingsDetailScreen] with crisp white
 * check indicator and zero rounded card wrappers.
 */
@Composable
private fun SettingsRadioRow(
    label: String,
    selected: Boolean,
    horizontalPadding: Dp,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val bg by animateColorAsState(
        targetValue = if (selected) Color(0xFF141414) else Color.Transparent,
        animationSpec = tween(120, easing = FastOutSlowInEasing),
        label = "SettingsRadioBg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .ividsHoverZoom(hovered = isHovered, shape = RoundedCornerShape(10.dp))
            .pointerHoverIcon(PointerIcon.Hand)
            .hoverable(interactionSource)
            .tvFocusable(shape = RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ividsPressRipple(IvidsHoverStyle.NEUTRAL),
                onClick = onClick
            )
            .padding(horizontal = horizontalPadding, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = if (selected) Icons.Rounded.Check else Icons.Rounded.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (selected) IvidsColorTokens.ChalkInk else IvidsColorTokens.SlateSubtle,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = if (selected) IvidsColorTokens.ChalkInk else IvidsColorTokens.SlateMuted,
            modifier = Modifier.weight(1f)
        )
    }
}

/** Maps an [UpdateMode] enum value to its localized label in [AppStrings]. */
private fun formatUpdateModeLabel(mode: UpdateMode, strings: AppStrings): String = when (mode) {
    UpdateMode.AUTO -> strings["update_mode_auto"]
    UpdateMode.MANUAL -> strings["update_mode_manual"]
    UpdateMode.OFF -> strings["update_mode_off"]
    UpdateMode.DEVELOPER -> strings["update_mode_developer"]
}
