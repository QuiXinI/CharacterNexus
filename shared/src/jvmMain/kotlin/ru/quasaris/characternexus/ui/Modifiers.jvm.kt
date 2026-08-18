package ru.quasaris.characternexus.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asSkiaPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.unit.Dp
import org.jetbrains.skia.FilterBlurMode
import org.jetbrains.skia.MaskFilter
import org.jetbrains.skia.Paint

actual fun Modifier.outerShadow(
    shape: Shape,
    color: Color,
    blur: Dp,
    offsetY: Dp,
    offsetX: Dp
): Modifier = this.drawBehind {
    drawIntoCanvas { canvas ->
        val outline = shape.createOutline(size, layoutDirection, this)
        val path = Path().apply { addOutline(outline) }

        canvas.save()
        canvas.clipPath(path, clipOp = ClipOp.Difference)

        val skiaPaint = Paint().apply {
            maskFilter = MaskFilter.makeBlur(FilterBlurMode.NORMAL, blur.toPx())
            this.color = color.toArgb()
        }

        val skiaCanvas = canvas.nativeCanvas as org.jetbrains.skia.Canvas
        skiaCanvas.save()
        skiaCanvas.translate(offsetX.toPx(), offsetY.toPx())
        skiaCanvas.drawPath(path.asSkiaPath(), skiaPaint)
        skiaCanvas.restore()

        canvas.restore()
    }
}
