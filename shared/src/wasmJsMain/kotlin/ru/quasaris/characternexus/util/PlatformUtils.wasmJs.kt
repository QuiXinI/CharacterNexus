package ru.quasaris.characternexus.util

import okio.Path
import androidx.compose.ui.graphics.ImageBitmap
import ru.quasaris.characternexus.backend.cropper.ImageCropState

actual object PlatformUtils {
    actual val isAndroid: Boolean = false

    actual fun logError(tag: String, message: String, throwable: Throwable?) {
    }

    actual fun setClipboardText(label: String, text: String) {
    }

    actual fun performHapticFeedback(type: HapticType) {
    }

    actual fun showMessage(message: String) {
    }

    actual fun getClipboardContent(): ClipboardContent? {
        return null
    }
}

actual object ImageProcessor {
    actual fun generateThumbnail(sourcePath: Path, targetPath: Path) {
    }

    actual fun saveCompressedImage(bytes: ByteArray, targetPath: Path, width: Int?, height: Int?) {
    }

    actual suspend fun encodeToByteArray(bitmap: ImageBitmap): ByteArray {
        return ByteArray(0)
    }

    actual fun crop(bitmap: ImageBitmap, state: ImageCropState): ImageBitmap {
        return bitmap
    }
}

actual fun generateUuid(): String = ""
