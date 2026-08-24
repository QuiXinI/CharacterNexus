package ru.quasaris.characternexus.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ru.quasaris.characternexus.backend.*
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.getAppDataDir
import ru.quasaris.characternexus.platformFileSystem
import ru.quasaris.characternexus.backend.JsonConfig
import ru.quasaris.characternexus.ui.editors.*
import okio.Path
import kotlinx.serialization.json.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleEditorWindow(
    moduleManager: ModuleManager,
    spellbookManager: SpellbookManager,
    initialModule: InstalledModule,
    onBack: () -> Unit,
    forceBlurEnabled: Boolean = false,
    settingsViewModel: SettingsViewModel? = null
) {
    var manifest by remember { mutableStateOf(initialModule.manifest) }
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var editingSpell by remember { mutableStateOf<SpellCard?>(null) }
    var editingClass by remember { mutableStateOf<GameClass?>(null) }
    var editingSubclass by remember { mutableStateOf<GameSubclass?>(null) }
    var editingSpecies by remember { mutableStateOf<GameSpecies?>(null) }
    var editingFeat by remember { mutableStateOf<GameFeat?>(null) }
    
    var showAddMenu by remember { mutableStateOf(false) }

    val colorScheme = MaterialTheme.colorScheme
    val isOled = colorScheme.background == Color.Black

    BackHandler(onBack = onBack)
    
    LaunchedEffect(manifest.name) {
        val path = mutableListOf<NavNode>()
        path.add(NavNode("modules", "Модули", 0) { onBack() })
        path.add(NavNode("editor", manifest.name.ifBlank { "Новый" }, 1))
        // We could also report components here if we wanted to mirror them in the sidebar
        NavigationPathManager.updatePath("modules", path)
    }

    if (editingSpell != null) {
        SpellEditorWindow(
            spell = editingSpell!!,
            onDismiss = { editingSpell = null },
            onSave = { updated ->
                spellbookManager.addOrUpdateSpell(updated)
                val existing = manifest.contents.find { it.id == updated.id && it.type == "spell" }
                if (existing == null) {
                    manifest = manifest.copy(contents = manifest.contents + ModuleContent("spell", updated.id, "${updated.id}.json"))
                }
                editingSpell = null
            },
            onDelete = { spellToDelete ->
                spellbookManager.deleteSpell(spellToDelete.id)
                manifest = manifest.copy(contents = manifest.contents.filterNot { it.id == spellToDelete.id && it.type == "spell" })
                editingSpell = null
            },
            forceBlurEnabled = forceBlurEnabled,
            settingsViewModel = settingsViewModel
        )
        return
    }

    if (editingClass != null || editingSubclass != null) {
        ClassEditorWindow(
            initialClass = editingClass,
            initialSubclass = editingSubclass,
            onDismiss = { editingClass = null; editingSubclass = null },
            onSave = { json ->
                val type = if (editingSubclass != null) "subclass" else "class"
                val obj = json.jsonObject
                val id = obj["id"]?.jsonPrimitive?.content ?: ""
                saveItem(if (type == "class") "classes" else "subclasses", id, json.toString())
                
                val existing = manifest.contents.find { it.id == id && it.type == type }
                if (existing == null) {
                    manifest = manifest.copy(contents = manifest.contents + ModuleContent(type, id, "$id.json"))
                }
                editingClass = null
                editingSubclass = null
            },
            forceBlurEnabled = forceBlurEnabled,
            settingsViewModel = settingsViewModel
        )
        return
    }

    if (editingSpecies != null) {
        SpeciesEditorWindow(
            initialSpecies = editingSpecies,
            onDismiss = { editingSpecies = null },
            onSave = { json ->
                val id = json.jsonObject["id"]?.jsonPrimitive?.content ?: ""
                saveItem("species", id, json.toString())
                val existing = manifest.contents.find { it.id == id && it.type == "species" }
                if (existing == null) {
                    manifest = manifest.copy(contents = manifest.contents + ModuleContent("species", id, "$id.json"))
                }
                editingSpecies = null
            },
            forceBlurEnabled = forceBlurEnabled,
            settingsViewModel = settingsViewModel
        )
        return
    }

    if (editingFeat != null) {
        FeatEditorWindow(
            initialFeat = editingFeat,
            onDismiss = { editingFeat = null },
            onSave = { json ->
                val id = json.jsonObject["id"]?.jsonPrimitive?.content ?: ""
                saveItem("feats", id, json.toString())
                val existing = manifest.contents.find { it.id == id && it.type == "feat" }
                if (existing == null) {
                    manifest = manifest.copy(contents = manifest.contents + ModuleContent("feat", id, "$id.json"))
                }
                editingFeat = null
            },
            forceBlurEnabled = forceBlurEnabled,
            settingsViewModel = settingsViewModel
        )
        return
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Редактор модуля", fontWeight = FontWeight.Black, color = colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = colorScheme.onSurface)
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            scope.launch {
                                isSaving = true
                                moduleManager.updateModule(initialModule.manifest.id, manifest)
                                isSaving = false
                                snackbarHostState.showSnackbar("Модуль сохранен")
                            }
                        },
                        enabled = !isSaving,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Сохранить")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (forceBlurEnabled && !isOled) Color.Transparent.copy(alpha = 0.0f) else colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = if (forceBlurEnabled && !isOled) Color.Transparent.copy(alpha = 0.0f) else colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Основная информация", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)

            OutlinedTextField(
                value = manifest.name,
                onValueChange = { manifest = manifest.copy(name = it) },
                label = { Text("Название") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = colorScheme.onSurface, unfocusedTextColor = colorScheme.onSurface)
            )

            OutlinedTextField(
                value = manifest.id,
                onValueChange = { manifest = manifest.copy(id = it) },
                label = { Text("ID модуля") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(disabledTextColor = colorScheme.onSurface.copy(alpha = 0.6f))
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = manifest.version,
                    onValueChange = { manifest = manifest.copy(version = it) },
                    label = { Text("Версия") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = colorScheme.onSurface, unfocusedTextColor = colorScheme.onSurface)
                )
                OutlinedTextField(
                    value = manifest.system,
                    onValueChange = { manifest = manifest.copy(system = it) },
                    label = { Text("Система") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = colorScheme.onSurface, unfocusedTextColor = colorScheme.onSurface)
                )
            }

            OutlinedTextField(
                value = manifest.description,
                onValueChange = { manifest = manifest.copy(description = it) },
                label = { Text("Описание") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = colorScheme.onSurface, unfocusedTextColor = colorScheme.onSurface)
            )

            Spacer(Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Компоненты", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), color = colorScheme.onSurface)
                Box {
                    IconButton(onClick = { showAddMenu = true }) {
                        Icon(Icons.Default.Add, null, tint = colorScheme.primary)
                    }
                    DropdownMenu(expanded = showAddMenu, onDismissRequest = { showAddMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Заклинание") },
                            onClick = {
                                showAddMenu = false
                                editingSpell = SpellCard(
                                    id = ru.quasaris.characternexus.util.generateUuid(),
                                    source = manifest.name,
                                    sourceModuleId = manifest.id
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Класс") },
                            onClick = {
                                showAddMenu = false
                                editingClass = GameClass(id = "class_${ru.quasaris.characternexus.util.generateUuid().take(8)}")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Подкласс") },
                            onClick = {
                                showAddMenu = false
                                editingSubclass = GameSubclass(id = "subclass_${ru.quasaris.characternexus.util.generateUuid().take(8)}")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Вид") },
                            onClick = {
                                showAddMenu = false
                                editingSpecies = GameSpecies(id = "species_${ru.quasaris.characternexus.util.generateUuid().take(8)}")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Черта") },
                            onClick = {
                                showAddMenu = false
                                editingFeat = GameFeat(id = "feat_${ru.quasaris.characternexus.util.generateUuid().take(8)}")
                            }
                        )
                    }
                }
            }

            if (manifest.contents.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Text("Нет компонентов", color = colorScheme.onSurfaceVariant)
                }
            } else {
                manifest.contents.forEach { content ->
                    ComponentItem(
                        content = content,
                        onEdit = {
                            when (content.type) {
                                "spell" -> {
                                    editingSpell = spellbookManager.loadSpells().find { it.id == content.id }
                                }
                                "class" -> {
                                    editingClass = loadItem<GameClass>("classes", content.file)
                                }
                                "subclass" -> {
                                    editingSubclass = loadItem<GameSubclass>("subclasses", content.file)
                                }
                                "species" -> {
                                    editingSpecies = loadItem<GameSpecies>("species", content.file)
                                }
                                "feat" -> {
                                    editingFeat = loadItem<GameFeat>("feats", content.file)
                                }
                            }
                        },
                        onDelete = {
                            manifest = manifest.copy(contents = manifest.contents.filterNot { it.id == content.id && it.type == content.type })
                        }
                    )
                }
            }
            
            Spacer(Modifier.height(80.dp))
        }
    }
}

private fun saveItem(dir: String, id: String, jsonContent: String) {
    val path = getAppDataDir().resolve("glossary/$dir/$id.json")
    val parent = path.parent
    if (parent != null && !platformFileSystem.exists(parent)) {
        platformFileSystem.createDirectories(parent)
    }
    platformFileSystem.write(path) {
        writeUtf8(jsonContent)
    }
}

private inline fun <reified T> loadItem(dir: String, fileName: String): T? {
    val path = getAppDataDir().resolve("glossary/$dir/$fileName")
    return if (platformFileSystem.exists(path)) {
        try {
            val json = platformFileSystem.read(path) { readUtf8() }
            JsonConfig.json.decodeFromString<T>(json)
        } catch (e: Exception) {
            ru.quasaris.characternexus.util.Logger.e("ModuleEditor", "Error loading item", e)
            null
        }
    } else null
}

@Composable
fun ComponentItem(
    content: ModuleContent,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = when(content.type) {
                    "spell" -> Icons.Default.AutoFixHigh
                    "class" -> Icons.Default.Shield
                    "subclass" -> Icons.Default.KeyboardDoubleArrowDown
                    "feat" -> Icons.Default.Star
                    "species" -> Icons.Default.Groups
                    else -> Icons.Default.Extension
                },
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(content.id, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                Text(content.type.uppercase(), style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, null, modifier = Modifier.size(20.dp), tint = colorScheme.onSurface)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, modifier = Modifier.size(20.dp), tint = colorScheme.error)
            }
        }
    }
}
