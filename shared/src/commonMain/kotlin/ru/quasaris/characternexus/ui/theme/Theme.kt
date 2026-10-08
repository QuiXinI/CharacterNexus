package ru.quasaris.characternexus.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.ApplySystemBarEffects
import ru.quasaris.characternexus.getDynamicColorScheme

val LocalAppThemeMode = compositionLocalOf { AppThemeMode.M3 }

private fun parseColor(hex: String, fallback: Color): Color {
    return try {
        Color(hex.removePrefix("#").toLong(16) or 0xFF000000)
    } catch (e: Exception) {
        fallback
    }
}

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
    primary = Color(0xFFFFAE3D),
    onPrimary = Color(0xFF4A2800),
    primaryContainer = Color(0xFF663B00),
    onPrimaryContainer = Color(0xFFFFDCBE),
    secondary = Color(0xFFF05D35),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF5C2214),
    onSecondaryContainer = Color(0xFFFFDBD1),
    tertiary = Color(0xFF5CDBBA),
    onTertiary = Color(0xFF00382B),
    tertiaryContainer = Color(0xFF005140),
    onTertiaryContainer = Color(0xFF7CE4C8),
    error = Color(0xFFFFB4AB),
    background = Color(0xFF261C19),
    surface = Color(0xFF261C19),
    onSurface = Color(0xFFEDE0DC),
    surfaceVariant = Color(0xFF3D2F2A),
    onSurfaceVariant = Color(0xFFEDE0DC),
    outline = Color(0xFFA08D87),
    surfaceContainer = Color(0xFF322622),
    surfaceContainerHigh = Color(0xFF3D2F2A)
)

@Composable
fun quasarisTheme(
    themeBehavior: AppThemeBehavior = AppThemeBehavior.SYSTEM,
    themeMode: AppThemeMode = AppThemeMode.M3,
    avatarColor: Int? = null,
    m3SeedColor: String = "#6750A4",
    customThemeSeed: String = "#6750A4",
    customUseFlexibleColors: Boolean = false,
    customPrimary: String = "#6750A4",
    customOnPrimary: String = "#FFFFFF",
    customPrimaryContainer: String = "#EADDFF",
    customOnPrimaryContainer: String = "#21005D",
    customSecondary: String = "#625B71",
    customOnSecondary: String = "#FFFFFF",
    customSecondaryContainer: String = "#E8DEF8",
    customOnSecondaryContainer: String = "#1D192B",
    customTertiary: String = "#7D5260",
    customOnTertiary: String = "#FFFFFF",
    customTertiaryContainer: String = "#FFD8E4",
    customOnTertiaryContainer: String = "#31111D",
    customBackground: String = "#FFFBFE",
    customOnBackground: String = "#1C1B1F",
    customSurface: String = "#FFFBFE",
    customOnSurface: String = "#1C1B1F",
    customSurfaceVariant: String = "#E7E0EC",
    customOnSurfaceVariant: String = "#49454F",
    customOutline: String = "#79747E",
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        AppThemeMode.OFF -> true
        AppThemeMode.WHITE -> false
        else -> when (themeBehavior) {
            AppThemeBehavior.LIGHT -> false
            AppThemeBehavior.DARK -> true
            AppThemeBehavior.SYSTEM -> systemDark
        }
    }

    val dynamicM3 = getDynamicColorScheme(darkTheme)

    val colorScheme = remember(
        themeMode, darkTheme, avatarColor, m3SeedColor,
        customThemeSeed, customUseFlexibleColors,
        customPrimary, customOnPrimary, customPrimaryContainer, customOnPrimaryContainer,
        customSecondary, customOnSecondary, customSecondaryContainer, customOnSecondaryContainer,
        customTertiary, customOnTertiary, customTertiaryContainer, customOnTertiaryContainer,
        customBackground, customOnBackground, customSurface, customOnSurface,
        customSurfaceVariant, customOnSurfaceVariant, customOutline,
        dynamicM3
    ) {
        when (themeMode) {
            AppThemeMode.STOCK -> {
                if (darkTheme) StockDarkColors else StockLightColors
            }
            AppThemeMode.M3 -> {
                dynamicM3 ?: run {
                    val seed = parseColor(m3SeedColor, StockPrimary)
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
                    primaryContainer = Color.Black,
                    onPrimaryContainer = Color.White,
                    secondary = Color.White,
                    onSecondary = Color.Black,
                    secondaryContainer = Color.Black,
                    onSecondaryContainer = Color.White,
                    tertiary = Color.White,
                    onTertiary = Color.Black,
                    tertiaryContainer = Color.Black,
                    onTertiaryContainer = Color.White,
                    surfaceVariant = Color.Black,
                    onSurfaceVariant = Color.White,
                    outline = Color.White,
                    outlineVariant = Color.White.copy(alpha = 0.5f),
                    error = Color.White,
                    onError = Color.Black,
                    errorContainer = Color.Black,
                    onErrorContainer = Color.White,
                    onBackground = Color.White
                )
            }
            AppThemeMode.WHITE -> {
                lightColorScheme(
                    background = Color.White,
                    surface = Color.White,
                    onSurface = Color.Black,
                    primary = Color.Black,
                    onPrimary = Color.White,
                    primaryContainer = Color.White,
                    onPrimaryContainer = Color.Black,
                    secondary = Color.Black,
                    onSecondary = Color.White,
                    secondaryContainer = Color.White,
                    onSecondaryContainer = Color.Black,
                    tertiary = Color.Black,
                    onTertiary = Color.White,
                    tertiaryContainer = Color.White,
                    onTertiaryContainer = Color.Black,
                    surfaceVariant = Color.White,
                    onSurfaceVariant = Color.Black,
                    outline = Color.Black,
                    outlineVariant = Color.Black.copy(alpha = 0.3f),
                    error = Color.Black,
                    onError = Color.White,
                    errorContainer = Color.White,
                    onErrorContainer = Color.Black,
                    onBackground = Color.Black
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
            AppThemeMode.CUSTOM -> {
                if (customUseFlexibleColors) {
                    val p = parseColor(customPrimary, Color(0xFF6750A4))
                    val onP = parseColor(customOnPrimary, Color.White)
                    val pCont = parseColor(customPrimaryContainer, Color(0xFFEADDFF))
                    val onPCont = parseColor(customOnPrimaryContainer, Color(0xFF21005D))
                    val s = parseColor(customSecondary, Color(0xFF625B71))
                    val onS = parseColor(customOnSecondary, Color.White)
                    val sCont = parseColor(customSecondaryContainer, Color(0xFFE8DEF8))
                    val onSCont = parseColor(customOnSecondaryContainer, Color(0xFF1D192B))
                    val t = parseColor(customTertiary, Color(0xFF7D5260))
                    val onT = parseColor(customOnTertiary, Color.White)
                    val tCont = parseColor(customTertiaryContainer, Color(0xFFFFD8E4))
                    val onTCont = parseColor(customOnTertiaryContainer, Color(0xFF31111D))
                    val bg = parseColor(customBackground, if (darkTheme) Color(0xFF121212) else Color.White)
                    val onBg = parseColor(customOnBackground, if (darkTheme) Color.White else Color.Black)
                    val surf = parseColor(customSurface, if (darkTheme) Color(0xFF121212) else Color.White)
                    val onSurf = parseColor(customOnSurface, if (darkTheme) Color.White else Color.Black)
                    val surfVar = parseColor(customSurfaceVariant, if (darkTheme) Color.DarkGray else Color.LightGray)
                    val onSurfVar = parseColor(customOnSurfaceVariant, if (darkTheme) Color.White else Color.Black)
                    val out = parseColor(customOutline, if (darkTheme) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.6f))

                    if (darkTheme) {
                        darkColorScheme(
                            primary = p, onPrimary = onP, primaryContainer = pCont, onPrimaryContainer = onPCont,
                            secondary = s, onSecondary = onS, secondaryContainer = sCont, onSecondaryContainer = onSCont,
                            tertiary = t, onTertiary = onT, tertiaryContainer = tCont, onTertiaryContainer = onTCont,
                            background = bg, onBackground = onBg, surface = surf, onSurface = onSurf,
                            surfaceVariant = surfVar, onSurfaceVariant = onSurfVar, outline = out
                        )
                    } else {
                        lightColorScheme(
                            primary = p, onPrimary = onP, primaryContainer = pCont, onPrimaryContainer = onPCont,
                            secondary = s, onSecondary = onS, secondaryContainer = sCont, onSecondaryContainer = onSCont,
                            tertiary = t, onTertiary = onT, tertiaryContainer = tCont, onTertiaryContainer = onTCont,
                            background = bg, onBackground = onBg, surface = surf, onSurface = onSurf,
                            surfaceVariant = surfVar, onSurfaceVariant = onSurfVar, outline = out
                        )
                    }
                } else {
                    val seed = parseColor(customThemeSeed, StockPrimary)
                    buildColorSchemeFromSeed(seed, darkTheme)
                }
            }
        }
    }

    CompositionLocalProvider(LocalAppThemeMode provides themeMode) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography
        ) {
            ApplySystemBarEffects(colorScheme.surface, darkTheme)
            content()
        }
    }
}
