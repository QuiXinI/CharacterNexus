package ru.quasaris.characternexus.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
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
import androidx.compose.ui.unit.dp

actual fun Modifier.outerShadow(
    shape: Shape,
    color: Color,
    blur: Dp,
    offsetY: Dp,
    offsetX: Dp
): Modifier = this.drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    val path = Path().apply { addOutline(outline) }
    val androidPath = path.asAndroidPath()
    
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
        this.color = color.toArgb()
        setShadowLayer(
            blur.toPx(),
            offsetX.toPx(),
            offsetY.toPx(),
            color.toArgb()
        )
    }

    onDrawBehind {
        if (blur <= 0.dp && offsetY == 0.dp && offsetX == 0.dp) return@onDrawBehind

        drawIntoCanvas { canvas ->
            canvas.save()
            try {
                canvas.clipPath(path, clipOp = ClipOp.Difference)
                canvas.nativeCanvas.drawPath(androidPath, paint)
            } finally {
                canvas.restore()
            }
        }
    }
}
