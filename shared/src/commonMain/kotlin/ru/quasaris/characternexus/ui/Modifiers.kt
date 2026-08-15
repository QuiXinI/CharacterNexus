package ru.quasaris.characternexus.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Adds an outer shadow to the component that doesn't bleed through transparent backgrounds.
 * It clips the shadow area under the component itself.
 */
expect fun Modifier.outerShadow(
    shape: Shape,
    color: Color = Color(0x80000000),
    blur: Dp = 8.dp,
    offsetY: Dp = 4.dp,
    offsetX: Dp = 0.dp
): Modifier
