package ru.quasaris.characternexus.ui.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Simplified KMP version of outerShadow.
 * In a real project, this might use more complex drawing or expect/actual.
 */
fun Modifier.outerShadow(
    shape: Shape,
    color: Color = Color.Black.copy(alpha = 0.5f),
    blur: Dp = 8.dp,
    offsetY: Dp = 4.dp,
    offsetX: Dp = 0.dp
): Modifier = this.shadow(
    elevation = blur,
    shape = shape,
    ambientColor = color,
    spotColor = color
)
