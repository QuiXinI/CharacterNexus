package ru.quasaris.characters.master.backend.cropper

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

fun Modifier.cropperInteractions(
    state: ImageCropState,
    enabled: Boolean = true
): Modifier = if (!enabled) this else pointerInput(state) {
    detectTransformGestures { _, pan, zoom, _ ->
        state.onTransform(pan, zoom)
    }
}
