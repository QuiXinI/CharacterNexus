package ru.quasaris.characternexus.tabs.infoblocks

import androidx.compose.runtime.*
import ru.quasaris.characternexus.model.CharacterTab
import ru.quasaris.characternexus.model.DynamicContentBlock
import ru.quasaris.characternexus.model.DynamicContentBlock.InfoBlock
import ru.quasaris.characternexus.model.DynamicContentBlock.Resource
import ru.quasaris.characternexus.model.DynamicNoteState
import ru.quasaris.characternexus.tabs.DynamicContentParser
import ru.quasaris.characternexus.ui.CharacterDetailState
import ru.quasaris.characternexus.util.generateUuid

class InfoBlockManager(
    private val owner: CharacterDetailState,
    initial: List<InfoBlock>
) {
    var items by mutableStateOf(initial)
        private set

    private val index by derivedStateOf { items.associateBy { it.id } }

    operator fun get(id: String): InfoBlock? = index[id]

    fun upsert(infoBlock: InfoBlock) {
        if (infoBlock.id.isEmpty()) return
        val existing = index[infoBlock.id]
        if (existing == infoBlock) return
        items = if (existing != null) {
            items.map { if (it.id == infoBlock.id) infoBlock else it }
        } else {
            items + infoBlock
        }
    }

    fun create(
        title: String = "Новый инфоблок",
        link: String? = null,
        description: String = ""
    ): InfoBlock {
        val block = InfoBlock(
            title = title,
            link = link,
            description = description,
            id = generateUuid()
        )
        upsert(block)
        return block
    }

    fun createTag(title: String = "Новый инфоблок"): String =
        DynamicContentBlock.InfoBlockRef(create(title).id).toTag()

    private fun sections(): List<Pair<String, List<DynamicNoteState>>> = listOf(
        CharacterTab.NOTES.title to owner.notes,
        CharacterTab.SKILLS_FEATS.title to owner.skillsAndTraits,
        CharacterTab.INVENTORY.title to owner.inventory,
        CharacterTab.SPELLS.title to owner.spells,
        CharacterTab.BIO.title to owner.bioLongSections
    )

    private fun mutateNotes(transform: (String) -> String) {
        fun List<DynamicNoteState>.mutated() = map { note ->
            val updated = transform(note.content)
            if (updated != note.content) note.copy(content = updated) else note
        }
        owner.notes = owner.notes.mutated()
        owner.skillsAndTraits = owner.skillsAndTraits.mutated()
        owner.inventory = owner.inventory.mutated()
        owner.spells = owner.spells.mutated()
        owner.bioLongSections = owner.bioLongSections.mutated()
    }

    fun deleteCompletely(infoBlockId: String) {
        if (infoBlockId.isEmpty()) return
        items = items.filterNot { it.id == infoBlockId }
        mutateNotes { DynamicContentParser.removeInfoBlockById(it, infoBlockId) }
    }

    fun convertToResource(infoBlockId: String): Resource? {
        val block = index[infoBlockId] ?: return null
        
        val resource = Resource(
            name = block.title,
            current = "0",
            max = "0",
            link = block.link,
            notes = block.description,
            id = infoBlockId
        )

        // Remove from infoblocks list
        items = items.filterNot { it.id == infoBlockId }

        // Add to resources manager
        owner.resourceManager.upsert(resource)

        // Replace tag in all notes from InfoBlockRef to ResourceRef
        mutateNotes { DynamicContentParser.replaceInfoBlockRefWithResourceRef(it, infoBlockId) }

        return resource
    }

    fun normalize() {
        val store = LinkedHashMap<String, InfoBlock>()
        items.forEach { if (it.id.isNotEmpty()) store[it.id] = it }

        fun normalizeNote(note: DynamicNoteState): DynamicNoteState {
            val original = note.content
            if (original.isEmpty()) return note

            val content = DynamicContentParser.rewriteInlineInfoBlocks(original) { inline ->
                val id = inline.id.ifEmpty { generateUuid() }
                if (id !in store) store[id] = inline.copy(id = id)
                DynamicContentBlock.InfoBlockRef(id)
            }

            DynamicContentParser.infoBlockIds(content).forEach { id ->
                if (id !in store) {
                    store[id] = InfoBlock(title = "Инфоблок", id = id)
                }
            }

            return if (content == original) note else note.copy(content = content)
        }

        fun List<DynamicNoteState>.normalized() = map { normalizeNote(it) }

        owner.notes = owner.notes.normalized()
        owner.skillsAndTraits = owner.skillsAndTraits.normalized()
        owner.inventory = owner.inventory.normalized()
        owner.spells = owner.spells.normalized()
        owner.bioLongSections = owner.bioLongSections.normalized()

        val newItems = store.values.toList()
        if (newItems != items) items = newItems
    }
}
