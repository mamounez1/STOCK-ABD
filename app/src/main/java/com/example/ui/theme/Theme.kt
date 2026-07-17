package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = StockCyanAccent,
    onPrimary = Color(0xFF023E8A),
    primaryContainer = Color(0xFF0077B6),
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = StockBlueLight,
    onSecondary = Color.White,
    background = Slate900,
    onBackground = OnSlate900,
    surface = Slate800,
    onSurface = OnSlate900,
    error = StockRed,
    onError = Color.White,
    surfaceVariant = Slate700,
    onSurfaceVariant = OnSlate900
)

private val LightColorScheme = lightColorScheme(
    primary = StockBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = StockBlueLight,
    onSecondary = Color.White,
    background = Slate50,
    onBackground = OnSlate100,
    surface = Color.White,
    onSurface = OnSlate100,
    error = StockRed,
    onError = Color.White,
    surfaceVariant = Slate100,
    onSurfaceVariant = OnSlate100
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
