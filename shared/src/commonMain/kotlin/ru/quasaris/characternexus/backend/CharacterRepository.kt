package ru.quasaris.characternexus.backend

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import ru.quasaris.characternexus.backend.storage.CharacterStorage
import ru.quasaris.characternexus.Character
import ru.quasaris.characternexus.CharacterSummary
import ru.quasaris.characternexus.ioDispatcher
import ru.quasaris.characternexus.runBlockingPlatform

class CharacterRepository(
    private val storage: CharacterStorage,
    private val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + ioDispatcher)
) {
    private val _charactersSummaryState = MutableStateFlow<List<CharacterSummary>>(emptyList())
    val charactersSummaryState: StateFlow<List<CharacterSummary>> = _charactersSummaryState.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val fullCharactersCache = mutableMapOf<String, Character>()

    init {
        appScope.launch {
            loadSummaries()
            _isInitialized.value = true
        }
    }

    private suspend fun loadSummaries() {
        val summaries = storage.loadAllSummaries()
        _charactersSummaryState.value = summaries
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
        appScope.launch {
            fullCharactersCache.values.forEach {
                storage.saveCharacter(it)
            }
            storage.saveSummaries(_charactersSummaryState.value)
        }
    }

    /**
     * Synchronously writes all cached data to disk. 
     * Used on Desktop during application exit.
     */
    fun flushBlocking() {
        runBlockingPlatform {
            fullCharactersCache.values.forEach {
                storage.saveCharacter(it)
            }
            storage.saveSummaries(_charactersSummaryState.value)
        }
    }

    fun updateCharacters(characters: List<Character>) {
        characters.forEach { updateCharacter(it) }
    }
}
