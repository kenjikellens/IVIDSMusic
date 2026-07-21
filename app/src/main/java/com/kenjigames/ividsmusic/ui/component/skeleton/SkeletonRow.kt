package com.kenjigames.ividsmusic.ui.component.skeleton

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kenjigames.ividsmusic.ui.theme.TextPrimary
import com.kenjigames.ividsmusic.ui.theme.Typography

/**
 * Reusable horizontal scrollable row of [SkeletonTile] elements for loading states.
 */
@Composable
fun SkeletonRow(
    modifier: Modifier = Modifier,
    title: String? = null,
    itemCount: Int = 5
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (title != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = Typography.titleLarge,
                    color = TextPrimary
                )
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(itemCount) {
                SkeletonTile()
            }
        }
    }
}
