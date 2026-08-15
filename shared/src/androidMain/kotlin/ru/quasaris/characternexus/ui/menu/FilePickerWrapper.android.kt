package ru.quasaris.characternexus.ui.menu

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.darkrockstudios.libraries.mpfilepicker.FilePicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.quasaris.characternexus.util.PlatformUtils

@Composable
actual fun FilePickerWrapper(
    show: Boolean,
    fileExtensions: List<String>,
    onFileSelected: (ByteArray?) -> Unit
) {
    val scope = rememberCoroutineScope()
    
    // On Android, we use */* to ensure custom extensions like .charbook and .lsskiller 
    // are visible and selectable in the system picker, as they don't have standard MIME types.
    
    FilePicker(show = show, fileExtensions = listOf("*/*")) { platformFile ->
        if (platformFile == null) {
            onFileSelected(null)
        } else {
            scope.launch {
                val bytes = withContext(Dispatchers.IO) {
                    try {
                        val path = platformFile.path
                        if (path.startsWith("content://")) {
                            val uri = Uri.parse(path)
                            PlatformUtils.androidContext.contentResolver.openInputStream(uri)?.use { 
                                it.readBytes() 
                            }
                        } else {
                            platformFile.getFileByteArray()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                onFileSelected(bytes)
            }
        }
    }
}
