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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kenjigames.ividsmusic.domain.port.LibraryRepository
import com.kenjigames.ividsmusic.ui.components.CreatePlaylistDialog
import com.kenjigames.ividsmusic.ui.components.FramedScreenContainer
import com.kenjigames.ividsmusic.ui.components.IvidsIconButton
import com.kenjigames.ividsmusic.ui.components.IvidsPrimaryButton
import com.kenjigames.ividsmusic.ui.components.IvidsSegmentedBar
import com.kenjigames.ividsmusic.ui.components.IvidsTonalPill
import com.kenjigames.ividsmusic.ui.components.MusicCard
import com.kenjigames.ividsmusic.ui.components.ToastController
import com.kenjigames.ividsmusic.ui.components.TrackRowItem
import com.kenjigames.ividsmusic.ui.components.TrackSortFilterRow
import com.kenjigames.ividsmusic.ui.components.TrackTableHeader
import com.kenjigames.ividsmusic.ui.components.ividsSearchFieldColors
import com.kenjigames.ividsmusic.ui.components.rememberTrackPlaylistModalState
import com.kenjigames.ividsmusic.ui.i18n.LocalStrings
import com.kenjigames.ividsmusic.ui.player.PlayerUiState
import com.kenjigames.ividsmusic.ui.player.PlayerViewModel
import com.kenjigames.ividsmusic.ui.theme.IvidsColorTokens
import com.kenjigames.ividsmusic.ui.theme.LocalLayoutMetrics
import com.kenjigames.ividsmusic.ui.theme.ividsScreenPadding
import com.kenjigames.ividsmusic.ui.theme.tvFocusable

/**
 * Renders the user's local Library inside a framed viewport (max 1360dp) with underline
 * collection tabs, compact sort and offline filters, and clean empty states without clutter.
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
    val metrics = LocalLayoutMetrics.current
    val playlistModal = rememberTrackPlaylistModalState(state.playlists, libraryRepository, toastController)
    val filteredLiked = remember(
        state.likedTracks,
        state.searchQuery,
        state.sortOption,
        state.offlineOnly,
        downloadedIds
    ) {
        state.filteredLikedTracks(downloadedIds)
    }
    val hasAnyContent = state.likedTracks.isNotEmpty() ||
        state.savedAlbums.isNotEmpty() ||
        state.playlists.isNotEmpty() ||
        state.searchQuery.isNotEmpty() ||
        state.offlineOnly

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
            contentPadding = ividsScreenPadding(top = if (metrics.isCompactMobile) 12.dp else 22.dp),
            verticalArrangement = Arrangement.spacedBy(if (metrics.isCompactMobile) 16.dp else 20.dp)
        ) {
            if (!metrics.isCompactMobile) {
                item(key = "library_desktop_header") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings["library"],
                            style = MaterialTheme.typography.headlineMedium,
                            color = IvidsColorTokens.ChalkInk
                        )
                        IvidsTonalPill(
                            onClick = { viewModel.setCreatePlaylistDialogVisible(true) },
                            baseColor = IvidsColorTokens.GlassSurface,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 9.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                tint = IvidsColorTokens.ChalkInk,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = strings["create_playlist"],
                                style = MaterialTheme.typography.labelLarge,
                                color = IvidsColorTokens.ChalkInk,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            item(key = "library_controls") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IvidsSegmentedBar(
                            items = LibraryTab.entries,
                            selected = state.selectedTab,
                            labelProvider = { tab ->
                                val (label, count) = when (tab) {
                                    LibraryTab.LIKED_SONGS -> strings["liked_songs"] to state.likedTracks.size
                                    LibraryTab.SAVED_ALBUMS -> strings["albums"] to state.savedAlbums.size
                                    LibraryTab.PLAYLISTS -> strings["playlists"] to state.playlists.size
                                }
                                if (count > 0) "$label ($count)" else label
                            },
                            onSelect = viewModel::selectTab,
                            modifier = Modifier.weight(1f)
                        )
                        if (metrics.isCompactMobile) {
                            IvidsIconButton(
                                onClick = { viewModel.setCreatePlaylistDialogVisible(true) },
                                size = 36.dp
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = strings["create_playlist"],
                                    tint = IvidsColorTokens.ChalkInk,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    if (hasAnyContent) {
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
                            shape = RoundedCornerShape(14.dp),
                            colors = ividsSearchFieldColors(),
                            modifier = Modifier.fillMaxWidth().tvFocusable(shape = RoundedCornerShape(14.dp))
                        )
                    }

                    if (state.selectedTab == LibraryTab.LIKED_SONGS &&
                        (state.likedTracks.isNotEmpty() || state.offlineOnly)
                    ) {
                        TrackSortFilterRow(
                            selectedSort = state.sortOption,
                            offlineOnly = state.offlineOnly,
                            onSelectSort = viewModel::setSortOption,
                            onToggleOfflineOnly = viewModel::toggleOfflineOnly,
                            strings = strings,
                            trailingContent = if (filteredLiked.isNotEmpty()) {
                                {
                                    IvidsIconButton(
                                        onClick = { playerViewModel.playQueue(filteredLiked) },
                                        size = 36.dp,
                                        hoverStyle = com.kenjigames.ividsmusic.ui.components.IvidsHoverStyle.PRIMARY_ACCENT
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.PlayArrow,
                                            contentDescription = strings["play_all"],
                                            tint = IvidsColorTokens.PrimaryCyan,
                                            modifier = Modifier.size(21.dp)
                                        )
                                    }
                                    IvidsIconButton(
                                        onClick = { playerViewModel.playQueue(filteredLiked, shuffled = true) },
                                        size = 36.dp
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Shuffle,
                                            contentDescription = strings["shuffle"],
                                            tint = IvidsColorTokens.SlateMuted,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                }
                            } else {
                                null
                            }
                        )
                    }
                }
            }

            when (state.selectedTab) {
                LibraryTab.LIKED_SONGS -> {
                    if (filteredLiked.isEmpty()) {
                        item(key = "liked_empty") {
                            LibraryEmptyCard(
                                title = strings["library_empty_title"],
                                description = strings["library_empty_desc"],
                                actionLabel = strings["explore_music"],
                                onAction = onExploreMusic
                            )
                        }
                    } else {
                        if (!metrics.isCompactMobile) {
                            item(key = "liked_table_header") {
                                TrackTableHeader(strings = strings, trailingWidth = 84.dp)
                            }
                        }
                        itemsIndexed(filteredLiked, key = { idx, t -> "ll_${idx}_${t.id}" }) { idx, track ->
                            TrackRowItem(
                                index = idx + 1,
                                track = track,
                                isCurrentlyPlaying = playerState.currentTrack?.id == track.id,
                                isLiked = true,
                                isDownloaded = track.id in downloadedIds || track.isDownloaded,
                                onPlay = { playerViewModel.playTrack(track, filteredLiked) },
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
                                    val subtitle = if (playlist.tracks.size == 1) {
                                        strings["track_singular"]
                                    } else {
                                        strings.format("tracks_count", "count" to playlist.tracks.size.toString())
                                    }
                                    MusicCard(
                                        title = playlist.title,
                                        subtitle = subtitle,
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
