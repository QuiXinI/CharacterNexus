package ru.quasaris.characternexus.util

import okio.Path
import ru.quasaris.characternexus.platformFileSystem
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import ru.quasaris.characternexus.backend.cropper.ImageCropState
import org.jetbrains.skia.*

actual object PlatformUtils {
    actual fun logError(tag: String, message: String, throwable: Throwable?) {
        System.err.println("[$tag] $message")
        throwable?.printStackTrace()
    }

    actual fun setClipboardText(label: String, text: String) {
        val selection = StringSelection(text)
        Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
    }

    actual fun performHapticFeedback(type: HapticType) {
        // No haptic feedback on Desktop usually
    }

    actual fun showMessage(message: String) {
        println("MESSAGE: $message")
    }
}

actual object ImageProcessor {
    actual fun generateThumbnail(sourcePath: Path, targetPath: Path) {
        try {
            val bytes = platformFileSystem.read(sourcePath) { readByteArray() }
            val image = Image.makeFromEncoded(bytes)
            
            val targetSize = 200
            val surface = Surface.makeRasterN32Premul(targetSize, targetSize)
            val canvas = surface.canvas
            canvas.drawImageRect(image, Rect.makeWH(targetSize.toFloat(), targetSize.toFloat()))
            
            val scaledImage = surface.makeImageSnapshot()
            val data = scaledImage.encodeToData(EncodedImageFormat.WEBP, 80)
            data?.let {
                platformFileSystem.write(targetPath) { write(it.bytes) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual fun saveCompressedImage(bytes: ByteArray, targetPath: Path, width: Int?, height: Int?) {
        try {
            val image = Image.makeFromEncoded(bytes)
            val data = if (width == null && height == null) {
                image.encodeToData(EncodedImageFormat.WEBP, 90)
            } else {
                val surface = Surface.makeRasterN32Premul(width!!, height!!)
                val canvas = surface.canvas
                canvas.drawImageRect(image, Rect.makeWH(width.toFloat(), height.toFloat()))
                val scaledImage = surface.makeImageSnapshot()
                scaledImage.encodeToData(EncodedImageFormat.WEBP, 80)
            }
            
            data?.let {
                val targetFile = File(targetPath.toString())
                targetFile.parentFile?.mkdirs()
                platformFileSystem.write(targetPath) { write(it.bytes) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual fun encodeToByteArray(bitmap: ImageBitmap): ByteArray {
        val skiaImage = Image.makeFromBitmap(bitmap.asSkiaBitmap())
        return skiaImage.encodeToData(EncodedImageFormat.WEBP, 90)?.bytes ?: byteArrayOf()
    }

    actual fun crop(bitmap: ImageBitmap, state: ImageCropState): ImageBitmap {
        val skiaBitmap = bitmap.asSkiaBitmap()
        val targetSize = 1024
        
        val surface = Surface.makeRasterN32Premul(targetSize, targetSize)
        val canvas = surface.canvas
        
        val viewport = state.viewportRect
        
        val v = FloatArray(9)
        state.matrix.getValues(v)
        
        // Skia matrix is row-major: [scaleX, skewX, transX, skewY, scaleY, transY, persp0, persp1, persp2]
        // state.matrix (Compose) is also usually row-major in getValues? 
        // Actually Compose Matrix is 4x4. ImageCropState.matrix is likely a Compose Matrix.
        // Let's check how state.matrix values mapping works.
        
        val skiaMatrix = Matrix33(
            v[0], v[1], v[2],
            v[3], v[4], v[5],
            v[6], v[7], v[8]
        )
        
        val finalScale = targetSize.toFloat() / viewport.width
        
        canvas.scale(finalScale, finalScale)
        canvas.translate(-viewport.left, -viewport.top)
        canvas.concat(skiaMatrix)
        
        canvas.drawImage(Image.makeFromBitmap(skiaBitmap), 0f, 0f)
        
        return surface.makeImageSnapshot().toComposeImageBitmap()
    }
}

actual fun generateUuid(): String = java.util.UUID.randomUUID().toString()
