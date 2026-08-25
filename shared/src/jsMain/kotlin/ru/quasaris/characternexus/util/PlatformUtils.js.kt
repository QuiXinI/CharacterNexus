package ru.quasaris.characternexus.util

import okio.Path

actual object PlatformUtils {
    actual fun logError(tag: String, message: String, throwable: Throwable?) {
    }

    actual fun setClipboardText(label: String, text: String) {
    }

    actual fun performHapticFeedback(type: HapticType) {
    }
}

actual object ImageProcessor {
    actual fun generateThumbnail(sourcePath: Path, targetPath: Path) {
    }

    actual fun saveCompressedImage(bytes: ByteArray, targetPath: Path, width: Int?, height: Int?) {
    }

    actual suspend fun encodeToByteArray(bitmap: androidx.compose.ui.graphics.ImageBitmap): ByteArray {
        return ByteArray(0)
    }

    actual fun crop(bitmap: androidx.compose.ui.graphics.ImageBitmap, state: ru.quasaris.characternexus.backend.cropper.ImageCropState): androidx.compose.ui.graphics.ImageBitmap {
        return bitmap
    }
}

actual fun generateUuid(): String = ""
