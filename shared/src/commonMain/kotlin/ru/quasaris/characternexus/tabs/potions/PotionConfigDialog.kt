package ru.quasaris.characternexus.tabs.potions

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
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
import ru.quasaris.characternexus.ui.theme.hazePopover
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.ui.editors.DamageTypeMultiSelect
import ru.quasaris.characternexus.ui.editors.SpellCardSectionTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PotionConfigDialog(
    initialPotion: PotionState,
    onDismiss: () -> Unit,
    onSave: (PotionState) -> Unit,
    onDelete: (PotionState) -> Unit,
    forceBlurEnabled: Boolean = false,
    settingsViewModel: SettingsViewModel? = null,
    hazeState: HazeState? = null,
    popupHazeState: HazeState? = null,
    isDesktop: Boolean = false
) {
    var state by remember { mutableStateOf(initialPotion) }
    
    LaunchedEffect(state) {
        onSave(state)
    }

    val focusManager = LocalFocusManager.current
    val blurRadius = rememberEffectiveBlurRadius(settingsViewModel)
    
    val handleDismiss = {
        focusManager.clearFocus()
        onDismiss()
    }
    
    var sliderStepText by remember { mutableStateOf(initialPotion.quantity.sliderStep?.toString() ?: "") }
    var colorText by remember { mutableStateOf(initialPotion.colorHex) }
    var shortRestAll by remember { mutableStateOf(initialPotion.quantity.shortRest.lowercase() == "all" || initialPotion.quantity.shortRest.lowercase() == "все") }
    var longRestAll by remember { mutableStateOf(initialPotion.quantity.longRest.lowercase() == "all" || initialPotion.quantity.longRest.lowercase() == "все") }
    var dawnRestAll by remember { mutableStateOf(initialPotion.quantity.dawnRest.lowercase() == "all" || initialPotion.quantity.dawnRest.lowercase() == "все") }
    val isPremium by settingsViewModel?.isPremium?.collectAsState() ?: remember { mutableStateOf(true) }

    if (isDesktop) {
        PotionConfigDialogContent(
            state = state,
            onStateChange = { state = it },
            sliderStepText = sliderStepText,
            onSliderStepTextChange = { sliderStepText = it },
            colorText = colorText,
            onColorTextChange = { colorText = it },
            shortRestAll = shortRestAll,
            onShortRestAllChange = { shortRestAll = it },
            longRestAll = longRestAll,
            onLongRestAllChange = { longRestAll = it },
            dawnRestAll = dawnRestAll,
            onDawnRestAllChange = { dawnRestAll = it },
            isPremium = isPremium,
            onDismiss = handleDismiss,
            onDelete = { potionToDelete ->
                onDelete(potionToDelete)
                onDismiss()
            },
            forceBlurEnabled = forceBlurEnabled,
            hazeState = popupHazeState ?: hazeState,
            blurRadius = blurRadius,
            settingsViewModel = settingsViewModel,
            isNew = initialPotion.name.isBlank(),
            isDesktop = isDesktop
        )
    } else {
        Dialog(
            onDismissRequest = handleDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            DialogDimStyle(0f)
            PotionConfigDialogContent(
                state = state,
                onStateChange = { state = it },
                sliderStepText = sliderStepText,
                onSliderStepTextChange = { sliderStepText = it },
                colorText = colorText,
                onColorTextChange = { colorText = it },
                shortRestAll = shortRestAll,
                onShortRestAllChange = { shortRestAll = it },
                longRestAll = longRestAll,
                onLongRestAllChange = { longRestAll = it },
                dawnRestAll = dawnRestAll,
                onDawnRestAllChange = { dawnRestAll = it },
                isPremium = isPremium,
                onDismiss = handleDismiss,
                onDelete = { potionToDelete ->
                    onDelete(potionToDelete)
                    onDismiss()
                },
                forceBlurEnabled = forceBlurEnabled,
                hazeState = popupHazeState ?: hazeState,
                blurRadius = blurRadius,
                settingsViewModel = settingsViewModel,
                isNew = initialPotion.name.isBlank(),
                isDesktop = isDesktop
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PotionConfigDialogContent(
    state: PotionState,
    onStateChange: (PotionState) -> Unit,
    sliderStepText: String,
    onSliderStepTextChange: (String) -> Unit,
    colorText: String,
    onColorTextChange: (String) -> Unit,
    shortRestAll: Boolean,
    onShortRestAllChange: (Boolean) -> Unit,
    longRestAll: Boolean,
    onLongRestAllChange: (Boolean) -> Unit,
    dawnRestAll: Boolean,
    onDawnRestAllChange: (Boolean) -> Unit,
    isPremium: Boolean,
    onDismiss: () -> Unit,
    onDelete: (PotionState) -> Unit,
    forceBlurEnabled: Boolean,
    hazeState: HazeState?,
    blurRadius: androidx.compose.ui.unit.Dp = 24.dp,
    settingsViewModel: SettingsViewModel? = null,
    isNew: Boolean = false,
    isDesktop: Boolean = false
) {
    BackHandler(onBack = onDismiss)
    val colorScheme = MaterialTheme.colorScheme
    val isOled = colorScheme.background == Color.Black
    val masterBlurEnabled by settingsViewModel?.masterBlurEnabled?.collectAsState() ?: remember { mutableStateOf(true) }
    val focusManager = LocalFocusManager.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

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
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .hazePopover(
                        state = hazeState,
                        blurRadius = blurRadius,
                        forceBlurEnabled = forceBlurEnabled,
                        isOled = isOled
                    )
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
                        title = { Text("Настройка зелья", fontWeight = FontWeight.Black) },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Закрыть")
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = if (forceBlurEnabled && !isOled && hazeState != null && !showDeleteConfirm) Color.Transparent.copy(alpha = 0.0f) else colorScheme.surface
                        )
                    )
                },
                containerColor = if (forceBlurEnabled && !isOled && hazeState != null && !showDeleteConfirm) Color.Transparent.copy(alpha = 0.0f) else colorScheme.background
            ) { paddingValues ->
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
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
                            value = state.formula,
                            onValueChange = { onStateChange(state.copy(formula = it)) },
                            label = { Text("Формула (напр. 2d4+2)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Column {
                            SpellCardSectionTitle("ТИП ФОРМУЛЫ")
                            DamageTypeMultiSelect(
                                selectedTypes = state.damageTypes,
                                onToggle = { type ->
                                    val newList = if (state.damageTypes.contains(type)) state.damageTypes - type else state.damageTypes + type
                                    onStateChange(state.copy(damageTypes = newList))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = "Тип формулы"
                            )
                        }

                        Column {
                            SpellCardSectionTitle("РЕДКОСТЬ")
                            var rarityExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = state.rarity.displayName,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Редкость") },
                                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                                    modifier = Modifier.fillMaxWidth().clickable { rarityExpanded = true },
                                    enabled = false,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Box(modifier = Modifier.matchParentSize().clickable { rarityExpanded = true })
                                DropdownMenu(
                                    expanded = rarityExpanded,
                                    onDismissRequest = { rarityExpanded = false }) {
                                    PotionRarity.entries.forEach { rarity ->
                                        DropdownMenuItem(
                                            text = { Text(rarity.displayName) },
                                            onClick = {
                                                onStateChange(state.copy(rarity = rarity))
                                                rarityExpanded = false
                                            })
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = state.description,
                            onValueChange = { onStateChange(state.copy(description = it)) },
                            label = { Text("Описание") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            shape = RoundedCornerShape(8.dp)
                        )

                        HorizontalDivider()

                        // Icon selection
                        Text("Иконка", fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            (1..4).forEach { index ->
                                val iconRes = when (index) {
                                    1 -> Res.drawable.small_potion
                                    2 -> Res.drawable.potion
                                    3 -> Res.drawable.big_potion
                                    4 -> Res.drawable.huge_potion
                                    else -> Res.drawable.small_potion
                                }
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (state.iconIndex == index) colorScheme.primaryContainer else colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                        .border(
                                            width = 2.dp,
                                            color = if (state.iconIndex == index) colorScheme.primary else Color.Transparent,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { onStateChange(state.copy(iconIndex = index)) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(iconRes),
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        tint = if (state.iconIndex == index) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Color Selection
                        OutlinedTextField(
                            value = colorText,
                            onValueChange = onColorTextChange,
                            label = { Text("Цвет (HEX)") },
                            placeholder = { Text("RRGGBB или RRGGBBAA") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            trailingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(parseColor(colorText))
                                        .border(1.dp, colorScheme.outline, RoundedCornerShape(4.dp))
                                )
                            }
                        )

                        HorizontalDivider()

                        Text("Количество", fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = state.quantity.current,
                                onValueChange = { onStateChange(state.copy(quantity = state.quantity.copy(current = it))) },
                                label = { Text("Текущее") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(8.dp)
                            )
                            OutlinedTextField(
                                value = state.quantity.max,
                                onValueChange = { onStateChange(state.copy(quantity = state.quantity.copy(max = it))) },
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
                                            if (it) onStateChange(state.copy(quantity = state.quantity.copy(shortRest = "all")))
                                            else onStateChange(state.copy(quantity = state.quantity.copy(shortRest = "0")))
                                        }
                                    )
                                }
                                if (!shortRestAll) {
                                    OutlinedTextField(
                                        value = if (state.quantity.shortRest.lowercase() == "all" || state.quantity.shortRest.lowercase() == "все") "" else state.quantity.shortRest,
                                        onValueChange = { onStateChange(state.copy(quantity = state.quantity.copy(shortRest = it))) },
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
                                            if (it) onStateChange(state.copy(quantity = state.quantity.copy(longRest = "all")))
                                            else onStateChange(state.copy(quantity = state.quantity.copy(longRest = "0")))
                                        }
                                    )
                                }
                                if (!longRestAll) {
                                    OutlinedTextField(
                                        value = if (state.quantity.longRest.lowercase() == "all" || state.quantity.longRest.lowercase() == "все") "" else state.quantity.longRest,
                                        onValueChange = { onStateChange(state.copy(quantity = state.quantity.copy(longRest = it))) },
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
                                            if (it) onStateChange(state.copy(quantity = state.quantity.copy(dawnRest = "all")))
                                            else onStateChange(state.copy(quantity = state.quantity.copy(dawnRest = "0")))
                                        }
                                    )
                                }
                                if (!dawnRestAll) {
                                    OutlinedTextField(
                                        value = if (state.quantity.dawnRest.lowercase() == "all" || state.quantity.dawnRest.lowercase() == "все") "" else state.quantity.dawnRest,
                                        onValueChange = { onStateChange(state.copy(quantity = state.quantity.copy(dawnRest = it))) },
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
                                        checked = state.quantity.useSlider,
                                        onCheckedChange = { onStateChange(state.copy(quantity = state.quantity.copy(useSlider = it))) }
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

                        Spacer(modifier = Modifier.height(32.dp))

                        if (!isNew) {
                            OutlinedButton(
                                onClick = { showDeleteConfirm = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                border = BorderStroke(1.dp, Color.Red),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Удалить")
                            }
                        }

                        Spacer(modifier = Modifier.height(80.dp))
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.surfaceVariant.compositeOver(colorScheme.background),
                            contentColor = colorScheme.onSurfaceVariant
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp,
                            focusedElevation = 0.dp,
                            hoveredElevation = 0.dp
                        )
                    ) {
                        Text("Закрыть", fontSize = 16.sp, fontWeight = FontWeight.Medium)
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
                title = "Удалить зелье?",
                settingsViewModel = settingsViewModel
            )
        }
    }
}

private fun parseColor(hex: String): Color {
    return try {
        val cleanHex = hex.removePrefix("#")
        when (cleanHex.length) {
            6 -> Color(
                red = cleanHex.substring(0, 2).toInt(16),
                green = cleanHex.substring(2, 4).toInt(16),
                blue = cleanHex.substring(4, 6).toInt(16)
            )
            8 -> Color(
                red = cleanHex.substring(0, 2).toInt(16),
                green = cleanHex.substring(2, 4).toInt(16),
                blue = cleanHex.substring(4, 6).toInt(16),
                alpha = cleanHex.substring(6, 8).toInt(16)
            )
            else -> Color.Gray
        }
    } catch (e: Exception) {
        Color.Gray
    }
}
