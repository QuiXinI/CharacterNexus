package ru.quasaris.characternexus.backend

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import ru.quasaris.characternexus.util.log
import ru.quasaris.characternexus.model.ExportFormat

object ImageExporter {

    /**
     * Encodes a bitmap into a ByteArray using the specified format.
     * structured for future Skia (org.jetbrains.skia.Image.encodeToData) migration.
     */
    fun encodeImage(bitmap: Bitmap, format: ExportFormat, quality: Int = 90): ByteArray {
        val stream = java.io.ByteArrayOutputStream()
        val compressFormat = when (format) {
            ExportFormat.WEBP -> if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSLESS
            } else {
                Bitmap.CompressFormat.WEBP
            }
            ExportFormat.PNG -> Bitmap.CompressFormat.PNG
            ExportFormat.JPG -> Bitmap.CompressFormat.JPEG
        }
        bitmap.compress(compressFormat, quality, stream)
        return stream.toByteArray()
    }

    /**
     * Saves the image data to the specified directory (as URI string from SAF) or default Downloads.
     */
    suspend fun saveToDirectory(
        context: Context,
        data: ByteArray,
        fileName: String,
        format: ExportFormat,
        directoryUri: String?
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val extension = format.name.lowercase()
            val fullFileName = if (fileName.endsWith(".$extension")) fileName else "$fileName.$extension"

            if (directoryUri != null) {
                val pickedDir = DocumentFile.fromTreeUri(context, Uri.parse(directoryUri))
                if (pickedDir != null && pickedDir.exists()) {
                    val file = pickedDir.createFile("image/${format.name.lowercase()}", fullFileName)
                    if (file != null) {
                        context.contentResolver.openOutputStream(file.uri)?.use { out ->
                            out.write(data)
                        }
                        return@withContext true
                    }
                }
            }

            // Fallback to default Downloads / Characters Master subfolder
            val baseDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val downloadsDir = File(baseDownloads, "Character Nexus")
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val destFile = File(downloadsDir, fullFileName)
            FileOutputStream(destFile).use { out ->
                out.write(data)
            }
            true
        } catch (e: Exception) {
            e.log()
            false
        }
    }

    fun getDefaultDownloadsPath(): String {
        val baseDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return File(baseDownloads, "Character Nexus").absolutePath
    }
}
