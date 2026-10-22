package ru.quasaris.characternexus.tabs.spells

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.backend.*
import ru.quasaris.characternexus.tabs.attacks.calculateAttackFormulaParts
import ru.quasaris.characternexus.ui.outerShadow
import sh.calvin.reorderable.*

@Composable
fun DynamicSpellFieldCard(
    spellFieldId: String,
    state: ru.quasaris.characternexus.ui.CharacterDetailState?,
    spellbookManager: SpellbookManager? = null,
    hazeState: HazeState? = null,
    popupHazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false,
    settingsViewModel: SettingsViewModel? = null,
    statsMap: Map<String, String> = emptyMap(),
    characterLevel: Int = 1,
    onRoll: (RollResult) -> Unit = {},
    isDesktop: Boolean = false
) {
    val colorScheme = MaterialTheme.colorScheme
    val spellFieldManager = state?.spellFieldManager
    val spellField = spellFieldManager?.get(spellFieldId) ?: DynamicSpellField(id = spellFieldId, title = "Заклинания")

    val isExpanded = spellField.isExpanded

    var title by remember(spellField.title) { mutableStateOf(spellField.title) }
    var isEditMode by remember { mutableStateOf(false) }
    var showSelectionDialog by remember { mutableStateOf(false) }
    var showDividerEditor by remember { mutableStateOf<SpellLevelDivider?>(null) }
    var editingSpell by remember { mutableStateOf<SpellCard?>(null) }

    LaunchedEffect(isEditMode) {
        if (isEditMode && !isExpanded) {
            spellFieldManager?.upsert(spellField.copy(isExpanded = true))
        }
    }

    val baseSpells = remember(state?.refreshTrigger) {
        spellbookManager?.loadSpells() ?: emptyList()
    }

    val spellSettings = state?.spellSettings ?: SpellSettings()
    val characterLevelInt = state?.level?.toIntOrNull() ?: characterLevel
    val pb = getProficiencyBonus(characterLevelInt.toString())
    val abilityModifier = remember(spellSettings.spellcastingAbility, statsMap) {
        if (spellSettings.spellcastingAbility != Attribute.NONE) {
            val statKey = spellSettings.spellcastingAbility.name.lowercase()
            calculateModifier(statsMap[statKey] ?: "10")
        } else 0
    }

    val renderDiceInOrder by settingsViewModel?.renderDiceInOrder?.collectAsState() ?: remember { mutableStateOf(true) }
    val collapseSpellsOnEdit by settingsViewModel?.collapseSpellsOnEdit?.collectAsState() ?: remember { mutableStateOf(true) }
    val blurCards by settingsViewModel?.blurCards?.collectAsState() ?: remember { mutableStateOf(true) }

    val autoSlots = remember(spellSettings.casterType, spellSettings.isMulticlass, spellSettings.fullCasterLevel, spellSettings.halfCasterLevel, spellSettings.thirdCasterLevel, characterLevelInt) {
        if (spellSettings.isMulticlass) {
            SpellSlotCalculator.getMulticlassSlots(spellSettings.fullCasterLevel, spellSettings.halfCasterLevel, spellSettings.thirdCasterLevel)
        } else {
            SpellSlotCalculator.getSlotsForLevel(spellSettings.casterType, characterLevelInt)
        }
    }

    val availableSlotLevels = remember(autoSlots, spellSettings) {
        val levels = mutableSetOf<Int>()
        for (i in 1..9) {
            val count = spellSettings.overrideSlots[i.toFloat()] ?: autoSlots.getOrNull(i - 1) ?: 0
            if (count > 0) levels.add(i)
        }
        levels.toList().sorted()
    }

    val remainingSlots = remember(autoSlots, spellSettings) {
        val res = mutableMapOf<Int, Int>()
        for (lvl in 1..9) {
            val level = lvl.toFloat()
            val baseSlots = spellSettings.overrideSlots[level] ?: autoSlots.getOrNull(lvl - 1) ?: 0
            val totalUsed = (spellSettings.usedSlots[level] ?: 0) + (spellSettings.usedSlotsShortRest[level] ?: 0) + (spellSettings.usedSlotsDawn[level] ?: 0)
            res[lvl] = (baseSlots - totalUsed).coerceAtLeast(0)
        }
        res
    }

    val allCharacterSpells = remember(baseSpells, spellSettings, state?.spells) {
        spellSettings.getCharacterSpells(baseSpells, state?.spells ?: emptyList(), state?.spellFieldManager?.items ?: emptyList())
    }

    val (spellAtk, spellAtkDice) = calculateAttackFormulaParts(
        baseFlat = pb + abilityModifier,
        bonuses = spellSettings.spellAttackBonuses,
        stats = statsMap,
        renderInOrder = renderDiceInOrder
    )
    val (spellDc, spellSaveDiceParts) = calculateAttackFormulaParts(
        baseFlat = 8 + pb + abilityModifier,
        bonuses = spellSettings.spellSaveDcBonuses,
        stats = statsMap,
        renderInOrder = renderDiceInOrder
    )

    var draggingItemKey by remember { mutableStateOf<Any?>(null) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .outerShadow(
                shape = RoundedCornerShape(12.dp),
                blur = 4.dp,
                offsetY = 2.dp
            )
            .run {
                if (!isEditMode) {
                    this.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        spellFieldManager?.upsert(spellField.copy(isExpanded = !isExpanded))
                    }
                } else this
            },
        shape = RoundedCornerShape(12.dp),
        color = if (blurCards && hazeState != null) colorScheme.surfaceContainerLow.copy(alpha = 0.5f)
        else colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .clipToBounds()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = title,
                    onValueChange = { newTitle ->
                        title = newTitle
                        spellFieldManager?.upsert(spellField.copy(title = newTitle))
                    },
                    enabled = isEditMode,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(colorScheme.primary),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp, end = 8.dp),
                    decorationBox = { innerTextField ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (title.isEmpty()) {
                                Text(
                                    "Подпись для всего поля",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontSize = 18.sp,
                                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { isEditMode = !isEditMode },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isEditMode) Icons.Default.Done else Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (isEditMode) colorScheme.primary else colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded || isEditMode,
                enter = fadeIn() + expandVertically(clip = true),
                exit = fadeOut() + shrinkVertically(clip = true),
                modifier = Modifier.graphicsLayer { clip = true }
            ) {
                Column {
                    Spacer(modifier = Modifier.height(4.dp))

                    ReorderableColumn(
                        list = spellField.items,
                        onSettle = { from, to ->
                            val updatedList = spellField.items.toMutableList().apply { add(to, removeAt(from)) }
                            spellFieldManager?.upsert(spellField.copy(items = updatedList))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) { idx, item, isDragging ->
                        val itemKey = remember(item) { item.spellId ?: item.divider?.id ?: idx }
                        LaunchedEffect(isDragging) {
                            if (isDragging) draggingItemKey = itemKey
                            else if (draggingItemKey == itemKey) draggingItemKey = null
                        }

                        val abilityForThisItem = remember(spellField.items, idx, spellSettings.spellcastingAbility) {
                            var current = spellSettings.spellcastingAbility
                            for (i in 0..idx) {
                                val itm = if (i < spellField.items.size) spellField.items[i] else null
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
                                if (isEditMode) {
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
                                        val dividerScale by animateFloatAsState(targetValue = if (isDragging) 1.02f else 1f)
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .scale(dividerScale),
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
                                                if (isEditMode) {
                                                    Spacer(Modifier.width(8.dp))
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        IconButton(onClick = { showDividerEditor = item.divider }, modifier = Modifier.size(24.dp)) {
                                                            Icon(Icons.Default.Settings, null, modifier = Modifier.size(16.dp), tint = colorScheme.onSurfaceVariant)
                                                        }
                                                        IconButton(onClick = {
                                                            val updated = spellField.items.toMutableList().apply { removeAt(idx) }
                                                            spellFieldManager?.upsert(spellField.copy(items = updated))
                                                        }, modifier = Modifier.size(24.dp)) {
                                                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp), tint = colorScheme.error.copy(alpha = 0.7f))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else if (item.spellId != null) {
                                        val card = allCharacterSpells.find { it.matchesId(item.spellId) } ?: baseSpells.find { it.matchesId(item.spellId) }
                                        if (card != null) {
                                            val spellAbility = spellSettings.spellAbilityOverrides[card.id] ?: abilityForThisItem
                                            val spellMod = if (spellAbility != Attribute.NONE) {
                                                calculateModifier(statsMap[spellAbility.name.lowercase()] ?: "10")
                                            } else abilityModifier

                                            val localStatsMap = remember(statsMap, spellAbility, spellMod) {
                                                if (spellAbility != Attribute.NONE && spellAbility != spellSettings.spellcastingAbility) {
                                                    statsMap.toMutableMap().apply {
                                                        val modStr = spellMod.toString()
                                                        put("mag_mod", modStr)
                                                        put("[MAG MOD]", modStr)
                                                        put("[МАГ МОД]", modStr)
                                                        put("mag_mod_bonus", modStr)
                                                    }
                                                } else statsMap
                                            }

                                            val (itemAtk, itemAtkDice) = calculateAttackFormulaParts(
                                                baseFlat = pb + spellMod,
                                                bonuses = spellSettings.spellAttackBonuses,
                                                stats = localStatsMap,
                                                renderInOrder = renderDiceInOrder
                                            )
                                            val (itemDc, itemSaveDiceParts) = calculateAttackFormulaParts(
                                                baseFlat = 8 + pb + spellMod,
                                                bonuses = spellSettings.spellSaveDcBonuses,
                                                stats = localStatsMap,
                                                renderInOrder = renderDiceInOrder
                                            )

                                            var expanded by remember { mutableStateOf(false) }

                                            SpellCardItem(
                                                spell = card,
                                                isExpanded = expanded,
                                                onToggleExpand = { expanded = !expanded },
                                                onEdit = {
                                                    if (state != null) {
                                                        state.closeFullscreenDialogs()
                                                        state.editingSpell = card
                                                        state.isSpellEditorOpen = true
                                                    } else {
                                                        editingSpell = card
                                                    }
                                                },
                                                onRollDamage = { formula, titleText, advantage ->
                                                    val isHealing = card.damageTypes.contains(DamageType.HEALING)
                                                    val finalTitle = if (isHealing) titleText.replace("Урон:", "Лечение:").replace("Дополнительный урон:", "Дополнительное лечение:") else titleText
                                                    onRoll(DiceRoller.roll(
                                                        title = finalTitle,
                                                        baseModifier = 0,
                                                        bonuses = listOf(SimpleBonus(formula = formula, name = if (isHealing) "Лечение" else "Урон")),
                                                        isDamage = !isHealing,
                                                        isHealing = isHealing,
                                                        stats = localStatsMap,
                                                        exhaustion = 0,
                                                        sourceType = RollSourceType.OTHER,
                                                        advantageType = advantage,
                                                        advantageLogic = AdvantageLogic.TOTAL
                                                    ))
                                                },
                                                onRollAttack = { advantage ->
                                                    if (card.attackTypes.contains(MagicAttackType.ATTACK)) {
                                                        onRoll(DiceRoller.roll(
                                                            title = "${card.name} (Атака)",
                                                            baseModifier = pb + spellMod,
                                                            bonuses = spellSettings.spellAttackBonuses,
                                                            stats = localStatsMap,
                                                            exhaustion = 0,
                                                            sourceType = RollSourceType.ATTACK,
                                                            advantageType = advantage,
                                                            advantageLogic = AdvantageLogic.TOTAL
                                                        ))
                                                    }
                                                },
                                                isEditable = true,
                                                statsMap = localStatsMap,
                                                characterLevel = characterLevelInt,
                                                spellAttackBonus = itemAtk,
                                                spellAttackDice = itemAtkDice,
                                                spellSaveDc = itemDc,
                                                spellSaveDice = itemSaveDiceParts,
                                                availableSlotLevels = availableSlotLevels,
                                                remainingSlots = remainingSlots,
                                                allowCantripUpcast = spellSettings.allowCantripUpcast,
                                                hazeState = hazeState,
                                                popupHazeState = popupHazeState,
                                                forceBlurEnabled = forceBlurEnabled,
                                                blurCards = blurCards,
                                                highlightRitual = false,
                                                isEditMode = isEditMode,
                                                collapseOnEdit = collapseSpellsOnEdit,
                                                isDragging = isDragging,
                                                isAnyItemDragging = draggingItemKey != null,
                                                isOverridden = card.id in spellSettings.spellOverrides,
                                                settingsViewModel = settingsViewModel
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (isEditMode) {
                        TextButton(
                            onClick = { showDividerEditor = SpellLevelDivider(title = "Разделитель") },
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
                                if (state != null) {
                                    state.closeFullscreenDialogs()
                                    state.editingSpell = SpellCard(level = "1")
                                    state.isSpellEditorOpen = true
                                } else {
                                    editingSpell = SpellCard(level = "1")
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Создать", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                if (state != null) {
                                    state.closeFullscreenDialogs()
                                    state.activeSpellFieldIdForSelection = spellFieldId
                                    state.isSpellbookSelectionOpen = true
                                } else {
                                    showSelectionDialog = true
                                }
                            },
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

    if (showDividerEditor != null) {
        val existing = showDividerEditor!!
        var dividerTitle by remember { mutableStateOf(existing.title) }
        var dividerAbility by remember { mutableStateOf(existing.ability) }

        AlertDialog(
            onDismissRequest = { showDividerEditor = null },
            title = { Text(if (existing.id.isEmpty() || spellField.items.none { it.divider?.id == existing.id }) "Новый разделитель" else "Настройка разделителя") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = dividerTitle,
                        onValueChange = { dividerTitle = it },
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

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            row1.forEach { attr ->
                                FilterChip(
                                    selected = dividerAbility == attr,
                                    onClick = { dividerAbility = if (dividerAbility == attr) Attribute.NONE else attr },
                                    label = { Text(attr.shortName, fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp)) },
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                )
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            row2.forEach { attr ->
                                FilterChip(
                                    selected = dividerAbility == attr,
                                    onClick = { dividerAbility = if (dividerAbility == attr) Attribute.NONE else attr },
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
                    val currentList = spellField.items.toMutableList()
                    val idx = currentList.indexOfFirst { it.divider?.id == existing.id }
                    if (idx != -1) {
                        currentList[idx] = currentList[idx].copy(divider = existing.copy(title = dividerTitle, ability = dividerAbility))
                    } else {
                        currentList.add(SpellLevelItem(divider = SpellLevelDivider(title = dividerTitle, ability = dividerAbility)))
                    }
                    spellFieldManager?.upsert(spellField.copy(items = currentList))
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

    if (showSelectionDialog && spellbookManager != null) {
        val currentIds = spellField.items.mapNotNull { it.spellId }.toSet()
        SpellbookSelectionDialog(
            spellbookManager = spellbookManager,
            selectedIds = currentIds.toList(),
            preparedIds = state?.spellSettings?.preparedSpellIds ?: emptyList(),
            isSpellbookEnabled = false,
            onDismiss = { showSelectionDialog = false },
            onSave = { selected, _ ->
                val newItems = mutableListOf<SpellLevelItem>()
                val existingDividers = spellField.items.filter { it.divider != null }
                newItems.addAll(existingDividers)
                selected.forEach { id ->
                    if (newItems.none { it.spellId == id }) {
                        newItems.add(SpellLevelItem(spellId = id))
                    }
                }
                spellFieldManager?.upsert(spellField.copy(items = newItems))
                if (state != null && state.spellSettings.isSpellbookEnabled) {
                    val curSelected = state.spellSettings.selectedSpellIds
                    val updatedSelected = (curSelected + selected).distinct()
                    state.spellSettings = state.spellSettings.copy(
                        selectedSpellIds = updatedSelected
                    )
                }
            },
            hazeState = hazeState,
            forceBlurEnabled = forceBlurEnabled,
            blurCards = blurCards,
            statsMap = statsMap,
            characterLevel = characterLevelInt,
            spellAttackBonus = spellAtk,
            spellAttackDice = spellAtkDice,
            spellSaveDc = spellDc,
            spellSaveDice = spellSaveDiceParts,
            onRollDamage = { formula, titleText, advantage ->
                onRoll(DiceRoller.roll(
                    title = titleText,
                    baseModifier = 0,
                    bonuses = listOf(SimpleBonus(formula = formula, name = "Урон")),
                    stats = statsMap,
                    exhaustion = 0,
                    sourceType = RollSourceType.OTHER,
                    advantageType = advantage,
                    advantageLogic = AdvantageLogic.TOTAL
                ))
            },
            onRollAttack = { advantage ->
                onRoll(DiceRoller.roll(
                    title = "Атака заклинанием",
                    baseModifier = pb + abilityModifier,
                    bonuses = spellSettings.spellAttackBonuses,
                    stats = statsMap,
                    exhaustion = 0,
                    sourceType = RollSourceType.ATTACK,
                    advantageType = advantage,
                    advantageLogic = AdvantageLogic.TOTAL
                ))
            },
            settingsViewModel = settingsViewModel,
            isDesktop = isDesktop
        )
    }

    if (editingSpell != null) {
        ru.quasaris.characternexus.ui.editors.SpellEditorWindow(
            spell = editingSpell!!,
            onDismiss = { editingSpell = null },
            onSave = { updated ->
                val saved = spellbookManager?.addOrUpdateSpell(updated) ?: updated
                val newItems = spellField.items + SpellLevelItem(spellId = saved.id)
                spellFieldManager?.upsert(spellField.copy(items = newItems))
                if (state != null && state.spellSettings.isSpellbookEnabled) {
                    val curSelected = state.spellSettings.selectedSpellIds
                    if (!curSelected.any { saved.matchesId(it) }) {
                        state.spellSettings = state.spellSettings.copy(
                            selectedSpellIds = curSelected + saved.id
                        )
                    }
                }
                editingSpell = null
            },
            onDelete = { deletedSpell ->
                spellbookManager?.deleteSpell(deletedSpell.id)
                val newItems = spellField.items.filterNot { it.spellId == deletedSpell.id }
                spellFieldManager?.upsert(spellField.copy(items = newItems))
                editingSpell = null
            },
            forceBlurEnabled = forceBlurEnabled,
            settingsViewModel = settingsViewModel,
            isDesktop = isDesktop
        )
    }
}
