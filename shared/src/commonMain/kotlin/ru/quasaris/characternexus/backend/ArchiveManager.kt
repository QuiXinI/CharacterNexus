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

    suspend fun importCharacters(bytes: ByteArray): List<Character> {
        val importedCharacters = mutableListOf<Character>()
        
        // Detect if it's a ZIP file (PK header: 50 4B 03 04)
        if (bytes.size > 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()) {
            try {
                val unzipped = ZipUtils.unzip(bytes)
                
                // Legacy Strategy: 
                // 1. Look for character.json (Native)
                // 2. If not found, check if ANY json is LSS
                
                var character: Character? = null
                var dir = ""

                val nativeEntry = unzipped.entries.find { it.key.endsWith("character.json") }
                if (nativeEntry != null) {
                    val jsonContent = nativeEntry.value.decodeToString()
                    character = try { importJson.decodeFromString<Character>(jsonContent) } catch (e: Exception) { null }
                    dir = if (nativeEntry.key.contains("/")) nativeEntry.key.substringBeforeLast("/") + "/" else ""
                } else {
                    // Fallback to searching for LSS in any JSON
                    val jsonEntries = unzipped.filter { it.key.endsWith(".json") && !it.key.contains("__MACOSX") }
                    for ((fileName, jsonBytes) in jsonEntries) {
                        val content = jsonBytes.decodeToString()
                        val element = try { importJson.parseToJsonElement(content) } catch (e: Exception) { null }
                        if (element != null && LongStoryShortImporter.isLongStoryShort(element)) {
                            character = LongStoryShortImporter.parse(element)
                            dir = if (fileName.contains("/")) fileName.substringBeforeLast("/") + "/" else ""
                            break
                        }
                    }
                }

                if (character != null) {
                    val portraitBytes = unzipped[dir + "portrait.webp"] ?: unzipped[dir + "portrait.png"] ?: unzipped[dir + "portrait.jpg"]
                    val originalBytes = unzipped[dir + "original.webp"] ?: unzipped[dir + "original.png"] ?: unzipped[dir + "original.jpg"]
                    
                    val finalChar = if (portraitBytes != null || originalBytes != null) {
                        val imageId = ImageManager.saveImportedAvatar(
                            characterUuid = character.uuid,
                            portraitBytes = portraitBytes,
                            originalBytes = originalBytes
                        )
                        character.copy(imageData = imageId)
                    } else character
                    
                    importedCharacters.add(finalChar)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            // Handle single JSON format
            val jsonString = bytes.decodeToString()
            val jsonElement = try { importJson.parseToJsonElement(jsonString) } catch (e: Exception) { null }

            var character: Character?
            var portraitBytes: ByteArray? = null
            val originalBytes: ByteArray? = null

            if (jsonElement != null && LongStoryShortImporter.isLongStoryShort(jsonElement)) {
                character = LongStoryShortImporter.parse(jsonElement)
            } else {
                character = try {
                    importJson.decodeFromString<Character>(jsonString)
                } catch (e: Exception) {
                    null
                }
            }

            character?.let { char ->
                // Check if imageData is a Base64 string
                val imageData = char.imageData
                if (imageData != null && imageData.length > 100) {
                    try {
                        val decoded = imageData.decodeBase64()
                        if (decoded != null) {
                            portraitBytes = decoded.toByteArray()
                            character = char.copy(imageData = generateUuid())
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            character?.let { char ->
                val finalChar = if (portraitBytes != null || originalBytes != null) {
                    val imageId = ImageManager.saveImportedAvatar(
                        characterUuid = char.uuid,
                        portraitBytes = portraitBytes,
                        originalBytes = originalBytes
                    )
                    char.copy(imageData = imageId)
                } else char
                importedCharacters.add(finalChar)
            }
        }
        
        return importedCharacters
    }

    @Deprecated("Use importCharacters", ReplaceWith("importCharacters(bytes).firstOrNull()"))
    suspend fun importCharacter(bytes: ByteArray): Character? {
        return importCharacters(bytes).firstOrNull()
    }

    suspend fun importCharactersFromFile(path: Path): List<Character> {
        val bytes = try {
            platformFileSystem.read(path) { readByteArray() }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } ?: return emptyList()
        
        return importCharacters(bytes)
    }

    @Deprecated("Use importCharactersFromFile", ReplaceWith("importCharactersFromFile(path).firstOrNull()"))
    suspend fun importCharacterFromFile(path: Path): Character? {
        return importCharactersFromFile(path).firstOrNull()
    }
}
