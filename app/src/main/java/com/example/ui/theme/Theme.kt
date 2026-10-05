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
    primary = PrimaryBlueDark,
    onPrimary = Color.Black,
    primaryContainer = Slate800,
    onPrimaryContainer = PrimaryBlueDark,
    secondary = AccentCyan,
    onSecondary = Color.Black,
    secondaryContainer = Slate700,
    onSecondaryContainer = Color.White,
    background = Slate900,
    onBackground = Slate100,
    surface = Slate800,
    onSurface = Slate100,
    surfaceVariant = Slate700,
    onSurfaceVariant = Slate100,
    error = RoseError
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = PrimaryBlue,
    secondary = AccentCyan,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E8F0),
    onSecondaryContainer = Color(0xFF1E293B),
    background = Slate50,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate700,
    error = RoseError
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    accentTheme: String = "Modern Slate",
    content: @Composable () -> Unit
) {
    val baseScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Apply custom accent tint if selected
    val accentColor = when (accentTheme) {
        "Midnight Blue" -> ThemeMidnightBlue
        "Emerald Forest" -> ThemeEmeraldForest
        "Sunset Crimson" -> ThemeSunsetCrimson
        "Royal Purple" -> ThemeRoyalPurple
        else -> ThemeModernSlate
    }

    val customScheme = baseScheme.copy(
        primary = accentColor,
        primaryContainer = if (darkTheme) Slate800 else accentColor.copy(alpha = 0.15f),
        onPrimaryContainer = if (darkTheme) accentColor else accentColor
    )

    MaterialTheme(
        colorScheme = customScheme,
        typography = Typography,
        content = content
    )
}
