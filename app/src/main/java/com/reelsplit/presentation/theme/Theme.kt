package com.reelsplit.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Brand Colors
private val Primary = Color(0xFF6366F1)
private val PrimaryVariant = Color(0xFF4F46E5)
private val Secondary = Color(0xFFEC4899)
private val SecondaryVariant = Color(0xFFDB2777)

// Light Theme Colors
private val LightBackground = Color(0xFFFFFFFF)
private val LightSurface = Color(0xFFF8FAFC)
private val LightOnBackground = Color(0xFF0F172A)
private val LightOnSurface = Color(0xFF1E293B)

// Dark Theme Colors
private val DarkBackground = Color(0xFF0F172A)
private val DarkSurface = Color(0xFF1E293B)
private val DarkOnBackground = Color(0xFFF8FAFC)
private val DarkOnSurface = Color(0xFFE2E8F0)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = Primary.copy(alpha = 0.12f),
    onPrimaryContainer = PrimaryVariant,
    secondary = Secondary,
    onSecondary = Color.White,
    secondaryContainer = Secondary.copy(alpha = 0.12f),
    onSecondaryContainer = SecondaryVariant,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    error = Color(0xFFEF4444),
    onError = Color.White,
)

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimaryVariant,
    onPrimaryContainer = Color.White,
    secondary = Secondary,
    onSecondary = Color.White,
    secondaryContainer = SecondaryVariant,
    onSecondaryContainer = Color.White,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    error = Color(0xFFFF6B6B),
    onError = Color.White,
)

@Composable
fun ReelSplitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disabled by default to use brand colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
