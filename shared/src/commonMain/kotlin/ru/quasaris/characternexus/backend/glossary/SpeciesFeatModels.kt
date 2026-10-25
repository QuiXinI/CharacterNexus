package ru.quasaris.characternexus.backend.glossary

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Вид (раса). image — путь или ссылка, формат определяет отображающий код. */
@Serializable
data class SpeciesDefinition(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String = "",
    @SerialName("system") val system: String = "5.5",
    @SerialName("description") val description: String = "",
    @SerialName("creature_type") val creatureType: String = "",
    @SerialName("size") val size: String = "",
    @SerialName("speed") val speed: String = "",
    @SerialName("image") val image: String = "",
    @SerialName("features") val features: List<FeatureDef> = emptyList(),
    @SerialName("tables") val tables: List<GlossaryTable> = emptyList(),
    @SerialName("source") val source: String = "",
    @SerialName("source_module_id") val sourceModuleId: String? = null
)

/** Черта. category — свободный текст («Черта происхождения», «Общая черта» и т.п.). */
@Serializable
data class FeatDefinition(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String = "",
    @SerialName("system") val system: String = "5.5",
    @SerialName("category") val category: String = "",
    @SerialName("prerequisites") val prerequisites: String = "",
    @SerialName("repeatable") val repeatable: Boolean = false,
    @SerialName("description") val description: String = "",
    @SerialName("features") val features: List<FeatureDef> = emptyList(),
    @SerialName("source") val source: String = "",
    @SerialName("source_module_id") val sourceModuleId: String? = null
)
