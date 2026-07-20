package com.kenjigames.ividsmusic.ui.component.tile

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kenjigames.ividsmusic.domain.model.Artist

/**
 * Concrete tile component for rendering [Artist] models.
 * Specializes [BaseTile] by clipping artwork to a circular avatar shape.
 *
 * @param artist Domain artist model
 * @param modifier Compose modifier
 * @param cardWidth Width of artist card
 * @param onClick Click callback when artist card is tapped
 */
@Composable
fun ArtistTile(
    artist: Artist,
    modifier: Modifier = Modifier,
    cardWidth: Dp = 140.dp,
    onClick: () -> Unit = {}
) {
    BaseTile(
        coverUrl = artist.imageUrl,
        title = artist.name,
        subtitle = "Artist",
        modifier = modifier,
        imageShape = CircleShape,
        cardWidth = cardWidth,
        onClick = onClick
    )
}
