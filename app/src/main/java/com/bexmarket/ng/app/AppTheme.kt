package com.bexmarket.ng.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkGreen = Color(0xFF006400)

private val DarkColorScheme = darkColorScheme(
    primary = DarkGreen,
    secondary = Color(0xFFE8F5E9),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = DarkGreen,
    secondary = Color(0xFFE8F5E9),
    background = Color(0xFFF9F9F9),
    surface = Color.White,
    onPrimary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black
)

@Composable
fun AppTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val themePreferences = remember { ThemePreferences(context) }
    val isDarkModePref by themePreferences.isDarkMode.collectAsState(initial = isSystemInDarkTheme())

    val colorScheme = if (isDarkModePref) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
