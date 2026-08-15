package ru.quasaris.characternexus.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
}

@Composable
actual fun CommonFilePicker(
    show: Boolean,
    fileExtensions: List<String>,
    onFileSelected: (CommonPlatformFile?) -> Unit
) {
}

actual fun Modifier.outerShadow(
    shape: Shape,
    color: Color,
    blur: Dp,
    offsetY: Dp,
    offsetX: Dp
): Modifier = this
