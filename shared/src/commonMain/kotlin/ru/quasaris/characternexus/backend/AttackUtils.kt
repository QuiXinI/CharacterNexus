package ru.quasaris.characternexus.backend

import ru.quasaris.characternexus.model.*

fun formatFullDamage(
    baseFormula: String,
    baseDamageBonus: Int,
    bonuses: List<IBonus>,
    stats: Map<String, String> = emptyMap(),
    renderInOrder: Boolean = true
): String {
    val activeBonuses = bonuses.filter { it.isActive }
    val overrides = activeBonuses.filter { it.operation == BonusOperation.OVERRIDE }
    
    val allParts = mutableListOf<FormulaPart>()
    
    if (overrides.isNotEmpty()) {
        // Если есть заменители, берем только их (последний по логике)
        val lastOverride = overrides.last()
        allParts.addAll(parseFormulaOrdered(lastOverride.formula, stats))
    } else {
        // Process base formula
        allParts.addAll(parseFormulaOrdered(baseFormula, stats))
        if (baseDamageBonus != 0) {
            allParts.add(FormulaPart.Flat(baseDamageBonus))
        }
        
        // Process additional bonuses
        activeBonuses.forEach { bonus ->
            val fParts = parseFormulaOrdered(bonus.formula, stats)
            when (bonus.operation) {
                BonusOperation.ADD -> allParts.addAll(fParts)
                BonusOperation.SUBTRACT -> {
                    fParts.forEach { part ->
                        when (part) {
                            is FormulaPart.Dice -> allParts.add(FormulaPart.Dice(-part.count, part.sides))
                            is FormulaPart.Flat -> allParts.add(FormulaPart.Flat(-part.value))
                        }
                    }
                }
                BonusOperation.OVERRIDE -> {} // Already handled
            }
        }
    }
    
    if (allParts.isEmpty()) return "0"

    val resultParts = mutableListOf<String>()
    
    if (renderInOrder) {
        allParts.forEach { part ->
            when (part) {
                is FormulaPart.Dice -> if (part.count != 0) resultParts.add("${part.count}d${part.sides}")
                is FormulaPart.Flat -> if (part.value != 0) resultParts.add(part.value.toString())
            }
        }
    } else {
        val combinedDice = mutableMapOf<Int, Int>()
        var flatTotal = 0
        allParts.forEach { part ->
            when (part) {
                is FormulaPart.Dice -> combinedDice[part.sides] = (combinedDice[part.sides] ?: 0) + part.count
                is FormulaPart.Flat -> flatTotal += part.value
            }
        }
        
        combinedDice.filter { it.value != 0 }.toList().sortedBy { it.first }.forEach { (sides, count) ->
            resultParts.add("${count}d$sides")
        }
        if (flatTotal != 0) {
            resultParts.add(flatTotal.toString())
        }
    }

    if (resultParts.isEmpty()) return "0"
    
    return resultParts.joinToString(" + ")
        .replace("+ -", "- ")
        .replace(" - -", " + ") 
        .trim()
        .removePrefix("+ ")
}

fun calculateTotalBonus(
    bonuses: List<IBonus>,
    stats: Map<String, String> = emptyMap(),
    initialValue: Int = 0
): Int {
    return applyBonuses(initialValue, bonuses, stats)
}
