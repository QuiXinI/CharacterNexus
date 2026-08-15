package ru.quasaris.characternexus.backend.cropper

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

@Composable
actual fun AvatarCropperWindow(
    imageBitmap: ImageBitmap,
    onCrop: (ImageBitmap) -> Unit,
    onDismiss: () -> Unit
) {
    // No-op implementation for now
}
