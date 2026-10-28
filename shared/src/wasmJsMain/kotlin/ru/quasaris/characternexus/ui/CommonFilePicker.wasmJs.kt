package ru.quasaris.characternexus.ui

import androidx.compose.runtime.Composable

@Composable
actual fun CommonFilePicker(
    show: Boolean,
    fileExtensions: List<String>,
    onFileSelected: (CommonPlatformFile?) -> Unit
) {
}

@Composable
actual fun CommonFileSaver(
    show: Boolean,
    fileName: String,
    fileExtension: String,
    onSaveSelected: (CommonPlatformSaver?) -> Unit
) {
}
