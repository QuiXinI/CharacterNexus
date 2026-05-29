package ru.quasaris.characters.master.MainWindow

import ru.quasaris.characters.master.ArmorClassEntry
import ru.quasaris.characters.master.InitiativeEntry
import ru.quasaris.characters.master.SpeedEntry
import ru.quasaris.characters.master.HeaderCode.evaluateFormula

object CombatCalculations {
    fun calculateAC(activeArmorClassId: String?, armorClassEntries: List<ArmorClassEntry>, statsMap: Map<String, String>): String {
        val active = armorClassEntries.find { it.id == activeArmorClassId }
        return if (active != null) evaluateFormula(active.formula, statsMap).toString() else "10"
    }

    fun calculateInitiative(activeInitiativeId: String?, initiativeEntries: List<InitiativeEntry>, statsMap: Map<String, String>): String {
        val active = initiativeEntries.find { it.id == activeInitiativeId }
        val v = if (active != null) evaluateFormula(active.formula, statsMap) else 0
        return if (v >= 0) "+$v" else v.toString()
    }

    fun calculateSpeed(activeSpeedId: String?, speedEntries: List<SpeedEntry>, statsMap: Map<String, String>): String {
        val active = speedEntries.find { it.id == activeSpeedId }
        return if (active != null) evaluateFormula(active.formula, statsMap).toString() else "30"
    }
}
