package ru.quasaris.characters.master

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.chrisbanes.haze.*
import ru.quasaris.characters.master.HeaderCode.Fullscreen.HealthSettingsContent
import ru.quasaris.characters.master.tabs.attacks.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterSettingsWindow(
    state: CharacterDetailState,
    statsMap: Map<String, String>,
    onDismiss: () -> Unit,
    hazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val colorScheme = MaterialTheme.colorScheme
        val isOled = colorScheme.background == Color.Black
        var selectedTabIndex by remember { mutableIntStateOf(0) }
        val tabs = listOf("Идентичность", "Хиты")

        Scaffold(
            topBar = {
                Column(
                    modifier = Modifier.background(
                        if (forceBlurEnabled && !isOled) Color.Transparent else colorScheme.surface
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
            containerColor = if (forceBlurEnabled && !isOled) Color.Transparent else colorScheme.background,
            modifier = Modifier.run {
                if (forceBlurEnabled && hazeState != null && !isOled) {
                    hazeEffect(state = hazeState) {
                        style = HazeStyle(blurRadius = 24.dp, tints = listOf(HazeTint(colorScheme.surface.copy(alpha = 0.1f))))
                        inputScale = HazeInputScale.Fixed(0.7f)
                    }
                } else this
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                when (selectedTabIndex) {
                    0 -> IdentitySettingsSection(state)
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
}

@Composable
fun IdentitySettingsSection(state: CharacterDetailState) {
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

        Spacer(Modifier.height(8.dp))
        SectionHeader("Классы и уровни")

        state.classes.forEachIndexed { index, entry ->
            ClassEntryRow(
                entry = entry,
                onUpdate = { updated ->
                    val newList = state.classes.toMutableList()
                    newList[index] = updated
                    state.classes = newList
                    state.syncIdentity()
                },
                onDelete = {
                    val newList = state.classes.toMutableList()
                    newList.removeAt(index)
                    state.classes = newList
                    state.syncIdentity()
                }
            )
        }

        Button(
            onClick = {
                state.classes = state.classes + ClassEntry()
                state.syncIdentity()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("Добавить класс")
        }

        Spacer(Modifier.height(32.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassEntryRow(
    entry: ClassEntry,
    onUpdate: (ClassEntry) -> Unit,
    onDelete: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                var expanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = entry.className.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Класс") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { expanded = true })
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        listOf(
                            CharacterClass.BARBARIAN,
                            CharacterClass.FIGHTER,
                            CharacterClass.ARTIFICER,
                            CharacterClass.WIZARD
                        ).forEach { cls ->
                            DropdownMenuItem(
                                text = { Text(cls.displayName) },
                                onClick = {
                                    onUpdate(entry.copy(className = cls))
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = if (entry.level == 0) "" else entry.level.toString(),
                    onValueChange = { s ->
                        val v = s.filter { it.isDigit() }.toIntOrNull() ?: 0
                        onUpdate(entry.copy(level = v))
                    },
                    label = { Text("Ур") },
                    modifier = Modifier.width(70.dp),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, null, tint = colorScheme.error)
                }
            }

            OutlinedTextField(
                value = entry.subclass,
                onValueChange = { onUpdate(entry.copy(subclass = it)) },
                label = { Text("Подкласс") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }
    }
}
