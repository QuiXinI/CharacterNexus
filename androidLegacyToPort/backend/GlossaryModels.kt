package ru.quasaris.characters.master.backend

import com.google.gson.annotations.SerializedName

data class GameFeature(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("level") val level: Int? = null,
    @SerializedName("description") val description: String? = null
)

data class TableSchemaColumn(
    @SerializedName("key") val key: String? = null,
    @SerializedName("label") val label: String? = null,
    @SerializedName("type") val type: String? = null
)

data class TableSchema(
    @SerializedName("columns") val columns: List<TableSchemaColumn>? = null
)

data class GameTable(
    @SerializedName("id") val id: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("schema") val schema: TableSchema? = null,
    @SerializedName("rows") val rows: List<Map<String, Any>>? = null
)

data class GameClass(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("system") val system: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("primary_ability") val primaryAbility: String? = null,
    @SerializedName("hit_die") val hitDie: String? = null,
    @SerializedName("progression_table") val progressionTable: GameTable? = null,
    @SerializedName("features") val features: List<GameFeature>? = null
)

data class GameSubclass(
    @SerializedName("id") val id: String? = null,
    @SerializedName("class_id") val classId: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("system") val system: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("features") val features: List<GameFeature>? = null
)

data class GameSpecies(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("system") val system: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("creature_type") val creatureType: String? = null,
    @SerializedName("size") val size: String? = null,
    @SerializedName("speed") val speed: Any? = null,
    @SerializedName("tables") val tables: List<GameTable>? = null,
    @SerializedName("features") val features: List<GameFeature>? = null,
    @SerializedName("image") val image: String? = null
)

data class GameFeat(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("system") val system: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("prerequisites") val prerequisites: String? = null,
    @SerializedName("repeatable") val repeatable: Boolean? = null,
    @SerializedName("features") val features: List<GameFeature>? = null
)
