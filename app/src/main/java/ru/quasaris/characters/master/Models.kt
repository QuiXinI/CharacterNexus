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
    override val formula: String = "",
    val hasAdvantage: Boolean = false
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

enum class SpellMode {
    TEXT,
    CARDS
}

enum class CasterType(val displayName: String) {
    NONE("Нет"),
    FULL("Заклинатель"),
    HALF("Полузаклинатель"),
    THIRD("Особый заклинатель")
}

enum class SlotAlignment {
    LEFT, CENTER, RIGHT
}

enum class SlotFillDirection {
    LTR, CENTER, RTL
}

enum class MagicAttackType {
    ATTACK,
    SAVE
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

data class DynamicNoteState(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val content: String = "",
    val isExpanded: Boolean = true,
    val isLocked: Boolean = false
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
    val showNotes: Boolean = false,
    val isMagic: Boolean = false,
    val magicType: MagicAttackType = MagicAttackType.ATTACK
)

data class SpellSettings(
    val isMagicEnabled: Boolean = true,
    val spellAttackBonus: String = "",
    val spellAttackBonuses: List<AttackBonus> = emptyList(),
    val spellSaveDcBonus: String = "",
    val spellSaveDcBonuses: List<AttackBonus> = emptyList(),
    val spellcastingAbility: Attribute = Attribute.NONE,
    val spellMode: SpellMode = SpellMode.TEXT,
    val casterType: CasterType = CasterType.NONE,
    val isMulticlass: Boolean = false,
    val fullCasterLevel: Int = 0,
    val halfCasterLevel: Int = 0,
    val thirdCasterLevel: Int = 0,
    val isPactEnabled: Boolean = false,
    val specialSlots: List<SpecialSlotSettings> = emptyList(),
    val overrideSlots: Map<Float, Int> = emptyMap(),
    val pactSlotLevel: Float = 1f,
    val pactSlotsCount: Int = 0,
    val usedSlots: Map<Float, Int> = emptyMap(), // Key 1-9 are levels (Long Rest)
    val usedSlotsShortRest: Map<Float, Int> = emptyMap() // Key 0 is Pact (if not merged), 1-9 are levels
)

data class SpecialSlotSettings(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val level: Float = 1f,
    val count: Int = 0,
    val restoreOnShortRest: Boolean = false
)


data class Wallet(
    val platinum: Double = 0.0,
    val gold: Double = 0.0,
    val electrum: Double = 0.0,
    val silver: Double = 0.0,
    val copper: Double = 0.0,
    val visibleCurrencies: List<String> = listOf("platinum", "gold", "silver", "copper")
)

data class BioShortField(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val value: String = "",
    val widthRatio: Float = 0.5f, // 0.5 for 1/2, 0.33f for 1/3
    val isCustom: Boolean = false
)

data class BioSection(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val content: String = "",
    val isExpanded: Boolean = true
)

data class Character(

    val id: Int,
    val name: String,
    val characterClass: String,
    val order: String,
    val imageData: String? = null,
    val avatarUrl: String? = null,
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
    val attacks: List<AttackEntry> = emptyList(),
    val notes: List<DynamicNoteState> = listOf(DynamicNoteState()),
    val skillsAndTraits: List<DynamicNoteState> = listOf(
        DynamicNoteState(title = "Умения"),
        DynamicNoteState(title = "Черты", content = "**_Черты происхождения:_**\n\n\n---\n**_Общие черты_**\n")
    ),
    val inventory: List<DynamicNoteState> = listOf(
        DynamicNoteState(title = "Снаряжение"),
        DynamicNoteState(title = "Сокровища"),
        DynamicNoteState(title = "Экипировано", content = "\n\n---\n**_Настройки_**\n1. \n2. \n3. ")
    ),
    val spells: List<DynamicNoteState> = listOf(DynamicNoteState(title = "Заговоры")) + (1..9).map {
        DynamicNoteState(title = "$it уровень")
    },
    val bioShortFields: List<BioShortField> = listOf(
        BioShortField(title = "Предыстория", widthRatio = 0.5f),
        BioShortField(title = "Мировоззрение", widthRatio = 0.5f),
        BioShortField(title = "Рост", widthRatio = 0.33f),
        BioShortField(title = "Вес", widthRatio = 0.33f),
        BioShortField(title = "Возраст", widthRatio = 0.33f),
        BioShortField(title = "Кожа", widthRatio = 0.33f),
        BioShortField(title = "Глаза", widthRatio = 0.33f),
        BioShortField(title = "Волосы", widthRatio = 0.33f)
    ),
    val bioLongSections: List<DynamicNoteState> = listOf(
        DynamicNoteState(title = "Предыстория персонажа"),
        DynamicNoteState(title = "Союзники и организации"),
        DynamicNoteState(title = "Враги и организации"),
        DynamicNoteState(title = "Черты характера"),
        DynamicNoteState(title = "Идеалы"),
        DynamicNoteState(title = "Привязанности"),
        DynamicNoteState(title = "Слабости")
    ),
    val spellSettings: SpellSettings = SpellSettings(),
    val wallet: Wallet = Wallet(),
    val cropX: Float? = null,
    val cropY: Float? = null,
    val cropW: Float? = null,
    val cropH: Float? = null
)


data class ShieldEntry(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String = "",
    override val formula: String = ""
) : FormulaEntry
