package ru.quasaris.characternexus.tabs.folders

import androidx.compose.runtime.*
import ru.quasaris.characternexus.model.CharacterTab
import ru.quasaris.characternexus.model.DynamicContentBlock
import ru.quasaris.characternexus.model.DynamicFolder
import ru.quasaris.characternexus.model.DynamicNoteState
import ru.quasaris.characternexus.tabs.DynamicContentParser
import ru.quasaris.characternexus.ui.CharacterDetailState
import ru.quasaris.characternexus.util.generateUuid

class FolderManager(
    private val owner: CharacterDetailState,
    initial: List<DynamicFolder>
) {
    var items by mutableStateOf(initial)
        private set

    private val index by derivedStateOf { items.associateBy { it.id } }

    operator fun get(id: String): DynamicFolder? = index[id]

    fun upsert(folder: DynamicFolder) {
        if (folder.id.isEmpty()) return
        val existing = index[folder.id]
        if (existing == folder) return
        items = if (existing != null) {
            items.map { if (it.id == folder.id) folder else it }
        } else {
            items + folder
        }
    }

    fun create(title: String = "Папка"): DynamicFolder {
        val block = DynamicFolder(
            title = title,
            id = generateUuid()
        )
        upsert(block)
        return block
    }

    fun createTag(title: String = "Папка"): String =
        DynamicContentBlock.FolderRef(create(title).id).toTag()

    private fun sections(): List<Pair<String, List<DynamicNoteState>>> {
        val list = mutableListOf(
            "Заметки" to owner.bioLongSections,
            CharacterTab.SKILLS_FEATS.title to owner.skillsAndTraits,
            CharacterTab.INVENTORY.title to owner.inventory,
            CharacterTab.SPELLS.title to owner.spells
        )
        owner.customTabs.forEach { tab ->
            list.add(tab.title to tab.content)
        }
        return list
    }

    private fun mutateNotes(transform: (String) -> String) {
        fun List<DynamicNoteState>.mutated() = map { note ->
            val updated = transform(note.content)
            if (updated != note.content) note.copy(content = updated) else note
        }
        owner.bioLongSections = owner.bioLongSections.mutated()
        owner.skillsAndTraits = owner.skillsAndTraits.mutated()
        owner.inventory = owner.inventory.mutated()
        owner.spells = owner.spells.mutated()
        owner.customTabs = owner.customTabs.map { tab ->
            tab.copy(content = tab.content.mutated())
        }
    }

    fun deleteCompletely(folderId: String) {
        if (folderId.isEmpty()) return
        items = items.filterNot { it.id == folderId }
        mutateNotes { DynamicContentParser.removeFolderById(it, folderId) }
    }

    fun normalize() {
        val referencedIds = mutableSetOf<String>()
        sections().forEach { (_, list) ->
            list.forEach { note ->
                referencedIds.addAll(DynamicContentParser.folderIds(note.content))
            }
        }
        referencedIds.forEach { id ->
            if (index[id] == null) {
                upsert(DynamicFolder(id = id, title = "Папка"))
            }
        }
    }
}
