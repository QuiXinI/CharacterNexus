package ru.quasaris.characternexus.backend

import kotlinx.coroutines.withContext
import okio.Path
import ru.quasaris.characternexus.getAppDataDir
import ru.quasaris.characternexus.getCacheDir
import ru.quasaris.characternexus.platformFileSystem
import ru.quasaris.characternexus.ioDispatcher
import ru.quasaris.characternexus.util.generateUuid
import ru.quasaris.characternexus.util.ImageProcessor

object ImageManager {
    private const val ORIGINAL_DIR = "originals"
    private const val PORTRAIT_DIR = "portraits"
    private const val THUMB_DIR = "thumbnails"
    private const val CHARACTERS_DIR = "Characters"

    private val fileSystem = platformFileSystem

    fun getCharacterDir(uuid: String): Path {
        val path = getAppDataDir().div(CHARACTERS_DIR).div(uuid)
        if (!fileSystem.exists(path)) fileSystem.createDirectories(path)
        return path
    }

    /**
     * Returns the portrait file for a character.
     * Tries new character-specific folder first, then legacy global folder.
     */
    fun getPortraitFile(imageIdOrUuid: String, characterUuid: String? = null): Path {
        // 1. Try new structure: Characters/[uuid]/portrait.webp
        if (!characterUuid.isNullOrBlank()) {
            val charDir = getAppDataDir().div(CHARACTERS_DIR).div(characterUuid)
            val newFile = charDir.div("portrait.webp")
            if (fileSystem.exists(newFile)) return newFile
        }

        // 2. Try using imageIdOrUuid as folder name (in case it IS the UUID or a folder-based ID)
        if (imageIdOrUuid.isNotBlank()) {
            val directFolder = getAppDataDir().div(CHARACTERS_DIR).div(imageIdOrUuid).div("portrait.webp")
            if (fileSystem.exists(directFolder)) return directFolder
        }
        
        // 3. Fallback to legacy global folder
        return getAppDataDir().div(PORTRAIT_DIR).div("$imageIdOrUuid.webp")
    }

    fun getOriginalFile(imageIdOrUuid: String, characterUuid: String? = null): Path {
        if (!characterUuid.isNullOrBlank()) {
            val charDir = getAppDataDir().div(CHARACTERS_DIR).div(characterUuid)
            val newFile = charDir.div("original.webp")
            if (fileSystem.exists(newFile)) return newFile
        }

        if (imageIdOrUuid.isNotBlank()) {
            val directFolder = getAppDataDir().div(CHARACTERS_DIR).div(imageIdOrUuid).div("original.webp")
            if (fileSystem.exists(directFolder)) return directFolder
        }
        
        return getAppDataDir().div(ORIGINAL_DIR).div("$imageIdOrUuid.webp")
    }

    fun getThumbnailFile(imageIdOrUuid: String, characterUuid: String? = null): Path {
        if (!characterUuid.isNullOrBlank()) {
            val charDir = getAppDataDir().div(CHARACTERS_DIR).div(characterUuid)
            val newFile = charDir.div("thumbnail.webp")
            if (fileSystem.exists(newFile)) return newFile
        }

        if (imageIdOrUuid.isNotBlank()) {
            val directFolder = getAppDataDir().div(CHARACTERS_DIR).div(imageIdOrUuid).div("thumbnail.webp")
            if (fileSystem.exists(directFolder)) return directFolder
        }
        
        return getCacheDir().div(THUMB_DIR).div("$imageIdOrUuid.webp")
    }

    /**
     * Saves the image bytes as original and generates a portrait and thumbnail.
     */
    suspend fun saveNewImage(bytes: ByteArray): String = withContext(ioDispatcher) {
        val id = generateUuid()
        
        // Save Original
        val originalDir = getAppDataDir().div(ORIGINAL_DIR)
        if (!fileSystem.exists(originalDir)) fileSystem.createDirectories(originalDir)
        val originalFile = originalDir.div("$id.webp")
        ImageProcessor.saveCompressedImage(bytes, originalFile)

        // Save Portrait
        val portraitDir = getAppDataDir().div(PORTRAIT_DIR)
        if (!fileSystem.exists(portraitDir)) fileSystem.createDirectories(portraitDir)
        val portraitFile = portraitDir.div("$id.webp")
        ImageProcessor.saveCompressedImage(bytes, portraitFile)

        // Save Thumbnail
        val thumbDir = getCacheDir().div(THUMB_DIR)
        if (!fileSystem.exists(thumbDir)) fileSystem.createDirectories(thumbDir)
        val thumbFile = thumbDir.div("$id.webp")
        ImageProcessor.generateThumbnail(portraitFile, thumbFile)

        id
    }

    suspend fun saveCroppedImage(id: String, croppedBytes: ByteArray) = withContext(ioDispatcher) {
        // Save Portrait (overwriting legacy or global one)
        val portraitDir = getAppDataDir().div(PORTRAIT_DIR)
        if (!fileSystem.exists(portraitDir)) fileSystem.createDirectories(portraitDir)
        val portraitFile = portraitDir.div("$id.webp")
        ImageProcessor.saveCompressedImage(croppedBytes, portraitFile)

        // Save Thumbnail
        val thumbDir = getCacheDir().div(THUMB_DIR)
        if (!fileSystem.exists(thumbDir)) fileSystem.createDirectories(thumbDir)
        val thumbFile = thumbDir.div("$id.webp")
        ImageProcessor.generateThumbnail(portraitFile, thumbFile)
    }
}
