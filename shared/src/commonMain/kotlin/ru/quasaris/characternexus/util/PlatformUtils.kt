package ru.quasaris.characternexus.util

import okio.Path
import androidx.compose.ui.graphics.ImageBitmap
import ru.quasaris.characternexus.backend.cropper.ImageCropState

enum class HapticType {
    CLICK, LONG_PRESS, SUCCESS, ERROR
}

expect object PlatformUtils {
    fun logError(tag: String, message: String, throwable: Throwable? = null)
    fun setClipboardText(label: String, text: String)
    fun performHapticFeedback(type: HapticType = HapticType.CLICK)
    fun showMessage(message: String)
}

expect object ImageProcessor {
    fun generateThumbnail(sourcePath: Path, targetPath: Path)
    fun saveCompressedImage(bytes: ByteArray, targetPath: Path, width: Int? = null, height: Int? = null)
    suspend fun encodeToByteArray(bitmap: ImageBitmap): ByteArray
    fun crop(bitmap: ImageBitmap, state: ImageCropState): ImageBitmap
}
