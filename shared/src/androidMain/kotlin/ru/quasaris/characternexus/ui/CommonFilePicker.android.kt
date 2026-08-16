package ru.quasaris.characternexus.ui

import androidx.compose.runtime.Composable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import ru.quasaris.characternexus.util.PlatformUtils
import androidx.compose.runtime.LaunchedEffect

@Composable
actual fun CommonFilePicker(
    show: Boolean,
    fileExtensions: List<String>,
    onFileSelected: (CommonPlatformFile?) -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onFileSelected(AndroidUriPlatformFile(uri))
        } else {
            onFileSelected(null)
        }
    }

    LaunchedEffect(show) {
        if (show) {
            // Revert to legacy behavior: GetContent with */*
            launcher.launch("*/*")
        }
    }
}

class AndroidUriPlatformFile(private val uri: Uri) : CommonPlatformFile {
    override val path: String get() = uri.toString()
    override suspend fun readBytes(): ByteArray {
        return PlatformUtils.androidContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: ByteArray(0)
    }
}
