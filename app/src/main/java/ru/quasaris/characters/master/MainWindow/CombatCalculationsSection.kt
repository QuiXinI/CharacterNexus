package ru.quasaris.characters.master.MainWindow

import ru.quasaris.characters.master.ArmorClassEntry
import ru.quasaris.characters.master.InitiativeEntry
import ru.quasaris.characters.master.SpeedEntry
import ru.quasaris.characters.master.ShieldEntry
import ru.quasaris.characters.master.backend.evaluateFormula

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
        var v = if (active != null) evaluateFormula(active.formula, statsMap) else 10
        if (isShieldActive) {
            val shield = shieldEntries.find { it.id == activeShieldId }
            if (shield != null) {
                v += evaluateFormula(shield.formula, statsMap)
            }
        }
        return v.toString()
    }

    fun calculateInitiative(activeInitiativeId: String?, initiativeEntries: List<InitiativeEntry>, statsMap: Map<String, String>, exhaustion: Int = 0): String {
        val active = initiativeEntries.find { it.id == activeInitiativeId }
        var v = if (active != null) evaluateFormula(active.formula, statsMap) else 0
        v -= exhaustion * 2
        return if (v >= 0) "+$v" else v.toString()
    }

    fun calculateSpeed(activeSpeedId: String?, speedEntries: List<SpeedEntry>, statsMap: Map<String, String>, exhaustion: Int = 0): String {
        val active = speedEntries.find { it.id == activeSpeedId }
        val v = (if (active != null) evaluateFormula(active.formula, statsMap) else 30)
        return maxOf(0, v - exhaustion * 5).toString()
    }
}
