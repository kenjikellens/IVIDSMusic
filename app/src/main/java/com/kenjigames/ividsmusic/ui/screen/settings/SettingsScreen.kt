package com.kenjigames.ividsmusic.ui.screen.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kenjigames.ividsmusic.ui.theme.DarkSurface
import com.kenjigames.ividsmusic.ui.theme.DarkSurfaceVariant
import com.kenjigames.ividsmusic.ui.theme.GlassBorder
import com.kenjigames.ividsmusic.ui.theme.PrimaryAccent
import com.kenjigames.ividsmusic.ui.theme.SecondaryAccent
import com.kenjigames.ividsmusic.ui.theme.TextMuted
import com.kenjigames.ividsmusic.ui.theme.TextPrimary
import com.kenjigames.ividsmusic.ui.theme.TextSecondary
import com.kenjigames.ividsmusic.ui.theme.Typography

/** Supported language option data model */
private data class LanguageOption(val code: String, val name: String, val nativeName: String)

private val supportedLanguages = listOf(
    LanguageOption("en", "English", "English"),
    LanguageOption("nl", "Dutch", "Nederlands"),
    LanguageOption("de", "German", "Deutsch"),
    LanguageOption("fr", "French", "Français"),
    LanguageOption("es", "Spanish", "Español"),
    LanguageOption("it", "Italian", "Italiano"),
    LanguageOption("pt", "Portuguese", "Português"),
    LanguageOption("ro", "Romanian", "Română"),
    LanguageOption("ru", "Russian", "Русский"),
    LanguageOption("zh", "Chinese", "中文"),
    LanguageOption("hi", "Hindi", "हिन्दी"),
    LanguageOption("ar", "Arabic", "العربية"),
    LanguageOption("ja", "Japanese", "日本語")
)

/** Preset scale values */
private val scalePresets = listOf(0.75f, 0.90f, 1.00f, 1.10f, 1.25f, 1.50f)

/**
 * Premium Settings screen composable featuring UI scale slider, language selector, cache controls, and version info.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentScale = uiState.userPreferences.uiScale
    val currentLang = uiState.userPreferences.languageCode

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 80.dp)
    ) {
        // Page Title
        Text(
            text = "Settings",
            style = Typography.headlineLarge,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 1. Interface Scale Section
        SettingsSectionHeader(
            title = "Interface Scale",
            icon = Icons.Default.AspectRatio
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Scale Factor",
                        style = Typography.titleMedium,
                        color = TextPrimary
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PrimaryAccent.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, PrimaryAccent)
                    ) {
                        Text(
                            text = "${(currentScale * 100).toInt()}%" + if (currentScale == 1.0f) " (Default)" else "",
                            style = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = PrimaryAccent,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Continuous Slider
                Slider(
                    value = currentScale,
                    onValueChange = { viewModel.setUiScale(it) },
                    valueRange = 0.75f..1.50f,
                    steps = 5,
                    colors = SliderDefaults.colors(
                        thumbColor = PrimaryAccent,
                        activeTrackColor = PrimaryAccent,
                        inactiveTrackColor = DarkSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable Preset Chips
                LazyRow(
                    contentPadding = PaddingValues(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(scalePresets) { scale ->
                        val isSelected = (currentScale * 100).toInt() == (scale * 100).toInt()
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { viewModel.setUiScale(scale) },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) PrimaryAccent else DarkSurfaceVariant,
                            border = if (isSelected) BorderStroke(1.5.dp, Color.White) else BorderStroke(1.dp, GlassBorder)
                        ) {
                            Text(
                                text = "${(scale * 100).toInt()}%",
                                style = Typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isSelected) Color.White else TextSecondary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. Language Section
        SettingsSectionHeader(
            title = "Language",
            icon = Icons.Default.Language
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Select application display language:",
                    style = Typography.bodyMedium,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    contentPadding = PaddingValues(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(supportedLanguages, key = { it.code }) { lang ->
                        val isSelected = currentLang == lang.code
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { viewModel.setLanguage(lang.code) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) PrimaryAccent.copy(alpha = 0.25f) else DarkSurfaceVariant,
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) PrimaryAccent else GlassBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = PrimaryAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = "${lang.nativeName} (${lang.code.uppercase()})",
                                    style = Typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) TextPrimary else TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3. Storage & Cache Section
        SettingsSectionHeader(
            title = "Storage & Cache",
            icon = Icons.Default.Storage
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Audio Stream Cache", style = Typography.titleMedium, color = TextPrimary)
                        Text(text = "Max Cache Limit: 500 MB", style = Typography.bodyMedium, color = TextMuted)
                    }

                    OutlinedButton(
                        onClick = { /* Clear cache action */ },
                        border = BorderStroke(1.dp, SecondaryAccent.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear",
                            tint = SecondaryAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Clear Cache", color = SecondaryAccent, style = Typography.bodyMedium)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 4. About & Version Section
        SettingsSectionHeader(
            title = "About",
            icon = Icons.Default.Info
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(PrimaryAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "IV",
                            style = Typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "IVIDS Music",
                            style = Typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = "Version: ${uiState.appVersion}",
                            style = Typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { /* Check updates action */ },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Check for Updates",
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = uiState.updateStatus,
                        style = Typography.bodyMedium,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

/** Section header composable with icon and title */
@Composable
private fun SettingsSectionHeader(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = PrimaryAccent,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = Typography.titleLarge,
            color = TextPrimary
        )
    }
}
