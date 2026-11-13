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
    if (blur <= 0.dp && offsetY == 0.dp && offsetX == 0.dp) {
        return@drawWithCache onDrawBehind {}
    }

    val outline = shape.createOutline(size, layoutDirection, this)
    val path = Path().apply { addOutline(outline) }
    val androidPath = path.asAndroidPath()

    val blurPx = blur.toPx().coerceAtLeast(0.01f)
    val offsetXPx = offsetX.toPx()
    val offsetYPx = offsetY.toPx()

    val shadowPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        this.color = color.toArgb()
        setShadowLayer(
            blurPx,
            offsetXPx,
            offsetYPx,
            color.toArgb()
        )
    }

    val erasePaint = android.graphics.Paint().apply {
        isAntiAlias = true
        xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_OUT)
    }

    onDrawBehind {
        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            // Hardware-accelerated offscreen layer for GPU PorterDuff blending
            val checkpoint = nativeCanvas.saveLayer(
                -blurPx + offsetXPx,
                -blurPx + offsetYPx,
                size.width + blurPx + offsetXPx,
                size.height + blurPx + offsetYPx,
                null
            )
            try {
                // 1. Draw hardware shadow
                nativeCanvas.drawPath(androidPath, shadowPaint)
                // 2. Erase inner shape using GPU hardware DST_OUT blending (prevents shadow from crawling under transparent cards)
                nativeCanvas.drawPath(androidPath, erasePaint)
            } finally {
                nativeCanvas.restoreToCount(checkpoint)
            }
        }
    }
}
