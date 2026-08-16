package ru.quasaris.characternexus.ui

import androidx.compose.runtime.Composable
import com.darkrockstudios.libraries.mpfilepicker.FilePicker
import com.darkrockstudios.libraries.mpfilepicker.MPFile
import ru.quasaris.characternexus.util.Logger

@Composable
actual fun CommonFilePicker(
    show: Boolean,
    fileExtensions: List<String>,
    onFileSelected: (CommonPlatformFile?) -> Unit
) {
    if (show) {
        Logger.d("CommonFilePicker", "Showing picker for extensions: $fileExtensions")
    }
    
    FilePicker(show = show, fileExtensions = fileExtensions) { selectedFile ->
        if (selectedFile != null) {
            Logger.d("CommonFilePicker", "File selected: ${selectedFile.path}")
            onFileSelected(JVMPlatformFile(selectedFile))
        } else {
            if (show) Logger.d("CommonFilePicker", "Picker cancelled or returned null")
            onFileSelected(null)
        }
    }
}

class JVMPlatformFile(private val platformFile: MPFile<*>) : CommonPlatformFile {
    override val path: String get() = platformFile.path
    override suspend fun readBytes(): ByteArray {
        val file = java.io.File(path)
        if (!file.exists()) {
            Logger.e("CommonFilePicker", "File does not exist: $path")
            return ByteArray(0)
        }
        Logger.d("CommonFilePicker", "Reading file: $path, size: ${file.length()}")
        return platformFile.getFileByteArray()
    }
}
