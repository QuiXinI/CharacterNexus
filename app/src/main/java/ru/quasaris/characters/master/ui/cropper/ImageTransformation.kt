package ru.quasaris.characters.master.ui.cropper

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset

@Immutable
data class ImageTransformation(
    val matrix: android.graphics.Matrix = android.graphics.Matrix()
)
