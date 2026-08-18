package ru.quasaris.characternexus.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.backend.calculateModifier
import ru.quasaris.characternexus.backend.evaluateFormula
import ru.quasaris.characternexus.backend.getProficiencyBonus
import ru.quasaris.characternexus.tabs.attacks.DiceIcon
import ru.quasaris.characternexus.backend.parseFormulaParts
import ru.quasaris.characternexus.backend.DicePart
import ru.quasaris.characternexus.ui.outerShadow
import ru.quasaris.characternexus.ui.DeleteConfirmationDialog

@Composable
fun ResourceBlock(
    resource: DynamicContentBlock.Resource,
    statsMap: Map<String, String>,
    onUpdate: (DynamicContentBlock.Resource) -> Unit,
    hazeState: HazeState? = null,
    onDeleteRequest: () -> Unit,
    forceBlurEnabled: Boolean = false,
    blurDynamicFields: Boolean = true,
    blurPopups: Boolean = false,
    settingsViewModel: ru.quasaris.characternexus.backend.SettingsViewModel? = null
) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    var showConfig by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var infoIconPosition by remember { mutableStateOf(Offset.Zero) }

    val curValue = resource.current.toIntOrNull() ?: 0
    val maxValue = evaluateFormula(resource.max, statsMap)

    val level = statsMap["level"] ?: "1"
    val pb = getProficiencyBonus(level)

    val canIncrement = curValue < maxValue || resource.max == "0"
    val canDecrement = curValue > 0
    
    val useHaze = hazeState != null && blurDynamicFields

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .outerShadow(
                shape = RoundedCornerShape(16.dp),
                blur = 2.dp,
                offsetY = 1.dp
            )
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (useHaze) 0.6f else 0.4f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                showConfig = true
            }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = resource.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Rests info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    if (resource.shortRest != "0") {
                        RestIndicator(
                            isShort = true,
                            value = resource.shortRest,
                            statsMap = statsMap,
                            proficiencyBonus = pb
                        )
                    }
                    if (resource.longRest != "0") {
                        RestIndicator(
                            isShort = false,
                            value = resource.longRest,
                            statsMap = statsMap,
                            proficiencyBonus = pb
                        )
                    }
                    if (resource.dawnRest != "0") {
                        RestIndicator(
                            isShort = false,
                            isDawn = true,
                            value = resource.dawnRest,
                            statsMap = statsMap,
                            proficiencyBonus = pb
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Info button
                if (resource.notes.isNotEmpty() && resource.showNotes) {
                    IconButton(
                        onClick = { showInfo = true },
                        modifier = Modifier
                            .size(36.dp)
                            .onGloballyPositioned { coords ->
                                infoIconPosition = coords.positionInWindow()
                            }
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Info",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Link button
                if (!resource.link.isNullOrBlank()) {
                    IconButton(
                        onClick = {
                            try {
                                val trimmedLink = resource.link.trim()
                                // Automatic link scheming
                                val formattedLink = if (!trimmedLink.startsWith("http://") && !trimmedLink.startsWith("https://")) {
                                    "https://$trimmedLink"
                                } else {
                                    trimmedLink
                                }
                                uriHandler.openUri(formattedLink)
                            } catch (e: Exception) {
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Link,
                            contentDescription = "Link",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Minus button
                ResourceActionButton(
                    icon = Icons.Default.Remove,
                    enabled = canDecrement,
                    onClick = { onUpdate(resource.copy(current = (curValue - 1).coerceAtLeast(0).toString())) }
                )

                // Current/Max display (Square)
                Box(
                    modifier = Modifier
                        .size(height = 42.dp, width = 64.dp) // Adjusted to be more "square-ish" but fit numbers
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (resource.max != "0") "$curValue/$maxValue" else curValue.toString(),
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                    )
                }

                // Plus button
                ResourceActionButton(
                    icon = Icons.Default.Add,
                    enabled = canIncrement,
                    onClick = { onUpdate(resource.copy(current = (curValue + 1).toString())) }
                )
            }
        }
    }

    if (showConfig) {
        ResourceConfigDialog(
            resource = resource,
            onDismiss = { showConfig = false },
            onSave = {
                onUpdate(it)
                showConfig = false
            },
            onDelete = {
                onDeleteRequest()
                showConfig = false
            },
            hazeState = hazeState,
            forceBlurEnabled = forceBlurEnabled,
            blurDynamicFields = blurDynamicFields,
            settingsViewModel = settingsViewModel
        )
    }

    if (showInfo) {
        ResourceInfoPopover(
            title = resource.name,
            notes = resource.notes,
            anchorPosition = infoIconPosition,
            onDismiss = { showInfo = false },
            hazeState = hazeState,
            forceBlurEnabled = blurPopups
        )
    }
}

@Composable
fun ResourceActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(36.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
fun RestIndicator(
    isShort: Boolean,
    isDawn: Boolean = false,
    value: String,
    statsMap: Map<String, String>,
    proficiencyBonus: Int
) {
    val icon = when {
        isDawn -> Icons.Outlined.WbTwilight
        isShort -> Icons.Outlined.WbSunny
        else -> Icons.Outlined.NightsStay
    }
    val color = when {
        isDawn -> Color(0xFFCE93D8)
        isShort -> Color(0xFFFFB300)
        else -> Color(0xFF42A5F5)
    }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = color
        )
        if (value.lowercase() != "all" && value.lowercase() != "все") {
            val (flat, dice) = parseFormulaParts(value, stats = statsMap)
            if (dice.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    dice.forEach { die ->
                        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                            DiceIcon(die)
                        }
                    }
                    if (flat != 0) {
                        Text(
                            text = if (flat > 0) "+$flat" else flat.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            color = color.copy(alpha = 0.9f)
                        )
                    }
                }
            } else {
                val evaluatedValue = evaluateFormula(value, statsMap)
                Text(
                    text = evaluatedValue.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = color.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceConfigDialog(
    resource: DynamicContentBlock.Resource,
    onDismiss: () -> Unit,
    onSave: (DynamicContentBlock.Resource) -> Unit,
    onDelete: (DynamicContentBlock.Resource) -> Unit,
    hazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false,
    blurDynamicFields: Boolean = true,
    settingsViewModel: ru.quasaris.characternexus.backend.SettingsViewModel? = null
) {
    var state by remember { mutableStateOf(resource) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    var shortRestAll by remember { mutableStateOf(resource.shortRest.lowercase() == "all" || resource.shortRest.lowercase() == "все") }
    var longRestAll by remember { mutableStateOf(resource.longRest.lowercase() == "all" || resource.longRest.lowercase() == "все") }
    var dawnRestAll by remember { mutableStateOf(resource.dawnRest.lowercase() == "all" || resource.dawnRest.lowercase() == "все") }

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
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CompositionLocalProvider(LocalTextToolbar provides emptyTextToolbar) {
            val colorScheme = MaterialTheme.colorScheme
            val isOled = colorScheme.background == Color.Black

            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text("Настройка ресурса", fontWeight = FontWeight.Black) },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Закрыть")
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = if (forceBlurEnabled && !isOled) Color.Transparent.copy(alpha = 0.0f) else colorScheme.surface
                        )
                    )
                },
                containerColor = if (forceBlurEnabled && !isOled) Color.Transparent.copy(alpha = 0.0f) else colorScheme.background,
                modifier = Modifier.run {
                    // No more hazeEffect for the dialog background
                    this
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(androidx.compose.foundation.rememberScrollState())
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
                        onClick = { onSave(state) },
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

@OptIn(ExperimentalHazeApi::class)
@Composable
fun ResourceInfoPopover(
    title: String,
    notes: String,
    anchorPosition: Offset,
    onDismiss: () -> Unit,
    hazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false
) {
    val colorScheme = MaterialTheme.colorScheme
    val isOled = colorScheme.background == Color.Black

    Popup(
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Surface(
            modifier = Modifier
                .padding(8.dp)
                .widthIn(max = 260.dp)
                .run {
                    if (forceBlurEnabled && hazeState != null && !isOled) {
                        this.clip(RoundedCornerShape(16.dp))
                            .hazeEffect(state = hazeState) {
                                style = HazeStyle(blurRadius = 24.dp, tints = listOf(HazeTint(colorScheme.surface.copy(alpha = 0.1f))))
                                inputScale = HazeInputScale.Fixed(0.6f)
                            }
                    } else this
                }
                .clickable { onDismiss() },
            shape = RoundedCornerShape(16.dp),
            color = if (isOled) Color.Black else colorScheme.surface.copy(alpha = if (forceBlurEnabled) 0.1f else 1.0f),
            tonalElevation = 8.dp,
            border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = if (isOled) 0.3f else 0.15f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = notes.ifBlank { "Нет описания" },
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    color = colorScheme.onSurface
                )
            }
        }
    }
}
