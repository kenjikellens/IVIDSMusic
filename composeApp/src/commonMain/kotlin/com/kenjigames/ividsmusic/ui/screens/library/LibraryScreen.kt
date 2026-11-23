package com.kenjigames.ividsmusic.ui.screens.library

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kenjigames.ividsmusic.domain.port.LibraryRepository
import com.kenjigames.ividsmusic.ui.components.CreatePlaylistDialog
import com.kenjigames.ividsmusic.ui.components.FramedScreenContainer
import com.kenjigames.ividsmusic.ui.components.IvidsPrimaryButton
import com.kenjigames.ividsmusic.ui.components.IvidsSegmentedBar
import com.kenjigames.ividsmusic.ui.components.IvidsTonalPill
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
import com.kenjigames.ividsmusic.ui.theme.ividsScreenPadding
import com.kenjigames.ividsmusic.ui.theme.tvFocusable

/**
 * Renders the user's local Library inside a framed viewport (max 1360dp) with adaptive mobile
 * paddings, recessed tab selectors, real-time filtering, and disabled-aware batch actions.
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

    FramedScreenContainer(
        maxWidth = 1360.dp,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = ividsScreenPadding(top = 22.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
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
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = viewModel::onSearchQueryChanged,
                            placeholder = { Text(strings["search_library_placeholder"], color = IvidsColorTokens.SlateMuted) },
                            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = IvidsColorTokens.SlateMuted) },
                            trailingIcon = {
                                if (state.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                        Icon(Icons.Rounded.Close, strings["close"], tint = IvidsColorTokens.SlateMuted)
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = IvidsColorTokens.GlassSurface,
                                unfocusedContainerColor = IvidsColorTokens.RecessedTrough,
                                focusedBorderColor = IvidsColorTokens.HairlineBorder,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = IvidsColorTokens.ChalkInk,
                                unfocusedTextColor = IvidsColorTokens.ChalkInk
                            ),
                            modifier = Modifier.weight(1f).tvFocusable(shape = RoundedCornerShape(16.dp))
                        )
                        IvidsTonalPill(
                            onClick = {
                                val tracks = state.filteredLikedTracks
                                if (tracks.isNotEmpty()) playerViewModel.playQueue(tracks, shuffled = true)
                            },
                            enabled = state.filteredLikedTracks.isNotEmpty(),
                            baseColor = IvidsColorTokens.GlassSurface,
                            hoverColor = IvidsColorTokens.GlassHover,
                            baseBorderColor = IvidsColorTokens.HairlineSubtle,
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)
                        ) {
                            Icon(Icons.Rounded.Shuffle, null, tint = IvidsColorTokens.SlateMuted, modifier = Modifier.size(18.dp))
                            Text(
                                text = strings["shuffle"],
                                style = MaterialTheme.typography.labelLarge,
                                color = IvidsColorTokens.ChalkInk,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    val tabOptions = listOf(
                        LibraryTab.LIKED_SONGS to "${strings["liked_songs"]} (${state.likedTracks.size})",
                        LibraryTab.SAVED_ALBUMS to "${strings["saved_albums"]} (${state.savedAlbums.size})",
                        LibraryTab.PLAYLISTS to "${strings["playlists"]} (${state.playlists.size})"
                    )
                    IvidsSegmentedBar(
                        items = tabOptions.map { it.first },
                        selected = state.selectedTab,
                        labelProvider = { tab -> tabOptions.first { it.first == tab }.second },
                        onSelect = viewModel::selectTab
                    )
                }
            }

            if (state.recentlyListened.isNotEmpty()) {
                item(key = "library_recent") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
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
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
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
}

/**
 * Renders a borderless tonal empty-state panel inside [LibraryScreen] with an action pill.
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
        shape = RoundedCornerShape(20.dp),
        color = IvidsColorTokens.GlassSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.LibraryMusic,
                contentDescription = null,
                tint = IvidsColorTokens.ChalkInk,
                modifier = Modifier.size(46.dp)
            )
            Text(title, style = MaterialTheme.typography.titleLarge, color = IvidsColorTokens.ChalkInk)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = IvidsColorTokens.SlateMuted)
            Spacer(modifier = Modifier.height(4.dp))
            IvidsPrimaryButton(onClick = onAction) {
                Icon(Icons.Rounded.Add, null, tint = IvidsColorTokens.ObsidianStage, modifier = Modifier.size(18.dp))
                Text(actionLabel, color = IvidsColorTokens.ObsidianStage)
            }
        }
    }
}
