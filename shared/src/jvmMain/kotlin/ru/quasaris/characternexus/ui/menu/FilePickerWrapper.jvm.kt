package ru.quasaris.characternexus.ui.menu

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.darkrockstudios.libraries.mpfilepicker.FilePicker
import kotlinx.coroutines.launch

@Composable
actual fun FilePickerWrapper(
    show: Boolean,
    fileExtensions: List<String>,
    onFileSelected: (ByteArray?) -> Unit
) {
    val scope = rememberCoroutineScope()
    FilePicker(show = show, fileExtensions = fileExtensions) { platformFile ->
        if (platformFile == null) {
            onFileSelected(null)
        } else {
            scope.launch {
                onFileSelected(platformFile.getFileByteArray())
            }
        }
    }
}
