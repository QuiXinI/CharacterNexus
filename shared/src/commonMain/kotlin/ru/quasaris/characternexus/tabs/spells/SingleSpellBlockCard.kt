package ru.quasaris.characternexus.tabs.spells

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import ru.quasaris.characternexus.backend.*
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.tabs.NotionBlockEditor
import ru.quasaris.characternexus.tabs.attacks.calculateAttackFormulaParts
import ru.quasaris.characternexus.ui.CharacterDetailState
import ru.quasaris.characternexus.ui.editors.SpellCardSectionTitle
import ru.quasaris.characternexus.ui.editors.SpellConfigContent
import ru.quasaris.characternexus.ui.outerShadow
import ru.quasaris.characternexus.util.generateUuid

@Composable
fun SingleSpellBlockCard(
    block: DynamicContentBlock.SpellRef,
    onUpdateBlock: (DynamicContentBlock.SpellRef) -> Unit,
    onDeleteBlock: () -> Unit = {},
    state: CharacterDetailState?,
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

    val allCharacterSpells: List<SpellCard> = remember(baseSpells, spellSettings) {
        spellSettings.getCharacterSpells(baseSpells)
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

    val resolvedSpell: SpellCard? = remember(block.spellId, allCharacterSpells, baseSpells, spellSettings.spellOverrides) {
        block.spellId?.let { id ->
            spellSettings.spellOverrides[id]
                ?: allCharacterSpells.find { it.matchesId(id) }
                ?: baseSpells.find { it.matchesId(id) }
        }
    }

    if (resolvedSpell == null) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .outerShadow(
                    shape = RoundedCornerShape(12.dp),
                    blur = 4.dp,
                    offsetY = 2.dp
                ),
            shape = RoundedCornerShape(12.dp),
            color = if (blurCards && hazeState != null) colorScheme.surfaceContainerLow.copy(alpha = 0.5f)
            else colorScheme.surfaceContainerLow,
            tonalElevation = 1.dp,
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    "Заклинание не выбрано",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            state?.openSingleSpellDialog(block, initialTab = 0)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.LibraryBooks, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Выбрать", fontSize = 13.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            state?.openSingleSpellDialog(block, initialTab = 1)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Добавить", fontSize = 13.sp)
                    }
                }
            }
        }
    } else {
        var expanded by remember { mutableStateOf(false) }

        SpellCardItem(
            spell = resolvedSpell,
            isExpanded = expanded,
            onToggleExpand = { expanded = !expanded },
            onEdit = {
                state?.openSingleSpellDialog(block, initialTab = 1)
            },
            onRollDamage = { formula, titleText, advantage ->
                val isHealing = resolvedSpell.damageTypes.contains(DamageType.HEALING)
                val finalTitle = if (isHealing) titleText.replace("Урон:", "Лечение:").replace("Дополнительный урон:", "Дополнительное лечение:") else titleText
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
                    advantageLogic = AdvantageLogic.TOTAL
                ))
            },
            onRollAttack = { advantage ->
                if (resolvedSpell.attackTypes.contains(MagicAttackType.ATTACK)) {
                    onRoll(DiceRoller.roll(
                        title = "${resolvedSpell.name} (Атака)",
                        baseModifier = pb + abilityModifier,
                        bonuses = spellSettings.spellAttackBonuses,
                        stats = statsMap,
                        exhaustion = 0,
                        sourceType = RollSourceType.ATTACK,
                        advantageType = advantage,
                        advantageLogic = AdvantageLogic.TOTAL
                    ))
                }
            },
            isEditable = true,
            statsMap = statsMap,
            characterLevel = characterLevelInt,
            spellAttackBonus = spellAtk,
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
            highlightRitual = false,
            isEditMode = false,
            collapseOnEdit = collapseSpellsOnEdit,
            isDragging = false,
            isAnyItemDragging = false,
            isOverridden = resolvedSpell.id in spellSettings.spellOverrides,
            settingsViewModel = settingsViewModel
        )
    }
}

@Composable
fun SpellSelectionTabContent(
    spellbookManager: SpellbookManager,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    currentSelected: Set<String>,
    onCurrentSelectedChange: (Set<String>) -> Unit,
    currentPrepared: Set<String>,
    onCurrentPreparedChange: (Set<String>) -> Unit,
    isBookMode: Boolean,
    onIsBookModeChange: (Boolean) -> Unit,
    expandedIds: Set<String>,
    onExpandedIdsChange: (Set<String>) -> Unit,
    showFilters: Boolean,
    onShowFiltersChange: (Boolean) -> Unit,
    filterState: SpellFilterState,
    onFilterStateChange: (SpellFilterState) -> Unit,
    isSpellbookEnabled: Boolean,
    statsMap: Map<String, String>,
    characterLevel: Int,
    spellOverrides: Map<String, SpellCard>,
    settingsViewModel: SettingsViewModel?,
    onRollDamage: (String, String, AdvantageType) -> Unit,
    onRollAttack: (AdvantageType) -> Unit,
    onDismiss: () -> Unit
) {
    val allSpells = remember(spellOverrides) {
        val base = spellbookManager.loadSpells()
        SpellSettings(spellOverrides = spellOverrides).getAvailableSpells(base)
    }

    fun matchesAny(spell: SpellCard, idSet: Set<String>): Boolean {
        return idSet.any { spell.matchesId(it) }
    }

    val filteredSpells = remember(allSpells, searchQuery, filterState, isBookMode, currentSelected) {
        allSpells.filter { spell ->
            val matchesSearch = spell.matches(filterState, searchQuery)
            if (isBookMode) matchesSearch && matchesAny(spell, currentSelected) else matchesSearch
        }
    }

    val colorScheme = MaterialTheme.colorScheme

    Column(modifier = Modifier.fillMaxSize()) {
        SpellFiltersArea(
            visible = showFilters,
            filterState = filterState,
            onFilterChange = onFilterStateChange
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            placeholder = { Text("Поиск...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    onCurrentSelectedChange(currentSelected + filteredSpells.map { it.id })
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Default.SelectAll, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Выбрать все", fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = {
                    val filteredIds = filteredSpells.map { it.id }.toSet()
                    val newSelected = currentSelected.toMutableSet()
                    filteredIds.forEach { id ->
                        if (id in newSelected) newSelected.remove(id) else newSelected.add(id)
                    }
                    onCurrentSelectedChange(newSelected)
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Default.Flip, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Инвертировать", fontSize = 12.sp)
            }
        }

        SpellListGrid(
            settingsViewModel = settingsViewModel,
            spells = filteredSpells,
            filterState = filterState,
            expandedIds = expandedIds,
            onToggleExpand = { id ->
                onExpandedIdsChange(if (id in expandedIds) expandedIds - id else expandedIds + id)
            },
            modifier = Modifier.weight(1f),
            selectedIds = if (isBookMode) currentPrepared else currentSelected,
            onToggleSelect = { id, checked ->
                if (isBookMode) {
                    onCurrentPreparedChange(if (checked) currentPrepared + id else currentPrepared - id)
                } else {
                    onCurrentSelectedChange(if (checked) currentSelected + id else currentSelected - id)
                    if (!isSpellbookEnabled) {
                        onCurrentPreparedChange(if (checked) currentSelected + id else currentSelected - id)
                    }
                }
            },
            statsMap = statsMap,
            characterLevel = characterLevel,
            spellAttackBonus = 0,
            spellAttackDice = emptyList(),
            spellSaveDc = 0,
            spellSaveDice = emptyList(),
            onRollDamage = onRollDamage,
            onRollAttack = onRollAttack,
            blurCards = true,
            spellOverrides = spellOverrides
        )

        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorScheme.surfaceVariant,
                contentColor = colorScheme.onSurfaceVariant
            )
        ) {
            val count = if (isBookMode) currentPrepared.size else currentSelected.size
            Text("Закрыть ($count)", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SpellEditorTabContent(
    spell: SpellCard,
    state: SpellCard,
    onStateChange: (SpellCard) -> Unit,
    onDismiss: () -> Unit,
    forceBlurEnabled: Boolean,
    hazeState: HazeState?,
    settingsViewModel: SettingsViewModel?
) {
    val colorScheme = MaterialTheme.colorScheme
    var englishNameError by remember { mutableStateOf<String?>(null) }
    val allowedCharsRegex = remember { Regex("^[a-zA-Z0-9'\\-._,() ]*$") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = state.name,
            onValueChange = { onStateChange(state.copy(name = it)) },
            label = { Text("Название") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Английское название",
                modifier = Modifier.weight(1f),
                color = colorScheme.onSurface
            )
            Switch(
                checked = state.showEnglishName,
                onCheckedChange = { onStateChange(state.copy(showEnglishName = it)) })
        }

        OutlinedTextField(
            value = state.englishName,
            onValueChange = { newValue ->
                if (newValue.all { it.toString().matches(allowedCharsRegex) }) {
                    onStateChange(state.copy(englishName = newValue))
                    englishNameError = null
                } else {
                    englishNameError = "Разрешены только латиница, цифры и знаки ' - . _ , ( )"
                }
            },
            label = { Text("English Name") },
            isError = englishNameError != null,
            supportingText = {
                if (englishNameError != null) {
                    Text(englishNameError!!, color = MaterialTheme.colorScheme.error)
                }
            },
            modifier = Modifier.fillMaxWidth().alpha(if (state.showEnglishName) 1f else 0.7f),
            shape = RoundedCornerShape(8.dp)
        )

        // Level
        OutlinedTextField(
            value = state.level,
            onValueChange = { onStateChange(state.copy(level = it)) },
            label = { Text("Уровень (0 - заговор)") },
            modifier = Modifier.fillMaxWidth().alpha(if (state.level.isBlank()) 0.7f else 1f),
            shape = RoundedCornerShape(8.dp)
        )

        // Version
        Column {
            SpellCardSectionTitle("ВЕРСИЯ")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SpellVersion.entries.forEachIndexed { index, version ->
                    SegmentedButton(
                        selected = state.version == version,
                        onClick = { onStateChange(state.copy(version = version)) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = SpellVersion.entries.size
                        )
                    ) {
                        Text(version.displayName)
                    }
                }
            }
        }

        // School
        val schoolActive = state.school != SpellSchool.NONE
        Column(modifier = Modifier.alpha(if (schoolActive) 1f else 0.7f)) {
            SpellCardSectionTitle("ШКОЛА")
            var schoolExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = state.school.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Школа") },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    shape = RoundedCornerShape(8.dp)
                )
                Box(modifier = Modifier.matchParentSize().clickable { schoolExpanded = true })
                DropdownMenu(
                    expanded = schoolExpanded,
                    onDismissRequest = { schoolExpanded = false }) {
                    SpellSchool.entries.forEach { school ->
                        DropdownMenuItem(
                            text = { Text(school.displayName) },
                            onClick = {
                                onStateChange(state.copy(school = school))
                                schoolExpanded = false
                            })
                    }
                }
            }
        }

        // Description
        val spellDescState = remember(state.name, state.description) {
            DynamicNoteState(id = "spell_desc_${state.name}", content = state.description)
        }
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("Описание", style = MaterialTheme.typography.labelMedium, color = colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = colorScheme.surfaceVariant.copy(alpha = 0.3f),
                border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(12.dp)) {
                    NotionBlockEditor(
                        field = spellDescState,
                        onFieldChange = { updated: DynamicNoteState -> onStateChange(state.copy(description = updated.content)) },
                        canEdit = true,
                        isReorderMode = false,
                        contentPlaceholder = "Описание заклинания...",
                        isBasicMode = true
                    )
                }
            }
        }

        // Save Button at bottom
        Button(
            onClick = onDismiss,
            enabled = state.name.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorScheme.surfaceVariant,
                contentColor = colorScheme.onSurfaceVariant
            )
        ) {
            Text("Сохранить и закрыть", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(64.dp))
    }
}
