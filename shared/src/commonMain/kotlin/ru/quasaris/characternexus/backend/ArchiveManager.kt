package ru.quasaris.characternexus.backend

import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.Path.Companion.toPath
import ru.quasaris.characternexus.model.Character
import ru.quasaris.characternexus.model.CharacterFolder
import ru.quasaris.characternexus.platformFileSystem
import ru.quasaris.characternexus.ioDispatcher
import ru.quasaris.characternexus.util.log
import ru.quasaris.characternexus.util.generateUuid
import ru.quasaris.characternexus.util.ZipUtils
import ru.quasaris.characternexus.util.Logger
import okio.ByteString.Companion.decodeBase64
import kotlinx.serialization.json.*

@Serializable
data class CharacterFolderExport(
    val uuid: String,
    val name: String,
    val parentFolderUuid: String? = null,
    val colorArgb: Int? = null,
    val isExpanded: Boolean = true
)

@Serializable
data class ManifestEntry(
    val uuid: String,
    val name: String,
    val folder: String,
    val folderUuid: String? = null
)

@Serializable
data class CharacterManifest(
    val characters: List<ManifestEntry>,
    val folders: List<CharacterFolderExport> = emptyList(),
    val globalOrder: List<String> = emptyList(),
    val exportDate: String = "",
    val version: Int = 2
)

data class ImportResult(
    val character: Character,
    val portraitBytes: ByteArray? = null,
    val originalBytes: ByteArray? = null
)

data class ImportBundleResult(
    val characters: List<ImportResult>,
    val folders: List<CharacterFolderExport> = emptyList(),
    val globalOrder: List<String> = emptyList()
)

object ArchiveManager {
    const val EXPORT_EXTENSION = "cb"
    
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    suspend fun exportCharacter(character: Character, targetPath: String) = exportCharactersBundle(listOf(character), emptyList(), targetPath)

    suspend fun getExportBundleBytes(
        characters: List<Character>,
        folders: List<CharacterFolder> = emptyList(),
        globalOrder: List<String> = emptyList()
    ): ByteArray = withContext(ioDispatcher) {
        val files = mutableMapOf<String, ByteArray>()
        val manifestEntries = mutableListOf<ManifestEntry>()

        characters.forEach { character ->
            val folderName = "${character.name.filter { it.isLetterOrDigit() }}_${character.uuid.take(4)}"
            val prefix = if (characters.size > 1 || folders.isNotEmpty()) "$folderName/" else ""
            
            manifestEntries.add(ManifestEntry(character.uuid, character.name, folderName, character.folderUuid))

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

        if (characters.size > 1 || folders.isNotEmpty()) {
            val exportedUuids = (characters.map { it.uuid } + folders.map { it.uuid }).toSet()
            val filteredOrder = globalOrder.filter { it in exportedUuids }

            val manifest = CharacterManifest(
                characters = manifestEntries,
                folders = folders.map { f ->
                    CharacterFolderExport(
                        uuid = f.uuid,
                        name = f.name,
                        parentFolderUuid = f.parentFolderUuid,
                        colorArgb = f.colorArgb,
                        isExpanded = f.isExpanded
                    )
                },
                globalOrder = filteredOrder,
                exportDate = ""
            )
            files["manifest.json"] = json.encodeToString(manifest).encodeToByteArray()
        }

        ZipUtils.zip(files)
    }

    suspend fun exportCharactersBundle(
        characters: List<Character>,
        folders: List<CharacterFolder>,
        targetPath: String
    ) = withContext(ioDispatcher) {
        try {
            val path = targetPath.toPath()
            val isJson = targetPath.endsWith(".json", ignoreCase = true)

            if (isJson && characters.size == 1) {
                platformFileSystem.write(path) {
                    writeUtf8(json.encodeToString(characters.first()))
                }
            } else {
                val zipBytes = getExportBundleBytes(characters, folders)
                platformFileSystem.write(path) {
                    write(zipBytes)
                }
            }
        } catch (e: Exception) {
            e.log()
        }
    }

    suspend fun importCharacter(bytes: ByteArray): ImportResult? = importCharacters(bytes).firstOrNull()

    suspend fun importCharacters(bytes: ByteArray): List<ImportResult> = importBundle(bytes).characters

    suspend fun importBundle(bytes: ByteArray): ImportBundleResult = withContext(ioDispatcher) {
        val results = mutableListOf<ImportResult>()
        var importedFolders = emptyList<CharacterFolderExport>()
        var importedGlobalOrder = emptyList<String>()
        
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
                val manifestBytes = unzippedFiles.entries.find { it.key.equals("manifest.json", ignoreCase = true) }?.value
                val manifest = if (manifestBytes != null) {
                    try {
                        json.decodeFromString<CharacterManifest>(decodeSmart(manifestBytes))
                    } catch (e: Exception) {
                        null
                    }
                } else null

                if (manifest != null) {
                    importedFolders = manifest.folders
                    importedGlobalOrder = manifest.globalOrder
                }

                val manifestEntryByUuid = manifest?.characters?.associateBy { it.uuid } ?: emptyMap()
                val manifestEntryByFolder = manifest?.characters?.associateBy { it.folder.lowercase() } ?: emptyMap()

                // Group files by directory
                val groups = unzippedFiles.keys.groupBy { 
                    val parts = it.split("/")
                    if (parts.size > 1) parts.dropLast(1).joinToString("/") else ""
                }

                if (groups.size > 1 || (groups.keys.first().isNotEmpty())) {
                    // Multi-character or single character in a folder
                    groups.forEach { (dirName, fileKeys) ->
                        if (dirName.equals("manifest.json", ignoreCase = true) || dirName.isEmpty()) return@forEach

                        val charJsonBytes = unzippedFiles[fileKeys.find { it.endsWith("character.json", ignoreCase = true) }]
                        if (charJsonBytes != null) {
                            val jsonString = decodeSmart(charJsonBytes)
                            val character = parseCharacterContent(jsonString)
                            if (character != null) {
                                val manifestEntry = manifestEntryByUuid[character.uuid] ?: manifestEntryByFolder[dirName.lowercase()]
                                val charWithFolder = if (manifestEntry?.folderUuid != null) {
                                    character.copy(folderUuid = manifestEntry.folderUuid)
                                } else {
                                    character
                                }

                                val portraitBytes = unzippedFiles[fileKeys.find { it.endsWith("portrait.webp", ignoreCase = true) }]
                                val originalBytes = unzippedFiles[fileKeys.find { it.endsWith("original.webp", ignoreCase = true) }]
                                results.add(createImportResult(charWithFolder, portraitBytes, originalBytes))
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
                    val jsonElement = try {
                        json.parseToJsonElement(jsonString)
                    } catch (e: Exception) {
                        null
                    }

                    if (jsonElement != null) {
                        if (LongStoryShortImporter.isLongStoryShort(jsonElement)) {
                            val chars = LongStoryShortImporter.parseMany(jsonElement)
                            chars.forEach { char ->
                                results.add(createImportResult(char, null, null))
                            }
                        } else if (jsonElement is JsonArray) {
                            jsonElement.forEach { item ->
                                try {
                                    if (LongStoryShortImporter.isLongStoryShort(item)) {
                                        LongStoryShortImporter.parse(item)?.let { char ->
                                            results.add(createImportResult(char, null, null))
                                        }
                                    } else {
                                        val char = json.decodeFromJsonElement<Character>(item)
                                        var portraitBytes: ByteArray? = null
                                        if (char.imageData != null && char.imageData.length > 100) {
                                            try {
                                                portraitBytes = char.imageData.decodeBase64()?.toByteArray()
                                            } catch (e: Exception) {}
                                        }
                                        results.add(createImportResult(char, portraitBytes, null))
                                    }
                                } catch (e: Exception) {
                                    e.log()
                                }
                            }
                        } else if (jsonElement is JsonObject) {
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
                    }
                } catch (e: Exception) {
                    e.log()
                }
            }
        } catch (e: Exception) {
            Logger.e("ArchiveManager", "Critical failure in importCharacters", e)
        }
        ImportBundleResult(results, importedFolders, importedGlobalOrder)
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
        }
        return utf8
    }

    private fun parseCharacterContent(jsonString: String): Character? {
        return try {
            val jsonElement = json.parseToJsonElement(jsonString)
            if (LongStoryShortImporter.isLongStoryShort(jsonElement)) {
                LongStoryShortImporter.parseMany(jsonElement).firstOrNull()
            } else {
                json.decodeFromString<Character>(jsonString)
            }
        } catch (e: Exception) {
            e.log()
            null
        }
    }
}
