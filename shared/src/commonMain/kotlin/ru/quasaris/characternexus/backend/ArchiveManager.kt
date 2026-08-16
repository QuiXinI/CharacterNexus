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

object ArchiveManager {
    const val EXPORT_EXTENSION = "charbook"
    
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    suspend fun exportCharacter(character: Character, targetPath: String) = withContext(ioDispatcher) {
        // Simple implementation: just save JSON for now, or use Okio Zip if available
        // Since multiplatform Zip is not easily available without extra libs, 
        // we might just export JSON or use a common Zip library.
        // For now, let's just save JSON to the path.
        try {
            val path = targetPath.toPath()
            platformFileSystem.write(path) {
                writeUtf8(json.encodeToString(character))
            }
        } catch (e: Exception) {
            e.log()
        }
    }

    suspend fun importCharacter(bytes: ByteArray): Character? = withContext(ioDispatcher) {
        try {
            val jsonString = bytes.decodeToString()
            // Try to detect if it's our format
            if (jsonString.contains("\"uuid\"") && jsonString.contains("\"name\"")) {
                return@withContext json.decodeFromString<Character>(jsonString)
            }
            // LongStoryShort support would need more logic here
        } catch (e: Exception) {
            e.log()
        }
        null
    }
}
