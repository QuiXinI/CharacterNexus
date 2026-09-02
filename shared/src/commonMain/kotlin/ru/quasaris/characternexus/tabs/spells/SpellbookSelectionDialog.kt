package ru.quasaris.characternexus.tabs.spells

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.ui.BackHandler
import ru.quasaris.characternexus.backend.SpellbookManager
import ru.quasaris.characternexus.backend.DicePart
import ru.quasaris.characternexus.tabs.spells.SpellFiltersArea
import ru.quasaris.characternexus.ui.DialogDimStyle
import ru.quasaris.characternexus.ui.theme.rememberEffectiveBlurRadius
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.HazeInputScale

@OptIn(ExperimentalMaterial3Api::class, dev.chrisbanes.haze.ExperimentalHazeApi::class)
@Composable
fun SpellbookSelectionDialog(
    spellbookManager: SpellbookManager,
    selectedIds: List<String>,
    preparedIds: List<String> = emptyList(),
    isSpellbookEnabled: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (List<String>, List<String>) -> Unit,
    hazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false,
    blurCards: Boolean = true,
    statsMap: Map<String, String> = emptyMap(),
    characterLevel: Int = 1,
    spellAttackBonus: Int = 0,
    spellAttackDice: List<DicePart> = emptyList(),
    spellSaveDc: Int = 0,
    spellSaveDice: List<DicePart> = emptyList(),
    onRollDamage: (String, String, AdvantageType) -> Unit = { _, _, _ -> },
    onRollAttack: (AdvantageType) -> Unit = {},
    settingsViewModel: ru.quasaris.characternexus.backend.SettingsViewModel? = null,
    isDesktop: Boolean = false
) {
    var searchQuery by remember { mutableStateOf("") }
    var currentSelected by remember { mutableStateOf(selectedIds.toSet()) }
    var currentPrepared by remember { mutableStateOf(preparedIds.toSet()) }
    var isBookMode by remember { mutableStateOf(false) }

    var expandedIds by remember { mutableStateOf(setOf<String>()) }
    var showFilters by remember { mutableStateOf(false) }
    var filterState by remember { mutableStateOf(SpellFilterState()) }

    val blurRadius = rememberEffectiveBlurRadius(settingsViewModel)

    val handleDismiss = {
        onDismiss()
    }

    val handleSave = {
        val finalPrepared = if (isSpellbookEnabled) currentPrepared else currentSelected
        onSave(currentSelected.toList(), finalPrepared.toList())
        onDismiss()
    }

    if (isDesktop) {
        SpellbookSelectionContent(
            spellbookManager = spellbookManager,
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            currentSelected = currentSelected,
            onCurrentSelectedChange = { currentSelected = it },
            currentPrepared = currentPrepared,
            onCurrentPreparedChange = { currentPrepared = it },
            isBookMode = isBookMode,
            onIsBookModeChange = { isBookMode = it },
            expandedIds = expandedIds,
            onExpandedIdsChange = { expandedIds = it },
            showFilters = showFilters,
            onShowFiltersChange = { showFilters = it },
            filterState = filterState,
            onFilterStateChange = { filterState = it },
            isSpellbookEnabled = isSpellbookEnabled,
            onDismiss = handleDismiss,
            onSave = handleSave,
            hazeState = hazeState,
            forceBlurEnabled = forceBlurEnabled,
            blurCards = blurCards,
            statsMap = statsMap,
            characterLevel = characterLevel,
            spellAttackBonus = spellAttackBonus,
            spellAttackDice = spellAttackDice,
            spellSaveDc = spellSaveDc,
            spellSaveDice = spellSaveDice,
            onRollDamage = onRollDamage,
            onRollAttack = onRollAttack,
            settingsViewModel = settingsViewModel,
            blurRadius = blurRadius
        )
    } else {
        Dialog(
            onDismissRequest = handleDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            DialogDimStyle(0f)
            SpellbookSelectionContent(
                spellbookManager = spellbookManager,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                currentSelected = currentSelected,
                onCurrentSelectedChange = { currentSelected = it },
                currentPrepared = currentPrepared,
                onCurrentPreparedChange = { currentPrepared = it },
                isBookMode = isBookMode,
                onIsBookModeChange = { isBookMode = it },
                expandedIds = expandedIds,
                onExpandedIdsChange = { expandedIds = it },
                showFilters = showFilters,
                onShowFiltersChange = { showFilters = it },
                filterState = filterState,
                onFilterStateChange = { filterState = it },
                isSpellbookEnabled = isSpellbookEnabled,
                onDismiss = handleDismiss,
                onSave = handleSave,
                hazeState = hazeState,
                forceBlurEnabled = forceBlurEnabled,
                blurCards = blurCards,
                statsMap = statsMap,
                characterLevel = characterLevel,
                spellAttackBonus = spellAttackBonus,
                spellAttackDice = spellAttackDice,
                spellSaveDc = spellSaveDc,
                spellSaveDice = spellSaveDice,
                onRollDamage = onRollDamage,
                onRollAttack = onRollAttack,
                settingsViewModel = settingsViewModel,
                blurRadius = blurRadius
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpellbookSelectionContent(
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
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    hazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false,
    blurCards: Boolean = true,
    statsMap: Map<String, String> = emptyMap(),
    characterLevel: Int = 1,
    spellAttackBonus: Int = 0,
    spellAttackDice: List<DicePart> = emptyList(),
    spellSaveDc: Int = 0,
    spellSaveDice: List<DicePart> = emptyList(),
    onRollDamage: (String, String, AdvantageType) -> Unit = { _, _, _ -> },
    onRollAttack: (AdvantageType) -> Unit = {},
    settingsViewModel: ru.quasaris.characternexus.backend.SettingsViewModel? = null,
    blurRadius: androidx.compose.ui.unit.Dp = 24.dp
) {
    val allSpells = remember { spellbookManager.loadSpells() }
    val filteredSpells = remember(allSpells, searchQuery, filterState, isBookMode, currentSelected) {
        allSpells.filter { spell ->
            val matchesSearch = spell.matches(filterState, searchQuery)
            if (isBookMode) matchesSearch && spell.id in currentSelected else matchesSearch
        }
    }

    BackHandler(onBack = onDismiss)
    val colorScheme = MaterialTheme.colorScheme
    val isOled = colorScheme.background == Color.Black

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isSpellbookEnabled) {
                            IconButton(
                                onClick = { onIsBookModeChange(!isBookMode) },
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Icon(
                                    if (isBookMode) Icons.Default.MenuBook else Icons.Default.Public,
                                    contentDescription = "Режим книги",
                                    tint = if (isBookMode) colorScheme.primary else colorScheme.onSurface
                                )
                            }
                        }
                        Text(if (isBookMode) "Книга заклинаний" else "Список заклинаний", fontWeight = FontWeight.Black)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                    }
                },
                actions = {
                    IconButton(onClick = { onFilterStateChange(filterState.copy(isCompact = !filterState.isCompact)) }) {
                        Icon(
                            if (filterState.isCompact) Icons.Default.ViewHeadline else Icons.Default.ViewModule,
                            contentDescription = "Компактный режим",
                            tint = if (filterState.isCompact) colorScheme.primary else colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { onShowFiltersChange(!showFilters) }) {
                        Icon(Icons.Default.FilterList, null, tint = if (showFilters) colorScheme.primary else colorScheme.onSurface)
                    }
                    val targetSet = if (isSpellbookEnabled) currentPrepared else currentSelected
                    val counts = (0..9).map { level ->
                        allSpells.filter { it.id in targetSet && it.level == level.toString() }.size
                    }
                    Text(
                        text = counts.joinToString("/") { it.toString() },
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = if (forceBlurEnabled && hazeState != null && !isOled) Color.Transparent else colorScheme.surface
                )
            )
        },
        containerColor = if (forceBlurEnabled && hazeState != null && !isOled) Color.Transparent else colorScheme.background,
        modifier = Modifier
            .fillMaxSize()
            .run {
                if (forceBlurEnabled && hazeState != null && !isOled) {
                    this.hazeEffect(state = hazeState) {
                        style = HazeStyle(
                            blurRadius = blurRadius,
                            tints = listOf(HazeTint(Color.Black.copy(alpha = 0.2f)))
                        )
                    }
                } else this
            }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
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

            SpellListGrid(settingsViewModel = settingsViewModel,
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
                spellAttackBonus = spellAttackBonus,
                spellAttackDice = spellAttackDice,
                spellSaveDc = spellSaveDc,
                spellSaveDice = spellSaveDice,
                onRollDamage = onRollDamage,
                onRollAttack = onRollAttack,
                hazeState = hazeState,
                forceBlurEnabled = forceBlurEnabled,
                blurCards = blurCards
            )

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                val count = if (isBookMode) currentPrepared.size else currentSelected.size
                Text("Выбрать ($count)", fontWeight = FontWeight.Bold)
            }
        }
    }
}
