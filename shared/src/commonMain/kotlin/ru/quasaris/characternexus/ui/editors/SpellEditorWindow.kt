package ru.quasaris.characternexus.ui.editors

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
import androidx.compose.ui.draw.alpha
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.ui.*
import ru.quasaris.characternexus.ui.DeleteConfirmationDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpellEditorWindow(
    spell: SpellCard,
    onDismiss: () -> Unit,
    onSave: (SpellCard) -> Unit,
    onDelete: (SpellCard) -> Unit,
    onExport: (SpellCard) -> Unit = {},
    forceBlurEnabled: Boolean = false,
    settingsViewModel: ru.quasaris.characternexus.backend.SettingsViewModel? = null
) {
    var state by remember { mutableStateOf(spell) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var englishNameError by remember { mutableStateOf<String?>(null) }
    val allowedCharsRegex = remember { Regex("^[a-zA-Z0-9'\\-._,() ]*$") }

    LaunchedEffect(state.name) {
        val path = mutableListOf<NavNode>()
        path.add(NavNode("modules", "Модули", 0) { onDismiss() })
        path.add(NavNode("editor", "Редактор заклинания", 1) { onDismiss() })
        path.add(NavNode("comp", state.name.ifBlank { "Новое" }, 2))
        NavigationPathManager.updatePath("modules", path)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val colorScheme = MaterialTheme.colorScheme
        val isOled = colorScheme.background == Color.Black

        val surfaceColor = if (forceBlurEnabled && !isOled) {
            Color.Transparent.copy(alpha = 0.0f)
        } else {
            colorScheme.surface
        }
        val backgroundColor = if (forceBlurEnabled && !isOled) {
            Color.Transparent.copy(alpha = 0.0f)
        } else {
            colorScheme.background
        }

        Surface(
            color = backgroundColor,
            contentColor = colorScheme.onSurface
        ) {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { 
                            Text(
                                if (spell.name.isBlank()) "Новое заклинание" else "Редактировать", 
                                fontWeight = FontWeight.Black
                            ) 
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Закрыть")
                            }
                        },
                        actions = {
                            if (state.name.isNotBlank()) {
                                IconButton(onClick = { onExport(state) }) {
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
                containerColor = Color.Transparent
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
                            onValueChange = { state = state.copy(name = it) },
                            label = { Text("Название") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedTextColor = colorScheme.onSurface,
                                unfocusedTextColor = colorScheme.onSurface
                            )
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Английское название", 
                                modifier = Modifier.weight(1f),
                                color = colorScheme.onSurface
                            )
                            Switch(checked = state.showEnglishName, onCheckedChange = { state = state.copy(showEnglishName = it) })
                        }

                        OutlinedTextField(
                            value = state.englishName,
                            onValueChange = { newValue ->
                                if (newValue.all { it.toString().matches(allowedCharsRegex) }) {
                                    state = state.copy(englishName = newValue)
                                    englishNameError = null
                                } else {
                                    englishNameError = "Разрешены только латиница, цифры и знаки ' - . _ , ( )"
                                }
                            },
                            label = { Text("English Name") },
                            isError = englishNameError != null,
                            supportingText = {
                                if (englishNameError != null) {
                                    Text(englishNameError!!, color = MaterialTheme.colorScheme.error)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                errorContainerColor = Color.Yellow.copy(alpha = 0.15f),
                                errorTextColor = MaterialTheme.colorScheme.onSurface,
                                errorLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                errorBorderColor = Color.Yellow.copy(alpha = 0.8f)
                            ),
                            modifier = Modifier.fillMaxWidth().alpha(if (state.showEnglishName) 1f else 0.7f),
                            shape = RoundedCornerShape(8.dp)
                        )

                        // Level
                        OutlinedTextField(
                            value = state.level,
                            onValueChange = { state = state.copy(level = it) },
                            label = { Text("Уровень (0 - заговор)") },
                            modifier = Modifier.fillMaxWidth().alpha(if (state.level.isBlank()) 0.7f else 1f),
                            shape = RoundedCornerShape(8.dp)
                        )

                        // Version
                        Column {
                            SpellCardSectionTitle("ВЕРСИЯ")
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                SpellVersion.entries.forEachIndexed { index, version ->
                                    SegmentedButton(
                                        selected = state.version == version,
                                        onClick = { state = state.copy(version = version) },
                                        shape = SegmentedButtonDefaults.itemShape(index = index, count = SpellVersion.entries.size)
                                    ) {
                                        Text(version.displayName)
                                    }
                                }
                            }
                        }

                        // School
                        val schoolActive = state.school != SpellSchool.NONE
                        Column(modifier = Modifier.alpha(if (schoolActive) 1f else 0.7f)) {
                            SpellCardSectionTitle("ШКОЛА")
                            var schoolExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = state.school.displayName,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Школа") },
                                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                                    modifier = Modifier.fillMaxWidth().clickable { schoolExpanded = true },
                                    enabled = false, // Set to false but allow box clickable
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                // A transparent overlay to capture clicks since the textfield is disabled
                                Box(modifier = Modifier.matchParentSize().clickable { schoolExpanded = true })
                                DropdownMenu(expanded = schoolExpanded, onDismissRequest = { schoolExpanded = false }) {
                                    SpellSchool.entries.forEach { school ->
                                        DropdownMenuItem(text = { Text(school.displayName) }, onClick = { state = state.copy(school = school); schoolExpanded = false })
                                    }
                                }
                            }
                        }

                        // Classes
                        val classesActive = state.classes.isNotEmpty()
                        Column(modifier = Modifier.alpha(if (classesActive) 1f else 0.7f)) {
                            SpellCardSectionTitle("КЛАССЫ")
                            var classesExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.fillMaxWidth()) {
                                val selectedClassesText = if (state.classes.isEmpty()) "Не выбрано" else state.classes.joinToString { it.displayName }
                                OutlinedTextField(
                                    value = selectedClassesText,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Доступно классам") },
                                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = false,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Box(modifier = Modifier.matchParentSize().clickable { classesExpanded = true })
                                DropdownMenu(expanded = classesExpanded, onDismissRequest = { classesExpanded = false }) {
                                    CharacterClass.entries.forEach { cls ->
                                        DropdownMenuItem(
                                            text = { 
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Checkbox(checked = state.classes.contains(cls), onCheckedChange = null)
                                                    Spacer(Modifier.width(8.dp))
                                                    Text(cls.displayName)
                                                }
                                            },
                                            onClick = {
                                                val newList = if (state.classes.contains(cls)) state.classes - cls else state.classes + cls
                                                state = state.copy(classes = newList)
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Components
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SpellCardSectionTitle("КОМПОНЕНТЫ")
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                ComponentToggleButton("О", state.isCircle) { state = state.copy(isCircle = it) }
                                ComponentToggleButton("Р", state.isRitual) { state = state.copy(isRitual = it) }
                                ComponentToggleButton("В", state.hasVerbalComponent) { state = state.copy(hasVerbalComponent = it) }
                                ComponentToggleButton("С", state.hasSomaticComponent) { state = state.copy(hasSomaticComponent = it) }
                            }

                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                MaterialComponentType.entries.forEachIndexed { index, type ->
                                    SegmentedButton(
                                        selected = state.materialComponentType == type,
                                        onClick = { state = state.copy(materialComponentType = type) },
                                        shape = SegmentedButtonDefaults.itemShape(index = index, count = MaterialComponentType.entries.size)
                                    ) {
                                        Text(type.displayName)
                                    }
                                }
                            }
                        }

                        val materialActive = state.materialComponentType != MaterialComponentType.NONE
                        OutlinedTextField(
                            value = state.materialComponents,
                            onValueChange = { state = state.copy(materialComponents = it) },
                            label = { Text("Материальные компоненты") },
                            modifier = Modifier.fillMaxWidth().alpha(if (materialActive) 1f else 0.7f),
                            shape = RoundedCornerShape(8.dp)
                        )

                        // Casting Time
                        Column {
                            SpellCardSectionTitle("ВРЕМЯ НАЛОЖЕНИЯ")
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val cornerRadius = 16.dp
                            val castingTypes = CastingTimeType.entries
                            
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy((-9).dp)
                            ) {
                                // Top Row: ACTION and BONUS_ACTION
                                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                    SegmentedButton(
                                        selected = state.castingTimeType == castingTypes[0], // ACTION
                                        onClick = { state = state.copy(castingTimeType = castingTypes[0]) },
                                        shape = RoundedCornerShape(topStart = cornerRadius, topEnd = 0.dp, bottomEnd = 0.dp, bottomStart = 0.dp)
                                    ) { Text(castingTypes[0].displayName, fontSize = 12.sp) }

                                    SegmentedButton(
                                        selected = state.castingTimeType == castingTypes[1], // BONUS_ACTION
                                        onClick = { state = state.copy(castingTimeType = castingTypes[1]) },
                                        shape = RoundedCornerShape(topStart = 0.dp, topEnd = cornerRadius, bottomEnd = 0.dp, bottomStart = 0.dp)
                                    ) { Text(castingTypes[1].displayName, fontSize = 12.sp) }
                                }

                                // Bottom Row: REACTION and OTHER
                                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                    SegmentedButton(
                                        selected = state.castingTimeType == castingTypes[2], // REACTION
                                        onClick = { state = state.copy(castingTimeType = castingTypes[2]) },
                                        shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomEnd = 0.dp, bottomStart = cornerRadius)
                                    ) { Text(castingTypes[2].displayName, fontSize = 12.sp) }

                                    SegmentedButton(
                                        selected = state.castingTimeType == castingTypes[3], // OTHER
                                        onClick = { state = state.copy(castingTimeType = castingTypes[3]) },
                                        shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomEnd = cornerRadius, bottomStart = 0.dp)
                                    ) { Text(castingTypes[3].displayName, fontSize = 12.sp) }
                                }
                            }
                            
                            val isCastingTimeEnabled = true // Always enabled to allow typing
                            val isCastingTimeActive = state.castingTime.isNotBlank() || 
                                                     state.castingTimeType == CastingTimeType.REACTION || 
                                                     state.castingTimeType == CastingTimeType.OTHER
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            OutlinedTextField(
                                value = state.castingTime,
                                onValueChange = { state = state.copy(castingTime = it) },
                                label = { Text("Описание времени наложения") },
                                modifier = Modifier.fillMaxWidth().alpha(if (isCastingTimeActive) 1f else 0.7f),
                                enabled = isCastingTimeEnabled,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }

                        // Duration and Concentration
                        Column {
                            SpellCardSectionTitle("ДЛИТЕЛЬНОСТЬ")
                            Row(
                                modifier = Modifier.fillMaxWidth(), 
                                horizontalArrangement = Arrangement.spacedBy(8.dp), 
                                verticalAlignment = Alignment.Bottom
                            ) {
                                ComponentToggleButton("К", state.hasConcentration, modifier = Modifier.size(56.dp)) { state = state.copy(hasConcentration = it) }
                                
                                val isValueDisabled = !state.durationUnit.requiresValue

                                OutlinedTextField(
                                    value = state.durationValue,
                                    onValueChange = {
                                        val onlyDigits = it.filter { c -> c.isDigit() }
                                        state = state.copy(durationValue = onlyDigits)
                                    },
                                    label = { Text("Кол-во") },
                                    modifier = Modifier.width(90.dp).alpha(if (isValueDisabled) 0.7f else 1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(8.dp)
                                )

                                var dimExpanded by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.weight(1f)) {
                                    OutlinedTextField(
                                        value = state.durationUnit.displayName,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Размерность") },
                                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = false,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    Box(modifier = Modifier.matchParentSize().clickable { dimExpanded = true })
                                    DropdownMenu(expanded = dimExpanded, onDismissRequest = { dimExpanded = false }) {
                                        DurationUnit.entries.forEach { unit ->
                                            DropdownMenuItem(
                                                text = { Text(unit.displayName) },
                                                onClick = {
                                                    state = state.copy(durationUnit = unit)
                                                    dimExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Description
                        OutlinedTextField(
                            value = state.description,
                            onValueChange = { state = state.copy(description = it) },
                            label = { Text("Описание") },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp).alpha(if (state.description.isBlank()) 0.7f else 1f),
                            shape = RoundedCornerShape(8.dp)
                        )

                        if (state.level == "0") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SpellCardSectionTitle("НАСТРОЙКИ ЗАГОВОРА")
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Нет урона на 1 уровне", 
                                        modifier = Modifier.weight(1f), 
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colorScheme.onSurface
                                    )
                                    Switch(checked = state.noDamageAtLevel1, onCheckedChange = { state = state.copy(noDamageAtLevel1 = it) })
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Урон не увеличивается с уровнем", 
                                        modifier = Modifier.weight(1f), 
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colorScheme.onSurface
                                    )
                                    Switch(checked = state.noScaling, onCheckedChange = { state = state.copy(noScaling = it) })
                                }
                            }
                        }

                        // Damage
                        Column {
                            SpellCardSectionTitle("УРОН")
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Наносимый урон", 
                                    modifier = Modifier.weight(1f),
                                    color = colorScheme.onSurface
                                )
                                Switch(checked = state.hasDamage, onCheckedChange = { state = state.copy(hasDamage = it) })
                            }

                            if (state.hasDamage) {
                                // Base damage row
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = state.damageFormula,
                                        onValueChange = { state = state.copy(damageFormula = it) },
                                        label = { Text("Формула") },
                                        modifier = Modifier.weight(1f),
                                        placeholder = { Text("1d8") },
                                        shape = RoundedCornerShape(8.dp)
                                    )

                                    DamageTypeMultiSelect(
                                        selectedTypes = state.damageTypes,
                                        onToggle = { type ->
                                            val newList = if (state.damageTypes.contains(type)) state.damageTypes - type else state.damageTypes + type
                                            state = state.copy(damageTypes = newList)
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                if (state.level != "0" && state.level.isNotBlank() && state.level.toIntOrNull() != null) {
                                    OutlinedTextField(
                                        value = state.upcastDamageFormula,
                                        onValueChange = { state = state.copy(upcastDamageFormula = it) },
                                        label = { Text("Урон за уровень ячейки") },
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                        placeholder = { Text("1d8") },
                                        shape = RoundedCornerShape(8.dp)
                                    )

                                    // Upcast settings toggles
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 12.dp)
                                            .background(colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                            .padding(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                "Увеличивается только 1 формула",
                                                modifier = Modifier.weight(1f),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colorScheme.onSurface
                                            )
                                            Switch(
                                                checked = state.upcastOnlyOne,
                                                onCheckedChange = { 
                                                    state = state.copy(
                                                        upcastOnlyOne = it,
                                                        upcastUserChoice = if (it) true else state.upcastUserChoice
                                                    ) 
                                                }
                                            )
                                        }
                                        
                                        val isUserChoiceActive = state.upcastOnlyOne
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.alpha(if (isUserChoiceActive) 1f else 0.5f)
                                        ) {
                                            Text(
                                                "Выбор для увеличения",
                                                modifier = Modifier.weight(1f),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colorScheme.onSurface
                                            )
                                            Switch(
                                                checked = state.upcastUserChoice,
                                                onCheckedChange = { if (isUserChoiceActive) state = state.copy(upcastUserChoice = it) },
                                                enabled = isUserChoiceActive
                                            )
                                        }
                                    }
                                }

                                // Additional damage rows
                                state.additionalDamageFormulas.forEachIndexed { index, formula ->
                                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = formula,
                                                onValueChange = { newFormula ->
                                                    val newList = state.additionalDamageFormulas.toMutableList()
                                                    newList[index] = newFormula
                                                    state = state.copy(additionalDamageFormulas = newList)
                                                },
                                                label = { Text("Доп. Формула") },
                                                modifier = Modifier.weight(1f),
                                                placeholder = { Text("1d4") },
                                                shape = RoundedCornerShape(8.dp)
                                            )

                                            val currentTypes = state.additionalDamageTypesList.getOrNull(index) ?: emptyList()
                                            DamageTypeMultiSelect(
                                                selectedTypes = currentTypes,
                                                onToggle = { type ->
                                                    val newList = state.additionalDamageTypesList.toMutableList()
                                                    while (newList.size <= index) newList.add(emptyList())
                                                    val rowList = newList[index]
                                                    newList[index] = if (rowList.contains(type)) rowList - type else rowList + type
                                                    state = state.copy(additionalDamageTypesList = newList)
                                                },
                                                modifier = Modifier.weight(1f)
                                            )

                                            IconButton(onClick = {
                                                val newFormulas = state.additionalDamageFormulas.toMutableList()
                                                newFormulas.removeAt(index)
                                                val newUpcasts = state.additionalUpcastDamageFormulas.toMutableList()
                                                if (index < newUpcasts.size) newUpcasts.removeAt(index)
                                                val newTypes = state.additionalDamageTypesList.toMutableList()
                                                if (index < newTypes.size) newTypes.removeAt(index)
                                                state = state.copy(
                                                    additionalDamageFormulas = newFormulas,
                                                    additionalUpcastDamageFormulas = newUpcasts,
                                                    additionalDamageTypesList = newTypes
                                                )
                                            }) {
                                                Icon(Icons.Default.Delete, contentDescription = null, tint = colorScheme.error)
                                            }
                                        }

                                        if (state.level != "0" && state.level.isNotBlank() && state.level.toIntOrNull() != null) {
                                            val upcastVal = state.additionalUpcastDamageFormulas.getOrNull(index) ?: ""
                                            OutlinedTextField(
                                                value = upcastVal,
                                                onValueChange = { newVal ->
                                                    val newList = state.additionalUpcastDamageFormulas.toMutableList()
                                                    while (newList.size <= index) newList.add("")
                                                    newList[index] = newVal
                                                    state = state.copy(additionalUpcastDamageFormulas = newList)
                                                },
                                                label = { Text("Доп. урон за уровень ячейки") },
                                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                                placeholder = { Text("1d4") },
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                        }
                                    }
                                }

                                Button(
                                    onClick = {
                                        state = state.copy(
                                            additionalDamageFormulas = state.additionalDamageFormulas + "",
                                            additionalUpcastDamageFormulas = state.additionalUpcastDamageFormulas + "",
                                            additionalDamageTypesList = state.additionalDamageTypesList + listOf(emptyList<DamageType>())
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.secondaryContainer, contentColor = colorScheme.onSecondaryContainer)
                                ) {
                                    Icon(Icons.Default.Add, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Добавить урон")
                                }
                            }
                        }

                        // Attack/Save Switch
                        Column {
                            var typeExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.fillMaxWidth()) {
                                val selectedTypesText = if (state.attackTypes.isEmpty()) "Нет" else state.attackTypes.joinToString { it.displayName }
                                OutlinedTextField(
                                    value = selectedTypesText,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Тип проверки") },
                                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = false,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Box(modifier = Modifier.matchParentSize().clickable { typeExpanded = true })
                                DropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                                    MagicAttackType.entries.forEach { type ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Checkbox(checked = state.attackTypes.contains(type), onCheckedChange = null)
                                                    Spacer(Modifier.width(8.dp))
                                                    Text(type.displayName)
                                                }
                                            },
                                            onClick = {
                                                val newList = if (state.attackTypes.contains(type)) state.attackTypes - type else state.attackTypes + type
                                                state = state.copy(attackTypes = newList)
                                            }
                                        )
                                    }
                                }
                            }
                            
                            val isSave = state.attackTypes.contains(MagicAttackType.SAVE)
                            var attrExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp).alpha(if (isSave) 1f else 0.7f)) {
                                val selectedAttrsText = if (state.savingThrowAttributes.isEmpty()) "Не выбрано" else state.savingThrowAttributes.joinToString { it.fullName }
                                OutlinedTextField(
                                    value = selectedAttrsText,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Характеристика спасброска") },
                                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = false,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Box(modifier = Modifier.matchParentSize().clickable { attrExpanded = true })
                                DropdownMenu(expanded = attrExpanded, onDismissRequest = { attrExpanded = false }) {
                                    Attribute.entries.filter { it != Attribute.NONE }.forEach { attr ->
                                        DropdownMenuItem(
                                            text = { 
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Checkbox(checked = state.savingThrowAttributes.contains(attr), onCheckedChange = null)
                                                    Spacer(Modifier.width(8.dp))
                                                    Text(attr.fullName)
                                                }
                                            },
                                            onClick = {
                                                val newList = if (state.savingThrowAttributes.contains(attr)) state.savingThrowAttributes - attr else state.savingThrowAttributes + attr
                                                state = state.copy(savingThrowAttributes = newList)
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Distance
                        OutlinedTextField(
                            value = state.distance,
                            onValueChange = { state = state.copy(distance = it) },
                            label = { Text("Дистанция") },
                            modifier = Modifier.fillMaxWidth().alpha(if (state.distance.isBlank()) 0.7f else 1f),
                            shape = RoundedCornerShape(8.dp)
                        )

                        // Notes and Link
                        OutlinedTextField(
                            value = state.notes,
                            onValueChange = { state = state.copy(notes = it) },
                            label = { Text("Заметки") },
                            modifier = Modifier.fillMaxWidth().alpha(if (state.notes.isBlank()) 0.7f else 1f),
                            shape = RoundedCornerShape(8.dp)
                        )

                        OutlinedTextField(
                            value = state.link ?: "",
                            onValueChange = { state = state.copy(link = it.ifBlank { null }) },
                            label = { Text("Ссылка") },
                            modifier = Modifier.fillMaxWidth().alpha(if (state.link.isNullOrBlank()) 0.7f else 1f),
                            shape = RoundedCornerShape(8.dp)
                        )

                        // Additional Links
                        val linksActive = state.additionalLinks.isNotEmpty()
                        Column(modifier = Modifier.alpha(if (linksActive) 1f else 0.7f)) {
                            SpellCardSectionTitle("ДОПОЛНИТЕЛЬНЫЕ ССЫЛКИ")
                            state.additionalLinks.forEachIndexed { idx, link ->
                                Row(modifier = Modifier.padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = link.name, onValueChange = { n ->
                                        val newList = state.additionalLinks.toMutableList()
                                        newList[idx] = link.copy(name = n)
                                        state = state.copy(additionalLinks = newList)
                                    }, label = { Text("Название") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp))
                                    
                                    OutlinedTextField(value = link.url, onValueChange = { u ->
                                        val newList = state.additionalLinks.toMutableList()
                                        newList[idx] = link.copy(url = u)
                                        state = state.copy(additionalLinks = newList)
                                    }, label = { Text("URL") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp))
                                    
                                    IconButton(onClick = {
                                        val newList = state.additionalLinks.toMutableList()
                                        newList.removeAt(idx)
                                        state = state.copy(additionalLinks = newList)
                                    }) { Icon(Icons.Default.Clear, null, tint = Color.Red) }
                                }
                            }
                            Button(onClick = { state = state.copy(additionalLinks = state.additionalLinks + SpellLink()) }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Add, null)
                                Spacer(Modifier.width(8.dp))
                                Text("ДОБАВИТЬ ССЫЛКУ")
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        if (spell.name.isNotBlank()) {
                            OutlinedButton(
                                onClick = { showDeleteConfirm = true },
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
                        onClick = { onSave(state) },
                        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Сохранить", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
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
}

@Composable
fun DamageTypeMultiSelect(
    selectedTypes: List<DamageType>,
    onToggle: (DamageType) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        val text = if (selectedTypes.isEmpty()) "Вид урона" else selectedTypes.joinToString("/") { it.displayName }
        OutlinedTextField(
            value = text,
            onValueChange = {},
            readOnly = true,
            label = { Text("Вид урона") },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
            modifier = Modifier.fillMaxWidth(),
            enabled = false,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            shape = RoundedCornerShape(8.dp)
        )
        Box(modifier = Modifier.matchParentSize().clickable(enabled = enabled) { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DamageType.entries.forEach { type ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = selectedTypes.contains(type), onCheckedChange = null)
                            Spacer(Modifier.width(8.dp))
                            Text(type.displayName)
                        }
                    },
                    onClick = { onToggle(type) }
                )
            }
        }
    }
}

@Composable
fun ComponentToggleButton(label: String, selected: Boolean, modifier: Modifier = Modifier, onToggle: (Boolean) -> Unit) {
    Surface(
        onClick = { onToggle(!selected) },
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier.widthIn(min = 48.dp).heightIn(min = 48.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(label, fontWeight = FontWeight.Bold, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun SpellCardSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
    )
}
