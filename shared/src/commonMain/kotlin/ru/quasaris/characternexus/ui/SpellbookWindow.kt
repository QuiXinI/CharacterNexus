package ru.quasaris.characternexus.ui

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.quasaris.characternexus.backend.SpellbookManager
import ru.quasaris.characternexus.tabs.spells.SpellCardItem
import ru.quasaris.characternexus.ui.editors.SpellEditorWindow
import ru.quasaris.characternexus.tabs.spells.SpellFiltersArea
import ru.quasaris.characternexus.tabs.spells.ExportModuleDialog
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.ui.CommonFilePicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpellbookWindow(
    spellbookManager: SpellbookManager,
    glossaryImporter: ru.quasaris.characternexus.backend.GlossaryImporter,
    onOpenDrawer: () -> Unit,
    onFullscreenDialogOpenChange: (Boolean) -> Unit = {},
    hazeState: HazeState? = null,
    popupHazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false,
    settingsViewModel: ru.quasaris.characternexus.backend.SettingsViewModel? = null,
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

    var showExportModuleDialog by remember { mutableStateOf(false) }
    var pendingManifest by remember { mutableStateOf<ModuleManifest?>(null) }
    
    val lastExportName by settingsViewModel?.lastModuleExportName?.collectAsState() ?: remember { mutableStateOf("") }
    val lastExportDesc by settingsViewModel?.lastModuleExportDescription?.collectAsState() ?: remember { mutableStateOf("") }
    val lastExportVersion by settingsViewModel?.lastModuleExportVersion?.collectAsState() ?: remember { mutableStateOf("1.0.0") }
    val lastExportId by settingsViewModel?.lastModuleExportId?.collectAsState() ?: remember { mutableStateOf("") }

    // A trigger to refresh the list when spells are updated
    var refreshTrigger by remember { mutableIntStateOf(0) }
    
    val allSpells = remember(refreshTrigger) { spellbookManager.loadSpells() }
    val filteredSpells = remember(allSpells, searchQuery, filterState) {
        allSpells.filter { spell ->
            spell.matches(filterState, searchQuery)
        }.sortedBy { it.level }
    }

    var downgradeData by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    val downgradeResult = remember { mutableStateOf<Boolean?>(null) }

    var showImportPicker by remember { mutableStateOf(false) }
    CommonFilePicker(show = showImportPicker, fileExtensions = listOf("json", "spellbook")) { file ->
        showImportPicker = false
        file?.let { platformFile ->
            scope.launch {
                val bytes = platformFile.readBytes()
                val success = glossaryImporter.importModule(
                    bytes = bytes,
                    onProgress = { cur, total -> importProgress = cur to total },
                    onDowngradeConfirm = { name, oldV, newV ->
                        downgradeData = Triple(name, oldV, newV)
                        while (downgradeResult.value == null) {
                            kotlinx.coroutines.delay(100)
                        }
                        val res = downgradeResult.value ?: false
                        downgradeResult.value = null
                        downgradeData = null
                        res
                    },
                    onError = { name, reason ->
                        // Fallback to legacy import or show error
                        scope.launch {
                             spellbookManager.importSpells(
                                bytes = bytes,
                                onProgress = { cur, total -> importProgress = cur to total },
                                onError = { n, r, callback -> errorDialogData = Triple(n, r, callback) }
                            )
                            importProgress = null
                            refreshTrigger++
                        }
                    }
                )
                importProgress = null
                if (success) refreshTrigger++
            }
        }
    }

    var showExportPicker by remember { mutableStateOf(false) }
    CommonFilePicker(show = showExportPicker, fileExtensions = listOf("spellbook")) { file ->
        showExportPicker = false
        file?.let { platformFile ->
            scope.launch {
                pendingManifest?.let { manifest ->
                    if (selectedSpellIds.isNotEmpty()) {
                        spellbookManager.exportSpellbook(platformFile.path, manifest, selectedSpellIds.toList())
                    } else {
                        spellbookManager.exportSpellbook(platformFile.path, manifest)
                    }
                }
            }
        }
    }

    var showSingleExportPicker by remember { mutableStateOf(false) }
    CommonFilePicker(show = showSingleExportPicker, fileExtensions = listOf("json")) { file ->
        showSingleExportPicker = false
        file?.let { platformFile ->
            editingSpell?.let { spell ->
                scope.launch {
                    spellbookManager.exportSingleSpell(platformFile.path, spell)
                }
            }
        }
    }

    val colorScheme = MaterialTheme.colorScheme
    val isOled = colorScheme.background == Color.Black
    
    val isAnyFullscreenDialogOpen = editingSpell != null || showExportModuleDialog

    LaunchedEffect(isAnyFullscreenDialogOpen) {
        onFullscreenDialogOpenChange(isAnyFullscreenDialogOpen)
    }

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
                            IconButton(onClick = { 
                                showExportModuleDialog = true 
                            }) {
                                Icon(Icons.Default.FileUpload, contentDescription = "Экспорт выбранных")
                            }
                        } else {
                            IconButton(onClick = { showFilters = !showFilters }) {
                                Icon(Icons.Default.FilterList, null, tint = if (showFilters) colorScheme.primary else colorScheme.onSurface)
                            }
                            IconButton(onClick = { showImportPicker = true }) {
                                Icon(Icons.Default.FileDownload, contentDescription = "Импорт")
                            }
                            IconButton(onClick = { 
                                showExportModuleDialog = true 
                            }) {
                                Icon(Icons.Default.FileUpload, contentDescription = "Экспорт")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (forceBlurEnabled && !isOled) Color.Transparent.copy(alpha = 0.0f) else colorScheme.surface
                    )
                )
            },
            floatingActionButton = {
                if (!isSelectionMode && !isAnyFullscreenDialogOpen) {
                    FloatingActionButton(onClick = { editingSpell = SpellCard() }) {
                        Icon(Icons.Default.Add, contentDescription = "Добавить")
                    }
                }
            },
            containerColor = if (forceBlurEnabled && !isOled) Color.Transparent.copy(alpha = 0.0f) else colorScheme.background,
            modifier = Modifier
                .blur(if (isAnyFullscreenDialogOpen && forceBlurEnabled && !isOled) 24.dp else 0.dp)
                .run {
                    if (isAnyFullscreenDialogOpen && forceBlurEnabled && !isOled) {
                        this.drawWithContent {
                            drawContent()
                            drawRect(colorScheme.surface.copy(alpha = 0.1f))
                        }
                    } else this
                }
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                SpellFiltersArea(
                    visible = showFilters,
                    filterState = filterState,
                    onFilterChange = { filterState = it },
                    modifier = Modifier.weight(1f, fill = false)
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
                                },
                                hazeState = hazeState,
                                forceBlurEnabled = forceBlurEnabled
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
                    .outerShadow(RoundedCornerShape(12.dp), blur = 6.dp),
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
        SpellEditorWindow(
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
                showSingleExportPicker = true
            },
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

    if (downgradeData != null) {
        AlertDialog(
            onDismissRequest = { downgradeResult.value = false },
            title = { Text("Понижение версии") },
            text = { Text("Вы хотите понизить версию модуля \"${downgradeData!!.first}\" с ${downgradeData!!.second} до ${downgradeData!!.third}?") },
            confirmButton = {
                Button(onClick = { downgradeResult.value = true }) { Text("Да") }
            },
            dismissButton = {
                TextButton(onClick = { downgradeResult.value = false }) { Text("Нет") }
            }
        )
    }

    if (showExportModuleDialog) {
        ExportModuleDialog(
            initialName = lastExportName,
            initialDescription = lastExportDesc,
            initialVersion = lastExportVersion,
            initialId = lastExportId,
            onDismiss = { showExportModuleDialog = false },
            onConfirm = { name, desc, version, id ->
                settingsViewModel?.updateLastModuleExport(name, desc, version, id)
                pendingManifest = ModuleManifest(
                    id = id,
                    name = name,
                    version = version,
                    description = desc
                )
                showExportModuleDialog = false
                showExportPicker = true
            }
        )
    }
}
