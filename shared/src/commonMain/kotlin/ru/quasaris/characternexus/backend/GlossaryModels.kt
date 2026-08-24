package ru.quasaris.characternexus.backend

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class GameFeature(
    @SerialName("id") val id: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("level") val level: Int? = null,
    @SerialName("description") val description: String? = null
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
    @SerialName("description") val description: String? = null,
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
)

@Serializable
data class GameSubclass(
    @SerialName("id") val id: String? = null,
    @SerialName("class_id") val classId: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("system") val system: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("features") val features: List<GameFeature>? = null
)

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
