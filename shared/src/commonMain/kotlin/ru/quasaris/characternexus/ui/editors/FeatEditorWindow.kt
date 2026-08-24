package ru.quasaris.characternexus.ui.editors

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
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
import ru.quasaris.characternexus.backend.*
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.ui.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatEditorWindow(
    initialFeat: GameFeat? = null,
    onDismiss: () -> Unit,
    onSave: (JsonElement) -> Unit,
    forceBlurEnabled: Boolean = false,
    settingsViewModel: SettingsViewModel? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val isOled = colorScheme.background == Color.Black

    var name by remember { mutableStateOf(initialFeat?.name ?: "") }
    var id by remember { mutableStateOf(initialFeat?.id ?: "") }
    var description by remember { mutableStateOf(initialFeat?.description ?: "") }
    var prerequisites by remember { mutableStateOf(initialFeat?.prerequisites ?: "") }
    var repeatable by remember { mutableStateOf(initialFeat?.repeatable ?: false) }
    
    val features = remember { 
        mutableStateListOf<GameFeature>().apply { 
            addAll(initialFeat?.features ?: emptyList()) 
        } 
    }
    
    var editingFeature by remember { mutableStateOf<GameFeature?>(null) }

    BackHandler(onBack = onDismiss)
    
    LaunchedEffect(name) {
        val path = mutableListOf<NavNode>()
        path.add(NavNode("modules", "Модули", 0) { onDismiss() })
        path.add(NavNode("editor", "Редактор черты", 1) { onDismiss() })
        path.add(NavNode("comp", name.ifBlank { "Новая" }, 2))
        NavigationPathManager.updatePath("modules", path)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Редактор черты", fontWeight = FontWeight.Black, color = colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = colorScheme.onSurface)
                    }
                },
                actions = {
                    Button(onClick = {
                        val json = JsonConfig.json.encodeToJsonElement(GameFeat(
                            id = id,
                            name = name,
                            description = description,
                            prerequisites = prerequisites,
                            repeatable = repeatable,
                            features = features.toList()
                        ))
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
                value = name,
                onValueChange = { name = it },
                label = { Text("Название") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = colorScheme.onSurface, unfocusedTextColor = colorScheme.onSurface)
            )

            OutlinedTextField(
                value = id,
                onValueChange = { id = it },
                label = { Text("ID") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = colorScheme.onSurface, unfocusedTextColor = colorScheme.onSurface)
            )

            OutlinedTextField(
                value = prerequisites,
                onValueChange = { prerequisites = it },
                label = { Text("Требования") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = colorScheme.onSurface, unfocusedTextColor = colorScheme.onSurface)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Повторяемая", modifier = Modifier.weight(1f), color = colorScheme.onSurface)
                Switch(checked = repeatable, onCheckedChange = { repeatable = it })
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
                Text("Способности черты", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), color = colorScheme.onSurface)
                IconButton(onClick = { 
                    editingFeature = GameFeature(id = ru.quasaris.characternexus.util.generateUuid()) 
                }) {
                    Icon(Icons.Default.Add, null, tint = colorScheme.primary)
                }
            }

            features.forEach { feat ->
                FeatureItem(
                    feature = feat,
                    onEdit = { editingFeature = feat },
                    onDelete = { features.remove(feat) }
                )
            }

            Spacer(Modifier.height(80.dp))
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
