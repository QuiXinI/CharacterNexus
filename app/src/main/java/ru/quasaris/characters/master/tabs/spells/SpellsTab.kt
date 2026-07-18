package ru.quasaris.characters.master.tabs.spells

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.quasaris.characters.master.Attribute
import ru.quasaris.characters.master.DynamicNoteState
import ru.quasaris.characters.master.SpellSettings
import ru.quasaris.characters.master.backend.SettingsViewModel
import ru.quasaris.characters.master.backend.SpellSlotCalculator
import ru.quasaris.characters.master.backend.calculateModifier
import ru.quasaris.characters.master.backend.getProficiencyBonus
import ru.quasaris.characters.master.backend.RollResult
import ru.quasaris.characters.master.backend.DiceRoller
import ru.quasaris.characters.master.backend.RollSourceType
import ru.quasaris.characters.master.tabs.DynamicFieldsTab
import ru.quasaris.characters.master.tabs.attacks.DiceIcon
import ru.quasaris.characters.master.tabs.attacks.DicePart
import ru.quasaris.characters.master.tabs.attacks.parseFormulaParts
import ru.quasaris.characters.master.tabs.attacks.AttackBonusIndicator
import dev.chrisbanes.haze.HazeState

@Composable
fun SpellsTab(
    spells: List<DynamicNoteState>,
    onSpellsChange: (List<DynamicNoteState>) -> Unit,
    characterLevel: Int = 1,
    spellSettings: SpellSettings = SpellSettings(),
    onSpellSettingsChange: (SpellSettings) -> Unit = {},
    hazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false,
    isEditMode: Boolean = false,
    settingsViewModel: SettingsViewModel? = null,
    onRoll: (RollResult) -> Unit = {},
    statsMap: Map<String, String> = emptyMap(),
    exhaustion: Int = 0
) {
    val autoSlots = remember(spellSettings.casterType, spellSettings.isMulticlass, spellSettings.fullCasterLevel, spellSettings.halfCasterLevel, spellSettings.thirdCasterLevel, characterLevel) {
        if (spellSettings.isMulticlass) {
            SpellSlotCalculator.getMulticlassSlots(spellSettings.fullCasterLevel, spellSettings.halfCasterLevel, spellSettings.thirdCasterLevel)
        } else {
            SpellSlotCalculator.getSlotsForLevel(spellSettings.casterType, characterLevel)
        }
    }

    val pb = getProficiencyBonus(characterLevel.toString())
    val abilityModifier = if (spellSettings.spellcastingAbility != Attribute.NONE) {
        val statKey = when (spellSettings.spellcastingAbility) {
            Attribute.STRENGTH -> "strength"
            Attribute.DEXTERITY -> "dexterity"
            Attribute.CONSTITUTION -> "constitution"
            Attribute.INTELLIGENCE -> "intelligence"
            Attribute.WISDOM -> "wisdom"
            Attribute.CHARISMA -> "charisma"
            else -> ""
        }
        ru.quasaris.characters.master.backend.calculateModifier(statsMap[statKey] ?: "10")
    } else 0

    val magicAtkCalculation = remember(spellSettings.spellAttackBonuses, pb, abilityModifier, statsMap, exhaustion) {
        var totalFlat = pb + abilityModifier - (exhaustion * 2)
        val allDice = mutableMapOf<Int, Int>()
        spellSettings.spellAttackBonuses.forEach { bonus ->
            val (fFlat, fDice) = parseFormulaParts(bonus.formula, stats = statsMap, proficiencyBonus = pb)
            totalFlat += fFlat
            fDice.forEach { allDice[it.sides] = (allDice[it.sides] ?: 0) + it.count }
        }
        Pair(totalFlat, allDice.map { DicePart(it.value, it.key) }.sortedBy { it.sides })
    }

    val magicSaveCalculation = remember(spellSettings.spellSaveDcBonuses, pb, abilityModifier, statsMap) {
        var totalFlat = 8 + pb + abilityModifier
        val allDice = mutableMapOf<Int, Int>()
        spellSettings.spellSaveDcBonuses.forEach { bonus ->
            val (fFlat, fDice) = parseFormulaParts(bonus.formula, stats = statsMap, proficiencyBonus = pb)
            totalFlat += fFlat
            fDice.forEach { allDice[it.sides] = (allDice[it.sides] ?: 0) + it.count }
        }
        Pair(totalFlat, allDice.map { DicePart(it.value, it.key) }.sortedBy { it.sides })
    }

    val spellAttackBonus = magicAtkCalculation.first
    val spellAttackDice = magicAtkCalculation.second
    val spellSaveDc = magicSaveCalculation.first
    val spellSaveDice = magicSaveCalculation.second

    val processedSpells = remember(spells, spellSettings.specialSlots) {
        val specialLevels = spellSettings.specialSlots.map { it.level }.filter { it > 9 }.distinct()
        val missingLevels = specialLevels.filter { level ->
            val title = "$level уровень"
            spells.none { it.title.equals(title, ignoreCase = true) }
        }
        
        if (missingLevels.isEmpty()) {
            spells
        } else {
            spells.toMutableList().apply {
                missingLevels.forEach { level ->
                    add(DynamicNoteState(title = "$level уровень"))
                }
            }
        }
    }

    LaunchedEffect(processedSpells.size) {
        if (processedSpells.size != spells.size) {
            onSpellsChange(processedSpells)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (spellSettings.isMagicEnabled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Spell Save DC
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Сложность спасброска",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 10.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = spellSaveDc.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (spellSaveDice.isNotEmpty()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    spellSaveDice.forEach { DiceIcon(it) }
                                }
                            }
                        }
                    }
                }

                // Right: Spell Attack Bonus Roll Button
                Surface(
                    modifier = Modifier
                        .clickable { 
                            onRoll(DiceRoller.roll(
                                title = "Атака заклинанием", 
                                baseModifier = spellAttackBonus + (exhaustion * 2), 
                                bonusFormulas = spellSettings.spellAttackBonuses.map { it.formula },
                                stats = statsMap, 
                                exhaustion = exhaustion, 
                                sourceType = RollSourceType.ATTACK
                            )) 
                        },
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Атака заклинанием",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 10.sp
                        )
                        AttackBonusIndicator(
                            bonus = spellAttackBonus,
                            dice = spellAttackDice,
                            size = 42.dp,
                            fontSize = 16.sp,
                            showLabel = false,
                            showDice = true,
                            diceOnLeft = true
                        )
                    }
                }
            }
        }

        DynamicFieldsTab(
            fields = if (spellSettings.isMagicEnabled) processedSpells else emptyList(),
            onFieldsChange = onSpellsChange,
            hazeState = hazeState,
            forceBlurEnabled = forceBlurEnabled,
            isEditMode = isEditMode,
            emptyListText = if (spellSettings.isMagicEnabled) "Список заклинаний пуст" else "Магия нужна слабым",
            titlePlaceholder = "Заголовок",
            contentPlaceholder = "Список заклинаний...",
            settingsViewModel = settingsViewModel,
            statsMap = statsMap,
            isCollapsible = false,
            isTitleReadOnly = true,
            isAddButtonVisible = false,
            isReorderButtonVisible = false,
            extraContent = { spell ->
                val title = spell.title.lowercase()
                val levelMatch = Regex("(\\d+)").find(title)
                val level = levelMatch?.value?.toIntOrNull() ?: 0
                
                if (level > 0) {
                    val baseSlots = if (level in 1..9) (spellSettings.overrideSlots[level] ?: autoSlots[level - 1]) else (spellSettings.overrideSlots[level] ?: 0)
                    val specialLong = spellSettings.specialSlots.filter { it.level == level && !it.restoreOnShortRest }.sumOf { it.count }
                    val maxLong = baseSlots + specialLong

                    val pactSlots = if (spellSettings.isPactEnabled && spellSettings.pactSlotLevel == level) spellSettings.pactSlotsCount else 0
                    val specialShort = spellSettings.specialSlots.filter { it.level == level && it.restoreOnShortRest }.sumOf { it.count }
                    val maxShort = pactSlots + specialShort

                    if (maxLong > 0 || maxShort > 0) {
                        Row(
                            modifier = Modifier.padding(end = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (maxLong > 0) {
                                SpellSlotTracker(
                                    maxSlots = maxLong,
                                    usedSlots = spellSettings.usedSlots[level] ?: 0,
                                    isShortRest = false,
                                    onUsedSlotsChange = { newUsed ->
                                        onSpellSettingsChange(
                                            spellSettings.copy(
                                                usedSlots = spellSettings.usedSlots.toMutableMap().apply { put(level, newUsed) }
                                            )
                                        )
                                    }
                                )
                            }
                            
                            if (maxShort > 0) {
                                SpellSlotTracker(
                                    maxSlots = maxShort,
                                    usedSlots = spellSettings.usedSlotsShortRest[level] ?: 0,
                                    isShortRest = true,
                                    onUsedSlotsChange = { newUsed ->
                                        onSpellSettingsChange(
                                            spellSettings.copy(
                                                usedSlotsShortRest = spellSettings.usedSlotsShortRest.toMutableMap().apply { put(level, newUsed) }
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}
