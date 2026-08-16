package ru.quasaris.characternexus.backend.cropper

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import ru.quasaris.characternexus.util.ImageProcessor

sealed interface ImageCropResult {
    data object Idle : ImageCropResult
    data class Success(val bitmap: ImageBitmap) : ImageCropResult
    data object Cancelled : ImageCropResult
    data class Error(val exception: Throwable) : ImageCropResult
}

interface ImageCropper {
    val cropState: ImageCropState?
    suspend fun cropImage(bmp: ImageBitmap): ImageCropResult
}

class ImageCropperImpl(
    initialShape: ImageCropShape = CircleImgCropShape,
    initialAspectRatio: ImageAspectRatio = ImageAspectRatio.Square
) : ImageCropper {
    override var cropState: ImageCropState? by mutableStateOf(ImageCropStateImpl(initialShape, initialAspectRatio))
    
    override suspend fun cropImage(bmp: ImageBitmap): ImageCropResult {
        val state = cropState ?: return ImageCropResult.Cancelled
        return try {
            val cropped = ImageProcessor.crop(bmp, state)
            ImageCropResult.Success(cropped)
        } catch (e: Exception) {
            ImageCropResult.Error(e)
        }
    }
}

@Composable
fun rememberImageCropper(): ImageCropper {
    return remember { ImageCropperImpl() }
}
