package ru.quasaris.characternexus.backend

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import ru.quasaris.characternexus.backend.storage.CharacterStorage
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.ioDispatcher
import ru.quasaris.characternexus.runBlockingPlatform
import ru.quasaris.characternexus.runBlockingPlatform

class CharacterRepository(
    private val storage: CharacterStorage,
    private val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + ioDispatcher)
) {
    private val _charactersSummaryState = MutableStateFlow<List<CharacterSummary>>(emptyList())
    val charactersSummaryState: StateFlow<List<CharacterSummary>> = _charactersSummaryState.asStateFlow()

    private val _foldersState = MutableStateFlow<List<CharacterFolder>>(emptyList())
    val foldersState: StateFlow<List<CharacterFolder>> = _foldersState.asStateFlow()

    private val _globalOrderState = MutableStateFlow<List<String>>(emptyList())
    val globalOrderState: StateFlow<List<String>> = _globalOrderState.asStateFlow()

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
        var state = storage.loadListState()
        
        // Recovery logic: if cache is empty but characters exist in storage, rebuild it
        if (state.characters.isEmpty()) {
            val uuids = storage.listCharacterUuids()
            if (uuids.isNotEmpty()) {
                val recovered = mutableListOf<CharacterSummary>()
                uuids.forEach { uuid ->
                    storage.loadCharacter(uuid)?.let { char ->
                        recovered.add(char.toSummary())
                    }
                }
                if (recovered.isNotEmpty()) {
                    state = state.copy(characters = recovered, globalOrder = recovered.map { it.uuid })
                    storage.saveListState(state)
                }
            }
        }
        
        // Ensure global order is initialized if missing
        if (state.globalOrder.isEmpty() && (state.characters.isNotEmpty() || state.folders.isNotEmpty())) {
            val newOrder = mutableListOf<String>()
            val foldersMap = state.folders.associateBy { it.uuid }
            
            // Add folders and their children
            state.folders.forEach { folder ->
                newOrder.add(folder.uuid)
                val folderChars = state.characters.filter { it.folderUuid == folder.uuid }
                newOrder.addAll(folderChars.map { it.uuid })
            }
            
            // Add root characters
            val rootChars = state.characters.filter { it.folderUuid == null || !foldersMap.containsKey(it.folderUuid) }
            newOrder.addAll(rootChars.map { it.uuid }.filter { it !in newOrder })
            
            state = state.copy(globalOrder = newOrder)
            storage.saveListState(state)
        }
        
        _charactersSummaryState.value = state.characters
        _foldersState.value = state.folders
        _globalOrderState.value = state.globalOrder
    }

    private fun saveListState() {
        appScope.launch {
            storage.saveListState(CharacterListState(
                characters = _charactersSummaryState.value,
                folders = _foldersState.value,
                globalOrder = _globalOrderState.value
            ))
        }
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
        
        val existingFolderUuid = currentSummaries.find { it.uuid == character.uuid }?.folderUuid
        val newSummary = character.toSummary(existingFolderUuid)
        
        if (index != -1) {
            currentSummaries[index] = newSummary
        } else {
            currentSummaries.add(newSummary)
            _globalOrderState.value = _globalOrderState.value + character.uuid
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
            saveListState()
        }
    }

    fun deleteCharacter(uuid: String) {
        appScope.launch {
            storage.deleteCharacter(uuid)
            fullCharactersCache.remove(uuid)
            _charactersSummaryState.value = _charactersSummaryState.value.filter { it.uuid != uuid }
            _globalOrderState.value = _globalOrderState.value.filter { it != uuid }
            saveListState()
        }
    }

    fun flush() {
        appScope.launch {
            fullCharactersCache.values.forEach {
                storage.saveCharacter(it)
            }
            saveListState()
            
            // Cleanup orphaned character folders on disk
            ImageManager.cleanupOrphanedCharacters(_charactersSummaryState.value.map { it.uuid })
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
            storage.saveListState(CharacterListState(
                characters = _charactersSummaryState.value,
                folders = _foldersState.value
            ))
            
            // Cleanup orphaned character folders on disk
            ImageManager.cleanupOrphanedCharacters(_charactersSummaryState.value.map { it.uuid })
        }
    }

    fun updateCharacters(characters: List<Character>) {
        characters.forEach { updateCharacter(it) }
    }

    fun updateSummariesOrder(uuids: List<String>) {
        // We need to ensure that folders always carry their children with them
        // uuids is the order of items visible in the UI (expanded)
        val fullOrder = mutableListOf<String>()
        val handled = mutableSetOf<String>()
        
        uuids.forEach { uuid ->
            if (uuid in handled) return@forEach
            fullOrder.add(uuid)
            handled.add(uuid)
            
            val folder = _foldersState.value.find { it.uuid == uuid }
            if (folder != null) {
                // Find all descendants recursively that weren't in the visible list (collapsed)
                addDescendantsRecursive(uuid, fullOrder, handled, visibleUuids = uuids.toSet())
            }
        }
        
        // Add anything completely missed
        _globalOrderState.value.forEach { if (it !in handled) fullOrder.add(it) }
        
        _globalOrderState.value = fullOrder
        saveListState()
    }
    
    private fun addDescendantsRecursive(parentUuid: String, result: MutableList<String>, handled: MutableSet<String>, visibleUuids: Set<String>) {
        // Character children
        val childChars = _charactersSummaryState.value.filter { it.folderUuid == parentUuid }
        childChars.forEach { 
            if (it.uuid !in visibleUuids && it.uuid !in handled) {
                result.add(it.uuid)
                handled.add(it.uuid)
            }
        }
        
        // Folder children
        val childFolders = _foldersState.value.filter { it.parentFolderUuid == parentUuid }
        childFolders.forEach { folder ->
            if (folder.uuid !in visibleUuids && folder.uuid !in handled) {
                result.add(folder.uuid)
                handled.add(folder.uuid)
                addDescendantsRecursive(folder.uuid, result, handled, visibleUuids)
            }
        }
    }

    fun createFolder(name: String, colorArgb: Int? = null) {
        val newFolder = CharacterFolder(name = name, colorArgb = colorArgb)
        _foldersState.value = _foldersState.value + newFolder
        _globalOrderState.value = listOf(newFolder.uuid) + _globalOrderState.value
        saveListState()
    }

    fun updateFolder(folder: CharacterFolder) {
        _foldersState.value = _foldersState.value.map { if (it.uuid == folder.uuid) folder else it }
        saveListState()
    }

    fun deleteFolder(folderUuid: String, deleteCharacters: Boolean) {
        if (deleteCharacters) {
            val charsToDelete = _charactersSummaryState.value.filter { it.folderUuid == folderUuid }
            charsToDelete.forEach { deleteCharacter(it.uuid) }
        } else {
            // Move characters to root
            _charactersSummaryState.value = _charactersSummaryState.value.map {
                if (it.folderUuid == folderUuid) it.copy(folderUuid = null) else it
            }
        }
        _foldersState.value = _foldersState.value.filter { it.uuid != folderUuid }
        _globalOrderState.value = _globalOrderState.value.filter { it != folderUuid }
        saveListState()
    }

    fun moveCharactersToFolder(uuids: List<String>, folderUuid: String?, afterUuid: String? = null) {
        _charactersSummaryState.value = _charactersSummaryState.value.map {
            if (it.uuid in uuids) it.copy(folderUuid = folderUuid) else it
        }
        
        // Re-calculate global order to keep children with their parent
        updateGlobalOrderAfterMove(uuids, folderUuid, afterUuid)
        saveListState()
    }

    fun moveFolderToFolder(folderUuid: String, newParentUuid: String?, afterUuid: String? = null) {
        if (folderUuid == newParentUuid) return // Can't move to self
        
        // Circular dependency check
        if (newParentUuid != null) {
            var curr: String? = newParentUuid
            while (curr != null) {
                if (curr == folderUuid) return // Prevent cycle
                curr = _foldersState.value.find { it.uuid == curr }?.parentFolderUuid
            }
        }
        
        _foldersState.value = _foldersState.value.map {
            if (it.uuid == folderUuid) it.copy(parentFolderUuid = newParentUuid) else it
        }
        
        // Also move in global order
        updateGlobalOrderAfterMove(listOf(folderUuid), newParentUuid, afterUuid)
        saveListState()
    }

    private fun updateGlobalOrderAfterMove(uuids: List<String>, targetFolderUuid: String?, afterUuid: String? = null) {
        val currentOrder = _globalOrderState.value.toMutableList()
        
        // Find all descendants if any of the moved items are folders
        val allMovedUuids = mutableListOf<String>()
        val queue = uuids.toMutableList()
        while(queue.isNotEmpty()){
            val current = queue.removeAt(0)
            if (current !in allMovedUuids) {
                allMovedUuids.add(current)
                // If it's a folder, find its character children and subfolders
                val childChars = _charactersSummaryState.value.filter { it.folderUuid == current }.map { it.uuid }
                val childFolders = _foldersState.value.filter { it.parentFolderUuid == current }.map { it.uuid }
                queue.addAll(childChars)
                queue.addAll(childFolders)
            }
        }

        currentOrder.removeAll { it in allMovedUuids }
        
        if (afterUuid != null) {
            // Find the last descendant of the afterUuid if it's a folder
            var lastDescendant = afterUuid
            fun findLast(p: String) {
                val chars = _charactersSummaryState.value.filter { it.folderUuid == p }.map { it.uuid }
                val folders = _foldersState.value.filter { it.parentFolderUuid == p }.map { it.uuid }
                
                // We want the last one in the current global order
                val children = (chars + folders).sortedBy { currentOrder.indexOf(it).let { if (it == -1) Int.MAX_VALUE else it } }
                val last = children.lastOrNull()
                if (last != null) {
                    lastDescendant = last
                    findLast(last)
                }
            }
            findLast(afterUuid)
            
            val index = currentOrder.indexOf(lastDescendant)
            if (index != -1) {
                currentOrder.addAll(index + 1, allMovedUuids)
            } else {
                currentOrder.addAll(allMovedUuids)
            }
        } else if (targetFolderUuid != null) {
            val folderIndex = currentOrder.indexOf(targetFolderUuid)
            if (folderIndex != -1) {
                // Insert after parent folder
                currentOrder.addAll(folderIndex + 1, allMovedUuids)
            } else {
                currentOrder.addAll(allMovedUuids)
            }
        } else {
            currentOrder.addAll(allMovedUuids)
        }
        _globalOrderState.value = currentOrder
    }
    
    fun toggleFolderExpansion(folderUuid: String) {
        _foldersState.value = _foldersState.value.map {
            if (it.uuid == folderUuid) it.copy(isExpanded = !it.isExpanded) else it
        }
        saveListState()
    }
}
