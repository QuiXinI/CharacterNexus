package ru.quasaris.characternexus.backend.cropper

import android.graphics.Matrix
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
import kotlin.math.max
import kotlin.math.min

interface ImageCropState {
    var matrix: Matrix
    var shape: ImageCropShape
    
    var containerSize: Size
    var imageSize: IntSize
    
    val viewportRect: Rect

    fun rotateCW()
    fun rotateCCW()
    fun flipHorizontal()
    fun flipVertical()
    
    fun onTransform(pan: Offset, zoom: Float)
    
    fun reset()
}

class ImageCropStateImpl(
    initialShape: ImageCropShape,
    @Suppress("UNUSED_PARAMETER") initialAspectRatio: ImageAspectRatio,
) : ImageCropState {
    override var matrix by mutableStateOf(Matrix())
    override var shape by mutableStateOf(initialShape)
    
    override var containerSize by mutableStateOf(Size.Zero)
    override var imageSize by mutableStateOf(IntSize.Zero)

    override val viewportRect: Rect
        get() {
            if (containerSize == Size.Zero) return Rect.Zero
            val side = min(containerSize.width, containerSize.height) * 0.8f
            return Rect(
                offset = Offset((containerSize.width - side) / 2f, (containerSize.height - side) / 2f),
                size = Size(side, side)
            )
        }

    override fun rotateCW() {
        val newMatrix = Matrix(matrix)
        newMatrix.postRotate(90f, viewportRect.center.x, viewportRect.center.y)
        matrix = newMatrix
        clamp()
    }

    override fun rotateCCW() {
        val newMatrix = Matrix(matrix)
        newMatrix.postRotate(-90f, viewportRect.center.x, viewportRect.center.y)
        matrix = newMatrix
        clamp()
    }

    override fun flipHorizontal() {
        val newMatrix = Matrix(matrix)
        newMatrix.postScale(-1f, 1f, viewportRect.center.x, viewportRect.center.y)
        matrix = newMatrix
        clamp()
    }

    override fun flipVertical() {
        val newMatrix = Matrix(matrix)
        newMatrix.postScale(1f, -1f, viewportRect.center.x, viewportRect.center.y)
        matrix = newMatrix
        clamp()
    }

    override fun onTransform(pan: Offset, zoom: Float) {
        val newMatrix = Matrix(matrix)
        val center = viewportRect.center
        newMatrix.postScale(zoom, zoom, center.x, center.y)
        newMatrix.postTranslate(pan.x, pan.y)
        matrix = newMatrix
        clamp()
    }

    override fun reset() {
        if (containerSize == Size.Zero || imageSize == IntSize.Zero) return
        
        val viewport = viewportRect
        val imgW = imageSize.width.toFloat()
        val imgH = imageSize.height.toFloat()
        
        val newMatrix = Matrix()
        
        // 1. Initial scale to cover viewport
        val scale = max(viewport.width / imgW, viewport.height / imgH)
        newMatrix.postScale(scale, scale)
        
        // 2. Initial translation to center
        val offsetX = viewport.center.x - (imgW * scale) / 2f
        val offsetY = viewport.center.y - (imgH * scale) / 2f
        newMatrix.postTranslate(offsetX, offsetY)
        
        matrix = newMatrix
        clamp()
    }

    private fun clamp() {
        if (containerSize == Size.Zero || imageSize == IntSize.Zero) return
        
        val viewport = viewportRect
        val imgW = imageSize.width.toFloat()
        val imgH = imageSize.height.toFloat()
        
        val newMatrix = Matrix(matrix)
        
        // Transformed image corners
        val corners = floatArrayOf(
            0f, 0f,
            imgW, 0f,
            imgW, imgH,
            0f, imgH
        )
        newMatrix.mapPoints(corners)
        
        // For 90-degree rotations, the image is still an axis-aligned rectangle on screen
        val left = min(min(corners[0], corners[2]), min(corners[4], corners[6]))
        val top = min(min(corners[1], corners[3]), min(corners[5], corners[7]))
        val right = max(max(corners[0], corners[2]), max(corners[4], corners[6]))
        val bottom = max(max(corners[1], corners[3]), max(corners[5], corners[7]))
        
        val currentW = right - left
        val currentH = bottom - top
        
        // If image is smaller than viewport in any dimension, scale it up
        if (currentW < viewport.width || currentH < viewport.height) {
            val scaleFactor = max(viewport.width / currentW, viewport.height / currentH)
            newMatrix.postScale(scaleFactor, scaleFactor, viewport.center.x, viewport.center.y)
            
            // Re-calculate boundaries after scale
            val newCorners = floatArrayOf(0f, 0f, imgW, 0f, imgW, imgH, 0f, imgH)
            newMatrix.mapPoints(newCorners)
            val nLeft = min(min(newCorners[0], newCorners[2]), min(newCorners[4], newCorners[6]))
            val nTop = min(min(newCorners[1], newCorners[3]), min(newCorners[5], newCorners[7]))
            val nRight = max(max(newCorners[0], newCorners[2]), max(newCorners[4], newCorners[6]))
            val nBottom = max(max(newCorners[1], newCorners[3]), max(newCorners[5], newCorners[7]))
            
            // Clamp translation
            var dx = 0f
            var dy = 0f
            
            if (nLeft > viewport.left) dx = viewport.left - nLeft
            if (nRight < viewport.right) dx = viewport.right - nRight
            if (nTop > viewport.top) dy = viewport.top - nTop
            if (nBottom < viewport.bottom) dy = viewport.bottom - nBottom
            
            newMatrix.postTranslate(dx, dy)
        } else {
            // Just clamp translation
            var dx = 0f
            var dy = 0f
            
            if (left > viewport.left) dx = viewport.left - left
            if (right < viewport.right) dx = viewport.right - right
            if (top > viewport.top) dy = viewport.top - top
            if (bottom < viewport.bottom) dy = viewport.bottom - bottom
            
            newMatrix.postTranslate(dx, dy)
        }
        
        matrix = newMatrix
    }
}
