package ru.quasaris.characternexus.backend

import kotlin.math.*

/**
 * Configuration for dice-related calculations.
 */
object DiceCalculationConfig {
    var isDiceCalculationEnabled: Boolean = true
}

/**
 * Description of a dice part (e.g., 2d6).
 */
data class DicePart(val count: Int, val sides: Int)

/**
 * Calculates attribute modifier from score.
 */
fun calculateModifier(scoreStr: String): Int {
    val score = scoreStr.toIntOrNull() ?: 10
    return floor((score - 10) / 2.0).toInt()
}

/**
 * Gets base proficiency bonus by level.
 */
fun getProficiencyBonus(levelStr: String): Int {
    val level = levelStr.toIntOrNull() ?: 1
    return floor((level - 1) / 4.0).toInt() + 2
}

/**
 * Experience threshold for next level.
 */
fun getNextLevelThreshold(levelStr: String): String {
    val level = levelStr.toIntOrNull() ?: 1
    return when (level) {
        0, 1 -> "300"
        2 -> "900"
        3 -> "2700"
        4 -> "6500"
        5 -> "14000"
        6 -> "23000"
        7 -> "34000"
        8 -> "48000"
        9 -> "64000"
        10 -> "85000"
        11 -> "100000"
        12 -> "120000"
        13 -> "140000"
        14 -> "165000"
        15 -> "195000"
        16 -> "225000"
        17 -> "265000"
        18 -> "305000"
        19 -> "355000"
        else -> ((level - 19) * 50000 + 355000).toString()
    }
}

fun getPreviousLevelThreshold(levelStr: String): String {
    val level = levelStr.toIntOrNull() ?: 1
    return when (level) {
        0, 1 -> "0"
        2 -> "300"
        3 -> "900"
        4 -> "2700"
        5 -> "6500"
        6 -> "14000"
        7 -> "23000"
        8 -> "34000"
        9 -> "48000"
        10 -> "64000"
        11 -> "85000"
        12 -> "100000"
        13 -> "120000"
        14 -> "140000"
        15 -> "165000"
        16 -> "195000"
        17 -> "225000"
        18 -> "265000"
        19 -> "305000"
        20 -> "355000"
        else -> ((level - 20) * 50000 + 355000).toString()
    }
}

/**
 * Calculates level based on accumulated experience.
 */
fun calculateLevelFromExperience(expStr: String): Int {
    val exp = expStr.toLongOrNull() ?: 0L
    if (exp >= 355000) {
        return 20 + ((exp - 355000) / 50000).toInt()
    }
    return when {
        exp >= 305000 -> 19
        exp >= 265000 -> 18
        exp >= 225000 -> 17
        exp >= 195000 -> 16
        exp >= 165000 -> 15
        exp >= 140000 -> 14
        exp >= 120000 -> 13
        exp >= 100000 -> 12
        exp >= 85000 -> 11
        exp >= 64000 -> 10
        exp >= 48000 -> 9
        exp >= 34000 -> 8
        exp >= 23000 -> 7
        exp >= 14000 -> 6
        exp >= 6500 -> 5
        exp >= 2700 -> 4
        exp >= 900 -> 3
        exp >= 300 -> 2
        else -> 1
    }
}
