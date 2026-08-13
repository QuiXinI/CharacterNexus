package ru.quasaris.characternexus.ui.menu

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.darkrockstudios.libraries.mpfilepicker.FilePicker
import kotlinx.coroutines.launch

import androidx.compose.runtime.rememberCoroutineScope
import com.darkrockstudios.libraries.mpfilepicker.FilePicker
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import android.net.Uri

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
            scope.launch(Dispatchers.IO) {
                val bytes = try {
                    val path = platformFile.path
                    if (path.startsWith("content://")) {
                        val uri = Uri.parse(path)
                        ru.quasaris.characternexus.util.PlatformUtils.androidContext.contentResolver.openInputStream(uri)?.use { 
                            it.readBytes() 
                        }
                    } else {
                        platformFile.getFileByteArray()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
                onFileSelected(bytes)
            }
        }
    }
}
