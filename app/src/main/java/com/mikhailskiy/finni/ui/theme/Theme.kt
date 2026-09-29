package com.mikhailskiy.finni.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FinniColorScheme = lightColorScheme(
    primary = FinniPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8E1FF),
    onPrimaryContainer = FinniPurpleDark,
    secondary = FinniGreen,
    onSecondary = Color.White,
    secondaryContainer = FinniMint,
    onSecondaryContainer = Color(0xFF083D33),
    tertiary = FinniOrange,
    onTertiary = Color.White,
    tertiaryContainer = FinniYellow,
    onTertiaryContainer = Color(0xFF4E2B00),
    background = FinniBackground,
    onBackground = FinniInk,
    surface = Color.White,
    onSurface = FinniInk,
    surfaceVariant = Color(0xFFF0EDF7),
    onSurfaceVariant = FinniMuted,
    outline = FinniOutline,
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFFFDAD6),
)

@Composable
fun FinniTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FinniColorScheme,
        typography = Typography,
        content = content,
    )
}
