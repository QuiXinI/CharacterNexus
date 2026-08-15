package ru.quasaris.characternexus.backend.cropper

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

import androidx.compose.ui.platform.LocalDensity

@Composable
fun ImageCropperPreview(
    image: ImageBitmap,
    state: ImageCropState,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val containerW = maxWidth
        val containerH = maxHeight

        LaunchedEffect(containerW, containerH, image) {
            state.containerSize = with(density) { Size(containerW.toPx(), containerH.toPx()) }
            state.imageSize = IntSize(image.width, image.height)
            state.reset()
        }

        val androidBitmap = remember(image) { image.asAndroidBitmap() }

        // 1. Image Layer (Bottom)
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawIntoCanvas { canvas ->
                val paint = Paint().apply {
                    isFilterBitmap = true
                    isAntiAlias = true
                }
                canvas.nativeCanvas.drawBitmap(androidBitmap, state.matrix, paint)
            }
        }

        // 2. Overlay Layer (Top)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val viewportRect = state.viewportRect
            if (viewportRect == Rect.Zero) return@Canvas

            val path = Path().apply {
                if (state.shape is CircleImgCropShape) {
                    addOval(viewportRect)
                } else {
                    addRect(viewportRect)
                }
            }
            
            // Draw full screen dark overlay with hole
            clipPath(path, clipOp = ClipOp.Difference) {
                drawRect(color = Color.Black.copy(alpha = 0.8f))
            }
            
            // Draw border
            drawPath(
                path = path,
                color = Color.White.copy(alpha = 0.5f),
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}
