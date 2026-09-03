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

    fun calculateInitiative(activeInitiativeId: String?, initiativeEntries: List<InitiativeEntry>, statsMap: Map<String, String>, exhaustion: Int = 0): String {
        val active = initiativeEntries.find { it.id == activeInitiativeId }
        val base = if (active != null) evaluateFormula(active.formula, statsMap) else 0
        
        var v = if (active != null) {
            applyBonuses(base, active.bonuses, statsMap)
        } else base
        
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
}
