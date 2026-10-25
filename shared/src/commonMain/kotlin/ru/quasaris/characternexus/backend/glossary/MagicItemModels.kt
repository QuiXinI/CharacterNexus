package ru.quasaris.characternexus.backend.glossary

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Вид магического предмета. У каждого вида свой набор полей в редакторе,
 * но модель общая — лишние поля для вида просто остаются пустыми.
 */
@Serializable
enum class MagicItemKind(val displayName: String) {
    POTION("Зелье"),
    ARMOR("Доспех"),
    ROD("Жезл"),
    RING("Кольцо"),
    WEAPON("Оружие"),
    STAFF("Посох"),
    SCROLL("Свиток"),
    WONDROUS("Чудесный предмет"),
    SHIELD("Щит")
}

@Serializable
enum class ItemRarity(val displayName: String) {
    COMMON("Обычный"),
    UNCOMMON("Необычный"),
    RARE("Редкий"),
    VERY_RARE("Очень редкий"),
    LEGENDARY("Легендарный"),
    ARTIFACT("Артефакт")
}

/**
 * Настройка предмета.
 * required — предмет требует настройки.
 * classIds — id классов, которые могут настроиться (кнопка «все» в редакторе
 * заполняет список всеми классами модуля). Пустой список при required = true
 * означает, что настроиться не может никто, кроме условия из requirement.
 * requirement — дополнительное текстовое условие, например «Колдун 5+».
 */
@Serializable
data class AttunementSettings(
    @SerialName("required") val required: Boolean = false,
    @SerialName("class_ids") val classIds: List<String> = emptyList(),
    @SerialName("requirement") val requirement: String = ""
)

/**
 * Магический предмет. Описание хранится строкой с \n и собственным
 * форматированием компонента редактора текста (как в SpellConfigDialog).
 */
@Serializable
data class MagicItem(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String = "",
    @SerialName("kind") val kind: MagicItemKind = MagicItemKind.POTION,
    @SerialName("rarity") val rarity: ItemRarity = ItemRarity.COMMON,
    @SerialName("english_name") val englishName: String = "",
    @SerialName("show_english_name") val showEnglishName: Boolean = false,
    /** Формула (актуально для зелий), пустая строка если не нужна. */
    @SerialName("formula") val formula: String = "",
    @SerialName("weight") val weight: String = "",
    /** Поля зелий, сохранённые для совместимости с листом персонажа. */
    @SerialName("icon_index") val iconIndex: Int = 1,
    @SerialName("color_hex") val colorHex: String = "FF0000",
    @SerialName("damage_types") val damageTypes: List<ru.quasaris.characternexus.model.DamageType> = emptyList(),
    @SerialName("attunement") val attunement: AttunementSettings = AttunementSettings(),
    @SerialName("description") val description: String = "",
    @SerialName("source") val source: String = "",
    @SerialName("source_module_id") val sourceModuleId: String? = null
)
