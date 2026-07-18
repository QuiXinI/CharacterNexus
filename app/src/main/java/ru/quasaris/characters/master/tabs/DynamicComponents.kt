package ru.quasaris.characters.master.tabs

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.util.Log
import ru.quasaris.characters.master.backend.evaluateFormula
import ru.quasaris.characters.master.backend.SettingsViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.HazeInputScale

@Composable
fun SpoilerComponent(
    content: AnnotatedString,
    modifier: Modifier = Modifier
) {
    // We use regular remember so it resets on tab recreation/recomposition if the parent state changes
    var isExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .clickable { isExpanded = !isExpanded }
                .padding(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (isExpanded) "Скрыть" else "Показать спойлер",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Text(
                    text = content,
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
fun ResourceComponent(
    resource: DynamicContentBlock.Resource,
    statsMap: Map<String, String>,
    onResourceUpdate: (String) -> Unit, // Returns new tag string
    hazeState: HazeState? = null,
    settingsViewModel: SettingsViewModel? = null
) {
    var showSettings by remember { mutableStateOf(false) }
    var showNotesDialog by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    val curValue = evaluateFormula(resource.current, statsMap)
    val maxValue = evaluateFormula(resource.max, statsMap)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { showSettings = true },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = resource.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (resource.showNotes) {
                        IconButton(
                            onClick = { showNotesDialog = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Info, 
                                null, 
                                modifier = Modifier.size(18.dp), 
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Rest indicators
                    if (resource.shortRest != "0") {
                        RestIndicator(isShort = true, value = resource.shortRest)
                    }
                    if (resource.longRest != "0") {
                        RestIndicator(isShort = false, value = resource.longRest)
                    }
                }
            }

    if (resource.link != null) {
        val fullLink = remember(resource.link) {
            if (resource.link.contains("://")) resource.link else "https://${resource.link}"
        }
        IconButton(
            onClick = { 
                try {
                    uriHandler.openUri(fullLink) 
                } catch (e: Exception) {
                    Log.e("ResourceComponent", "Failed to open URI: $fullLink", e)
                }
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.OpenInNew, null, modifier = Modifier.size(20.dp))
        }
    }

            Text("-", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = {
                        val newCur = (curValue - 1).coerceAtLeast(0)
                        onResourceUpdate(updateResourceTag(resource, current = newCur.toString()))
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Remove, null, tint = MaterialTheme.colorScheme.primary)
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (maxValue > 0) "$curValue/$maxValue" else curValue.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                IconButton(
                    onClick = {
                        val newCur = if (maxValue > 0) (curValue + 1).coerceAtMost(maxValue) else curValue + 1
                        onResourceUpdate(updateResourceTag(resource, current = newCur.toString()))
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    if (showNotesDialog) {
        AlertDialog(
            onDismissRequest = { 
                focusManager.clearFocus()
                showNotesDialog = false 
            },
            title = { Text(resource.name) },
            text = { Text(resource.notes) },
            confirmButton = {
                TextButton(onClick = { 
                    focusManager.clearFocus()
                    showNotesDialog = false 
                }) {
                    Text("ОК")
                }
            }
        )
    }

    if (showSettings) {
        ResourceSettingsDialog(
            resource = resource,
            onSave = { updated ->
                onResourceUpdate(updated)
                showSettings = false
            },
            onDismiss = { showSettings = false },
            hazeState = hazeState,
            statsMap = statsMap
        )
    }
}

@Composable
fun RestIndicator(isShort: Boolean, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Icon(
            imageVector = if (isShort) Icons.Default.WbSunny else Icons.Default.NightsStay,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = if (isShort) Color(0xFFFFB74D) else Color(0xFF90CAF9)
        )
        if (value != "all" && (isShort || value != "1")) {
            Text(value, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceSettingsDialog(
    resource: DynamicContentBlock.Resource,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
    hazeState: HazeState? = null,
    statsMap: Map<String, String>
) {
    var name by remember { mutableStateOf(resource.name) }
    var current by remember { mutableStateOf(resource.current) }
    var max by remember { mutableStateOf(resource.max) }
    var shortRest by remember { mutableStateOf(resource.shortRest) }
    var longRest by remember { mutableStateOf(resource.longRest) }
    var link by remember { mutableStateOf(resource.link ?: "") }
    var notes by remember { mutableStateOf(resource.notes) }
    var showNotes by remember { mutableStateOf(resource.showNotes) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
        val colorScheme = MaterialTheme.colorScheme
        val isOled = colorScheme.background == Color.Black

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Настройка ресурса", fontWeight = FontWeight.Black) },
                    navigationIcon = {
                        IconButton(onClick = {
                            focusManager.clearFocus()
                            onDismiss()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = if (hazeState != null && !isOled) Color.Transparent else colorScheme.surface
                    )
                )
            },
            containerColor = if (hazeState != null && !isOled) Color.Transparent else colorScheme.background,
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) { focusManager.clearFocus() }
                .run {
                    if (hazeState != null && !isOled) {
                        hazeEffect(state = hazeState) {
                            style = HazeStyle(blurRadius = 24.dp, tints = listOf(HazeTint(Color.Black.copy(alpha = 0.2f))))
                        }
                    } else this
                }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it },
                    label = { Text("Ссылка (URL)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    placeholder = { Text("https://...") }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = current,
                        onValueChange = { current = it },
                        label = { Text("Текущее") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = max,
                        onValueChange = { max = it },
                        label = { Text("Максимум") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Short Rest
                Card(
                    colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WbSunny, null, tint = Color(0xFFFFB74D), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Короткий отдых", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = shortRest == "all", onCheckedChange = { if (it) shortRest = "all" else shortRest = "0" })
                                Text("Все")
                            }
                        }
                        if (shortRest != "all") {
                            OutlinedTextField(
                                value = shortRest,
                                onValueChange = { shortRest = it },
                                label = { Text("Восстановить") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                // Long Rest
                Card(
                    colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NightsStay, null, tint = Color(0xFF90CAF9), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Длинный отдых", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = longRest == "all", onCheckedChange = { if (it) longRest = "all" else longRest = "0" })
                                Text("Все")
                            }
                        }
                        if (longRest != "all") {
                            OutlinedTextField(
                                value = longRest,
                                onValueChange = { longRest = it },
                                label = { Text("Восстановить") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, null, tint = colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Заметки", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = showNotes, onCheckedChange = { showNotes = it })
                                Text("Отображать")
                            }
                        }
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Заметка ресурса") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            minLines = 2
                        )
                    }
                }

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        val tag = "[Ресурс: $name | cur=$current | max=$max | sr=$shortRest | lr=$longRest${if (link.isNotEmpty()) " | link=$link" else ""}${if (notes.isNotEmpty()) " | notes=$notes" else ""}${if (showNotes) " | showNotes=true" else ""}]"
                        onSave(tag)
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Сохранить", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }
    }
}

private fun updateResourceTag(
    resource: DynamicContentBlock.Resource,
    name: String = resource.name,
    current: String = resource.current,
    max: String = resource.max,
    sr: String = resource.shortRest,
    lr: String = resource.longRest,
    link: String? = resource.link,
    notes: String = resource.notes,
    showNotes: Boolean = resource.showNotes
): String {
    val linkPart = if (link != null) " | link=$link" else ""
    val notesPart = if (notes.isNotEmpty()) " | notes=$notes" else ""
    val showNotesPart = if (showNotes) " | showNotes=true" else ""
    return "[Ресурс: $name | cur=$current | max=$max | sr=$sr | lr=$lr$linkPart$notesPart$showNotesPart]"
}
