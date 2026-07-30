package ru.quasaris.characters.master.tabs.attacks

import ru.quasaris.characters.master.IBonus
import ru.quasaris.characters.master.BonusOperation
import ru.quasaris.characters.master.backend.preprocessFormula

data class DicePart(val count: Int, val sides: Int)

fun parseFormulaParts(
    formula: String,
    stats: Map<String, String> = emptyMap()
): Pair<Int, List<DicePart>> {
    val processed = preprocessFormula(formula, stats)
    
    var flat = 0
    val dice = mutableMapOf<Int, Int>() // Sides -> Count
    
    val diceRegex = Regex("([+-]?\\d*)[dkк](\\d+)", RegexOption.IGNORE_CASE)
    val remainingFormula = diceRegex.replace(processed) { match ->
        val countStr = match.groupValues[1]
        val count = when (countStr) {
            "", "+" -> 1
            "-" -> -1
            else -> countStr.toIntOrNull() ?: 1
        }
        val sides = match.groupValues[2].toInt()
        if (sides > 0) {
            dice[sides] = (dice[sides] ?: 0) + count
        }
        ""
    }
    
    val flatRegex = Regex("(?<![dkк\\d])([+-]?\\d+)(?![dkк\\d])", RegexOption.IGNORE_CASE)
    flatRegex.findAll(remainingFormula).forEach { match ->
        flat += match.groupValues[1].toIntOrNull() ?: 0
    }
    
    val diceList = dice.map { DicePart(it.value, it.key) }
        .filter { it.count != 0 }
        .sortedBy { it.sides }
    
    return Pair(flat, diceList)
}

fun formatFullDamage(
    baseFormula: String,
    baseDamageBonus: Int,
    bonuses: List<IBonus>,
    stats: Map<String, String> = emptyMap()
): String {
    val diceParts = mutableListOf<DicePart>()
    var flatTotal = 0
    
    // Process base formula
    val (baseFlat, baseDice) = parseFormulaParts(baseFormula, stats)
    diceParts.addAll(baseDice)
    flatTotal = baseFlat + baseDamageBonus
    
    // Process additional bonuses
    bonuses.filter { it.isActive }.forEach { bonus ->
        val (fFlat, fDice) = parseFormulaParts(bonus.formula, stats)
        when (bonus.operation) {
            BonusOperation.ADD -> {
                diceParts.addAll(fDice)
                flatTotal += fFlat
            }
            BonusOperation.SUBTRACT -> {
                fDice.forEach { diceParts.add(DicePart(-it.count, it.sides)) }
                flatTotal -= fFlat
            }
            BonusOperation.OVERRIDE -> {
                diceParts.clear()
                diceParts.addAll(fDice)
                flatTotal = fFlat
            }
        }
    }
    
    val combinedDice = mutableMapOf<Int, Int>()
    diceParts.forEach { combinedDice[it.sides] = (combinedDice[it.sides] ?: 0) + it.count }
    
    val resultParts = mutableListOf<String>()
    combinedDice.filter { it.value != 0 }.toList().sortedBy { it.first }.forEach { (sides, count) ->
        resultParts.add("${count}d$sides")
    }
    if (flatTotal != 0) {
        resultParts.add(if (flatTotal > 0) flatTotal.toString() else "($flatTotal)")
    }
    
    return if (resultParts.isEmpty()) "0" else resultParts.joinToString(" + ").replace("+ -", "- ")
}

fun calculateTotalBonus(
    bonuses: List<IBonus>,
    stats: Map<String, String> = emptyMap(),
    initialValue: Int = 0
): Int {
    var total = initialValue
    bonuses.filter { it.isActive }.forEach { bonus ->
        val (flat, _) = parseFormulaParts(bonus.formula, stats)
        total = when (bonus.operation) {
            BonusOperation.ADD -> total + flat
            BonusOperation.SUBTRACT -> total - flat
            BonusOperation.OVERRIDE -> flat
        }
    }
    return total
}
