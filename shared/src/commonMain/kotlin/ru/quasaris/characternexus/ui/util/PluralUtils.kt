package ru.quasaris.characternexus.util

/**
 * Возвращает нужную форму слова в зависимости от [count] по правилам
 * русского языка (с учётом исключения для 11–14, которые всегда идут
 * с формой "many", даже если оканчиваются на 1-4).
 *
 * Примеры:
 *  pluralizeRu(1, "персонаж", "персонажа", "персонажей")  -> "персонаж"
 *  pluralizeRu(2, "персонаж", "персонажа", "персонажей")  -> "персонажа"
 *  pluralizeRu(4, "персонаж", "персонажа", "персонажей")  -> "персонажа"
 *  pluralizeRu(5, "персонаж", "персонажа", "персонажей")  -> "персонажей"
 *  pluralizeRu(11, "персонаж", "персонажа", "персонажей") -> "персонажей"
 *  pluralizeRu(14, "персонаж", "персонажа", "персонажей") -> "персонажей"
 *  pluralizeRu(21, "персонаж", "персонажа", "персонажей") -> "персонаж"
 *
 * @param one   форма для 1, 21, 31, 101, ...
 * @param few   форма для 2-4, 22-24, 32-34, ...
 * @param many  форма для 0, 5-20, 25-30, 100-104(0), 11-14, ...
 */
fun pluralizeRu(count: Int, one: String, few: String, many: String): String {
    val absCount = kotlin.math.abs(count)
    val mod100 = absCount % 100
    val mod10 = absCount % 10

    return when {
        mod100 in 11..14 -> many
        mod10 == 1 -> one
        mod10 in 2..4 -> few
        else -> many
    }
}

/**
 * Готовая подпись вида "1 персонаж" / "3 персонажа" / "11 персонажей".
 */
fun charactersCountLabel(count: Int): String {
    val word = pluralizeRu(count, one = "персонаж", few = "персонажа", many = "персонажей")
    return "$count $word"
}
