package ru.quasaris.characternexus.tabs.glossary

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DtoProgressionTable(
    val headers: List<String> = emptyList(),
    val rows: List<List<String>> = emptyList()
)

@Serializable
data class DtoClassTraits(
    val title: String? = null,
    val lines: List<String> = emptyList(),
    val parsed: Map<String, String>? = null
)

@Serializable
data class DtoFeature(
    val name: String? = null,
    val type: String? = null,
    @SerialName("level_line") val levelLine: String? = null,
    val description: List<String> = emptyList()
)

@Serializable
data class DtoSpellcastingGroup(
    val group: String? = null,
    val items: List<String> = emptyList()
)

@Serializable
data class DtoSpellcasting(
    @SerialName("tab_title") val tabTitle: String? = null,
    val groups: List<DtoSpellcastingGroup> = emptyList()
)

@Serializable
data class DtoInvocationItem(
    val name: String? = null,
    val requirements: String? = null,
    val description: List<String> = emptyList()
)

@Serializable
data class DtoInvocations(
    @SerialName("tab_title") val tabTitle: String? = null,
    val intro: List<String> = emptyList(),
    val items: List<DtoInvocationItem> = emptyList()
)

@Serializable
data class DtoItemPlans(
    @SerialName("tab_title") val tabTitle: String? = null,
    val groups: List<DtoSpellcastingGroup> = emptyList()
)

@Serializable
data class DtoClassTab(
    val title: String? = null,
    @SerialName("progression_table") val progressionTable: DtoProgressionTable? = null,
    @SerialName("class_traits") val classTraits: DtoClassTraits? = null,
    val features: List<DtoFeature> = emptyList()
)

@Serializable
data class DtoClassData(
    val id: String? = null,
    val name: String? = null,
    @SerialName("class_tab") val classTab: DtoClassTab? = null,
    val spellcasting: DtoSpellcasting? = null,
    val invocations: DtoInvocations? = null,
    @SerialName("item_plans") val itemPlans: DtoItemPlans? = null,
    val subclass: List<String> = emptyList(),
    val description: List<String> = emptyList(),
    val features: List<DtoFeature> = emptyList()
)

@Serializable
data class DtoSubclassData(
    @SerialName("class_id") val classId: String? = null,
    @SerialName("class_name") val className: String? = null,
    val name: String? = null,
    val description: List<String> = emptyList(),
    val features: List<DtoFeature> = emptyList(),
    @SerialName("linked_subclasses") val linkedSubclasses: List<String> = emptyList()
)
