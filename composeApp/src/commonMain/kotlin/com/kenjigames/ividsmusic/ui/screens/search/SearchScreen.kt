package com.kenjigames.ividsmusic.ui.screens.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.kenjigames.ividsmusic.ui.components.IvidsTonalPill
import com.kenjigames.ividsmusic.ui.components.LivingAlbumHero
import com.kenjigames.ividsmusic.ui.components.MusicCard
import com.kenjigames.ividsmusic.ui.components.ShimmerSkeletonRow
import com.kenjigames.ividsmusic.ui.components.ToastController
import com.kenjigames.ividsmusic.ui.components.TrackRowItem
import com.kenjigames.ividsmusic.ui.components.ividsTouchFeedback
import com.kenjigames.ividsmusic.ui.components.rememberTrackPlaylistModalState
import com.kenjigames.ividsmusic.ui.i18n.LocalStrings
import com.kenjigames.ividsmusic.ui.player.PlayerUiState
import com.kenjigames.ividsmusic.ui.player.PlayerViewModel
import com.kenjigames.ividsmusic.ui.theme.IvidsColorTokens
import com.kenjigames.ividsmusic.ui.theme.IvidsTextStyles
import com.kenjigames.ividsmusic.ui.theme.LocalDynamicAccent
import com.kenjigames.ividsmusic.ui.theme.LocalLayoutMetrics
import com.kenjigames.ividsmusic.ui.theme.ividsScreenPadding
import com.kenjigames.ividsmusic.ui.theme.tvFocusable

/** Visual tile descriptor for a browse genre card shown when the Search query is empty. */
private data class BrowseCategoryTile(val label: String, val query: String, val tint: Color)

/**
 * Renders the Search & Discovery screen featuring an adaptive 2-column mobile / wrapped desktop
 * browse genre grid, clear hover borders, segmented filters, release-year range, and results.
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
    val metrics = LocalLayoutMetrics.current
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
            contentPadding = ividsScreenPadding(top = 22.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
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
                                IconButton(
                                    onClick = { showYearPanel = !showYearPanel },
                                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CalendarMonth,
                                        contentDescription = strings["year_filter"],
                                        tint = if (state.yearFilterEnabled || showYearPanel) IvidsColorTokens.ChalkInk else IvidsColorTokens.SlateMuted
                                    )
                                }
                                if (state.query.isNotEmpty()) {
                                    IconButton(
                                        onClick = { viewModel.onQueryChanged("") },
                                        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                                    ) {
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
                                        activeTrackColor = IvidsColorTokens.ChalkInk,
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
                    @Composable
                    fun GenreCategoryCard(cat: BrowseCategoryTile, cardModifier: Modifier) {
                        val tileHoverSource = remember { MutableInteractionSource() }
                        val tileHovered by tileHoverSource.collectIsHoveredAsState()
                        Surface(
                            onClick = { viewModel.submitQuery(cat.query) },
                            interactionSource = tileHoverSource,
                            shape = RoundedCornerShape(18.dp),
                            color = IvidsColorTokens.GlassSurface,
                            modifier = cardModifier
                                .ividsTouchFeedback(interactionSource = tileHoverSource, pressedScale = 0.965f)
                                .pointerHoverIcon(PointerIcon.Hand)
                                .tvFocusable(shape = RoundedCornerShape(18.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                cat.tint.copy(alpha = if (tileHovered) 0.96f else 0.76f),
                                                IvidsColorTokens.GlassSurface
                                            )
                                        )
                                    )
                                    .padding(if (metrics.isCompactMobile) 14.dp else 18.dp),
                                contentAlignment = Alignment.BottomStart
                            ) {
                                Text(
                                    text = cat.label,
                                    style = if (metrics.isCompactMobile) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                                    color = IvidsColorTokens.ChalkInk
                                )
                            }
                        }
                    }

                    if (metrics.isCompactMobile) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            browseCategories.chunked(2).forEach { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    pair.forEach { cat ->
                                        GenreCategoryCard(
                                            cat = cat,
                                            cardModifier = Modifier.weight(1f).height(88.dp)
                                        )
                                    }
                                    if (pair.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    } else {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            browseCategories.forEach { cat ->
                                GenreCategoryCard(
                                    cat = cat,
                                    cardModifier = Modifier.width(184.dp).height(98.dp)
                                )
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
                                        Text(strings["see_all"], color = IvidsColorTokens.SlateMuted)
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
                                        Text(strings["see_all"], color = IvidsColorTokens.SlateMuted)
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
                                    Text(strings["see_all"], color = IvidsColorTokens.SlateMuted)
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
                            IvidsTonalPill(
                                onClick = { if (!state.isLoadingMore) viewModel.loadMore() },
                                baseColor = IvidsColorTokens.GlassSurface,
                                hoverColor = IvidsColorTokens.GlassHover,
                                baseBorderColor = IvidsColorTokens.HairlineBorder,
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = if (state.isLoadingMore) strings["loading"] else strings["load_more"],
                                    style = MaterialTheme.typography.labelLarge,
                                    color = IvidsColorTokens.ChalkInk
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
