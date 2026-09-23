package ru.quasaris.characternexus.tabs.resources

import androidx.compose.runtime.*
import ru.quasaris.characternexus.backend.evaluateFormula
import ru.quasaris.characternexus.backend.parseFormulaParts
import ru.quasaris.characternexus.model.CharacterTab
import ru.quasaris.characternexus.model.DynamicContentBlock
import ru.quasaris.characternexus.model.DynamicContentBlock.Resource
import ru.quasaris.characternexus.model.DynamicNoteState
import ru.quasaris.characternexus.tabs.DynamicContentParser
import ru.quasaris.characternexus.ui.CharacterDetailState
import ru.quasaris.characternexus.util.generateUuid
import kotlin.math.abs

class ResourceManager(
    private val owner: CharacterDetailState,
    initial: List<Resource>
) {
    var items by mutableStateOf(initial)
        private set

    private val index by derivedStateOf { items.associateBy { it.id } }

    operator fun get(id: String): Resource? = index[id]
    fun upsert(resource: Resource) {
        if (resource.id.isEmpty()) return
        val existing = index[resource.id]
        if (existing == resource) return
        items = if (existing != null) {
            items.map { if (it.id == resource.id) resource else it }
        } else {
            items + resource
        }
        
        // Sync back to potions
        owner.potions = owner.potions.map { potion ->
            if (potion.quantity.id == resource.id) potion.copy(quantity = resource) else potion
        }
    }

    fun create(name: String = "Ресурс"): Resource {
        val res = Resource(name = name, current = "0", max = "0", id = generateUuid())
        upsert(res)
        return res
    }

    fun createTag(name: String = "Ресурс"): String =
        DynamicContentBlock.ResourceRef(create(name).id).toTag()

    private fun sections(): List<Pair<String, List<DynamicNoteState>>> = listOf(
        CharacterTab.NOTES.title to owner.notes,
        CharacterTab.SKILLS_FEATS.title to owner.skillsAndTraits,
        CharacterTab.INVENTORY.title to owner.inventory,
        CharacterTab.SPELLS.title to owner.spells,
        CharacterTab.BIO.title to owner.bioLongSections
    )

    fun usages(): Map<String, List<ResourceUsage>> {
        val notesUsage = ResourceMigration.collectUsages(sections()).toMutableMap()
        
        // Add potion usages
        owner.potions.forEach { potion ->
            val res = potion.quantity
            if (res.id.isNotEmpty()) {
                val usage = ResourceUsage(
                    noteId = potion.id,
                    noteTitle = potion.name,
                    section = "Зелья",
                    blockIndex = 0 // Potions only have one resource
                )
                val list = notesUsage.getOrPut(res.id) { mutableListOf() }.toMutableList()
                list.add(usage)
                notesUsage[res.id] = list
            }
        }
        
        return notesUsage
    }

    private fun idAt(noteId: String, blockIndex: Int): String? {
        val note = sections().firstNotNullOfOrNull { (_, list) -> list.firstOrNull { it.id == noteId } } ?: return null
        val blocks = ru.quasaris.characternexus.tabs.BlockContentParser.toBlocks(note.content)
        return when (val block = blocks.getOrNull(blockIndex)) {
            is DynamicContentBlock.ResourceRef -> block.id
            is DynamicContentBlock.Resource -> block.id.ifEmpty { null }
            else -> blocks.filterIsInstance<DynamicContentBlock.ResourceRef>().firstOrNull()?.id
                ?: blocks.filterIsInstance<DynamicContentBlock.Resource>().firstOrNull()?.id
        }
    }

    private fun mutateNote(noteId: String, transform: (String) -> String) {
        fun List<DynamicNoteState>.mutated() =
            map { if (it.id == noteId) it.copy(content = transform(it.content)) else it }
        owner.notes = owner.notes.mutated()
        owner.skillsAndTraits = owner.skillsAndTraits.mutated()
        owner.inventory = owner.inventory.mutated()
        owner.spells = owner.spells.mutated()
        owner.bioLongSections = owner.bioLongSections.mutated()
    }

    private fun dropIfUnused(id: String) {
        if (usages()[id].isNullOrEmpty()) items = items.filterNot { it.id == id }
    }

    fun linkConfig(resourceId: String, noteId: String, blockIndex: Int): ResourceLinkConfig {
        val usage = usages()
        val sharedWith = usage[resourceId].orEmpty()
            .filterNot { it.noteId == noteId && it.blockIndex == blockIndex }
        val candidates = items
            .filter { it.id != resourceId }
            .map { res -> ResourceLinkCandidate(res, usage[res.id].orEmpty()) }
            .sortedBy { it.resource.name.lowercase() }
        return ResourceLinkConfig(
            candidates = candidates,
            sharedWith = sharedWith,
            onLink = { targetId -> relink(noteId, blockIndex, targetId, resourceId) },
            onUnlink = { unlink(noteId, blockIndex, resourceId) }
        )
    }

    fun relink(noteId: String, blockIndex: Int, targetId: String, currentResourceId: String? = null) {
        val target = index[targetId] ?: return
        val oldId = currentResourceId ?: idAt(noteId, blockIndex)
        
        // Try notes first
        var foundInNotes = false
        val sections = sections()
        for ((_, list) in sections) {
            if (list.any { it.id == noteId }) {
                mutateNote(noteId) { DynamicContentParser.relinkResource(it, blockIndex, targetId, oldId) }
                foundInNotes = true
                break
            }
        }
        
        // If not in notes, check potions
        if (!foundInNotes) {
            owner.potions = owner.potions.map { potion ->
                if (potion.id == noteId) potion.copy(quantity = target) else potion
            }
        }
        
        if (oldId != null && oldId != targetId) dropIfUnused(oldId)
    }

    fun unlink(noteId: String, blockIndex: Int, currentResourceId: String? = null): Resource? {
        val id = currentResourceId ?: idAt(noteId, blockIndex) ?: return null
        val source = index[id] ?: return null
        val copy = source.copy(id = generateUuid())
        upsert(copy)
        
        // Try notes
        var foundInNotes = false
        val sections = sections()
        for ((_, list) in sections) {
            if (list.any { it.id == noteId }) {
                mutateNote(noteId) { DynamicContentParser.relinkResource(it, blockIndex, copy.id, id) }
                foundInNotes = true
                break
            }
        }
        
        // If not in notes, check potions
        if (!foundInNotes) {
            owner.potions = owner.potions.map { potion ->
                if (potion.id == noteId) potion.copy(quantity = copy) else potion
            }
        }
        
        return copy
    }

    /** Убрать одно размещение из текста. Данные удаляются, только если это было последнее размещение. */
    fun removePlacement(noteId: String, blockIndex: Int, targetResourceId: String? = null) {
        val id = targetResourceId ?: idAt(noteId, blockIndex)
        mutateNote(noteId) { DynamicContentParser.removeResource(it, blockIndex, id) }
        if (id != null) dropIfUnused(id)
    }

    /** Полностью удалить ресурс из списка ресурсов и из всех заметок/зелий. */
    fun deleteResourceCompletely(resourceId: String) {
        if (resourceId.isEmpty()) return
        items = items.filterNot { it.id == resourceId }
        
        fun List<DynamicNoteState>.cleaned() = map { note ->
            if (note.content.contains(resourceId)) {
                note.copy(content = DynamicContentParser.removeResourceById(note.content, resourceId))
            } else note
        }
        
        owner.notes = owner.notes.cleaned()
        owner.skillsAndTraits = owner.skillsAndTraits.cleaned()
        owner.inventory = owner.inventory.cleaned()
        owner.spells = owner.spells.cleaned()
        owner.bioLongSections = owner.bioLongSections.cleaned()
        
        owner.potions = owner.potions.mapNotNull { potion ->
            if (potion.quantity.id == resourceId) null
            else potion
        }
    }

    fun normalize() {
        val store = LinkedHashMap<String, Resource>()
        items.forEach { if (it.id.isNotEmpty()) store[it.id] = it }

        fun List<DynamicNoteState>.normalized() = map { ResourceMigration.normalizeNote(it, store) }
        owner.notes = owner.notes.normalized()
        owner.skillsAndTraits = owner.skillsAndTraits.normalized()
        owner.inventory = owner.inventory.normalized()
        owner.spells = owner.spells.normalized()
        owner.bioLongSections = owner.bioLongSections.normalized()
        
        // Sync potions and pick up new resources from them if they have IDs
        owner.potions = owner.potions.map { potion ->
            val res = potion.quantity
            if (res.id.isNotEmpty()) {
                val existing = store[res.id]
                if (existing == null) {
                    store[res.id] = res
                    potion
                } else if (existing != res) {
                    // Manager has different data, manager wins
                    potion.copy(quantity = existing)
                } else potion
            } else potion
        }

        val newItems = store.values.toList()
        if (newItems != items) items = newItems
    }

    fun applyRest(restType: String, statsMap: Map<String, String>) {
        normalize()
        items = items.map { restored(it, restType, statsMap) }
        
        // Also sync back to potions since they hold a direct copy of the resource
        owner.potions = owner.potions.map { potion ->
            val id = potion.quantity.id
            if (id.isNotEmpty()) {
                val updated = index[id] ?: potion.quantity
                potion.copy(quantity = updated)
            } else potion
        }
    }

    private fun restored(block: Resource, restType: String, statsMap: Map<String, String>): Resource {
        val recovery = when (restType) {
            "short" -> block.shortRest
            "long" -> block.longRest
            "dawn" -> block.dawnRest
            else -> "0"
        }
        val actualRecovery = if (restType == "long" && recovery == "0") block.shortRest else recovery
        if (actualRecovery == "0") return block

        val maxVal = evaluateFormula(block.max, statsMap)
        val curVal = block.current.toIntOrNull() ?: 0
        val isAll = actualRecovery.lowercase() == "all" || actualRecovery.lowercase() == "все"

        val newCur = if (isAll) {
            maxVal
        } else {
            val (flat, dice) = parseFormulaParts(actualRecovery, statsMap)
            var rolled = flat
            dice.forEach { part ->
                val sides = part.sides
                val count = abs(part.count)
                val sign = if (part.count >= 0) 1 else -1
                repeat(count) {
                    rolled += (1..sides).random() * sign
                }
            }
            minOf(maxVal, curVal + rolled)
        }
        return block.copy(current = newCur.toString())
    }
}
