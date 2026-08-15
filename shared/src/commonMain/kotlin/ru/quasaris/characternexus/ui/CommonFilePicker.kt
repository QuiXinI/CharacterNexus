package ru.quasaris.characternexus.ui

import androidx.compose.runtime.Composable

@Composable
expect fun CommonFilePicker(
    show: Boolean,
    fileExtensions: List<String>,
    onFileSelected: (CommonPlatformFile?) -> Unit
)

interface CommonPlatformFile {
    val path: String
    suspend fun readBytes(): ByteArray
}
