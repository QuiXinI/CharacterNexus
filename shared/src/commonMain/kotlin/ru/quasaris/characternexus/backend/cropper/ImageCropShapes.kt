package ru.quasaris.characternexus.backend.cropper

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

interface ImageCropShape {
    val shape: Shape
    val label: String
}

object CircleImgCropShape : ImageCropShape {
    override val label: String = "Circle"
    override val shape: Shape = object : Shape {
        override fun createOutline(
            size: Size,
            layoutDirection: LayoutDirection,
            density: Density
        ): Outline {
            val path = Path().apply {
                addOval(Rect(0f, 0f, size.width, size.height))
            }
            return Outline.Generic(path)
        }
    }
}

object RectImgCropShape : ImageCropShape {
    override val label: String = "Rectangle"
    override val shape: Shape = object : Shape {
        override fun createOutline(
            size: Size,
            layoutDirection: LayoutDirection,
            density: Density
        ): Outline {
            return Outline.Rectangle(Rect(0f, 0f, size.width, size.height))
        }
    }
}
