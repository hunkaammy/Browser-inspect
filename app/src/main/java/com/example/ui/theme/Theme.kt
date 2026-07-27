package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = DevCyan,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF164E63),
    onPrimaryContainer = Color(0xFFCFFAFE),
    secondary = DevEmerald,
    onSecondary = Color.Black,
    tertiary = DevAmber,
    background = DevDarkBackground,
    onBackground = Color(0xFFF3F4F6),
    surface = DevDarkSurface,
    onSurface = Color(0xFFF3F4F6),
    surfaceVariant = DevDarkCard,
    onSurfaceVariant = Color(0xFFD1D5DB)
)

private val LightColorScheme = darkColorScheme(
    primary = DevCyan,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF164E63),
    onPrimaryContainer = Color(0xFFCFFAFE),
    secondary = DevEmerald,
    onSecondary = Color.Black,
    tertiary = DevAmber,
    background = DevDarkBackground,
    onBackground = Color(0xFFF3F4F6),
    surface = DevDarkSurface,
    onSurface = Color(0xFFF3F4F6),
    surfaceVariant = DevDarkCard,
    onSurfaceVariant = Color(0xFFD1D5DB)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = DarkColorScheme
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

