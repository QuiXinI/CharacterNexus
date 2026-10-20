package ru.quasaris.characternexus.tabs.attacks

import ru.quasaris.characternexus.model.*
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

fun formatDistanceValue(value: String): String {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return ""
    return if (trimmed.all { it.isDigit() }) "$trimmed фт." else trimmed
}

fun formatRangeText(rangeNormal: String, rangeMax: String): String {
    val normRaw = rangeNormal.trim()
    val maxRaw = rangeMax.trim()

    val normIsDigits = normRaw.isNotEmpty() && normRaw.all { it.isDigit() }
    val maxIsDigits = maxRaw.isNotEmpty() && maxRaw.all { it.isDigit() }

    return when {
        normRaw.isNotEmpty() && maxRaw.isNotEmpty() -> {
            if (normIsDigits && maxIsDigits) {
                "$normRaw/$maxRaw фт."
            } else {
                val normFormatted = formatDistanceValue(normRaw)
                val maxFormatted = formatDistanceValue(maxRaw)
                if (normFormatted.endsWith(" фт.") && maxFormatted.endsWith(" фт.")) {
                    val normClean = normFormatted.removeSuffix(" фт.").trim()
                    val maxClean = maxFormatted.removeSuffix(" фт.").trim()
                    "$normClean/$maxClean фт."
                } else {
                    "$normFormatted / $maxFormatted"
                }
            }
        }
        normRaw.isNotEmpty() -> {
            formatDistanceValue(normRaw)
        }
        maxRaw.isNotEmpty() -> {
            if (maxIsDigits) "—/$maxRaw фт." else "— / ${formatDistanceValue(maxRaw)}"
        }
        else -> ""
    }
}

fun List<AttackEntry>.linkAttacks(secondaryId: String, primaryId: String): List<AttackEntry> {
    if (secondaryId == primaryId || secondaryId.isEmpty() || primaryId.isEmpty()) return this
    val currentSecondary = find { it.id == secondaryId } ?: return this
    val currentPrimary = find { it.id == primaryId } ?: return this

    val newAttacks = toMutableList()

    val formerSecondaries = currentSecondary.linkedIds
    if (formerSecondaries.isNotEmpty()) {
        formerSecondaries.forEach { secId ->
            val idx = newAttacks.indexOfFirst { it.id == secId }
            if (idx != -1) {
                newAttacks[idx] = newAttacks[idx].copy(linkedTo = primaryId)
            }
        }
    }

    val secIdx = newAttacks.indexOfFirst { it.id == secondaryId }
    if (secIdx != -1) {
        newAttacks[secIdx] = currentSecondary.copy(
            linkedTo = primaryId,
            linkedIds = emptyList()
        )
    }

    val primIdx = newAttacks.indexOfFirst { it.id == primaryId }
    if (primIdx != -1) {
        val updatedLinkedIds = (currentPrimary.linkedIds + secondaryId + formerSecondaries).distinct()
        newAttacks[primIdx] = currentPrimary.copy(linkedIds = updatedLinkedIds)
    }

    return newAttacks
}

fun List<AttackEntry>.unlinkAttack(targetId: String): List<AttackEntry> {
    if (targetId.isEmpty()) return this
    val target = find { it.id == targetId } ?: return this

    val newAttacks = toMutableList()

    if (target.linkedTo != null) {
        // Unlinking a secondary attack
        val primaryId = target.linkedTo
        val primaryIdx = newAttacks.indexOfFirst { it.id == primaryId }
        val primary = if (primaryIdx != -1) newAttacks[primaryIdx] else null

        if (primary != null) {
            newAttacks[primaryIdx] = primary.copy(
                linkedIds = primary.linkedIds.filter { it != targetId }
            )
            val secIdx = newAttacks.indexOfFirst { it.id == targetId }
            if (secIdx != -1) {
                newAttacks[secIdx] = primary.copy(
                    id = targetId,
                    linkedTo = null,
                    linkedIds = emptyList()
                )
            }
        } else {
            val secIdx = newAttacks.indexOfFirst { it.id == targetId }
            if (secIdx != -1) {
                newAttacks[secIdx] = target.copy(
                    linkedTo = null,
                    linkedIds = emptyList()
                )
            }
        }
    } else if (target.linkedIds.isNotEmpty()) {
        // Unlinking a primary attack
        val newPrimaryId = target.linkedIds.first()
        val remainingLinkedIds = target.linkedIds.drop(1)

        val newPrimaryIdx = newAttacks.indexOfFirst { it.id == newPrimaryId }
        if (newPrimaryIdx != -1) {
            // First secondary becomes new primary with copy of target's data
            newAttacks[newPrimaryIdx] = target.copy(
                id = newPrimaryId,
                linkedTo = null,
                linkedIds = remainingLinkedIds
            )
            // Relink remaining secondaries to new primary
            remainingLinkedIds.forEach { secId ->
                val secIdx = newAttacks.indexOfFirst { it.id == secId }
                if (secIdx != -1) {
                    newAttacks[secIdx] = newAttacks[secIdx].copy(
                        linkedTo = newPrimaryId,
                        linkedIds = emptyList()
                    )
                }
            }
        }
        // Old primary becomes independent with copy of its data
        val oldPrimaryIdx = newAttacks.indexOfFirst { it.id == targetId }
        if (oldPrimaryIdx != -1) {
            newAttacks[oldPrimaryIdx] = target.copy(
                id = targetId,
                linkedTo = null,
                linkedIds = emptyList()
            )
        }
    } else {
        val idx = newAttacks.indexOfFirst { it.id == targetId }
        if (idx != -1) {
            newAttacks[idx] = target.copy(linkedTo = null, linkedIds = emptyList())
        }
    }

    return newAttacks
}


