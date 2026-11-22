package com.kenjigames.ividsmusic.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Defines IVIDS Music's signature dark obsidian, cyan, and ocean-blue color tokens
 * inspired by the original GUI stylesheet and refined for native GPU rendering.
 */
object IvidsColorTokens {
    val ObsidianStage: Color = Color(0xFF0A0B0E)
    val VinylSurface: Color = Color(0xFF15161B)
    val GrooveElevated: Color = Color(0xFF1E2028)
    val HairlineBorder: Color = Color(0xFF2A2E39)
    val ChalkInk: Color = Color(0xFFF0F4F8)
    val SlateMuted: Color = Color(0xFF94A3B8)
    val SlateSubtle: Color = Color(0xFF64748B)
    val PrimaryCyan: Color = Color(0xFF4DD0E1)
    val OceanAccent: Color = Color(0xFF0288D1)
    val CyanHighlight: Color = Color(0xFF80DEEA)
    val DangerCoral: Color = Color(0xFFFF5252)
    val SuccessMint: Color = Color(0xFF22C55E)
    val WarningAmber: Color = Color(0xFFF59E0B)
    val OnAccentDark: Color = Color(0xFF071016)
}

/**
 * Immutable extended color palette propagated alongside MaterialTheme for IVIDS-specific
 * elevated surfaces, hairline borders, and semantic status indicators.
 */
@Immutable
data class IvidsExtendedColors(
    val stageBackground: Color = IvidsColorTokens.ObsidianStage,
    val surface: Color = IvidsColorTokens.VinylSurface,
    val elevatedSurface: Color = IvidsColorTokens.GrooveElevated,
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
