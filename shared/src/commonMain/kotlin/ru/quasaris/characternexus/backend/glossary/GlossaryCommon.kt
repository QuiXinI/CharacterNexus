package ru.quasaris.characternexus.backend.glossary

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ru.quasaris.characternexus.util.generateUuid

/**
 * Единый Json для сущностей глоссария. isLenient и coerceInputValues нужны, чтобы
 * файлы модулей можно было спокойно править вручную вне приложения
 * (числа вместо строк, неизвестные значения enum, null вместо значения по умолчанию).
 */
val GlossaryJson: Json = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
    encodeDefaults = true
    isLenient = true
    coerceInputValues = true
}

const val MAX_CLASS_LEVEL = 20

/** Генерация id для новой сущности: "class_1a2b3c4d". */
fun newGlossaryId(prefix: String): String = "${prefix}_${generateUuid().take(8)}"

/** Разбор строки вида "a | b | c" в ячейки. */
fun parseCells(line: String): List<String> = line.split("|").map { it.trim() }

/** Сборка ячеек обратно в строку вида "a | b | c". */
fun List<String>.toCellsLine(): String = joinToString(" | ")

/**
 * Умение класса, подкласса, вида или черты.
 * level = null — умение не привязано к уровню. id по умолчанию генерируется,
 * поэтому при ручном редактировании JSON его можно не указывать.
 */
@Serializable
data class FeatureDef(
    @SerialName("id") val id: String = generateUuid(),
    @SerialName("name") val name: String = "",
    @SerialName("level") val level: Int? = null,
    @SerialName("requirement") val requirement: String = "",
    @SerialName("description") val description: String = ""
)

/** Запись произвольного списка (воззвание, договор, ритуал): название, условие, описание. */
@Serializable
data class GlossaryEntry(
    @SerialName("id") val id: String = generateUuid(),
    @SerialName("name") val name: String = "",
    @SerialName("requirement") val requirement: String = "",
    @SerialName("repeatable") val repeatable: Boolean = false,
    @SerialName("description") val description: String = ""
)

/** Простая таблица: заголовки и строки текста. */
@Serializable
data class GlossaryTable(
    @SerialName("id") val id: String = generateUuid(),
    @SerialName("title") val title: String = "",
    @SerialName("headers") val headers: List<String> = emptyList(),
    @SerialName("rows") val rows: List<List<String>> = emptyList()
)

/** Блок содержимого кастомной вкладки. */
@Serializable
sealed interface TabBlock {
    val id: String

    @Serializable
    @SerialName("text")
    data class Text(
        @SerialName("id") override val id: String = generateUuid(),
        @SerialName("text") val text: String = ""
    ) : TabBlock

    @Serializable
    @SerialName("table")
    data class Table(
        @SerialName("id") override val id: String = generateUuid(),
        @SerialName("title") val title: String = "",
        @SerialName("headers") val headers: List<String> = emptyList(),
        @SerialName("rows") val rows: List<List<String>> = emptyList()
    ) : TabBlock

    @Serializable
    @SerialName("entries")
    data class Entries(
        @SerialName("id") override val id: String = generateUuid(),
        @SerialName("title") val title: String = "",
        @SerialName("items") val items: List<GlossaryEntry> = emptyList()
    ) : TabBlock
}

/**
 * Кастомная вкладка класса или подкласса («Воззвания», «Заговоры и ритуалы» и т.п.).
 * Не называется CustomTab, чтобы не конфликтовать с model.CustomTab при wildcard-импорте.
 */
@Serializable
data class GlossaryCustomTab(
    @SerialName("id") val id: String = generateUuid(),
    @SerialName("title") val title: String = "Новая вкладка",
    @SerialName("blocks") val blocks: List<TabBlock> = emptyList()
)

/** Количество ячеек заклинаний одного уровня. */
@Serializable
data class SlotCount(
    @SerialName("slot_level") val slotLevel: Int,
    @SerialName("count") val count: Int
)

/**
 * Тип сущности глоссария. wireName — значение ModuleContent.type в manifest.json,
 * folder — папка и в архиве модуля, и в данных приложения.
 */
@Serializable
enum class GlossaryType(val wireName: String, val folder: String) {
    SPELL("spell", "spells"),
    CLASS("class", "classes"),
    SUBCLASS("subclass", "subclasses"),
    SPECIES("species", "species"),
    FEAT("feat", "feats"),
    MAGIC_ITEM("magic_item", "magic_items");

    companion object {
        fun fromWireName(name: String): GlossaryType? = entries.find { it.wireName == name }
    }
}
