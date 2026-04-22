package ru.quasaris.characters.master

import java.util.UUID

interface FormulaEntry {
    val id: String
    val name: String
    val formula: String
}

data class ArmorClassEntry(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String = "",
    override val formula: String = ""
) : FormulaEntry

data class InitiativeEntry(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String = "",
    override val formula: String = ""
) : FormulaEntry

data class Character(
    val id: Int,
    val name: String,
    val characterClass: String,
    val order: String,
    val imageData: String? = null,
    val level: String = "1",
    val strength: String = "10",
    val dexterity: String = "10",
    val constitution: String = "10",
    val intelligence: String = "10",
    val wisdom: String = "10",
    val charisma: String = "10",
    val armorClassEntries: List<ArmorClassEntry> = listOf(ArmorClassEntry(name = "Базовый КД", formula = "10 + [ЛОВ]")),
    val activeArmorClassId: String? = armorClassEntries.firstOrNull()?.id,
    val initiativeEntries: List<InitiativeEntry> = listOf(InitiativeEntry(name = "Базовая Инициатива", formula = "[ЛОВ]")),
    val activeInitiativeId: String? = initiativeEntries.firstOrNull()?.id
)
