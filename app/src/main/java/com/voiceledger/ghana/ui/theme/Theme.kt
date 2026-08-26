package com.voiceledger.ghana.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/*
 * "Japanese + Ghanaian warmth": a calm, spacious canvas (cream / warm white, soft ink text,
 * generous rounding) with warm Ghanaian accents (a grounded forest green and a kente gold).
 * Restrained on colour, gentle on shape — welcoming rather than corporate, and legible in
 * bright market sunlight.
 */

private val ForestGreen = Color(0xFF386641)   // primary
private val SoftGreenContainer = Color(0xFFD3E7D6)
private val KenteGold = Color(0xFFCB8A14)      // secondary / accent
private val GoldContainer = Color(0xFFF6E2B8)
private val Cream = Color(0xFFFBF6EC)          // background
private val WarmWhite = Color(0xFFFFFDF8)      // surface
private val WarmSand = Color(0xFFEFE7D6)       // surface variant
private val Ink = Color(0xFF2B2A26)            // on background / surface
private val SoftInk = Color(0xFF6B6659)        // muted text
private val ClayRed = Color(0xFFB3402E)        // error

private val WarmLightColors = lightColorScheme(
    primary = ForestGreen,
    onPrimary = Color.White,
    primaryContainer = SoftGreenContainer,
    onPrimaryContainer = Color(0xFF14321C),
    secondary = KenteGold,
    onSecondary = Color.White,
    secondaryContainer = GoldContainer,
    onSecondaryContainer = Color(0xFF3D2B00),
    tertiary = KenteGold,
    background = Cream,
    onBackground = Ink,
    surface = WarmWhite,
    onSurface = Ink,
    surfaceVariant = WarmSand,
    onSurfaceVariant = SoftInk,
    error = ClayRed,
    onError = Color.White
)

private val SoftShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun VoiceLedgerTheme(
    content: @Composable () -> Unit
) {
    // Intentionally light-only for now: the warm cream identity is core to the look.
    MaterialTheme(
        colorScheme = WarmLightColors,
        shapes = SoftShapes,
        content = content
    )
}
