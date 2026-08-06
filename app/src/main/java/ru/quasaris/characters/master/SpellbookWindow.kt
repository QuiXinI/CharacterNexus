package ru.quasaris.characters.master

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.quasaris.characters.master.backend.SpellbookManager
import ru.quasaris.characters.master.tabs.spells.SpellCardItem
import ru.quasaris.characters.master.tabs.spells.SpellCardEditorDialog
import ru.quasaris.characters.master.tabs.spells.SpellFiltersArea
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.HazeInputScale
import kotlinx.coroutines.launch
import ru.quasaris.characters.master.ui.DeleteConfirmationDialog

@OptIn(ExperimentalMaterial3Api::class, dev.chrisbanes.haze.ExperimentalHazeApi::class)
@Composable
fun SpellbookWindow(
    spellbookManager: SpellbookManager,
    onOpenDrawer: () -> Unit,
    hazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false,
    settingsViewModel: ru.quasaris.characters.master.backend.SettingsViewModel? = null,
) {
    var searchQuery by remember { mutableStateOf("") }
    
    val scope = rememberCoroutineScope()
    
    var editingSpell by remember { mutableStateOf<SpellCard?>(null) }
    var expandedSpellId by remember { mutableStateOf<String?>(null) }
    var selectedSpellIds by remember { mutableStateOf(setOf<String>()) }
    var isSelectionMode by remember { mutableStateOf(false) }

    // Filters
    var showFilters by remember { mutableStateOf(false) }
    var filterState by remember { mutableStateOf(SpellFilterState()) }

    // Progress Dialog
    var importProgress by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var errorDialogData by remember { mutableStateOf<Triple<String, String, (SpellbookManager.ImportAction) -> Unit>?>(null) }
    var showDeleteAllConfirm by remember { mutableStateOf(false) }

    // A trigger to refresh the list when spells are updated
    var refreshTrigger by remember { mutableIntStateOf(0) }
    
    val allSpells = remember(refreshTrigger) { spellbookManager.loadSpells() }
    val filteredSpells = remember(allSpells, searchQuery, filterState) {
        allSpells.filter { spell ->
            val matchesSearch = searchQuery.isBlank() || 
                spell.name.contains(searchQuery, ignoreCase = true) || 
                (spell.showEnglishName && spell.englishName.contains(searchQuery, ignoreCase = true))
            
            val matchesLevel = filterState.levels.isEmpty() || spell.level in filterState.levels
            val matchesClass = filterState.classes.isEmpty() || spell.classes.any { it in filterState.classes }
            val matchesSchool = filterState.schools.isEmpty() || spell.school in filterState.schools
            val matchesVersion = filterState.versions.isEmpty() || spell.version in filterState.versions
            val matchesCastingTime = filterState.castingTimeTypes.isEmpty() || spell.castingTimeType in filterState.castingTimeTypes
            val matchesDuration = filterState.durationUnits.isEmpty() || spell.durationUnit in filterState.durationUnits
            val matchesAttackType = filterState.attackTypes.isEmpty() || spell.attackType in filterState.attackTypes
            val matchesSaveAttr = filterState.savingThrowAttributes.isEmpty() || spell.savingThrowAttributes.any { it in filterState.savingThrowAttributes }
            
            val matchesConc = filterState.hasConcentration == null || spell.hasConcentration == filterState.hasConcentration
            val matchesRitual = filterState.isRitual == null || spell.isRitual == filterState.isRitual
            val matchesCircle = filterState.isCircle == null || spell.isCircle == filterState.isCircle
            val matchesDamage = filterState.hasDamage == null || spell.hasDamage == filterState.hasDamage
            
            val matchesAttackOrSave = when(filterState.attackOrSave) {
                MagicAttackType.ATTACK -> spell.attackType == MagicAttackType.ATTACK
                MagicAttackType.SAVE -> spell.attackType == MagicAttackType.SAVE
                null -> true
            }

            val matchesComponents = if (filterState.components.isEmpty()) true else {
                filterState.components.all { component ->
                    when(component) {
                        SpellComponentFilter.VERBAL -> spell.hasVerbalComponent
                        SpellComponentFilter.SOMATIC -> spell.hasSomaticComponent
                        SpellComponentFilter.MATERIAL -> spell.materialComponentType != MaterialComponentType.NONE
                        SpellComponentFilter.MATERIAL_COST -> spell.materialComponents.contains("gp", ignoreCase = true) || spell.materialComponents.contains(" зм", ignoreCase = true)
                        SpellComponentFilter.MATERIAL_CONSUMED -> spell.materialComponents.contains("consume", ignoreCase = true) || spell.materialComponents.contains("расходует", ignoreCase = true)
                        SpellComponentFilter.NO_VERBAL -> !spell.hasVerbalComponent
                        SpellComponentFilter.NO_SOMATIC -> !spell.hasSomaticComponent
                        SpellComponentFilter.NO_MATERIAL -> spell.materialComponentType == MaterialComponentType.NONE
                        SpellComponentFilter.NO_MATERIAL_COST -> !(spell.materialComponents.contains("gp", ignoreCase = true) || spell.materialComponents.contains(" зм", ignoreCase = true))
                        SpellComponentFilter.NO_MATERIAL_CONSUMED -> !(spell.materialComponents.contains("consume", ignoreCase = true) || spell.materialComponents.contains("расходует", ignoreCase = true))
                    }
                }
            }

            val matchesCastingTimeQuery = filterState.castingTimeQuery.isBlank() || spell.castingTime.contains(filterState.castingTimeQuery, ignoreCase = true)
            val matchesDurationQuery = filterState.durationQuery.isBlank() || spell.durationValue == filterState.durationQuery

            matchesSearch && matchesLevel && matchesClass && matchesSchool && matchesVersion && 
            matchesCastingTime && matchesDuration && matchesAttackType && matchesSaveAttr &&
            matchesConc && matchesRitual && matchesCircle && matchesDamage && matchesComponents &&
            matchesAttackOrSave && matchesCastingTimeQuery && matchesDurationQuery
        }.sortedBy { it.level }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                spellbookManager.importSpells(
                    uri = it,
                    onProgress = { cur, total -> importProgress = cur to total },
                    onError = { name, reason, callback -> errorDialogData = Triple(name, reason, callback) }
                )
                importProgress = null
                refreshTrigger++
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let {
            scope.launch {
                if (selectedSpellIds.isNotEmpty()) {
                    spellbookManager.exportSpellbook(it, selectedSpellIds.toList())
                } else {
                    spellbookManager.exportSpellbook(it)
                }
            }
        }
    }

    val singleExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            editingSpell?.let { spell ->
                scope.launch {
                    spellbookManager.exportSingleSpell(it, spell)
                }
            }
        }
    }

    val colorScheme = MaterialTheme.colorScheme
    val isOled = colorScheme.background == Color.Black

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Книга заклинаний", fontWeight = FontWeight.Black) },
                    navigationIcon = {
                        if (isSelectionMode) {
                            IconButton(onClick = { isSelectionMode = false; selectedSpellIds = emptySet() }) {
                                Icon(Icons.Default.Close, null)
                            }
                        } else {
                            IconButton(onClick = onOpenDrawer) {
                                Icon(Icons.Default.Menu, contentDescription = "Меню")
                            }
                        }
                    },
                    actions = {
                        if (isSelectionMode) {
                            IconButton(onClick = {
                                selectedSpellIds = selectedSpellIds + filteredSpells.map { it.id }
                            }) {
                                Icon(Icons.Default.SelectAll, contentDescription = "Выбрать все")
                            }
                            IconButton(onClick = {
                                val filteredIds = filteredSpells.map { it.id }.toSet()
                                val newSelected = selectedSpellIds.toMutableSet()
                                filteredIds.forEach { id ->
                                    if (id in newSelected) newSelected.remove(id) else newSelected.add(id)
                                }
                                selectedSpellIds = newSelected
                            }) {
                                Icon(Icons.Default.Flip, contentDescription = "Инвертировать выделение")
                            }
                            IconButton(onClick = { exportLauncher.launch("selected_spells.spellbook") }) {
                                Icon(Icons.Default.FileUpload, contentDescription = "Экспорт выбранных")
                            }
                        } else {
                            IconButton(onClick = { showFilters = !showFilters }) {
                                Icon(Icons.Default.FilterList, null, tint = if (showFilters) colorScheme.primary else colorScheme.onSurface)
                            }
                            IconButton(onClick = { importLauncher.launch("*/*") }) {
                                Icon(Icons.Default.FileDownload, contentDescription = "Импорт")
                            }
                            IconButton(onClick = { exportLauncher.launch("spellbook.spellbook") }) {
                                Icon(Icons.Default.FileUpload, contentDescription = "Экспорт")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (forceBlurEnabled && !isOled) Color.Transparent else colorScheme.surface
                    )
                )
            },
            floatingActionButton = {
                if (!isSelectionMode) {
                    FloatingActionButton(onClick = { editingSpell = SpellCard() }) {
                        Icon(Icons.Default.Add, contentDescription = "Добавить")
                    }
                }
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
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Поиск (RU/EN)...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = { if (searchQuery.isNotEmpty()) IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, null) } },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val grouped = filteredSpells.groupBy { 
                        val l = it.level.trim()
                        if (l == "0") "ЗАГОВОРЫ"
                        else if (l.toIntOrNull() != null) "$l УРОВЕНЬ"
                        else "ПРОЧЕЕ"
                    }
                    
                    grouped.forEach { (title, spells) ->
                        item {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium,
                                color = colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }
                        items(spells, key = { it.id }) { spell ->
                            SpellCardItem(
                                spell = spell,
                                isExpanded = expandedSpellId == spell.id,
                                onToggleExpand = { 
                                    if (isSelectionMode) {
                                        val newSelection = if (spell.id in selectedSpellIds) selectedSpellIds - spell.id else selectedSpellIds + spell.id
                                        selectedSpellIds = newSelection
                                        if (newSelection.isEmpty()) {
                                            isSelectionMode = false
                                        }
                                    } else {
                                        expandedSpellId = if (expandedSpellId == spell.id) null else spell.id 
                                    }
                                },
                                onEdit = { editingSpell = spell },
                                isEditable = true,
                                isSelected = spell.id in selectedSpellIds,
                                onLongClick = {
                                    if (!isSelectionMode) {
                                        isSelectionMode = true
                                        selectedSpellIds = setOf(spell.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isSelectionMode && selectedSpellIds.isNotEmpty(),
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp)
                .padding(bottom = 8.dp)
        ) {
            Button(
                onClick = { showDeleteAllConfirm = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .shadow(6.dp, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить")
                Spacer(Modifier.width(8.dp))
                Text("Удалить выбранные (${selectedSpellIds.size})", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
    }

    editingSpell?.let { spell ->
        SpellCardEditorDialog(
            spell = spell,
            onDismiss = { editingSpell = null },
            onSave = {
                spellbookManager.addOrUpdateSpell(it)
                editingSpell = null
                refreshTrigger++
            },
            onDelete = {
                spellbookManager.deleteSpell(it.id)
                editingSpell = null
                refreshTrigger++
            },
            onExport = {
                singleExportLauncher.launch("${it.name.ifBlank { "spell" }}.json")
            },
            hazeState = hazeState,
            forceBlurEnabled = forceBlurEnabled,
            settingsViewModel = settingsViewModel
        )
    }

    importProgress?.let { (cur, total) ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Импорт заклинаний") },
            text = {
                Column {
                    Text("Пожалуйста, не закрывайте приложение...")
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(progress = { cur.toFloat() / total.toFloat() }, modifier = Modifier.fillMaxWidth())
                    Text("$cur / $total", modifier = Modifier.align(Alignment.End))
                }
            },
            confirmButton = {}
        )
    }

    errorDialogData?.let { (name, reason, callback) ->
        AlertDialog(
            onDismissRequest = { callback(SpellbookManager.ImportAction.CANCEL); errorDialogData = null },
            title = { Text("Ошибка импорта") },
            text = {
                Text("Заклинание \"$name\" не было импортировано по причине:\n$reason\n\nПопробовать еще раз?")
            },
            confirmButton = {
                Column {
                    TextButton(onClick = { callback(SpellbookManager.ImportAction.REPLACE); errorDialogData = null }) { Text("Да (Заменить)") }
                    if (reason.contains("существует")) {
                        TextButton(onClick = { callback(SpellbookManager.ImportAction.RENAME); errorDialogData = null }) { Text("Добавить с изм. названием") }
                    }
                    TextButton(onClick = { callback(SpellbookManager.ImportAction.SKIP); errorDialogData = null }) { Text("Пропустить") }
                }
            }
        )
    }

    DeleteConfirmationDialog(
        showDialog = showDeleteAllConfirm,
        onDismiss = { showDeleteAllConfirm = false },
        onConfirm = {
            selectedSpellIds.forEach { spellbookManager.deleteSpell(it) }
            selectedSpellIds = emptySet()
            isSelectionMode = false
            refreshTrigger++
            showDeleteAllConfirm = false
        },
        title = "Удалить выбранные заклинания (${selectedSpellIds.size})?",
        settingsViewModel = settingsViewModel
    )
}

@Composable
@Deprecated("Use SpellFiltersArea")
fun SpellbookFilters(
    filterLevel: String?, onLevelChange: (String?) -> Unit,
    filterClass: CharacterClass?, onClassChange: (CharacterClass?) -> Unit,
    filterSchool: SpellSchool?, onSchoolChange: (SpellSchool?) -> Unit,
    filterConc: Boolean?, onConcChange: (Boolean?) -> Unit,
    filterDamage: Boolean?, onDamageChange: (Boolean?) -> Unit,
    filterType: MagicAttackType?, onTypeChange: (MagicAttackType?) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = filterLevel == "0", onClick = { onLevelChange(if (filterLevel == "0") null else "0") }, label = { Text("Заговоры") })
            FilterChip(selected = filterConc == true, onClick = { onConcChange(if (filterConc == true) null else true) }, label = { Text("Концентрация") })
            FilterChip(selected = filterDamage == true, onClick = { onDamageChange(if (filterDamage == true) null else true) }, label = { Text("С уроном") })
        }
        Text("Подробные фильтры будут в выпадающих списках...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
    }
}
