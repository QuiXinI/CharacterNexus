package ru.quasaris.characternexus.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp

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

        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            this.color = android.graphics.Color.TRANSPARENT
            setShadowLayer(
                blur.toPx(),
                offsetX.toPx(),
                offsetY.toPx(),
                color.toArgb()
            )
        }

        canvas.nativeCanvas.drawPath(path.asAndroidPath(), paint)
        canvas.restore()
    }
}
