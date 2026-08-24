package ru.quasaris.characternexus.ui

import androidx.compose.runtime.Composable

@Composable
expect fun CommonFileSaver(
    show: Boolean,
    fileName: String,
    fileExtension: String,
    onSaveSelected: (CommonPlatformSaver?) -> Unit
)

interface CommonPlatformSaver {
    suspend fun save(bytes: ByteArray): Boolean
}
