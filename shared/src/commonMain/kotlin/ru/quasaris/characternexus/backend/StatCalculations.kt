package ru.quasaris.characternexus.backend

import kotlin.math.*

/**
 * Конфигурация для расчетов, связанных с костями.
 */
object DiceCalculationConfig {
    var isDiceCalculationEnabled: Boolean = true
}

/**
 * Описание части формулы с кубами (например, 2d6).
 */
data class DicePart(val count: Int, val sides: Int)

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
 * Представление части формулы.
 */
sealed class FormulaPart {
    data class Dice(val count: Int, val sides: Int) : FormulaPart()
    data class Flat(val value: Int) : FormulaPart()
}

/**
 * Разбор формулы на плоский бонус и список кубов.
 */
fun parseFormulaParts(
    formula: String,
    stats: Map<String, String> = emptyMap()
): Pair<Int, List<DicePart>> {
    val ordered = parseFormulaOrdered(formula, stats)
    var flat = 0
    val dice = mutableListOf<DicePart>()
    
    ordered.forEach { part ->
        when (part) {
            is FormulaPart.Dice -> dice.add(DicePart(part.count, part.sides))
            is FormulaPart.Flat -> flat += part.value
        }
    }
    
    return flat to dice
}

/**
 * Разбор формулы с сохранением порядка следования элементов.
 */
fun parseFormulaOrdered(
    formula: String,
    stats: Map<String, String> = emptyMap()
): List<FormulaPart> {
    val processed = preprocessFormula(formula, stats)
    val result = mutableListOf<FormulaPart>()
    
    // Регулярное выражение для поиска кубов (например, 1d6, +2d8, -d4)
    val diceRegex = Regex("([+-]?\\d*)[dkк](\\d+)", RegexOption.IGNORE_CASE)
    
    // Ищем все вхождения кубов и их позиции
    val matches = diceRegex.findAll(processed).toList()
    var lastIndex = 0
    
    matches.forEach { match ->
        // Все, что между предыдущим кубом и текущим - это потенциально плоский бонус
        val gap = processed.substring(lastIndex, match.range.first).trim()
        if (gap.isNotEmpty() && gap != "+" && gap != "-") {
            val flatVal = evaluateFormulaDouble(gap, stats).toInt()
            if (flatVal != 0) {
                result.add(FormulaPart.Flat(flatVal))
            }
        }

        val countStr = match.groupValues[1]
        val count = when (countStr) {
            "", "+" -> 1
            "-" -> -1
            else -> countStr.toIntOrNull() ?: 1
        }
        val sides = match.groupValues[2].toInt()
        if (sides > 0) {
            result.add(FormulaPart.Dice(count, sides))
        }
        
        lastIndex = match.range.last + 1
    }
    
    // Обработка остатка формулы после последнего куба
    val remaining = processed.substring(lastIndex).trim()
    if (remaining.isNotEmpty()) {
        val flatVal = evaluateFormulaDouble(remaining, stats).toInt()
        if (flatVal != 0) {
            result.add(FormulaPart.Flat(flatVal))
        }
    }
    
    return result
}

/**
 * Предварительная обработка формулы: замена всех текстовых токенов [...] на числовые значения.
 */
fun preprocessFormula(formula: String, stats: Map<String, String>): String {
    val tokenRegex = Regex("\\[([^\\]]+)\\]")
    
    val statMapping = mapOf(
        "СИЛ" to listOf("strength", "STR", "СИЛ"),
        "STR" to listOf("strength", "STR", "СИЛ"),
        "ЛОВ" to listOf("dexterity", "DEX", "ЛОВ"),
        "DEX" to listOf("dexterity", "DEX", "ЛОВ"),
        "ТЕЛ" to listOf("constitution", "CON", "ТЕЛ"),
        "CON" to listOf("constitution", "CON", "ТЕЛ"),
        "ИНТ" to listOf("intelligence", "INT", "ИНТ"),
        "INT" to listOf("intelligence", "INT", "ИНТ"),
        "МУД" to listOf("wisdom", "WIS", "МУД"),
        "WIS" to listOf("wisdom", "WIS", "МУД"),
        "ХАР" to listOf("charisma", "CHA", "ХАР"),
        "CHA" to listOf("charisma", "CHA", "ХАР")
    )

    fun getStatValue(token: String): String {
        val upperToken = token.uppercase().trim()
        
        // 1. Проверка на модификатор НАСТ (текущий базовый)
        if (upperToken.startsWith("НАСТ ") || upperToken.startsWith("CUR ")) {
            val attr = upperToken.removePrefix("НАСТ ").removePrefix("CUR ").trim()
            if (attr == "БМ" || attr == "PB") {
                val level = stats["level"] ?: "1"
                return getProficiencyBonus(level).toString()
            }
            val keys = statMapping[attr]
            if (keys != null) {
                for (k in keys) {
                    val score = stats["base_$k"] ?: stats[k]
                    if (score != null) return calculateModifier(score).toString()
                }
            }
        }
        
        // 2. Проверка на значение ЗНАЧ/SCR
        if (upperToken.endsWith(" ЗНАЧ") || upperToken.endsWith(" SCR")) {
            val isBase = upperToken.startsWith("НАСТ ") || upperToken.startsWith("CUR ")
            val attr = upperToken.removeSuffix(" ЗНАЧ").removeSuffix(" SCR")
                .removePrefix("НАСТ ").removePrefix("CUR ").trim()
            val keys = statMapping[attr]
            if (keys != null) {
                for (k in keys) {
                    val key = if (isBase) "base_$k" else k
                    val score = stats[key] ?: if (isBase) stats[k] else null
                    if (score != null) return score
                }
            }
            return stats[upperToken] ?: "10"
        }

        // 3. Стандартные сокращения модификаторов [STR], [СИЛ] и т.д.
        statMapping[upperToken]?.let { keys ->
            for (k in keys) {
                stats[k]?.let { return calculateModifier(it).toString() }
            }
        }

        // 4. Специальные токены
        return when (upperToken) {
            "LVL", "УР", "LEVEL" -> stats["level"] ?: "1"
            "XP", "ОП" -> stats["xp"] ?: "0"
            "HP", "ХП" -> stats["hp"] ?: "0"
            "MHP", "МХП" -> stats["max_hp"] ?: "0"
            "THP", "ВХП" -> stats["temp_hp"] ?: "0"
            "AC", "КД" -> stats["ac"] ?: "10"
            "EX", "ИСТ", "EXHAUSTION" -> stats["exhaustion"] ?: "0"
            "COND", "СОСТ", "CONDITIONS" -> stats["conditions"] ?: "0"
            "MHD", "МКХ" -> stats["totalMaxHitDice"] ?: stats["manualMaxHitDice"] ?: stats["level"] ?: "1"
            "CHD", "ТКХ" -> stats["totalCurrentHitDice"] ?: stats["chd"] ?: "0"
            "БМ", "PB", "PROF" -> {
                val level = stats["level"] ?: "1"
                stats["proficiencyBonus"] ?: getProficiencyBonus(level).toString()
            }
            "НАСТ БМ", "REAL PB" -> getProficiencyBonus(stats["level"] ?: "1").toString()
            "MATC", "МАТК" -> stats["[MAG ATC BON]"] ?: stats["[МАГ АТК БОН]"] ?: stats["mag_atk_bonus"] ?: "0"
            "MSAVEB", "МСПАСБ" -> stats["[MAG SAVE BON]"] ?: stats["[МАГ СПАС БОН]"] ?: stats["mag_save_bonus"] ?: "0"
            "MMOD", "ММОД" -> stats["[MAG MOD]"] ?: stats["[МАГ МОД]"] ?: stats["mag_mod"] ?: "0"
            else -> stats[token] ?: stats[upperToken] ?: "0"
        }
    }

    return tokenRegex.replace(formula) { match ->
        getStatValue(match.groupValues[1])
    }
}

/**
 * Оценка математической формулы (целочисленная).
 */
fun evaluateFormula(formula: String, stats: Map<String, String> = emptyMap()): Int {
    return evaluateFormulaDouble(formula, stats).roundToInt()
}

/**
 * Оценка математической формулы с плавающей точкой.
 */
fun evaluateFormulaDouble(formula: String, stats: Map<String, String> = emptyMap()): Double {
    if (formula.isBlank()) return 0.0
    val processed = preprocessFormula(formula, stats)
    return try {
        FormulaParser(processed).parse()
    } catch (e: Exception) {
        0.0
    }
}

/**
 * Парсер математических выражений методом рекурсивного спуска.
 */
private class FormulaParser(private val input: String) {
    private var pos = -1
    private var ch = 0

    private fun nextChar() {
        ch = if (++pos < input.length) input[pos].code else -1
    }

    private fun eat(charToEat: Int): Boolean {
        while (ch == ' '.code) nextChar()
        if (ch == charToEat) {
            nextChar()
            return true
        }
        return false
    }

    fun parse(): Double {
        nextChar()
        return parseExpression()
    }

    private fun parseExpression(): Double {
        var x = parseTerm()
        while (true) {
            if (eat('+'.code)) x += parseTerm()
            else if (eat('-'.code)) x -= parseTerm()
            else return x
        }
    }

    private fun parseTerm(): Double {
        var x = parseFactor()
        while (true) {
            if (eat('*'.code)) x *= parseFactor()
            else if (eat('/'.code)) {
                val y = parseFactor()
                x = if (y != 0.0) x / y else 0.0
            } else return x
        }
    }

    private fun parseFactor(): Double {
        if (eat('+'.code)) return parseFactor()
        if (eat('-'.code)) return -parseFactor()

        var x: Double
        val startPos = this.pos
        if (eat('('.code)) {
            x = parseExpression()
            eat(')'.code)
        } else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) {
            while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) nextChar()
            x = input.substring(startPos, this.pos).toDouble()
        } else if (isLetter(ch)) {
            while (isLetter(ch) || (ch >= '0'.code && ch <= '9'.code)) nextChar()
            val funcOrDice = input.substring(startPos, this.pos).lowercase()
            
            // Проверка на кости без префикса (например, "d6")
            if (funcOrDice.startsWith("d") || funcOrDice.startsWith("k") || funcOrDice.startsWith("к")) {
                x = handleDiceSuffix(1.0, funcOrDice)
            } else {
                val args = mutableListOf<Double>()
                if (eat('('.code)) {
                    do {
                        args.add(parseExpression())
                    } while (eat(','.code) || eat(';'.code))
                    eat(')'.code)
                }
                
                x = when (funcOrDice) {
                    "min", "мин" -> args.minOrNull() ?: 0.0
                    "max", "макс" -> args.maxOrNull() ?: 0.0
                    "floor", "вниз" -> floor(args.firstOrNull() ?: 0.0)
                    "ceil", "вверх" -> ceil(args.firstOrNull() ?: 0.0)
                    "abs" -> abs(args.firstOrNull() ?: 0.0)
                    "round" -> round(args.firstOrNull() ?: 0.0)
                    else -> 0.0
                }
            }
        } else {
            x = 0.0
        }

        // Проверка на суффикс кубов (например, "1d6")
        if (ch == 'd'.code || ch == 'k'.code || ch == 'к'.code) {
            val startDice = pos
            nextChar() // eat d/k
            while (ch >= '0'.code && ch <= '9'.code) nextChar()
            val diceStr = input.substring(startDice, pos)
            x = handleDiceSuffix(x, diceStr)
        }

        if (eat('^'.code)) x = x.pow(parseFactor())

        return x
    }

    private fun isLetter(c: Int): Boolean {
        return (c >= 'a'.code && c <= 'z'.code) || (c >= 'A'.code && c <= 'Z'.code) || 
               (c >= 'а'.code && c <= 'я'.code) || (c >= 'А'.code && c <= 'Я'.code)
    }

    private fun handleDiceSuffix(count: Double, diceStr: String): Double {
        // diceStr looks like "d6", "k20", etc.
        if (!DiceCalculationConfig.isDiceCalculationEnabled) return 0.0
        
        // В evaluateFormula мы не делаем реальных бросков, так как это чистая функция оценки.
        // Обычно такие формулы используются для отображения или расчета статических значений.
        // Если требуется именно РОЛЛ, это делает DiceRoller.
        // Однако, для корректного парсинга "1d4 + 10" при отключенных кубах мы возвращаем 0 для кубов.
        // Если кубы включены, но мы в evaluateFormula, мы можем вернуть среднее или 0.
        // Пользователь просил: "при отключении просто игнорирует, условное, 1к4, а не превращает его в 14"
        return 0.0 
    }
}

fun parseConditions(content: String): List<ru.quasaris.characternexus.Condition> {
    return content.split("##").filter { it.isNotBlank() }.map { s ->
        val lines = s.trim().lines()
        ru.quasaris.characternexus.Condition(lines.firstOrNull()?.trim() ?: "", lines.drop(1).joinToString("\n").trim())
    }
}

fun scaleCantripFormula(formula: String, level: Int, noDamageAtLevel1: Boolean): String {
    val scaleFactor = when {
        level >= 17 -> 4
        level >= 11 -> 3
        level >= 5 -> 2
        else -> 1
    }
    
    val actualScale = if (noDamageAtLevel1) scaleFactor - 1 else scaleFactor
    if (actualScale <= 0) return "0"
    
    // Find all dice and multiply them
    val diceRegex = Regex("(\\d+)([dkк])(\\d+)", RegexOption.IGNORE_CASE)
    return diceRegex.replace(formula) { match ->
        val count = match.groupValues[1].toInt()
        val type = match.groupValues[2]
        val sides = match.groupValues[3]
        "${count * actualScale}$type$sides"
    }
}
