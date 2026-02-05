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

// =============================================================================
// Light Color Scheme
// =============================================================================

private val LightColorScheme = lightColorScheme(
    // Primary colors
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF), // Proper opaque indigo container
    onPrimaryContainer = PrimaryVariant,
    inversePrimary = PrimaryLight,
    
    // Secondary colors
    secondary = Secondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFCE7F3), // Proper opaque pink container
    onSecondaryContainer = SecondaryVariant,
    
    // Tertiary colors
    tertiary = Tertiary,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFEDD5), // Proper opaque orange container
    onTertiaryContainer = TertiaryVariant,
    
    // Background and surface
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceTint = Primary,
    inverseSurface = DarkSurface,
    inverseOnSurface = DarkOnSurface,
    
    // Surface containers (Material 3 1.2+)
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
    
    // Error colors
    error = ErrorLight,
    onError = OnError,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    
    // Outline colors
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    
    // Scrim
    scrim = Color.Black.copy(alpha = 0.32f)
)

// =============================================================================
// Dark Color Scheme
// =============================================================================

private val DarkColorScheme = darkColorScheme(
    // Primary colors
    primary = PrimaryLight,
    onPrimary = PrimaryVariant,
    primaryContainer = Color(0xFF312E81), // Deep indigo container for dark mode
    onPrimaryContainer = PrimaryLight,
    inversePrimary = Primary,
    
    // Secondary colors
    secondary = SecondaryLight,
    onSecondary = SecondaryVariant,
    secondaryContainer = Color(0xFF831843), // Deep pink container for dark mode
    onSecondaryContainer = SecondaryLight,
    
    // Tertiary colors
    tertiary = TertiaryLight,
    onTertiary = TertiaryVariant,
    tertiaryContainer = Color(0xFF7C2D12), // Deep orange container for dark mode
    onTertiaryContainer = TertiaryLight,
    
    // Background and surface
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceTint = PrimaryLight,
    inverseSurface = LightSurface,
    inverseOnSurface = LightOnSurface,
    
    // Surface containers (Material 3 1.2+)
    surfaceContainerLowest = DarkBackground,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
    
    // Error colors
    error = ErrorDark,
    onError = OnError,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    
    // Outline colors
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    
    // Scrim
    scrim = Color.Black.copy(alpha = 0.5f)
)

// =============================================================================
// ReelSplit Theme Composable
// =============================================================================

/**
 * Main theme composable for the ReelSplit app.
 * 
 * Supports:
 * - Light and dark themes based on system settings
 * - Dynamic colors on Android 12+ (Material You)
 * - Edge-to-edge display with proper status bar handling
 * 
 * @param darkTheme Whether to use dark theme. Defaults to system setting.
 * @param dynamicColor Whether to use dynamic colors (Android 12+). Defaults to false
 *                     to preserve brand colors. Set to true to use Material You.
 * @param content The composable content to display within the theme.
 */
@Composable
fun ReelSplitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disabled by default to preserve brand identity
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Use dynamic colors on Android 12+ if enabled
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Set up edge-to-edge display
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val window = activity.window
            
            // Make status bar and navigation bar transparent
            @Suppress("DEPRECATION")
            window.statusBarColor = Color.Transparent.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = Color.Transparent.toArgb()
            
            // Set appearance for status bar icons based on theme
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
