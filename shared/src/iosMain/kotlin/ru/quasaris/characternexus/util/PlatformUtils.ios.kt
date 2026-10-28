package ru.quasaris.characternexus.util

import okio.Path
import platform.Foundation.NSUUID

actual object PlatformUtils {
    actual val isAndroid: Boolean = false

    actual fun logError(tag: String, message: String, throwable: Throwable?) {
        println("[$tag] $message")
        throwable?.printStackTrace()
    }

    actual fun setClipboardText(label: String, text: String) {
    }

    actual fun getClipboardContent(): ClipboardContent? {
        val pasteboard = platform.UIKit.UIPasteboard.generalPasteboard
        val text = pasteboard.string ?: ""
        val html = pasteboard.valueForPasteboardType("public.html") as? String
        return ClipboardContent(plainText = text, htmlText = html)
    }

    actual fun performHapticFeedback(type: HapticType) {
        when (type) {
            HapticType.CLICK -> {
                val generator = platform.UIKit.UIImpactFeedbackGenerator(platform.UIKit.UIImpactFeedbackStyle.UIImpactFeedbackStyleLight)
                generator.impactOccurred()
            }
            HapticType.LONG_PRESS -> {
                val generator = platform.UIKit.UIImpactFeedbackGenerator(platform.UIKit.UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium)
                generator.impactOccurred()
            }
            HapticType.SUCCESS -> {
                val generator = platform.UIKit.UINotificationFeedbackGenerator()
                generator.notificationOccurred(platform.UIKit.UINotificationFeedbackType.UINotificationFeedbackTypeSuccess)
            }
            HapticType.ERROR -> {
                val generator = platform.UIKit.UINotificationFeedbackGenerator()
                generator.notificationOccurred(platform.UIKit.UINotificationFeedbackType.UINotificationFeedbackTypeError)
            }
        }
    }

    actual fun showMessage(message: String) {
        println("MSG: $message")
    }
}

actual object Logger {
    actual fun d(tag: String, message: String) { println("D/$tag: $message") }
    actual fun e(tag: String, message: String, throwable: Throwable?) { 
        println("E/$tag: $message")
        throwable?.printStackTrace()
    }
    actual fun i(tag: String, message: String) { println("I/$tag: $message") }
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

actual fun generateUuid(): String = NSUUID().UUIDString()
