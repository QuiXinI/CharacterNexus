package ru.quasaris.characters.master.backend

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
    private const val ORIGINAL_DIR = "originals"
    private const val PORTRAIT_DIR = "portraits"
    private const val THUMB_DIR = "thumbnails"
    private const val CHARACTERS_DIR = "Characters"

    fun getCharacterDir(context: Context, uuid: String): File {
        return File(File(context.filesDir, CHARACTERS_DIR), uuid).apply { if (!exists()) mkdirs() }
    }

    /**
     * Returns the portrait file for a character.
     * Tries new character-specific folder first, then legacy global folder.
     */
    fun getPortraitFile(context: Context, imageIdOrUuid: String, characterUuid: String? = null): File {
        // 1. Try new structure: Characters/[uuid]/portrait.webp
        if (!characterUuid.isNullOrBlank()) {
            val charDirFile = File(File(context.filesDir, CHARACTERS_DIR), characterUuid)
            val newFile = File(charDirFile, "portrait.webp")
            if (newFile.exists()) return newFile
        }

        // 2. Try using imageIdOrUuid as folder name (in case it IS the UUID or a folder-based ID)
        if (imageIdOrUuid.isNotBlank()) {
            val directFolderFile = File(File(File(context.filesDir, CHARACTERS_DIR), imageIdOrUuid), "portrait.webp")
            if (directFolderFile.exists()) return directFolderFile
        }
        
        // 3. Fallback to legacy global folder
        return File(File(context.filesDir, PORTRAIT_DIR), "$imageIdOrUuid.webp")
    }

    fun getOriginalFile(context: Context, imageIdOrUuid: String, characterUuid: String? = null): File {
        if (!characterUuid.isNullOrBlank()) {
            val charDirFile = File(File(context.filesDir, CHARACTERS_DIR), characterUuid)
            val newFile = File(charDirFile, "original.webp")
            if (newFile.exists()) return newFile
        }

        if (imageIdOrUuid.isNotBlank()) {
            val directFolderFile = File(File(File(context.filesDir, CHARACTERS_DIR), imageIdOrUuid), "original.webp")
            if (directFolderFile.exists()) return directFolderFile
        }
        
        return File(File(context.filesDir, ORIGINAL_DIR), "$imageIdOrUuid.webp")
    }

    fun getThumbnailFile(context: Context, imageIdOrUuid: String, characterUuid: String? = null): File {
        if (!characterUuid.isNullOrBlank()) {
            val charDirFile = File(File(context.filesDir, CHARACTERS_DIR), characterUuid)
            val newFile = File(charDirFile, "thumbnail.webp")
            if (newFile.exists()) return newFile
        }

        if (imageIdOrUuid.isNotBlank()) {
            val directFolderFile = File(File(File(context.filesDir, CHARACTERS_DIR), imageIdOrUuid), "thumbnail.webp")
            if (directFolderFile.exists()) return directFolderFile
        }
        
        return File(File(context.cacheDir, THUMB_DIR), "$imageIdOrUuid.webp")
    }

    /**
     * Just saves the original image from Uri and returns a new ID.
     * For new system, we still return a UUID and later move it to character folder.
     */
    suspend fun saveOriginal(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
        val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return@withContext null

        val originalDir = File(context.filesDir, ORIGINAL_DIR).apply { if (!exists()) mkdirs() }
        val originalFile = File(originalDir, "$id.webp")
        FileOutputStream(originalFile).use { out ->
            originalBitmap.compress(Bitmap.CompressFormat.WEBP, 80, out)
        }
        id
    }

    suspend fun saveBitmapAsOriginal(context: Context, bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val originalDir = File(context.filesDir, ORIGINAL_DIR).apply { if (!exists()) mkdirs() }
        val originalFile = File(originalDir, "$id.webp")
        FileOutputStream(originalFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.WEBP, 80, out)
        }
        id
    }

    /**
     * Saves the cropped bitmap as portrait and generates a thumbnail.
     */
    suspend fun saveCropped(context: Context, id: String, croppedBitmap: Bitmap) = withContext(Dispatchers.IO) {
        // 1. Full-Resolution Portrait (Cropped)
        val portraitDir = File(context.filesDir, PORTRAIT_DIR).apply { if (!exists()) mkdirs() }
        val portraitFile = File(portraitDir, "$id.webp")
        FileOutputStream(portraitFile).use { out ->
            croppedBitmap.compress(Bitmap.CompressFormat.WEBP, 80, out)
        }

        // 2. UI Thumbnail (200x200px)
        val thumbDir = File(context.cacheDir, THUMB_DIR).apply { if (!exists()) mkdirs() }
        val thumbFile = File(thumbDir, "$id.webp")
        val thumbBitmap = Bitmap.createScaledBitmap(croppedBitmap, 200, 200, true)
        FileOutputStream(thumbFile).use { out ->
            thumbBitmap.compress(Bitmap.CompressFormat.WEBP, 80, out)
        }
    }

    suspend fun processAndSaveImage(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext ""
        val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return@withContext ""

        // Save Original
        val originalDir = File(context.filesDir, ORIGINAL_DIR).apply { if (!exists()) mkdirs() }
        FileOutputStream(File(originalDir, "$id.webp")).use { out ->
            originalBitmap.compress(Bitmap.CompressFormat.WEBP, 80, out)
        }

        // Save Portrait
        val portraitDir = File(context.filesDir, PORTRAIT_DIR).apply { if (!exists()) mkdirs() }
        val portraitFile = File(portraitDir, "$id.webp")
        FileOutputStream(portraitFile).use { out ->
            originalBitmap.compress(Bitmap.CompressFormat.WEBP, 80, out)
        }

        // Save Thumbnail
        val thumbDir = File(context.cacheDir, THUMB_DIR).apply { if (!exists()) mkdirs() }
        val thumbFile = File(thumbDir, "$id.webp")
        val thumbBitmap = Bitmap.createScaledBitmap(originalBitmap, 200, 200, true)
        FileOutputStream(thumbFile).use { out ->
            thumbBitmap.compress(Bitmap.CompressFormat.WEBP, 80, out)
        }

        return@withContext id
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
