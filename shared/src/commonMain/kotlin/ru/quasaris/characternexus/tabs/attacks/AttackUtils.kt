package ru.quasaris.characternexus.tabs.attacks

import ru.quasaris.characternexus.IBonus
import ru.quasaris.characternexus.BonusOperation
import ru.quasaris.characternexus.backend.FormulaPart
import ru.quasaris.characternexus.backend.parseFormulaOrdered
import ru.quasaris.characternexus.backend.DicePart
import ru.quasaris.characternexus.backend.parseFormulaParts

fun formatFullDamage(
    baseFormula: String,
    baseDamageBonus: Int,
    bonuses: List<IBonus>,
    stats: Map<String, String> = emptyMap(),
    renderInOrder: Boolean = true
): String {
    val allParts = mutableListOf<FormulaPart>()
    
    // Process base formula
    allParts.addAll(parseFormulaOrdered(baseFormula, stats))
    if (baseDamageBonus != 0) {
        allParts.add(FormulaPart.Flat(baseDamageBonus))
    }
    
    // Process additional bonuses
    bonuses.filter { it.isActive }.forEach { bonus ->
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
            BonusOperation.OVERRIDE -> {
                allParts.clear()
                allParts.addAll(fParts)
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
        // Grouping logic for backward compatibility or different settings
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
    
    // Join and fix signs for display
    return resultParts.joinToString(" + ")
        .replace("+ -", "- ")
        .replace(" - -", " + ") // double negative
        .trim()
        .removePrefix("+ ")
}

fun calculateAttackFormulaParts(
    baseFlat: Int,
    baseFormula: String = "",
    bonuses: List<IBonus>,
    stats: Map<String, String> = emptyMap(),
    renderInOrder: Boolean = true
): Pair<Int, List<DicePart>> {
    var totalFlat = baseFlat
    val allDiceList = mutableListOf<DicePart>()

    // Process base formula if provided
    if (baseFormula.isNotBlank()) {
        val (fFlat, fDice) = parseFormulaParts(baseFormula, stats)
        totalFlat += fFlat
        allDiceList.addAll(fDice)
    }

    bonuses.filter { it.isActive }.forEach { bonus ->
        val (fFlat, fDice) = parseFormulaParts(bonus.formula, stats)
        when (bonus.operation) {
            BonusOperation.ADD -> {
                totalFlat += fFlat
                allDiceList.addAll(fDice)
            }
            BonusOperation.SUBTRACT -> {
                totalFlat -= fFlat
                fDice.forEach { allDiceList.add(DicePart(-it.count, it.sides)) }
            }
            BonusOperation.OVERRIDE -> {
                totalFlat = fFlat
                allDiceList.clear()
                allDiceList.addAll(fDice)
            }
        }
    }

    val finalDice = if (renderInOrder) {
        allDiceList
    } else {
        allDiceList.groupBy { it.sides }
            .map { (sides, parts) -> DicePart(parts.sumOf { it.count }, sides) }
            .filter { it.count != 0 }
            .sortedBy { it.sides }
    }

    return totalFlat to finalDice
}

fun calculateTotalBonus(
    bonuses: List<IBonus>,
    stats: Map<String, String> = emptyMap(),
    initialValue: Int = 0
): Int {
    var total = initialValue
    bonuses.filter { it.isActive }.forEach { bonus ->
        val parts = parseFormulaOrdered(bonus.formula, stats)
        var bonusFlat = 0
        parts.forEach { if (it is FormulaPart.Flat) bonusFlat += it.value }
        
        total = when (bonus.operation) {
            BonusOperation.ADD -> total + bonusFlat
            BonusOperation.SUBTRACT -> total - bonusFlat
            BonusOperation.OVERRIDE -> bonusFlat
        }
    }
    return total
}
