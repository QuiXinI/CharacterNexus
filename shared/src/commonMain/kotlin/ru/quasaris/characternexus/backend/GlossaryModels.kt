package ru.quasaris.characternexus.backend

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

fun JsonElement.toSafeString(): String = when (this) {
    is JsonPrimitive -> content
    is JsonArray -> joinToString("\n") { (it as? JsonPrimitive)?.content ?: it.toString() }
    else -> toString()
}

@Serializable
data class GameFeature(
    @SerialName("id") val id: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("level") val level: Int? = null,
    @SerialName("description") val description: JsonElement? = null,
    @SerialName("type") val type: String? = null,
    @SerialName("level_line") val levelLine: String? = null,
    @SerialName("tables") val tables: List<ProgressionTable>? = null
) {
    val descriptionText: String get() = description?.toSafeString() ?: ""
}

@Serializable
data class ProgressionTable(
    @SerialName("headers") val headers: List<String>? = null,
    @SerialName("rows") val rows: List<List<String>>? = null
)

@Serializable
data class BecomingClass(
    @SerialName("title") val title: String? = null,
    @SerialName("lines") val lines: List<String>? = null
)

@Serializable
data class ClassTraits(
    @SerialName("title") val title: String? = null,
    @SerialName("lines") val lines: List<String>? = null,
    @SerialName("parsed") val parsed: Map<String, String>? = null
)

@Serializable
data class ClassTab(
    @SerialName("title") val title: String? = null,
    @SerialName("progression_table") val progressionTable: ProgressionTable? = null,
    @SerialName("becoming_class") val becomingClass: BecomingClass? = null,
    @SerialName("class_traits") val classTraits: ClassTraits? = null,
    @SerialName("features") val features: List<GameFeature>? = null
)

@Serializable
data class SpellcastingGroup(
    @SerialName("group") val group: String? = null,
    @SerialName("items") val items: List<String>? = null
)

@Serializable
data class Spellcasting(
    @SerialName("tab_title") val tabTitle: String? = null,
    @SerialName("groups") val groups: List<SpellcastingGroup>? = null
)

@Serializable
data class InvocationItem(
    @SerialName("name") val name: String? = null,
    @SerialName("requirements") val requirements: String? = null,
    @SerialName("description") val description: List<String>? = null
)

@Serializable
data class Invocations(
    @SerialName("tab_title") val tabTitle: String? = null,
    @SerialName("intro") val intro: List<String>? = null,
    @SerialName("items") val items: List<InvocationItem>? = null
)

@Serializable
data class ItemPlans(
    @SerialName("tab_title") val tabTitle: String? = null,
    @SerialName("groups") val groups: List<SpellcastingGroup>? = null
)

@Serializable
data class TableSchemaColumn(
    @SerialName("key") val key: String? = null,
    @SerialName("label") val label: String? = null,
    @SerialName("type") val type: String? = null
)

@Serializable
data class TableSchema(
    @SerialName("columns") val columns: List<TableSchemaColumn>? = null
)

@Serializable
data class GameTable(
    @SerialName("id") val id: String? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("schema") val schema: TableSchema? = null,
    @SerialName("rows") val rows: List<Map<String, JsonElement>>? = null
)

@Serializable
data class SkillProficiencyInfo(
    @SerialName("choose") val choose: Int? = null,
    @SerialName("from") val from: List<String>? = null
)

@Serializable
data class StartingEquipmentInfo(
    @SerialName("options") val options: List<String>? = null
)

@Serializable
data class MulticlassingInfo(
    @SerialName("prerequisites") val prerequisites: List<String>? = null,
    @SerialName("proficiencies") val proficiencies: List<String>? = null
)

@Serializable
data class GameClass(
    @SerialName("id") val id: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("system") val system: String? = null,
    @SerialName("description") val description: JsonElement? = null,
    @SerialName("source_file") val sourceFile: String? = null,
    
    // Modern structure
    @SerialName("class_tab") val classTab: ClassTab? = null,
    @SerialName("spellcasting") val spellcasting: Spellcasting? = null,
    @SerialName("invocations") val invocations: Invocations? = null,
    @SerialName("item_plans") val itemPlans: ItemPlans? = null,
    @SerialName("subclass") val subclasses: List<String>? = null,

    // Legacy compatibility (optional)
    @SerialName("primary_ability") val primaryAbility: String? = null,
    @SerialName("hit_die") val hitDie: String? = null,
    @SerialName("hit_dice") val hitDice: String? = null,
    @SerialName("hp_at_1st_level") val hpAt1stLevel: String? = null,
    @SerialName("hp_at_higher_levels") val hpAtHigherLevels: String? = null,
    @SerialName("headers") val headers: List<TableSchemaColumn>? = null,
    @SerialName("progression_table") val progressionTable: List<Map<String, JsonElement>>? = null,
    @SerialName("saving_throw_proficiencies") val savingThrowProficiencies: List<String>? = null,
    @SerialName("skill_proficiencies") val skillProficiencies: SkillProficiencyInfo? = null,
    @SerialName("weapon_proficiencies") val weaponProficiencies: List<String>? = null,
    @SerialName("armor_proficiencies") val armorProficiencies: List<String>? = null,
    @SerialName("starting_equipment") val startingEquipment: StartingEquipmentInfo? = null,
    @SerialName("multiclassing") val multiclassing: MulticlassingInfo? = null,
    @SerialName("features") val features: List<GameFeature>? = null
) {
    val descriptionText: String get() = description?.toSafeString() ?: ""
}

@Serializable
data class GameSubclass(
    @SerialName("id") val id: String? = null,
    @SerialName("class_id") val classId: String? = null,
    @SerialName("class_name") val className: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("system") val system: String? = null,
    @SerialName("description") val description: JsonElement? = null,
    @SerialName("features") val features: List<GameFeature>? = null,
    @SerialName("linked_subclasses") val linkedSubclasses: List<String>? = null
) {
    val descriptionText: String get() = description?.toSafeString() ?: ""
}

@Serializable
data class GameSpecies(
    @SerialName("id") val id: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("system") val system: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("creature_type") val creatureType: String? = null,
    @SerialName("size") val size: String? = null,
    @SerialName("speed") val speed: JsonElement? = null,
    @SerialName("tables") val tables: List<GameTable>? = null,
    @SerialName("features") val features: List<GameFeature>? = null,
    @SerialName("image") val image: String? = null
)

@Serializable
data class GameFeat(
    @SerialName("id") val id: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("system") val system: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("prerequisites") val prerequisites: String? = null,
    @SerialName("repeatable") val repeatable: Boolean? = null,
    @SerialName("features") val features: List<GameFeature>? = null
)
