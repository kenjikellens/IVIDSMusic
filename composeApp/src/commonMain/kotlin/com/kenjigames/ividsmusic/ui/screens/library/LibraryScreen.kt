package com.kenjigames.ividsmusic.ui.screens.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kenjigames.ividsmusic.domain.port.LibraryRepository
import com.kenjigames.ividsmusic.ui.components.CreatePlaylistDialog
import com.kenjigames.ividsmusic.ui.components.LivingAlbumHero
import com.kenjigames.ividsmusic.ui.components.MusicCard
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
 * Renders the user's local Library with tabs for Liked Songs, Saved Albums, and Custom Playlists,
 * real-time search filtering, Recently Listened history, and batch Play/Shuffle actions.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    playerState: PlayerUiState,
    playerViewModel: PlayerViewModel,
    likedIds: Set<String>,
    downloadedIds: Set<String>,
    libraryRepository: LibraryRepository,
    toastController: ToastController,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String, String) -> Unit,
    onOpenSongDetail: (String) -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    onExploreMusic: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val strings = LocalStrings.current
    val accent = LocalDynamicAccent.current
    val playlistModal = rememberTrackPlaylistModalState(state.playlists, libraryRepository, toastController)

    if (state.isCreatePlaylistOpen) {
        CreatePlaylistDialog(
            onDismiss = { viewModel.setCreatePlaylistDialogVisible(false) },
            onCreate = viewModel::createPlaylist
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item(key = "library_hero") {
            LivingAlbumHero(
                eyebrow = strings["library"],
                title = strings["library"],
                subtitle = strings["library_desc"],
                artworkUrl = state.likedTracks.firstOrNull()?.coverUrl,
                primaryActionLabel = strings["play_all"],
                onPrimaryAction = {
                    val tracks = state.filteredLikedTracks
                    if (tracks.isNotEmpty()) {
                        playerViewModel.playQueue(tracks)
                    } else {
                        onExploreMusic()
                    }
                },
                secondaryActionLabel = strings["create_playlist"],
                onSecondaryAction = { viewModel.setCreatePlaylistDialogVisible(true) }
            )
        }

        item(key = "library_controls") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = viewModel::onSearchQueryChanged,
                        placeholder = { Text(strings["search_library_placeholder"], color = IvidsColorTokens.SlateMuted) },
                        leadingIcon = { Icon(Icons.Rounded.Search, null, tint = accent) },
                        trailingIcon = {
                            if (state.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                    Icon(Icons.Rounded.Close, strings["close"], tint = IvidsColorTokens.SlateMuted)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = IvidsColorTokens.GrooveElevated,
                            unfocusedContainerColor = IvidsColorTokens.VinylSurface,
                            focusedBorderColor = accent,
                            unfocusedBorderColor = IvidsColorTokens.HairlineBorder,
                            focusedTextColor = IvidsColorTokens.ChalkInk,
                            unfocusedTextColor = IvidsColorTokens.ChalkInk
                        ),
                        modifier = Modifier.weight(1f).tvFocusable(shape = RoundedCornerShape(14.dp))
                    )
                    OutlinedButton(
                        onClick = {
                            val tracks = state.filteredLikedTracks
                            if (tracks.isNotEmpty()) playerViewModel.playQueue(tracks, shuffled = true)
                        },
                        enabled = state.filteredLikedTracks.isNotEmpty(),
                        border = BorderStroke(1.dp, IvidsColorTokens.HairlineBorder),
                        modifier = Modifier.tvFocusable(shape = RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Rounded.Shuffle, null, tint = accent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings["shuffle"], color = IvidsColorTokens.ChalkInk)
                    }
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val tabs = listOf(
                        LibraryTab.LIKED_SONGS to "${strings["liked_songs"]} (${state.likedTracks.size})",
                        LibraryTab.SAVED_ALBUMS to "${strings["saved_albums"]} (${state.savedAlbums.size})",
                        LibraryTab.PLAYLISTS to "${strings["playlists"]} (${state.playlists.size})"
                    )
                    tabs.forEach { (tab, label) ->
                        val selected = state.selectedTab == tab
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.selectTab(tab) },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accent,
                                selectedLabelColor = IvidsColorTokens.OnAccentDark,
                                containerColor = IvidsColorTokens.VinylSurface,
                                labelColor = IvidsColorTokens.ChalkInk
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = IvidsColorTokens.HairlineBorder
                            )
                        )
                    }
                }
            }
        }

        if (state.recentlyListened.isNotEmpty()) {
            item(key = "library_recent") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings["recently_listened"],
                            style = MaterialTheme.typography.titleLarge,
                            color = IvidsColorTokens.ChalkInk
                        )
                        TextButton(onClick = viewModel::clearHistory) {
                            Text(strings["clear_all"], color = IvidsColorTokens.SlateMuted)
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        val recentTracks = state.recentlyListened.map { it.track }
                        items(recentTracks, key = { "lh_${it.id}" }) { track ->
                            MusicCard(
                                title = track.title,
                                subtitle = track.artist,
                                artworkUrl = track.coverUrl,
                                onClick = { onOpenSongDetail(track.id) },
                                onPlayClick = { playerViewModel.playTrack(track, recentTracks) }
                            )
                        }
                    }
                }
            }
        }

        when (state.selectedTab) {
            LibraryTab.LIKED_SONGS -> {
                if (state.filteredLikedTracks.isEmpty()) {
                    item(key = "liked_empty") {
                        LibraryEmptyCard(
                            title = strings["library_empty_title"],
                            description = strings["library_empty_desc"],
                            actionLabel = strings["explore_music"],
                            onAction = onExploreMusic
                        )
                    }
                } else {
                    itemsIndexed(state.filteredLikedTracks, key = { idx, t -> "ll_${idx}_${t.id}" }) { idx, track ->
                        TrackRowItem(
                            index = idx + 1,
                            track = track,
                            isCurrentlyPlaying = playerState.currentTrack?.id == track.id,
                            isLiked = true,
                            isDownloaded = track.id in downloadedIds || track.isDownloaded,
                            onPlay = { playerViewModel.playTrack(track, state.filteredLikedTracks) },
                            onToggleLike = { playerViewModel.toggleTrackLike(track) },
                            onPlayNext = { playerViewModel.enqueueNext(track) },
                            onAddToQueue = { playerViewModel.enqueueLast(track) },
                            onAddToPlaylist = { playlistModal.openPicker(track) },
                            onDownload = { playerViewModel.downloadTrack(track) },
                            onOpenArtist = { onOpenArtist(track.artistId, track.artist) },
                            onOpenSongDetail = { onOpenSongDetail(track.id) }
                        )
                    }
                }
            }

            LibraryTab.SAVED_ALBUMS -> {
                if (state.filteredSavedAlbums.isEmpty()) {
                    item(key = "albums_empty") {
                        LibraryEmptyCard(
                            title = strings["library_empty_title"],
                            description = strings["library_empty_desc"],
                            actionLabel = strings["explore_music"],
                            onAction = onExploreMusic
                        )
                    }
                } else {
                    item(key = "albums_grid") {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            state.filteredSavedAlbums.forEach { album ->
                                MusicCard(
                                    title = album.title,
                                    subtitle = album.artist,
                                    artworkUrl = album.coverUrl,
                                    badgeText = album.releaseYear?.toString(),
                                    onClick = { onOpenAlbum(album.id) }
                                )
                            }
                        }
                    }
                }
            }

            LibraryTab.PLAYLISTS -> {
                if (state.filteredPlaylists.isEmpty()) {
                    item(key = "playlists_empty") {
                        LibraryEmptyCard(
                            title = strings["no_playlists"],
                            description = strings["library_empty_desc"],
                            actionLabel = strings["create_playlist"],
                            onAction = { viewModel.setCreatePlaylistDialogVisible(true) }
                        )
                    }
                } else {
                    item(key = "playlists_grid") {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            state.filteredPlaylists.forEach { playlist ->
                                MusicCard(
                                    title = playlist.title,
                                    subtitle = "${playlist.tracks.size} ${strings["songs"]}",
                                    artworkUrl = playlist.tracks.firstOrNull()?.coverUrl,
                                    onClick = { onOpenPlaylist(playlist.id) },
                                    onPlayClick = {
                                        if (playlist.tracks.isNotEmpty()) {
                                            playerViewModel.playQueue(playlist.tracks)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Renders a styled empty-state panel inside [LibraryScreen] with an action button.
 */
@Composable
private fun LibraryEmptyCard(
    title: String,
    description: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    val accent = LocalDynamicAccent.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = IvidsColorTokens.VinylSurface,
        border = BorderStroke(1.dp, IvidsColorTokens.HairlineBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.LibraryMusic,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(44.dp)
            )
            Text(title, style = MaterialTheme.typography.titleLarge, color = IvidsColorTokens.ChalkInk)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = IvidsColorTokens.SlateMuted)
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = IvidsColorTokens.OnAccentDark
                )
            ) {
                Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(actionLabel)
            }
        }
    }
}
