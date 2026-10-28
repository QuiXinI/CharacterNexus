package ru.quasaris.characternexus.ui.menu

import androidx.compose.runtime.Composable

@Composable
actual fun FilePickerWrapper(
    show: Boolean,
    fileExtensions: List<String>,
    onFileSelected: (ByteArray?) -> Unit
) {
}
