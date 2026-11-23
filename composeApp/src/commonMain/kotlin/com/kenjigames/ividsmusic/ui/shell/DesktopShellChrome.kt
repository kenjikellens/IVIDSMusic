package com.kenjigames.ividsmusic.ui.shell

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import com.kenjigames.ividsmusic.resources.Res
import com.kenjigames.ividsmusic.resources.ic_ivids_brand
import com.kenjigames.ividsmusic.ui.components.IvidsHoverStyle
import com.kenjigames.ividsmusic.ui.components.IvidsIconButton
import com.kenjigames.ividsmusic.ui.components.ividsHoverZoom
import com.kenjigames.ividsmusic.ui.components.ividsPressRipple
import com.kenjigames.ividsmusic.ui.i18n.LocalStrings
import com.kenjigames.ividsmusic.ui.navigation.AppRoute
import com.kenjigames.ividsmusic.ui.theme.IvidsColorTokens
import com.kenjigames.ividsmusic.ui.theme.IvidsTextStyles
import com.kenjigames.ividsmusic.ui.theme.tvFocusable
import org.jetbrains.compose.resources.painterResource

/**
 * Pure neutral dark borderless sidebar navigation rail rendered on Desktop (`>= 600dp`) and Android TV.
 */
@Composable
internal fun SidebarNavigationRail(
    currentRoute: AppRoute,
    expanded: Boolean,
    isTv: Boolean,
    onSelectRoute: (AppRoute) -> Unit
) {
    val strings = LocalStrings.current
    val primaryEntries = remember {
        listOf(
            NavEntry(AppRoute.Home, "home", Icons.Rounded.Home) { it is AppRoute.Home },
            NavEntry(AppRoute.Recommended, "nav_foryou", Icons.Rounded.AutoAwesome) { it is AppRoute.Recommended },
            NavEntry(AppRoute.Search(), "search", Icons.Rounded.Search) { it is AppRoute.Search },
            NavEntry(AppRoute.Library, "library", Icons.Rounded.LibraryMusic) { it is AppRoute.Library },
            NavEntry(AppRoute.Profile, "you", Icons.Rounded.Person) { it is AppRoute.Profile }
        )
    }
    val utilityEntries = remember {
        listOf(
            NavEntry(AppRoute.Downloader, "nav_downloader", Icons.Rounded.Download) { it is AppRoute.Downloader },
            NavEntry(AppRoute.Settings, "settings", Icons.Rounded.Settings) { it is AppRoute.Settings || it is AppRoute.SettingsDetail }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(if (expanded) 236.dp else 78.dp)
            .clipToBounds()
            .background(IvidsColorTokens.AbyssRail)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Image(
                    painter = painterResource(Res.drawable.ic_ivids_brand),
                    contentDescription = "IVIDS Music",
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                )
                if (expanded) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "IVIDS",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = IvidsColorTokens.PrimaryCyan
                        )
                        Text(
                            text = if (isTv) "TV" else "MUSIC",
                            style = MaterialTheme.typography.titleSmall,
                            color = IvidsColorTokens.ChalkInk
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            primaryEntries.forEach { entry ->
                SidebarNavButton(
                    label = strings[entry.labelKey],
                    icon = entry.icon,
                    selected = entry.matcher(currentRoute),
                    expanded = expanded,
                    onClick = { onSelectRoute(entry.route) }
                )
            }

            HorizontalDivider(
                color = IvidsColorTokens.HairlineSubtle,
                modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)
            )

            utilityEntries.forEach { entry ->
                SidebarNavButton(
                    label = strings[entry.labelKey],
                    icon = entry.icon,
                    selected = entry.matcher(currentRoute),
                    expanded = expanded,
                    onClick = { onSelectRoute(entry.route) }
                )
            }
        }
    }
}

/**
 * Single borderless navigation pill inside [SidebarNavigationRail] giving only [selected] items the
 * cyan accent while hovered inactive items animate smoothly to neutral tonal fill.
 */
@Composable
private fun SidebarNavButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    expanded: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val bgColor by animateColorAsState(
        targetValue = if (selected) IvidsColorTokens.PrimaryCyan.copy(alpha = 0.16f) else Color.Transparent,
        animationSpec = tween(150, easing = FastOutSlowInEasing),
        label = "NavButtonBg"
    )
    val iconColor by animateColorAsState(
        targetValue = when {
            selected -> IvidsColorTokens.PrimaryCyan
            isHovered -> IvidsColorTokens.ChalkInk
            else -> IvidsColorTokens.SlateMuted
        },
        animationSpec = tween(150, easing = FastOutSlowInEasing),
        label = "NavButtonIcon"
    )
    val textColor by animateColorAsState(
        targetValue = when {
            selected || isHovered -> IvidsColorTokens.ChalkInk
            else -> IvidsColorTokens.SlateMuted
        },
        animationSpec = tween(150, easing = FastOutSlowInEasing),
        label = "NavButtonText"
    )

    val navHoverStyle = if (selected) IvidsHoverStyle.PRIMARY_ACCENT else IvidsHoverStyle.NEUTRAL

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .pointerHoverIcon(PointerIcon.Hand)
            .tvFocusable(shape = RoundedCornerShape(10.dp))
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = ividsPressRipple(navHoverStyle),
                onClick = onClick
            ),
        shape = RoundedCornerShape(10.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .ividsHoverZoom(
                    hovered = isHovered,
                    shape = RoundedCornerShape(10.dp),
                    style = navHoverStyle
                )
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (expanded) Arrangement.Start else Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(21.dp)
            )
            if (expanded) {
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                    ),
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Frameless Desktop & TV header anchoring back navigation on the far left, keeping the 440dp
 * search pill centered, and placing the profile shortcut on the far right.
 */
@Composable
internal fun DesktopTopBar(
    canNavigateBack: Boolean,
    searchFocusTrigger: Int,
    onBack: () -> Unit,
    onSearchSubmit: (String) -> Unit,
    onOpenProfile: () -> Unit
) {
    val strings = LocalStrings.current
    var query by remember { mutableStateOf("") }
    var searchFocused by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(searchFocusTrigger) {
        if (searchFocusTrigger > 0) {
            runCatching { searchFocusRequester.requestFocus() }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .padding(horizontal = 24.dp)
    ) {
        if (canNavigateBack) {
            IvidsIconButton(
                onClick = onBack,
                size = 38.dp,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .background(IvidsColorTokens.RecessedTrough, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = strings["back"],
                    tint = IvidsColorTokens.ChalkInk,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 52.dp),
            contentAlignment = Alignment.Center
        ) {
            val pillColor by animateColorAsState(
                targetValue = if (searchFocused) IvidsColorTokens.GlassSurface else IvidsColorTokens.RecessedTrough,
                animationSpec = tween(160),
                label = "SearchPillColor"
            )

            Surface(
                shape = CircleShape,
                color = pillColor,
                modifier = Modifier
                    .widthIn(min = 240.dp, max = 440.dp)
                    .fillMaxWidth()
                    .height(42.dp)
                    .tvFocusable(shape = CircleShape)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = if (searchFocused) IvidsColorTokens.ChalkInk else IvidsColorTokens.SlateMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (query.isEmpty()) {
                            Text(
                                text = strings["search_placeholder"],
                                style = MaterialTheme.typography.bodyMedium,
                                color = IvidsColorTokens.SlateMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = IvidsColorTokens.ChalkInk),
                            cursorBrush = SolidColor(IvidsColorTokens.ChalkInk),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                if (query.isNotBlank()) onSearchSubmit(query.trim())
                            }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(searchFocusRequester)
                                .onFocusChanged { searchFocused = it.isFocused }
                        )
                    }
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = { query = "" },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = strings["close"],
                                tint = IvidsColorTokens.SlateMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0x12FFFFFF)
                        ) {
                            Text(
                                text = "Ctrl+K",
                                style = IvidsTextStyles.tabularBadge(),
                                color = IvidsColorTokens.SlateMuted,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        val profileHoverSource = remember { MutableInteractionSource() }
        val profileHovered by profileHoverSource.collectIsHoveredAsState()
        Surface(
            onClick = onOpenProfile,
            shape = CircleShape,
            color = IvidsColorTokens.GlassSurface,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(40.dp)
                .pointerHoverIcon(PointerIcon.Hand)
                .hoverable(profileHoverSource)
                .tvFocusable(shape = CircleShape)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .ividsHoverZoom(hovered = profileHovered, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = strings["you"],
                    tint = IvidsColorTokens.ChalkInk,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
