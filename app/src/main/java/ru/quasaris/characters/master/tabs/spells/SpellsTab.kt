package ru.quasaris.characters.master.tabs.spells

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
        calculateModifier(statsMap[statKey] ?: "10")
    } else 0

    val spellAttackBonus = pb + abilityModifier + (spellSettings.spellAttackBonus.toIntOrNull() ?: 0)
    val spellSaveDc = 8 + pb + abilityModifier + (spellSettings.spellSaveDcBonus.toIntOrNull() ?: 0)

    val processedSpells = remember(spells, spellSettings.specialSlots) {
        val currentSpells = spells.toMutableList()
        val specialLevels = spellSettings.specialSlots.map { it.level }.filter { it > 9 }.distinct()
        
        specialLevels.forEach { level ->
            val title = "$level уровень"
            if (currentSpells.none { it.title.equals(title, ignoreCase = true) }) {
                currentSpells.add(DynamicNoteState(title = title))
            }
        }

        currentSpells
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
                            text = "Спасбросок",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = spellSaveDc.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Right: Spell Attack Bonus Roll Button
                Button(
                    onClick = { onRoll(DiceRoller.roll("Атака заклинанием", spellAttackBonus, stats = statsMap, exhaustion = exhaustion, sourceType = RollSourceType.ATTACK)) },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.height(IntrinsicSize.Min)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "АТАКА МАГИЕЙ",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = if (spellAttackBonus >= 0) "+$spellAttackBonus" else spellAttackBonus.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
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
