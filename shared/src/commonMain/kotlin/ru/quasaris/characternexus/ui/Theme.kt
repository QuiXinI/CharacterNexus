package ru.quasaris.characternexus.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import ru.quasaris.characternexus.AppThemeMode
import ru.quasaris.characternexus.ApplySystemBarEffects

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
fun quasarisTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeMode: AppThemeMode = AppThemeMode.M3,
    avatarColor: Int? = null,
    content: @Composable () -> Unit
) {
    val colorScheme = remember(themeMode, darkTheme, avatarColor) {
        when (themeMode) {
            AppThemeMode.OFF -> {
                darkColorScheme(
                    background = Color.Black,
                    surface = Color.Black,
                    onSurface = Color.White,
                    primary = Color.White,
                    onPrimary = Color.Black
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
                            background = Color(0xFF121212),
                            surface = Color(0xFF121212),
                            onSurface = Color.White
                        )
                    } else {
                        lightColorScheme(
                            primary = seedColor,
                            onPrimary = if (seedColor.luminance() > 0.5f) Color.Black else Color.White,
                            background = Color.White,
                            surface = Color.White,
                            onSurface = Color.Black
                        )
                    }
                }
            }
            else -> if (darkTheme) DarkColors else LightColors
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography
    ) {
        ApplySystemBarEffects(colorScheme.surface, darkTheme)
        content()
    }
}
