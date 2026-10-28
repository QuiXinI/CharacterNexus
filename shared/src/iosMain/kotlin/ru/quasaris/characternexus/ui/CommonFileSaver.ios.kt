package ru.quasaris.characternexus.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Path.Companion.toPath
import ru.quasaris.characternexus.getAppDataDir
import ru.quasaris.characternexus.platformFileSystem

@Composable
actual fun CommonFileSaver(
    show: Boolean,
    fileName: String,
    fileExtension: String,
    onSaveSelected: (CommonPlatformSaver?) -> Unit,
) {
    LaunchedEffect(show) {
        if (show) {
            val fullFileName = if (fileName.endsWith(".$fileExtension")) fileName else "$fileName.$fileExtension"
            val targetPath = getAppDataDir() / fullFileName
            onSaveSelected(IOSPathPlatformSaver(targetPath.toString()))
        }
    }
}

class IOSPathPlatformSaver(private val path: String) : CommonPlatformSaver {
    override suspend fun save(bytes: ByteArray): Boolean = withContext(Dispatchers.Default) {
        try {
            val targetPath = path.toPath()
            platformFileSystem.write(targetPath) {
                write(bytes)
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
