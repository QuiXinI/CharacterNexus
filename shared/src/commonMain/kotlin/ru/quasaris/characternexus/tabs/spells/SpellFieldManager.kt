package ru.quasaris.characternexus.tabs.spells

import androidx.compose.runtime.*
import ru.quasaris.characternexus.model.CharacterTab
import ru.quasaris.characternexus.model.DynamicContentBlock
import ru.quasaris.characternexus.model.DynamicSpellField
import ru.quasaris.characternexus.model.DynamicNoteState
import ru.quasaris.characternexus.tabs.DynamicContentParser
import ru.quasaris.characternexus.ui.CharacterDetailState
import ru.quasaris.characternexus.util.generateUuid

class SpellFieldManager(
    private val owner: CharacterDetailState,
    initial: List<DynamicSpellField>
) {
    var items by mutableStateOf(initial)
        private set

    private val index by derivedStateOf { items.associateBy { it.id } }

    operator fun get(id: String): DynamicSpellField? = index[id]

    fun upsert(spellField: DynamicSpellField) {
        if (spellField.id.isEmpty()) return
        val existing = index[spellField.id]
        if (existing == spellField) return
        items = if (existing != null) {
            items.map { if (it.id == spellField.id) spellField else it }
        } else {
            items + spellField
        }
    }

    fun create(title: String = "Заклинания"): DynamicSpellField {
        val block = DynamicSpellField(
            title = title,
            id = generateUuid()
        )
        upsert(block)
        return block
    }

    fun createTag(title: String = "Заклинания"): String =
        DynamicContentBlock.SpellFieldRef(create(title).id).toTag()

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

    fun deleteCompletely(spellFieldId: String) {
        if (spellFieldId.isEmpty()) return
        items = items.filterNot { it.id == spellFieldId }
        mutateNotes { DynamicContentParser.removeSpellFieldById(it, spellFieldId) }
    }

    fun normalize() {
        val referencedIds = mutableSetOf<String>()
        sections().forEach { (_, list) ->
            list.forEach { note ->
                referencedIds.addAll(DynamicContentParser.spellFieldIds(note.content))
            }
        }
        referencedIds.forEach { id ->
            if (index[id] == null) {
                upsert(DynamicSpellField(id = id, title = "Заклинания"))
            }
        }
    }
}
