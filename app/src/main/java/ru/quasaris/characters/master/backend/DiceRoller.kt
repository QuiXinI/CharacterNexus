package ru.quasaris.characters.master.backend

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import ru.quasaris.characters.master.Attribute
import ru.quasaris.characters.master.tabs.attacks.parseFormulaParts
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

object DiceRoller {

    fun roll(
        title: String,
        baseModifier: Int,
        bonusFormulas: List<String> = emptyList(),
        isDamage: Boolean = false,
        stats: Map<String, String> = emptyMap(),
        exhaustion: Int = 0,
        sourceType: RollSourceType = RollSourceType.OTHER,
        advantageType: AdvantageType = AdvantageType.NONE
    ): RollResult {
        val bonusDiceValues = mutableListOf<DieRoll>()
        val altBonusDiceValues = if (advantageType == AdvantageType.ADVANTAGE || advantageType == AdvantageType.DISADVANTAGE) mutableListOf<DieRoll>() else null
        
        val flatBonusValues = mutableListOf<Int>()
        val altFlatBonusValues = if (advantageType == AdvantageType.ADVANTAGE || advantageType == AdvantageType.DISADVANTAGE) mutableListOf<Int>() else null
        
        var total = 0
        var total2 = 0
        
        val attrModifiers = mutableMapOf<Attribute, Int>()
        Attribute.entries.forEach { attr ->
            if (attr != Attribute.NONE) {
                val score = stats[attr.name.lowercase()] ?: "10"
                attrModifiers[attr] = calculateModifier(score)
            }
        }
        val pb = (stats["proficiencyBonus"] ?: stats["level"]?.let { getProficiencyBonus(it).toString() } ?: "2").replace("+", "").toIntOrNull() ?: 2

        if (!isDamage) {
            val d20_1 = Random.nextInt(1, 21)
            val d20_2 = if (advantageType == AdvantageType.ADVANTAGE || advantageType == AdvantageType.DISADVANTAGE) Random.nextInt(1, 21) else d20_1
            
            val (mainD20, altD20) = when (advantageType) {
                AdvantageType.ADVANTAGE -> if (d20_1 >= d20_2) d20_1 to d20_2 else d20_2 to d20_1
                AdvantageType.DISADVANTAGE -> if (d20_1 <= d20_2) d20_1 to d20_2 else d20_2 to d20_1
                else -> d20_1 to null
            }

            total = mainD20
            total2 = altD20 ?: mainD20
            
            // Add base modifier
            if (baseModifier != 0) {
                total += baseModifier
                total2 += baseModifier
                flatBonusValues.add(baseModifier)
                altFlatBonusValues?.add(baseModifier)
            }
            
            // Add exhaustion penalty
            if (exhaustion > 0) {
                val penalty = -2 * exhaustion
                total += penalty
                total2 += penalty
                flatBonusValues.add(penalty)
                altFlatBonusValues?.add(penalty)
            }
            
            bonusFormulas.forEach { formula ->
                val (fFlat, fDice) = parseFormulaParts(formula, attrModifiers, pb)
                fDice.forEach { dice ->
                    repeat(kotlin.math.abs(dice.count)) {
                        val sign = if (dice.count > 0) 1 else -1
                        val r1 = Random.nextInt(1, dice.sides + 1)
                        val r2 = if (advantageType == AdvantageType.ADVANTAGE || advantageType == AdvantageType.DISADVANTAGE) Random.nextInt(1, dice.sides + 1) else r1
                        
                        val (chosen, discarded) = when (advantageType) {
                            AdvantageType.ADVANTAGE -> if (r1 >= r2) r1 to r2 else r2 to r1
                            AdvantageType.DISADVANTAGE -> if (r1 <= r2) r1 to r2 else r2 to r1
                            else -> r1 to null
                        }

                        total += chosen * sign
                        bonusDiceValues.add(DieRoll(chosen * sign, dice.sides))
                        
                        if (discarded != null) {
                            total2 += discarded * sign
                            altBonusDiceValues?.add(DieRoll(discarded * sign, dice.sides))
                        }
                    }
                }
                if (fFlat != 0) {
                    total += fFlat
                    total2 += fFlat
                    flatBonusValues.add(fFlat)
                    altFlatBonusValues?.add(fFlat)
                }
            }
            
            return RollResult(
                title = title,
                total = total,
                breakdown = "", // DiceRollOverlay uses structured lists now
                isCriticalSuccess = mainD20 == 20,
                isCriticalFailure = mainD20 == 1,
                isDamage = false,
                mainD20 = mainD20,
                alternativeD20 = altD20,
                bonusDice = bonusDiceValues,
                flatBonuses = flatBonusValues,
                alternativeDice = altBonusDiceValues,
                alternativeFlatBonuses = altFlatBonusValues,
                sourceType = sourceType,
                advantageType = advantageType,
                unusedTotal = if (altD20 != null) total2 else null
            )
        } else {
            total = baseModifier
            flatBonusValues.add(baseModifier)
            
            bonusFormulas.forEach { formula ->
                val (fFlat, fDice) = parseFormulaParts(formula, attrModifiers, pb)
                fDice.forEach { dice ->
                    // Double the dice count for critical damage
                    val rollCount = if (advantageType == AdvantageType.CRITICAL) kotlin.math.abs(dice.count) * 2 else kotlin.math.abs(dice.count)
                    
                    repeat(rollCount) {
                        val sign = if (dice.count > 0) 1 else -1
                        val r = Random.nextInt(1, dice.sides + 1)
                        total += r * sign
                        bonusDiceValues.add(DieRoll(r * sign, dice.sides))
                    }
                }
                if (fFlat != 0) {
                    total += fFlat
                    flatBonusValues.add(fFlat)
                }
            }
            
            return RollResult(
                title = title,
                total = total,
                breakdown = "",
                isDamage = true,
                bonusDice = bonusDiceValues,
                flatBonuses = flatBonusValues,
                sourceType = sourceType,
                advantageType = advantageType
            )
        }
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

        val shortWait = 60
        val longWait = 200

        // Используем современные "пинки" (Haptic Primitives) на Android 11+ (API 30)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_CLICK)) {
            val composition = VibrationEffect.startComposition()
            
            when {
                result.isCriticalSuccess && !result.isDamage -> {
                    // . _ . .
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, shortWait)
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, longWait)
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, shortWait)
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, shortWait)
                }
                result.isCriticalFailure && !result.isDamage -> {
                    // . . _ _ . . _
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, shortWait)
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, shortWait)
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, longWait)
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, longWait)
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, shortWait)
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, shortWait)
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, longWait)
                }
                else -> {
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 0)
                }
            }
            vibrator.vibrate(composition.compose())
            return
        }

        // Fallback для старых устройств или если примитивы не поддерживаются
        val legacyKick = 20L // Максимально короткий импульс для имитации клика
        val legacyShort = 40L
        val legacyLong = 150L

        val pattern = when {
            result.isCriticalSuccess && !result.isDamage -> {
                longArrayOf(legacyShort, legacyKick, legacyLong, legacyKick, legacyShort, legacyKick, legacyShort, legacyKick)
            }
            result.isCriticalFailure && !result.isDamage -> {
                longArrayOf(legacyShort, legacyKick, legacyShort, legacyKick, legacyLong, legacyKick, legacyLong, legacyKick, legacyShort, legacyKick, legacyShort, legacyKick, legacyLong, legacyKick)
            }
            else -> {
                longArrayOf(0, legacyKick)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }
}
