package ru.quasaris.characternexus.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
actual fun CommonFileSaver(
    show: Boolean,
    fileName: String,
    fileExtension: String,
    onSaveSelected: (CommonPlatformSaver?) -> Unit
) {
    LaunchedEffect(show) {
        if (show) {
            val dialog = FileDialog(null as Frame?, "Сохранить файл", FileDialog.SAVE)
            dialog.file = if (fileName.endsWith(".$fileExtension")) fileName else "$fileName.$fileExtension"
            dialog.isVisible = true
            
            val selectedFile = dialog.file
            val selectedDirectory = dialog.directory
            
            if (selectedFile != null && selectedDirectory != null) {
                val fullPath = File(selectedDirectory, selectedFile).absolutePath
                onSaveSelected(JVMPathPlatformSaver(fullPath))
            } else {
                onSaveSelected(null)
            }
        }
    }
}

class JVMPathPlatformSaver(private val path: String) : CommonPlatformSaver {
    override suspend fun save(bytes: ByteArray): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            file.writeBytes(bytes)
            true
        } catch (e: Exception) {
            false
        }
    }
}
