package ru.quasaris.characternexus.backend

import okio.Path
import ru.quasaris.characternexus.getAppDataDir
import ru.quasaris.characternexus.platformFileSystem
import ru.quasaris.characternexus.util.ImageProcessor
import ru.quasaris.characternexus.util.ensureNomedia

object ImageManager {
    private const val CHARACTERS_DIR = "Characters"

    private val appDataDir = getAppDataDir()

    fun getCharacterDir(uuid: String): Path {
        val path = appDataDir / CHARACTERS_DIR / uuid
        path.ensureNomedia()
        return path
    }

    fun getPortraitFile(imageIdOrUuid: String, characterUuid: String? = null): Path {
        val uuid = characterUuid ?: imageIdOrUuid
        val charDir = appDataDir / CHARACTERS_DIR / uuid
        return charDir / "portrait.webp"
    }

    fun getOriginalFile(imageIdOrUuid: String, characterUuid: String? = null): Path {
        val uuid = characterUuid ?: imageIdOrUuid
        val charDir = appDataDir / CHARACTERS_DIR / uuid
        return charDir / "original.webp"
    }

    fun getThumbnailFile(imageIdOrUuid: String, characterUuid: String? = null): Path {
        val uuid = characterUuid ?: imageIdOrUuid
        val charDir = appDataDir / CHARACTERS_DIR / uuid
        return charDir / "thumbnail.webp"
    }

    fun saveBitmapAsOriginal(bytes: ByteArray, characterUuid: String): String {
        val charDir = getCharacterDir(characterUuid)
        val originalFile = charDir / "original.webp"
        
        ImageProcessor.saveCompressedImage(bytes, originalFile)
        return characterUuid
    }

    fun saveCropped(characterUuid: String, croppedBytes: ByteArray) {
        val charDir = getCharacterDir(characterUuid)
        
        // 1. Full-Resolution Portrait (Cropped)
        val portraitFile = charDir / "portrait.webp"
        ImageProcessor.saveCompressedImage(croppedBytes, portraitFile)

        // 2. UI Thumbnail (200x200px)
        val thumbFile = charDir / "thumbnail.webp"
        ImageProcessor.saveCompressedImage(croppedBytes, thumbFile, 200, 200)
    }

    fun generateThumbnailFromPortrait(characterUuid: String) {
        val portraitFile = getPortraitFile("", characterUuid)
        if (!platformFileSystem.exists(portraitFile)) return
        
        val thumbFile = getThumbnailFile("", characterUuid)
        ImageProcessor.generateThumbnail(portraitFile, thumbFile)
    }

    fun saveImportedAvatar(characterUuid: String, portraitBytes: ByteArray?, originalBytes: ByteArray?): String {
        getCharacterDir(characterUuid)
        
        // Save Original
        val finalOriginalBytes = originalBytes ?: portraitBytes
        if (finalOriginalBytes != null) {
            val originalFile = getOriginalFile("", characterUuid)
            platformFileSystem.write(originalFile) { write(finalOriginalBytes) }
        }

        // Save Portrait
        val finalPortraitBytes = portraitBytes ?: originalBytes
        if (finalPortraitBytes != null) {
            val portraitFile = getPortraitFile("", characterUuid)
            platformFileSystem.write(portraitFile) { write(finalPortraitBytes) }
            
            // Generate Thumbnail
            val thumbFile = getThumbnailFile("", characterUuid)
            ImageProcessor.saveCompressedImage(finalPortraitBytes, thumbFile, 200, 200)
        }
        
        return characterUuid
    }
}
