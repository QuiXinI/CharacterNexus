package ru.quasaris.characters.master.backend

import ru.quasaris.characters.master.Attribute
import ru.quasaris.characters.master.tabs.attacks.parseFormulaParts
import kotlin.random.Random

data class DieRoll(
    val value: Int,
    val sides: Int
)

enum class RollSourceType {
    ABILITY,
    SKILL,
    SAVING_THROW,
    ATTACK,
    OTHER
}

data class RollResult(
    val title: String,
    val total: Int,
    val breakdown: String,
    val isCriticalSuccess: Boolean = false,
    val isCriticalFailure: Boolean = false,
    val isDamage: Boolean = false,
    val mainD20: Int? = null,
    val bonusDice: List<DieRoll> = emptyList(),
    val flatBonuses: List<Int> = emptyList(),
    val sourceType: RollSourceType = RollSourceType.OTHER
)

object DiceRoller {

    fun roll(
        title: String,
        baseModifier: Int,
        bonusFormulas: List<String> = emptyList(),
        isDamage: Boolean = false,
        stats: Map<String, String> = emptyMap(),
        exhaustion: Int = 0,
        sourceType: RollSourceType = RollSourceType.OTHER
    ): RollResult {
        val components = mutableListOf<String>()
        val bonusDiceValues = mutableListOf<DieRoll>()
        val flatBonusValues = mutableListOf<Int>()
        var total = 0
        
        // Prepare attribute modifiers for parseFormulaParts
        val attrModifiers = mutableMapOf<Attribute, Int>()
        Attribute.entries.forEach { attr ->
            if (attr != Attribute.NONE) {
                val score = stats[attr.name.lowercase()] ?: "10"
                attrModifiers[attr] = calculateModifier(score)
            }
        }
        val pb = (stats["proficiencyBonus"] ?: stats["level"]?.let { getProficiencyBonus(it).toString() } ?: "2").replace("+", "").toIntOrNull() ?: 2

        if (!isDamage) {
            val d20 = Random.nextInt(1, 21)
            if (d20 == 1) {
                return RollResult(
                    title = title,
                    total = 1,
                    breakdown = "1",
                    isCriticalFailure = true,
                    isDamage = false,
                    mainD20 = 1,
                    sourceType = sourceType
                )
            }
            
            total = d20
            components.add(d20.toString())
            
            // Add base modifier
            if (baseModifier != 0) {
                total += baseModifier
                components.add(if (baseModifier > 0) baseModifier.toString() else "($baseModifier)")
                flatBonusValues.add(baseModifier)
            }
            
            // Add exhaustion penalty: -2 per level for all d20 checks
            if (exhaustion > 0) {
                val penalty = -2 * exhaustion
                total += penalty
                components.add("($penalty)")
                flatBonusValues.add(penalty)
            }
            
            // Add bonus formulas
            bonusFormulas.forEach { formula ->
                val (fFlat, fDice) = parseFormulaParts(formula, attrModifiers, pb)
                fDice.forEach { dice ->
                    repeat(kotlin.math.abs(dice.count)) {
                        val r = Random.nextInt(1, dice.sides + 1)
                        val sign = if (dice.count > 0) 1 else -1
                        val valWithSign = r * sign
                        total += valWithSign
                        components.add(if (sign > 0) r.toString() else "(-$r)")
                        bonusDiceValues.add(DieRoll(valWithSign, dice.sides))
                    }
                }
                if (fFlat != 0) {
                    total += fFlat
                    components.add(if (fFlat > 0) fFlat.toString() else "($fFlat)")
                    flatBonusValues.add(fFlat)
                }
            }
            
            return RollResult(
                title = title,
                total = total,
                breakdown = components.joinToString(" + ").replace("+ -", "- ").replace("+ (-", "- "),
                isCriticalSuccess = d20 == 20,
                isDamage = false,
                mainD20 = d20,
                bonusDice = bonusDiceValues,
                flatBonuses = flatBonusValues,
                sourceType = sourceType
            )
        } else {
            total = baseModifier
            if (baseModifier != 0) {
                components.add(baseModifier.toString())
                flatBonusValues.add(baseModifier)
            }
            
            bonusFormulas.forEach { formula ->
                val (fFlat, fDice) = parseFormulaParts(formula, attrModifiers, pb)
                fDice.forEach { dice ->
                    repeat(kotlin.math.abs(dice.count)) {
                        val r = Random.nextInt(1, dice.sides + 1)
                        val sign = if (dice.count > 0) 1 else -1
                        val valWithSign = r * sign
                        total += valWithSign
                        components.add(if (sign > 0) r.toString() else "(-$r)")
                        bonusDiceValues.add(DieRoll(valWithSign, dice.sides))
                    }
                }
                if (fFlat != 0) {
                    total += fFlat
                    components.add(if (fFlat > 0) fFlat.toString() else "($fFlat)")
                    flatBonusValues.add(fFlat)
                }
            }
            
            return RollResult(
                title = title,
                total = total,
                breakdown = components.joinToString(" + ").replace("+ -", "- ").replace("+ (-", "- "),
                isDamage = true,
                bonusDice = bonusDiceValues,
                flatBonuses = flatBonusValues,
                sourceType = sourceType
            )
        }
    }
}
