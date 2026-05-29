package ru.quasaris.characters.master.HeaderCode

import java.util.Stack
import kotlin.math.floor

/**
 * Расчет модификатора характеристики по значению.
 */
fun calculateModifier(scoreStr: String): Int {
    val score = scoreStr.toIntOrNull() ?: 10
    return floor((score - 10) / 2.0).toInt()
}

/**
 * Получение базового бонуса мастерства по уровню.
 */
fun getProficiencyBonus(levelStr: String): Int {
    val level = levelStr.toIntOrNull() ?: 1
    return when {
        level >= 17 -> 6
        level >= 13 -> 5
        level >= 9 -> 4
        level >= 5 -> 3
        else -> 2
    }
}

/**
 * Порог опыта для следующего уровня.
 */
fun getNextLevelThreshold(levelStr: String): String {
    val level = levelStr.toIntOrNull() ?: 1
    return when (level) {
        0, 1 -> "300"
        2 -> "900"
        3 -> "2700"
        4 -> "6500"
        5 -> "14000"
        6 -> "23000"
        7 -> "34000"
        8 -> "48000"
        9 -> "64000"
        10 -> "85000"
        11 -> "100000"
        12 -> "120000"
        13 -> "140000"
        14 -> "165000"
        15 -> "195000"
        16 -> "225000"
        17 -> "265000"
        18 -> "305000"
        19 -> "355000"
        else -> "—"
    }
}

/**
 * Расчет уровня на основе накопленного опыта.
 */
fun calculateLevelFromExperience(expStr: String): Int {
    val exp = expStr.toLongOrNull() ?: 0L
    return when {
        exp >= 355000 -> 20
        exp >= 305000 -> 19
        exp >= 265000 -> 18
        exp >= 225000 -> 17
        exp >= 195000 -> 16
        exp >= 165000 -> 15
        exp >= 140000 -> 14
        exp >= 120000 -> 13
        exp >= 100000 -> 12
        exp >= 85000 -> 11
        exp >= 64000 -> 10
        exp >= 48000 -> 9
        exp >= 34000 -> 8
        exp >= 23000 -> 7
        exp >= 14000 -> 6
        exp >= 6500 -> 5
        exp >= 2700 -> 4
        exp >= 900 -> 3
        exp >= 300 -> 2
        else -> 1
    }
}

/**
 * Оценка математической формулы с учетом характеристик персонажа.
 */
fun evaluateFormula(formula: String, stats: Map<String, String>): Int {
    var processed = formula.uppercase()
    val statKeys = mapOf(
        "СИЛ" to "strength", "STR" to "strength",
        "ЛОВ" to "dexterity", "DEX" to "dexterity",
        "ТЕЛ" to "constitution", "CON" to "constitution",
        "ИНТ" to "intelligence", "INT" to "intelligence",
        "МУД" to "wisdom", "WIS" to "wisdom",
        "ХАР" to "charisma", "CHA" to "charisma"
    )
    statKeys.forEach { (key, statKey) ->
        val score = stats[statKey] ?: "10"
        val mod = calculateModifier(score).toString()
        processed = processed.replace("[$key ЗНАЧ]", score).replace("[$key SCR]", score)
            .replace("[$key]", " $mod ")
            .replace("[$key ", " $mod ")
    }
    val pb = stats["proficiencyBonus"] ?: "2"
    val level = stats["level"] ?: "1"
    val realPb = getProficiencyBonus(level).toString()
    
    // Заменяем [БМ] на реальный бонус или на то, что введено в поле "Бонус Мастерства"
    processed = processed.replace("[БМ]", " $pb ").replace("[PB]", " $pb ")
        .replace("[НАСТ БМ]", " $realPb ").replace("[REAL PB]", " $realPb ")
    
    if (processed.contains("[БМ]") || processed.contains("[PB]")) {
        processed = processed.replace("[БМ]", " $realPb ").replace("[PB]", " $realPb ")
    }
    
    fun processFunctions(input: String): String {
        var current = input
        val functions = listOf(
            listOf("МАКС", "MAX", "НИЗ", "FLOOR") to true,
            listOf("МИН", "MIN", "ВЕРХ", "CEIL") to false
        )
        functions.forEach { (names, isMax) ->
            names.forEach { func ->
                val patternStandard = Regex("(?:\\[$func\\s*\\(([^()]+)\\)]|$func\\s*\\(([^()]+)\\))")
                while (current.contains(func)) {
                    val match = patternStandard.find(current) ?: break
                    val content = match.groupValues[1].ifEmpty { match.groupValues[2] }
                    val values = content.split(Regex("[;,]")).map { evaluateFormula(it.trim(), stats) }
                    val result = if (isMax) values.maxOrNull() ?: 0 else values.minOrNull() ?: 0
                    current = current.replace(match.value, result.toString())
                }
                val patternTrailing = Regex("(-?\\d+)[^\\d\\[]*\\[$func]\\s*\\((-?\\d+)\\)")
                while (current.contains("[$func]")) {
                    val match = patternTrailing.find(current) ?: break
                    val val1 = match.groupValues[1].toInt()
                    val val2 = match.groupValues[2].toInt()
                    val result = if (isMax) maxOf(val1, val2) else minOf(val1, val2)
                    current = current.replace(match.value, result.toString())
                }
            }
        }
        return current
    }

    processed = processFunctions(processed).replace(Regex("[^\\d+\\-*/]"), " ")
    return try {
        val clean = processed.replace(" ", "")
        val tokens = mutableListOf<String>()
        var i = 0
        while (i < clean.length) {
            val c = clean[i]
            if (c.isDigit()) {
                val start = i
                while (i < clean.length && clean[i].isDigit()) i++
                tokens.add(clean.substring(start, i))
            } else if ("+*/".contains(c)) { tokens.add(c.toString()); i++ }
            else if (c == '-') {
                if (i > 0 && clean[i-1].isDigit()) { tokens.add("-"); i++ }
                else {
                    val s = i; i++; while (i < clean.length && clean[i].isDigit()) i++
                    if (i > s + 1) tokens.add(clean.substring(s, i)) else tokens.add("-")
                }
            } else i++
        }
        val vS = Stack<Int>(); val oS = Stack<String>()
        fun prc(o1: String, o2: String): Boolean = !((o1 == "*" || o1 == "/") && (o2 == "+" || o2 == "-"))
        fun app(op: String, b: Int, a: Int): Int = when (op) {
            "+" -> a + b; "-" -> a - b; "*" -> a * b; "/" -> if (b != 0) a / b else 0; else -> 0
        }
        for (token in tokens) {
            if (token.isEmpty()) continue
            if (token[0].isDigit() || (token.length > 1 && token[0] == '-' && token[1].isDigit())) vS.push(token.toInt())
            else if ("+-*/".contains(token)) {
                while (!oS.empty() && prc(token, oS.peek())) { if (vS.size < 2) break; vS.push(app(oS.pop(), vS.pop(), vS.pop())) }
                oS.push(token)
            }
        }
        while (!oS.empty() && vS.size >= 2) vS.push(app(oS.pop(), vS.pop(), vS.pop()))
        if (vS.isEmpty()) 0 else vS.pop()
    } catch (_: Exception) { 0 }
}

data class Condition(val name: String, val description: String)

fun parseConditions(content: String): List<Condition> {
    return content.split("##").filter { it.isNotBlank() }.map { s ->
        val lines = s.trim().lines()
        Condition(lines.firstOrNull()?.trim() ?: "", lines.drop(1).joinToString("\n").trim())
    }
}
