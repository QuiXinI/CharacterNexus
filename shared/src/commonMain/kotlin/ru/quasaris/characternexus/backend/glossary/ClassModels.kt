package ru.quasaris.characternexus.backend.glossary

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Тип заклинателя класса. Назван SpellcastingType, а не CasterType, чтобы не
 * конфликтовать с ru.quasaris.characternexus.model.CasterType (настройка ячеек
 * на листе персонажа) — оба пакета нередко импортируются через `*` в одном файле.
 */
@Serializable
enum class SpellcastingType {
    /** Не накладывает заклинаний. */
    NONE,
    /** Полный заклинатель: стандартная таблица ячеек PHB. */
    FULL,
    /** Полузаклинатель: уровень класса округляется вверх до чётного перед таблицей
     *  полного заклинателя (1→1, 2→1, 3→2, 4→2, 5→3, ...). */
    HALF,
    /** Особый заклинатель: ячейки по уровням вводит автор данных вручную
     *  (колдун и подобные классы с нестандартной прогрессией). */
    CUSTOM
}

/**
 * Ячейки особого заклинателя на одном уровне класса.
 */
@Serializable
data class CustomSlotRow(
    @SerialName("level") val level: Int,
    @SerialName("slots") val slots: List<SlotCount> = emptyList()
)

/**
 * Настройки заклинательства класса или подкласса.
 */
@Serializable
data class SpellcastingConfig(
    @SerialName("type") val type: SpellcastingType = SpellcastingType.NONE,
    /** Заклинательная характеристика, например «Харизма». Текстовое поле. */
    @SerialName("ability") val ability: String = "",
    /** Показывать вкладку со списком заклинаний класса/подкласса. */
    @SerialName("show_spell_list_tab") val showSpellListTab: Boolean = false,
    /** Явный список id заклинаний, прикреплённых к классу/подклассу напрямую
     *  (в дополнение к заклинаниям, у которых в glossaryClassIds указан id класса). */
    @SerialName("spell_ids") val spellIds: List<String> = emptyList(),
    /** Используется только при type = CUSTOM. */
    @SerialName("custom_slots") val customSlots: List<CustomSlotRow> = emptyList()
)

@Serializable
enum class ColumnKind {
    /** Произвольный текст, например «Умения». */
    TEXT,
    /** Число, например «Заговоры» или «Подг. Закл.». */
    NUMBER,
    /** Ячейки заклинаний. Значение не хранится, а вычисляется из SpellcastingConfig
     *  через SpellSlotCalculator и отображается как сворачиваемый блок. */
    SLOTS
}

/**
 * Колонка таблицы прогрессии класса.
 * В label символ \n задаёт перенос на вторую строку заголовка
 * (например "Подг.\nЗакл."), что и требуется для двухстрочного фиксированного заголовка.
 */
@Serializable
data class ProgressionColumn(
    @SerialName("key") val key: String,
    @SerialName("label") val label: String,
    @SerialName("kind") val kind: ColumnKind = ColumnKind.NUMBER
)

/**
 * Строка таблицы прогрессии: текстовые значения по ключу колонки.
 * Колонки kind = SLOTS в values не хранятся и игнорируются при рендере/сохранении.
 */
@Serializable
data class ProgressionRow(
    @SerialName("level") val level: Int,
    @SerialName("values") val values: Map<String, String> = emptyMap()
)

@Serializable
data class ClassProgression(
    @SerialName("columns") val columns: List<ProgressionColumn> = emptyList(),
    @SerialName("rows") val rows: List<ProgressionRow> = emptyList()
)

@Serializable
data class ClassDefinition(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String = "",
    @SerialName("system") val system: String = "5.5",
    @SerialName("description") val description: String = "",
    @SerialName("primary_ability") val primaryAbility: String = "",
    @SerialName("hit_die") val hitDie: String = "",
    /** Подпись раздела подклассов, например «Подкласс» или «Покровитель». */
    @SerialName("subclass_label") val subclassLabel: String = "Подкласс",
    @SerialName("progression") val progression: ClassProgression = ClassProgression(),
    /** Умения класса (Магия Договора, Увеличение Характеристик и т.п.). */
    @SerialName("features") val features: List<FeatureDef> = emptyList(),
    /** Произвольные дополнительные вкладки (Воззвания, Заговоры и ритуалы и т.п.). */
    @SerialName("custom_tabs") val customTabs: List<GlossaryCustomTab> = emptyList(),
    @SerialName("spellcasting") val spellcasting: SpellcastingConfig = SpellcastingConfig(),
    @SerialName("source") val source: String = "",
    @SerialName("source_module_id") val sourceModuleId: String? = null
)

/**
 * Заклинания, которые подкласс всегда имеет подготовленными начиная с указанного
 * уровня класса (например, «Заклинания Архифеи» у Колдуна: 3, 5, 7, 9 уровень).
 */
@Serializable
data class SpellGrant(
    @SerialName("class_level") val classLevel: Int,
    @SerialName("spell_ids") val spellIds: List<String> = emptyList()
)

@Serializable
data class SubclassDefinition(
    @SerialName("id") val id: String,
    @SerialName("class_id") val classId: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("description") val description: String = "",
    @SerialName("features") val features: List<FeatureDef> = emptyList(),
    @SerialName("custom_tabs") val customTabs: List<GlossaryCustomTab> = emptyList(),
    /**
     * Список заклинаний подкласса. Полностью обособлен от других подклассов того же
     * класса — ровно 0 заклинаний отсюда доступно остальным подклассам (см. задание).
     * spellIds — явный список, доступный подклассу целиком;
     * grants — заклинания, которые становятся всегда подготовленными на уровне класса.
     */
    @SerialName("show_spell_list_tab") val showSpellListTab: Boolean = false,
    @SerialName("spell_ids") val spellIds: List<String> = emptyList(),
    @SerialName("spell_grants") val spellGrants: List<SpellGrant> = emptyList(),
    @SerialName("source") val source: String = "",
    @SerialName("source_module_id") val sourceModuleId: String? = null
)

/**
 * Строки таблицы для всех уровней 1..MAX_CLASS_LEVEL: отсутствующие уровни
 * дополняются пустыми строками, лишние отбрасываются.
 */
fun ClassProgression.paddedRows(): List<ProgressionRow> =
    (1..MAX_CLASS_LEVEL).map { level -> rows.find { it.level == level } ?: ProgressionRow(level) }
