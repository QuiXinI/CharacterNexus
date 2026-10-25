package ru.quasaris.characternexus.backend.glossary

import ru.quasaris.characternexus.backend.SpellbookManager
import ru.quasaris.characternexus.model.SpellCard

/** Уровень заклинания как число (в SpellCard он хранится строкой). */
fun SpellCard.levelInt(): Int = level.toIntOrNull() ?: 0

/** Строка списка глоссария. */
data class GlossaryListEntry(
    val type: GlossaryType,
    val id: String,
    val name: String,
    val subtitle: String,
    val source: String
)

/**
 * Единая точка доступа UI к данным глоссария. Все методы блокирующие
 * (чтение маленьких JSON-файлов), вызывать можно из remember.
 */
class GlossaryRepository(
    private val spellbookManager: SpellbookManager,
    val store: GlossaryStore = GlossaryStore()
) {
    // --- Чтение ---

    fun classes(): List<ClassDefinition> =
        store.loadAll(GlossaryType.CLASS, ClassDefinition.serializer()).sortedBy { it.name }

    fun classById(id: String): ClassDefinition? = classes().find { it.id == id }

    fun subclasses(): List<SubclassDefinition> =
        store.loadAll(GlossaryType.SUBCLASS, SubclassDefinition.serializer()).sortedBy { it.name }

    fun subclassesOf(classId: String): List<SubclassDefinition> = subclasses().filter { it.classId == classId }

    fun species(): List<SpeciesDefinition> =
        store.loadAll(GlossaryType.SPECIES, SpeciesDefinition.serializer()).sortedBy { it.name }

    fun feats(): List<FeatDefinition> =
        store.loadAll(GlossaryType.FEAT, FeatDefinition.serializer()).sortedBy { it.name }

    fun magicItems(): List<MagicItem> =
        store.loadAll(GlossaryType.MAGIC_ITEM, MagicItem.serializer()).sortedBy { it.name }

    fun allSpells(): List<SpellCard> = spellbookManager.loadSpells()

    // --- Запись ---

    fun save(value: ClassDefinition) = store.save(GlossaryType.CLASS, value.id, value, ClassDefinition.serializer())
    fun save(value: SubclassDefinition) = store.save(GlossaryType.SUBCLASS, value.id, value, SubclassDefinition.serializer())
    fun save(value: SpeciesDefinition) = store.save(GlossaryType.SPECIES, value.id, value, SpeciesDefinition.serializer())
    fun save(value: FeatDefinition) = store.save(GlossaryType.FEAT, value.id, value, FeatDefinition.serializer())
    fun save(value: MagicItem) = store.save(GlossaryType.MAGIC_ITEM, value.id, value, MagicItem.serializer())

    /** Удаляет сущность. При удалении класса удаляются и его подклассы. */
    fun delete(type: GlossaryType, id: String) {
        if (type == GlossaryType.CLASS) subclassesOf(id).forEach { store.delete(GlossaryType.SUBCLASS, it.id) }
        if (type == GlossaryType.SPELL) spellbookManager.deleteSpell(id) else store.delete(type, id)
    }

    // --- Списки заклинаний ---

    private fun spellClassIndex(spells: List<SpellCard>): Map<String, Set<String>> =
        spells.associate { it.id to it.glossaryClassIds.toSet() }

    private val spellOrder = compareBy<SpellCard>({ it.levelInt() }, { it.name })

    fun spellsForClass(cls: ClassDefinition): List<SpellCard> {
        val spells = allSpells()
        val ids = SpellListResolver.classSpellIds(cls, spellClassIndex(spells))
        return spells.filter { it.id in ids }.sortedWith(spellOrder)
    }

    fun spellsForSubclass(sub: SubclassDefinition): List<SpellCard> {
        val ids = SpellListResolver.subclassSpellIds(sub)
        return allSpells().filter { it.id in ids }.sortedWith(spellOrder)
    }

    // --- Строки списков ---

    fun entries(type: GlossaryType): List<GlossaryListEntry> = when (type) {
        GlossaryType.CLASS -> {
            val subs = subclasses().groupBy { it.classId }
            classes().map {
                val n = subs[it.id]?.size ?: 0
                GlossaryListEntry(type, it.id, it.name, if (n > 0) "Подклассов: $n" else "", it.source)
            }
        }
        GlossaryType.SPECIES -> species().map {
            GlossaryListEntry(type, it.id, it.name, listOf(it.creatureType, it.size).filter { s -> s.isNotBlank() }.joinToString(" · "), it.source)
        }
        GlossaryType.FEAT -> feats().map {
            GlossaryListEntry(type, it.id, it.name, it.category, it.source)
        }
        GlossaryType.MAGIC_ITEM -> magicItems().map {
            GlossaryListEntry(type, it.id, it.name, "${it.kind.displayName} · ${it.rarity.displayName}", it.source)
        }
        GlossaryType.SUBCLASS -> subclasses().map {
            GlossaryListEntry(type, it.id, it.name, classById(it.classId)?.name.orEmpty(), it.source)
        }
        GlossaryType.SPELL -> allSpells().map {
            GlossaryListEntry(type, it.id, it.name, "Уровень ${it.level}", it.source)
        }
    }
}
