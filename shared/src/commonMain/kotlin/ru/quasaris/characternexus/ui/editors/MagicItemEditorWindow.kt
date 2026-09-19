package ru.quasaris.characternexus.ui.editors

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.draw.alpha
import dev.chrisbanes.haze.*
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.ui.*
import ru.quasaris.characternexus.backend.GameMagicItem
import ru.quasaris.characternexus.backend.MagicItemType
import org.jetbrains.compose.resources.painterResource
import characternexus.shared.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MagicItemEditorWindow(
    item: GameMagicItem,
    onDismiss: () -> Unit,
    onSave: (GameMagicItem) -> Unit,
    onDelete: (GameMagicItem) -> Unit,
    forceBlurEnabled: Boolean = false,
    settingsViewModel: ru.quasaris.characternexus.backend.SettingsViewModel? = null,
    isDesktop: Boolean = false,
    hazeState: HazeState? = null,
    popupHazeState: HazeState? = null
) {
    var state by remember { mutableStateOf(item) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    
    var englishNameError by remember { mutableStateOf<String?>(null) }
    val allowedCharsRegex = remember { Regex("^[a-zA-Z0-9'\\-._,() ]*$") }

    val handleDismiss = {
        focusManager.clearFocus()
        if (state.name?.isNotBlank() == true && state != item) {
            onSave(state)
        }
        onDismiss()
    }

    if (isDesktop) {
        MagicItemEditorContent(
            state = state,
            onStateChange = { state = it },
            onDismiss = handleDismiss,
            onDelete = { showDeleteConfirm = true },
            forceBlurEnabled = forceBlurEnabled,
            hazeState = hazeState,
            englishNameError = englishNameError,
            onEnglishNameErrorChange = { englishNameError = it },
            allowedCharsRegex = allowedCharsRegex
        )
    } else {
        Dialog(
            onDismissRequest = handleDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            MagicItemEditorContent(
                state = state,
                onStateChange = { state = it },
                onDismiss = handleDismiss,
                onDelete = { showDeleteConfirm = true },
                forceBlurEnabled = forceBlurEnabled,
                hazeState = popupHazeState ?: hazeState,
                englishNameError = englishNameError,
                onEnglishNameErrorChange = { englishNameError = it },
                allowedCharsRegex = allowedCharsRegex
            )
        }
    }

    DeleteConfirmationDialog(
        showDialog = showDeleteConfirm,
        onDismiss = { showDeleteConfirm = false },
        onConfirm = {
            onDelete(state)
            showDeleteConfirm = false
        },
        settingsViewModel = settingsViewModel
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MagicItemEditorContent(
    state: GameMagicItem,
    onStateChange: (GameMagicItem) -> Unit,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    forceBlurEnabled: Boolean,
    hazeState: HazeState?,
    englishNameError: String? = null,
    onEnglishNameErrorChange: (String?) -> Unit = {},
    allowedCharsRegex: Regex = Regex(".*")
) {
    val colorScheme = MaterialTheme.colorScheme
    val isOled = colorScheme.background == Color.Black

    val surfaceColor = if (forceBlurEnabled && !isOled && hazeState != null) {
        Color.Transparent.copy(alpha = 0.0f)
    } else {
        colorScheme.surface
    }
    val backgroundColor = if (forceBlurEnabled && !isOled && hazeState != null) {
        Color.Transparent.copy(alpha = 0.0f)
    } else {
        colorScheme.background
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .run {
                if (forceBlurEnabled && hazeState != null && !isOled) {
                    this.hazeEffect(state = hazeState) {
                        style = HazeStyle(
                            blurRadius = 24.dp,
                            tints = listOf(HazeTint(Color.Black.copy(alpha = 0.4f)))
                        )
                    }
                } else this
            },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (state.name.isNullOrBlank()) "Новый предмет" else "Редактировать",
                        fontWeight = FontWeight.Black
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                    }
                },
                actions = {
                    if (state.name?.isNotBlank() == true) {
                        IconButton(onClick = { /* Export logic */ }) {
                            Icon(Icons.Default.FileUpload, contentDescription = "Экспорт")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = surfaceColor,
                    titleContentColor = colorScheme.onSurface,
                    navigationIconContentColor = colorScheme.onSurface,
                    actionIconContentColor = colorScheme.onSurface
                )
            )
        },
        containerColor = backgroundColor
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
                    value = state.name ?: "",
                    onValueChange = { onStateChange(state.copy(name = it)) },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Английское название",
                        modifier = Modifier.weight(1f),
                        color = colorScheme.onSurface
                    )
                    Switch(
                        checked = state.showEnglishName ?: false,
                        onCheckedChange = { onStateChange(state.copy(showEnglishName = it)) })
                }

                OutlinedTextField(
                    value = state.englishName ?: "",
                    onValueChange = { newValue ->
                        if (newValue.all { it.toString().matches(allowedCharsRegex) }) {
                            onStateChange(state.copy(englishName = newValue))
                            onEnglishNameErrorChange(null)
                        } else {
                            onEnglishNameErrorChange("Разрешены только латиница, цифры и знаки ' - . _ , ( )")
                        }
                    },
                    label = { Text("English Name") },
                    isError = englishNameError != null,
                    supportingText = {
                        if (englishNameError != null) {
                            Text(englishNameError, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().alpha(if (state.showEnglishName == true) 1f else 0.7f),
                    shape = RoundedCornerShape(8.dp)
                )

                // Version
                Column {
                    SpellCardSectionTitle("ВЕРСИЯ")
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SpellVersion.entries.forEachIndexed { index, version ->
                            SegmentedButton(
                                selected = state.version == version,
                                onClick = { onStateChange(state.copy(version = version)) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = SpellVersion.entries.size
                                )
                            ) {
                                Text(version.displayName)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = state.formula ?: "",
                    onValueChange = { onStateChange(state.copy(formula = it)) },
                    label = { Text("Формула (напр. 2d4+2)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Column {
                    SpellCardSectionTitle("ТИП ПРЕДМЕТА")
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        MagicItemType.entries.forEachIndexed { index, type ->
                            SegmentedButton(
                                selected = state.type == type,
                                onClick = { onStateChange(state.copy(type = type)) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = MagicItemType.entries.size
                                )
                            ) {
                                Text(type.displayName)
                            }
                        }
                    }
                }

                Column {
                    SpellCardSectionTitle("ТИП ФОРМУЛЫ")
                    DamageTypeMultiSelect(
                        selectedTypes = state.damageTypes ?: emptyList(),
                        onToggle = { type ->
                            val current = state.damageTypes ?: emptyList()
                            val newList = if (current.contains(type)) current - type else current + type
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
                    value = state.description ?: "",
                    onValueChange = { onStateChange(state.copy(description = it)) },
                    label = { Text("Описание") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(8.dp)
                )

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
                var colorText by remember { mutableStateOf(state.colorHex) }
                OutlinedTextField(
                    value = colorText,
                    onValueChange = { 
                        colorText = it
                        onStateChange(state.copy(colorHex = it))
                    },
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

                OutlinedTextField(
                    value = state.source ?: "",
                    onValueChange = { onStateChange(state.copy(source = it)) },
                    label = { Text("Источник") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                if (!state.id.isNullOrBlank()) {
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Удалить")
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }

            Button(
                onClick = onDismiss,
                enabled = state.name?.isNotBlank() == true,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.surfaceVariant,
                    contentColor = colorScheme.onSurfaceVariant
                )
            ) {
                Text("Закрыть", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun parseColor(hex: String): Color {
    return try {
        val cleanHex = hex.removePrefix("#").removePrefix("0x")
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
