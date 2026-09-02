package ru.quasaris.characternexus.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.chrisbanes.haze.*
import ru.quasaris.characternexus.HeaderCode.Fullscreen.HealthSettingsContent
import ru.quasaris.characternexus.ui.BackHandler
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
    popupHazeState: HazeState? = null
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
            hazeState = popupHazeState ?: hazeState
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
                hazeState = popupHazeState ?: hazeState
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
    hazeState: HazeState? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val isOled = colorScheme.background == Color.Black
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Идентичность", "Хиты")

    BackHandler(onBack = onDismiss)

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
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdentitySettingsSection(state: CharacterDetailState, statsMap: Map<String, String>) {
    val colorScheme = MaterialTheme.colorScheme

    var lastSavedName by remember { mutableStateOf(state.name) }
    var lastSavedRace by remember { mutableStateOf(state.race) }
    var lastSavedLevel by remember { mutableStateOf(state.level) }
    var lastSavedExperience by remember { mutableStateOf(state.experience) }
    var lastSavedProficiency by remember { mutableStateOf(state.proficiencyBonus) }
    var lastSavedMulticlass by remember { mutableStateOf(state.isMulticlassHP) }
    var lastSavedClasses by remember { mutableStateOf(state.classes) }
    var lastSavedBaseClass by remember { mutableStateOf(state.characterClass) }

    val isDirty = state.name != lastSavedName ||
            state.race != lastSavedRace ||
            state.level != lastSavedLevel ||
            state.experience != lastSavedExperience ||
            state.proficiencyBonus != lastSavedProficiency ||
            state.isMulticlassHP != lastSavedMulticlass ||
            state.classes != lastSavedClasses ||
            state.characterClass != lastSavedBaseClass

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

        if (isDirty) {
            Button(
                onClick = {
                    lastSavedName = state.name
                    lastSavedRace = state.race
                    lastSavedLevel = state.level
                    lastSavedExperience = state.experience
                    lastSavedProficiency = state.proficiencyBonus
                    lastSavedMulticlass = state.isMulticlassHP
                    lastSavedClasses = state.classes
                    lastSavedBaseClass = state.characterClass
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, null)
                Spacer(Modifier.width(8.dp))
                Text("Сохранить")
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

// DELETE or keep if needed, but I'll remove as per plan to simplify
