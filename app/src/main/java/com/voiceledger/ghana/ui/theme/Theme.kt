package com.voiceledger.ghana.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GhanaGreen = Color(0xFF2E7D32)
private val GhanaGold = Color(0xFFF9A825)

private val LightColors = lightColorScheme(
    primary = GhanaGreen,
    secondary = GhanaGold
)

private val DarkColors = darkColorScheme(
    primary = GhanaGreen,
    secondary = GhanaGold
)

@Composable
fun VoiceLedgerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
