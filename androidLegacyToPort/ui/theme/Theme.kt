package ru.quasaris.characters.master.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import ru.quasaris.characters.master.backend.AppThemeMode

private val LightColors = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    error = md_theme_light_error,
    onError = md_theme_light_onError,
    errorContainer = md_theme_light_errorContainer,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline,
)

private val DarkColors = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    error = md_theme_dark_error,
    onError = md_theme_dark_onError,
    errorContainer = md_theme_dark_errorContainer,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    outline = md_theme_dark_outline,
)

@Composable
fun CharacterTheme(
    seedColor: Color?,
    isDark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = remember(seedColor, isDark) {
        if (seedColor == null) {
            if (isDark) DarkColors else LightColors
        } else {
            // Generates a scheme based on the seed color.
            // In a real app, you might use material-color-utilities for better results.
            if (isDark) {
                darkColorScheme(
                    primary = seedColor,
                    onPrimary = if (seedColor.luminance() > 0.5f) Color.Black else Color.White,
                    primaryContainer = seedColor.copy(alpha = 0.3f),
                    onPrimaryContainer = Color.White,
                    secondary = seedColor.copy(alpha = 0.8f),
                    background = Color(0xFF121212),
                    surface = Color(0xFF121212),
                    onSurface = Color.White,
                    surfaceVariant = seedColor.copy(alpha = 0.1f),
                    onSurfaceVariant = Color.White
                )
            } else {
                lightColorScheme(
                    primary = seedColor,
                    onPrimary = if (seedColor.luminance() > 0.5f) Color.Black else Color.White,
                    primaryContainer = seedColor.copy(alpha = 0.2f),
                    onPrimaryContainer = seedColor,
                    secondary = seedColor.copy(alpha = 0.7f),
                    background = Color.White,
                    surface = Color.White,
                    onSurface = Color.Black,
                    surfaceVariant = seedColor.copy(alpha = 0.05f),
                    onSurfaceVariant = Color.Black
                )
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun quasarisTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    themeMode: AppThemeMode = AppThemeMode.M3,
    avatarColor: Int? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    
    val colorScheme = remember(themeMode, darkTheme, avatarColor, dynamicColor) {
        when (themeMode) {
            AppThemeMode.M3 -> {
                if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                } else {
                    if (darkTheme) DarkColors else LightColors
                }
            }
            AppThemeMode.OFF -> {
                darkColorScheme(
                    primary = Color.White,
                    onPrimary = Color.Black,
                    primaryContainer = Color(0xFF222222),
                    onPrimaryContainer = Color.White,
                    secondary = Color.White,
                    onSecondary = Color.Black,
                    secondaryContainer = Color(0xFF111111),
                    onSecondaryContainer = Color.White,
                    background = Color.Black,
                    onBackground = Color.White,
                    surface = Color.Black,
                    onSurface = Color.White,
                    surfaceVariant = Color(0xFF121212),
                    onSurfaceVariant = Color.White,
                    outline = Color.White.copy(alpha = 0.6f),
                    outlineVariant = Color.White.copy(alpha = 0.3f)
                )
            }
            AppThemeMode.CHARACTER -> {
                val seedColor = avatarColor?.let { Color(it) }
                if (seedColor == null) {
                    if (darkTheme) DarkColors else LightColors
                } else {
                    if (darkTheme) {
                        darkColorScheme(
                            primary = seedColor,
                            onPrimary = if (seedColor.luminance() > 0.5f) Color.Black else Color.White,
                            primaryContainer = seedColor.copy(alpha = 0.3f),
                            onPrimaryContainer = Color.White,
                            secondary = seedColor.copy(alpha = 0.8f),
                            background = Color(0xFF121212),
                            surface = Color(0xFF121212),
                            onSurface = Color.White,
                            surfaceVariant = seedColor.copy(alpha = 0.1f),
                            onSurfaceVariant = Color.White
                        )
                    } else {
                        lightColorScheme(
                            primary = seedColor,
                            onPrimary = if (seedColor.luminance() > 0.5f) Color.Black else Color.White,
                            primaryContainer = seedColor.copy(alpha = 0.2f),
                            onPrimaryContainer = seedColor,
                            secondary = seedColor.copy(alpha = 0.7f),
                            background = Color.White,
                            surface = Color.White,
                            onSurface = Color.Black,
                            surfaceVariant = seedColor.copy(alpha = 0.05f),
                            onSurfaceVariant = Color.Black
                        )
                    }
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography
    ) {
        ApplySideEffects(colorScheme, darkTheme, themeMode)
        content()
    }
}

@Composable
private fun ApplySideEffects(colorScheme: ColorScheme, darkTheme: Boolean, themeMode: AppThemeMode) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme && themeMode != AppThemeMode.OFF
            insetsController.isAppearanceLightNavigationBars = !darkTheme && themeMode != AppThemeMode.OFF
            window.statusBarColor = colorScheme.surface.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
        }
    }
}
