package ru.quasaris.characters.master

import com.google.gson.annotations.SerializedName
import java.util.UUID

interface FormulaEntry {
    val id: String
    val name: String
    val formula: String
    val bonuses: List<AttackBonus>
}

data class ArmorClassEntry(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String = "",
    override val formula: String = "",
    override val bonuses: List<AttackBonus> = emptyList()
) : FormulaEntry

data class InitiativeEntry(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String = "",
    override val formula: String = "",
    val hasAdvantage: Boolean = false,
    override val bonuses: List<AttackBonus> = emptyList()
) : FormulaEntry

data class SpeedEntry(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String = "",
    override val formula: String = "",
    override val bonuses: List<AttackBonus> = emptyList()
) : FormulaEntry

data class ClassEntry(
    val id: String = UUID.randomUUID().toString(),
    val className: CharacterClass = CharacterClass.FIGHTER,
    val subclass: String = "",
    val level: Int = 1
)

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
    HYBRID,
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

enum class MagicAttackType(val displayName: String) {
    ATTACK("Бросок атаки"),
    SAVE("Спасбросок")
}

enum class BonusOperation {
    ADD, SUBTRACT, OVERRIDE
}

enum class AdvantagePreference {
    NONE,
    IGNORE_ADVANTAGE,
    ALWAYS_ADVANTAGE,
    IGNORE_DISADVANTAGE,
    ALWAYS_DISADVANTAGE,
    IGNORE_BOTH
}

interface IBonus {
    val id: String
    val name: String
    val formula: String
    val isActive: Boolean
    val operation: BonusOperation
    val advantagePreference: AdvantagePreference
}

data class AttackBonus(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String = "",
    override val formula: String = "",
    override val isActive: Boolean = true,
    override val operation: BonusOperation = BonusOperation.ADD,
    override val advantagePreference: AdvantagePreference = AdvantagePreference.NONE
) : IBonus

data class DamageBonus(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String = "",
    override val formula: String = "",
    override val isActive: Boolean = true,
    override val operation: BonusOperation = BonusOperation.ADD,
    override val advantagePreference: AdvantagePreference = AdvantagePreference.NONE,
    val damageType: String = ""
) : IBonus

enum class StatBonusType {
    SAVING_THROW,
    ABILITY_CHECK,
    CHARACTERISTIC_VALUE
}

data class StatBonus(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String = "",
    override val formula: String = "",
    override val isActive: Boolean = true,
    override val operation: BonusOperation = BonusOperation.ADD,
    override val advantagePreference: AdvantagePreference = AdvantagePreference.NONE,
    val attribute: Attribute = Attribute.STRENGTH,
    val type: StatBonusType = StatBonusType.SAVING_THROW,
    val applyToSkills: Boolean = false
) : IBonus

data class SkillBonus(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String = "",
    override val formula: String = "",
    override val isActive: Boolean = true,
    override val operation: BonusOperation = BonusOperation.ADD,
    override val advantagePreference: AdvantagePreference = AdvantagePreference.NONE,
    val skillName: String = ""
) : IBonus

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

enum class MaterialComponentType(val displayName: String) {
    NONE("Нет"), M("М"), M_PLUS("М+"), M_PLUS_PLUS("М++")
}

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

enum class SpellVersion(val displayName: String) {
    V5_5("5.5"),
    V5E("5e"),
    HB("ХБ"),
    NONE("Нет")
}

enum class CharacterClass(val displayName: String) {
    BARD("Бард"), WIZARD("Волшебник"), DRUID("Друид"), CLERIC("Жрец"), 
    ARTIFICER("Изобретатель"), WARLOCK("Колдун"), PALADIN("Паладин"), 
    RANGER("Следопыт"), SORCERER("Чародей"),
    BARBARIAN("Варвар"), FIGHTER("Воин"), PUGILIST("Кулачник"),
    MONK("Монах"), ROGUE("Плут"), KINDRED("Сородич")
}

enum class DurationUnit(val displayName: String, val requiresValue: Boolean) {
    INSTANT("Мгновенная", false),
    SECONDS("Сек", true),
    MINUTES("Мин", true),
    HOURS("Ч", true),
    DAYS("Д", true),
    PERMANENT("Пока не будет рассеяно", false);
}

enum class CastingTimeType(val displayName: String) {
    ACTION("Действие"),
    BONUS_ACTION("Бонусное действие"),
    REACTION("Реакция"),
    OTHER("Другое")
}

data class SpellLink(
    @SerializedName("id") val id: String = UUID.randomUUID().toString(),
    @SerializedName("name") val name: String = "",
    @SerializedName("url") val url: String = ""
)

data class SpellCard(
    @SerializedName("name") val name: String = "",
    @SerializedName("englishName") val englishName: String = "",
    @SerializedName("showEnglishName") val showEnglishName: Boolean = false,
    @SerializedName("level") val level: String = "0",
    @SerializedName("version") val version: SpellVersion = SpellVersion.HB,
    @SerializedName("school") val school: SpellSchool = SpellSchool.NONE,
    @SerializedName("classes") val classes: List<CharacterClass> = emptyList(),
    @SerializedName("castingTimeType") val castingTimeType: CastingTimeType = CastingTimeType.ACTION,
    @SerializedName("castingTime") val castingTime: String = "",
    @SerializedName("hasVerbalComponent") val hasVerbalComponent: Boolean = false,
    @SerializedName("hasSomaticComponent") val hasSomaticComponent: Boolean = false,
    @SerializedName("isRitual") val isRitual: Boolean = false,
    @SerializedName("isCircle") val isCircle: Boolean = false,
    @SerializedName("materialComponentType") val materialComponentType: MaterialComponentType = MaterialComponentType.NONE,
    @SerializedName("materialComponents") val materialComponents: String = "",
    @SerializedName("hasConcentration") val hasConcentration: Boolean = false,
    @SerializedName("durationValue") val durationValue: String = "",
    @SerializedName("durationUnit") val durationUnit: DurationUnit = DurationUnit.INSTANT,
    @SerializedName("description") val description: String = "",
    @SerializedName("hasDamage") val hasDamage: Boolean = false,
    @SerializedName("noDamageAtLevel1") val noDamageAtLevel1: Boolean = false,
    @SerializedName("noScaling") val noScaling: Boolean = false,
    @SerializedName("damageFormula") val damageFormula: String = "",
    @SerializedName("upcastDamageFormula") val upcastDamageFormula: String = "",
    @SerializedName("damageType") val damageType: String = "",
    @SerializedName("damageTypes") val damageTypes: List<DamageType> = emptyList(),
    @SerializedName("additionalDamageFormulas") val additionalDamageFormulas: List<String> = emptyList(),
    @SerializedName("additionalDamageTypesList") val additionalDamageTypesList: List<List<DamageType>> = emptyList(),
    @SerializedName("attackType") val attackType: MagicAttackType? = null,
    @SerializedName("attackTypes") val attackTypes: List<MagicAttackType> = emptyList(),
    @SerializedName("savingThrowAttributes") val savingThrowAttributes: List<Attribute> = emptyList(),
    @SerializedName("distance") val distance: String = "",
    @SerializedName("notes") val notes: String = "",
    @SerializedName("link") val link: String? = null,
    @SerializedName("additionalLinks") val additionalLinks: List<SpellLink> = emptyList(),
    @SerializedName("id") val id: String = UUID.randomUUID().toString(),
    @SerializedName("sourceModuleId") val sourceModuleId: String? = null,
    @SerializedName("sourceModuleVersion") val sourceModuleVersion: String? = null
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

data class SpecialSlotSettings(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val level: Float = 1f,
    val count: Int = 0,
    val restoreOnShortRest: Boolean = false,
    val restoreOnDawn: Boolean = false
)

data class SpellLevelDivider(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val ability: Attribute = Attribute.NONE
)

data class SpellLevelItem(
    val spellId: String? = null,
    val divider: SpellLevelDivider? = null
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
    val usedSlots: Map<Float, Int> = emptyMap(),
    val usedSlotsShortRest: Map<Float, Int> = emptyMap(),
    val usedSlotsDawn: Map<Float, Int> = emptyMap(),
    val selectedSpellIds: List<String> = emptyList(),
    val preparedSpellIds: List<String> = emptyList(),
    val isSpellbookEnabled: Boolean = false,
    val levelContent: Map<String, List<SpellLevelItem>> = emptyMap(),
    val spellAbilityOverrides: Map<String, Attribute> = emptyMap()
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
    val widthRatio: Float = 0.5f,
    val isCustom: Boolean = false
)

data class BioSection(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val content: String = "",
    val isExpanded: Boolean = true
)

data class HitDiceEntry(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val formula: String = "",
    val spent: Int = 0
)

data class HPLevelEntry(
    val level: Int,
    val hitDie: Int,
    val rollResult: Int? = null,
    val manualValue: Int? = null
)

data class Character(

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
    val hitDiceEntries: List<HitDiceEntry> = emptyList(),
    val hitDiceMap: Map<Int, Int> = emptyMap(), // Die dimension -> Max count
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
)


data class ShieldEntry(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String = "",
    override val formula: String = "",
    override val bonuses: List<AttackBonus> = emptyList()
) : FormulaEntry

data class ModuleContent(
    @SerializedName("type") val type: String,
    @SerializedName("id") val id: String,
    @SerializedName("file") val file: String
)

data class ModuleManifest(
    @SerializedName("manifest_version") val manifestVersion: String = "1.0",
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("version") val version: String,
    @SerializedName("description") val description: String = "",
    @SerializedName("system") val system: String = "dnd_5.5",
    @SerializedName("contents") val contents: List<ModuleContent> = emptyList()
)

data class InstalledModule(
    val manifest: ModuleManifest,
    val installTimestamp: Long = System.currentTimeMillis()
)
