package ru.quasaris.characters.master.attacks

import ru.quasaris.characters.master.Attribute

data class DicePart(val count: Int, val sides: Int)

fun parseFormulaParts(
    formula: String,
    attributeModifiers: Map<Attribute, Int> = emptyMap(),
    proficiencyBonus: Int = 0
): Pair<Int, List<DicePart>> {
    var processed = formula.uppercase()
    
    val statMap = mapOf(
        "СИЛ" to Attribute.STRENGTH, "STR" to Attribute.STRENGTH,
        "ЛОВ" to Attribute.DEXTERITY, "DEX" to Attribute.DEXTERITY,
        "ТЕЛ" to Attribute.CONSTITUTION, "CON" to Attribute.CONSTITUTION,
        "ИНТ" to Attribute.INTELLIGENCE, "INT" to Attribute.INTELLIGENCE,
        "МУД" to Attribute.WISDOM, "WIS" to Attribute.WISDOM,
        "ХАР" to Attribute.CHARISMA, "CHA" to Attribute.CHARISMA
    )
    
    statMap.forEach { (shortName, attr) ->
        val mod = attributeModifiers[attr] ?: 0
        processed = processed.replace("[$shortName]", " $mod ")
    }
    
    processed = processed.replace("[БМ]", " $proficiencyBonus ")
        .replace("[PB]", " $proficiencyBonus ")
    
    var flat = 0
    val dice = mutableMapOf<Int, Int>() // Sides -> Count
    
    val diceRegex = Regex("(\\d*)[dkк](\\d+)", RegexOption.IGNORE_CASE)
    val remainingFormula = diceRegex.replace(processed) { match ->
        val countStr = match.groupValues[1]
        val count = if (countStr.isEmpty()) 1 else countStr.toInt()
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
    
    val diceList = dice.map { DicePart(it.value, it.key) }.sortedBy { it.sides }
    
    return Pair(flat, diceList)
}

fun formatFullDamage(
    baseFormula: String,
    baseDamageBonus: Int,
    bonusFormulas: List<String>,
    attributeModifiers: Map<Attribute, Int>,
    proficiencyBonus: Int
): String {
    val parts = mutableListOf<String>()
    
    // Process base formula
    val (baseFlat, baseDice) = parseFormulaParts(baseFormula, attributeModifiers, proficiencyBonus)
    baseDice.forEach { parts.add("${it.count}d${it.sides}") }
    if (baseFlat != 0) parts.add(if (baseFlat > 0) baseFlat.toString() else "($baseFlat)")
    
    // Add legacy base damage bonus if any
    if (baseDamageBonus != 0) {
        parts.add(if (baseDamageBonus > 0) baseDamageBonus.toString() else "($baseDamageBonus)")
    }
    
    // Process additional bonuses
    bonusFormulas.forEach { formula ->
        val (fFlat, fDice) = parseFormulaParts(formula, attributeModifiers, proficiencyBonus)
        fDice.forEach { parts.add("${it.count}d${it.sides}") }
        if (fFlat != 0) parts.add(if (fFlat > 0) fFlat.toString() else "($fFlat)")
    }
    
    return parts.joinToString(" + ").replace("+ -", "- ")
}
