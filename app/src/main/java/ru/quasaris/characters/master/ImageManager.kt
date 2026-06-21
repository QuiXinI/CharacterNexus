package ru.quasaris.characters.master

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageManager {
    private const val PORTRAIT_DIR = "portraits"
    private const val THUMB_DIR = "thumbnails"

    suspend fun processAndSaveImage(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext ""
        val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return@withContext ""

        // 1. Full-Resolution WebP
        val portraitDir = File(context.filesDir, PORTRAIT_DIR).apply { if (!exists()) mkdirs() }
        val portraitFile = File(portraitDir, "$id.webp")
        FileOutputStream(portraitFile).use { out ->
            originalBitmap.compress(Bitmap.CompressFormat.WEBP, 80, out)
        }

        // 2. UI Thumbnail (200x200px)
        val thumbDir = File(context.cacheDir, THUMB_DIR).apply { if (!exists()) mkdirs() }
        val thumbFile = File(thumbDir, "$id.webp")
        val thumbBitmap = Bitmap.createScaledBitmap(originalBitmap, 200, 200, true)
        FileOutputStream(thumbFile).use { out ->
            thumbBitmap.compress(Bitmap.CompressFormat.WEBP, 80, out)
        }

        return@withContext id
    }

    fun getPortraitFile(context: Context, id: String): File {
        return File(File(context.filesDir, PORTRAIT_DIR), "$id.webp")
    }

    fun getThumbnailFile(context: Context, id: String): File {
        return File(File(context.cacheDir, THUMB_DIR), "$id.webp")
    }
    
    suspend fun generateThumbnailFromPortrait(context: Context, id: String) = withContext(Dispatchers.IO) {
        val portraitFile = getPortraitFile(context, id)
        if (!portraitFile.exists()) return@withContext
        
        val originalBitmap = BitmapFactory.decodeFile(portraitFile.absolutePath) ?: return@withContext
        val thumbDir = File(context.cacheDir, THUMB_DIR).apply { if (!exists()) mkdirs() }
        val thumbFile = File(thumbDir, "$id.webp")
        val thumbBitmap = Bitmap.createScaledBitmap(originalBitmap, 200, 200, true)
        FileOutputStream(thumbFile).use { out ->
            thumbBitmap.compress(Bitmap.CompressFormat.WEBP, 80, out)
        }
    }
}
