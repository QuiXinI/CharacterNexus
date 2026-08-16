package ru.quasaris.characternexus.backend.cropper

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
actual fun AvatarCropperWindow(
    imageBitmap: ImageBitmap,
    onCrop: (ImageBitmap) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        AvatarCropperContent(
            imageBitmap = imageBitmap,
            onCrop = onCrop,
            onDismiss = onDismiss
        )
    }
}
