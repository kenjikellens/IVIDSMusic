package com.kenjigames.ividsmusic.ui.component.tile

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kenjigames.ividsmusic.domain.model.Album

/**
 * Concrete tile component for rendering [Album] models.
 * Specializes [BaseTile] for album displays with track count subtitle.
 *
 * @param album Domain album model
 * @param modifier Compose modifier
 * @param cardWidth Width of album card
 * @param onClick Click callback when album card is tapped
 */
@Composable
fun AlbumTile(
    album: Album,
    modifier: Modifier = Modifier,
    cardWidth: Dp = 140.dp,
    onClick: () -> Unit = {}
) {
    val subtitleText = if (album.trackCount > 0) "${album.artistName} • ${album.trackCount} tracks" else album.artistName
    BaseTile(
        coverUrl = album.coverUrl,
        title = album.title,
        subtitle = subtitleText,
        modifier = modifier,
        imageShape = RoundedCornerShape(10.dp),
        cardWidth = cardWidth,
        onClick = onClick
    )
}
