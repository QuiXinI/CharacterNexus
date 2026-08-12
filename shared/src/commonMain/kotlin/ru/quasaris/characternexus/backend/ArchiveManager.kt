package ru.quasaris.characternexus.backend

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okio.ByteString.Companion.decodeBase64
import okio.Path
import ru.quasaris.characternexus.model.Character
import ru.quasaris.characternexus.platformFileSystem
import ru.quasaris.characternexus.util.ZipUtils
import ru.quasaris.characternexus.util.generateUuid

object ArchiveManager {
    const val EXPORT_EXTENSION = "charbook"
    
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val importJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun exportCharacter(character: Character): ByteArray {
        val files = mutableMapOf<String, ByteArray>()
        
        // 1. character.json
        val characterJson = json.encodeToString(character)
        files["character.json"] = characterJson.encodeToByteArray()

        // 2. Images
        character.imageData?.let { imageId ->
            val originalFile = ImageManager.getOriginalFile(imageId, character.uuid)
            if (platformFileSystem.exists(originalFile)) {
                files["original.webp"] = platformFileSystem.read(originalFile) { readByteArray() }
            }

            val portraitFile = ImageManager.getPortraitFile(imageId, character.uuid)
            if (platformFileSystem.exists(portraitFile)) {
                files["portrait.webp"] = platformFileSystem.read(portraitFile) { readByteArray() }
            }
        }

        return ZipUtils.zip(files)
    }

    suspend fun importCharacter(bytes: ByteArray): Character? {
        var character: Character? = null
        var portraitBytes: ByteArray? = null
        var originalBytes: ByteArray? = null

        // Detect if it's a ZIP file (PK header: 50 4B 03 04)
        if (bytes.size > 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()) {
            try {
                val unzipped = ZipUtils.unzip(bytes)
                unzipped["character.json"]?.let {
                    character = importJson.decodeFromString<Character>(it.decodeToString())
                }
                portraitBytes = unzipped["portrait.webp"]
                originalBytes = unzipped["original.webp"]
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        } else {
            // Handle JSON formats
            val jsonString = bytes.decodeToString()
            val jsonElement = try { importJson.parseToJsonElement(jsonString) } catch (e: Exception) { null }

            if (jsonElement != null && LongStoryShortImporter.isLongStoryShort(jsonElement)) {
                character = LongStoryShortImporter.parse(jsonElement)
            } else {
                // Legacy JSON or current KMP JSON
                character = try {
                    importJson.decodeFromString<Character>(jsonString)
                } catch (e: Exception) {
                    null
                }
            }

            character?.let { char ->
                // Check if imageData is a Base64 string (longer than a UUID)
                val imageData = char.imageData
                if (imageData != null && imageData.length > 100) {
                    try {
                        val decoded = imageData.decodeBase64()
                        if (decoded != null) {
                            portraitBytes = decoded.toByteArray()
                            // Reset imageData to a new UUID so the logic below saves it properly
                            character = char.copy(imageData = generateUuid())
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        character?.let { char ->
            val finalChar = if (portraitBytes != null || originalBytes != null) {
                val newId = char.imageData ?: generateUuid()
                
                // Use a temporary character UUID if the character doesn't have one (though Models.kt defaults it)
                val charUuid = char.uuid
                
                // Save Original
                if (originalBytes != null) {
                    val originalFile = ImageManager.getOriginalFile(newId, charUuid)
                    platformFileSystem.createDirectories(originalFile.parent!!)
                    platformFileSystem.write(originalFile) { write(originalBytes!!) }
                } else if (portraitBytes != null) {
                    // If no original, save portrait as original for fallback
                    val originalFile = ImageManager.getOriginalFile(newId, charUuid)
                    platformFileSystem.createDirectories(originalFile.parent!!)
                    platformFileSystem.write(originalFile) { write(portraitBytes!!) }
                }

                // Save Portrait
                if (portraitBytes != null) {
                    val portraitFile = ImageManager.getPortraitFile(newId, charUuid)
                    platformFileSystem.createDirectories(portraitFile.parent!!)
                    platformFileSystem.write(portraitFile) { write(portraitBytes!!) }
                } else if (originalBytes != null) {
                    // If only original, save it as portrait too
                    val portraitFile = ImageManager.getPortraitFile(newId, charUuid)
                    platformFileSystem.createDirectories(portraitFile.parent!!)
                    platformFileSystem.write(portraitFile) { write(originalBytes!!) }
                }

                // Regenerate thumbnail
                ImageManager.generateThumbnailFromPortrait(newId)
                char.copy(imageData = newId)
            } else char

            return finalChar
        }
        return null
    }

    suspend fun importCharacterFromFile(path: Path): Character? {
        val bytes = try {
            platformFileSystem.read(path) { readByteArray() }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } ?: return null
        
        return importCharacter(bytes)
    }
}
