package com.kenjigames.ividsmusic.ui.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kenjigames.ividsmusic.domain.model.Track
import com.kenjigames.ividsmusic.ui.components.FramedScreenContainer
import com.kenjigames.ividsmusic.ui.components.IvidsTonalPill
import com.kenjigames.ividsmusic.ui.components.LivingAlbumHero
import com.kenjigames.ividsmusic.ui.components.MusicCard
import com.kenjigames.ividsmusic.ui.i18n.LocalStrings
import com.kenjigames.ividsmusic.ui.player.PlayerViewModel
import com.kenjigames.ividsmusic.ui.theme.IvidsColorTokens
import com.kenjigames.ividsmusic.ui.theme.IvidsTextStyles
import com.kenjigames.ividsmusic.ui.theme.LocalDynamicAccent
import com.kenjigames.ividsmusic.ui.theme.LocalLayoutMetrics
import com.kenjigames.ividsmusic.ui.theme.ividsScreenPadding
import com.kenjigames.ividsmusic.ui.theme.tvFocusable

/**
 * Displays the framed listener profile card (max 900dp) with adaptive mobile stat tiles of equal
 * height, deduplicated [FlowRow] action pills, listening history, and top genre weights.
 */
@OptIn(ExperimentalLayoutApi::class)
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
    val metrics = LocalLayoutMetrics.current

    if (state.isEditModalOpen) {
        AlertDialog(
            onDismissRequest = { viewModel.setEditModalOpen(false) },
            containerColor = IvidsColorTokens.GlassSurface,
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
                    shape = CircleShape,
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

    FramedScreenContainer(
        maxWidth = 900.dp,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = ividsScreenPadding(top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(if (metrics.isCompactMobile) 18.dp else 24.dp)
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
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProfilePillAction(
                            icon = Icons.Rounded.Download,
                            label = strings["nav_downloader"],
                            accent = accent,
                            onClick = onOpenDownloader
                        )
                    }
                }
            }

            item(key = "profile_stats") {
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(if (metrics.isCompactMobile) 10.dp else 14.dp)
                ) {
                    ProfileStatCard(
                        value = state.likedCount.toString(),
                        label = strings["liked_songs"],
                        isCompact = metrics.isCompactMobile,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    ProfileStatCard(
                        value = state.playlistCount.toString(),
                        label = strings["playlists"],
                        isCompact = metrics.isCompactMobile,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    ProfileStatCard(
                        value = state.minutesPlayed.toString(),
                        label = strings["minutes_played"],
                        isCompact = metrics.isCompactMobile,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }

            item(key = "profile_recent") {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
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
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                            shape = RoundedCornerShape(20.dp),
                            color = IvidsColorTokens.GlassSurface
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                state.topGenres.forEach { (genre, weight) ->
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextButton(
                                                onClick = { onOpenGenreSearch(genre) },
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Text(genre, style = MaterialTheme.typography.titleMedium, color = IvidsColorTokens.ChalkInk)
                                            }
                                            Text(
                                                text = "${(weight * 100).toInt()}%",
                                                style = IvidsTextStyles.tabularBadge(),
                                                color = IvidsColorTokens.SlateMuted
                                            )
                                        }
                                        LinearProgressIndicator(
                                            progress = { weight },
                                            color = IvidsColorTokens.ChalkInk,
                                            trackColor = IvidsColorTokens.RecessedTrough,
                                            modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape)
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
}

/**
 * Renders a sleek tonal pill shortcut button inside [ProfileScreen] with single-line non-wrapping text.
 */
@Composable
private fun ProfilePillAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    IvidsTonalPill(
        onClick = onClick,
        baseColor = IvidsColorTokens.GlassSurface,
        hoverColor = IvidsColorTokens.GlassHover,
        baseBorderColor = IvidsColorTokens.HairlineSubtle
    ) {
        Icon(icon, null, tint = IvidsColorTokens.SlateMuted, modifier = Modifier.size(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = IvidsColorTokens.ChalkInk,
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * Renders a borderless tabular numeric metric tile inside [ProfileScreen]'s stats grid with
 * centered labels and uniform row height.
 */
@Composable
private fun ProfileStatCard(
    value: String,
    label: String,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = IvidsColorTokens.GlassSurface
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(if (isCompact) 14.dp else 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum"),
                color = IvidsColorTokens.ChalkInk
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = IvidsColorTokens.SlateMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}
