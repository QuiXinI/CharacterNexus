package ru.quasaris.characternexus.tabs.spells

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import ru.quasaris.characternexus.ui.outerShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import dev.chrisbanes.haze.HazeState
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.backend.*
import ru.quasaris.characternexus.tabs.DynamicFieldsTab
import ru.quasaris.characternexus.tabs.attacks.AttackBonusIndicator
import ru.quasaris.characternexus.tabs.attacks.DiceIcon
import ru.quasaris.characternexus.tabs.attacks.calculateTotalBonus
import ru.quasaris.characternexus.tabs.attacks.calculateAttackFormulaParts
import ru.quasaris.characternexus.ui.DiceRollAdvantagePopup
import ru.quasaris.characternexus.ui.TabControlHeader
import ru.quasaris.characternexus.ui.editors.SpellEditorWindow
import sh.calvin.reorderable.*
import kotlin.math.floor

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun SpellsTab(
    spells: List<DynamicNoteState>,
    onSpellsChange: (List<DynamicNoteState>) -> Unit,
    characterLevel: Int = 1,
    spellSettings: SpellSettings = SpellSettings(),
    onSpellSettingsChange: (SpellSettings) -> Unit = {},
    hazeState: HazeState? = null,
    popupHazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false,
    blurPopups: Boolean = false,
    isEditMode: Boolean = false,
    onToggleEditMode: () -> Unit = {},
    onToggleAllExpansion: () -> Unit = {},
    anyCollapsed: Boolean = false,
    onShowSpellSettings: () -> Unit = {},
    settingsViewModel: SettingsViewModel? = null,
    onRoll: (RollResult) -> Unit = {},
    statsMap: Map<String, String> = emptyMap(),
    exhaustion: Int = 0,
    advantageLogic: AdvantageLogic = AdvantageLogic.TOTAL,
    spellbookManager: SpellbookManager? = null,
    onSpellEditorOpenChange: (Boolean) -> Unit = {},
    onMagicBonusSettingsOpenChange: (Boolean) -> Unit = {},
    onFullscreenDialogOpenChange: (Boolean) -> Unit = {},
    onFullscreenVisibilityChanged: (Boolean) -> Unit = {},
    onSpellbookSelectionOpenChange: (Boolean) -> Unit = {},
    state: ru.quasaris.characternexus.ui.CharacterDetailState? = null,
    header: @Composable () -> Unit = {}
) {
    var showAddLevelDialog by remember { mutableStateOf(false) }

    var showSpellAtkPopup by remember { mutableStateOf(false) }
    var spellAtkBtnSize by remember { mutableStateOf(IntSize.Zero) }

    var editingSpell by remember { mutableStateOf<SpellCard?>(null) }
    var showMagicBonusSettings by remember { mutableStateOf(false) }
    
    LaunchedEffect(editingSpell, showMagicBonusSettings) {
        onSpellEditorOpenChange(editingSpell != null)
        onMagicBonusSettingsOpenChange(showMagicBonusSettings)
        state?.editingSpell = editingSpell
        state?.isSpellEditorOpen = editingSpell != null
    }

    LaunchedEffect(state?.isSpellEditorOpen) {
        if (state?.isSpellEditorOpen == false) {
            editingSpell = null
        }
    }

    LaunchedEffect(state?.isMagicBonusSettingsOpen) {
        if (state?.isMagicBonusSettingsOpen == false) {
            showMagicBonusSettings = false
        }
    }

    var showSelectionDialog by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(showSelectionDialog) {
        onSpellbookSelectionOpenChange(showSelectionDialog)
    }

    LaunchedEffect(state?.isSpellbookSelectionOpen) {
        if (state?.isSpellbookSelectionOpen == false) {
            showSelectionDialog = false
        }
    }

    var levelInEditMode by remember { mutableStateOf<Float?>(null) }
    var showDividerEditor by remember { mutableStateOf<Pair<Float, SpellLevelDivider?>?>(null) }

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

    val availableSlotLevels = remember(autoSlots, spellSettings.overrideSlots, spellSettings.specialSlots, spellSettings.isPactEnabled, spellSettings.pactSlotLevel, spellSettings.pactSlotsCount) {
        val levels = mutableSetOf<Int>()
        for (i in 1..9) {
            val count = spellSettings.overrideSlots[i.toFloat()] ?: autoSlots.getOrNull(i - 1) ?: 0
            if (count > 0) levels.add(i)
        }
        if (spellSettings.isPactEnabled && spellSettings.pactSlotsCount > 0) {
            levels.add(spellSettings.pactSlotLevel.toInt())
        }
        spellSettings.specialSlots.forEach {
            if (it.count > 0) levels.add(it.level.toInt())
        }
        levels.toList().sorted()
    }

    val remainingSlots = remember(autoSlots, spellSettings.overrideSlots, spellSettings.specialSlots, spellSettings.isPactEnabled, spellSettings.pactSlotLevel, spellSettings.pactSlotsCount, spellSettings.usedSlots, spellSettings.usedSlotsShortRest, spellSettings.usedSlotsDawn) {
        val res = mutableMapOf<Int, Int>()
        for (lvl in 1..9) {
            val level = lvl.toFloat()
            val baseSlots = spellSettings.overrideSlots[level] ?: autoSlots.getOrNull(lvl - 1) ?: 0
            val specialLong = spellSettings.specialSlots.filter { it.level == level && !it.restoreOnShortRest && !it.restoreOnDawn }.sumOf { it.count }
            val pactSlots = if (spellSettings.isPactEnabled && spellSettings.pactSlotLevel == level) spellSettings.pactSlotsCount else 0
            val specialShort = spellSettings.specialSlots.filter { it.level == level && it.restoreOnShortRest }.sumOf { it.count }
            val dawnSlots = spellSettings.specialSlots.filter { it.level == level && it.restoreOnDawn }.sumOf { it.count }

            val totalMax = baseSlots + specialLong + pactSlots + specialShort + dawnSlots
            val totalUsed = (spellSettings.usedSlots[level] ?: 0) + (spellSettings.usedSlotsShortRest[level] ?: 0) + (spellSettings.usedSlotsDawn[level] ?: 0)
            res[lvl] = (totalMax - totalUsed).coerceAtLeast(0)
        }
        res
    }

    fun parseLevelFromTitle(title: String): Float {
        val t = title.lowercase()
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
    val abilityModifier = remember(spellSettings.spellcastingAbility, statsMap) {
        if (spellSettings.spellcastingAbility != Attribute.NONE) {
            val statKey = spellSettings.spellcastingAbility.name.lowercase()
            calculateModifier(statsMap[statKey] ?: "10")
        } else 0
    }

    val renderDiceInOrder by settingsViewModel?.renderDiceInOrder?.collectAsState() ?: remember { mutableStateOf(true) }
    val collapseSpellsOnEdit by settingsViewModel?.collapseSpellsOnEdit?.collectAsState() ?: remember { mutableStateOf(true) }

    val magicAtkCalculation = remember(spellSettings.spellAttackBonuses, pb, abilityModifier, statsMap, exhaustion, renderDiceInOrder) {
        val baseFlat = pb + abilityModifier
        val (totalFlat, finalDice) = calculateAttackFormulaParts(
            baseFlat = baseFlat,
            bonuses = spellSettings.spellAttackBonuses,
            stats = statsMap,
            renderInOrder = renderDiceInOrder
        )
        Triple(totalFlat, baseFlat, finalDice)
    }

    val magicSaveCalculation = remember(spellSettings.spellSaveDcBonuses, pb, abilityModifier, statsMap, renderDiceInOrder) {
        val baseFlat = 8 + pb + abilityModifier
        val (totalFlat, finalDice) = calculateAttackFormulaParts(
            baseFlat = baseFlat,
            bonuses = spellSettings.spellSaveDcBonuses,
            stats = statsMap,
            renderInOrder = renderDiceInOrder
        )
        Triple(totalFlat, baseFlat, finalDice)
    }

    val spellAttackBonus = magicAtkCalculation.first
    val displaySpellAttackBonus = spellAttackBonus - (exhaustion * 2)
    val spellAttackBase = magicAtkCalculation.second
    val spellAttackDice = magicAtkCalculation.third
    val spellSaveDc = magicSaveCalculation.first
    val spellSaveDice = magicSaveCalculation.third

    val longRestAlignment by settingsViewModel?.longRestAlignment?.collectAsState() ?: remember { mutableStateOf(SlotAlignment.RIGHT) }
    val longRestFillDirection by settingsViewModel?.longRestFillDirection?.collectAsState() ?: remember { mutableStateOf(SlotFillDirection.LTR) }
    val shortRestAlignment by settingsViewModel?.shortRestAlignment?.collectAsState() ?: remember { mutableStateOf(SlotAlignment.RIGHT) }
    val shortRestFillDirection by settingsViewModel?.shortRestFillDirection?.collectAsState() ?: remember { mutableStateOf(SlotFillDirection.LTR) }
    val dawnRestAlignment by settingsViewModel?.dawnRestAlignment?.collectAsState() ?: remember { mutableStateOf(SlotAlignment.RIGHT) }
    val dawnRestFillDirection by settingsViewModel?.dawnRestFillDirection?.collectAsState() ?: remember { mutableStateOf(SlotFillDirection.LTR) }
    val blurCards by settingsViewModel?.blurCards?.collectAsState() ?: remember { mutableStateOf(true) }

    val processedSpells = remember(spells, spellSettings.selectedSpellIds, spellSettings.specialSlots, spellSettings.pactSlotLevel, spellSettings.isPactEnabled, spellSettings.casterType, spellSettings.isMulticlass, characterSpells) {
        val specialLevels = spellSettings.specialSlots.map { it.level }.toMutableSet()
        if (spellSettings.isPactEnabled) specialLevels.add(spellSettings.pactSlotLevel)

        if (spellSettings.casterType != CasterType.NONE || spellSettings.isMulticlass) {
            for (i in 1..9) specialLevels.add(i.toFloat())
        }
        specialLevels.add(0f)

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

    val colorScheme = MaterialTheme.colorScheme

    Column(modifier = Modifier.fillMaxSize()) {
        if (spellSettings.isMagicEnabled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max)
                    .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxHeight()
                        .outerShadow(
                            shape = RoundedCornerShape(12.dp),
                            blur = 4.dp,
                            offsetY = 2.dp
                    ),
                    color = colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Спасбросок",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.primary,
                            fontSize = 10.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = spellSaveDc.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurface
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
                        .fillMaxHeight()
                        .onGloballyPositioned { coords -> spellAtkBtnSize = coords.size }
                        .outerShadow(
                            shape = RoundedCornerShape(12.dp),
                            blur = 4.dp,
                            offsetY = 2.dp
                        )
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
                    color = colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 0.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Атака",
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.primary,
                                fontSize = 10.sp
                            )
                            AttackBonusIndicator(
                                bonus = displaySpellAttackBonus,
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
                                isOled = colorScheme.background == Color.Black,
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
                isReorderButtonVisible = true,
                isContentVisible = spellSettings.spellMode != SpellMode.CARDS,
                collapseOnEdit = collapseSpellsOnEdit,
                onFullscreenDialogOpenChange = onFullscreenDialogOpenChange,
                onFullscreenVisibilityChanged = onFullscreenVisibilityChanged,
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
                        val levelStr = if (level == 0f) "0" else if (level == -1f) "" else level.toInt().toString()
                        val levelItems = remember(level, spellSettings.levelContent, characterSpells) {
                            val savedItems = spellSettings.levelContent[levelStr] ?: emptyList()
                            val currentSpellIds = characterSpells.filter {
                                val cardLevel = it.level.trim()
                                if (level == -1f) cardLevel != "0" && cardLevel.toIntOrNull() == null
                                else cardLevel == levelStr
                            }.map { it.id }.toSet()

                            val result = savedItems.filter { it.divider != null || (it.spellId != null && it.spellId in currentSpellIds) }.toMutableList()
                            val handledSpellIds = result.mapNotNull { it.spellId }.toSet()
                            currentSpellIds.filter { it !in handledSpellIds }.forEach {
                                result.add(SpellLevelItem(spellId = it))
                            }
                            result
                        }

                        val isListEditMode = levelInEditMode == level
                        var draggingItemKey by remember { mutableStateOf<Any?>(null) }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                                .outerShadow(
                                    shape = RoundedCornerShape(12.dp),
                                    blur = 2.dp,
                                    offsetY = 1.dp
                                ),
                            shape = RoundedCornerShape(12.dp),
                            color = if (blurCards && hazeState != null) colorScheme.surfaceContainerLow.copy(alpha = 0.5f)
                            else colorScheme.surfaceContainerLow,
                            tonalElevation = 1.dp,
                            shadowElevation = 0.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .clipToBounds()
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isListEditMode) "Редактирование порядка" else "Список заклинаний",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colorScheme.primary.copy(alpha = 0.7f)
                                    )
                                    IconButton(
                                        onClick = { levelInEditMode = if (isListEditMode) null else level },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isListEditMode) Icons.Default.Done else Icons.Default.Edit,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (isListEditMode) colorScheme.primary else colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                ReorderableColumn(
                                    list = levelItems,
                                    onSettle = { from, to ->
                                        val updatedList = levelItems.toMutableList().apply { add(to, removeAt(from)) }
                                        onSpellSettingsChange(spellSettings.copy(
                                            levelContent = spellSettings.levelContent.toMutableMap().apply { put(levelStr, updatedList) }
                                        ))
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) { idx, item, isDragging ->
                                    val itemKey = remember(item) { item.spellId ?: item.divider?.id ?: idx }
                                    LaunchedEffect(isDragging) {
                                        if (isDragging) draggingItemKey = itemKey
                                        else if (draggingItemKey == itemKey) draggingItemKey = null
                                    }

                                    val abilityForThisItem = remember(item, spellSettings.spellcastingAbility) {
                                        var current = spellSettings.spellcastingAbility
                                        for (i in 0..idx) {
                                            val itm = if (i < levelItems.size) levelItems[i] else null
                                            if (itm?.divider != null) {
                                                current = if (itm.divider.ability != Attribute.NONE) itm.divider.ability else spellSettings.spellcastingAbility
                                            }
                                        }
                                        current
                                    }

                                    ReorderableItem {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (isListEditMode) {
                                                Icon(
                                                    imageVector = Icons.Default.DragHandle,
                                                    contentDescription = "Перетащить",
                                                    modifier = Modifier
                                                        .padding(end = 8.dp)
                                                        .size(32.dp)
                                                        .draggableHandle(),
                                                    tint = colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                                )
                                            }

                                            Box(modifier = Modifier.weight(1f)) {
                                                if (item.divider != null) {
                                                    val isAnyItemDragging = draggingItemKey != null
                                                    val dividerScale by animateFloatAsState(targetValue = if (isDragging) 1.02f else 1f)
                                                    val dividerBlur by animateDpAsState(targetValue = if (isAnyItemDragging && !isDragging) 6.dp else 0.dp)
                                                    Surface(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .scale(dividerScale)
                                                            .then(
                                                                if (dividerBlur > 0.dp) 
                                                                    Modifier.blur(dividerBlur, edgeTreatment = BlurredEdgeTreatment.Unbounded) 
                                                                else Modifier
                                                            )
                                                            .then(if (isDragging) Modifier.outerShadow(RoundedCornerShape(8.dp), blur = 16.dp, offsetY = 8.dp) else Modifier),
                                                        color = colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                                                        shape = RoundedCornerShape(8.dp),
                                                        shadowElevation = 0.dp
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.weight(1f),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Text(
                                                                    item.divider.title,
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 14.sp,
                                                                    color = colorScheme.onSurface,
                                                                    modifier = Modifier.weight(1f, fill = false)
                                                                )
                                                                if (item.divider.ability != Attribute.NONE) {
                                                                    Spacer(Modifier.width(8.dp))
                                                                    Surface(
                                                                        color = colorScheme.primary.copy(alpha = 0.1f),
                                                                        shape = RoundedCornerShape(4.dp)
                                                                    ) {
                                                                        Text(
                                                                            item.divider.ability.shortName,
                                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                                                            fontSize = 10.sp,
                                                                            color = colorScheme.primary
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                            if (isListEditMode) {
                                                                Spacer(Modifier.width(8.dp))
                                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                                    IconButton(onClick = { showDividerEditor = level to item.divider }, modifier = Modifier.size(24.dp)) {
                                                                        Icon(Icons.Default.Settings, null, modifier = Modifier.size(16.dp), tint = colorScheme.onSurfaceVariant)
                                                                    }
                                                                    IconButton(onClick = {
                                                                        val updated = levelItems.toMutableList().apply { removeAt(idx) }
                                                                        onSpellSettingsChange(spellSettings.copy(
                                                                            levelContent = spellSettings.levelContent.toMutableMap().apply { put(levelStr, updated) }
                                                                        ))
                                                                    }, modifier = Modifier.size(24.dp)) {
                                                                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp), tint = colorScheme.error.copy(alpha = 0.7f))
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                } else if (item.spellId != null) {
                                                    val card = characterSpells.find { it.id == item.spellId }
                                                    if (card != null) {
                                                        val spellAbility = spellSettings.spellAbilityOverrides[card.id] ?: abilityForThisItem
                                                        val spellMod = if (spellAbility != Attribute.NONE) {
                                                            calculateModifier(statsMap[spellAbility.name.lowercase()] ?: "10")
                                                        } else abilityModifier

                                                        val (spellAtk, spellAtkDice) = calculateAttackFormulaParts(
                                                            baseFlat = pb + spellMod,
                                                            bonuses = spellSettings.spellAttackBonuses,
                                                            stats = statsMap,
                                                            renderInOrder = renderDiceInOrder
                                                        )
                                                        val displaySpellAtk = spellAtk - (exhaustion * 2)
                                                        val (spellDc, spellSaveDiceParts) = calculateAttackFormulaParts(
                                                            baseFlat = 8 + pb + spellMod,
                                                            bonuses = spellSettings.spellSaveDcBonuses,
                                                            stats = statsMap,
                                                            renderInOrder = renderDiceInOrder
                                                        )

                                                        var expanded by remember { mutableStateOf(false) }
                                                        var savedExpanded by remember { mutableStateOf<Boolean?>(null) }
                                                        LaunchedEffect(isListEditMode) {
                                                            if (isListEditMode) {
                                                                if (collapseSpellsOnEdit) {
                                                                    savedExpanded = expanded
                                                                    expanded = false
                                                                }
                                                            } else {
                                                                savedExpanded?.let {
                                                                    if (collapseSpellsOnEdit) {
                                                                        expanded = it
                                                                    }
                                                                    savedExpanded = null
                                                                }
                                                            }
                                                        }

                                                        SpellCardItem(
                                                            spell = card,
                                                            isExpanded = expanded,
                                                            onToggleExpand = { expanded = !expanded },
                                                            onEdit = { editingSpell = card },
                                                            onRollDamage = { formula, title, advantage ->
                                                                val isHealing = card.damageTypes.contains(DamageType.HEALING)
                                                                val finalTitle = if (isHealing) title.replace("Урон:", "Лечение:").replace("Дополнительный урон:", "Дополнительное лечение:") else title
                                                                onRoll(DiceRoller.roll(
                                                                    title = finalTitle,
                                                                    baseModifier = 0,
                                                                    bonuses = listOf(SimpleBonus(formula = formula, name = if (isHealing) "Лечение" else "Урон")),
                                                                    isDamage = !isHealing,
                                                                    isHealing = isHealing,
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
                                                                        baseModifier = pb + spellMod,
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
                                                            characterLevel = characterLevel,
                                                            spellAttackBonus = displaySpellAtk,
                                                            spellAttackDice = spellAtkDice,
                                                            spellSaveDc = spellDc,
                                                            spellSaveDice = spellSaveDiceParts,
                                                            availableSlotLevels = availableSlotLevels,
                                                            remainingSlots = remainingSlots,
                                                            allowCantripUpcast = spellSettings.allowCantripUpcast,
                                                            hazeState = hazeState,
                                                            popupHazeState = popupHazeState,
                                                            forceBlurEnabled = forceBlurEnabled,
                                                            blurCards = blurCards,
                                                            highlightRitual = spellSettings.isSpellbookEnabled && card.id !in spellSettings.preparedSpellIds && card.isRitual,
                                                            isEditMode = isListEditMode,
                                                            collapseOnEdit = collapseSpellsOnEdit,
                                                            isDragging = isDragging,
                                                            isAnyItemDragging = draggingItemKey != null,
                                                            settingsViewModel = settingsViewModel
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                
                                if (isListEditMode) {
                                    TextButton(
                                        onClick = { showDividerEditor = level to null },
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Добавить разделитель")
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
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
                }
            )


            if (spellSettings.isMagicEnabled) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.12f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
        }
    }

    if (showDividerEditor != null) {
        val level = showDividerEditor!!.first
        val existing = showDividerEditor!!.second
        var title by remember { mutableStateOf(existing?.title ?: "") }
        var ability by remember { mutableStateOf(existing?.ability ?: Attribute.NONE) }

        AlertDialog(
            onDismissRequest = { showDividerEditor = null },
            title = { Text(if (existing == null) "Новый разделитель" else "Настройка разделителя") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Заголовок") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Характеристика для заклинаний под разделителем:", style = MaterialTheme.typography.labelSmall)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        val row1 = listOf(Attribute.STRENGTH, Attribute.DEXTERITY, Attribute.CONSTITUTION)
                        val row2 = listOf(Attribute.INTELLIGENCE, Attribute.WISDOM, Attribute.CHARISMA)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            row1.forEach { attr ->
                                FilterChip(
                                    selected = ability == attr,
                                    onClick = { ability = if (ability == attr) Attribute.NONE else attr },
                                    label = { Text(attr.shortName, fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp)) },
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            row2.forEach { attr ->
                                FilterChip(
                                    selected = ability == attr,
                                    onClick = { ability = if (ability == attr) Attribute.NONE else attr },
                                    label = { Text(attr.shortName, fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp)) },
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val levelStr = if (level == 0f) "0" else if (level == -1f) "" else level.toInt().toString()
                    val currentList = spellSettings.levelContent[levelStr]?.toMutableList() ?: mutableListOf()

                    if (existing == null) {
                        currentList.add(SpellLevelItem(divider = SpellLevelDivider(title = title, ability = ability)))
                    } else {
                        val idx = currentList.indexOfFirst { it.divider?.id == existing.id }
                        if (idx != -1) {
                            currentList[idx] = currentList[idx].copy(divider = existing.copy(title = title, ability = ability))
                        }
                    }

                    onSpellSettingsChange(spellSettings.copy(
                        levelContent = spellSettings.levelContent.toMutableMap().apply { put(levelStr, currentList) }
                    ))
                    showDividerEditor = null
                }) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDividerEditor = null }) {
                    Text("Отмена")
                }
            }
        )
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

    if (editingSpell != null && state == null) {
        SpellEditorWindow(
            spell = editingSpell!!,
            onDismiss = { editingSpell = null },
            onSave = { updated ->
                spellbookManager?.addOrUpdateSpell(updated)
                if (updated.id !in spellSettings.selectedSpellIds) {
                    onSpellSettingsChange(spellSettings.copy(selectedSpellIds = spellSettings.selectedSpellIds + updated.id))
                }
                editingSpell = null
                refreshTrigger++
            },
            onDelete = { deletedSpell ->
                spellbookManager?.deleteSpell(deletedSpell.id)
                onSpellSettingsChange(spellSettings.copy(selectedSpellIds = spellSettings.selectedSpellIds - deletedSpell.id))
                editingSpell = null
                refreshTrigger++
            },
            forceBlurEnabled = forceBlurEnabled,
            settingsViewModel = settingsViewModel
        )
    }
}
