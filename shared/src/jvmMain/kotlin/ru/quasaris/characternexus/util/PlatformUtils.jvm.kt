package ru.quasaris.characternexus.util

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import okio.Path
import ru.quasaris.characternexus.platformFileSystem
import java.awt.Image
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import java.io.File

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
}

actual object ImageProcessor {
    actual fun generateThumbnail(sourcePath: Path, targetPath: Path) {
        val img = ImageIO.read(File(sourcePath.toString())) ?: return
        val scaled = img.getScaledInstance(200, 200, Image.SCALE_SMOOTH)
        val buffered = BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB)
        val g = buffered.createGraphics()
        g.drawImage(scaled, 0, 0, null)
        g.dispose()
        val targetFile = File(targetPath.toString())
        targetFile.parentFile?.mkdirs()
        ImageIO.write(buffered, "png", targetFile)
    }

    actual fun saveCompressedImage(bytes: ByteArray, targetPath: Path, width: Int?, height: Int?) {
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
    }
}

actual fun generateUuid(): String = java.util.UUID.randomUUID().toString()
