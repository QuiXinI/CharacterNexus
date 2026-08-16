package ru.quasaris.characternexus.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.quasaris.characternexus.util.generateUuid

@Serializable
sealed interface FormulaEntry {
    val id: String
    val name: String
    val formula: String
    val bonuses: List<AttackBonus>
}

@Serializable
data class ArmorClassEntry(
    override val id: String = generateUuid(),
    override val name: String = "",
    override val formula: String = "",
    override val bonuses: List<AttackBonus> = emptyList()
) : FormulaEntry

@Serializable
data class InitiativeEntry(
    override val id: String = generateUuid(),
    override val name: String = "",
    override val formula: String = "",
    val hasAdvantage: Boolean = false,
    override val bonuses: List<AttackBonus> = emptyList()
) : FormulaEntry

@Serializable
data class SpeedEntry(
    override val id: String = generateUuid(),
    override val name: String = "",
    override val formula: String = "",
    override val bonuses: List<AttackBonus> = emptyList()
) : FormulaEntry

@Serializable
data class ClassEntry(
    val id: String = generateUuid(),
    val className: CharacterClass = CharacterClass.FIGHTER,
    val subclass: String = "",
    val level: Int = 1
)

@Serializable
enum class CharacterTab(val title: String) {
    STATS("Характеристики"),
    ATTACKS("Атаки"),
    SKILLS_FEATS("Умения/Черты"),
    INVENTORY("Инвентарь"),
    NOTES("Заметки"),
    BIO("Био"),
    SPELLS("Заклинания")
}

@Serializable
enum class Attribute(val fullName: String, val shortName: String) {
    STRENGTH("Сила", "СИЛ"),
    DEXTERITY("Ловкость", "ЛОВ"),
    CONSTITUTION("Телосложение", "ТЕЛ"),
    INTELLIGENCE("Интеллект", "ИНТ"),
    WISDOM("Мудрость", "МУД"),
    CHARISMA("Харизма", "ХАР"),
    NONE("Нет", "НЕТ")
}

@Serializable
enum class SpellMode {
    TEXT,
    HYBRID,
    CARDS
}

@Serializable
enum class CasterType(val displayName: String) {
    NONE("Нет"),
    FULL("Заклинатель"),
    HALF("Полузаклинатель"),
    THIRD("Особый заклинатель")
}

@Serializable
enum class SlotAlignment {
    LEFT, CENTER, RIGHT
}

@Serializable
enum class SlotFillDirection {
    LTR, CENTER, RTL
}

@Serializable
enum class AppThemeMode {
    M3, OFF, CHARACTER
}

@Serializable
enum class ExportFormat {
    WEBP, PNG, JPG
}

@Serializable
data class Condition(val name: String, val description: String)

@Serializable
enum class DamageType(val displayName: String) {
    BLUDGEONING("Дробящий"),
    PIERCING("Колющий"),
    SLASHING("Рубящий"),
    THUNDER("Звуковой"),
    RADIANT("Излучающий"),
    ACID("Кислотный"),
    NECROTIC("Некротический"),
    FIRE("Огненный"),
    PSYCHIC("Психический"),
    FORCE("Силовой"),
    COLD("Холодный"),
    LIGHTNING("Электрический"),
    POISON("Ядовитый"),
    HEALING("Лечение"),
    OTHER("Другой");

    companion object {
        fun fromDisplayName(name: String): DamageType? = entries.find { it.displayName.equals(name, ignoreCase = true) }
    }
}

@Serializable
enum class MagicAttackType(val displayName: String) {
    ATTACK("Бросок атаки"),
    SAVE("Спасбросок")
}

@Serializable
enum class BonusOperation {
    ADD, SUBTRACT, OVERRIDE
}

@Serializable
enum class AdvantagePreference {
    NONE,
    IGNORE_ADVANTAGE,
    ALWAYS_ADVANTAGE,
    IGNORE_DISADVANTAGE,
    ALWAYS_DISADVANTAGE,
    IGNORE_BOTH
}

@Serializable
sealed interface IBonus {
    val id: String
    val name: String
    val formula: String
    val isActive: Boolean
    val operation: BonusOperation
    val advantagePreference: AdvantagePreference
}

@Serializable
data class AttackBonus(
    override val id: String = generateUuid(),
    override val name: String = "",
    override val formula: String = "",
    override val isActive: Boolean = true,
    override val operation: BonusOperation = BonusOperation.ADD,
    override val advantagePreference: AdvantagePreference = AdvantagePreference.NONE
) : IBonus

@Serializable
data class SimpleBonus(
    override val id: String = generateUuid(),
    override val name: String = "",
    override val formula: String = "",
    override val isActive: Boolean = true,
    override val operation: BonusOperation = BonusOperation.ADD,
    override val advantagePreference: AdvantagePreference = AdvantagePreference.NONE
) : IBonus

@Serializable
data class DamageBonus(
    override val id: String = generateUuid(),
    override val name: String = "",
    override val formula: String = "",
    override val isActive: Boolean = true,
    override val operation: BonusOperation = BonusOperation.ADD,
    override val advantagePreference: AdvantagePreference = AdvantagePreference.NONE,
    val damageType: String = ""
) : IBonus

@Serializable
enum class StatBonusType {
    SAVING_THROW,
    ABILITY_CHECK,
    CHARACTERISTIC_VALUE
}

@Serializable
data class StatBonus(
    override val id: String = generateUuid(),
    override val name: String = "",
    override val formula: String = "",
    override val isActive: Boolean = true,
    override val operation: BonusOperation = BonusOperation.ADD,
    override val advantagePreference: AdvantagePreference = AdvantagePreference.NONE,
    val attribute: Attribute = Attribute.STRENGTH,
    val type: StatBonusType = StatBonusType.SAVING_THROW,
    val applyToSkills: Boolean = false
) : IBonus

@Serializable
data class SkillBonus(
    override val id: String = generateUuid(),
    override val name: String = "",
    override val formula: String = "",
    override val isActive: Boolean = true,
    override val operation: BonusOperation = BonusOperation.ADD,
    override val advantagePreference: AdvantagePreference = AdvantagePreference.NONE,
    val skillName: String = ""
) : IBonus

@Serializable
data class DynamicNoteState(
    val id: String = generateUuid(),
    val title: String = "",
    val content: String = "",
    val isExpanded: Boolean = true,
    val isLocked: Boolean = false
)

@Serializable
data class AttackEntry(
    val id: String = generateUuid(),
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

@Serializable
enum class MaterialComponentType(val displayName: String) {
    NONE("Нет"), M("М"), M_PLUS("М+"), M_PLUS_PLUS("М++")
}

@Serializable
enum class SpellSchool(val displayName: String) {
    EVOCATION("Воплощение"),
    ILLUSION("Иллюзия"),
    NECROMANCY("Некромантия"),
    ABJURATION("Ограждение"),
    ENCHANTMENT("Очарование"),
    TRANSMUTATION("Преобразование"),
    CONJURATION("Призыв"),
    DIVINATION("Прорицание"),
    NONE("Нет")
}

@Serializable
enum class SpellVersion(val displayName: String) {
    V5_5("5.5"),
    V5E("5e"),
    HB("ХБ"),
    NONE("Нет")
}

@Serializable
enum class CharacterClass(val displayName: String) {
    BARD("Бард"), WIZARD("Волшебник"), DRUID("Друид"), CLERIC("Жрец"), 
    ARTIFICER("Изобретатель"), WARLOCK("Колдун"), PALADIN("Паладин"), 
    RANGER("Следопыт"), SORCERER("Чародей"),
    BARBARIAN("Варвар"), FIGHTER("Воин"), PUGILIST("Кулачник"),
    MONK("Монах"), ROGUE("Плут"), KINDRED("Сородич")
}

@Serializable
enum class DurationUnit(val displayName: String, val requiresValue: Boolean) {
    INSTANT("Мгновенная", false),
    SECONDS("Сек", true),
    MINUTES("Мин", true),
    HOURS("Ч", true),
    DAYS("Д", true),
    PERMANENT("Пока не будет рассеяно", false);
}

@Serializable
enum class CastingTimeType(val displayName: String) {
    ACTION("Действие"),
    BONUS_ACTION("Бонусное действие"),
    REACTION("Реакция"),
    OTHER("Другое")
}

@Serializable
data class SpellLink(
    @SerialName("id") val id: String = generateUuid(),
    @SerialName("name") val name: String = "",
    @SerialName("url") val url: String = ""
)

@Serializable
data class SpellCard(
    @SerialName("name") val name: String = "",
    @SerialName("englishName") val englishName: String = "",
    @SerialName("showEnglishName") val showEnglishName: Boolean = false,
    @SerialName("level") val level: String = "0",
    @SerialName("version") val version: SpellVersion = SpellVersion.HB,
    @SerialName("school") val school: SpellSchool = SpellSchool.NONE,
    @SerialName("classes") val classes: List<CharacterClass> = emptyList(),
    @SerialName("castingTimeType") val castingTimeType: CastingTimeType = CastingTimeType.ACTION,
    @SerialName("castingTime") val castingTime: String = "",
    @SerialName("hasVerbalComponent") val hasVerbalComponent: Boolean = false,
    @SerialName("hasSomaticComponent") val hasSomaticComponent: Boolean = false,
    @SerialName("isRitual") val isRitual: Boolean = false,
    @SerialName("isCircle") val isCircle: Boolean = false,
    @SerialName("materialComponentType") val materialComponentType: MaterialComponentType = MaterialComponentType.NONE,
    @SerialName("materialComponents") val materialComponents: String = "",
    @SerialName("hasConcentration") val hasConcentration: Boolean = false,
    @SerialName("durationValue") val durationValue: String = "",
    @SerialName("durationUnit") val durationUnit: DurationUnit = DurationUnit.INSTANT,
    @SerialName("description") val description: String = "",
    @SerialName("hasDamage") val hasDamage: Boolean = false,
    @SerialName("noDamageAtLevel1") val noDamageAtLevel1: Boolean = false,
    @SerialName("noScaling") val noScaling: Boolean = false,
    @SerialName("damageFormula") val damageFormula: String = "",
    @SerialName("upcastDamageFormula") val upcastDamageFormula: String = "",
    @SerialName("damageType") val damageType: String = "",
    @SerialName("damageTypes") val damageTypes: List<DamageType> = emptyList(),
    @SerialName("additionalDamageFormulas") val additionalDamageFormulas: List<String> = emptyList(),
    @SerialName("additionalUpcastDamageFormulas") val additionalUpcastDamageFormulas: List<String> = emptyList(),
    @SerialName("additionalDamageTypesList") val additionalDamageTypesList: List<List<DamageType>> = emptyList(),
    @SerialName("upcastOnlyOne") val upcastOnlyOne: Boolean = false,
    @SerialName("upcastUserChoice") val upcastUserChoice: Boolean = false,
    @SerialName("attackType") val attackType: MagicAttackType? = null,
    @SerialName("attackTypes") val attackTypes: List<MagicAttackType> = emptyList(),
    @SerialName("savingThrowAttributes") val savingThrowAttributes: List<Attribute> = emptyList(),
    @SerialName("distance") val distance: String = "",
    @SerialName("notes") val notes: String = "",
    @SerialName("link") val link: String? = null,
    @SerialName("additionalLinks") val additionalLinks: List<SpellLink> = emptyList(),
    @SerialName("id") val id: String = generateUuid(),
    @SerialName("sourceModuleId") val sourceModuleId: String? = null,
    @SerialName("sourceModuleVersion") val sourceModuleVersion: String? = null
) {
    val duration: String get() = if (durationUnit.requiresValue) {
        if (durationValue.isBlank()) durationUnit.displayName else "$durationValue ${durationUnit.displayName}"
    } else {
        durationUnit.displayName
    }

    fun matches(filter: SpellFilterState, searchQuery: String): Boolean {
        val matchesSearch = searchQuery.isBlank() ||
                name.contains(searchQuery, ignoreCase = true) ||
                (showEnglishName && englishName.contains(searchQuery, ignoreCase = true))

        val matchesLevel = filter.levels.isEmpty() || level in filter.levels
        val matchesClass = filter.classes.isEmpty() || classes.any { it in filter.classes }
        val matchesSchool = filter.schools.isEmpty() || school in filter.schools
        val matchesVersion = filter.versions.isEmpty() || version in filter.versions
        val matchesCastingTime = filter.castingTimeTypes.isEmpty() || castingTimeType in filter.castingTimeTypes
        val matchesDuration = filter.durationUnits.isEmpty() || durationUnit in filter.durationUnits
        val matchesAttackType = filter.attackTypes.isEmpty() || attackTypes.any { it in filter.attackTypes }
        val matchesDamageType = filter.damageTypes.isEmpty() ||
                damageTypes.any { it in filter.damageTypes } ||
                additionalDamageTypesList.any { list -> list.any { it in filter.damageTypes } }
        val matchesSaveAttr = filter.savingThrowAttributes.isEmpty() || savingThrowAttributes.any { it in filter.savingThrowAttributes }

        val matchesConc = filter.hasConcentration == null || hasConcentration == filter.hasConcentration
        val matchesRitual = filter.isRitual == null || isRitual == filter.isRitual
        val matchesCircle = filter.isCircle == null || isCircle == filter.isCircle
        val matchesDamage = filter.hasDamage == null || hasDamage == filter.hasDamage

        val matchesAttackOrSave = when(filter.attackOrSave) {
            MagicAttackType.ATTACK -> attackTypes.contains(MagicAttackType.ATTACK)
            MagicAttackType.SAVE -> attackTypes.contains(MagicAttackType.SAVE)
            null -> true
        }

        val matchesComponents = if (filter.components.isEmpty()) true else {
            filter.components.all { component ->
                when(component) {
                    SpellComponentFilter.VERBAL -> hasVerbalComponent
                    SpellComponentFilter.SOMATIC -> hasSomaticComponent
                    SpellComponentFilter.MATERIAL -> materialComponentType != MaterialComponentType.NONE
                    SpellComponentFilter.MATERIAL_COST -> materialComponents.contains("gp", ignoreCase = true) || materialComponents.contains(" зм", ignoreCase = true)
                    SpellComponentFilter.MATERIAL_CONSUMED -> materialComponents.contains("consume", ignoreCase = true) || materialComponents.contains("расходует", ignoreCase = true)
                    SpellComponentFilter.NO_VERBAL -> !hasVerbalComponent
                    SpellComponentFilter.NO_SOMATIC -> !hasSomaticComponent
                    SpellComponentFilter.NO_MATERIAL -> materialComponentType == MaterialComponentType.NONE
                    SpellComponentFilter.NO_MATERIAL_COST -> !(materialComponents.contains("gp", ignoreCase = true) || materialComponents.contains(" зм", ignoreCase = true))
                    SpellComponentFilter.NO_MATERIAL_CONSUMED -> !(materialComponents.contains("consume", ignoreCase = true) || materialComponents.contains("расходует", ignoreCase = true))
                }
            }
        }

        val matchesCastingTimeQuery = filter.castingTimeQuery.isBlank() || castingTime.contains(filter.castingTimeQuery, ignoreCase = true)
        val matchesDurationQuery = filter.durationQuery.isBlank() || durationValue == filter.durationQuery

        return matchesSearch && matchesLevel && matchesClass && matchesSchool && matchesVersion &&
                matchesCastingTime && matchesDuration && matchesAttackType && matchesDamageType && matchesSaveAttr &&
                matchesConc && matchesRitual && matchesCircle && matchesDamage && matchesComponents &&
                matchesAttackOrSave && matchesCastingTimeQuery && matchesDurationQuery
    }
}

enum class SpellComponentFilter {
    VERBAL, SOMATIC, MATERIAL, MATERIAL_COST, MATERIAL_CONSUMED,
    NO_VERBAL, NO_SOMATIC, NO_MATERIAL, NO_MATERIAL_COST, NO_MATERIAL_CONSUMED
}

data class SpellFilterState(
    val levels: Set<String> = emptySet(),
    val classes: Set<CharacterClass> = emptySet(),
    val schools: Set<SpellSchool> = emptySet(),
    val versions: Set<SpellVersion> = emptySet(),
    val castingTimeTypes: Set<CastingTimeType> = emptySet(),
    val castingTimeQuery: String = "",
    val durationUnits: Set<DurationUnit> = emptySet(),
    val durationQuery: String = "",
    val attackTypes: Set<MagicAttackType?> = emptySet(),
    val damageTypes: Set<DamageType> = emptySet(),
    val savingThrowAttributes: Set<Attribute> = emptySet(),
    val components: Set<SpellComponentFilter> = emptySet(),
    val hasConcentration: Boolean? = null,
    val hasDamage: Boolean? = null,
    val isRitual: Boolean? = null,
    val isCircle: Boolean? = null,
    val attackOrSave: MagicAttackType? = null
)

@Serializable
data class SpecialSlotSettings(
    val id: String = generateUuid(),
    val name: String = "",
    val level: Float = 1f,
    val count: Int = 0,
    val restoreOnShortRest: Boolean = false,
    val restoreOnDawn: Boolean = false
)

@Serializable
data class SpellLevelDivider(
    val id: String = generateUuid(),
    val title: String = "",
    val ability: Attribute = Attribute.NONE
)

@Serializable
data class SpellLevelItem(
    val spellId: String? = null,
    val divider: SpellLevelDivider? = null
)

@Serializable
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
    val usedSlots: Map<Float, Int> = emptyMap(),
    val usedSlotsShortRest: Map<Float, Int> = emptyMap(),
    val usedSlotsDawn: Map<Float, Int> = emptyMap(),
    val selectedSpellIds: List<String> = emptyList(),
    val preparedSpellIds: List<String> = emptyList(),
    val isSpellbookEnabled: Boolean = false,
    val levelContent: Map<String, List<SpellLevelItem>> = emptyMap(),
    val spellAbilityOverrides: Map<String, Attribute> = emptyMap(),
    val allowCantripUpcast: Boolean = false
)

@Serializable
data class Wallet(
    val platinum: Double = 0.0,
    val gold: Double = 0.0,
    val electrum: Double = 0.0,
    val silver: Double = 0.0,
    val copper: Double = 0.0,
    val visibleCurrencies: List<String> = listOf("platinum", "gold", "silver", "copper")
)

@Serializable
data class BioShortField(
    val id: String = generateUuid(),
    val title: String = "",
    val value: String = "",
    val widthRatio: Float = 0.5f,
    val isCustom: Boolean = false
)

@Serializable
data class BioSection(
    val id: String = generateUuid(),
    val title: String = "",
    val content: String = "",
    val isExpanded: Boolean = true
)

@Serializable
data class HitDiceEntry(
    val id: String = generateUuid(),
    val name: String = "",
    val formula: String = "",
    val spent: Int = 0
)

@Serializable
data class HPLevelEntry(
    val level: Int,
    val hitDie: Int,
    val rollResult: Int? = null,
    val manualValue: Int? = null
)

@Serializable
data class CharacterSummary(
    val uuid: String = "",
    val id: Int = 0,
    val name: String = "",
    val characterClass: String = "",
    val level: String = "1",
    val currentHp: String = "0",
    val maxHp: String = "0",
    val tempHp: String = "0",
    val imageData: String? = null,
    val themeSeedColorArgb: Int? = null,
    val experience: String = "0",
    val order: String = ""
)

@Serializable
data class Character(
    val uuid: String = generateUuid(),
    val id: Int = 0,
    val name: String = "",
    val characterClass: String = "",
    val order: String = "",
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
    val skilledProficiencies: List<String> = emptyList(),
    val skilledExpertise: List<String> = emptyList(),
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
    val hitDiceEntries: List<HitDiceEntry> = emptyList(),
    val hitDiceMap: Map<Int, Int> = emptyMap(),
    val defaultHitDie: Int = 8,
    val hpLevelData: List<HPLevelEntry> = emptyList(),
    val manualHPLevelData: List<HPLevelEntry> = emptyList(),
    val isMulticlassHP: Boolean = false,
    val isManualHP: Boolean = false,
    val manualMaxHp: Int = 0,
    val manualMaxHitDice: Int = 0,
    val hpBonusesAtLevel: List<AttackBonus> = emptyList(),
    val hpBonusesTotal: List<AttackBonus> = emptyList(),
    val hasInspiration: Boolean = false,
    val spellSettings: SpellSettings = SpellSettings(),
    val wallet: Wallet = Wallet(),
    val cropX: Float? = null,
    val cropY: Float? = null,
    val cropW: Float? = null,
    val cropH: Float? = null,
    val race: String = "",
    val classes: List<ClassEntry> = emptyList()
) {
    fun toSummary(): CharacterSummary = CharacterSummary(
        uuid = uuid,
        id = id,
        name = name,
        characterClass = characterClass,
        level = level,
        currentHp = currentHp,
        maxHp = maxHp,
        tempHp = tempHp,
        imageData = imageData,
        themeSeedColorArgb = themeSeedColorArgb,
        experience = experience,
        order = order
    )
}

@Serializable
data class ShieldEntry(
    override val id: String = generateUuid(),
    override val name: String = "",
    override val formula: String = "",
    override val bonuses: List<AttackBonus> = emptyList()
) : FormulaEntry

@Serializable
data class ModuleContent(
    @SerialName("type") val type: String,
    @SerialName("id") val id: String,
    @SerialName("file") val file: String
)

@Serializable
data class ModuleManifest(
    @SerialName("manifest_version") val manifestVersion: String = "1.0",
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("version") val version: String,
    @SerialName("description") val description: String = "",
    @SerialName("system") val system: String = "dnd_5.5",
    @SerialName("contents") val contents: List<ModuleContent> = emptyList()
)

@Serializable
data class InstalledModule(
    val manifest: ModuleManifest,
    val installTimestamp: Long = 0L
)
