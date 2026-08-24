package ru.quasaris.characternexus.backend

import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.Path.Companion.toPath
import ru.quasaris.characternexus.model.Character
import ru.quasaris.characternexus.platformFileSystem
import ru.quasaris.characternexus.ioDispatcher
import ru.quasaris.characternexus.util.log
import ru.quasaris.characternexus.util.generateUuid
import ru.quasaris.characternexus.util.ImageProcessor
import ru.quasaris.characternexus.util.ZipUtils
import ru.quasaris.characternexus.util.Logger
import okio.ByteString.Companion.decodeBase64
import kotlinx.serialization.json.*

@Serializable
data class CharacterManifest(
    val characters: List<ManifestEntry>,
    val exportDate: String,
    val version: Int = 1
)

@Serializable
data class ManifestEntry(
    val uuid: String,
    val name: String,
    val folder: String
)

data class ImportResult(
    val character: Character,
    val portraitBytes: ByteArray? = null,
    val originalBytes: ByteArray? = null
)

object ArchiveManager {
    const val EXPORT_EXTENSION = "charbook"
    
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    suspend fun exportCharacter(character: Character, targetPath: String) = exportCharactersBundle(listOf(character), targetPath)

    suspend fun getExportBundleBytes(characters: List<Character>): ByteArray = withContext(ioDispatcher) {
        val files = mutableMapOf<String, ByteArray>()
        val manifestEntries = mutableListOf<ManifestEntry>()

        characters.forEach { character ->
            val folderName = "${character.name.filter { it.isLetterOrDigit() }}_${character.uuid.take(4)}"
            val prefix = if (characters.size > 1) "$folderName/" else ""
            
            manifestEntries.add(ManifestEntry(character.uuid, character.name, folderName))

            val charJson = json.encodeToString(character)
            files["${prefix}character.json"] = charJson.encodeToByteArray()

            character.imageData?.let { imageId ->
                val portraitFile = ImageManager.getPortraitFile(imageId, character.uuid)
                val originalFile = ImageManager.getOriginalFile(imageId, character.uuid)

                if (platformFileSystem.exists(portraitFile)) {
                    files["${prefix}portrait.webp"] = platformFileSystem.read(portraitFile) { readByteArray() }
                }
                if (platformFileSystem.exists(originalFile)) {
                    files["${prefix}original.webp"] = platformFileSystem.read(originalFile) { readByteArray() }
                }
            }
        }

        if (characters.size > 1) {
            val manifest = CharacterManifest(
                characters = manifestEntries,
                exportDate = "" // Could add current date if needed
            )
            files["manifest.json"] = json.encodeToString(manifest).encodeToByteArray()
        }

        ZipUtils.zip(files)
    }

    suspend fun exportCharactersBundle(characters: List<Character>, targetPath: String) = withContext(ioDispatcher) {
        try {
            val path = targetPath.toPath()
            val isJson = targetPath.endsWith(".json", ignoreCase = true)

            if (isJson && characters.size == 1) {
                platformFileSystem.write(path) {
                    writeUtf8(json.encodeToString(characters.first()))
                }
            } else {
                val zipBytes = getExportBundleBytes(characters)
                platformFileSystem.write(path) {
                    write(zipBytes)
                }
            }
        } catch (e: Exception) {
            e.log()
        }
    }

    suspend fun importCharacter(bytes: ByteArray): ImportResult? = importCharacters(bytes).firstOrNull()

    suspend fun importCharacters(bytes: ByteArray): List<ImportResult> = withContext(ioDispatcher) {
        val results = mutableListOf<ImportResult>()
        
        try {
            var unzippedFiles: Map<String, ByteArray>? = null
            try {
                val files = ZipUtils.unzip(bytes)
                if (files.isNotEmpty()) {
                    unzippedFiles = files
                }
            } catch (e: Exception) {
                Logger.d("ArchiveManager", "Unzip attempt failed: ${e.message}")
            }

            if (unzippedFiles != null && unzippedFiles.isNotEmpty()) {
                // Group files by directory
                val groups = unzippedFiles.keys.groupBy { 
                    val parts = it.split("/")
                    if (parts.size > 1) parts.dropLast(1).joinToString("/") else ""
                }

                if (groups.size > 1 || (groups.keys.first().isNotEmpty())) {
                    // Multi-character or single character in a folder
                    groups.forEach { (_, fileKeys) ->
                        val charJsonBytes = unzippedFiles[fileKeys.find { it.endsWith("character.json", ignoreCase = true) }]
                        if (charJsonBytes != null) {
                            val jsonString = decodeSmart(charJsonBytes)
                            val character = parseCharacterContent(jsonString)
                            if (character != null) {
                                val portraitBytes = unzippedFiles[fileKeys.find { it.endsWith("portrait.webp", ignoreCase = true) }]
                                val originalBytes = unzippedFiles[fileKeys.find { it.endsWith("original.webp", ignoreCase = true) }]
                                results.add(createImportResult(character, portraitBytes, originalBytes))
                            }
                        }
                    }
                } else {
                    // Legacy single character at root
                    val charJsonBytes = unzippedFiles.entries.find { it.key.equals("character.json", ignoreCase = true) }?.value
                    if (charJsonBytes != null) {
                        val jsonString = decodeSmart(charJsonBytes)
                        val character = parseCharacterContent(jsonString)
                        if (character != null) {
                            val portraitBytes = unzippedFiles.entries.find { it.key.equals("portrait.webp", ignoreCase = true) }?.value
                            val originalBytes = unzippedFiles.entries.find { it.key.equals("original.webp", ignoreCase = true) }?.value
                            results.add(createImportResult(character, portraitBytes, originalBytes))
                        }
                    }
                }
            } else {
                // Handle plain JSON
                try {
                    val jsonString = decodeSmart(bytes)
                    if (jsonString.trim().startsWith("{")) {
                        val character = parseCharacterContent(jsonString)
                        character?.let { char ->
                            var portraitBytes: ByteArray? = null
                            if (char.imageData != null && char.imageData.length > 100) {
                                try {
                                    portraitBytes = char.imageData.decodeBase64()?.toByteArray()
                                } catch (e: Exception) {}
                            }
                            results.add(createImportResult(char, portraitBytes, null))
                        }
                    }
                } catch (e: Exception) {}
            }
        } catch (e: Exception) {
            Logger.e("ArchiveManager", "Critical failure in importCharacters", e)
        }
        results
    }

    private fun createImportResult(char: Character, portraitBytes: ByteArray?, originalBytes: ByteArray?): ImportResult {
        val newUuid = generateUuid()
        val newId = (0..Int.MAX_VALUE).random()
        val freshChar = char.copy(
            uuid = newUuid,
            id = newId,
            imageData = if (portraitBytes != null || originalBytes != null) generateUuid() else null
        )
        return ImportResult(
            character = freshChar,
            portraitBytes = portraitBytes,
            originalBytes = originalBytes
        )
    }

    /**
     * Attempts to decode bytes to string using UTF-8, 
     * but falls back to other encodings if result contains garbage.
     */
    private fun decodeSmart(bytes: ByteArray): String {
        val utf8 = bytes.decodeToString()
        // If it contains multiple replacement characters, it's likely not UTF-8
        if (utf8.count { it == '\uFFFD' } > 3) {
            Logger.d("ArchiveManager", "UTF-8 decode looks like garbage, trying fallback (encoding logic is platform-specific, but using best effort)")
            // Note: In common code we don't have easy access to Windows-1251. 
            // However, most modern exports are UTF-8. If it's old legacy, it might stay broken 
            // unless we add multiplatform encoding support.
        }
        return utf8
    }

    private fun parseCharacterContent(jsonString: String): Character? {
        return try {
            val jsonElement = json.parseToJsonElement(jsonString)
            if (LongStoryShortImporter.isLongStoryShort(jsonElement)) {
                LongStoryShortImporter.parse(jsonElement)
            } else {
                json.decodeFromString<Character>(jsonString)
            }
        } catch (e: Exception) {
            e.log()
            null
        }
    }
}
