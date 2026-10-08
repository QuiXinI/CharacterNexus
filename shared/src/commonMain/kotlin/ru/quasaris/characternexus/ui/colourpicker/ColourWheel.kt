package ru.quasaris.characternexus.ui.colourpicker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.*

@Composable
fun ColorWheel(
    color: Color,
    onColorChange: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    val hueSweep = remember {
        Brush.sweepGradient(
            colors = listOf(
                Color.Red, Color.Yellow, Color.Green,
                Color.Cyan, Color.Blue, Color.Magenta, Color.Red
            )
        )
    }
    val saturationRadial = remember {
        Brush.radialGradient(
            colors = listOf(Color.White, Color.Transparent)
        )
    }

    val currentHsl = remember(color) { ColourUtils.colorToHsl(color) }
    val currentHue = currentHsl[0]
    val currentSat = currentHsl[1]
    val currentLightness = currentHsl[2]

    var center by remember { mutableStateOf(Offset.Zero) }
    var radius by remember { mutableFloatStateOf(0f) }

    fun updateColorFromOffset(offset: Offset) {
        if (radius <= 0f) return
        val dx = offset.x - center.x
        val dy = offset.y - center.y
        val dist = sqrt(dx * dx + dy * dy)
        val sat = (dist / radius).coerceIn(0f, 1f)
        
        var angle = atan2(dy, dx) * 180f / PI.toFloat()
        if (angle < 0f) angle += 360f

        val effectiveLightness = if (currentLightness <= 0.01f || currentLightness >= 0.99f) 0.5f else currentLightness
        val newColor = ColourUtils.hslToColor(angle, sat, effectiveLightness, color.alpha)
        onColorChange(newColor)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .pointerInput(center, radius) {
                detectTapGestures { offset ->
                    updateColorFromOffset(offset)
                }
            }
            .pointerInput(center, radius) {
                detectDragGestures { change, _ ->
                    change.consume()
                    updateColorFromOffset(change.position)
                }
            }
    ) {
        val minDim = size.minDimension
        val currentCenter = Offset(size.width / 2f, size.height / 2f)
        val currentRadius = minDim / 2f

        center = currentCenter
        radius = currentRadius

        // 1. Draw Hue wheel
        drawCircle(
            brush = hueSweep,
            radius = currentRadius,
            center = currentCenter
        )

        // 2. Overlay White-to-transparent radial gradient for Saturation
        drawCircle(
            brush = saturationRadial,
            radius = currentRadius,
            center = currentCenter
        )

        // 3. Draw active handle indicator thumb
        val angleRad = currentHue * PI.toFloat() / 180f
        val thumbDist = currentSat * currentRadius
        val thumbPos = Offset(
            x = currentCenter.x + thumbDist * cos(angleRad),
            y = currentCenter.y + thumbDist * sin(angleRad)
        )

        val handleOuterRadius = 12.dp.toPx()
        val handleInnerRadius = 8.dp.toPx()

        // Outer white glow/border
        drawCircle(
            color = Color.White,
            radius = handleOuterRadius,
            center = thumbPos
        )
        // Dark stroke outline
        drawCircle(
            color = Color.Black.copy(alpha = 0.6f),
            radius = handleOuterRadius,
            center = thumbPos,
            style = Stroke(width = 2.dp.toPx())
        )
        // Inner fill with opaque active color
        drawCircle(
            color = color.copy(alpha = 1f),
            radius = handleInnerRadius,
            center = thumbPos
        )
    }
}
