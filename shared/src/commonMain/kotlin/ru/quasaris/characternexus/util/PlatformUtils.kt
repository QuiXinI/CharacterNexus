package ru.quasaris.characternexus.util

import okio.Path

expect object PlatformUtils {
    fun logError(tag: String, message: String, throwable: Throwable? = null)
    fun setClipboardText(label: String, text: String)
    fun performHapticFeedback()
    fun showMessage(message: String)
}

expect object ImageProcessor {
    fun generateThumbnail(sourcePath: Path, targetPath: Path)
    fun saveCompressedImage(bytes: ByteArray, targetPath: Path, width: Int? = null, height: Int? = null)
    fun encodeToByteArray(bitmap: androidx.compose.ui.graphics.ImageBitmap): ByteArray
    fun crop(bitmap: androidx.compose.ui.graphics.ImageBitmap, state: ru.quasaris.characternexus.backend.cropper.ImageCropState): androidx.compose.ui.graphics.ImageBitmap
}
