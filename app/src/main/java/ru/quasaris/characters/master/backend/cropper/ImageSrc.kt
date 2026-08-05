package ru.quasaris.characters.master.backend.cropper

import androidx.compose.ui.graphics.ImageBitmap

sealed interface ImageSrc {
    data class Bitmap(val imageBitmap: ImageBitmap) : ImageSrc
}
