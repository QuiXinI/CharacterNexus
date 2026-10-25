package ru.quasaris.characternexus.backend.glossary

/**
 * Заготовка под автосборку листа персонажа.
 *
 * Идея: любая сущность глоссария умеет сказать, что она даёт персонажу на заданном
 * уровне (BuildGrant). Планировщик собирает выдачи по выбору игрока, а хук листа
 * персонажа применяет их к листу. Сам лист и применение — позже.
 */
enum class GrantKind { FEATURE, SPELL_LIST, SPELL_SLOTS, ALWAYS_PREPARED_SPELLS }

data class BuildGrant(
    val sourceType: GlossaryType,
    val sourceId: String,
    val sourceName: String,
    val kind: GrantKind,
    val level: Int?,
    val title: String,
    val description: String = "",
    val spellIds: List<String> = emptyList(),
    val slots: List<SlotCount> = emptyList()
)

/** Выбор игрока, из которого собирается лист. */
data class BuildSelection(
    val classId: String? = null,
    val classLevel: Int = 1,
    val subclassId: String? = null,
    val speciesId: String? = null,
    val featIds: List<String> = emptyList()
)

/** Источник выдач: класс, подкласс, вид или черта. */
interface CharacterBuildSource {
    fun grantsUpTo(level: Int): List<BuildGrant>
}

/** TODO сделать хук для автосборки: реализовать применение выдач к листу персонажа. */
interface CharacterSheetBuildHook {
    fun onGrantsCollected(selection: BuildSelection, grants: List<BuildGrant>)
}

private fun FeatureDef.toGrant(type: GlossaryType, id: String, owner: String) = BuildGrant(
    sourceType = type, sourceId = id, sourceName = owner, kind = GrantKind.FEATURE,
    level = level, title = name, description = description
)

fun ClassDefinition.asBuildSource(): CharacterBuildSource = object : CharacterBuildSource {
    override fun grantsUpTo(level: Int): List<BuildGrant> {
        val result = mutableListOf<BuildGrant>()
        features.filter { (it.level ?: 1) <= level }
            .mapTo(result) { it.toGrant(GlossaryType.CLASS, id, name) }
        if (spellcasting.type != SpellcastingType.NONE) {
            result += BuildGrant(
                GlossaryType.CLASS, id, name, GrantKind.SPELL_SLOTS, level, "Ячейки заклинаний",
                slots = SpellSlotCalculator.slotsFor(spellcasting, level)
            )
            result += BuildGrant(
                GlossaryType.CLASS, id, name, GrantKind.SPELL_LIST, null, "Список заклинаний",
                spellIds = spellcasting.spellIds
            )
        }
        return result
    }
}

fun SubclassDefinition.asBuildSource(): CharacterBuildSource = object : CharacterBuildSource {
    override fun grantsUpTo(level: Int): List<BuildGrant> {
        val result = mutableListOf<BuildGrant>()
        features.filter { (it.level ?: 1) <= level }
            .mapTo(result) { it.toGrant(GlossaryType.SUBCLASS, id, name) }
        spellGrants.filter { it.classLevel <= level }.mapTo(result) {
            BuildGrant(
                GlossaryType.SUBCLASS, id, name, GrantKind.ALWAYS_PREPARED_SPELLS, it.classLevel,
                "Всегда подготовленные заклинания", spellIds = it.spellIds
            )
        }
        return result
    }
}

fun SpeciesDefinition.asBuildSource(): CharacterBuildSource = object : CharacterBuildSource {
    override fun grantsUpTo(level: Int): List<BuildGrant> =
        features.filter { (it.level ?: 1) <= level }.map { it.toGrant(GlossaryType.SPECIES, id, name) }
}

fun FeatDefinition.asBuildSource(): CharacterBuildSource = object : CharacterBuildSource {
    override fun grantsUpTo(level: Int): List<BuildGrant> =
        features.filter { (it.level ?: 1) <= level }.map { it.toGrant(GlossaryType.FEAT, id, name) }
}

/** Собирает выдачи по выбору игрока. Применение к листу делает CharacterSheetBuildHook. */
class BuildPlanner(private val repository: GlossaryRepository) {
    fun collect(selection: BuildSelection): List<BuildGrant> {
        val sources = mutableListOf<CharacterBuildSource>()
        selection.classId?.let { id -> repository.classById(id)?.let { sources += it.asBuildSource() } }
        selection.subclassId?.let { id ->
            repository.subclasses().find { it.id == id }?.let { sources += it.asBuildSource() }
        }
        selection.speciesId?.let { id -> repository.species().find { it.id == id }?.let { sources += it.asBuildSource() } }
        val feats = repository.feats()
        selection.featIds.forEach { id -> feats.find { it.id == id }?.let { sources += it.asBuildSource() } }
        return sources.flatMap { it.grantsUpTo(selection.classLevel) }
    }
}
