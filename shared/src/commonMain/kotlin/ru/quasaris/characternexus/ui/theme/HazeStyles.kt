package ru.quasaris.characternexus.ui.theme

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.*
import ru.quasaris.characternexus.backend.SettingsViewModel
import ru.quasaris.characternexus.model.AppThemeMode

/**
 * Standard Haze style for all popovers and overlays in the app.
 * Uses settings-defined blur radius and consistent transparency.
 */
@Composable
fun rememberEffectiveBlurRadius(settingsViewModel: SettingsViewModel?): Dp {
    val blurRadiusVal by settingsViewModel?.blurRadius?.collectAsState() ?: remember { mutableStateOf(24) }
    val customBlurRadiusVal by settingsViewModel?.customBlurRadius?.collectAsState() ?: remember { mutableStateOf(24) }
    val targetBlurRadius = if (blurRadiusVal >= 48) customBlurRadiusVal else blurRadiusVal
    return targetBlurRadius.dp
}

@Composable
fun rememberEffectiveHazeStyle(
    blurRadius: Dp,
    tintAlpha: Float = 0.1f
): HazeStyle {
    val colorScheme = androidx.compose.material3.MaterialTheme.colorScheme
    val themeMode = LocalAppThemeMode.current
    val isBlack = themeMode == AppThemeMode.OFF
    val isLight = colorScheme.background.luminance() > 0.5f
    val tintColor = when {
        isBlack -> Color.Black.copy(alpha = tintAlpha)
        isLight -> Color.White.copy(alpha = tintAlpha)
        else -> Color.Black.copy(alpha = tintAlpha)
    }
    return remember(blurRadius, tintAlpha, isBlack, isLight) {
        HazeStyle(
            blurRadius = blurRadius,
            tints = listOf(HazeTint(tintColor))
        )
    }
}

@Composable
fun Modifier.hazePopover(
    state: HazeState?,
    blurRadius: Dp,
    tint: Color = Color.Unspecified,
    alpha: Float = 0.4f,
    forceBlurEnabled: Boolean = true,
    isOled: Boolean = false
): Modifier {
    val defaultStyle = rememberEffectiveHazeStyle(blurRadius = blurRadius, tintAlpha = alpha)
    val effectiveTint = if (tint != Color.Unspecified) tint else defaultStyle.tints.first().color

    return this.run {
        if (forceBlurEnabled && state != null) {
            this.hazeEffect(
                state = state,
                style = HazeStyle(
                    blurRadius = blurRadius,
                    tints = listOf(HazeTint(effectiveTint))
                )
            )
        } else this
    }
}
