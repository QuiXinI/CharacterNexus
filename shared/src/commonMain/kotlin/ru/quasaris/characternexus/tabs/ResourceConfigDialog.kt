package ru.quasaris.characternexus.tabs

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.draw.clip
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
import org.jetbrains.compose.resources.painterResource
import ru.quasaris.characternexus.backend.SettingsViewModel
import characternexus.shared.generated.resources.*
import ru.quasaris.characternexus.ui.DeleteConfirmationDialog
import ru.quasaris.characternexus.ui.BackHandler
import ru.quasaris.characternexus.ui.DialogDimStyle
import ru.quasaris.characternexus.ui.util.PayWall

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

    var sliderStepText by remember { mutableStateOf(resource.sliderStep?.toString() ?: "") }

    var shortRestAll by remember { mutableStateOf(resource.shortRest.lowercase() == "all" || resource.shortRest.lowercase() == "все") }
    var longRestAll by remember { mutableStateOf(resource.longRest.lowercase() == "all" || resource.longRest.lowercase() == "все") }
    var dawnRestAll by remember { mutableStateOf(resource.dawnRest.lowercase() == "all" || resource.dawnRest.lowercase() == "все") }

    val isPremium by settingsViewModel?.isPremium?.collectAsState() ?: remember { mutableStateOf(true) }

    val isOled = MaterialTheme.colorScheme.background == Color.Black
    val effectiveBlur = forceBlurEnabled && !isOled

    val currentOnFullscreenDialogOpenChange by rememberUpdatedState(onFullscreenDialogOpenChange)

    DisposableEffect(Unit) {
        currentOnFullscreenDialogOpenChange(true)
        onDispose {
            currentOnFullscreenDialogOpenChange(false)
        }
    }
    
    val blurRadiusVal by settingsViewModel?.blurRadius?.collectAsState() ?: remember { mutableStateOf(16) }
    val customBlurRadiusVal by settingsViewModel?.customBlurRadius?.collectAsState() ?: remember { mutableStateOf(16) }
    val targetBlurRadius = if (blurRadiusVal == -1) customBlurRadiusVal else blurRadiusVal
    
    val deleteConfirmBlurRadius by animateDpAsState(
        targetValue = if (showDeleteConfirm && forceBlurEnabled) targetBlurRadius.dp else 0.dp,
        animationSpec = tween(durationMillis = 300)
    )

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
        BackHandler(onBack = onDismiss)
        CompositionLocalProvider(LocalTextToolbar provides emptyTextToolbar) {
            val colorScheme = MaterialTheme.colorScheme

            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text("Настройка ресурса", fontWeight = FontWeight.Black) },
                        navigationIcon = {
                            IconButton(onClick = {
                                onDismiss()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Закрыть")
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = if (effectiveBlur) Color.Transparent else colorScheme.surface
                        )
                    )
                },
                containerColor = if (effectiveBlur) Color.Transparent else colorScheme.background,
                modifier = Modifier.blur(deleteConfirmBlurRadius)
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
                                    Text("Короткий отдых", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                                    Text("Все", style = MaterialTheme.typography.labelMedium, color = colorScheme.onSurfaceVariant)
                                    Switch(
                                        checked = shortRestAll,
                                        onCheckedChange = {
                                            shortRestAll = it
                                            if (it) state = state.copy(shortRest = "all")
                                            else state = state.copy(shortRest = "0")
                                        }
                                    )
                                }
                                if (!shortRestAll) {
                                    OutlinedTextField(
                                        value = if (state.shortRest.lowercase() == "all" || state.shortRest.lowercase() == "все") "" else state.shortRest,
                                        onValueChange = { state = state.copy(shortRest = it) },
                                        label = { Text("Восстановление") },
                                        placeholder = { Text("0 (по умолчанию)") },
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
                                    Text("Продолжительный отдых", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                                    Text("Все", style = MaterialTheme.typography.labelMedium, color = colorScheme.onSurfaceVariant)
                                    Switch(
                                        checked = longRestAll,
                                        onCheckedChange = {
                                            longRestAll = it
                                            if (it) state = state.copy(longRest = "all")
                                            else state = state.copy(longRest = "0")
                                        }
                                    )
                                }
                                if (!longRestAll) {
                                    OutlinedTextField(
                                        value = if (state.longRest.lowercase() == "all" || state.longRest.lowercase() == "все") "" else state.longRest,
                                        onValueChange = { state = state.copy(longRest = it) },
                                        label = { Text("Восстановление") },
                                        placeholder = { Text("0 (по умолчанию)") },
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
                                    Text("Рассвет", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                                    Text("Все", style = MaterialTheme.typography.labelMedium, color = colorScheme.onSurfaceVariant)
                                    Switch(
                                        checked = dawnRestAll,
                                        onCheckedChange = {
                                            dawnRestAll = it
                                            if (it) state = state.copy(dawnRest = "all")
                                            else state = state.copy(dawnRest = "0")
                                        }
                                    )
                                }
                                if (!dawnRestAll) {
                                    OutlinedTextField(
                                        value = if (state.dawnRest.lowercase() == "all" || state.dawnRest.lowercase() == "все") "" else state.dawnRest,
                                        onValueChange = { state = state.copy(dawnRest = it) },
                                        label = { Text("Восстановление") },
                                        placeholder = { Text("0 (по умолчанию)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }

                        // Slider setting
                        PayWall(isLocked = !isPremium) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Режим слайдера",
                                        modifier = Modifier.weight(1f),
                                        fontWeight = FontWeight.Bold,
                                        color = colorScheme.onSurface
                                    )
                                    Switch(
                                        checked = state.useSlider,
                                        onCheckedChange = { state = state.copy(useSlider = it) }
                                    )
                                }
                            }
                        }

                        // Resource Step setting
                        PayWall(isLocked = !isPremium) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        "Шаг изменения",
                                        fontWeight = FontWeight.Bold,
                                        color = colorScheme.onSurface,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    OutlinedTextField(
                                        value = sliderStepText,
                                        onValueChange = {
                                            sliderStepText = it
                                        },
                                        label = { Text("Значение шага") },
                                        modifier = Modifier.fillMaxWidth(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        placeholder = { Text("1.0 (по умолчанию)") },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Отображать заметки", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
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
                            val sanitizedState = state.copy(
                                sliderStep = sliderStepText.toDoubleOrNull()
                            )
                            onSave(sanitizedState)
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
