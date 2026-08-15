package ru.quasaris.characternexus

import kotlinx.serialization.Serializable

@Serializable
data class DieRoll(
    val value: Int,
    val sides: Int,
    val discardedValue: Int? = null
)

@Serializable
sealed class RollPart {
    @Serializable
    data class Dice(val roll: DieRoll) : RollPart()
    @Serializable
    data class Flat(val value: Int) : RollPart()
}

@Serializable
enum class AdvantageType {
    NONE,
    ADVANTAGE,
    DISADVANTAGE,
    CRITICAL
}

@Serializable
enum class AdvantageLogic {
    TOTAL,       // Общее значение (по умолчанию)
    INDIVIDUAL,  // Покубово
    SOURCE,      // По источнику
    POOL         // Общий пулл
}

@Serializable
enum class RollSourceType {
    ABILITY,
    SKILL,
    SAVING_THROW,
    ATTACK,
    OTHER
}

@Serializable
enum class DiceRollPosition {
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
}

@Serializable
data class RollResult(
    val title: String,
    val total: Int,
    val breakdown: String,
    val isCriticalSuccess: Boolean = false,
    val isCriticalFailure: Boolean = false,
    val isDamage: Boolean = false,
    val isHealing: Boolean = false,
    val mainD20: Int? = null,
    val alternativeD20: Int? = null,
    val bonusDice: List<DieRoll> = emptyList(),
    val flatBonuses: List<Int> = emptyList(),
    val alternativeDice: List<DieRoll>? = null,
    val alternativeFlatBonuses: List<Int>? = null,
    val sourceType: RollSourceType = RollSourceType.OTHER,
    val advantageType: AdvantageType = AdvantageType.NONE,
    val unusedTotal: Int? = null,
    val orderedParts: List<RollPart> = emptyList(),
    val altOrderedParts: List<RollPart>? = null
)
