package com.kenjigames.ividsmusic.ui.screens.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.kenjigames.ividsmusic.domain.model.Playlist
import com.kenjigames.ividsmusic.domain.model.SearchFilterType
import com.kenjigames.ividsmusic.domain.port.LibraryRepository
import com.kenjigames.ividsmusic.ui.components.FramedScreenContainer
import com.kenjigames.ividsmusic.ui.components.IvidsSegmentedBar
import com.kenjigames.ividsmusic.ui.components.IvidsSwitch
import com.kenjigames.ividsmusic.ui.components.LivingAlbumHero
import com.kenjigames.ividsmusic.ui.components.MusicCard
import com.kenjigames.ividsmusic.ui.components.ShimmerSkeletonRow
import com.kenjigames.ividsmusic.ui.components.ToastController
import com.kenjigames.ividsmusic.ui.components.TrackRowItem
import com.kenjigames.ividsmusic.ui.components.rememberTrackPlaylistModalState
import com.kenjigames.ividsmusic.ui.i18n.LocalStrings
import com.kenjigames.ividsmusic.ui.player.PlayerUiState
import com.kenjigames.ividsmusic.ui.player.PlayerViewModel
import com.kenjigames.ividsmusic.ui.theme.IvidsColorTokens
import com.kenjigames.ividsmusic.ui.theme.IvidsTextStyles
import com.kenjigames.ividsmusic.ui.theme.LocalDynamicAccent
import com.kenjigames.ividsmusic.ui.theme.tvFocusable

/** Visual tile descriptor for a browse genre card shown when the Search query is empty. */
private data class BrowseCategoryTile(val label: String, val query: String, val tint: Color)

/**
 * Renders the Search & Discovery screen featuring instant browse genre tiles, recessed category
 * segmented controls (All/Songs/Artists/Albums), release-year filter, and paginated results.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    initialQuery: String,
    viewModel: SearchViewModel,
    playerState: PlayerUiState,
    playerViewModel: PlayerViewModel,
    playlists: List<Playlist>,
    likedIds: Set<String>,
    downloadedIds: Set<String>,
    libraryRepository: LibraryRepository,
    toastController: ToastController,
    onOpenArtist: (String, String) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenSongDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val strings = LocalStrings.current
    val accent = LocalDynamicAccent.current
    var showYearPanel by remember { mutableStateOf(false) }
    val playlistModal = rememberTrackPlaylistModalState(playlists, libraryRepository, toastController)

    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank() && initialQuery != state.query) {
            viewModel.submitQuery(initialQuery)
        }
    }

    val browseCategories = remember {
        listOf(
            BrowseCategoryTile("Pop", "Pop", Color(0xFF0288D1)),
            BrowseCategoryTile("Hip-Hop", "Hip Hop", Color(0xFF7C3AED)),
            BrowseCategoryTile("Rock", "Rock", Color(0xFFDC2626)),
            BrowseCategoryTile("Dance", "Dance", Color(0xFF0891B2)),
            BrowseCategoryTile("Jazz", "Jazz", Color(0xFFD97706)),
            BrowseCategoryTile("Classical", "Classical", Color(0xFF059669)),
            BrowseCategoryTile("Chill", "Chill", Color(0xFF2563EB)),
            BrowseCategoryTile("Workout", "Workout", Color(0xFFDB2777))
        )
    }

    FramedScreenContainer(
        maxWidth = 1360.dp,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            item(key = "search_bar") {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = viewModel::onQueryChanged,
                        placeholder = { Text(strings["search_placeholder"], color = IvidsColorTokens.SlateMuted) },
                        leadingIcon = { Icon(Icons.Rounded.Search, null, tint = IvidsColorTokens.SlateMuted) },
                        trailingIcon = {
                            Row {
                                IconButton(onClick = { showYearPanel = !showYearPanel }) {
                                    Icon(
                                        imageVector = Icons.Rounded.CalendarMonth,
                                        contentDescription = strings["year_filter"],
                                        tint = if (state.yearFilterEnabled) accent else IvidsColorTokens.SlateMuted
                                    )
                                }
                                if (state.query.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onQueryChanged("") }) {
                                        Icon(Icons.Rounded.Close, strings["close"], tint = IvidsColorTokens.SlateMuted)
                                    }
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { viewModel.submitQuery(state.query) }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = IvidsColorTokens.GlassSurface,
                            unfocusedContainerColor = IvidsColorTokens.RecessedTrough,
                            focusedBorderColor = IvidsColorTokens.HairlineBorder,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = IvidsColorTokens.ChalkInk,
                            unfocusedTextColor = IvidsColorTokens.ChalkInk
                        ),
                        modifier = Modifier.fillMaxWidth().tvFocusable(shape = RoundedCornerShape(16.dp))
                    )

                    val filterOptions = listOf(
                        SearchFilterType.ALL to strings["filter_all"],
                        SearchFilterType.SONG to strings["songs"],
                        SearchFilterType.ARTIST to strings["artists"],
                        SearchFilterType.ALBUM to strings["albums"]
                    )
                    IvidsSegmentedBar(
                        items = filterOptions.map { it.first },
                        selected = state.filterType,
                        labelProvider = { type -> filterOptions.first { it.first == type }.second },
                        onSelect = viewModel::setFilterType
                    )

                    AnimatedVisibility(visible = showYearPanel) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = IvidsColorTokens.GlassSurface
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${strings["year_filter"]}: ${
                                            if (state.yearFilterEnabled) {
                                                "${state.yearRange.start.toInt()} – ${state.yearRange.endInclusive.toInt()}"
                                            } else {
                                                strings["any_year"]
                                            }
                                        }",
                                        style = IvidsTextStyles.tabularBadge(),
                                        color = IvidsColorTokens.ChalkInk
                                    )
                                    IvidsSwitch(
                                        checked = state.yearFilterEnabled,
                                        onCheckedChange = viewModel::setYearFilterEnabled
                                    )
                                }
                                RangeSlider(
                                    value = state.yearRange,
                                    onValueChange = viewModel::setYearRange,
                                    valueRange = 1950f..2026f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = IvidsColorTokens.ChalkInk,
                                        activeTrackColor = accent,
                                        inactiveTrackColor = IvidsColorTokens.RecessedTrough
                                    )
                                )
                            }
                        }
                    }
                }
            }

            if (state.query.isBlank()) {
                item(key = "browse_hero") {
                    LivingAlbumHero(
                        eyebrow = strings["tagline"],
                        title = strings["start_experience_title"],
                        subtitle = strings["start_experience_desc"],
                        artworkUrl = null,
                        primaryActionLabel = strings["explore_trends"],
                        onPrimaryAction = { viewModel.submitQuery("Top Hits") }
                    )
                }
                item(key = "browse_categories") {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        browseCategories.forEach { cat ->
                            Surface(
                                modifier = Modifier
                                    .width(184.dp)
                                    .height(98.dp)
                                    .tvFocusable(shape = RoundedCornerShape(18.dp))
                                    .clickable { viewModel.submitQuery(cat.query) },
                                shape = RoundedCornerShape(18.dp),
                                color = IvidsColorTokens.GlassSurface
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(
                                                listOf(cat.tint.copy(alpha = 0.78f), IvidsColorTokens.GlassSurface)
                                            )
                                        )
                                        .padding(18.dp),
                                    contentAlignment = Alignment.BottomStart
                                ) {
                                    Text(
                                        text = cat.label,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = IvidsColorTokens.ChalkInk
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (state.isLoading) {
                item(key = "search_loading") { ShimmerSkeletonRow() }
            } else {
                if (state.errorMessage != null) {
                    item(key = "search_error") {
                        Text(
                            text = state.errorMessage.orEmpty(),
                            color = IvidsColorTokens.DangerCoral,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                if (state.artists.isNotEmpty()) {
                    item(key = "artists_section") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(strings["artists"], style = MaterialTheme.typography.headlineSmall, color = IvidsColorTokens.ChalkInk)
                                if (state.filterType == SearchFilterType.ALL) {
                                    TextButton(onClick = { viewModel.setFilterType(SearchFilterType.ARTIST) }) {
                                        Text(strings["see_all"], color = accent)
                                    }
                                }
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                items(state.artists, key = { "sa_${it.id}" }) { artist ->
                                    MusicCard(
                                        title = artist.name,
                                        subtitle = strings["artists"],
                                        artworkUrl = artist.coverUrl,
                                        isCircular = true,
                                        onClick = { onOpenArtist(artist.id, artist.name) }
                                    )
                                }
                            }
                        }
                    }
                }

                if (state.albums.isNotEmpty()) {
                    item(key = "albums_section") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(strings["albums"], style = MaterialTheme.typography.headlineSmall, color = IvidsColorTokens.ChalkInk)
                                if (state.filterType == SearchFilterType.ALL) {
                                    TextButton(onClick = { viewModel.setFilterType(SearchFilterType.ALBUM) }) {
                                        Text(strings["see_all"], color = accent)
                                    }
                                }
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                items(state.albums, key = { "sal_${it.id}" }) { album ->
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

                if (state.songs.isNotEmpty()) {
                    item(key = "songs_header") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(strings["songs"], style = MaterialTheme.typography.headlineSmall, color = IvidsColorTokens.ChalkInk)
                            if (state.filterType == SearchFilterType.ALL) {
                                TextButton(onClick = { viewModel.setFilterType(SearchFilterType.SONG) }) {
                                    Text(strings["see_all"], color = accent)
                                }
                            }
                        }
                    }
                    items(state.songs, key = { "ss_${it.id}" }) { track ->
                        TrackRowItem(
                            track = track,
                            isCurrentlyPlaying = playerState.currentTrack?.id == track.id,
                            isLiked = track.id in likedIds || track.isLiked,
                            isDownloaded = track.id in downloadedIds || track.isDownloaded,
                            onPlay = { playerViewModel.playTrack(track, state.songs) },
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

                if (state.canLoadMore) {
                    item(key = "load_more") {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Surface(
                                onClick = { if (!state.isLoadingMore) viewModel.loadMore() },
                                shape = CircleShape,
                                color = IvidsColorTokens.GlassSurface,
                                border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
                                modifier = Modifier.tvFocusable(shape = CircleShape)
                            ) {
                                Text(
                                    text = if (state.isLoadingMore) strings["loading"] else strings["load_more"],
                                    style = MaterialTheme.typography.labelLarge,
                                    color = accent,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
