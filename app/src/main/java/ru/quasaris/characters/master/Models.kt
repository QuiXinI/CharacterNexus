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

data class SpeedEntry(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String = "",
    override val formula: String = ""
) : FormulaEntry

enum class CharacterTab(val title: String) {
    STATS("Характеристики"),
    ATTACKS("Атаки"),
    SKILLS_FEATS("Умения/Черты"),
    INVENTORY("Инвентарь"),
    NOTES("Заметки"),
    BIO("Био"),
    SPELLS("Заклинания")
}

enum class Attribute(val fullName: String, val shortName: String) {
    STRENGTH("Сила", "СИЛ"),
    DEXTERITY("Ловкость", "ЛОВ"),
    CONSTITUTION("Телосложение", "ТЕЛ"),
    INTELLIGENCE("Интеллект", "ИНТ"),
    WISDOM("Мудрость", "МУД"),
    CHARISMA("Харизма", "ХАР"),
    NONE("Нет", "НЕТ")
}

data class AttackBonus(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val formula: String = ""
)

data class DamageBonus(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val formula: String = "",
    val damageType: String = ""
)

enum class StatBonusType {
    SAVING_THROW,
    ABILITY_CHECK
}

data class StatBonus(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val formula: String = "",
    val attribute: Attribute = Attribute.STRENGTH,
    val type: StatBonusType = StatBonusType.SAVING_THROW
)

data class SkillBonus(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val formula: String = "",
    val skillName: String = ""
)

data class AttackEntry(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val isProficient: Boolean = false,
    val attribute: Attribute = Attribute.NONE,
    val attackBonus: Int = 0,
    val attackBonuses: List<AttackBonus> = emptyList(),
    val damageFormula: String = "",
    val damageType: String = "",
    val damageBonus: Int = 0,
    val damageBonuses: List<DamageBonus> = emptyList(),
    val notes: String = "",
    val showNotes: Boolean = false
)


data class Character(
    val id: Int,
    val name: String,
    val characterClass: String,
    val order: String,
    val imageData: String? = null,
    val level: String = "1",
    val experience: String = "0",
    val strength: String = "10",
    val dexterity: String = "10",
    val constitution: String = "10",
    val intelligence: String = "10",
    val wisdom: String = "10",
    val charisma: String = "10",
    val strengthProficient: Boolean = false,
    val dexterityProficient: Boolean = false,
    val constitutionProficient: Boolean = false,
    val intelligenceProficient: Boolean = false,
    val wisdomProficient: Boolean = false,
    val charismaProficient: Boolean = false,
    val armorClassEntries: List<ArmorClassEntry> = listOf(ArmorClassEntry(name = "Базовый КД", formula = "10 + [ЛОВ]")),
    val activeArmorClassId: String? = armorClassEntries.firstOrNull()?.id,
    val initiativeEntries: List<InitiativeEntry> = listOf(InitiativeEntry(name = "Базовая Инициатива", formula = "[ЛОВ]")),
    val activeInitiativeId: String? = initiativeEntries.firstOrNull()?.id,
    val speedEntries: List<SpeedEntry> = listOf(SpeedEntry(name = "Базовая Скорость", formula = "30")),
    val activeSpeedId: String? = speedEntries.firstOrNull()?.id,
    val maxHp: String = "0",
    val currentHp: String = "0",
    val tempHp: String = "0",
    val proficiencyBonus: String = "[НАСТ БМ]",
    val selectedConditions: List<String> = emptyList(),
    val exhaustion: Int = 0,
    val isShieldActive: Boolean = false,
    val shieldEntries: List<ShieldEntry> = listOf(ShieldEntry(name = "Базовый Щит", formula = "2")),
    val activeShieldId: String? = shieldEntries.firstOrNull()?.id,
    val skilledProficiencies: List<String> = emptyList(), // Track proficient skills
    val skilledExpertise: List<String> = emptyList(), // Track expertise skills
    val statBonuses: List<StatBonus> = emptyList(),
    val skillBonuses: List<SkillBonus> = emptyList(),
    val themeSeedColorArgb: Int? = null,
    val attacks: List<AttackEntry> = emptyList()
)

data class ShieldEntry(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String = "",
    override val formula: String = ""
) : FormulaEntry
