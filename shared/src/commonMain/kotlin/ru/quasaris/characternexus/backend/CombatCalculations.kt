package ru.quasaris.characternexus.backend

import ru.quasaris.characternexus.model.*

object CombatCalculations {
    fun calculateAC(
        activeArmorClassId: String?, 
        armorClassEntries: List<ArmorClassEntry>, 
        statsMap: Map<String, String>,
        isShieldActive: Boolean = false,
        activeShieldId: String? = null,
        shieldEntries: List<ShieldEntry> = emptyList()
    ): String {
        val active = armorClassEntries.find { it.id == activeArmorClassId }
        val base = if (active != null) evaluateFormula(active.formula, statsMap) else 10
        
        var total = if (active != null) {
            applyBonuses(base, active.bonuses, statsMap)
        } else base

        if (isShieldActive) {
            val shield = shieldEntries.find { it.id == activeShieldId }
            if (shield != null) {
                val sVal = evaluateFormula(shield.formula, statsMap)
                val sTotal = applyBonuses(sVal, shield.bonuses, statsMap)
                total += sTotal
            }
        }
        return total.toString()
    }

    fun calculateInitiative(activeInitiativeId: String?, initiativeEntries: List<InitiativeEntry>, statsMap: Map<String, String>, exhaustion: Int = 0, isJackOfAllTrades: Boolean = false): String {
        val active = initiativeEntries.find { it.id == activeInitiativeId }
        val base = if (active != null) evaluateFormula(active.formula, statsMap) else 0
        
        var v = if (active != null) {
            applyBonuses(base, active.bonuses, statsMap)
        } else base
        
        if (isJackOfAllTrades) {
            val pbStr = statsMap["proficiencyBonus"] ?: "2"
            val pb = pbStr.replace("+", "").toIntOrNull() ?: 2
            v += pb / 2
        }

        v -= exhaustion * 2
        return if (v >= 0) "+$v" else v.toString()
    }

    fun calculateSpeed(activeSpeedId: String?, speedEntries: List<SpeedEntry>, statsMap: Map<String, String>, exhaustion: Int = 0): String {
        val active = speedEntries.find { it.id == activeSpeedId }
        val base = if (active != null) evaluateFormula(active.formula, statsMap) else 30
        
        val v = if (active != null) {
            applyBonuses(base, active.bonuses, statsMap)
        } else base
        
        return maxOf(0, v - exhaustion * 5).toString()
    }

    fun calculateCargo(state: CargoState, statsMap: Map<String, String>): Pair<Int, Int> {
        val attrValue = statsMap[state.carryAttribute.name.lowercase()]?.toIntOrNull() ?: 10
        val effectiveSize = if (state.useOverrideSize) state.overrideSize else state.size
        val baseCarry = (attrValue * 15 * effectiveSize.carryMultiplier).toInt()
        val basePush = (attrValue * 30 * effectiveSize.carryMultiplier).toInt()

        val carry = applyBonuses(baseCarry, state.carryBonuses, statsMap)
        val push = applyBonuses(basePush, state.pushBonuses, statsMap)

        return carry to push
    }

    fun calculateJumping(state: CargoState, statsMap: Map<String, String>): Map<String, Int> {
        val attrValue = statsMap[state.jumpAttribute.name.lowercase()]?.toIntOrNull() ?: 10
        val attrMod = calculateModifier(attrValue.toString())

        val runningLong = applyBonuses(attrValue, state.longJumpBonuses, statsMap)
        val standingLong = runningLong / 2

        val runningHigh = applyBonuses(3 + attrMod, state.highJumpBonuses, statsMap)
        val standingHigh = runningHigh / 2

        return mapOf(
            "runningLong" to runningLong,
            "standingLong" to standingLong,
            "runningHigh" to runningHigh,
            "standingHigh" to standingHigh
        )
    }
}
