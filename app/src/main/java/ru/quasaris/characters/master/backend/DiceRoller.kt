package ru.quasaris.characters.master.backend

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import ru.quasaris.characters.master.Attribute
import ru.quasaris.characters.master.IBonus
import ru.quasaris.characters.master.BonusOperation
import ru.quasaris.characters.master.AdvantagePreference
import ru.quasaris.characters.master.backend.parseFormulaParts
import ru.quasaris.characters.master.backend.calculateModifier
import ru.quasaris.characters.master.backend.getProficiencyBonus
import java.util.UUID
import kotlin.random.Random

data class DieRoll(
    val value: Int,
    val sides: Int,
    val discardedValue: Int? = null
)

enum class AdvantageType {
    NONE,
    ADVANTAGE,
    DISADVANTAGE,
    CRITICAL
}

enum class AdvantageLogic {
    TOTAL,       // Общее значение (по умолчанию)
    INDIVIDUAL,  // Покубово
    SOURCE,      // По источнику
    POOL         // Общий пулл
}

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
    val alternativeD20: Int? = null,
    val bonusDice: List<DieRoll> = emptyList(),
    val flatBonuses: List<Int> = emptyList(),
    val alternativeDice: List<DieRoll>? = null,
    val alternativeFlatBonuses: List<Int>? = null,
    val sourceType: RollSourceType = RollSourceType.OTHER,
    val advantageType: AdvantageType = AdvantageType.NONE,
    val unusedTotal: Int? = null
)

data class SimpleBonus(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String = "",
    override val formula: String = "",
    override val isActive: Boolean = true,
    override val operation: BonusOperation = BonusOperation.ADD,
    override val advantagePreference: AdvantagePreference = AdvantagePreference.NONE
) : IBonus

object DiceRoller {

    fun roll(
        title: String,
        baseModifier: Int,
        bonuses: List<IBonus> = emptyList(),
        isDamage: Boolean = false,
        stats: Map<String, String> = emptyMap(),
        exhaustion: Int = 0,
        sourceType: RollSourceType = RollSourceType.OTHER,
        advantageType: AdvantageType = AdvantageType.NONE,
        advantageLogic: AdvantageLogic = AdvantageLogic.TOTAL
    ): RollResult {
        val attrModifiers = mutableMapOf<Attribute, Int>()
        Attribute.entries.forEach { attr ->
            if (attr != Attribute.NONE) {
                val score = stats[attr.name.lowercase()] ?: "10"
                attrModifiers[attr] = calculateModifier(score)
            }
        }
        val pb = (stats["proficiencyBonus"] ?: stats["level"]?.let { getProficiencyBonus(it).toString() } ?: "2").replace("+", "").toIntOrNull() ?: 2

        val activeBonuses = bonuses.filter { it.isActive }

        return when (advantageLogic) {
            AdvantageLogic.TOTAL -> rollTotal(title, baseModifier, activeBonuses, isDamage, exhaustion, sourceType, advantageType, stats)
            AdvantageLogic.INDIVIDUAL -> rollIndividual(title, baseModifier, activeBonuses, isDamage, exhaustion, sourceType, advantageType, attrModifiers, pb, stats)
            AdvantageLogic.SOURCE -> rollSource(title, baseModifier, activeBonuses, isDamage, exhaustion, sourceType, advantageType, attrModifiers, pb, stats)
            AdvantageLogic.POOL -> rollPool(title, baseModifier, activeBonuses, isDamage, exhaustion, sourceType, advantageType, attrModifiers, pb, stats)
        }
    }

    private fun resolveAdvantage(global: AdvantageType, preference: AdvantagePreference): AdvantageType {
        // IGNORE_BOTH always wins
        if (preference == AdvantagePreference.IGNORE_BOTH) return AdvantageType.NONE

        val prefType = when (preference) {
            AdvantagePreference.ALWAYS_ADVANTAGE -> AdvantageType.ADVANTAGE
            AdvantagePreference.ALWAYS_DISADVANTAGE -> AdvantageType.DISADVANTAGE
            AdvantagePreference.IGNORE_ADVANTAGE -> if (global == AdvantageType.ADVANTAGE) return AdvantageType.NONE else global
            AdvantagePreference.IGNORE_DISADVANTAGE -> if (global == AdvantageType.DISADVANTAGE) return AdvantageType.NONE else global
            else -> global
        }
        
        // Cancellation logic: if global and preference (ALWAYS) are opposite, they cancel.
        if (global == AdvantageType.ADVANTAGE && preference == AdvantagePreference.ALWAYS_DISADVANTAGE) return AdvantageType.NONE
        if (global == AdvantageType.DISADVANTAGE && preference == AdvantagePreference.ALWAYS_ADVANTAGE) return AdvantageType.NONE
        
        return prefType
    }

    private fun applyOp(current: Int, value: Int, op: BonusOperation): Int {
        return when (op) {
            BonusOperation.ADD -> current + value
            BonusOperation.SUBTRACT -> current - value
            BonusOperation.OVERRIDE -> value
        }
    }

    private fun rollTotal(
        title: String, baseModifier: Int, bonuses: List<IBonus>, isDamage: Boolean,
        exhaustion: Int, sourceType: RollSourceType, advantageType: AdvantageType,
        stats: Map<String, String>
    ): RollResult {
        val rollHasAdvantage = (advantageType == AdvantageType.ADVANTAGE || advantageType == AdvantageType.DISADVANTAGE) || 
                               bonuses.any { 
                                   val res = resolveAdvantage(advantageType, it.advantagePreference)
                                   res == AdvantageType.ADVANTAGE || res == AdvantageType.DISADVANTAGE 
                               }
        
        data class ComponentRoll(
            val sum1: Int, val dice1: List<DieRoll>, val sum2: Int, val dice2: List<DieRoll>,
            val d20Main: Int? = null, val d20Alt: Int? = null, val isOverride: Boolean = false,
            val flat1: Int = 0, val flat2: Int = 0
        )

        val components = mutableListOf<ComponentRoll>()

        if (!isDamage) {
            val r1 = Random.nextInt(1, 21)
            val r2 = if (advantageType != AdvantageType.NONE) Random.nextInt(1, 21) else r1
            val baseModSum = baseModifier + (if (exhaustion > 0) -2 * exhaustion else 0)
            components.add(ComponentRoll(r1 + baseModSum, emptyList(), r2 + baseModSum, emptyList(), r1, r2, flat1 = baseModSum, flat2 = baseModSum))
        } else {
            components.add(ComponentRoll(baseModifier, emptyList(), baseModifier, emptyList(), flat1 = baseModifier, flat2 = baseModifier))
        }

        bonuses.forEach { bonus ->
            val (fFlat, fDice) = parseFormulaParts(bonus.formula, stats)
            val bonusAdv = resolveAdvantage(advantageType, bonus.advantagePreference)
            
            var bSum1 = fFlat; var bSum2 = fFlat
            val bDice1 = mutableListOf<DieRoll>(); val bDice2 = mutableListOf<DieRoll>()

            fDice.forEach { ds ->
                val count = if (isDamage && advantageType == AdvantageType.CRITICAL) kotlin.math.abs(ds.count) * 2 else kotlin.math.abs(ds.count)
                repeat(count) {
                    val sign = if (ds.count > 0) 1 else -1
                    val r1 = Random.nextInt(1, ds.sides + 1)
                    val r2 = if (bonusAdv == AdvantageType.ADVANTAGE || bonusAdv == AdvantageType.DISADVANTAGE) Random.nextInt(1, ds.sides + 1) else r1
                    
                    val val1 = r1 * sign
                    val val2 = r2 * sign
                    
                    bSum1 += val1; bSum2 += val2
                    
                    val disp1 = if (bonus.operation == BonusOperation.SUBTRACT) -val1 else val1
                    val disp2 = if (bonus.operation == BonusOperation.SUBTRACT) -val2 else val2
                    
                    bDice1.add(DieRoll(disp1, ds.sides))
                    bDice2.add(DieRoll(disp2, ds.sides))
                }
            }
            components.add(ComponentRoll(
                bSum1, bDice1, bSum2, bDice2, 
                isOverride = bonus.operation == BonusOperation.OVERRIDE,
                flat1 = if (bonus.operation == BonusOperation.SUBTRACT) -fFlat else fFlat,
                flat2 = if (bonus.operation == BonusOperation.SUBTRACT) -fFlat else fFlat
            ))
        }

        fun assemble(variant: Int): Triple<Int, List<DieRoll>, Pair<List<Int>, Int?>> {
            var sum = 0
            val allDice = mutableListOf<DieRoll>()
            val flats = mutableListOf<Int>()
            var d20Res: Int? = null

            components.forEach { comp ->
                val cSum = if (variant == 1) comp.sum1 else comp.sum2
                val cDice = if (variant == 1) comp.dice1 else comp.dice2
                val cd20 = if (variant == 1) comp.d20Main else comp.d20Alt
                val cFlat = if (variant == 1) comp.flat1 else comp.flat2

                if (comp.isOverride) {
                    sum = cSum
                    allDice.clear(); flats.clear(); d20Res = null
                } else {
                    sum += cSum
                }
                allDice.addAll(cDice)
                if (cFlat != 0) flats.add(cFlat)
                if (cd20 != null) d20Res = cd20
            }
            
            return Triple(sum, allDice, flats to d20Res)
        }

        val res1 = assemble(1)
        if (!rollHasAdvantage) {
            return RollResult(
                title = title, total = res1.first, breakdown = "",
                isCriticalSuccess = !isDamage && res1.third.second == 20,
                isCriticalFailure = !isDamage && res1.third.second == 1,
                isDamage = isDamage, mainD20 = res1.third.second,
                bonusDice = res1.second, flatBonuses = res1.third.first,
                sourceType = sourceType, advantageType = advantageType
            )
        }

        val res2 = assemble(2)
        val useFirst = when (advantageType) {
            AdvantageType.ADVANTAGE -> res1.first >= res2.first
            AdvantageType.DISADVANTAGE -> res1.first <= res2.first
            else -> res1.first >= res2.first
        }

        val (main, alt) = if (useFirst) res1 to res2 else res2 to res1
        return RollResult(
            title = title, total = main.first, breakdown = "",
            isCriticalSuccess = !isDamage && main.third.second == 20,
            isCriticalFailure = !isDamage && main.third.second == 1,
            isDamage = isDamage, mainD20 = main.third.second, alternativeD20 = alt.third.second,
            bonusDice = main.second, flatBonuses = main.third.first,
            alternativeDice = alt.second, alternativeFlatBonuses = alt.third.first,
            sourceType = sourceType, advantageType = advantageType, unusedTotal = alt.first
        )
    }

    private fun rollIndividual(
        title: String, baseModifier: Int, bonuses: List<IBonus>, isDamage: Boolean,
        exhaustion: Int, sourceType: RollSourceType, advantageType: AdvantageType,
        attrModifiers: Map<Attribute, Int>, pb: Int, stats: Map<String, String>
    ): RollResult {
        val bonusDiceValues = mutableListOf<DieRoll>()
        val altBonusDiceValues = mutableListOf<DieRoll>()
        val flatBonusValues = mutableListOf<Int>()
        val altFlatBonusValues = mutableListOf<Int>()
        
        var total = 0
        var total2 = 0
        var mainD20: Int? = null
        var altD20: Int? = null

        if (!isDamage) {
            val d20_1 = Random.nextInt(1, 21)
            val d20_2 = Random.nextInt(1, 21)
            val (chosen, discarded) = when (advantageType) {
                AdvantageType.ADVANTAGE -> if (d20_1 >= d20_2) d20_1 to d20_2 else d20_2 to d20_1
                AdvantageType.DISADVANTAGE -> if (d20_1 <= d20_2) d20_1 to d20_2 else d20_2 to d20_1
                else -> d20_1 to d20_1
            }
            total = chosen; total2 = discarded
            mainD20 = chosen; altD20 = if (advantageType == AdvantageType.ADVANTAGE || advantageType == AdvantageType.DISADVANTAGE) discarded else null
            if (baseModifier != 0) { total += baseModifier; total2 += baseModifier; flatBonusValues.add(baseModifier); altFlatBonusValues.add(baseModifier) }
            if (exhaustion > 0) { val penalty = -2 * exhaustion; total += penalty; total2 += penalty; flatBonusValues.add(penalty); altFlatBonusValues.add(penalty) }
        } else {
            total = baseModifier; total2 = baseModifier; flatBonusValues.add(baseModifier); altFlatBonusValues.add(baseModifier)
        }
        
        bonuses.forEach { bonus ->
            val (fFlat, fDice) = parseFormulaParts(bonus.formula, stats)
            val adv = resolveAdvantage(advantageType, bonus.advantagePreference)
            var bonusSum1 = 0
            var bonusSum2 = 0
            
            val currentDice1 = mutableListOf<DieRoll>()
            val currentDice2 = mutableListOf<DieRoll>()
            
            fDice.forEach { diceSpec ->
                val rollCount = if (isDamage && advantageType == AdvantageType.CRITICAL) kotlin.math.abs(diceSpec.count) * 2 else kotlin.math.abs(diceSpec.count)
                repeat(rollCount) {
                    val sign = if (diceSpec.count > 0) 1 else -1
                    val r1 = Random.nextInt(1, diceSpec.sides + 1)
                    val r2 = Random.nextInt(1, diceSpec.sides + 1)
                    val (chosen, discarded) = when (adv) {
                        AdvantageType.ADVANTAGE -> if (r1 >= r2) r1 to r2 else r2 to r1
                        AdvantageType.DISADVANTAGE -> if (r1 <= r2) r1 to r2 else r2 to r1
                        else -> r1 to r1
                    }
                    val val1 = chosen * sign
                    val val2 = discarded * sign
                    bonusSum1 += val1
                    bonusSum2 += val2
                    
                    val dispVal1 = if (bonus.operation == BonusOperation.SUBTRACT) -val1 else val1
                    val dispVal2 = if (bonus.operation == BonusOperation.SUBTRACT) -val2 else val2
                    
                    val isComp = adv == AdvantageType.ADVANTAGE || adv == AdvantageType.DISADVANTAGE
                    currentDice1.add(DieRoll(dispVal1, diceSpec.sides, discardedValue = if (isComp) dispVal2 else null))
                    if (isComp || (advantageType == AdvantageType.ADVANTAGE || advantageType == AdvantageType.DISADVANTAGE)) {
                         currentDice2.add(DieRoll(dispVal2, diceSpec.sides))
                    }
                }
            }
            
            when (bonus.operation) {
                BonusOperation.ADD -> {
                    total += (bonusSum1 + fFlat)
                    total2 += (bonusSum2 + fFlat)
                    bonusDiceValues.addAll(currentDice1)
                    altBonusDiceValues.addAll(currentDice2)
                    if (fFlat != 0) { flatBonusValues.add(fFlat); altFlatBonusValues.add(fFlat) }
                }
                BonusOperation.SUBTRACT -> {
                    total -= (bonusSum1 + fFlat)
                    total2 -= (bonusSum2 + fFlat)
                    bonusDiceValues.addAll(currentDice1)
                    altBonusDiceValues.addAll(currentDice2)
                    if (fFlat != 0) { flatBonusValues.add(-fFlat); altFlatBonusValues.add(-fFlat) }
                }
                BonusOperation.OVERRIDE -> {
                    total = bonusSum1 + fFlat
                    total2 = bonusSum2 + fFlat
                    bonusDiceValues.clear()
                    altBonusDiceValues.clear()
                    flatBonusValues.clear()
                    altFlatBonusValues.clear()
                    mainD20 = null
                    altD20 = null
                    bonusDiceValues.addAll(currentDice1)
                    altBonusDiceValues.addAll(currentDice2)
                    if (fFlat != 0) { flatBonusValues.add(fFlat); altFlatBonusValues.add(fFlat) }
                }
            }
        }
        
        val hasAlt = (advantageType == AdvantageType.ADVANTAGE || advantageType == AdvantageType.DISADVANTAGE) || 
                     bonuses.any { 
                         val res = resolveAdvantage(advantageType, it.advantagePreference)
                         res == AdvantageType.ADVANTAGE || res == AdvantageType.DISADVANTAGE 
                     }

        return RollResult(
            title = title, total = total, breakdown = "",
            isCriticalSuccess = !isDamage && mainD20 == 20, isCriticalFailure = !isDamage && mainD20 == 1,
            isDamage = isDamage, mainD20 = mainD20, alternativeD20 = altD20,
            bonusDice = bonusDiceValues, flatBonuses = flatBonusValues,
            alternativeDice = if (hasAlt) altBonusDiceValues else null, 
            alternativeFlatBonuses = if (hasAlt) altFlatBonusValues else null,
            sourceType = sourceType, advantageType = advantageType, unusedTotal = if (hasAlt) total2 else null
        )
    }

    private fun rollSource(
        title: String, baseModifier: Int, bonuses: List<IBonus>, isDamage: Boolean,
        exhaustion: Int, sourceType: RollSourceType, advantageType: AdvantageType,
        attrModifiers: Map<Attribute, Int>, pb: Int, stats: Map<String, String>
    ): RollResult {
        val bonusDiceValues = mutableListOf<DieRoll>()
        val altBonusDiceValues = mutableListOf<DieRoll>()
        val flatBonusValues = mutableListOf<Int>()
        val altFlatBonusValues = mutableListOf<Int>()
        
        var total = 0
        var total2 = 0
        var mainD20: Int? = null
        var altD20: Int? = null

        if (!isDamage) {
            val d20_1 = Random.nextInt(1, 21)
            val d20_2 = Random.nextInt(1, 21)
            val s1 = d20_1 + baseModifier + (if (exhaustion > 0) -2 * exhaustion else 0)
            val s2 = d20_2 + baseModifier + (if (exhaustion > 0) -2 * exhaustion else 0)
            
            val (m, a) = when (advantageType) {
                AdvantageType.ADVANTAGE -> if (s1 >= s2) (d20_1 to s1) to (d20_2 to s2) else (d20_2 to s2) to (d20_1 to s1)
                AdvantageType.DISADVANTAGE -> if (s1 <= s2) (d20_1 to s1) to (d20_2 to s2) else (d20_2 to s2) to (d20_1 to s1)
                else -> (d20_1 to s1) to (d20_1 to s1)
            }
            total = m.second; total2 = a.second; mainD20 = m.first
            altD20 = if (advantageType == AdvantageType.ADVANTAGE || advantageType == AdvantageType.DISADVANTAGE) a.first else null
            if (baseModifier != 0) { flatBonusValues.add(baseModifier); altFlatBonusValues.add(baseModifier) }
            if (exhaustion > 0) { val p = -2 * exhaustion; flatBonusValues.add(p); altFlatBonusValues.add(p) }
        } else {
            total = baseModifier; total2 = baseModifier
            flatBonusValues.add(baseModifier); altFlatBonusValues.add(baseModifier)
        }

        bonuses.forEach { bonus ->
            val (fFlat, fDice) = parseFormulaParts(bonus.formula, stats)
            val adv = resolveAdvantage(advantageType, bonus.advantagePreference)
            
            val rollBonusSet = { currentAdv: AdvantageType ->
                val dice = mutableListOf<DieRoll>()
                var sum = fFlat
                fDice.forEach { ds ->
                    val rollCount = if (isDamage && advantageType == AdvantageType.CRITICAL) kotlin.math.abs(ds.count) * 2 else kotlin.math.abs(ds.count)
                    repeat(rollCount) {
                        val sign = if (ds.count > 0) 1 else -1
                        val (r, discarded) = if (currentAdv == AdvantageType.ADVANTAGE) {
                            val r1 = Random.nextInt(1, ds.sides + 1); val r2 = Random.nextInt(1, ds.sides + 1)
                            if (r1 >= r2) r1 to r2 else r2 to r1
                        } else if (currentAdv == AdvantageType.DISADVANTAGE) {
                            val r1 = Random.nextInt(1, ds.sides + 1); val r2 = Random.nextInt(1, ds.sides + 1)
                            if (r1 <= r2) r1 to r2 else r2 to r1
                        } else {
                            Random.nextInt(1, ds.sides + 1) to null
                        }
                        val valWithSign = r * sign
                        sum += valWithSign
                        dice.add(DieRoll(
                            value = if (bonus.operation == BonusOperation.SUBTRACT) -valWithSign else valWithSign, 
                            sides = ds.sides,
                            discardedValue = discarded?.let { if (bonus.operation == BonusOperation.SUBTRACT) -it * sign else it * sign }
                        ))
                    }
                }
                sum to dice
            }

            val r1 = rollBonusSet(adv)
            val hasBonusAlt = adv == AdvantageType.ADVANTAGE || adv == AdvantageType.DISADVANTAGE
            
            if (hasBonusAlt || (advantageType == AdvantageType.ADVANTAGE || advantageType == AdvantageType.DISADVANTAGE)) {
                val r2 = if (hasBonusAlt) rollBonusSet(adv) else r1
                
                when (bonus.operation) {
                    BonusOperation.ADD -> {
                        total += r1.first; total2 += r2.first
                        bonusDiceValues.addAll(r1.second); altBonusDiceValues.addAll(r2.second)
                        if (fFlat != 0) { flatBonusValues.add(fFlat); altFlatBonusValues.add(fFlat) }
                    }
                    BonusOperation.SUBTRACT -> {
                        total -= r1.first; total2 -= r2.first
                        bonusDiceValues.addAll(r1.second); altBonusDiceValues.addAll(r2.second)
                        if (fFlat != 0) { flatBonusValues.add(-fFlat); altFlatBonusValues.add(-fFlat) }
                    }
                    BonusOperation.OVERRIDE -> {
                        total = r1.first; total2 = r2.first
                        bonusDiceValues.clear(); altBonusDiceValues.clear()
                        flatBonusValues.clear(); altFlatBonusValues.clear()
                        mainD20 = null; altD20 = null
                        bonusDiceValues.addAll(r1.second); altBonusDiceValues.addAll(r2.second)
                        if (fFlat != 0) { flatBonusValues.add(fFlat); altFlatBonusValues.add(fFlat) }
                    }
                }
            } else {
                when (bonus.operation) {
                    BonusOperation.ADD -> {
                        total += r1.first; total2 += r1.first
                        bonusDiceValues.addAll(r1.second); altBonusDiceValues.addAll(r1.second)
                        if (fFlat != 0) { flatBonusValues.add(fFlat); altFlatBonusValues.add(fFlat) }
                    }
                    BonusOperation.SUBTRACT -> {
                        total -= r1.first; total2 -= r1.first
                        bonusDiceValues.addAll(r1.second); altBonusDiceValues.addAll(r1.second)
                        if (fFlat != 0) { flatBonusValues.add(-fFlat); altFlatBonusValues.add(-fFlat) }
                    }
                    BonusOperation.OVERRIDE -> {
                        total = r1.first; total2 = r1.first
                        bonusDiceValues.clear(); altBonusDiceValues.clear()
                        flatBonusValues.clear(); altFlatBonusValues.clear()
                        mainD20 = null; altD20 = null
                        bonusDiceValues.addAll(r1.second); altBonusDiceValues.addAll(r1.second)
                        if (fFlat != 0) { flatBonusValues.add(fFlat); altFlatBonusValues.add(fFlat) }
                    }
                }
            }
        }

        val hasAlt = (advantageType == AdvantageType.ADVANTAGE || advantageType == AdvantageType.DISADVANTAGE) || 
                     bonuses.any { 
                         val res = resolveAdvantage(advantageType, it.advantagePreference)
                         res == AdvantageType.ADVANTAGE || res == AdvantageType.DISADVANTAGE 
                     }

        return RollResult(
            title = title, total = total, breakdown = "",
            isCriticalSuccess = !isDamage && mainD20 == 20, isCriticalFailure = !isDamage && mainD20 == 1,
            isDamage = isDamage, mainD20 = mainD20, alternativeD20 = altD20,
            bonusDice = bonusDiceValues, flatBonuses = flatBonusValues,
            alternativeDice = if (hasAlt) altBonusDiceValues else null,
            alternativeFlatBonuses = if (hasAlt) altFlatBonusValues else null,
            sourceType = sourceType, advantageType = advantageType, unusedTotal = if (hasAlt) total2 else null
        )
    }

    private fun rollPool(
        title: String, baseModifier: Int, bonuses: List<IBonus>, isDamage: Boolean,
        exhaustion: Int, sourceType: RollSourceType, advantageType: AdvantageType,
        attrModifiers: Map<Attribute, Int>, pb: Int, stats: Map<String, String>
    ): RollResult {
        val bonusDiceValues = mutableListOf<DieRoll>()
        val altBonusDiceValues = mutableListOf<DieRoll>()
        val flatBonusValues = mutableListOf<Int>()
        val altFlatBonusValues = mutableListOf<Int>()
        
        var total = 0
        var total2 = 0
        var mainD20: Int? = null
        var altD20: Int? = null

        if (!isDamage) {
            val d20_1 = Random.nextInt(1, 21)
            val d20_2 = Random.nextInt(1, 21)
            val (chosen, discarded) = when (advantageType) {
                AdvantageType.ADVANTAGE -> if (d20_1 >= d20_2) d20_1 to d20_2 else d20_2 to d20_1
                AdvantageType.DISADVANTAGE -> if (d20_1 <= d20_2) d20_1 to d20_2 else d20_2 to d20_1
                else -> d20_1 to d20_1
            }
            total = chosen; total2 = discarded; mainD20 = chosen
            altD20 = if (advantageType == AdvantageType.ADVANTAGE || advantageType == AdvantageType.DISADVANTAGE) discarded else null
            if (baseModifier != 0) { total += baseModifier; total2 += baseModifier; flatBonusValues.add(baseModifier); altFlatBonusValues.add(baseModifier) }
            if (exhaustion > 0) { val p = -2 * exhaustion; total += p; total2 += p; flatBonusValues.add(p); altFlatBonusValues.add(p) }
        } else {
            total = baseModifier; total2 = baseModifier; flatBonusValues.add(baseModifier); altFlatBonusValues.add(baseModifier)
        }

        bonuses.forEach { bonus ->
            val (fFlat, fDice) = parseFormulaParts(bonus.formula, stats)
            val adv = resolveAdvantage(advantageType, bonus.advantagePreference)
            var bonusSum1 = 0
            var bonusSum2 = 0
            
            val currentDice1 = mutableListOf<DieRoll>()
            val currentDice2 = mutableListOf<DieRoll>()
            
            fDice.forEach { ds ->
                val count = if (isDamage && advantageType == AdvantageType.CRITICAL) kotlin.math.abs(ds.count) * 2 else kotlin.math.abs(ds.count)
                val sign = if (ds.count > 0) 1 else -1
                
                if (adv == AdvantageType.ADVANTAGE || adv == AdvantageType.DISADVANTAGE) {
                    val rolls = List(count * 2) { Random.nextInt(1, ds.sides + 1) * sign }.sortedBy { it * sign }
                    val (chosen, discarded) = if (adv == AdvantageType.ADVANTAGE) {
                        rolls.takeLast(count) to rolls.take(count)
                    } else {
                        rolls.take(count) to rolls.takeLast(count)
                    }
                    
                    for (i in chosen.indices) {
                        val v = chosen[i]
                        val d = discarded[i]
                        bonusSum1 += v
                        bonusSum2 += d
                        val dispV = if (bonus.operation == BonusOperation.SUBTRACT) -v else v
                        val dispD = if (bonus.operation == BonusOperation.SUBTRACT) -d else d
                        currentDice1.add(DieRoll(dispV, ds.sides, discardedValue = dispD))
                        currentDice2.add(DieRoll(dispD, ds.sides))
                    }
                } else {
                    repeat(count) {
                        val r = Random.nextInt(1, ds.sides + 1) * sign
                        bonusSum1 += r; bonusSum2 += r
                        val dispR = if (bonus.operation == BonusOperation.SUBTRACT) -r else r
                        currentDice1.add(DieRoll(dispR, ds.sides))
                        currentDice2.add(DieRoll(dispR, ds.sides))
                    }
                }
            }
            
            when (bonus.operation) {
                BonusOperation.ADD -> {
                    total += (bonusSum1 + fFlat); total2 += (bonusSum2 + fFlat)
                    bonusDiceValues.addAll(currentDice1); altBonusDiceValues.addAll(currentDice2)
                    if (fFlat != 0) { flatBonusValues.add(fFlat); altFlatBonusValues.add(fFlat) }
                }
                BonusOperation.SUBTRACT -> {
                    total -= (bonusSum1 + fFlat); total2 -= (bonusSum2 + fFlat)
                    bonusDiceValues.addAll(currentDice1); altBonusDiceValues.addAll(currentDice2)
                    if (fFlat != 0) { flatBonusValues.add(-fFlat); altFlatBonusValues.add(-fFlat) }
                }
                BonusOperation.OVERRIDE -> {
                    total = bonusSum1 + fFlat; total2 = bonusSum2 + fFlat
                    bonusDiceValues.clear(); altBonusDiceValues.clear()
                    flatBonusValues.clear(); altFlatBonusValues.clear()
                    mainD20 = null; altD20 = null
                    bonusDiceValues.addAll(currentDice1); altBonusDiceValues.addAll(currentDice2)
                    if (fFlat != 0) { flatBonusValues.add(fFlat); altFlatBonusValues.add(fFlat) }
                }
            }
        }

        val hasAlt = (advantageType == AdvantageType.ADVANTAGE || advantageType == AdvantageType.DISADVANTAGE) || 
                     bonuses.any { 
                         val res = resolveAdvantage(advantageType, it.advantagePreference)
                         res == AdvantageType.ADVANTAGE || res == AdvantageType.DISADVANTAGE 
                     }

        return RollResult(
            title = title, total = total, breakdown = "",
            isCriticalSuccess = !isDamage && mainD20 == 20, isCriticalFailure = !isDamage && mainD20 == 1,
            isDamage = isDamage, mainD20 = mainD20, alternativeD20 = altD20,
            bonusDice = bonusDiceValues, flatBonuses = flatBonusValues,
            alternativeDice = if (hasAlt) altBonusDiceValues else null, 
            alternativeFlatBonuses = if (hasAlt) altFlatBonusValues else null,
            sourceType = sourceType, advantageType = advantageType, unusedTotal = if (hasAlt) total2 else null
        )
    }

    fun performHapticFeedback(context: Context, result: RollResult) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return
        if (!vibrator.hasVibrator()) return
        val shortWait = 60; val longWait = 200
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_CLICK)) {
            val comp = VibrationEffect.startComposition()
            when {
                result.isCriticalSuccess && !result.isDamage -> {
                    comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, shortWait)
                    comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, longWait)
                    comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, shortWait)
                    comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, shortWait)
                }
                result.isCriticalFailure && !result.isDamage -> {
                    comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, shortWait)
                    comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, shortWait)
                    comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, longWait)
                    comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, longWait)
                    comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, shortWait)
                    comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, shortWait)
                    comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, longWait)
                }
                else -> comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 0)
            }
            vibrator.vibrate(comp.compose()); return
        }
        val legacyKick = 20L; val legacyShort = 40L; val legacyLong = 150L
        val pattern = when {
            result.isCriticalSuccess && !result.isDamage -> longArrayOf(legacyShort, legacyKick, legacyLong, legacyKick, legacyShort, legacyKick, legacyShort, legacyKick)
            result.isCriticalFailure && !result.isDamage -> longArrayOf(legacyShort, legacyKick, legacyShort, legacyKick, legacyLong, legacyKick, legacyLong, legacyKick, legacyShort, legacyKick, legacyShort, legacyKick, legacyLong, legacyKick)
            else -> longArrayOf(0, legacyKick)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        else @Suppress("DEPRECATION") vibrator.vibrate(pattern, -1)
    }

    fun getProficiencyBonus(level: String): Int {
        return ru.quasaris.characters.master.backend.getProficiencyBonus(level)
    }

    fun rollPool(
        pool: Map<Int, Int>,
        title: String = "Бросок кубов"
    ): RollResult {
        val allDice = mutableListOf<DieRoll>()
        var total = 0
        
        pool.forEach { (sides, count) ->
            repeat(count) {
                val r = kotlin.random.Random.nextInt(1, sides + 1)
                total += r
                allDice.add(DieRoll(r, sides))
            }
        }
        
        return RollResult(
            title = title,
            total = total,
            breakdown = pool.entries.joinToString(" + ") { "${it.value}d${it.key}" },
            bonusDice = allDice,
            sourceType = RollSourceType.OTHER
        )
    }
}
