package ru.quasaris.characternexus.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import ru.quasaris.characternexus.util.PlatformUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
actual fun CommonFileSaver(
    show: Boolean,
    fileName: String,
    fileExtension: String,
    onSaveSelected: (CommonPlatformSaver?) -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri: Uri? ->
        if (uri != null) {
            onSaveSelected(AndroidUriPlatformSaver(uri))
        } else {
            onSaveSelected(null)
        }
    }

    LaunchedEffect(show) {
        if (show) {
            val fullFileName = if (fileName.endsWith(".$fileExtension")) fileName else "$fileName.$fileExtension"
            launcher.launch(fullFileName)
        }
    }
}

class AndroidUriPlatformSaver(private val uri: Uri) : CommonPlatformSaver {
    override suspend fun save(bytes: ByteArray): Boolean = withContext(Dispatchers.IO) {
        try {
            PlatformUtils.androidContext.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(bytes)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
