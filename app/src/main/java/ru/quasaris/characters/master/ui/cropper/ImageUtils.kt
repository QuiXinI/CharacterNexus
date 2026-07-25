package ru.quasaris.characters.master.ui.cropper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap

object ImageUtils {
    fun crop(
        bitmap: ImageBitmap,
        state: ImageCropState
    ): ImageBitmap {
        val androidBitmap = bitmap.asAndroidBitmap()
        
        // Target resolution for avatars
        val targetSize = 1024
        val output = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        
        val viewport = state.viewportRect
        
        // Use a copy of the state's matrix
        val matrix = Matrix(state.matrix)
        
        // 1. Shift the matrix so the viewport area starts at (0,0)
        matrix.postTranslate(-viewport.left, -viewport.top)
        
        // 2. Scale the viewport area to the target bitmap size
        val finalScale = targetSize / viewport.width
        matrix.postScale(finalScale, finalScale)
        
        val paint = Paint().apply {
            isFilterBitmap = true
            isAntiAlias = true
        }
        canvas.drawBitmap(androidBitmap, matrix, paint)
        
        return output.asImageBitmap()
    }
}
