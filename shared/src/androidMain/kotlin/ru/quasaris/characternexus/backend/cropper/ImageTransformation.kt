package ru.quasaris.characternexus.backend.cropper

import android.graphics.Matrix
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset

@Immutable
data class ImageTransformation(
    val matrix: Matrix = Matrix()
)
