package com.kenjigames.ividsmusic.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Defines IVIDS Lumina's layered dark tonal palette, signature cyan and ocean-blue accents,
 * recessed control troughs, and whisper-thin hairline dividers for crisp HiDPI GPU rendering.
 */
object IvidsColorTokens {
    val AbyssRail: Color = Color(0xFF07080B)
    val ObsidianStage: Color = Color(0xFF0C0E13)
    val GlassSurface: Color = Color(0xFF141720)
    val GlassHover: Color = Color(0xFF1B202D)
    val RecessedTrough: Color = Color(0xFF0F1219)
    val HairlineSubtle: Color = Color(0x14FFFFFF)

    val VinylSurface: Color = GlassSurface
    val GrooveElevated: Color = GlassHover
    val HairlineBorder: Color = Color(0x1EFFFFFF)

    val ChalkInk: Color = Color(0xFFF4F7FA)
    val SlateMuted: Color = Color(0xFF94A3B8)
    val SlateSubtle: Color = Color(0xFF64748B)
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
 * tonal layers, recessed troughs, hairline dividers, and semantic status indicators.
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
