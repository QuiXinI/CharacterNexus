package ru.quasaris.characternexus.ui.editors

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
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
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
import ru.quasaris.characternexus.backend.*
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.ui.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassEditorWindow(
    initialClass: GameClass? = null,
    initialSubclass: GameSubclass? = null,
    onDismiss: () -> Unit,
    onSave: (JsonElement) -> Unit,
    forceBlurEnabled: Boolean = false,
    settingsViewModel: SettingsViewModel? = null
) {
    val isSubclass = initialSubclass != null
    
    var className by remember { mutableStateOf(initialClass?.name ?: initialSubclass?.name ?: "") }
    var classId by remember { mutableStateOf(initialClass?.id ?: initialSubclass?.id ?: "") }
    var description by remember { mutableStateOf(initialClass?.description ?: initialSubclass?.description ?: "") }
    var hitDie by remember { mutableStateOf(initialClass?.hitDie ?: "8") }
    var primaryAbility by remember { mutableStateOf(initialClass?.primaryAbility ?: "") }
    
    var parentClassId by remember { mutableStateOf(initialSubclass?.classId ?: "") }
    
    val features = remember { 
        mutableStateListOf<GameFeature>().apply { 
            addAll(initialClass?.features ?: initialSubclass?.features ?: emptyList()) 
        } 
    }
    
    var editingFeature by remember { mutableStateOf<GameFeature?>(null) }
    var showRightDrawer by remember { mutableStateOf(false) }

    val colorScheme = MaterialTheme.colorScheme
    val isOled = colorScheme.background == Color.Black

    BackHandler(onBack = if (showRightDrawer) { { showRightDrawer = false } } else onDismiss)
    
    LaunchedEffect(className, isSubclass, features.size) {
        val prefix = if (isSubclass) "Редактор подкласса" else "Редактор класса"
        val path = mutableListOf<NavNode>()
        path.add(NavNode("modules", "Модули", 0) { onDismiss() })
        path.add(NavNode("editor", prefix, 1) { onDismiss() })
        path.add(NavNode("comp", className.ifBlank { "Новый" }, 2))
        
        features.sortedBy { it.level ?: 0 }.forEach { feat ->
            path.add(NavNode("feat_${feat.id}", feat.name ?: "Умение", 3))
        }
        
        NavigationPathManager.updatePath("modules", path)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (isSubclass) "Редактор подкласса" else "Редактор класса", fontWeight = FontWeight.Black, color = colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = colorScheme.onSurface)
                    }
                },
                actions = {
                    IconButton(onClick = { showRightDrawer = true }) {
                        Icon(Icons.AutoMirrored.Filled.List, null, tint = colorScheme.onSurface)
                    }
                    Button(onClick = {
                        val json = if (isSubclass) {
                            JsonConfig.json.encodeToJsonElement(GameSubclass(
                                id = classId,
                                classId = parentClassId,
                                name = className,
                                description = description,
                                features = features.toList()
                            ))
                        } else {
                            JsonConfig.json.encodeToJsonElement(GameClass(
                                id = classId,
                                name = className,
                                description = description,
                                hitDie = hitDie,
                                primaryAbility = primaryAbility,
                                features = features.toList()
                            ))
                        }
                        onSave(json)
                    }, modifier = Modifier.padding(horizontal = 8.dp)) {
                        Text("Сохранить")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (forceBlurEnabled && !isOled) Color.Transparent.copy(alpha = 0.0f) else colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = className,
                    onValueChange = { className = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = colorScheme.onSurface, unfocusedTextColor = colorScheme.onSurface)
                )

                OutlinedTextField(
                    value = classId,
                    onValueChange = { classId = it },
                    label = { Text("ID") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = colorScheme.onSurface, unfocusedTextColor = colorScheme.onSurface)
                )

                if (isSubclass) {
                    OutlinedTextField(
                        value = parentClassId,
                        onValueChange = { parentClassId = it },
                        label = { Text("ID родительского класса") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = colorScheme.onSurface, unfocusedTextColor = colorScheme.onSurface)
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = hitDie,
                            onValueChange = { hitDie = it.filter { c -> c.isDigit() } },
                            label = { Text("Кость хитов (d?)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = colorScheme.onSurface, unfocusedTextColor = colorScheme.onSurface)
                        )
                        OutlinedTextField(
                            value = primaryAbility,
                            onValueChange = { primaryAbility = it },
                            label = { Text("Осн. хар-ка") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = colorScheme.onSurface, unfocusedTextColor = colorScheme.onSurface)
                        )
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 5,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = colorScheme.onSurface, unfocusedTextColor = colorScheme.onSurface)
                )

                Spacer(Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Способности", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), color = colorScheme.onSurface)
                    IconButton(onClick = { 
                        editingFeature = GameFeature(id = ru.quasaris.characternexus.util.generateUuid()) 
                    }) {
                        Icon(Icons.Default.Add, null, tint = colorScheme.primary)
                    }
                }

                features.sortedBy { it.level ?: 0 }.forEach { feat ->
                    FeatureItem(
                        feature = feat,
                        onEdit = { editingFeature = feat },
                        onDelete = { features.remove(feat) }
                    )
                }

                Spacer(Modifier.height(80.dp))
            }

            // Right Drawer
            if (showRightDrawer) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { showRightDrawer = false }
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(280.dp)
                            .align(Alignment.CenterEnd)
                            .clickable(enabled = false) {},
                        color = colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Умения по уровням", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                            Spacer(Modifier.height(16.dp))
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                features.sortedBy { it.level ?: 0 }.forEach { feat ->
                                    ListItem(
                                        headlineContent = { Text(feat.name ?: "Без названия", color = colorScheme.onSurface) },
                                        overlineContent = { Text("Уровень ${feat.level ?: 1}", color = colorScheme.onSurfaceVariant) },
                                        modifier = Modifier.clickable {
                                            showRightDrawer = false
                                            // Navigation logic would go here
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (editingFeature != null) {
        FeatureEditorDialog(
            feature = editingFeature!!,
            onDismiss = { editingFeature = null },
            onSave = { updated ->
                val index = features.indexOfFirst { it.id == updated.id }
                if (index != -1) {
                    features[index] = updated
                } else {
                    features.add(updated)
                }
                editingFeature = null
            }
        )
    }
}

@Composable
fun FeatureItem(
    feature: GameFeature,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(feature.name ?: "Без названия", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                Text("Уровень: ${feature.level ?: 1}", style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurfaceVariant)
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

@Composable
fun FeatureEditorDialog(
    feature: GameFeature,
    onDismiss: () -> Unit,
    onSave: (GameFeature) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var name by remember { mutableStateOf(feature.name ?: "") }
    var level by remember { mutableStateOf(feature.level?.toString() ?: "1") }
    var desc by remember { mutableStateOf(feature.description ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редактировать способность", color = colorScheme.onSurface) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name, 
                    onValueChange = { name = it }, 
                    label = { Text("Название") }, 
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = colorScheme.onSurface, unfocusedTextColor = colorScheme.onSurface)
                )
                OutlinedTextField(
                    value = level, 
                    onValueChange = { level = it.filter { c -> c.isDigit() } }, 
                    label = { Text("Уровень") }, 
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = colorScheme.onSurface, unfocusedTextColor = colorScheme.onSurface)
                )
                OutlinedTextField(
                    value = desc, 
                    onValueChange = { desc = it }, 
                    label = { Text("Описание") }, 
                    modifier = Modifier.fillMaxWidth(), 
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = colorScheme.onSurface, unfocusedTextColor = colorScheme.onSurface)
                )
            }
        },
        confirmButton = {
            Button(onClick = { 
                onSave(feature.copy(name = name, level = level.toIntOrNull() ?: 1, description = desc)) 
            }) {
                Text("ОК")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
