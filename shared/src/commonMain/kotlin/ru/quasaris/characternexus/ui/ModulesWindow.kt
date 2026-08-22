package ru.quasaris.characternexus.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch
import ru.quasaris.characternexus.backend.*
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.ui.CommonFilePicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModulesWindow(
    moduleManager: ModuleManager,
    spellbookManager: SpellbookManager,
    glossaryImporter: GlossaryImporter,
    onOpenDrawer: () -> Unit,
    onFullscreenDialogOpenChange: (Boolean) -> Unit = {},
    forceBlurEnabled: Boolean = false,
    settingsViewModel: SettingsViewModel? = null,
) {
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    val installedModules = remember(refreshTrigger) { moduleManager.getInstalledModules() }
    
    val filteredModules = remember(installedModules, searchQuery) {
        installedModules.filter { 
            it.manifest.name.contains(searchQuery, ignoreCase = true) || 
            it.manifest.id.contains(searchQuery, ignoreCase = true)
        }
    }

    var moduleToDelete by remember { mutableStateOf<InstalledModule?>(null) }
    var importProgress by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var downgradeData by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    var downgradeResult = remember { mutableStateOf<Boolean?>(null) }
    var showImportPicker by remember { mutableStateOf(false) }
    var showNotAModuleError by remember { mutableStateOf(false) }

    CommonFilePicker(
        show = showImportPicker,
        fileExtensions = listOf("spellbook", "sb", "databook", "db", "json")
    ) { file ->
        showImportPicker = false
        file?.let {
            scope.launch {
                val success = glossaryImporter.importModule(
                    bytes = it.readBytes(),
                    onProgress = { cur, total -> importProgress = cur to total },
                    onDowngradeConfirm = { name, oldV, newV ->
                        downgradeData = Triple(name, oldV, newV)
                        // Wait for user input
                        while (downgradeResult.value == null) {
                            kotlinx.coroutines.delay(100)
                        }
                        val res = downgradeResult.value ?: false
                        downgradeResult.value = null
                        downgradeData = null
                        res
                    },
                    onError = { _, _ -> /* Log error */ }
                )
                importProgress = null
                if (success) {
                    refreshTrigger++
                } else {
                    showNotAModuleError = true
                }
            }
        }
    }

    val colorScheme = MaterialTheme.colorScheme
    val isOled = colorScheme.background == androidx.compose.ui.graphics.Color.Black

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Модули", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Меню")
                    }
                },
                actions = {
                    IconButton(onClick = { showImportPicker = true }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Импорт")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (forceBlurEnabled && !isOled) Color.Transparent.copy(alpha = 0.0f) else colorScheme.surface
                )
            )
        },
        containerColor = if (forceBlurEnabled && !isOled) Color.Transparent.copy(alpha = 0.0f) else colorScheme.background
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Поиск модулей...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                singleLine = true
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredModules, key = { it.manifest.id }) { module ->
                    ModuleItem(
                        module = module,
                        onDelete = { moduleToDelete = module }
                    )
                }
            }
        }
    }

    if (moduleToDelete != null) {
        DeleteConfirmationDialog(
            showDialog = true,
            onDismiss = { moduleToDelete = null },
            onConfirm = {
                moduleManager.deleteModule(moduleToDelete!!.manifest.id, spellbookManager)
                refreshTrigger++
            },
            title = "Удалить модуль?",
            text = "Вы действительно хотите удалить модуль \"${moduleToDelete?.manifest?.name}\"? Все данные, добавленные этим модулем в глоссарий, будут безвозвратно удалены.",
            settingsViewModel = settingsViewModel
        )
    }

    if (importProgress != null) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Импорт модуля...") },
            text = {
                val progress = importProgress // Capture current state
                if (progress != null) {
                    Column {
                        LinearProgressIndicator(
                            progress = { progress.first.toFloat() / progress.second.toFloat() },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("${progress.first} / ${progress.second}", modifier = Modifier.align(Alignment.End))
                    }
                }
            },
            confirmButton = {}
        )
    }

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

    if (showNotAModuleError) {
        AlertDialog(
            onDismissRequest = { showNotAModuleError = false },
            title = { Text("Ошибка") },
            text = { Text("Этот файл не является модулем") },
            confirmButton = {
                Button(onClick = { showNotAModuleError = false }) { Text("ОК") }
            }
        )
    }
}

@Composable
fun ModuleItem(
    module: InstalledModule,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(module.manifest.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Версия: ${module.manifest.version}", style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                }
            }
            if (module.manifest.description.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(module.manifest.description, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
