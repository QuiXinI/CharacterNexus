package ru.quasaris.characternexus.tabs.resources

import ru.quasaris.characternexus.model.Character
import ru.quasaris.characternexus.model.DynamicContentBlock
import ru.quasaris.characternexus.model.DynamicNoteState
import ru.quasaris.characternexus.tabs.DynamicContentParser
import ru.quasaris.characternexus.util.generateUuid

data class ResourceUsage(
    val noteId: String,
    val noteTitle: String,
    val section: String,
    val blockIndex: Int
) {
    val label: String get() = if (noteTitle.isBlank()) section else "$section › $noteTitle"
}

data class ResourceLinkCandidate(
    val resource: DynamicContentBlock.Resource,
    val usages: List<ResourceUsage>
)

object ResourceMigration {
    fun normalizeNote(
        note: DynamicNoteState,
        store: MutableMap<String, DynamicContentBlock.Resource>
    ): DynamicNoteState {
        val original = note.content
        if (original.isEmpty()) return note

        val content = DynamicContentParser.rewriteInlineResources(original) { inline ->
            val id = inline.id.ifEmpty { generateUuid() }
            if (id !in store) store[id] = inline.copy(id = id)
            DynamicContentBlock.ResourceRef(id)
        }

        DynamicContentParser.resourceIds(content).forEach { id ->
            if (id !in store) {
                store[id] = DynamicContentBlock.Resource(name = "Ресурс", current = "0", max = "0", id = id)
            }
        }

        return if (content == original) note else note.copy(content = content)
    }

    fun migrate(character: Character): Character {
        val store = LinkedHashMap<String, DynamicContentBlock.Resource>()
        character.resources.forEach { if (it.id.isNotEmpty()) store[it.id] = it }

        fun List<DynamicNoteState>.normalized() = map { normalizeNote(it, store) }

        val migrated = character.copy(
            notes = character.notes.normalized(),
            skillsAndTraits = character.skillsAndTraits.normalized(),
            inventory = character.inventory.normalized(),
            spells = character.spells.normalized(),
            bioLongSections = character.bioLongSections.normalized()
        )
        return migrated.copy(resources = store.values.toList())
    }

    fun collectUsages(
        sections: List<Pair<String, List<DynamicNoteState>>>
    ): Map<String, List<ResourceUsage>> {
        val result = LinkedHashMap<String, MutableList<ResourceUsage>>()
        for ((sectionTitle, notes) in sections) {
            for (note in notes) {
                if (note.content.isEmpty()) continue
                ru.quasaris.characternexus.tabs.BlockContentParser.toBlocks(note.content).forEachIndexed { index, block ->
                    val id = when (block) {
                        is DynamicContentBlock.ResourceRef -> block.id
                        is DynamicContentBlock.Resource -> block.id
                        else -> ""
                    }
                    if (id.isNotEmpty()) {
                        result.getOrPut(id) { mutableListOf() }
                            .add(ResourceUsage(note.id, note.title, sectionTitle, index))
                    }
                }
            }
        }
        return result
    }
}
