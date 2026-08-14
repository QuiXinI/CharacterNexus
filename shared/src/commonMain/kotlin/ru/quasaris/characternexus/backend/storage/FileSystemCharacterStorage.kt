package ru.quasaris.characternexus.backend.storage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.Path
import ru.quasaris.characternexus.getAppDataDir
import ru.quasaris.characternexus.model.Character
import ru.quasaris.characternexus.model.CharacterSummary
import ru.quasaris.characternexus.platformFileSystem

import ru.quasaris.characternexus.ioDispatcher
import ru.quasaris.characternexus.util.ensureNomedia

class FileSystemCharacterStorage : CharacterStorage {

    private val fileSystem = platformFileSystem
    private val baseDir = getAppDataDir().div("Characters")
    private val cacheFile = getAppDataDir().div("characters_cache.json")
    
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    init {
        baseDir.ensureNomedia()
    }

    override suspend fun saveCharacter(character: Character): Unit = withContext(ioDispatcher) {
        val charDir = baseDir.div(character.uuid)
        charDir.ensureNomedia()
        
        val charFile = charDir.div("character.json")
        fileSystem.write(charFile) {
            writeUtf8(json.encodeToString(character))
        }
    }

    override suspend fun loadCharacter(uuid: String): Character? = withContext(ioDispatcher) {
        val charFile = baseDir.div(uuid).div("character.json")
        if (!fileSystem.exists(charFile)) return@withContext null
        
        try {
            fileSystem.read(charFile) {
                val content = readUtf8()
                json.decodeFromString<Character>(content)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun deleteCharacter(uuid: String) = withContext(ioDispatcher) {
        val charDir = baseDir.div(uuid)
        if (fileSystem.exists(charDir)) {
            fileSystem.deleteRecursively(charDir)
        }
    }

    override suspend fun loadAllSummaries(): List<CharacterSummary> = withContext(ioDispatcher) {
        if (!fileSystem.exists(cacheFile)) {
            return@withContext rebuildCache()
        }
        
        try {
            fileSystem.read(cacheFile) {
                val content = readUtf8()
                json.decodeFromString<List<CharacterSummary>>(content)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            rebuildCache()
        }
    }

    private suspend fun rebuildCache(): List<CharacterSummary> = withContext(ioDispatcher) {
        val uuids = listCharacterUuids()
        val summaries = uuids.mapNotNull { uuid ->
            loadCharacter(uuid)?.toSummary()
        }
        saveSummaries(summaries)
        summaries
    }

    override suspend fun saveSummaries(summaries: List<CharacterSummary>): Unit = withContext(ioDispatcher) {
        val parent = cacheFile.parent
        parent?.ensureNomedia()
        
        fileSystem.write(cacheFile) {
            writeUtf8(json.encodeToString(summaries))
        }
    }

    override suspend fun listCharacterUuids(): List<String> = withContext(ioDispatcher) {
        if (!fileSystem.exists(baseDir)) return@withContext emptyList()
        fileSystem.list(baseDir).filter { fileSystem.metadata(it).isDirectory }.map { it.name }
    }

    override suspend fun saveImage(uuid: String, fileName: String, bytes: ByteArray): String = withContext(ioDispatcher) {
        val charDir = baseDir.div(uuid)
        charDir.ensureNomedia()
        
        val imageFile = charDir.div(fileName)
        fileSystem.write(imageFile) {
            write(bytes)
        }
        imageFile.toString()
    }

    override suspend fun getImagePath(uuid: String, fileName: String): String? = withContext(ioDispatcher) {
        val imageFile = baseDir.div(uuid).div(fileName)
        if (fileSystem.exists(imageFile)) imageFile.toString() else null
    }

    override suspend fun deleteImage(uuid: String, fileName: String) = withContext(ioDispatcher) {
        val imageFile = baseDir.div(uuid).div(fileName)
        if (fileSystem.exists(imageFile)) {
            fileSystem.delete(imageFile)
        }
    }
}
