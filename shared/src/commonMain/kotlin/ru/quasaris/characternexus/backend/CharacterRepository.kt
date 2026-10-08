package ru.quasaris.characternexus.backend

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import ru.quasaris.characternexus.backend.storage.CharacterStorage
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.ioDispatcher
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

        if (state.globalOrder.isEmpty() && (state.characters.isNotEmpty() || state.folders.isNotEmpty())) {
            val newOrder = mutableListOf<String>()
            val foldersMap = state.folders.associateBy { it.uuid }

            state.folders.forEach { folder ->
                newOrder.add(folder.uuid)
                val folderChars = state.characters.filter { it.folderUuid == folder.uuid }
                newOrder.addAll(folderChars.map { it.uuid })
            }

            val rootChars = state.characters.filter { it.folderUuid == null || !foldersMap.containsKey(it.folderUuid) }
            newOrder.addAll(rootChars.map { it.uuid }.filter { it !in newOrder })

            state = state.copy(globalOrder = newOrder)
            storage.saveListState(state)
        }

        _charactersSummaryState.value = state.characters
        _foldersState.value = state.folders
        _globalOrderState.value = state.globalOrder

        cleanupDuplicateFolders()
    }

    fun cleanupDuplicateFolders() {
        val currentFolders = _foldersState.value
        val nameMap = mutableMapOf<String, CharacterFolder>()
        val uuidMapping = mutableMapOf<String, String>()

        val cleanedFolders = mutableListOf<CharacterFolder>()

        currentFolders.forEach { folder ->
            val key = folder.name.trim().lowercase()
            if (key.isBlank()) {
                cleanedFolders.add(folder)
            } else {
                val existing = nameMap[key]
                if (existing == null) {
                    nameMap[key] = folder
                    cleanedFolders.add(folder)
                } else {
                    uuidMapping[folder.uuid] = existing.uuid
                }
            }
        }

        if (uuidMapping.isNotEmpty()) {
            _charactersSummaryState.value = _charactersSummaryState.value.map { char ->
                val newFolderUuid = char.folderUuid?.let { uuidMapping[it] ?: it }
                if (newFolderUuid != char.folderUuid) char.copy(folderUuid = newFolderUuid) else char
            }

            _foldersState.value = cleanedFolders.map { f ->
                val newParentUuid = f.parentFolderUuid?.let { uuidMapping[it] ?: it }
                if (newParentUuid != f.parentFolderUuid) f.copy(parentFolderUuid = newParentUuid) else f
            }

            _globalOrderState.value = _globalOrderState.value.filter { it !in uuidMapping.keys }

            saveListState()
        }
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
        val loaded = fullCharactersCache[uuid] ?: storage.loadCharacter(uuid)?.also {
            fullCharactersCache[uuid] = it
        }
        val currentFolderUuid = _charactersSummaryState.value.find { it.uuid == uuid }?.folderUuid
        return loaded?.copy(folderUuid = currentFolderUuid ?: loaded.folderUuid)
    }

    fun updateCharacter(character: Character) {
        fullCharactersCache[character.uuid] = character

        val currentSummaries = _charactersSummaryState.value.toMutableList()
        val index = currentSummaries.indexOfFirst { it.uuid == character.uuid }

        val existingFolderUuid = currentSummaries.find { it.uuid == character.uuid }?.folderUuid
        val newSummary = character.toSummary(existingFolderUuid)

        if (index != -1) {
            currentSummaries[index] = newSummary
            _charactersSummaryState.value = currentSummaries
        } else {
            currentSummaries.add(newSummary)
            _charactersSummaryState.value = currentSummaries
            if (_globalOrderState.value.none { it == character.uuid }) {
                _globalOrderState.value = _globalOrderState.value + character.uuid
            }
            if (newSummary.folderUuid != null) {
                updateGlobalOrderAfterMove(listOf(character.uuid), newSummary.folderUuid)
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

            ImageManager.cleanupOrphanedCharacters(_charactersSummaryState.value.map { it.uuid })
        }
    }

    fun flushBlocking() {
        runBlockingPlatform {
            fullCharactersCache.values.forEach {
                storage.saveCharacter(it)
            }
            storage.saveListState(CharacterListState(
                characters = _charactersSummaryState.value,
                folders = _foldersState.value,
                globalOrder = _globalOrderState.value
            ))

            ImageManager.cleanupOrphanedCharacters(_charactersSummaryState.value.map { it.uuid })
        }
    }

    fun updateCharacters(characters: List<Character>) {
        characters.forEach { updateCharacter(it) }
    }

    fun updateSummariesOrder(uuids: List<String>) {
        val fullOrder = mutableListOf<String>()
        val handled = mutableSetOf<String>()

        uuids.forEach { uuid ->
            if (uuid in handled) return@forEach
            fullOrder.add(uuid)
            handled.add(uuid)

            val folder = _foldersState.value.find { it.uuid == uuid }
            if (folder != null) {
                addDescendantsRecursive(uuid, fullOrder, handled, visibleUuids = uuids.toSet())
            }
        }

        _globalOrderState.value.forEach { if (it !in handled) fullOrder.add(it) }

        _globalOrderState.value = fullOrder
        saveListState()
    }

    private fun addDescendantsRecursive(parentUuid: String, result: MutableList<String>, handled: MutableSet<String>, visibleUuids: Set<String>) {
        val childChars = _charactersSummaryState.value.filter { it.folderUuid == parentUuid }
        childChars.forEach {
            if (it.uuid !in visibleUuids && it.uuid !in handled) {
                result.add(it.uuid)
                handled.add(it.uuid)
            }
        }

        val childFolders = _foldersState.value.filter { it.parentFolderUuid == parentUuid }
        childFolders.forEach { folder ->
            if (folder.uuid !in visibleUuids && folder.uuid !in handled) {
                result.add(folder.uuid)
                handled.add(folder.uuid)
                addDescendantsRecursive(folder.uuid, result, handled, visibleUuids)
            }
        }
    }

    fun createFolder(name: String, colorArgb: Int? = null): CharacterFolder {
        val newFolder = CharacterFolder(name = name, colorArgb = colorArgb)
        _foldersState.value = _foldersState.value + newFolder
        _globalOrderState.value = listOf(newFolder.uuid) + _globalOrderState.value
        saveListState()
        return newFolder
    }

    fun addFolder(folder: CharacterFolder) {
        if (_foldersState.value.none { it.uuid == folder.uuid }) {
            _foldersState.value = _foldersState.value + folder
            _globalOrderState.value = listOf(folder.uuid) + _globalOrderState.value
            saveListState()
        }
    }

    fun updateFolder(folder: CharacterFolder) {
        _foldersState.value = _foldersState.value.map { if (it.uuid == folder.uuid) folder else it }
        saveListState()
    }

    fun deleteFolder(folderUuid: String, deleteCharacters: Boolean) {
        val allFolderUuids = getAllSubfolderUuids(folderUuid)

        if (deleteCharacters) {
            val charsToDelete = _charactersSummaryState.value.filter { it.folderUuid in allFolderUuids }
            charsToDelete.forEach { deleteCharacter(it.uuid) }

            _foldersState.value = _foldersState.value.filter { it.uuid !in allFolderUuids }
            _globalOrderState.value = _globalOrderState.value.filter { it !in allFolderUuids }
        } else {
            val parentUuidOfDeleted = _foldersState.value.find { it.uuid == folderUuid }?.parentFolderUuid

            _foldersState.value = _foldersState.value
                .filter { it.uuid != folderUuid }
                .map { if (it.parentFolderUuid == folderUuid) it.copy(parentFolderUuid = parentUuidOfDeleted) else it }

            _charactersSummaryState.value = _charactersSummaryState.value.map {
                if (it.folderUuid == folderUuid) it.copy(folderUuid = parentUuidOfDeleted) else it
            }

            _globalOrderState.value = _globalOrderState.value.filter { it != folderUuid }
        }

        saveListState()
    }

    private fun getAllSubfolderUuids(folderUuid: String): Set<String> {
        val result = mutableSetOf(folderUuid)
        val children = _foldersState.value.filter { it.parentFolderUuid == folderUuid }
        children.forEach { child ->
            result.addAll(getAllSubfolderUuids(child.uuid))
        }
        return result
    }

    fun moveCharactersToFolder(uuids: List<String>, folderUuid: String?, beforeUuid: String? = null) {
        _charactersSummaryState.value = _charactersSummaryState.value.map {
            if (it.uuid in uuids) it.copy(folderUuid = folderUuid) else it
        }

        updateGlobalOrderAfterMove(uuids, folderUuid, beforeUuid)
        saveListState()
    }

    fun moveFolderToFolder(folderUuid: String, newParentUuid: String?, beforeUuid: String? = null) {
        if (folderUuid == newParentUuid) return // Can't move to self

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

        updateGlobalOrderAfterMove(listOf(folderUuid), newParentUuid, beforeUuid)
        saveListState()
    }
    private fun updateGlobalOrderAfterMove(uuids: List<String>, targetFolderUuid: String?, beforeUuid: String? = null) {
        val currentOrder = _globalOrderState.value.toMutableList()

        val allMovedUuids = mutableListOf<String>()
        val queue = uuids.toMutableList()
        while (queue.isNotEmpty()) {
            val current = queue.removeAt(0)
            if (current !in allMovedUuids) {
                allMovedUuids.add(current)
                val childChars = _charactersSummaryState.value.filter { it.folderUuid == current }.map { it.uuid }
                val childFolders = _foldersState.value.filter { it.parentFolderUuid == current }.map { it.uuid }
                queue.addAll(childChars)
                queue.addAll(childFolders)
            }
        }

        currentOrder.removeAll { it in allMovedUuids }

        // beforeUuid == null означает «в конец родителя». Раньше для папки здесь вставлялось
        // сразу после самой папки, т.е. в её НАЧАЛО — отсюда «прыжок наверх».
        val endIndex = if (targetFolderUuid != null) endOfFolderIndex(currentOrder, targetFolderUuid) else currentOrder.size
        val insertIndex = if (beforeUuid != null) {
            currentOrder.indexOf(beforeUuid).takeIf { it != -1 } ?: endIndex
        } else {
            endIndex
        }

        currentOrder.addAll(insertIndex, allMovedUuids)
        _globalOrderState.value = currentOrder
    }

    private fun parentOfId(id: String): String? =
        _charactersSummaryState.value.firstOrNull { it.uuid == id }?.folderUuid
            ?: _foldersState.value.firstOrNull { it.uuid == id }?.parentFolderUuid

    private fun isInsideFolder(id: String, folderUuid: String): Boolean {
        var cur = parentOfId(id)
        var guard = 0
        while (cur != null && guard++ < 128) {
            if (cur == folderUuid) return true
            cur = parentOfId(cur)
        }
        return false
    }

    /** Индекс сразу после последнего потомка папки (или после самой папки, если она пуста). */
    private fun endOfFolderIndex(order: List<String>, folderUuid: String): Int {
        var last = order.indexOf(folderUuid)
        if (last == -1) return order.size
        order.forEachIndexed { i, id ->
            if (i > last && isInsideFolder(id, folderUuid)) last = i
        }
        return last + 1
    }

    fun toggleFolderExpansion(folderUuid: String) {
        _foldersState.value = _foldersState.value.map {
            if (it.uuid == folderUuid) it.copy(isExpanded = !it.isExpanded) else it
        }
        saveListState()
    }
}