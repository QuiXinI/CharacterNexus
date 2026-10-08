package ru.quasaris.characternexus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import ru.quasaris.characternexus.ui.colourpicker.ColourPickerDialog
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.chrisbanes.haze.*
import kotlinx.coroutines.launch
import ru.quasaris.characternexus.backend.ImageManager
import ru.quasaris.characternexus.ui.theme.rememberEffectiveBlurRadius
import ru.quasaris.characternexus.ui.theme.rememberEffectiveHazeStyle
import ru.quasaris.characternexus.backend.SettingsViewModel
import ru.quasaris.characternexus.HeaderCode.Fullscreen.HealthSettingsContent
import ru.quasaris.characternexus.ui.BackHandler
import ru.quasaris.characternexus.ui.PredictiveBackBox
import ru.quasaris.characternexus.tabs.attacks.SectionHeader
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.*
import ru.quasaris.characternexus.HeaderCode.LevelPanel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterSettingsWindow(
    state: CharacterDetailState,
    statsMap: Map<String, String>,
    onDismiss: () -> Unit,
    forceBlurEnabled: Boolean = false,
    isDesktop: Boolean = false,
    hazeState: HazeState? = null,
    popupHazeState: HazeState? = null,
    settingsViewModel: SettingsViewModel? = null
) {
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val handleDismiss = {
        focusManager.clearFocus()
        onDismiss()
    }

    if (isDesktop) {
        CharacterSettingsContent(
            state = state,
            statsMap = statsMap,
            onDismiss = handleDismiss,
            forceBlurEnabled = forceBlurEnabled,
            hazeState = popupHazeState ?: hazeState,
            settingsViewModel = settingsViewModel
        )
    } else {
        Dialog(
            onDismissRequest = handleDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            DialogDimStyle(0f)
            CharacterSettingsContent(
                state = state,
                statsMap = statsMap,
                onDismiss = handleDismiss,
                forceBlurEnabled = forceBlurEnabled,
                hazeState = popupHazeState ?: hazeState,
                settingsViewModel = settingsViewModel
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterSettingsContent(
    state: CharacterDetailState,
    statsMap: Map<String, String>,
    onDismiss: () -> Unit,
    forceBlurEnabled: Boolean,
    hazeState: HazeState? = null,
    settingsViewModel: SettingsViewModel? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val isOled = colorScheme.background == Color.Black
    val blurRadius = rememberEffectiveBlurRadius(settingsViewModel)
    val hazeStyle = rememberEffectiveHazeStyle(blurRadius = blurRadius)
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Идентичность", "Хиты")

    PredictiveBackBox(
        onBack = onDismiss,
        modifier = Modifier.fillMaxSize()
    ) { _ ->
        Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .run {
                if (forceBlurEnabled && hazeState != null && !isOled) {
                    this.hazeEffect(state = hazeState, style = hazeStyle)
                } else this
            },
        topBar = {
            Column(
                modifier = Modifier.background(
                    if (forceBlurEnabled && !isOled) Color.Transparent.copy(alpha = 0.0f) else colorScheme.surface
                )
            ) {
                CenterAlignedTopAppBar(
                    title = { Text("Настройки персонажа", fontWeight = FontWeight.Black) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
                PrimaryTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent,
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }
        },
        containerColor = if (forceBlurEnabled && !isOled) Color.Transparent.copy(alpha = 0.0f) else colorScheme.background
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            when (selectedTabIndex) {
                0 -> IdentitySettingsSection(state, statsMap)
                1 -> HealthSettingsContent(
                    isManual = state.isManualHP,
                    onManualChange = { state.isManualHP = it },
                    manualMaxHp = state.manualMaxHp,
                    onManualMaxHpChange = { state.manualMaxHp = it },
                    isMulticlass = state.isMulticlassHP,
                    onMulticlassChange = { state.isMulticlassHP = it },
                    currentHitDie = state.defaultHitDie,
                    onHitDieChange = { state.defaultHitDie = it },
                    hpLevelData = state.hpLevelData,
                    onHPLevelDataChange = { state.hpLevelData = it },
                    manualHPLevelData = state.manualHPLevelData,
                    onManualHPLevelDataChange = { state.manualHPLevelData = it },
                    manualMaxHitDice = state.manualMaxHitDice,
                    onManualMaxHitDiceChange = { state.manualMaxHitDice = it },
                    hpBonusesAtLevel = state.hpBonusesAtLevel,
                    onHpBonusesAtLevelChange = { state.hpBonusesAtLevel = it },
                    hpBonusesTotal = state.hpBonusesTotal,
                    onHpBonusesTotalChange = { state.hpBonusesTotal = it },
                    statsMap = statsMap,
                    level = state.level.toIntOrNull() ?: 1
                )
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
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.compositeOver(MaterialTheme.colorScheme.background),
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdentitySettingsSection(state: CharacterDetailState, statsMap: Map<String, String>) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionHeader("Основная информация")

        OutlinedTextField(
            value = state.name,
            onValueChange = { state.name = it },
            label = { Text("Имя") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = state.race,
            onValueChange = { state.race = it },
            label = { Text("Вид") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        SectionHeader("Уровень и Опыт")

        LevelPanel(
            level = state.level,
            onLevelChange = {
                state.level = it
                state.syncHPDataExpansion()
                state.syncIdentity()
            },
            exp = state.experience,
            onExpChange = { state.experience = it },
            prof = state.proficiencyBonus,
            onProfChange = { state.proficiencyBonus = it },
            nextExp = state.nextLevelExp,
            stats = statsMap,
            standalone = false
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Мультикласс", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        if (state.isMulticlassHP) "Активен ручной выбор классов" else "Один основной класс",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = state.isMulticlassHP,
                    onCheckedChange = {
                        state.isMulticlassHP = it
                        if (it && state.classes.isEmpty()) {
                            state.classes = listOf(ClassEntry(className = state.characterClass, level = state.level.toIntOrNull() ?: 1))
                        }
                        state.syncIdentity()
                    },
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        }

        if (!state.isMulticlassHP) {
            SectionHeader("Класс")
            
            val firstClass = state.classes.firstOrNull() ?: ClassEntry()
            
            OutlinedTextField(
                value = state.characterClass,
                onValueChange = { 
                    state.characterClass = it
                    state.classes = listOf(firstClass.copy(className = it))
                    state.syncIdentity()
                },
                label = { Text("Класс") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            
            OutlinedTextField(
                value = firstClass.subclass,
                onValueChange = { 
                    state.classes = listOf(firstClass.copy(subclass = it))
                    state.syncIdentity()
                },
                label = { Text("Подкласс") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        } else {
            SectionHeader("Список классов")

            state.classes.forEachIndexed { index, entry ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = entry.className,
                                onValueChange = { s ->
                                    val newList = state.classes.toMutableList()
                                    newList[index] = entry.copy(className = s)
                                    state.classes = newList
                                    state.syncIdentity()
                                },
                                label = { Text("Класс") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            )

                            if (state.classes.size > 1) {
                                IconButton(onClick = {
                                    val newList = state.classes.toMutableList()
                                    newList.removeAt(index)
                                    state.classes = newList
                                    state.syncIdentity()
                                }) {
                                    Icon(Icons.Default.Delete, null, tint = colorScheme.error)
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = entry.subclass,
                                onValueChange = { s ->
                                    val newList = state.classes.toMutableList()
                                    newList[index] = entry.copy(subclass = s)
                                    state.classes = newList
                                    state.syncIdentity()
                                },
                                label = { Text("Подкласс") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            )

                            val totalLvl = state.level.toIntOrNull() ?: 1
                            val placeholder = if (index == 0) {
                                totalLvl.toString()
                            } else {
                                val otherSum = state.classes.filterIndexed { i, _ -> i != index }.sumOf { it.level }
                                (totalLvl - otherSum).coerceAtLeast(0).toString()
                            }

                            OutlinedTextField(
                                value = if (entry.level == 0) "" else entry.level.toString(),
                                onValueChange = { s ->
                                    val filtered = s.filter { it.isDigit() }
                                    val valInt = filtered.toIntOrNull() ?: 0
                                    val newList = state.classes.toMutableList()
                                    newList[index] = entry.copy(level = valInt)
                                    state.classes = newList
                                    state.syncIdentity()
                                },
                                label = { Text("Уровень") },
                                placeholder = { Text(placeholder) },
                                modifier = Modifier.width(100.dp),
                                shape = RoundedCornerShape(8.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    val newList = state.classes.toMutableList()
                    newList.add(ClassEntry(className = "", level = 1))
                    state.classes = newList
                    state.syncIdentity()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Добавить класс")
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Бард - Мастер на все руки", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Вы можете добавить половину своего Бонуса Мастерства к любой проверке характеристики которую вы совершаете, которой вы не владеете и которая другим образом не использует ваш Бонус Мастерства.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = state.isJackOfAllTrades,
                    onCheckedChange = {
                        state.isJackOfAllTrades = it
                        state.syncIdentity()
                    },
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        }

        SectionHeader("Цветовая схема")

        val coroutineScope = rememberCoroutineScope()
        var hexInputText by remember(state.themeSeedColorArgb, state.isThemeSeedManual) {
            mutableStateOf(formatColorToHex(state.themeSeedColorArgb))
        }
        var showColourPicker by remember { mutableStateOf(false) }

        if (showColourPicker) {
            val currentColor = state.themeSeedColorArgb?.let { Color(it) } ?: colorScheme.primary
            ColourPickerDialog(
                initialColor = currentColor,
                onDismiss = { showColourPicker = false },
                onColorConfirmed = { chosenColor ->
                    val argb = chosenColor.toArgb()
                    state.themeSeedColorArgb = argb
                    state.isThemeSeedManual = true
                    hexInputText = formatColorToHex(argb)
                },
                showAlpha = false,
                title = "Выбор цвета темы"
            )
        }

        OutlinedTextField(
            value = hexInputText,
            onValueChange = { input ->
                hexInputText = input
                val parsed = parseHexColor(input)
                if (parsed != null) {
                    state.themeSeedColorArgb = parsed
                    state.isThemeSeedManual = true
                } else if (input.isBlank()) {
                    state.themeSeedColorArgb = null
                    state.isThemeSeedManual = true
                } else {
                    state.isThemeSeedManual = true
                }
            },
            label = { Text("Сид цвета темы (HEX)") },
            placeholder = { Text("#6750A4") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            leadingIcon = {
                val displayColor = state.themeSeedColorArgb?.let { Color(it) } ?: colorScheme.primary
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable { showColourPicker = true }
                        .background(displayColor, CircleShape)
                        .border(1.dp, colorScheme.outline.copy(alpha = 0.5f), CircleShape)
                )
            },
            trailingIcon = {
                if (state.isThemeSeedManual) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                state.isThemeSeedManual = false
                                val imageId = state.characterImageData
                                var newSeed: Int? = null
                                if (!imageId.isNullOrBlank()) {
                                    try {
                                        val path = ImageManager.getPortraitFile(imageId, state.characterUuid)
                                        if (platformFileSystem.exists(path)) {
                                            val bytes = platformFileSystem.read(path) { readByteArray() }
                                            newSeed = PaletteHelper.extractSeedColor(bytes)
                                        }
                                    } catch (_: Exception) {}
                                }
                                state.themeSeedColorArgb = newSeed
                                hexInputText = formatColorToHex(newSeed)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Сбросить к автоматическому сиду",
                            tint = colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            supportingText = {
                if (state.isThemeSeedManual) {
                    Text("Цвет переписан вручную", color = colorScheme.primary)
                } else if (state.themeSeedColorArgb != null) {
                    Text("Цвет автоматически извлечен из аватарки")
                } else {
                    Text("Используется стандартная тема")
                }
            }
        )

        Spacer(Modifier.height(100.dp))
    }
}

private fun formatColorToHex(colorInt: Int?): String {
    if (colorInt == null) return ""
    val rgb = colorInt and 0xFFFFFF
    return "#" + rgb.toString(16).padStart(6, '0').uppercase()
}

private fun parseHexColor(hex: String): Int? {
    val clean = hex.removePrefix("#").trim()
    if (clean.length == 6) {
        val rgb = clean.toLongOrNull(16) ?: return null
        return (0xFF000000.toInt() or rgb.toInt())
    } else if (clean.length == 8) {
        val argb = clean.toLongOrNull(16) ?: return null
        return argb.toInt()
    }
    return null
}

