package ru.quasaris.characters.master.backend

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
    return floor((level - 1) / 4.0).toInt() + 2
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
        else -> ((level - 19) * 50000 + 355000).toString()
    }
}

fun getPreviousLevelThreshold(levelStr: String): String {
    val level = levelStr.toIntOrNull() ?: 1
    return when (level) {
        0, 1 -> "0"
        2 -> "300"
        3 -> "900"
        4 -> "2700"
        5 -> "6500"
        6 -> "14000"
        7 -> "23000"
        8 -> "34000"
        9 -> "48000"
        10 -> "64000"
        11 -> "85000"
        12 -> "100000"
        13 -> "120000"
        14 -> "140000"
        15 -> "165000"
        16 -> "195000"
        17 -> "225000"
        18 -> "265000"
        19 -> "305000"
        20 -> "355000"
        else -> ((level - 20) * 50000 + 355000).toString()
    }
}

/**
 * Расчет уровня на основе накопленного опыта.
 */
fun calculateLevelFromExperience(expStr: String): Int {
    val exp = expStr.toLongOrNull() ?: 0L
    if (exp >= 355000) {
        return 20 + ((exp - 355000) / 50000).toInt()
    }
    return when {
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
 * Предварительная обработка формулы: замена всех текстовых токенов на числовые значения.
 */
fun preprocessFormula(formula: String, stats: Map<String, String>): String {
    var processed = formula.uppercase()
    
    // 1. Характеристики (Attribute Tokens)
    val statKeys = mapOf(
        "СИЛ" to "strength", "STR" to "strength",
        "ЛОВ" to "dexterity", "DEX" to "dexterity",
        "ТЕЛ" to "constitution", "CON" to "constitution",
        "ИНТ" to "intelligence", "INT" to "intelligence",
        "МУД" to "wisdom", "WIS" to "wisdom",
        "ХАР" to "charisma", "CHA" to "charisma"
    )
    
    statKeys.forEach { (key, statKey) ->
        // Эффективное значение (со всеми бонусами)
        val effScore = stats[statKey] ?: "10"
        val effMod = calculateModifier(effScore).toString()
        
        // Базовое значение (до бонусов)
        val baseScore = stats["base_$statKey"] ?: effScore
        val baseMod = calculateModifier(baseScore).toString()
        
        // 1. Сначала заменяем самые длинные и специфичные токены
        processed = processed.replace("[НАСТ $key ЗНАЧ]", baseScore)
            .replace("[CUR $key SCR]", baseScore)
            .replace("[$key ЗНАЧ]", effScore)
            .replace("[$key SCR]", effScore)
            
        // 2. Затем заменяем токены модификаторов (сначала НАСТ)
        processed = processed.replace("[НАСТ $key]", " $baseMod ")
            .replace("[CUR $key]", " $baseMod ")
            .replace("[$key]", " $effMod ")
    }
    
    // 2. Уровень и опыт
    val level = stats["level"] ?: "1"
    processed = processed.replace("[LVL]", " $level ")
        .replace("[УР]", " $level ")
        .replace("[LEVEL]", " $level ")
        
    val xp = stats["xp"] ?: "0"
    processed = processed.replace("[XP]", " $xp ")
        .replace("[ОП]", " $xp ")

    // 3. Здоровье и ресурсы
    val hp = stats["hp"] ?: "0"
    processed = processed.replace("[HP]", " $hp ").replace("[ХП]", " $hp ")
    val mhp = stats["max_hp"] ?: "0"
    processed = processed.replace("[MHP]", " $mhp ").replace("[МХП]", " $mhp ")
    val thp = stats["temp_hp"] ?: "0"
    processed = processed.replace("[THP]", " $thp ").replace("[ВХП]", " $thp ")

    // 4. Боевые показатели
    val ac = stats["ac"] ?: "10"
    processed = processed.replace("[AC]", " $ac ").replace("[КД]", " $ac ")
    val ex = stats["exhaustion"] ?: "0"
    processed = processed.replace("[EX]", " $ex ").replace("[ИСТ]", " $ex ")
    val cond = stats["conditions"] ?: "0"
    processed = processed.replace("[COND]", " $cond ").replace("[СОСТ]", " $cond ")

    // 5. Бонус мастерства
    val realPb = getProficiencyBonus(level).toString()
    val pbValue = stats["proficiencyBonus"] ?: realPb
    
    // Если в proficiencyBonus записана формула (например "[НАСТ БМ]"), вычисляем её рекурсивно или берем realPb
    val safePb = if (pbValue.contains("[БМ]") || pbValue.contains("[PB]")) realPb else pbValue
    
    processed = processed.replace("[БМ]", " $safePb ")
        .replace("[PB]", " $safePb ")
        .replace("[PROF]", " $safePb ")
        .replace("[НАСТ БМ]", " $realPb ")
        .replace("[REAL PB]", " $realPb ")

    // 6. Магические бонусы
    val magAtk = stats["[MAG ATC BON]"] ?: stats["[МАГ АТК БОН]"] ?: "0"
    val magSave = stats["[MAG SAVE BON]"] ?: stats["[МАГ СПАС БОН]"] ?: "0"
    val magMod = stats["[MAG MOD]"] ?: stats["[МАГ МОД]"] ?: "0"

    processed = processed.replace("[MAG ATC BON]", " $magAtk ")
        .replace("[МАГ АТК БОН]", " $magAtk ")
        .replace("[MAG SAVE BON]", " $magSave ")
        .replace("[МАГ СПАС БОН]", " $magSave ")
        .replace("[MAG MOD]", " $magMod ")
        .replace("[МАГ МОД]", " $magMod ")
        
    return processed
}

/**
 * Оценка математической формулы с учетом характеристик персонажа.
 */
fun evaluateFormula(formula: String, stats: Map<String, String>): Int {
    var processed = preprocessFormula(formula, stats)
    
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
