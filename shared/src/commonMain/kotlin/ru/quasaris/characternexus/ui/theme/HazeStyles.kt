package ru.quasaris.characternexus.ui.theme

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.*
import ru.quasaris.characternexus.backend.SettingsViewModel

/**
 * Standard Haze style for all popovers and overlays in the app.
 * Uses settings-defined blur radius and consistent transparency.
 */
@Composable
fun rememberEffectiveBlurRadius(settingsViewModel: SettingsViewModel?): Dp {
    val blurRadiusVal by settingsViewModel?.blurRadius?.collectAsState() ?: remember { mutableStateOf(16) }
    val customBlurRadiusVal by settingsViewModel?.customBlurRadius?.collectAsState() ?: remember { mutableStateOf(16) }
    val targetBlurRadius = if (blurRadiusVal >= 48) customBlurRadiusVal else blurRadiusVal
    return targetBlurRadius.dp
}

fun Modifier.hazePopover(
    state: HazeState?,
    blurRadius: Dp,
    tint: Color = Color.Black,
    alpha: Float = 0.2f,
    forceBlurEnabled: Boolean = true,
    isOled: Boolean = false
): Modifier = this.run {
    if (forceBlurEnabled && state != null && !isOled) {
        this.hazeEffect(state = state) {
            style = HazeStyle(
                blurRadius = blurRadius,
                tints = listOf(HazeTint(tint.copy(alpha = alpha)))
            )
        }
    } else this
}
