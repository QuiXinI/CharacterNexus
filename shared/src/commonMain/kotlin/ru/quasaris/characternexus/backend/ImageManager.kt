package ru.quasaris.characternexus.backend

import kotlinx.coroutines.withContext
import okio.Path
import ru.quasaris.characternexus.getAppDataDir
import ru.quasaris.characternexus.getCacheDir
import ru.quasaris.characternexus.platformFileSystem
import ru.quasaris.characternexus.ioDispatcher
import ru.quasaris.characternexus.util.generateUuid
import ru.quasaris.characternexus.util.ImageProcessor
import ru.quasaris.characternexus.util.Logger

object ImageManager {
    private const val ORIGINAL_DIR = "originals"
    private const val PORTRAIT_DIR = "portraits"
    private const val THUMB_DIR = "thumbnails"
    private const val CHARACTERS_DIR = "Characters"

    private val fileSystem = platformFileSystem

    /**
     * Cleans up any character data folders that do not belong to active characters.
     */
    fun cleanupOrphanedCharacters(activeUuids: List<String>) {
        try {
            val baseDir = getAppDataDir().div(CHARACTERS_DIR)
            if (!fileSystem.exists(baseDir)) return

            val existingFolders = fileSystem.list(baseDir)
            existingFolders.forEach { path ->
                if (fileSystem.metadata(path).isDirectory) {
                    val uuid = path.name
                    if (uuid !in activeUuids) {
                        Logger.d("ImageManager", "Deleting orphaned character folder: $uuid")
                        fileSystem.deleteRecursively(path)
                    }
                }
            }
            
            // Also cleanup global legacy thumbnails if they don't match active UUIDs (if any remain)
            val thumbDir = getCacheDir().div(THUMB_DIR)
            if (fileSystem.exists(thumbDir)) {
                fileSystem.list(thumbDir).forEach { path ->
                    val fileName = path.name.removeSuffix(".webp")
                    // If fileName is a UUID and not in active list, delete
                    if (fileName.length > 20 && fileName !in activeUuids) {
                        fileSystem.delete(path)
                    }
                }
            }
        } catch (e: Exception) {
            Logger.e("ImageManager", "Cleanup failed", e)
        }
    }

    fun getCharacterDir(uuid: String): Path {
        val path = getAppDataDir().div(CHARACTERS_DIR).div(uuid)
        if (!fileSystem.exists(path)) fileSystem.createDirectories(path)
        return path
    }

    /**
     * Returns the portrait file for a character.
     * Strictly uses character-specific folder.
     */
    fun getPortraitFile(imageIdOrUuid: String, characterUuid: String? = null): Path {
        val targetUuid = characterUuid ?: imageIdOrUuid
        val charDir = getAppDataDir().div(CHARACTERS_DIR).div(targetUuid)
        val file = charDir.div("portrait.webp")
        
        if (fileSystem.exists(file)) return file
        
        // Fallback for legacy global folder (migration)
        return getAppDataDir().div(PORTRAIT_DIR).div("$imageIdOrUuid.webp")
    }

    fun getOriginalFile(imageIdOrUuid: String, characterUuid: String? = null): Path {
        val targetUuid = characterUuid ?: imageIdOrUuid
        val charDir = getAppDataDir().div(CHARACTERS_DIR).div(targetUuid)
        val file = charDir.div("original.webp")
        
        if (fileSystem.exists(file)) return file
        
        return getAppDataDir().div(ORIGINAL_DIR).div("$imageIdOrUuid.webp")
    }

    fun getThumbnailFile(imageIdOrUuid: String, characterUuid: String? = null): Path {
        val targetUuid = characterUuid ?: imageIdOrUuid
        val charDir = getAppDataDir().div(CHARACTERS_DIR).div(targetUuid)
        val file = charDir.div("thumbnail.webp")
        
        if (fileSystem.exists(file)) return file
        
        return getCacheDir().div(THUMB_DIR).div("$imageIdOrUuid.webp")
    }

    /**
     * Finalizes and saves character images after cropping.
     */
    suspend fun saveCharacterImages(
        characterUuid: String,
        originalBytes: ByteArray?,
        portraitBytes: ByteArray?,
        croppedBytes: ByteArray?
    ) = withContext(ioDispatcher) {
        val charDir = getCharacterDir(characterUuid)
        
        // 1. Save Original
        if (originalBytes != null) {
            val originalFile = charDir.div("original.webp")
            ImageProcessor.saveCompressedImage(originalBytes, originalFile)
        }

        // 2. Save Portrait (can be the same as original if not cropped yet, but usually it's the high-res one)
        if (portraitBytes != null) {
            val portraitFile = charDir.div("portrait.webp")
            ImageProcessor.saveCompressedImage(portraitBytes, portraitFile)
        }

        // 3. Save Cropped Portrait
        if (croppedBytes != null) {
            val croppedFile = charDir.div("portrait.webp") // Overwrite portrait with cropped version
            ImageProcessor.saveCompressedImage(croppedBytes, croppedFile)
            
            // 4. Save Thumbnail
            val thumbFile = charDir.div("thumbnail.webp")
            ImageProcessor.generateThumbnail(croppedFile, thumbFile)
        } else if (portraitBytes != null) {
            // Fallback thumbnail if no cropping done (shouldn't happen with mandatory crop)
            val portraitFile = charDir.div("portrait.webp")
            val thumbFile = charDir.div("thumbnail.webp")
            ImageProcessor.generateThumbnail(portraitFile, thumbFile)
        }
    }
}
