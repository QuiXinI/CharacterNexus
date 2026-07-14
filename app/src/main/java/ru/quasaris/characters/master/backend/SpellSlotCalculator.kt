package ru.quasaris.characters.master.backend

import ru.quasaris.characters.master.CasterType
import kotlin.math.ceil

object SpellSlotCalculator {

    private val fullCasterTable = listOf(
        listOf(2, 0, 0, 0, 0, 0, 0, 0, 0),
        listOf(3, 0, 0, 0, 0, 0, 0, 0, 0),
        listOf(4, 2, 0, 0, 0, 0, 0, 0, 0),
        listOf(4, 3, 0, 0, 0, 0, 0, 0, 0),
        listOf(4, 3, 2, 0, 0, 0, 0, 0, 0),
        listOf(4, 3, 3, 0, 0, 0, 0, 0, 0),
        listOf(4, 3, 3, 1, 0, 0, 0, 0, 0),
        listOf(4, 3, 3, 2, 0, 0, 0, 0, 0),
        listOf(4, 3, 3, 3, 1, 0, 0, 0, 0),
        listOf(4, 3, 3, 3, 2, 0, 0, 0, 0),
        listOf(4, 3, 3, 3, 2, 1, 0, 0, 0),
        listOf(4, 3, 3, 3, 2, 1, 0, 0, 0),
        listOf(4, 3, 3, 3, 2, 1, 1, 0, 0),
        listOf(4, 3, 3, 3, 2, 1, 1, 0, 0),
        listOf(4, 3, 3, 3, 2, 1, 1, 1, 0),
        listOf(4, 3, 3, 3, 2, 1, 1, 1, 0),
        listOf(4, 3, 3, 3, 2, 1, 1, 1, 1),
        listOf(4, 3, 3, 3, 3, 1, 1, 1, 1),
        listOf(4, 3, 3, 3, 3, 2, 1, 1, 1),
        listOf(4, 3, 3, 3, 3, 2, 2, 1, 1)
    )

    fun getSlotsForLevel(casterType: CasterType, characterLevel: Int): List<Int> {
        val effectiveLevel = when (casterType) {
            CasterType.FULL -> characterLevel
            CasterType.HALF -> if (characterLevel < 2) 0 else (characterLevel + 1) / 2
            CasterType.THIRD -> if (characterLevel < 3) 0 else (characterLevel + 2) / 3
            CasterType.NONE -> 0
        }

        return if (effectiveLevel > 0) {
            fullCasterTable[effectiveLevel.coerceIn(1, 20) - 1]
        } else {
            List(9) { 0 }
        }
    }

    fun getMulticlassSlots(full: Int, half: Int, third: Int): List<Int> {
        val effectiveLevel = full + ceil(half / 2.0).toInt() + ceil(third / 3.0).toInt()

        return if (effectiveLevel > 0) {
            fullCasterTable[effectiveLevel.coerceIn(1, 20) - 1]
        } else {
            List(9) { 0 }
        }
    }
}
