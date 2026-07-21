package com.kenjigames.ividsmusic.ui.component.row

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kenjigames.ividsmusic.domain.model.Album
import com.kenjigames.ividsmusic.domain.model.Artist
import com.kenjigames.ividsmusic.domain.model.MusicItem
import com.kenjigames.ividsmusic.domain.model.Song
import com.kenjigames.ividsmusic.ui.component.tile.AlbumTile
import com.kenjigames.ividsmusic.ui.component.tile.ArtistTile
import com.kenjigames.ividsmusic.ui.component.tile.SongTile
import com.kenjigames.ividsmusic.ui.theme.PrimaryAccent
import com.kenjigames.ividsmusic.ui.theme.TextPrimary
import com.kenjigames.ividsmusic.ui.theme.Typography

/**
 * Reusable horizontal scrollable row container for rendering polymorphic lists of [MusicItem].
 * Uses bold modern typography for genre titles matching Spotify/Apple Music styling.
 *
 * @param title Section header title
 * @param items List of polymorphic MusicItem domain models
 * @param modifier Compose modifier
 * @param onSeeAllClick Action executed when "See All" is tapped
 * @param onItemClick Callback when a tile is tapped
 */
@Composable
fun HorizontalTileRow(
    title: String,
    items: List<MusicItem>,
    modifier: Modifier = Modifier,
    onSeeAllClick: (() -> Unit)? = null,
    onItemClick: (MusicItem) -> Unit = {}
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Section Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = TextStyle(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.4).sp,
                    color = TextPrimary
                )
            )
            if (onSeeAllClick != null) {
                TextButton(
                    onClick = onSeeAllClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "See all",
                        style = Typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = PrimaryAccent
                        )
                    )
                }
            }
        }

        // LazyRow Container
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items, key = { it.id }) { item ->
                when (item) {
                    is Song -> SongTile(song = item, onClick = { onItemClick(item) })
                    is Artist -> ArtistTile(artist = item, onClick = { onItemClick(item) })
                    is Album -> AlbumTile(album = item, onClick = { onItemClick(item) })
                }
            }
        }
    }
}
