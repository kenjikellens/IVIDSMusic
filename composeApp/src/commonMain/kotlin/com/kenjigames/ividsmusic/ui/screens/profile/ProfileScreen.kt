package com.kenjigames.ividsmusic.ui.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kenjigames.ividsmusic.domain.model.Track
import com.kenjigames.ividsmusic.ui.components.LivingAlbumHero
import com.kenjigames.ividsmusic.ui.components.MusicCard
import com.kenjigames.ividsmusic.ui.i18n.LocalStrings
import com.kenjigames.ividsmusic.ui.player.PlayerViewModel
import com.kenjigames.ividsmusic.ui.theme.IvidsColorTokens
import com.kenjigames.ividsmusic.ui.theme.IvidsTextStyles
import com.kenjigames.ividsmusic.ui.theme.LocalDynamicAccent
import com.kenjigames.ividsmusic.ui.theme.tvFocusable

/**
 * Displays local listener profile card, stat counters (Liked Songs, Playlists, Minutes Played),
 * last 20 recently played tracks, and top listened genre weights.
 */
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    playerViewModel: PlayerViewModel,
    onOpenSongDetail: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDownloader: () -> Unit,
    onOpenGenreSearch: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val strings = LocalStrings.current
    val accent = LocalDynamicAccent.current

    if (state.isEditModalOpen) {
        AlertDialog(
            onDismissRequest = { viewModel.setEditModalOpen(false) },
            containerColor = IvidsColorTokens.VinylSurface,
            title = { Text(strings["profile_edit_title"], color = IvidsColorTokens.ChalkInk) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = state.editName,
                        onValueChange = viewModel::onEditNameChanged,
                        label = { Text(strings["profile_name_label"]) },
                        placeholder = { Text(strings["profile_name_placeholder"]) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.editBio,
                        onValueChange = viewModel::onEditBioChanged,
                        label = { Text(strings["profile_bio_label"]) },
                        placeholder = { Text(strings["profile_bio_placeholder"]) },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = viewModel::saveProfile,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = IvidsColorTokens.OnAccentDark
                    )
                ) {
                    Text(strings["profile_save"])
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setEditModalOpen(false) }) {
                    Text(strings["close"], color = IvidsColorTokens.SlateMuted)
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        item(key = "profile_hero") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LivingAlbumHero(
                    eyebrow = strings["you"],
                    title = state.profile.displayName.ifBlank { strings["listener"] },
                    subtitle = state.profile.bio.ifBlank { strings["no_bio"] },
                    artworkUrl = state.recentlyPlayed.firstOrNull()?.track?.coverUrl,
                    primaryActionLabel = strings["edit_profile"],
                    onPrimaryAction = { viewModel.setEditModalOpen(true) },
                    secondaryActionLabel = strings["settings"],
                    onSecondaryAction = onOpenSettings
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.setEditModalOpen(true) },
                        border = BorderStroke(1.dp, IvidsColorTokens.HairlineBorder),
                        modifier = Modifier.tvFocusable(shape = RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Rounded.Edit, null, tint = accent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings["edit_profile"], color = IvidsColorTokens.ChalkInk)
                    }
                    OutlinedButton(
                        onClick = onOpenDownloader,
                        border = BorderStroke(1.dp, IvidsColorTokens.HairlineBorder),
                        modifier = Modifier.tvFocusable(shape = RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Rounded.Download, null, tint = accent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings["nav_downloader"], color = IvidsColorTokens.ChalkInk)
                    }
                    OutlinedButton(
                        onClick = onOpenSettings,
                        border = BorderStroke(1.dp, IvidsColorTokens.HairlineBorder),
                        modifier = Modifier.tvFocusable(shape = RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Rounded.Settings, null, tint = accent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings["settings"], color = IvidsColorTokens.ChalkInk)
                    }
                }
            }
        }

        item(key = "profile_stats") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileStatCard(
                    value = state.likedCount.toString(),
                    label = strings["liked_songs"],
                    modifier = Modifier.weight(1f)
                )
                ProfileStatCard(
                    value = state.playlistCount.toString(),
                    label = strings["playlists"],
                    modifier = Modifier.weight(1f)
                )
                ProfileStatCard(
                    value = state.minutesPlayed.toString(),
                    label = strings["minutes_played"],
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item(key = "profile_recent") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = strings["recently_played"],
                    style = MaterialTheme.typography.headlineSmall,
                    color = IvidsColorTokens.ChalkInk
                )
                if (state.recentlyPlayed.isEmpty()) {
                    Text(
                        text = strings["no_history"],
                        style = MaterialTheme.typography.bodyMedium,
                        color = IvidsColorTokens.SlateMuted
                    )
                } else {
                    val tracks: List<Track> = state.recentlyPlayed.map { it.track }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(tracks, key = { "pr_${it.id}" }) { track ->
                            MusicCard(
                                title = track.title,
                                subtitle = track.artist,
                                artworkUrl = track.coverUrl,
                                onClick = { onOpenSongDetail(track.id) },
                                onPlayClick = { playerViewModel.playTrack(track, tracks) }
                            )
                        }
                    }
                }
            }
        }

        item(key = "profile_genres") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = strings["top_genres"],
                    style = MaterialTheme.typography.headlineSmall,
                    color = IvidsColorTokens.ChalkInk
                )
                if (state.topGenres.isEmpty()) {
                    Text(
                        text = strings["no_history"],
                        style = MaterialTheme.typography.bodyMedium,
                        color = IvidsColorTokens.SlateMuted
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = IvidsColorTokens.VinylSurface,
                        border = BorderStroke(1.dp, IvidsColorTokens.HairlineBorder)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            state.topGenres.forEach { (genre, weight) ->
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        TextButton(onClick = { onOpenGenreSearch(genre) }, contentPadding = PaddingValues(0.dp)) {
                                            Text(genre, style = MaterialTheme.typography.titleSmall, color = IvidsColorTokens.ChalkInk)
                                        }
                                        Text(
                                            text = "${(weight * 100).toInt()}%",
                                            style = IvidsTextStyles.tabularBadge(),
                                            color = accent
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { weight },
                                        color = accent,
                                        trackColor = IvidsColorTokens.HairlineBorder,
                                        modifier = Modifier.fillMaxWidth().height(6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Renders a single tabular numeric metric tile inside [ProfileScreen]'s stats grid.
 */
@Composable
private fun ProfileStatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    val accent = LocalDynamicAccent.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = IvidsColorTokens.GrooveElevated,
        border = BorderStroke(1.dp, IvidsColorTokens.HairlineBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum"),
                color = accent
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = IvidsColorTokens.SlateMuted
            )
        }
    }
}
