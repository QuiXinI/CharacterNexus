package ru.quasaris.characters.master.backend.cropper

import androidx.compose.runtime.Immutable

@Immutable
data class ImageAspectRatio(val x: Int, val y: Int) {
    val ratio: Float
        get() = if (y == 0) 1f else x.toFloat() / y.toFloat()

    companion object {
        val Free = ImageAspectRatio(0, 0)
        val Square = ImageAspectRatio(1, 1)
    }
}
