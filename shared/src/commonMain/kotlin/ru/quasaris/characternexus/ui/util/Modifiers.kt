package ru.quasaris.characternexus.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.quasaris.characternexus.model.AppThemeMode
import ru.quasaris.characternexus.ui.theme.LocalAppThemeMode
import ru.quasaris.characternexus.ui.outerShadow as coreOuterShadow

/**
 * Simplified KMP version of outerShadow.
 * It uses the platform-optimized implementation that clips the shadow under the component.
 */
@Composable
fun Modifier.outerShadow(
    shape: Shape,
    color: Color = Color.Unspecified,
    blur: Dp = 8.dp,
    offsetY: Dp = 4.dp,
    offsetX: Dp = 0.dp
): Modifier {
    val themeMode = LocalAppThemeMode.current
    val effectiveColor = if (color != Color.Unspecified) color else {
        when (themeMode) {
            AppThemeMode.OFF -> Color.White.copy(alpha = 0.4f)
            AppThemeMode.WHITE -> Color.Black.copy(alpha = 0.3f)
            else -> Color.Black.copy(alpha = 0.5f)
        }
    }
    return this.coreOuterShadow(
        shape = shape,
        color = effectiveColor,
        blur = blur,
        offsetY = offsetY,
        offsetX = offsetX
    )
}
