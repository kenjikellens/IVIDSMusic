package com.kenjigames.ividsmusic.ui.screens.playlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kenjigames.ividsmusic.domain.model.Playlist
import com.kenjigames.ividsmusic.domain.port.LibraryRepository
import com.kenjigames.ividsmusic.ui.components.ConfirmationDialog
import com.kenjigames.ividsmusic.ui.components.FramedScreenContainer
import com.kenjigames.ividsmusic.ui.components.IvidsHoverStyle
import com.kenjigames.ividsmusic.ui.components.IvidsTonalPill
import com.kenjigames.ividsmusic.ui.components.LivingAlbumHero
import com.kenjigames.ividsmusic.ui.components.ToastController
import com.kenjigames.ividsmusic.ui.components.TrackRowItem
import com.kenjigames.ividsmusic.ui.components.rememberTrackPlaylistModalState
import com.kenjigames.ividsmusic.ui.i18n.LocalStrings
import com.kenjigames.ividsmusic.ui.player.PlayerUiState
import com.kenjigames.ividsmusic.ui.player.PlayerViewModel
import com.kenjigames.ividsmusic.ui.theme.IvidsColorTokens
import com.kenjigames.ividsmusic.ui.theme.LocalDynamicAccent
import com.kenjigames.ividsmusic.ui.theme.ividsScreenPadding
import com.kenjigames.ividsmusic.ui.theme.tvFocusable

/**
 * Displays a framed custom playlist page (max 1360dp) with adaptive mobile padding, [FlowRow]
 * management pills, empty-playlist Play/Shuffle disabled guards, reordering, and track removal.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlaylistDetailScreen(
    viewModel: PlaylistDetailViewModel,
    playerState: PlayerUiState,
    playerViewModel: PlayerViewModel,
    allPlaylists: List<Playlist>,
    likedIds: Set<String>,
    downloadedIds: Set<String>,
    libraryRepository: LibraryRepository,
    toastController: ToastController,
    onOpenArtist: (String, String) -> Unit,
    onOpenSongDetail: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val strings = LocalStrings.current
    val accent = LocalDynamicAccent.current
    val playlistModal = rememberTrackPlaylistModalState(allPlaylists, libraryRepository, toastController)

    LaunchedEffect(state.isDeleted) {
        if (state.isDeleted) onNavigateBack()
    }

    if (state.showDeleteConfirm) {
        ConfirmationDialog(
            title = strings["delete_playlist"],
            message = state.playlist?.title.orEmpty(),
            confirmLabel = strings["delete_playlist"],
            isDestructive = true,
            onConfirm = viewModel::deletePlaylist,
            onDismiss = { viewModel.setDeleteConfirmVisible(false) }
        )
    }

    FramedScreenContainer(
        maxWidth = 1360.dp,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = ividsScreenPadding(top = 22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            val playlist = state.playlist
            if (playlist == null) {
                item(key = "pl_empty") {
                    Text(strings["loading"], color = IvidsColorTokens.SlateMuted)
                }
            } else {
                item(key = "pl_hero") {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        LivingAlbumHero(
                            eyebrow = strings["playlists"],
                            title = playlist.title,
                            subtitle = "${playlist.description.ifBlank { strings["library_desc"] }} • ${playlist.tracks.size} ${strings["songs"]}",
                            artworkUrl = playlist.tracks.firstOrNull()?.coverUrl,
                            primaryActionLabel = strings["play_all"],
                            primaryEnabled = playlist.tracks.isNotEmpty(),
                            onPrimaryAction = {
                                if (playlist.tracks.isNotEmpty()) playerViewModel.playQueue(playlist.tracks)
                            },
                            secondaryActionLabel = strings["shuffle"],
                            secondaryEnabled = playlist.tracks.isNotEmpty(),
                            onSecondaryAction = {
                                if (playlist.tracks.isNotEmpty()) playerViewModel.playQueue(playlist.tracks, shuffled = true)
                            }
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            IvidsTonalPill(
                                onClick = { viewModel.setEditing(!state.isEditing) },
                                baseColor = IvidsColorTokens.GlassSurface,
                                hoverColor = IvidsColorTokens.GlassHover,
                                baseBorderColor = IvidsColorTokens.HairlineSubtle
                            ) {
                                Icon(Icons.Rounded.Edit, null, tint = IvidsColorTokens.SlateMuted, modifier = Modifier.size(16.dp))
                                Text(
                                    text = strings["rename"],
                                    style = MaterialTheme.typography.labelLarge,
                                    color = IvidsColorTokens.ChalkInk,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            IvidsTonalPill(
                                onClick = { viewModel.setDeleteConfirmVisible(true) },
                                baseColor = IvidsColorTokens.DangerCoral.copy(alpha = 0.08f),
                                hoverStyle = IvidsHoverStyle.DESTRUCTIVE
                            ) {
                                Icon(Icons.Rounded.Delete, null, tint = IvidsColorTokens.DangerCoral, modifier = Modifier.size(16.dp))
                                Text(
                                    text = strings["delete_playlist"],
                                    style = MaterialTheme.typography.labelLarge,
                                    color = IvidsColorTokens.DangerCoral,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        if (state.isEditing) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = IvidsColorTokens.GlassSurface
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedTextField(
                                        value = state.editTitle,
                                        onValueChange = viewModel::onEditTitleChanged,
                                        label = { Text(strings["playlist_name"]) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = state.editDescription,
                                        onValueChange = viewModel::onEditDescriptionChanged,
                                        label = { Text(strings["playlist_desc"]) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Button(
                                            onClick = viewModel::saveMetadata,
                                            shape = CircleShape,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = IvidsColorTokens.ChalkInk,
                                                contentColor = IvidsColorTokens.ObsidianStage
                                            )
                                        ) {
                                            Text(strings["profile_save"])
                                        }
                                        TextButton(onClick = { viewModel.setEditing(false) }) {
                                            Text(strings["cancel"], color = IvidsColorTokens.SlateMuted)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (playlist.tracks.isEmpty()) {
                    item(key = "pl_no_tracks") {
                        Text(
                            text = strings["library_empty_desc"],
                            style = MaterialTheme.typography.bodyMedium,
                            color = IvidsColorTokens.SlateMuted
                        )
                    }
                } else {
                    itemsIndexed(playlist.tracks, key = { idx, t -> "pl_${playlist.id}_${idx}_${t.id}" }) { idx, track ->
                        TrackRowItem(
                            index = idx + 1,
                            track = track,
                            isCurrentlyPlaying = playerState.currentTrack?.id == track.id,
                            isLiked = track.id in likedIds || track.isLiked,
                            isDownloaded = track.id in downloadedIds || track.isDownloaded,
                            onPlay = { playerViewModel.playTrack(track, playlist.tracks) },
                            onToggleLike = { playerViewModel.toggleTrackLike(track) },
                            onPlayNext = { playerViewModel.enqueueNext(track) },
                            onAddToQueue = { playerViewModel.enqueueLast(track) },
                            onAddToPlaylist = { playlistModal.openPicker(track) },
                            onDownload = { playerViewModel.downloadTrack(track) },
                            onOpenArtist = { onOpenArtist(track.artistId, track.artist) },
                            onOpenSongDetail = { onOpenSongDetail(track.id) },
                            trailingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { viewModel.moveTrack(idx, -1) },
                                        enabled = idx > 0,
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Rounded.ArrowUpward, strings["move_up"], tint = IvidsColorTokens.SlateMuted, modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = { viewModel.moveTrack(idx, 1) },
                                        enabled = idx < playlist.tracks.lastIndex,
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Rounded.ArrowDownward, strings["move_down"], tint = IvidsColorTokens.SlateMuted, modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = { viewModel.removeTrack(track.id) },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Rounded.RemoveCircleOutline, strings["remove"], tint = IvidsColorTokens.DangerCoral, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
