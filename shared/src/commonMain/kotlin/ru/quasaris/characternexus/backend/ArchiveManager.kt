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

    suspend fun importCharacters(
        bytes: ByteArray,
        onAvatarPrompt: suspend () -> Boolean = { false }
    ): List<Character> {
        val importedCharacters = mutableListOf<Character>()
        
        // Detect if it's a ZIP file (PK header: 50 4B 03 04)
        val isZip = bytes.size > 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()
        
        if (isZip) {
            try {
                val unzipped = ZipUtils.unzip(bytes)
                
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
                        ImageManager.saveImportedAvatar(
                            characterUuid = character!!.uuid,
                            portraitBytes = portraitBytes,
                            originalBytes = originalBytes
                        )
                        character!!
                    } else character!!
                    
                    importedCharacters.add(finalChar)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            // Handle single JSON format
            val jsonString = try { bytes.decodeToString() } catch (e: Exception) { "" }
            if (jsonString.isNotBlank()) {
                val jsonElement = try { importJson.parseToJsonElement(jsonString) } catch (e: Exception) { null }

                var character: Character? = null
                var portraitBytes: ByteArray? = null
                var originalBytes: ByteArray? = null

                if (jsonElement != null && LongStoryShortImporter.isLongStoryShort(jsonElement)) {
                    character = LongStoryShortImporter.parse(jsonElement)
                    
                    // LSS Avatar handling
                    if (character?.avatarUrl != null) {
                        // TODO: Заменить на диалог обрезки картинки
                        val shouldDownload = onAvatarPrompt()
                        if (shouldDownload) {
                            portraitBytes = LssAvatarService.downloadAvatar(character!!)
                            originalBytes = portraitBytes
                        }
                    }
                } else if (jsonElement != null) {
                    character = try {
                        importJson.decodeFromString<Character>(jsonString)
                    } catch (e: Exception) {
                        null
                    }
                }

                character?.let { char ->
                    // Check if imageData is a Base64 string
                    val imageData = char.imageData
                    if (imageData != null && imageData.length > 100 && portraitBytes == null) {
                        try {
                            val decoded = imageData.decodeBase64()
                            if (decoded != null) {
                                portraitBytes = decoded.toByteArray()
                                character = char.copy(imageData = null)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                character?.let { char ->
                    if (portraitBytes != null || originalBytes != null) {
                        ImageManager.saveImportedAvatar(
                            characterUuid = char.uuid,
                            portraitBytes = portraitBytes,
                            originalBytes = originalBytes
                        )
                    }
                    importedCharacters.add(char)
                }
            }
        }
        
        return importedCharacters
    }

    @Deprecated("Use importCharacters", ReplaceWith("importCharacters(bytes, onAvatarPrompt).firstOrNull()"))
    suspend fun importCharacter(bytes: ByteArray, onAvatarPrompt: suspend () -> Boolean = { false }): Character? {
        return importCharacters(bytes, onAvatarPrompt).firstOrNull()
    }

    suspend fun importCharactersFromFile(path: Path, onAvatarPrompt: suspend () -> Boolean = { false }): List<Character> {
        val bytes = try {
            platformFileSystem.read(path) { readByteArray() }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } ?: return emptyList()
        
        return importCharacters(bytes, onAvatarPrompt)
    }

    @Deprecated("Use importCharactersFromFile", ReplaceWith("importCharactersFromFile(path).firstOrNull()"))
    suspend fun importCharacterFromFile(path: Path): Character? {
        return importCharactersFromFile(path).firstOrNull()
    }
}
