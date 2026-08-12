package ru.quasaris.characternexus.ui.menu

import androidx.compose.runtime.Composable

@Composable
expect fun FilePickerWrapper(
    show: Boolean,
    fileExtensions: List<String>,
    onFileSelected: (ByteArray?) -> Unit
)
