package ru.quasaris.characternexus.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import okio.Path
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Matrix as AndroidMatrix
import ru.quasaris.characternexus.platformFileSystem
import java.io.FileOutputStream
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream
import ru.quasaris.characternexus.backend.cropper.ImageCropState

actual object PlatformUtils {
    lateinit var androidContext: Context

    actual fun logError(tag: String, message: String, throwable: Throwable?) {
        Log.e(tag, message, throwable)
    }

    actual fun setClipboardText(label: String, text: String) {
        val clipboard = androidContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }

    actual fun performHapticFeedback() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = androidContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            androidContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
        }
    }

    actual fun showMessage(message: String) {
        android.widget.Toast.makeText(androidContext, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}

actual object ImageProcessor {
    actual fun generateThumbnail(sourcePath: Path, targetPath: Path) {
        val bitmap = BitmapFactory.decodeFile(sourcePath.toString()) ?: return
        val thumb = Bitmap.createScaledBitmap(bitmap, 200, 200, true)
        val file = java.io.File(targetPath.toString())
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { out ->
            @Suppress("DEPRECATION")
            thumb.compress(Bitmap.CompressFormat.WEBP, 80, out)
        }
    }

    actual fun saveCompressedImage(bytes: ByteArray, targetPath: Path, width: Int?, height: Int?) {
        val file = java.io.File(targetPath.toString())
        file.parentFile?.mkdirs()
        if (width == null && height == null) {
            platformFileSystem.write(targetPath) { write(bytes) }
        } else {
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return
            val scaled = Bitmap.createScaledBitmap(bitmap, width!!, height!!, true)
            FileOutputStream(file).use { out ->
                @Suppress("DEPRECATION")
                scaled.compress(Bitmap.CompressFormat.WEBP, 80, out)
            }
        }
    }

    actual fun encodeToByteArray(bitmap: ImageBitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        @Suppress("DEPRECATION")
        bitmap.asAndroidBitmap().compress(Bitmap.CompressFormat.WEBP, 80, stream)
        return stream.toByteArray()
    }

    actual fun crop(bitmap: ImageBitmap, state: ImageCropState): ImageBitmap {
        val androidBitmap = bitmap.asAndroidBitmap()
        val targetSize = 1024
        val output = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val viewport = state.viewportRect
        
        val androidMatrix = AndroidMatrix()
        val v = FloatArray(9)
        state.matrix.getValues(v)
        androidMatrix.setValues(v)
        
        // Match legacy Matrix application order:
        // 1. Shift by -viewport
        // 2. Scale by finalScale
        val finalScale = targetSize / viewport.width
        
        val cropMatrix = AndroidMatrix()
        cropMatrix.postTranslate(-viewport.left, -viewport.top)
        cropMatrix.postScale(finalScale, finalScale)
        
        // Final = CropMatrix * AndroidMatrix
        cropMatrix.preConcat(androidMatrix)
        
        val paint = Paint().apply {
            isFilterBitmap = true
            isAntiAlias = true
        }
        canvas.drawBitmap(androidBitmap, cropMatrix, paint)
        
        return output.asImageBitmap()
    }
}

actual fun generateUuid(): String = java.util.UUID.randomUUID().toString()
