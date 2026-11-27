package com.kenjigames.ividsmusic.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.kenjigames.ividsmusic.domain.model.Track
import com.kenjigames.ividsmusic.ui.components.BlurredArtworkBackdrop
import com.kenjigames.ividsmusic.ui.components.FramedScreenContainer
import com.kenjigames.ividsmusic.ui.components.IvidsIconButton
import com.kenjigames.ividsmusic.ui.components.IvidsPrimaryButton
import com.kenjigames.ividsmusic.ui.components.IvidsTonalPill
import com.kenjigames.ividsmusic.ui.components.MusicCard
import com.kenjigames.ividsmusic.ui.components.ividsSearchFieldColors
import com.kenjigames.ividsmusic.ui.i18n.LocalStrings
import com.kenjigames.ividsmusic.ui.player.PlayerViewModel
import com.kenjigames.ividsmusic.ui.theme.IvidsColorTokens
import com.kenjigames.ividsmusic.ui.theme.IvidsTextStyles
import com.kenjigames.ividsmusic.ui.theme.LocalLayoutMetrics
import com.kenjigames.ividsmusic.ui.theme.ividsScreenPadding

/**
 * Displays the framed listener profile page (max 900dp) with an avatar header, compact stat
 * tiles, recent listening carousel, and interactive top genre breakdown.
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
    val metrics = LocalLayoutMetrics.current
    val displayName = state.profile.displayName.ifBlank { strings["listener"] }
    val avatarInitial = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "I"
    val recentCover = state.recentlyPlayed.firstOrNull()?.track?.coverUrl

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
                        shape = RoundedCornerShape(14.dp),
                        colors = ividsSearchFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.editBio,
                        onValueChange = viewModel::onEditBioChanged,
                        label = { Text(strings["profile_bio_label"]) },
                        placeholder = { Text(strings["profile_bio_placeholder"]) },
                        maxLines = 3,
                        shape = RoundedCornerShape(14.dp),
                        colors = ividsSearchFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                IvidsPrimaryButton(
                    onClick = viewModel::saveProfile,
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 9.dp)
                ) {
                    Text(strings["profile_save"], color = IvidsColorTokens.ObsidianStage)
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
            contentPadding = ividsScreenPadding(top = if (metrics.isCompactMobile) 14.dp else 24.dp),
            verticalArrangement = Arrangement.spacedBy(if (metrics.isCompactMobile) 18.dp else 22.dp)
        ) {
            item(key = "profile_header") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(if (metrics.isCompactMobile) 18.dp else 22.dp),
                    color = IvidsColorTokens.GlassSurface
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(if (metrics.isCompactMobile) 18.dp else 22.dp))
                    ) {
                        if (!recentCover.isNullOrBlank()) {
                            BlurredArtworkBackdrop(
                                artworkUrl = recentCover,
                                tintColor = null,
                                scrimAlpha = 0.74f,
                                fallbackColor = IvidsColorTokens.GlassSurface,
                                modifier = Modifier.matchParentSize()
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(if (metrics.isCompactMobile) 16.dp else 22.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(if (metrics.isCompactMobile) 14.dp else 18.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(if (metrics.isCompactMobile) 56.dp else 68.dp)
                                    .clip(CircleShape)
                                    .background(IvidsColorTokens.PrimaryCyan.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!recentCover.isNullOrBlank()) {
                                    AsyncImage(
                                        model = recentCover,
                                        contentDescription = displayName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(
                                        text = avatarInitial,
                                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                        color = IvidsColorTokens.PrimaryCyan
                                    )
                                }
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = displayName,
                                    style = if (metrics.isCompactMobile) {
                                        MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                    } else {
                                        MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                                    },
                                    color = IvidsColorTokens.ChalkInk,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (state.profile.bio.isNotBlank()) {
                                    Text(
                                        text = state.profile.bio,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = IvidsColorTokens.SlateMuted,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Row(
                                    modifier = Modifier.padding(top = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IvidsTonalPill(
                                        onClick = { viewModel.setEditModalOpen(true) },
                                        baseColor = IvidsColorTokens.RecessedTrough,
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Edit,
                                            contentDescription = null,
                                            tint = IvidsColorTokens.ChalkInk,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = strings["edit_profile"],
                                            style = MaterialTheme.typography.labelLarge,
                                            color = IvidsColorTokens.ChalkInk,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                    if (!metrics.isCompactMobile) {
                                        IvidsIconButton(onClick = onOpenDownloader, size = 36.dp) {
                                            Icon(
                                                imageVector = Icons.Rounded.Download,
                                                contentDescription = strings["nav_downloader"],
                                                tint = IvidsColorTokens.SlateMuted,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IvidsIconButton(onClick = onOpenSettings, size = 36.dp) {
                                            Icon(
                                                imageVector = Icons.Rounded.Settings,
                                                contentDescription = strings["settings"],
                                                tint = IvidsColorTokens.SlateMuted,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
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

            if (state.recentlyPlayed.isEmpty() && state.topGenres.isEmpty()) {
                item(key = "profile_empty") {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = IvidsColorTokens.GlassSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = strings["no_history"],
                            style = MaterialTheme.typography.bodyMedium,
                            color = IvidsColorTokens.SlateMuted,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            } else {
                if (state.recentlyPlayed.isNotEmpty()) {
                    item(key = "profile_recent") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = strings["recently_played"],
                                style = MaterialTheme.typography.titleLarge,
                                color = IvidsColorTokens.ChalkInk
                            )
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

                if (state.topGenres.isNotEmpty()) {
                    item(key = "profile_genres") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = strings["top_genres"],
                                style = MaterialTheme.typography.titleLarge,
                                color = IvidsColorTokens.ChalkInk
                            )
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = IvidsColorTokens.GlassSurface
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    state.topGenres.forEach { (genre, weight) ->
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .pointerHoverIcon(PointerIcon.Hand)
                                                .clickable { onOpenGenreSearch(genre) }
                                                .padding(vertical = 4.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = genre,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = IvidsColorTokens.ChalkInk
                                                )
                                                Text(
                                                    text = "${(weight * 100).toInt()}%",
                                                    style = IvidsTextStyles.tabularBadge(),
                                                    color = IvidsColorTokens.SlateMuted
                                                )
                                            }
                                            LinearProgressIndicator(
                                                progress = { weight },
                                                color = IvidsColorTokens.PrimaryCyan,
                                                trackColor = IvidsColorTokens.RecessedTrough,
                                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape)
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
        shape = RoundedCornerShape(16.dp),
        color = IvidsColorTokens.GlassSurface
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(if (isCompact) 12.dp else 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
        ) {
            Text(
                text = value,
                style = (if (isCompact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium)
                    .copy(fontFeatureSettings = "tnum", fontWeight = FontWeight.Bold),
                color = IvidsColorTokens.PrimaryCyan
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = IvidsColorTokens.SlateMuted,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
