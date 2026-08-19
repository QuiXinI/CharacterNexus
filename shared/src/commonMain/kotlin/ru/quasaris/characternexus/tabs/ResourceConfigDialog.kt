package ru.quasaris.characternexus.tabs

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.quasaris.characternexus.backend.SettingsViewModel
import ru.quasaris.characternexus.ui.DeleteConfirmationDialog
import ru.quasaris.characternexus.ui.DialogDimStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceConfigDialog(
    resource: DynamicContentBlock.Resource,
    onDismiss: () -> Unit,
    onSave: (DynamicContentBlock.Resource) -> Unit,
    onDelete: (DynamicContentBlock.Resource) -> Unit,
    forceBlurEnabled: Boolean = false,
    settingsViewModel: SettingsViewModel? = null,
    onFullscreenDialogOpenChange: (Boolean) -> Unit = {}
) {
    var state by remember { mutableStateOf(resource) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    var shortRestAll by remember { mutableStateOf(resource.shortRest.lowercase() == "all" || resource.shortRest.lowercase() == "все") }
    var longRestAll by remember { mutableStateOf(resource.longRest.lowercase() == "all" || resource.longRest.lowercase() == "все") }
    var dawnRestAll by remember { mutableStateOf(resource.dawnRest.lowercase() == "all" || resource.dawnRest.lowercase() == "все") }

    val blurRadiusVal by settingsViewModel?.blurRadius?.collectAsState() ?: remember { mutableStateOf(16) }
    val customBlurRadiusVal by settingsViewModel?.customBlurRadius?.collectAsState() ?: remember { mutableStateOf(16) }
    val targetBlurRadius = if (blurRadiusVal == -1) customBlurRadiusVal else blurRadiusVal
    
    val blurRadius by animateDpAsState(
        targetValue = if (showDeleteConfirm && forceBlurEnabled) targetBlurRadius.dp else 0.dp,
        animationSpec = tween(durationMillis = 300)
    )

    LaunchedEffect(Unit) {
        onFullscreenDialogOpenChange(true)
    }

    val emptyTextToolbar = remember {
        object : TextToolbar {
            override val status: TextToolbarStatus = TextToolbarStatus.Hidden
            override fun hide() {}
            override fun showMenu(
                rect: Rect,
                onCopyRequested: (() -> Unit)?,
                onPasteRequested: (() -> Unit)?,
                onCutRequested: (() -> Unit)?,
                onSelectAllRequested: (() -> Unit)?
            ) {
            }
        }
    }

    Dialog(
        onDismissRequest = {
            onFullscreenDialogOpenChange(false)
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        DialogDimStyle(0f)
        CompositionLocalProvider(LocalTextToolbar provides emptyTextToolbar) {
            val colorScheme = MaterialTheme.colorScheme
            val isOled = colorScheme.background == Color.Black

            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text("Настройка ресурса", fontWeight = FontWeight.Black) },
                        navigationIcon = {
                            IconButton(onClick = {
                                onFullscreenDialogOpenChange(false)
                                onDismiss()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Закрыть")
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = if (forceBlurEnabled && !isOled) Color.Transparent.copy(alpha = 0.0f) else colorScheme.surface
                        )
                    )
                },
                containerColor = if (forceBlurEnabled && !isOled) Color.Transparent.copy(alpha = 0.0f) else colorScheme.background,
                modifier = Modifier.blur(blurRadius)
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = state.name,
                            onValueChange = { state = state.copy(name = it) },
                            label = { Text("Название") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )

                        OutlinedTextField(
                            value = state.link ?: "",
                            onValueChange = { state = state.copy(link = it.ifBlank { null }) },
                            label = { Text("Ссылка (опционально)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = state.current,
                                onValueChange = { state = state.copy(current = it) },
                                label = { Text("Текущее") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(8.dp)
                            )
                            OutlinedTextField(
                                value = state.max,
                                onValueChange = { state = state.copy(max = it) },
                                label = { Text("Максимум") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }

                        // Short rest recovery
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Короткий отдых", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                                    Text("Все", style = MaterialTheme.typography.labelMedium)
                                    Switch(
                                        checked = shortRestAll,
                                        onCheckedChange = {
                                            shortRestAll = it
                                            if (it) state = state.copy(shortRest = "all")
                                        }
                                    )
                                }
                                if (!shortRestAll) {
                                    OutlinedTextField(
                                        value = if (state.shortRest.lowercase() == "all" || state.shortRest.lowercase() == "все") "" else state.shortRest,
                                        onValueChange = { state = state.copy(shortRest = it) },
                                        label = { Text("Восстановление") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }

                        // Long rest recovery
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Продолжительный отдых", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                                    Text("Все", style = MaterialTheme.typography.labelMedium)
                                    Switch(
                                        checked = longRestAll,
                                        onCheckedChange = {
                                            longRestAll = it
                                            if (it) state = state.copy(longRest = "all")
                                        }
                                    )
                                }
                                if (!longRestAll) {
                                    OutlinedTextField(
                                        value = if (state.longRest.lowercase() == "all" || state.longRest.lowercase() == "все") "" else state.longRest,
                                        onValueChange = { state = state.copy(longRest = it) },
                                        label = { Text("Восстановление") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }

                        // Dawn recovery
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Рассвет", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                                    Text("Все", style = MaterialTheme.typography.labelMedium)
                                    Switch(
                                        checked = dawnRestAll,
                                        onCheckedChange = {
                                            dawnRestAll = it
                                            if (it) state = state.copy(dawnRest = "all")
                                        }
                                    )
                                }
                                if (!dawnRestAll) {
                                    OutlinedTextField(
                                        value = if (state.dawnRest.lowercase() == "all" || state.dawnRest.lowercase() == "все") "" else state.dawnRest,
                                        onValueChange = { state = state.copy(dawnRest = it) },
                                        label = { Text("Восстановление") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Заметки ресурса", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                            Switch(checked = state.showNotes, onCheckedChange = { state = state.copy(showNotes = it) })
                        }

                        OutlinedTextField(
                            value = state.notes,
                            onValueChange = { state = state.copy(notes = it) },
                            label = { Text("Поле для заметок") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            shape = RoundedCornerShape(8.dp)
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Delete Button
                        OutlinedButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                            border = BorderStroke(1.dp, Color.Red),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Удалить")
                        }

                        Spacer(modifier = Modifier.height(80.dp))
                    }

                    Button(
                        onClick = {
                            onFullscreenDialogOpenChange(false)
                            onSave(state)
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Сохранить", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }

                    DeleteConfirmationDialog(
                        showDialog = showDeleteConfirm,
                        onDismiss = { showDeleteConfirm = false },
                        onConfirm = {
                            onFullscreenDialogOpenChange(false)
                            onDelete(state)
                            showDeleteConfirm = false
                        },
                        title = "Удалить ресурс?",
                        settingsViewModel = settingsViewModel
                    )
                }
            }
        }
    }
}
