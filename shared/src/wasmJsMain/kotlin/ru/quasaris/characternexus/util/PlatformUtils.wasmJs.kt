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
}

actual fun generateUuid(): String = ""
