package ru.quasaris.characters.master.tabs.spells

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
import ru.quasaris.characters.master.SpellCard
import ru.quasaris.characters.master.SpellFilterState
import ru.quasaris.characters.master.SpellComponentFilter
import ru.quasaris.characters.master.CharacterClass
import ru.quasaris.characters.master.SpellSchool
import ru.quasaris.characters.master.SpellVersion
import ru.quasaris.characters.master.CastingTimeType
import ru.quasaris.characters.master.DurationUnit
import ru.quasaris.characters.master.MagicAttackType
import ru.quasaris.characters.master.Attribute
import ru.quasaris.characters.master.MaterialComponentType
import ru.quasaris.characters.master.backend.SpellbookManager
import ru.quasaris.characters.master.backend.DicePart
import ru.quasaris.characters.master.backend.AdvantageType
import ru.quasaris.characters.master.tabs.spells.SpellFiltersArea
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
    onRollAttack: (AdvantageType) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var currentSelected by remember { mutableStateOf(selectedIds.toSet()) }
    var currentPrepared by remember { mutableStateOf(preparedIds.toSet()) }
    var isBookMode by remember { mutableStateOf(false) }
    
    var expandedIds by remember { mutableStateOf(setOf<String>()) }
    var showFilters by remember { mutableStateOf(false) }
    var filterState by remember { mutableStateOf(SpellFilterState()) }
    
    val allSpells = remember { spellbookManager.loadSpells() }
    val filteredSpells = remember(allSpells, searchQuery, filterState, isBookMode, currentSelected) {
        allSpells.filter { spell ->
            val matchesSearch = spell.matches(filterState, searchQuery)
            if (isBookMode) matchesSearch && spell.id in currentSelected else matchesSearch
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val colorScheme = MaterialTheme.colorScheme
        val isOled = colorScheme.background == Color.Black

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSpellbookEnabled) {
                                IconButton(
                                    onClick = { isBookMode = !isBookMode },
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
                        IconButton(onClick = { showFilters = !showFilters }) {
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
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (forceBlurEnabled && !isOled) Color.Transparent else colorScheme.surface
                    )
                )
            },
            containerColor = if (forceBlurEnabled && !isOled) Color.Transparent else colorScheme.background,
            modifier = Modifier.run {
                if (forceBlurEnabled && hazeState != null && !isOled) {
                    hazeEffect(state = hazeState) {
                        style = HazeStyle(blurRadius = 24.dp, tints = listOf(HazeTint(colorScheme.surface.copy(alpha = 0.1f))))
                        inputScale = HazeInputScale.Fixed(0.7f)
                    }
                } else this
            }
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                SpellFiltersArea(
                    visible = showFilters,
                    filterState = filterState,
                    onFilterChange = { filterState = it }
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
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
                            currentSelected = currentSelected + filteredSpells.map { it.id }
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
                            currentSelected = newSelected
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

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val grouped = filteredSpells.groupBy { it.level }
                    grouped.keys.sortedBy { it.toIntOrNull() ?: Int.MAX_VALUE }.forEach { levelStr ->
                        item {
                            Text(
                                text = if (levelStr == "0") "ЗАГОВОРЫ" else "$levelStr УРОВЕНЬ",
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        items(grouped[levelStr] ?: emptyList(), key = { it.id }) { spell ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val isChecked = if (isBookMode) spell.id in currentPrepared else spell.id in currentSelected
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        if (isBookMode) {
                                            currentPrepared = if (checked) currentPrepared + spell.id else currentPrepared - spell.id
                                        } else {
                                            currentSelected = if (checked) currentSelected + spell.id else currentSelected - spell.id
                                            // If adding to book and not in spellbook mode, also add to prepared
                                            if (!isSpellbookEnabled) {
                                                currentPrepared = currentSelected
                                            }
                                        }
                                    }
                                )
                                Spacer(Modifier.width(4.dp))
                                SpellCardItem(
                                    spell = spell,
                                    isExpanded = spell.id in expandedIds,
                                    onToggleExpand = {
                                        expandedIds = if (spell.id in expandedIds) expandedIds - spell.id else expandedIds + spell.id
                                    },
                                    modifier = Modifier.weight(1f),
                                    isSelected = isChecked,
                                    onLongClick = {
                                        val checked = !isChecked
                                        if (isBookMode) {
                                            currentPrepared = if (checked) currentPrepared + spell.id else currentPrepared - spell.id
                                        } else {
                                            currentSelected = if (checked) currentSelected + spell.id else currentSelected - spell.id
                                            if (!isSpellbookEnabled) {
                                                currentPrepared = currentSelected
                                            }
                                        }
                                    },
                                    isEditable = false,
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
                            }
                        }
                    }
                }

                Button(
                    onClick = { 
                        val finalPrepared = if (isSpellbookEnabled) currentPrepared else currentSelected
                        onSave(currentSelected.toList(), finalPrepared.toList())
                        onDismiss() 
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    val count = if (isBookMode) currentPrepared.size else currentSelected.size
                    Text("Выбрать ($count)", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
