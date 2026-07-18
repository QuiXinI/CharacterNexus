package ru.quasaris.characters.master.tabs.attacks

import ru.quasaris.characters.master.Attribute

data class DicePart(val count: Int, val sides: Int)

fun parseFormulaParts(
    formula: String,
    attributeModifiers: Map<Attribute, Int> = emptyMap(),
    proficiencyBonus: Int = 0,
    stats: Map<String, String> = emptyMap()
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
        
        // Add support for scores/values
        val statKey = when(attr) {
            Attribute.STRENGTH -> "strength"
            Attribute.DEXTERITY -> "dexterity"
            Attribute.CONSTITUTION -> "constitution"
            Attribute.INTELLIGENCE -> "intelligence"
            Attribute.WISDOM -> "wisdom"
            Attribute.CHARISMA -> "charisma"
            else -> null
        }
        if (statKey != null) {
            val score = stats[statKey] ?: "10"
            processed = processed.replace("[$shortName ЗНАЧ]", score)
                .replace("[$shortName SCR]", score)
        }
    }
    
    processed = processed.replace("[БМ]", " $proficiencyBonus ")
        .replace("[PB]", " $proficiencyBonus ")
        .replace("[PROF]", " $proficiencyBonus ")
    
    val level = stats["level"] ?: "1"
    processed = processed.replace("[LVL]", " $level ")
        .replace("[УР]", " $level ")
        .replace("[LEVEL]", " $level ")

    val xp = stats["xp"] ?: "0"
    processed = processed.replace("[XP]", " $xp ").replace("[ОП]", " $xp ")

    val hp = stats["hp"] ?: "0"
    processed = processed.replace("[HP]", " $hp ").replace("[ХП]", " $hp ")

    val mhp = stats["max_hp"] ?: "0"
    processed = processed.replace("[MHP]", " $mhp ").replace("[МХП]", " $mhp ")

    val thp = stats["temp_hp"] ?: "0"
    processed = processed.replace("[THP]", " $thp ").replace("[ВХП]", " $thp ")

    val ac = stats["ac"] ?: "10"
    processed = processed.replace("[AC]", " $ac ").replace("[КД]", " $ac ")

    val ex = stats["exhaustion"] ?: "0"
    processed = processed.replace("[EX]", " $ex ").replace("[ИСТ]", " $ex ")

    val cond = stats["conditions"] ?: "0"
    processed = processed.replace("[COND]", " $cond ").replace("[СОСТ]", " $cond ")

    // Add Magic Bonuses
    val magAtk = stats["[MAG ATC BON]"] ?: stats["[МАГ АТК БОН]"] ?: "0"
    val magSave = stats["[MAG SAVE BON]"] ?: stats["[МАГ СПАС БОН]"] ?: "0"
    val magMod = stats["[MAG MOD]"] ?: stats["[МАГ МОД]"] ?: "0"
    
    processed = processed.replace("[MAG ATC BON]", " $magAtk ")
        .replace("[МАГ АТК БОН]", " $magAtk ")
        .replace("[MAG SAVE BON]", " $magSave ")
        .replace("[МАГ СПАС БОН]", " $magSave ")
        .replace("[MAG MOD]", " $magMod ")
        .replace("[МАГ МОД]", " $magMod ")
    
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
    bonusFormulas: List<String>,
    attributeModifiers: Map<Attribute, Int>,
    proficiencyBonus: Int,
    stats: Map<String, String> = emptyMap()
): String {
    val parts = mutableListOf<String>()
    
    // Process base formula
    val (baseFlat, baseDice) = parseFormulaParts(baseFormula, attributeModifiers, proficiencyBonus, stats)
    baseDice.forEach { parts.add("${it.count}d${it.sides}") }
    if (baseFlat != 0) parts.add(if (baseFlat > 0) baseFlat.toString() else "($baseFlat)")
    
    // Add legacy base damage bonus if any
    if (baseDamageBonus != 0) {
        parts.add(if (baseDamageBonus > 0) baseDamageBonus.toString() else "($baseDamageBonus)")
    }
    
    // Process additional bonuses
    bonusFormulas.forEach { formula ->
        val (fFlat, fDice) = parseFormulaParts(formula, attributeModifiers, proficiencyBonus, stats)
        fDice.forEach { parts.add("${it.count}d${it.sides}") }
        if (fFlat != 0) parts.add(if (fFlat > 0) fFlat.toString() else "($fFlat)")
    }
    
    return parts.joinToString(" + ").replace("+ -", "- ")
}

fun calculateTotalBonus(
    formulas: List<String>,
    attributeModifiers: Map<Attribute, Int>,
    proficiencyBonus: Int,
    stats: Map<String, String> = emptyMap()
): Int {
    var total = 0
    formulas.forEach { formula ->
        val (flat, _) = parseFormulaParts(formula, attributeModifiers, proficiencyBonus, stats)
        total += flat
    }
    return total
}
