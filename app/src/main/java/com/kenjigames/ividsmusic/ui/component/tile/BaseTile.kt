package com.kenjigames.ividsmusic.ui.component.tile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kenjigames.ividsmusic.ui.theme.DarkSurface
import com.kenjigames.ividsmusic.ui.theme.GlassBorder
import com.kenjigames.ividsmusic.ui.theme.TextPrimary
import com.kenjigames.ividsmusic.ui.theme.TextSecondary
import com.kenjigames.ividsmusic.ui.theme.Typography

/**
 * Base abstract composable representing the shared card tile architecture.
 * Subclasses (SongTile, ArtistTile, AlbumTile) customize the image shape and overlay actions.
 *
 * @param coverUrl Image URL for the tile cover
 * @param title Primary title string
 * @param subtitle Secondary subtitle string
 * @param modifier Compose modifier
 * @param imageShape Shape token for clipping artwork (square or circle)
 * @param cardWidth Dp width of the tile card
 * @param onClick Click event handler
 * @param onLongClick Long click event handler
 * @param imageOverlay Extra composable content rendered over the artwork
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BaseTile(
    coverUrl: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    imageShape: Shape = RoundedCornerShape(12.dp),
    cardWidth: Dp = 140.dp,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    imageOverlay: @Composable (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .width(cardWidth)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.5.dp, GlassBorder)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Image section with custom shape & overlay
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(imageShape)
            ) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                imageOverlay?.invoke()
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Title
            Text(
                text = title,
                style = Typography.titleMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Subtitle
            Text(
                text = subtitle,
                style = Typography.bodyMedium,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
