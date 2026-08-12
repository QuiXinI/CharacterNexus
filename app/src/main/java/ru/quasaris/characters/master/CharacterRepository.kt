package ru.quasaris.characters.master

import android.content.Context
import android.util.Log
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import ru.quasaris.characters.master.backend.GsonFactory
import ru.quasaris.characters.master.backend.ImageManager
import ru.quasaris.characters.master.backend.storage.AndroidCharacterStorage
import ru.quasaris.characters.master.backend.storage.CharacterStorage
import java.io.File

class CharacterRepository(
    private val context: Context,
    private val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    private val storage: CharacterStorage = AndroidCharacterStorage(context)
    private val sharedPreferences = context.getSharedPreferences("character_prefs", Context.MODE_PRIVATE)
    private val legacyGson = GsonFactory.create()
    private val charactersKey = "CHARACTERS_LIST"

    private val _charactersSummaryState = MutableStateFlow<List<CharacterSummary>>(emptyList())
    val charactersSummaryState: StateFlow<List<CharacterSummary>> = _charactersSummaryState.asStateFlow()

    private val fullCharactersCache = mutableMapOf<String, Character>()

    init {
        appScope.launch {
            migrateIfNeeded()
            loadSummaries()
        }
    }

    private suspend fun migrateIfNeeded() {
        val legacyJson = sharedPreferences.getString(charactersKey, null)
        if (legacyJson != null) {
            Log.d("CharacterRepository", "Found legacy data, starting migration...")
            try {
                val type = object : TypeToken<MutableList<Character>>() {}.type
                val legacyCharacters: List<Character> = legacyGson.fromJson(legacyJson, type)
                
                legacyCharacters.forEach { char ->
                    // Gson might load uuid as null if it was missing in JSON
                    val existingUuid = try { char.uuid } catch (e: Exception) { null }
                    val uuid = if (existingUuid.isNullOrBlank()) java.util.UUID.randomUUID().toString() else existingUuid
                    val migratedChar = char.copy(uuid = uuid)
                    
                    // Move images if they exist in legacy global folders
                    migratedChar.imageData?.let { imgId ->
                        if (imgId.isNotBlank()) {
                            moveLegacyImages(uuid, imgId)
                        }
                    }
                    
                    storage.saveCharacter(migratedChar)
                }
                
                sharedPreferences.edit().remove(charactersKey).apply()
                Log.d("CharacterRepository", "Migration completed successfully")
            } catch (e: Exception) {
                Log.e("CharacterRepository", "Migration failed", e)
            }
        }
    }

    private suspend fun moveLegacyImages(uuid: String, imageId: String) = withContext(Dispatchers.IO) {
        // Only move if it's actually in a legacy location.
        // If imageId == uuid, it might already be in the new location.
        
        val originalFile = ImageManager.getOriginalFile(context, imageId)
        val portraitFile = ImageManager.getPortraitFile(context, imageId)
        val thumbFile = ImageManager.getThumbnailFile(context, imageId)
        
        val newOriginal = storage.getImagePath(uuid, "original.webp")
        val newPortrait = storage.getImagePath(uuid, "portrait.webp")
        val newThumb = storage.getImagePath(uuid, "thumbnail.webp")

        if (originalFile.exists() && originalFile.absolutePath != newOriginal) {
            storage.saveImage(uuid, "original.webp", originalFile.readBytes())
            originalFile.delete()
        }
        if (portraitFile.exists() && portraitFile.absolutePath != newPortrait) {
            storage.saveImage(uuid, "portrait.webp", portraitFile.readBytes())
            portraitFile.delete()
        }
        if (thumbFile.exists() && thumbFile.absolutePath != newThumb) {
            storage.saveImage(uuid, "thumbnail.webp", thumbFile.readBytes())
            thumbFile.delete()
        }
    }

    private suspend fun loadSummaries() {
        val summaries = storage.loadAllSummaries()
        if (summaries.isEmpty()) {
            rebuildCache()
        } else {
            _charactersSummaryState.value = summaries
        }
    }

    private suspend fun rebuildCache() = withContext(Dispatchers.IO) {
        val uuids = storage.listCharacterUuids()
        val summaries = uuids.mapNotNull { uuid ->
            storage.loadCharacter(uuid)?.toSummary()
        }
        _charactersSummaryState.value = summaries
        storage.saveSummaries(summaries)
    }

    fun loadCharacters(): List<CharacterSummary> = _charactersSummaryState.value

    suspend fun getFullCharacter(uuid: String): Character? {
        return fullCharactersCache[uuid] ?: storage.loadCharacter(uuid)?.also {
            fullCharactersCache[uuid] = it
        }
    }

    fun updateCharacter(character: Character) {
        fullCharactersCache[character.uuid] = character
        
        val currentSummaries = _charactersSummaryState.value.toMutableList()
        val index = currentSummaries.indexOfFirst { it.uuid == character.uuid }
        val newSummary = character.toSummary()
        
        if (index != -1) {
            currentSummaries[index] = newSummary
        } else {
            currentSummaries.add(newSummary)
        }
        _charactersSummaryState.value = currentSummaries
        
        // Handle image moving for newly created characters with legacy-style images
        character.imageData?.let { imgId ->
            if (imgId.length < 50) { // Simple check if it's an ID and not Base64
                appScope.launch { moveLegacyImages(character.uuid, imgId) }
            }
        }
        
        saveCharacterDebounced(character)
    }

    private val saveJobs = mutableMapOf<String, Job>()

    private fun saveCharacterDebounced(character: Character) {
        saveJobs[character.uuid]?.cancel()
        saveJobs[character.uuid] = appScope.launch {
            delay(500)
            storage.saveCharacter(character)
            storage.saveSummaries(_charactersSummaryState.value)
        }
    }

    fun deleteCharacter(uuid: String) {
        appScope.launch {
            storage.deleteCharacter(uuid)
            fullCharactersCache.remove(uuid)
            val updatedSummaries = _charactersSummaryState.value.filter { it.uuid != uuid }
            _charactersSummaryState.value = updatedSummaries
            storage.saveSummaries(updatedSummaries)
        }
    }

    fun flush() {
        runBlocking {
            fullCharactersCache.values.forEach {
                storage.saveCharacter(it)
            }
            storage.saveSummaries(_charactersSummaryState.value)
        }
    }

    fun updateCharacters(characters: List<Character>) {
        characters.forEach { updateCharacter(it) }
    }

    fun saveCharacters(characters: List<Character>) {
        updateCharacters(characters)
    }
}
