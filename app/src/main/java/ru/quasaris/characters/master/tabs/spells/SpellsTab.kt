package ru.quasaris.characters.master.tabs.spells

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import ru.quasaris.characters.master.SpellCard
import ru.quasaris.characters.master.SpellMode
import ru.quasaris.characters.master.MagicAttackType
import ru.quasaris.characters.master.MaterialComponentType
import ru.quasaris.characters.master.backend.SpellbookManager
import ru.quasaris.characters.master.backend.SimpleBonus
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import ru.quasaris.characters.master.Attribute
import ru.quasaris.characters.master.DynamicNoteState
import ru.quasaris.characters.master.SpellSettings
import ru.quasaris.characters.master.SlotAlignment
import ru.quasaris.characters.master.SlotFillDirection
import ru.quasaris.characters.master.BonusOperation
import ru.quasaris.characters.master.backend.AdvantageType
import ru.quasaris.characters.master.backend.AdvantageLogic
import ru.quasaris.characters.master.backend.DiceRoller
import ru.quasaris.characters.master.backend.RollResult
import ru.quasaris.characters.master.backend.RollSourceType
import ru.quasaris.characters.master.backend.SettingsViewModel
import ru.quasaris.characters.master.backend.SpellSlotCalculator
import ru.quasaris.characters.master.backend.getProficiencyBonus
import ru.quasaris.characters.master.tabs.DynamicFieldsTab
import ru.quasaris.characters.master.tabs.attacks.AttackBonusIndicator
import ru.quasaris.characters.master.tabs.attacks.DiceIcon
import ru.quasaris.characters.master.backend.DicePart
import ru.quasaris.characters.master.backend.parseFormulaParts
import ru.quasaris.characters.master.tabs.attacks.calculateTotalBonus
import ru.quasaris.characters.master.ui.DiceRollAdvantagePopup
import dev.chrisbanes.haze.HazeState
import java.util.Locale
import kotlin.math.floor
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun SpellsTab(
    spells: List<DynamicNoteState>,
    onSpellsChange: (List<DynamicNoteState>) -> Unit,
    characterLevel: Int = 1,
    spellSettings: SpellSettings = SpellSettings(),
    onSpellSettingsChange: (SpellSettings) -> Unit = {},
    hazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false,
    blurPopups: Boolean = false,
    isEditMode: Boolean = false,
    settingsViewModel: SettingsViewModel? = null,
    onRoll: (RollResult) -> Unit = {},
    statsMap: Map<String, String> = emptyMap(),
    exhaustion: Int = 0,
    advantageLogic: AdvantageLogic = AdvantageLogic.TOTAL,
    spellbookManager: SpellbookManager? = null,
    header: @Composable () -> Unit = {}
) {
    var showAddLevelDialog by remember { mutableStateOf(false) }
    
    var showSpellAtkPopup by remember { mutableStateOf(false) }
    var spellAtkBtnSize by remember { mutableStateOf(IntSize.Zero) }
    
    var editingSpell by remember { mutableStateOf<SpellCard?>(null) }
    var showSelectionDialog by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    val characterSpells = remember(spellSettings.selectedSpellIds, spellSettings.preparedSpellIds, spellSettings.isSpellbookEnabled, refreshTrigger) {
        val all = spellbookManager?.loadSpells() ?: emptyList()
        if (spellSettings.isSpellbookEnabled) {
            all.filter { 
                it.id in spellSettings.preparedSpellIds || (it.id in spellSettings.selectedSpellIds && it.isRitual)
            }
        } else {
            all.filter { it.id in spellSettings.selectedSpellIds }
        }
    }

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
        if (t.contains("прочее")) return -1f
        val match = Regex("([\\d.]+)").find(t)
        return match?.value?.toFloatOrNull() ?: -1f
    }

    fun formatLevelTitle(level: Float): String {
        if (level == 0f) return "Заговоры"
        if (level == -1f) return "Прочее"
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
        val baseFlat = pb + abilityModifier
        var totalFlat = baseFlat - (exhaustion * 2)
        val allDice = mutableMapOf<Int, Int>()
        
        totalFlat = calculateTotalBonus(spellSettings.spellAttackBonuses, statsMap, initialValue = totalFlat)
        spellSettings.spellAttackBonuses.filter { it.isActive }.forEach { bonus ->
            val (_, fDice) = parseFormulaParts(bonus.formula, statsMap)
            fDice.forEach {
                val sign = if (bonus.operation == BonusOperation.SUBTRACT) -1 else 1
                allDice[it.sides] = (allDice[it.sides] ?: 0) + (it.count * sign)
            }
        }
        Triple(totalFlat, baseFlat, allDice.map { DicePart(it.value, it.key) }.sortedBy { it.sides })
    }

    val magicSaveCalculation = remember(spellSettings.spellSaveDcBonuses, pb, abilityModifier, statsMap) {
        val baseFlat = 8 + pb + abilityModifier
        var totalFlat = baseFlat
        val allDice = mutableMapOf<Int, Int>()
        
        totalFlat = calculateTotalBonus(spellSettings.spellSaveDcBonuses, statsMap, initialValue = totalFlat)
        spellSettings.spellSaveDcBonuses.filter { it.isActive }.forEach { bonus ->
            val (_, fDice) = parseFormulaParts(bonus.formula, statsMap)
            fDice.forEach {
                val sign = if (bonus.operation == BonusOperation.SUBTRACT) -1 else 1
                allDice[it.sides] = (allDice[it.sides] ?: 0) + (it.count * sign)
            }
        }
        Triple(totalFlat, baseFlat, allDice.map { DicePart(it.value, it.key) }.sortedBy { it.sides })
    }

    val spellAttackBonus = magicAtkCalculation.first
    val spellAttackBase = magicAtkCalculation.second
    val spellAttackDice = magicAtkCalculation.third
    val spellSaveDc = magicSaveCalculation.first
    val spellSaveBase = magicSaveCalculation.second
    val spellSaveDice = magicSaveCalculation.third

    val longRestAlignment by settingsViewModel?.longRestAlignment?.collectAsState() ?: remember { mutableStateOf(SlotAlignment.RIGHT) }
    val longRestFillDirection by settingsViewModel?.longRestFillDirection?.collectAsState() ?: remember { mutableStateOf(SlotFillDirection.LTR) }
    val shortRestAlignment by settingsViewModel?.shortRestAlignment?.collectAsState() ?: remember { mutableStateOf(SlotAlignment.RIGHT) }
    val shortRestFillDirection by settingsViewModel?.shortRestFillDirection?.collectAsState() ?: remember { mutableStateOf(SlotFillDirection.LTR) }
    val dawnRestAlignment by settingsViewModel?.dawnRestAlignment?.collectAsState() ?: remember { mutableStateOf(SlotAlignment.RIGHT) }
    val dawnRestFillDirection by settingsViewModel?.dawnRestFillDirection?.collectAsState() ?: remember { mutableStateOf(SlotFillDirection.LTR) }

    val processedSpells = remember(spells, spellSettings.selectedSpellIds, spellSettings.specialSlots, spellSettings.pactSlotLevel, spellSettings.isPactEnabled, spellSettings.casterType, spellSettings.isMulticlass, characterSpells) {
        val specialLevels = spellSettings.specialSlots.map { it.level }.toMutableSet()
        if (spellSettings.isPactEnabled) specialLevels.add(spellSettings.pactSlotLevel)
        
        if (spellSettings.casterType != ru.quasaris.characters.master.CasterType.NONE || spellSettings.isMulticlass) {
            for (i in 1..9) specialLevels.add(i.toFloat())
        }
        specialLevels.add(0f)
        
        // Add "Other" (-1f) if there are spells with non-numeric levels
        if (characterSpells.any { it.level.trim() != "0" && it.level.trim().toIntOrNull() == null }) {
            specialLevels.add(-1f)
        }

        val currentList = spells.toMutableList()
        val existingLevels = currentList.map { parseLevelFromTitle(it.title) }
        
        val missingLevels = specialLevels.filter { it !in existingLevels }.sorted()
        
        missingLevels.forEach { level ->
            currentList.add(DynamicNoteState(
                title = formatLevelTitle(level),
                isExpanded = level <= 1f
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
                    .padding(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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

                Surface(
                    modifier = Modifier
                        .onGloballyPositioned { coords ->
                            spellAtkBtnSize = coords.size
                        }
                        .combinedClickable(
                            onClick = { 
                                onRoll(DiceRoller.roll(
                                    title = "Атака заклинанием", 
                                    baseModifier = spellAttackBase, 
                                    bonuses = spellSettings.spellAttackBonuses,
                                    stats = statsMap, 
                                    exhaustion = exhaustion, 
                                    sourceType = RollSourceType.ATTACK,
                                    advantageType = AdvantageType.NONE,
                                    advantageLogic = advantageLogic
                                )) 
                            },
                            onLongClick = { showSpellAtkPopup = true }
                        ),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
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
                                diceOnLeft = true,
                                useXForZero = true
                            )
                        }
                        
                        if (showSpellAtkPopup) {
                            val density = LocalDensity.current
                            val sizeDp = with(density) { spellAtkBtnSize.toSize().let { androidx.compose.ui.unit.DpSize((it.width / density.density).dp, (it.height / density.density).dp) } }
                            DiceRollAdvantagePopup(
                                onAdvantage = {
                                    onRoll(DiceRoller.roll(
                                        title = "Атака заклинанием", 
                                        baseModifier = spellAttackBase, 
                                        bonuses = spellSettings.spellAttackBonuses,
                                        stats = statsMap, 
                                        exhaustion = exhaustion, 
                                        sourceType = RollSourceType.ATTACK,
                                        advantageType = AdvantageType.ADVANTAGE,
                                        advantageLogic = advantageLogic
                                    ))
                                },
                                onDisadvantage = {
                                    onRoll(DiceRoller.roll(
                                        title = "Атака заклинанием", 
                                        baseModifier = spellAttackBase, 
                                        bonuses = spellSettings.spellAttackBonuses,
                                        stats = statsMap, 
                                        exhaustion = exhaustion, 
                                        sourceType = RollSourceType.ATTACK,
                                        advantageType = AdvantageType.DISADVANTAGE,
                                        advantageLogic = advantageLogic
                                    ))
                                },
                                onDismiss = { showSpellAtkPopup = false },
                                hazeState = hazeState,
                                isOled = MaterialTheme.colorScheme.background == Color.Black,
                                modifier = Modifier.size(sizeDp)
                            )
                        }
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
                blurPopups = blurPopups,
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
                isContentVisible = spellSettings.spellMode != SpellMode.CARDS,
                header = header,
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
                        val specialLong = spellSettings.specialSlots.filter { it.level == level && !it.restoreOnShortRest && !it.restoreOnDawn }.sumOf { it.count }
                        val maxLong = baseSlots + specialLong

                        val pactSlots = if (spellSettings.isPactEnabled && spellSettings.pactSlotLevel == level) spellSettings.pactSlotsCount else 0
                        val specialShort = spellSettings.specialSlots.filter { it.level == level && it.restoreOnShortRest }.sumOf { it.count }
                        val maxShort = pactSlots + specialShort

                        val maxDawn = spellSettings.specialSlots.filter { it.level == level && it.restoreOnDawn }.sumOf { it.count }

                        if (maxLong > 0 || maxShort > 0 || maxDawn > 0) {
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
                                        if (maxDawn > 0) {
                                            SpellSlotTracker(
                                                maxSlots = maxDawn,
                                                usedSlots = spellSettings.usedSlotsDawn[level] ?: 0,
                                                isShortRest = false,
                                                isDawnRest = true,
                                                onUsedSlotsChange = { newUsed ->
                                                    onSpellSettingsChange(
                                                        spellSettings.copy(
                                                            usedSlotsDawn = spellSettings.usedSlotsDawn.toMutableMap().apply { put(level, newUsed) }
                                                        )
                                                    )
                                                },
                                                alignment = dawnRestAlignment,
                                                fillDirection = dawnRestFillDirection
                                            )
                                        }
                                    },
                                    measurePolicy = { measurables, constraints ->
                                        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0)) }
                                        val spacing = 8.dp.roundToPx()
                                        val totalHeight = if (placeables.isEmpty()) 0 else placeables.sumOf { it.height } + (placeables.size - 1) * spacing

                                        var currentY = 0
                                        layout(constraints.maxWidth, totalHeight) {
                                            placeables.forEachIndexed { index, placeable ->
                                                val alignment = when (index) {
                                                    0 -> longRestAlignment
                                                    1 -> shortRestAlignment
                                                    else -> dawnRestAlignment
                                                }
                                                val x = when (alignment) {
                                                    SlotAlignment.LEFT -> 0
                                                    SlotAlignment.CENTER -> (constraints.maxWidth - placeable.width) / 2
                                                    SlotAlignment.RIGHT -> constraints.maxWidth - placeable.width
                                                }
                                                placeable.place(x, currentY)
                                                currentY += placeable.height + spacing
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }

                    if (spellSettings.spellMode == SpellMode.CARDS || spellSettings.spellMode == SpellMode.HYBRID) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            val levelStr = if (level == 0f) "0" else if (level == -1f) "" else level.toInt().toString()
                            val levelSpells = characterSpells.filter { 
                                val cardLevel = it.level.trim()
                                if (level == -1f) {
                                    cardLevel != "0" && cardLevel.toIntOrNull() == null
                                } else {
                                    cardLevel == levelStr
                                }
                            }
                            
                            levelSpells.forEach { card ->
                                var expanded by remember { mutableStateOf(false) }
                                SpellCardItem(
                                    spell = card,
                                    isExpanded = expanded,
                                    onToggleExpand = { expanded = !expanded },
                                    onEdit = { editingSpell = card },
                                    onRollDamage = { formula, title, advantage ->
                                        onRoll(DiceRoller.roll(
                                            title = title,
                                            baseModifier = 0,
                                            bonuses = listOf(SimpleBonus(formula = formula, name = "Урон")),
                                            isDamage = true,
                                            stats = statsMap,
                                            exhaustion = 0,
                                            sourceType = RollSourceType.OTHER,
                                            advantageType = advantage,
                                            advantageLogic = advantageLogic
                                        ))
                                    },
                                    onRollAttack = { advantage ->
                                        if (card.attackTypes.contains(MagicAttackType.ATTACK)) {
                                            onRoll(DiceRoller.roll(
                                                title = "${card.name} (Атака)",
                                                baseModifier = spellAttackBase,
                                                bonuses = spellSettings.spellAttackBonuses,
                                                stats = statsMap,
                                                exhaustion = exhaustion,
                                                sourceType = RollSourceType.ATTACK,
                                                advantageType = advantage,
                                                advantageLogic = advantageLogic
                                            ))
                                        }
                                    },
                                    isEditable = true,
                                    statsMap = statsMap,
                                    spellAttackBonus = spellAttackBonus,
                                    spellAttackDice = spellAttackDice,
                                    spellSaveDc = spellSaveDc,
                                    spellSaveDice = spellSaveDice,
                                    hazeState = hazeState,
                                    forceBlurEnabled = forceBlurEnabled,
                                    highlightRitual = spellSettings.isSpellbookEnabled && card.id !in spellSettings.preparedSpellIds && card.isRitual
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { 
                                        val initialLevel = if (level == 0f) "0" else if (level == -1f) "" else level.toInt().toString()
                                        editingSpell = SpellCard(level = initialLevel) 
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Добавить", fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = { showSelectionDialog = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.LibraryBooks, null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Выбрать", fontSize = 12.sp)
                                }
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

    editingSpell?.let { spell ->
        SpellCardEditorDialog(
            spell = spell,
            onDismiss = { editingSpell = null },
            onSave = { updated ->
                spellbookManager?.addOrUpdateSpell(updated)
                if (updated.id !in spellSettings.selectedSpellIds) {
                    onSpellSettingsChange(spellSettings.copy(selectedSpellIds = spellSettings.selectedSpellIds + updated.id))
                }
                editingSpell = null
                refreshTrigger++
            },
            onDelete = {
                spellbookManager?.deleteSpell(it.id)
                onSpellSettingsChange(spellSettings.copy(selectedSpellIds = spellSettings.selectedSpellIds - it.id))
                editingSpell = null
                refreshTrigger++
            },
            hazeState = hazeState,
            forceBlurEnabled = forceBlurEnabled,
            settingsViewModel = settingsViewModel
        )
    }

    if (showSelectionDialog && spellbookManager != null) {
        SpellbookSelectionDialog(
            spellbookManager = spellbookManager,
            selectedIds = spellSettings.selectedSpellIds,
            preparedIds = spellSettings.preparedSpellIds,
            isSpellbookEnabled = spellSettings.isSpellbookEnabled,
            onDismiss = { showSelectionDialog = false },
            onSave = { newIds, newPrepared ->
                onSpellSettingsChange(spellSettings.copy(
                    selectedSpellIds = newIds,
                    preparedSpellIds = newPrepared
                ))
                refreshTrigger++
            },
            hazeState = hazeState,
            forceBlurEnabled = forceBlurEnabled,
            statsMap = statsMap,
            spellAttackBonus = spellAttackBonus,
            spellAttackDice = spellAttackDice,
            spellSaveDc = spellSaveDc,
            spellSaveDice = spellSaveDice,
            onRollDamage = { formula, title, advantage ->
                onRoll(DiceRoller.roll(
                    title = title,
                    baseModifier = 0,
                    bonuses = listOf(SimpleBonus(formula = formula, name = "Урон")),
                    isDamage = true,
                    stats = statsMap,
                    exhaustion = 0,
                    sourceType = RollSourceType.OTHER,
                    advantageType = advantage,
                    advantageLogic = advantageLogic
                ))
            },
            onRollAttack = { advantage ->
                onRoll(DiceRoller.roll(
                    title = "Атака заклинанием",
                    baseModifier = spellAttackBase,
                    bonuses = spellSettings.spellAttackBonuses,
                    stats = statsMap,
                    exhaustion = exhaustion,
                    sourceType = RollSourceType.ATTACK,
                    advantageType = advantage,
                    advantageLogic = advantageLogic
                ))
            }
        )
    }
}
