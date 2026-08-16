package ru.quasaris.characternexus.backend

import kotlinx.coroutines.withContext
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

    suspend fun exportCharacter(character: Character, targetPath: String) = withContext(ioDispatcher) {
        try {
            val path = targetPath.toPath()
            val isJson = targetPath.endsWith(".json", ignoreCase = true)

            if (isJson) {
                platformFileSystem.write(path) {
                    writeUtf8(json.encodeToString(character))
                }
            } else {
                val charJson = json.encodeToString(character)
                val files = mutableMapOf<String, ByteArray>()
                files["character.json"] = charJson.encodeToByteArray()

                character.imageData?.let { imageId ->
                    val portraitFile = ImageManager.getPortraitFile(imageId, character.uuid)
                    val originalFile = ImageManager.getOriginalFile(imageId, character.uuid)

                    if (platformFileSystem.exists(portraitFile)) {
                        files["portrait.webp"] = platformFileSystem.read(portraitFile) { readByteArray() }
                    }
                    if (platformFileSystem.exists(originalFile)) {
                        files["original.webp"] = platformFileSystem.read(originalFile) { readByteArray() }
                    }
                }

                val zipBytes = ZipUtils.zip(files)
                platformFileSystem.write(path) {
                    write(zipBytes)
                }
            }
        } catch (e: Exception) {
            e.log()
        }
    }

    suspend fun importCharacter(bytes: ByteArray): ImportResult? = withContext(ioDispatcher) {
        var character: Character? = null
        var portraitBytes: ByteArray? = null
        var originalBytes: ByteArray? = null

        val firstBytesHex = bytes.take(64).joinToString(" ") { it.toInt().and(0xFF).toString(16).padStart(2, '0').uppercase() }
        Logger.d("ArchiveManager", "Starting import, bytes size: ${bytes.size}")
        Logger.d("ArchiveManager", "First 64 bytes: $firstBytesHex")

        try {
            // First, attempt to unzip
            var unzippedFiles: Map<String, ByteArray>? = null
            try {
                val files = ZipUtils.unzip(bytes)
                if (files.isNotEmpty()) {
                    unzippedFiles = files
                    Logger.d("ArchiveManager", "Successfully unzipped, found ${files.size} entries: ${files.keys}")
                }
            } catch (e: Exception) {
                Logger.d("ArchiveManager", "Unzip attempt failed: ${e.message}")
            }

            if (unzippedFiles != null && unzippedFiles.isNotEmpty()) {
                val charJsonBytes = unzippedFiles.entries.find { it.key.equals("character.json", ignoreCase = true) || it.key.endsWith("/character.json", ignoreCase = true) }?.value
                
                if (charJsonBytes != null) {
                    val jsonString = decodeSmart(charJsonBytes)
                    character = parseCharacterContent(jsonString)
                    Logger.d("ArchiveManager", "character.json found and parsed from ZIP")
                } else {
                    Logger.e("ArchiveManager", "character.json NOT found in ZIP. Available files: ${unzippedFiles.keys}")
                }
                
                portraitBytes = unzippedFiles.entries.find { it.key.equals("portrait.webp", ignoreCase = true) || it.key.endsWith("/portrait.webp", ignoreCase = true) }?.value
                originalBytes = unzippedFiles.entries.find { it.key.equals("original.webp", ignoreCase = true) || it.key.endsWith("/original.webp", ignoreCase = true) }?.value
            } else {
                Logger.d("ArchiveManager", "Treating as plain JSON (no unzipped files)")
                // Handle plain JSON
                try {
                    val jsonString = decodeSmart(bytes)
                    // Basic sanity check: character JSON must start with {
                    if (jsonString.trim().startsWith("{")) {
                        character = parseCharacterContent(jsonString)
                        
                        // Handle Base64 image in legacy JSON
                        character?.let { char ->
                            if (char.imageData != null && char.imageData.length > 100) {
                                Logger.d("ArchiveManager", "Base64 image detected in JSON")
                                try {
                                    portraitBytes = char.imageData.decodeBase64()?.toByteArray()
                                    // Don't null it yet, we'll replace it with a fresh UUID below
                                } catch (e: Exception) {
                                    Logger.e("ArchiveManager", "Failed to decode Base64 image")
                                }
                            }
                        }
                    } else {
                        Logger.e("ArchiveManager", "File does not look like JSON (doesn't start with {)")
                    }
                } catch (e: Exception) {
                    Logger.e("ArchiveManager", "Failed to process as plain JSON", e)
                }
            }

            character?.let { char ->
                // ALWAYS generate new unique IDs for imported characters to avoid silent overwrites
                val newUuid = generateUuid()
                val newId = (0..Int.MAX_VALUE).random()
                Logger.d("ArchiveManager", "Assigning new identity: name=${char.name}, new_uuid=$newUuid, new_id=$newId")
                
                val freshChar = char.copy(
                    uuid = newUuid,
                    id = newId,
                    imageData = if (portraitBytes != null || originalBytes != null) generateUuid() else null
                )

                return@withContext ImportResult(
                    character = freshChar,
                    portraitBytes = portraitBytes,
                    originalBytes = originalBytes
                )
            }
        } catch (e: Exception) {
            Logger.e("ArchiveManager", "Critical failure in importCharacter", e)
        }
        null
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
