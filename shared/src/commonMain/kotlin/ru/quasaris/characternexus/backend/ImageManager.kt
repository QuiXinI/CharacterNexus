package ru.quasaris.characternexus.backend

import okio.Path
import ru.quasaris.characternexus.getAppDataDir
import ru.quasaris.characternexus.getCacheDir
import ru.quasaris.characternexus.platformFileSystem
import ru.quasaris.characternexus.util.ImageProcessor
import ru.quasaris.characternexus.util.generateUuid

object ImageManager {
    private const val ORIGINAL_DIR = "originals"
    private const val PORTRAIT_DIR = "portraits"
    private const val THUMB_DIR = "thumbnails"
    private const val CHARACTERS_DIR = "Characters"

    private val appDataDir = getAppDataDir()
    private val cacheDir = getCacheDir()

    fun getCharacterDir(uuid: String): Path {
        val path = appDataDir / CHARACTERS_DIR / uuid
        if (!platformFileSystem.exists(path)) platformFileSystem.createDirectories(path)
        return path
    }

    fun getPortraitFile(imageIdOrUuid: String, characterUuid: String? = null): Path {
        if (!characterUuid.isNullOrBlank()) {
            val charDir = appDataDir / CHARACTERS_DIR / characterUuid
            val newFile = charDir / "portrait.webp"
            if (platformFileSystem.exists(newFile)) return newFile
        }

        if (imageIdOrUuid.isNotBlank()) {
            val directFolder = appDataDir / CHARACTERS_DIR / imageIdOrUuid
            val directFile = directFolder / "portrait.webp"
            if (platformFileSystem.exists(directFile)) return directFile
        }
        
        return appDataDir / PORTRAIT_DIR / "$imageIdOrUuid.webp"
    }

    fun getOriginalFile(imageIdOrUuid: String, characterUuid: String? = null): Path {
        if (!characterUuid.isNullOrBlank()) {
            val charDir = appDataDir / CHARACTERS_DIR / characterUuid
            val newFile = charDir / "original.webp"
            if (platformFileSystem.exists(newFile)) return newFile
        }

        if (imageIdOrUuid.isNotBlank()) {
            val directFolder = appDataDir / CHARACTERS_DIR / imageIdOrUuid
            val directFile = directFolder / "original.webp"
            if (platformFileSystem.exists(directFile)) return directFile
        }
        
        return appDataDir / ORIGINAL_DIR / "$imageIdOrUuid.webp"
    }

    fun getThumbnailFile(imageIdOrUuid: String, characterUuid: String? = null): Path {
        if (!characterUuid.isNullOrBlank()) {
            val charDir = appDataDir / CHARACTERS_DIR / characterUuid
            val newFile = charDir / "thumbnail.webp"
            if (platformFileSystem.exists(newFile)) return newFile
        }

        if (imageIdOrUuid.isNotBlank()) {
            val directFolder = appDataDir / CHARACTERS_DIR / imageIdOrUuid
            val directFile = directFolder / "thumbnail.webp"
            if (platformFileSystem.exists(directFile)) return directFile
        }
        
        return cacheDir / THUMB_DIR / "$imageIdOrUuid.webp"
    }

    suspend fun saveBitmapAsOriginal(bytes: ByteArray): String {
        val id = generateUuid()
        val originalDir = appDataDir / ORIGINAL_DIR
        if (!platformFileSystem.exists(originalDir)) platformFileSystem.createDirectories(originalDir)
        val originalFile = originalDir / "$id.webp"
        
        ImageProcessor.saveCompressedImage(bytes, originalFile)
        return id
    }

    suspend fun saveCropped(id: String, croppedBytes: ByteArray) {
        // 1. Full-Resolution Portrait (Cropped)
        val portraitDir = appDataDir / PORTRAIT_DIR
        if (!platformFileSystem.exists(portraitDir)) platformFileSystem.createDirectories(portraitDir)
        val portraitFile = portraitDir / "$id.webp"
        ImageProcessor.saveCompressedImage(croppedBytes, portraitFile)

        // 2. UI Thumbnail (200x200px)
        val thumbDir = cacheDir / THUMB_DIR
        if (!platformFileSystem.exists(thumbDir)) platformFileSystem.createDirectories(thumbDir)
        val thumbFile = thumbDir / "$id.webp"
        ImageProcessor.saveCompressedImage(croppedBytes, thumbFile, 200, 200)
    }

    fun generateThumbnailFromPortrait(id: String) {
        val portraitFile = getPortraitFile(id)
        if (!platformFileSystem.exists(portraitFile)) return
        
        val thumbDir = cacheDir / THUMB_DIR
        if (!platformFileSystem.exists(thumbDir)) platformFileSystem.createDirectories(thumbDir)
        val thumbFile = thumbDir / "$id.webp"
        ImageProcessor.generateThumbnail(portraitFile, thumbFile)
    }
}
