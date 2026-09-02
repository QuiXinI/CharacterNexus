package ru.quasaris.characternexus.tabs

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.chrisbanes.haze.*
import org.jetbrains.compose.resources.painterResource
import ru.quasaris.characternexus.backend.SettingsViewModel
import characternexus.shared.generated.resources.*
import ru.quasaris.characternexus.ui.DeleteConfirmationDialog
import ru.quasaris.characternexus.ui.BackHandler
import ru.quasaris.characternexus.ui.DialogDimStyle
import ru.quasaris.characternexus.ui.util.PayWall
import ru.quasaris.characternexus.ui.theme.rememberEffectiveBlurRadius

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceConfigDialog(
    resource: DynamicContentBlock.Resource,
    onDismiss: () -> Unit,
    onSave: (DynamicContentBlock.Resource) -> Unit,
    onDelete: (DynamicContentBlock.Resource) -> Unit,
    forceBlurEnabled: Boolean = false,
    settingsViewModel: SettingsViewModel? = null,
    onFullscreenDialogOpenChange: (Boolean) -> Unit = {},
    isDesktop: Boolean = false,
    hazeState: HazeState? = null,
    popupHazeState: HazeState? = null,
    isNested: Boolean = false,
    asOverlay: Boolean = false
) {
    var state by remember { mutableStateOf(resource) }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val blurRadius = rememberEffectiveBlurRadius(settingsViewModel)
    
    val handleDismiss = {
        focusManager.clearFocus()
        onDismiss()
    }
    
    val handleSave = {
        focusManager.clearFocus()
        onSave(state)
    }

    var sliderStepText by remember { mutableStateOf(resource.sliderStep?.toString() ?: "") }
    var shortRestAll by remember { mutableStateOf(resource.shortRest.lowercase() == "all" || resource.shortRest.lowercase() == "все") }
    var longRestAll by remember { mutableStateOf(resource.longRest.lowercase() == "all" || resource.longRest.lowercase() == "все") }
    var dawnRestAll by remember { mutableStateOf(resource.dawnRest.lowercase() == "all" || resource.dawnRest.lowercase() == "все") }
    val isPremium by settingsViewModel?.isPremium?.collectAsState() ?: remember { mutableStateOf(true) }

    val currentOnFullscreenDialogOpenChange by rememberUpdatedState(onFullscreenDialogOpenChange)

    DisposableEffect(Unit) {
        currentOnFullscreenDialogOpenChange(true)
        onDispose {
            currentOnFullscreenDialogOpenChange(false)
        }
    }
    
    if (isDesktop || asOverlay) {
        ResourceConfigDialogContent(
            state = state,
            onStateChange = { state = it },
            sliderStepText = sliderStepText,
            onSliderStepTextChange = { sliderStepText = it },
            shortRestAll = shortRestAll,
            onShortRestAllChange = { shortRestAll = it },
            longRestAll = longRestAll,
            onLongRestAllChange = { longRestAll = it },
            dawnRestAll = dawnRestAll,
            onDawnRestAllChange = { dawnRestAll = it },
            isPremium = isPremium,
            onDismiss = handleDismiss,
            onSave = {
                state = state.copy(sliderStep = sliderStepText.toDoubleOrNull())
                handleSave()
            },
            onDelete = { resourceToDelete ->
                onDelete(resourceToDelete)
                onDismiss()
            },
            forceBlurEnabled = forceBlurEnabled,
            hazeState = hazeState,
            blurRadius = blurRadius,
            isNested = isNested,
            isDesktop = isDesktop,
            asOverlay = asOverlay,
            settingsViewModel = settingsViewModel
        )
    } else {
        Dialog(
            onDismissRequest = handleDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            DialogDimStyle(0f)
            ResourceConfigDialogContent(
                state = state,
                onStateChange = { state = it },
                sliderStepText = sliderStepText,
                onSliderStepTextChange = { sliderStepText = it },
                shortRestAll = shortRestAll,
                onShortRestAllChange = { shortRestAll = it },
                longRestAll = longRestAll,
                onLongRestAllChange = { longRestAll = it },
                dawnRestAll = dawnRestAll,
                onDawnRestAllChange = { dawnRestAll = it },
                isPremium = isPremium,
                onDismiss = handleDismiss,
                onSave = {
                    state = state.copy(sliderStep = sliderStepText.toDoubleOrNull())
                    handleSave()
                },
                onDelete = { resourceToDelete ->
                    onDelete(resourceToDelete)
                    onDismiss()
                },
                forceBlurEnabled = forceBlurEnabled,
                hazeState = popupHazeState ?: hazeState,
                blurRadius = blurRadius,
                isNested = isNested,
                isDesktop = isDesktop,
                settingsViewModel = settingsViewModel
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceConfigDialogContent(
    state: DynamicContentBlock.Resource,
    onStateChange: (DynamicContentBlock.Resource) -> Unit,
    sliderStepText: String,
    onSliderStepTextChange: (String) -> Unit,
    shortRestAll: Boolean,
    onShortRestAllChange: (Boolean) -> Unit,
    longRestAll: Boolean,
    onLongRestAllChange: (Boolean) -> Unit,
    dawnRestAll: Boolean,
    onDawnRestAllChange: (Boolean) -> Unit,
    isPremium: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onDelete: (DynamicContentBlock.Resource) -> Unit,
    forceBlurEnabled: Boolean,
    hazeState: HazeState?,
    blurRadius: androidx.compose.ui.unit.Dp = 24.dp,
    isNested: Boolean = false,
    isDesktop: Boolean = false,
    asOverlay: Boolean = false,
    settingsViewModel: SettingsViewModel? = null
) {
    BackHandler(onBack = onDismiss)
    val colorScheme = MaterialTheme.colorScheme
    val isOled = colorScheme.background == Color.Black
    val masterBlurEnabled by settingsViewModel?.masterBlurEnabled?.collectAsState() ?: remember { mutableStateOf(true) }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val innerContent = @Composable {
        ResourceConfigDialogInner(
            state = state,
            onStateChange = onStateChange,
            sliderStepText = sliderStepText,
            onSliderStepTextChange = onSliderStepTextChange,
            shortRestAll = shortRestAll,
            onShortRestAllChange = onShortRestAllChange,
            longRestAll = longRestAll,
            onLongRestAllChange = onLongRestAllChange,
            dawnRestAll = dawnRestAll,
            onDawnRestAllChange = onDawnRestAllChange,
            isPremium = isPremium,
            onSave = onSave,
            onDelete = { showDeleteConfirm = true }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .run {
                    if (showDeleteConfirm && masterBlurEnabled) {
                        this.blur(blurRadius)
                    } else this
                }
        ) {
            if (asOverlay) {
                // NESTED OVERLAY branch (matches MagicBonusSettingsContent asOverlay)
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text("Настройка ресурса", fontWeight = FontWeight.Black) },
                            navigationIcon = {
                                IconButton(onClick = onDismiss) {
                                    Icon(Icons.Default.Close, contentDescription = "Закрыть")
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = if (masterBlurEnabled && !showDeleteConfirm) Color.Transparent else colorScheme.surface
                            )
                        )
                    },
                    containerColor = if (masterBlurEnabled && !showDeleteConfirm) Color.Transparent else colorScheme.background
                ) { paddingValues ->
                    Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                        innerContent()
                    }
                }
            } else if (isNested && !isDesktop) {
                // MOBILE NESTED branch (if we want Card look, otherwise same as STANDALONE)
                // But user wants it full-screen on mobile, so let's use the STANDALONE pattern.
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text("Настройка ресурса", fontWeight = FontWeight.Black) },
                            navigationIcon = {
                                IconButton(onClick = onDismiss) {
                                    Icon(Icons.Default.Close, contentDescription = "Закрыть")
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = colorScheme.surface
                            )
                        )
                    },
                    containerColor = colorScheme.background
                ) { paddingValues ->
                    Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                        innerContent()
                    }
                }
            } else {
                // STANDALONE or DESKTOP branch
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .run {
                            if (forceBlurEnabled && hazeState != null && !isOled && !isNested) {
                                this.hazeEffect(state = hazeState) {
                                    style = HazeStyle(
                                        blurRadius = blurRadius,
                                        tints = listOf(HazeTint(Color.Black.copy(alpha = 0.2f)))
                                    )
                                }
                            } else this
                        }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDrag = { change, _ ->
                                    change.consume()
                                    focusManager.clearFocus()
                                }
                            )
                        }
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) { focusManager.clearFocus() },
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text("Настройка ресурса", fontWeight = FontWeight.Black) },
                            navigationIcon = {
                                IconButton(onClick = onDismiss) {
                                    Icon(Icons.Default.Close, contentDescription = "Закрыть")
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = if (forceBlurEnabled && !isOled && hazeState != null && !isNested && !showDeleteConfirm) Color.Transparent.copy(alpha = 0.0f) else colorScheme.surface
                            )
                        )
                    },
                    containerColor = if (forceBlurEnabled && !isOled && hazeState != null && !isNested && !showDeleteConfirm) Color.Transparent.copy(alpha = 0.0f) else colorScheme.background
                ) { paddingValues ->
                    Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                        innerContent()
                    }
                }
            }
        }

        if (showDeleteConfirm) {
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

@Composable
fun ResourceConfigDialogInner(
    state: DynamicContentBlock.Resource,
    onStateChange: (DynamicContentBlock.Resource) -> Unit,
    sliderStepText: String,
    onSliderStepTextChange: (String) -> Unit,
    shortRestAll: Boolean,
    onShortRestAllChange: (Boolean) -> Unit,
    longRestAll: Boolean,
    onLongRestAllChange: (Boolean) -> Unit,
    dawnRestAll: Boolean,
    onDawnRestAllChange: (Boolean) -> Unit,
    isPremium: Boolean,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
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
                onValueChange = { onStateChange(state.copy(name = it)) },
                label = { Text("Название") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )

            OutlinedTextField(
                value = state.link ?: "",
                onValueChange = { onStateChange(state.copy(link = it.ifBlank { null })) },
                label = { Text("Ссылка (опционально)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.current,
                    onValueChange = { onStateChange(state.copy(current = it)) },
                    label = { Text("Текущее") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = state.max,
                    onValueChange = { onStateChange(state.copy(max = it)) },
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
                                onShortRestAllChange(it)
                                if (it) onStateChange(state.copy(shortRest = "all"))
                                else onStateChange(state.copy(shortRest = "0"))
                            }
                        )
                    }
                    if (!shortRestAll) {
                        OutlinedTextField(
                            value = if (state.shortRest.lowercase() == "all" || state.shortRest.lowercase() == "все") "" else state.shortRest,
                            onValueChange = { onStateChange(state.copy(shortRest = it)) },
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
                                onLongRestAllChange(it)
                                if (it) onStateChange(state.copy(longRest = "all"))
                                else onStateChange(state.copy(longRest = "0"))
                            }
                        )
                    }
                    if (!longRestAll) {
                        OutlinedTextField(
                            value = if (state.longRest.lowercase() == "all" || state.longRest.lowercase() == "все") "" else state.longRest,
                            onValueChange = { onStateChange(state.copy(longRest = it)) },
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
                                onDawnRestAllChange(it)
                                if (it) onStateChange(state.copy(dawnRest = "all"))
                                else onStateChange(state.copy(dawnRest = "0"))
                            }
                        )
                    }
                    if (!dawnRestAll) {
                        OutlinedTextField(
                            value = if (state.dawnRest.lowercase() == "all" || state.dawnRest.lowercase() == "все") "" else state.dawnRest,
                            onValueChange = { onStateChange(state.copy(dawnRest = it)) },
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
                            onCheckedChange = { onStateChange(state.copy(useSlider = it)) }
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
                            onValueChange = onSliderStepTextChange,
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
                Switch(checked = state.showNotes, onCheckedChange = { onStateChange(state.copy(showNotes = it)) })
            }

            OutlinedTextField(
                value = state.notes,
                onValueChange = { onStateChange(state.copy(notes = it)) },
                label = { Text("Поле для заметок") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedButton(
                onClick = onDelete,
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
            onClick = onSave,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Сохранить", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}
