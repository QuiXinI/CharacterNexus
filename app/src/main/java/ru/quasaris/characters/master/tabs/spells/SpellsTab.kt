package ru.quasaris.characters.master.tabs.spells

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.quasaris.characters.master.Attribute
import ru.quasaris.characters.master.DynamicNoteState
import ru.quasaris.characters.master.SpellSettings
import ru.quasaris.characters.master.SlotAlignment
import ru.quasaris.characters.master.SlotFillDirection
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
import java.util.Locale
import kotlin.math.floor

@OptIn(ExperimentalLayoutApi::class)
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
    var showAddLevelDialog by remember { mutableStateOf(false) }

    val autoSlots = remember(spellSettings.casterType, spellSettings.isMulticlass, spellSettings.fullCasterLevel, spellSettings.halfCasterLevel, spellSettings.thirdCasterLevel, characterLevel) {
        if (spellSettings.isMulticlass) {
            SpellSlotCalculator.getMulticlassSlots(spellSettings.fullCasterLevel, spellSettings.halfCasterLevel, spellSettings.thirdCasterLevel)
        } else {
            SpellSlotCalculator.getSlotsForLevel(spellSettings.casterType, characterLevel)
        }
    }

    fun parseLevelFromTitle(title: String): Float {
        val t = title.lowercase(Locale.getDefault())
        if (t.contains("заговор")) return 0f
        val match = Regex("([\\d.]+)").find(t)
        return match?.value?.toFloatOrNull() ?: 0f
    }

    fun formatLevelTitle(level: Float): String {
        if (level == 0f) return "Заговоры"
        val isInt = level == floor(level)
        val levelStr = if (isInt) level.toInt().toString() else level.toString()
        return "$levelStr уровень"
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

    val longRestAlignment by settingsViewModel?.longRestAlignment?.collectAsState() ?: remember { mutableStateOf(SlotAlignment.RIGHT) }
    val longRestFillDirection by settingsViewModel?.longRestFillDirection?.collectAsState() ?: remember { mutableStateOf(SlotFillDirection.LTR) }
    val shortRestAlignment by settingsViewModel?.shortRestAlignment?.collectAsState() ?: remember { mutableStateOf(SlotAlignment.RIGHT) }
    val shortRestFillDirection by settingsViewModel?.shortRestFillDirection?.collectAsState() ?: remember { mutableStateOf(SlotFillDirection.LTR) }

    val processedSpells = remember(spells, spellSettings.specialSlots, spellSettings.pactSlotLevel, spellSettings.isPactEnabled, spellSettings.casterType, spellSettings.isMulticlass) {
        val specialLevels = spellSettings.specialSlots.map { it.level }.toMutableSet()
        if (spellSettings.isPactEnabled) specialLevels.add(spellSettings.pactSlotLevel)
        
        // Ensure 1-9 are there if caster
        if (spellSettings.casterType != ru.quasaris.characters.master.CasterType.NONE || spellSettings.isMulticlass) {
            for (i in 1..9) specialLevels.add(i.toFloat())
        }
        specialLevels.add(0f) // Cantrips

        val currentList = spells.toMutableList()
        val existingLevels = currentList.map { parseLevelFromTitle(it.title) }
        
        val missingLevels = specialLevels.filter { it !in existingLevels }.sorted()
        
        missingLevels.forEach { level ->
            currentList.add(DynamicNoteState(
                title = formatLevelTitle(level),
                isExpanded = level <= 1f // Expand Cantrips and Level 1 by default
            ))
        }

        currentList.sortedBy { parseLevelFromTitle(it.title) }
    }

    LaunchedEffect(processedSpells) {
        if (processedSpells != spells) {
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

        Box(modifier = Modifier.weight(1f)) {
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
                isCollapsible = true,
                isTitleReadOnly = true,
                isAddButtonVisible = false,
                isReorderButtonVisible = false,
                footer = {
                    if (spellSettings.isMagicEnabled) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(
                                onClick = { showAddLevelDialog = true },
                                modifier = Modifier.height(48.dp),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("ДОБАВИТЬ УРОВЕНЬ")
                            }
                        }
                    }
                },
                extraContent = { spell ->
                    val level = parseLevelFromTitle(spell.title)
                    
                    if (level > 0) {
                        val baseSlots = if (level >= 1f && level <= 9f && level == floor(level)) (spellSettings.overrideSlots[level] ?: autoSlots[level.toInt() - 1]) else (spellSettings.overrideSlots[level] ?: 0)
                        val specialLong = spellSettings.specialSlots.filter { it.level == level && !it.restoreOnShortRest }.sumOf { it.count }
                        val maxLong = baseSlots + specialLong

                        val pactSlots = if (spellSettings.isPactEnabled && spellSettings.pactSlotLevel == level) spellSettings.pactSlotsCount else 0
                        val specialShort = spellSettings.specialSlots.filter { it.level == level && it.restoreOnShortRest }.sumOf { it.count }
                        val maxShort = pactSlots + specialShort

                        if (maxLong > 0 || maxShort > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Layout(
                                    content = {
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
                                                },
                                                alignment = longRestAlignment,
                                                fillDirection = longRestFillDirection
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
                                                },
                                                alignment = shortRestAlignment,
                                                fillDirection = shortRestFillDirection
                                            )
                                        }
                                    },
                                    measurePolicy = { measurables, constraints ->
                                        val longMeasurable = if (maxLong > 0) measurables[0] else null
                                        val shortMeasurable = if (maxShort > 0) {
                                            if (maxLong > 0) measurables[1] else measurables[0]
                                        } else null

                                        val longPlaceable = longMeasurable?.measure(constraints.copy(minWidth = 0))
                                        val shortPlaceable = shortMeasurable?.measure(constraints.copy(minWidth = 0))

                                        val spacing = 8.dp.roundToPx()
                                        val longWidth = longPlaceable?.width ?: 0
                                        val shortWidth = shortPlaceable?.width ?: 0
                                        val longHeight = longPlaceable?.height ?: 0
                                        val shortHeight = shortPlaceable?.height ?: 0

                                        val lXPref = when (longRestAlignment) {
                                            SlotAlignment.LEFT -> 0
                                            SlotAlignment.CENTER -> (constraints.maxWidth - longWidth) / 2
                                            SlotAlignment.RIGHT -> constraints.maxWidth - longWidth
                                        }
                                        val sXPref = when (shortRestAlignment) {
                                            SlotAlignment.LEFT -> 0
                                            SlotAlignment.CENTER -> (constraints.maxWidth - shortWidth) / 2
                                            SlotAlignment.RIGHT -> constraints.maxWidth - shortWidth
                                        }

                                        val overlaps = if (longWidth > 0 && shortWidth > 0) {
                                            if (lXPref < sXPref) {
                                                lXPref + longWidth + spacing > sXPref
                                            } else if (sXPref < lXPref) {
                                                sXPref + shortWidth + spacing > lXPref
                                            } else {
                                                true
                                            }
                                        } else false

                                        val canFitInOneLine = !overlaps && ((longWidth > 0 && shortWidth > 0 && longWidth + shortWidth + spacing <= constraints.maxWidth) || 
                                                             (longWidth == 0 || shortWidth == 0))

                                        val totalHeight = if (canFitInOneLine) {
                                            maxOf(longHeight, shortHeight)
                                        } else {
                                            longHeight + shortHeight + spacing
                                        }

                                        layout(constraints.maxWidth, totalHeight) {
                                            if (canFitInOneLine) {
                                                val longX = when (longRestAlignment) {
                                                    SlotAlignment.LEFT -> 0
                                                    SlotAlignment.CENTER -> (constraints.maxWidth - longWidth) / 2
                                                    SlotAlignment.RIGHT -> {
                                                        if (shortWidth > 0 && shortRestAlignment == SlotAlignment.RIGHT) {
                                                            constraints.maxWidth - longWidth - shortWidth - spacing
                                                        } else {
                                                            constraints.maxWidth - longWidth
                                                        }
                                                    }
                                                }

                                                val shortX = when (shortRestAlignment) {
                                                    SlotAlignment.LEFT -> {
                                                        if (longWidth > 0 && longRestAlignment == SlotAlignment.LEFT) {
                                                            longWidth + spacing
                                                        } else {
                                                            0
                                                        }
                                                    }
                                                    SlotAlignment.CENTER -> (constraints.maxWidth - shortWidth) / 2
                                                    SlotAlignment.RIGHT -> constraints.maxWidth - shortWidth
                                                }

                                                longPlaceable?.place(longX, 0)
                                                shortPlaceable?.place(shortX, 0)
                                            } else {
                                                val longX = when (longRestAlignment) {
                                                    SlotAlignment.LEFT -> 0
                                                    SlotAlignment.CENTER -> (constraints.maxWidth - longWidth) / 2
                                                    SlotAlignment.RIGHT -> constraints.maxWidth - longWidth
                                                }
                                                val shortX = when (shortRestAlignment) {
                                                    SlotAlignment.LEFT -> 0
                                                    SlotAlignment.CENTER -> (constraints.maxWidth - shortWidth) / 2
                                                    SlotAlignment.RIGHT -> constraints.maxWidth - shortWidth
                                                }
                                                
                                                longPlaceable?.place(longX, 0)
                                                shortPlaceable?.place(shortX, longHeight + spacing)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    }

    if (showAddLevelDialog) {
        var levelText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddLevelDialog = false },
            title = { Text("Добавить уровень заклинаний") },
            text = {
                OutlinedTextField(
                    value = levelText,
                    onValueChange = { levelText = it },
                    label = { Text("Уровень (число)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val level = levelText.toFloatOrNull()
                        if (level != null) {
                            val title = formatLevelTitle(level)
                            if (spells.none { it.title.equals(title, ignoreCase = true) }) {
                                onSpellsChange((spells + DynamicNoteState(title = title, isExpanded = false)).sortedBy { parseLevelFromTitle(it.title) })
                            }
                        }
                        showAddLevelDialog = false
                    }
                ) {
                    Text("Добавить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddLevelDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}
