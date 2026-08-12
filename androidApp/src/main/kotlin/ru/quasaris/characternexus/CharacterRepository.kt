package ru.quasaris.characternexus

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import ru.quasaris.characternexus.backend.storage.AndroidCharacterStorage
import ru.quasaris.characternexus.backend.storage.CharacterStorage
import ru.quasaris.characternexus.model.Character
import ru.quasaris.characternexus.model.CharacterSummary

class CharacterRepository(
    private val context: Context,
    private val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    private val storage: CharacterStorage = AndroidCharacterStorage(context)
    private val sharedPreferences = context.getSharedPreferences("character_prefs", Context.MODE_PRIVATE)
    private val charactersKey = "CHARACTERS_LIST"

    private val _charactersSummaryState = MutableStateFlow<List<CharacterSummary>>(emptyList())
    val charactersSummaryState: StateFlow<List<CharacterSummary>> = _charactersSummaryState.asStateFlow()

    private val fullCharactersCache = mutableMapOf<String, Character>()

    init {
        appScope.launch {
            loadSummaries()
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
}
