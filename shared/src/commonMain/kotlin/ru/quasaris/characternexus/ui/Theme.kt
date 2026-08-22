package ru.quasaris.characternexus.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.ApplySystemBarEffects
import ru.quasaris.characternexus.getDynamicColorScheme

fun buildColorSchemeFromSeed(seed: Color, isDark: Boolean): ColorScheme {
    return if (isDark) {
        darkColorScheme(
            primary = seed,
            onPrimary = if (seed.luminance() > 0.5f) Color.Black else Color.White,
            primaryContainer = seed.copy(alpha = 0.3f),
            onPrimaryContainer = Color.White,
            secondary = seed.copy(alpha = 0.8f),
            onSecondary = if (seed.luminance() > 0.5f) Color.Black else Color.White,
            background = Color(0xFF121212),
            surface = Color(0xFF121212),
            onSurface = Color.White,
            surfaceVariant = seed.copy(alpha = 0.1f),
            onSurfaceVariant = Color.White,
            outline = Color.White.copy(alpha = 0.6f)
        )
    } else {
        lightColorScheme(
            primary = seed,
            onPrimary = if (seed.luminance() > 0.5f) Color.Black else Color.White,
            primaryContainer = seed.copy(alpha = 0.2f),
            onPrimaryContainer = seed,
            secondary = seed.copy(alpha = 0.7f),
            onSecondary = if (seed.luminance() > 0.5f) Color.Black else Color.White,
            background = Color.White,
            surface = Color.White,
            onSurface = Color.Black,
            surfaceVariant = seed.copy(alpha = 0.05f),
            onSurfaceVariant = Color.Black,
            outline = Color.Black.copy(alpha = 0.6f)
        )
    }
}

private val StockPrimary = Color(0xFFFF6F4B) // Orange
private val StockSecondary = Color(0xFFFD4C55) // Reddish Orange
private val StockTertiary = Color(0xFF3949AB) // Cold Purple (Indigo)

private val StockLightColors = lightColorScheme(
    primary = StockPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBCB),
    onPrimaryContainer = Color(0xFF341100),
    secondary = StockSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDAD9),
    onSecondaryContainer = Color(0xFF41000A),
    tertiary = StockTertiary,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDFE0FF),
    onTertiaryContainer = Color(0xFF000B63),
    error = Color(0xFFBA1A1A),
    background = Color(0xFFFFFBFF),
    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF201A18),
    surfaceVariant = Color(0xFFF5DED5),
    onSurfaceVariant = Color(0xFF53433E),
    outline = Color(0xFF85736D)
)

private val StockDarkColors = darkColorScheme(
    primary = Color(0xFFFFB59C),
    onPrimary = Color(0xFF5F1500),
    primaryContainer = Color(0xFF862200),
    onPrimaryContainer = Color(0xFFFFDBCB),
    secondary = Color(0xFFFFB3B4),
    onSecondary = Color(0xFF680016),
    secondaryContainer = Color(0xFF920023),
    onSecondaryContainer = Color(0xFFFFDAD9),
    tertiary = Color(0xFFBDBFFF),
    onTertiary = Color(0xFF001596),
    tertiaryContainer = Color(0xFF192A93),
    onTertiaryContainer = Color(0xFFDFE0FF),
    error = Color(0xFFFFB4AB),
    background = Color(0xFF201A18),
    surface = Color(0xFF201A18),
    onSurface = Color(0xFFEDE0DB),
    surfaceVariant = Color(0xFF53433E),
    onSurfaceVariant = Color(0xFFD8C2BB),
    outline = Color(0xFFA08D87)
)

@Composable
fun quasarisTheme(
    themeBehavior: AppThemeBehavior = AppThemeBehavior.SYSTEM,
    themeMode: AppThemeMode = AppThemeMode.M3,
    avatarColor: Int? = null,
    m3SeedColor: String = "#6750A4",
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        AppThemeMode.OFF -> true
        else -> when (themeBehavior) {
            AppThemeBehavior.LIGHT -> false
            AppThemeBehavior.DARK -> true
            AppThemeBehavior.SYSTEM -> systemDark
        }
    }

    val dynamicM3 = getDynamicColorScheme(darkTheme)

    val colorScheme = remember(themeMode, darkTheme, avatarColor, m3SeedColor, dynamicM3) {
        when (themeMode) {
            AppThemeMode.STOCK -> {
                if (darkTheme) StockDarkColors else StockLightColors
            }
            AppThemeMode.M3 -> {
                dynamicM3 ?: run {
                    val seed = try {
                        Color(m3SeedColor.removePrefix("#").toLong(16) or 0xFF000000)
                    } catch (e: Exception) {
                        StockPrimary
                    }
                    buildColorSchemeFromSeed(seed, darkTheme)
                }
            }
            AppThemeMode.OFF -> {
                darkColorScheme(
                    background = Color.Black,
                    surface = Color.Black,
                    onSurface = Color.White,
                    primary = Color.White,
                    onPrimary = Color.Black,
                    primaryContainer = Color(0xFF222222),
                    onPrimaryContainer = Color.White,
                    secondary = Color.White,
                    onSecondary = Color.Black,
                    secondaryContainer = Color(0xFF111111),
                    onSecondaryContainer = Color.White,
                    surfaceVariant = Color(0xFF121212),
                    onSurfaceVariant = Color.White,
                    outline = Color.White.copy(alpha = 0.6f),
                    outlineVariant = Color.White.copy(alpha = 0.3f)
                )
            }
            AppThemeMode.CHARACTER -> {
                val seedColor = avatarColor?.let { Color(it) }
                if (seedColor == null) {
                    if (darkTheme) StockDarkColors else StockLightColors
                } else {
                    buildColorSchemeFromSeed(seedColor, darkTheme)
                }
            }
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
