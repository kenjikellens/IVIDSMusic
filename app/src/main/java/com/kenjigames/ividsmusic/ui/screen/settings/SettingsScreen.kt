package com.kenjigames.ividsmusic.ui.screen.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kenjigames.ividsmusic.ui.theme.DarkSurface
import com.kenjigames.ividsmusic.ui.theme.PrimaryAccent
import com.kenjigames.ividsmusic.ui.theme.TextMuted
import com.kenjigames.ividsmusic.ui.theme.TextPrimary
import com.kenjigames.ividsmusic.ui.theme.Typography

/**
 * Settings screen composable featuring UI scale controls, 13-language selector, and app version info.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentScale = uiState.userPreferences.uiScale

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 80.dp)
    ) {
        Text(
            text = "Settings",
            style = Typography.headlineLarge,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Appearance section
        Text(text = "Interface Scale", style = Typography.titleLarge, color = TextPrimary)
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Choose UI scale factor:", style = Typography.bodyLarge, color = TextMuted)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val scales = listOf(0.75f, 0.9f, 1.0f, 1.1f, 1.25f, 1.5f)
                    scales.forEach { scale ->
                        val label = "${(scale * 100).toInt()}%"
                        val isSelected = currentScale == scale
                        Button(
                            onClick = { viewModel.setUiScale(scale) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) PrimaryAccent else DarkSurface
                            ),
                            modifier = Modifier.weight(1f).padding(2.dp)
                        ) {
                            Text(text = label, style = Typography.labelMedium)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // About section
        Text(text = "About", style = Typography.titleLarge, color = TextPrimary)
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "IVIDS Music", style = Typography.titleMedium, color = TextPrimary)
                Text(text = "Version: ${uiState.appVersion}", style = Typography.bodyMedium, color = TextMuted)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = uiState.updateStatus, style = Typography.bodyMedium, color = PrimaryAccent)
            }
        }
    }
}
