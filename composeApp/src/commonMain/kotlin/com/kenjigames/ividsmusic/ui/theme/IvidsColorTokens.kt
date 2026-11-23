package com.kenjigames.ividsmusic.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Defines IVIDS Lumina's pure neutral dark charcoal palette paired with selective signature cyan
 * accents for primary playback actions and active navigation indicators.
 */
object IvidsColorTokens {
    val AbyssRail: Color = Color(0xFF09090B)
    val ObsidianStage: Color = Color(0xFF0D0D10)
    val GlassSurface: Color = Color(0xFF17171B)
    val GlassHover: Color = Color(0xFF33333E)
    val RecessedTrough: Color = Color(0xFF111114)
    val HairlineSubtle: Color = Color(0x16FFFFFF)
    val HoverBorder: Color = Color(0x47FFFFFF)
    val HoverOverlay: Color = Color(0x26FFFFFF)

    val VinylSurface: Color = GlassSurface
    val GrooveElevated: Color = GlassHover
    val HairlineBorder: Color = Color(0x18FFFFFF)

    val ChalkInk: Color = Color(0xFFF5F5F7)
    val SlateMuted: Color = Color(0xFFA1A1AA)
    val SlateSubtle: Color = Color(0xFF71717A)
    val PrimaryCyan: Color = Color(0xFF4DD0E1)
    val OceanAccent: Color = Color(0xFF0288D1)
    val CyanHighlight: Color = Color(0xFF80DEEA)
    val DangerCoral: Color = Color(0xFFFF5252)
    val SuccessMint: Color = Color(0xFF22C55E)
    val WarningAmber: Color = Color(0xFFF59E0B)
    val OnAccentDark: Color = Color(0xFF061017)
}

/**
 * Immutable extended color palette propagated alongside MaterialTheme for IVIDS-specific
 * neutral tonal layers, recessed troughs, hairline dividers, and semantic status indicators.
 */
@Immutable
data class IvidsExtendedColors(
    val railBackground: Color = IvidsColorTokens.AbyssRail,
    val stageBackground: Color = IvidsColorTokens.ObsidianStage,
    val surface: Color = IvidsColorTokens.GlassSurface,
    val elevatedSurface: Color = IvidsColorTokens.GlassHover,
    val recessedTrough: Color = IvidsColorTokens.RecessedTrough,
    val hairlineSubtle: Color = IvidsColorTokens.HairlineSubtle,
    val hairlineBorder: Color = IvidsColorTokens.HairlineBorder,
    val textPrimary: Color = IvidsColorTokens.ChalkInk,
    val textSecondary: Color = IvidsColorTokens.SlateMuted,
    val textSubtle: Color = IvidsColorTokens.SlateSubtle,
    val primaryCyan: Color = IvidsColorTokens.PrimaryCyan,
    val oceanAccent: Color = IvidsColorTokens.OceanAccent,
    val cyanHighlight: Color = IvidsColorTokens.CyanHighlight,
    val danger: Color = IvidsColorTokens.DangerCoral,
    val success: Color = IvidsColorTokens.SuccessMint,
    val warning: Color = IvidsColorTokens.WarningAmber,
    val onAccent: Color = IvidsColorTokens.OnAccentDark
)

val LocalIvidsColors = staticCompositionLocalOf { IvidsExtendedColors() }
val LocalDynamicAccent = compositionLocalOf { IvidsColorTokens.PrimaryCyan }
val LocalUiScale = compositionLocalOf { 1.0f }
val LocalIsTvMode = compositionLocalOf { false }
