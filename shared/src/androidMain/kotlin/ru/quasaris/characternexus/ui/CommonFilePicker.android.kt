package ru.quasaris.characternexus.ui

import androidx.compose.runtime.Composable
import com.darkrockstudios.libraries.mpfilepicker.FilePicker
import com.darkrockstudios.libraries.mpfilepicker.MPFile

@Composable
actual fun CommonFilePicker(
    show: Boolean,
    fileExtensions: List<String>,
    onFileSelected: (CommonPlatformFile?) -> Unit
) {
    FilePicker(show = show, fileExtensions = fileExtensions) { file ->
        onFileSelected(file?.let { AndroidPlatformFile(it) })
    }
}

class AndroidPlatformFile(private val platformFile: MPFile<*>) : CommonPlatformFile {
    override val path: String get() = platformFile.path
    override suspend fun readBytes(): ByteArray = platformFile.getFileByteArray()
}
