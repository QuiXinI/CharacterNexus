package ru.quasaris.characters.master.ui.cropper

import androidx.compose.ui.graphics.ImageBitmap

sealed interface ImageSrc {
    data class Bitmap(val imageBitmap: ImageBitmap) : ImageSrc
}
