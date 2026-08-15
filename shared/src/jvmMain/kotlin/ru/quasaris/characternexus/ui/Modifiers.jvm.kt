package ru.quasaris.characternexus.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp

actual fun Modifier.outerShadow(
    shape: Shape,
    color: Color,
    blur: Dp,
    offsetY: Dp,
    offsetX: Dp
): Modifier = this.shadow(
    elevation = blur,
    shape = shape,
    ambientColor = color,
    spotColor = color
)
// TODO: Implement clipping version for JVM using Skia nativeCanvas if needed
