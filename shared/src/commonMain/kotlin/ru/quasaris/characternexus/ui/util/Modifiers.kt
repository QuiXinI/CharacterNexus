package ru.quasaris.characternexus.ui.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.quasaris.characternexus.ui.outerShadow as coreOuterShadow

/**
 * Simplified KMP version of outerShadow.
 * It uses the platform-optimized implementation that clips the shadow under the component.
 */
fun Modifier.outerShadow(
    shape: Shape,
    color: Color = Color.Black.copy(alpha = 0.5f),
    blur: Dp = 8.dp,
    offsetY: Dp = 4.dp,
    offsetX: Dp = 0.dp
): Modifier = this.coreOuterShadow(
    shape = shape,
    color = color,
    blur = blur,
    offsetY = offsetY,
    offsetX = offsetX
)
