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
        var base = if (active != null) evaluateFormula(active.formula, statsMap) else 10
        
        if (active != null) {
            base = calculateTotalBonus(active.bonuses, statsMap, base)
        }

        if (isShieldActive) {
            val shield = shieldEntries.find { it.id == activeShieldId }
            if (shield != null) {
                var sVal = evaluateFormula(shield.formula, statsMap)
                sVal = calculateTotalBonus(shield.bonuses, statsMap, sVal)
                base += sVal
            }
        }
        return base.toString()
    }

    fun calculateInitiative(activeInitiativeId: String?, initiativeEntries: List<InitiativeEntry>, statsMap: Map<String, String>, exhaustion: Int = 0): String {
        val active = initiativeEntries.find { it.id == activeInitiativeId }
        var v = if (active != null) evaluateFormula(active.formula, statsMap) else 0
        
        if (active != null) {
            v = calculateTotalBonus(active.bonuses, statsMap, v)
        }
        
        v -= exhaustion * 2
        return if (v >= 0) "+$v" else v.toString()
    }

    fun calculateSpeed(activeSpeedId: String?, speedEntries: List<SpeedEntry>, statsMap: Map<String, String>, exhaustion: Int = 0): String {
        val active = speedEntries.find { it.id == activeSpeedId }
        var v = (if (active != null) evaluateFormula(active.formula, statsMap) else 30)
        
        if (active != null) {
            v = calculateTotalBonus(active.bonuses, statsMap, v)
        }
        
        return maxOf(0, v - exhaustion * 5).toString()
    }
}
