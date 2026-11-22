package com.kenjigames.ividsmusic.ui.screens.playlist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.material3.OutlinedButton
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
import com.kenjigames.ividsmusic.ui.components.LivingAlbumHero
import com.kenjigames.ividsmusic.ui.components.ToastController
import com.kenjigames.ividsmusic.ui.components.TrackRowItem
import com.kenjigames.ividsmusic.ui.components.rememberTrackPlaylistModalState
import com.kenjigames.ividsmusic.ui.i18n.LocalStrings
import com.kenjigames.ividsmusic.ui.player.PlayerUiState
import com.kenjigames.ividsmusic.ui.player.PlayerViewModel
import com.kenjigames.ividsmusic.ui.theme.IvidsColorTokens
import com.kenjigames.ividsmusic.ui.theme.LocalDynamicAccent
import com.kenjigames.ividsmusic.ui.theme.tvFocusable

/**
 * Displays a custom playlist with inline title/description editing, track reordering, track removal,
 * batch Play/Shuffle controls, and playlist deletion.
 */
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

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val playlist = state.playlist
        if (playlist == null) {
            item(key = "pl_empty") {
                Text(strings["loading"], color = IvidsColorTokens.SlateMuted)
            }
        } else {
            item(key = "pl_hero") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LivingAlbumHero(
                        eyebrow = strings["playlists"],
                        title = playlist.title,
                        subtitle = "${playlist.description.ifBlank { strings["library_desc"] }} • ${playlist.tracks.size} ${strings["songs"]}",
                        artworkUrl = playlist.tracks.firstOrNull()?.coverUrl,
                        primaryActionLabel = strings["play_all"],
                        onPrimaryAction = {
                            if (playlist.tracks.isNotEmpty()) playerViewModel.playQueue(playlist.tracks)
                        },
                        secondaryActionLabel = strings["shuffle"],
                        onSecondaryAction = {
                            if (playlist.tracks.isNotEmpty()) playerViewModel.playQueue(playlist.tracks, shuffled = true)
                        }
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.setEditing(!state.isEditing) },
                            border = BorderStroke(1.dp, IvidsColorTokens.HairlineBorder),
                            modifier = Modifier.tvFocusable(shape = RoundedCornerShape(12.dp))
                        ) {
                            Icon(Icons.Rounded.Edit, null, tint = accent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings["rename"], color = IvidsColorTokens.ChalkInk)
                        }
                        OutlinedButton(
                            onClick = { viewModel.setDeleteConfirmVisible(true) },
                            border = BorderStroke(1.dp, IvidsColorTokens.DangerCoral.copy(alpha = 0.5f)),
                            modifier = Modifier.tvFocusable(shape = RoundedCornerShape(12.dp))
                        ) {
                            Icon(Icons.Rounded.Delete, null, tint = IvidsColorTokens.DangerCoral, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings["delete_playlist"], color = IvidsColorTokens.DangerCoral)
                        }
                    }

                    if (state.isEditing) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = IvidsColorTokens.VinylSurface,
                            border = BorderStroke(1.dp, accent.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = accent,
                                            contentColor = IvidsColorTokens.OnAccentDark
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
