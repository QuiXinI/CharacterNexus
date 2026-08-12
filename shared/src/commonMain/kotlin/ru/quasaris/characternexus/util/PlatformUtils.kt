package ru.quasaris.characternexus.util

import okio.Path

expect object PlatformUtils {
    fun logError(tag: String, message: String, throwable: Throwable? = null)
    fun setClipboardText(label: String, text: String)
    fun performHapticFeedback()
}

expect object ImageProcessor {
    fun generateThumbnail(sourcePath: Path, targetPath: Path)
    fun saveCompressedImage(bytes: ByteArray, targetPath: Path, width: Int? = null, height: Int? = null)
}
