package ru.quasaris.characternexus.util

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import okio.Path
import ru.quasaris.characternexus.platformFileSystem
import java.awt.Image
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import java.io.File
import java.io.ByteArrayOutputStream
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toComposeImageBitmap
import ru.quasaris.characternexus.backend.cropper.ImageCropState
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.AffineTransform

actual object PlatformUtils {
    actual fun logError(tag: String, message: String, throwable: Throwable?) {
        System.err.println("[$tag] $message")
        throwable?.printStackTrace()
    }

    actual fun setClipboardText(label: String, text: String) {
        val selection = StringSelection(text)
        Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
    }

    actual fun performHapticFeedback() {
        // No haptic feedback on Desktop usually
    }

    actual fun showMessage(message: String) {
        println("MESSAGE: $message")
    }
}

actual object ImageProcessor {
    actual fun generateThumbnail(sourcePath: Path, targetPath: Path) {
        try {
            val img = ImageIO.read(File(sourcePath.toString())) ?: return
            val scaled = img.getScaledInstance(200, 200, Image.SCALE_SMOOTH)
            val buffered = BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB)
            val g = buffered.createGraphics()
            g.drawImage(scaled, 0, 0, null)
            g.dispose()
            val targetFile = File(targetPath.toString())
            targetFile.parentFile?.mkdirs()
            ImageIO.write(buffered, "png", targetFile)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual fun saveCompressedImage(bytes: ByteArray, targetPath: Path, width: Int?, height: Int?) {
        try {
            val targetFile = File(targetPath.toString())
            targetFile.parentFile?.mkdirs()
            if (width == null && height == null) {
                platformFileSystem.write(targetPath) { write(bytes) }
            } else {
                val img = ImageIO.read(bytes.inputStream()) ?: return
                val scaled = img.getScaledInstance(width!!, height!!, Image.SCALE_SMOOTH)
                val buffered = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
                val g = buffered.createGraphics()
                g.drawImage(scaled, 0, 0, null)
                g.dispose()
                ImageIO.write(buffered, "png", targetFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual fun encodeToByteArray(bitmap: ImageBitmap): ByteArray {
        val awtImage = bitmap.toAwtImage()
        val stream = ByteArrayOutputStream()
        ImageIO.write(awtImage, "png", stream)
        return stream.toByteArray()
    }

    actual fun crop(bitmap: ImageBitmap, state: ImageCropState): ImageBitmap {
        val awtImage = bitmap.toAwtImage()
        val targetSize = 1024
        val output = BufferedImage(targetSize, targetSize, BufferedImage.TYPE_INT_ARGB)
        val g2d = output.createGraphics()
        
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        
        val viewport = state.viewportRect
        
        val v = FloatArray(9)
        state.matrix.getValues(v)
        
        // Construct the AffineTransform from the state matrix (row-major)
        // AffineTransform(m00, m10, m01, m11, m02, m12)
        val stateTransform = AffineTransform(
            v[0].toDouble(), v[3].toDouble(), v[1].toDouble(),
            v[4].toDouble(), v[2].toDouble(), v[5].toDouble()
        )
        
        // Calculate crop transform (Scale * Translate)
        val finalScale = targetSize.toDouble() / viewport.width.toDouble()
        val cropTransform = AffineTransform()
        cropTransform.scale(finalScale, finalScale)
        cropTransform.translate(-viewport.left.toDouble(), -viewport.top.toDouble())
        
        // Final = CropTransform * StateTransform
        cropTransform.concatenate(stateTransform)
        
        g2d.setTransform(cropTransform)
        g2d.drawImage(awtImage, 0, 0, null)
        g2d.dispose()
        
        return output.toComposeImageBitmap()
    }
}

actual fun generateUuid(): String = java.util.UUID.randomUUID().toString()
