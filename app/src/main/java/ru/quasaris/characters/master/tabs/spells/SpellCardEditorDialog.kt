package ru.quasaris.characters.master.tabs.spells

import androidx.compose.animation.*
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
import ru.quasaris.characters.master.Attribute
import ru.quasaris.characters.master.SpellCard
import ru.quasaris.characters.master.MaterialComponentType
import ru.quasaris.characters.master.MagicAttackType
import ru.quasaris.characters.master.SpellSchool
import ru.quasaris.characters.master.SpellVersion
import ru.quasaris.characters.master.CharacterClass
import ru.quasaris.characters.master.ui.DeleteConfirmationDialog
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.HazeInputScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpellCardEditorDialog(
    spell: SpellCard,
    onDismiss: () -> Unit,
    onSave: (SpellCard) -> Unit,
    onDelete: (SpellCard) -> Unit,
    onExport: (SpellCard) -> Unit = {},
    hazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false,
    settingsViewModel: ru.quasaris.characters.master.backend.SettingsViewModel? = null
) {
    var state by remember { mutableStateOf(spell) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val colorScheme = MaterialTheme.colorScheme
        val isOled = colorScheme.background == Color.Black

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(if (spell.name.isBlank()) "Новое заклинание" else "Редактировать", fontWeight = FontWeight.Black) },
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
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = if (forceBlurEnabled && !isOled) Color.Transparent else colorScheme.surface
                    )
                )
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
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Name and English Name
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = { state = state.copy(name = it) },
                        label = { Text("Название") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Английское название", modifier = Modifier.weight(1f))
                        Switch(checked = state.showEnglishName, onCheckedChange = { state = state.copy(showEnglishName = it) })
                    }

                    OutlinedTextField(
                        value = state.englishName,
                        onValueChange = { state = state.copy(englishName = it) },
                        label = { Text("English Name") },
                        modifier = Modifier.fillMaxWidth().alpha(if (state.showEnglishName) 1f else 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Level
                    OutlinedTextField(
                        value = state.level,
                        onValueChange = { state = state.copy(level = it) },
                        label = { Text("Уровень (0 - заговор)") },
                        modifier = Modifier.fillMaxWidth().alpha(if (state.level.isBlank()) 0.5f else 1f),
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
                    Column(modifier = Modifier.alpha(if (schoolActive) 1f else 0.5f)) {
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
                    Column(modifier = Modifier.alpha(if (classesActive) 1f else 0.5f)) {
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
                    Column {
                        SpellCardSectionTitle("КОМПОНЕНТЫ")
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            ComponentToggleButton("О", state.isCircle) { state = state.copy(isCircle = it) }
                            ComponentToggleButton("Р", state.isRitual) { state = state.copy(isRitual = it) }
                            ComponentToggleButton("В", state.hasVerbalComponent) { state = state.copy(hasVerbalComponent = it) }
                            ComponentToggleButton("С", state.hasSomaticComponent) { state = state.copy(hasSomaticComponent = it) }

                            SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
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
                    }

                    val materialActive = state.materialComponentType != MaterialComponentType.NONE
                    OutlinedTextField(
                        value = state.materialComponents,
                        onValueChange = { state = state.copy(materialComponents = it) },
                        label = { Text("Материальные компоненты") },
                        modifier = Modifier.fillMaxWidth().alpha(if (materialActive) 1f else 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Casting Time
                    Column {
                        SpellCardSectionTitle("ВРЕМЯ НАЛОЖЕНИЯ")
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            ru.quasaris.characters.master.CastingTimeType.entries.forEachIndexed { index, type ->
                                SegmentedButton(
                                    selected = state.castingTimeType == type,
                                    onClick = { state = state.copy(castingTimeType = type) },
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = ru.quasaris.characters.master.CastingTimeType.entries.size)
                                ) {
                                    Text(type.displayName, maxLines = 1)
                                }
                            }
                        }
                        
                        val isCastingTimeEnabled = true // Always enabled to allow typing
                        val isCastingTimeActive = state.castingTime.isNotBlank() || 
                                                 state.castingTimeType == ru.quasaris.characters.master.CastingTimeType.REACTION || 
                                                 state.castingTimeType == ru.quasaris.characters.master.CastingTimeType.OTHER
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = state.castingTime,
                            onValueChange = { state = state.copy(castingTime = it) },
                            label = { Text("Описание времени наложения") },
                            modifier = Modifier.fillMaxWidth().alpha(if (isCastingTimeActive) 1f else 0.5f),
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
                                modifier = Modifier.width(90.dp).alpha(if (isValueDisabled) 0.5f else 1f),
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
                                    ru.quasaris.characters.master.DurationUnit.entries.forEach { unit ->
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
                        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp).alpha(if (state.description.isBlank()) 0.5f else 1f),
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Damage
                    Column {
                        SpellCardSectionTitle("УРОН")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Наносимый урон", modifier = Modifier.weight(1f))
                            Switch(checked = state.hasDamage, onCheckedChange = { state = state.copy(hasDamage = it) })
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().alpha(if (state.hasDamage) 1f else 0.5f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = state.damageFormula,
                                onValueChange = { state = state.copy(damageFormula = it) },
                                label = { Text("Формула") },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("1d8") },
                                shape = RoundedCornerShape(8.dp),
                                enabled = state.hasDamage
                            )

                            OutlinedTextField(
                                value = state.damageType,
                                onValueChange = { state = state.copy(damageType = it) },
                                label = { Text("Вид Урона") },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Огненный") },
                                shape = RoundedCornerShape(8.dp),
                                enabled = state.hasDamage
                            )
                        }
                    }

                    // Attack/Save Switch
                    Column {
                        var typeExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = when(state.attackType) {
                                    null -> "Нет"
                                    MagicAttackType.ATTACK -> "Бросок атаки"
                                    MagicAttackType.SAVE -> "Спасбросок"
                                },
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
                                DropdownMenuItem(text = { Text("Нет") }, onClick = { state = state.copy(attackType = null); typeExpanded = false })
                                DropdownMenuItem(text = { Text("Бросок атаки") }, onClick = { state = state.copy(attackType = MagicAttackType.ATTACK); typeExpanded = false })
                                DropdownMenuItem(text = { Text("Спасбросок") }, onClick = { state = state.copy(attackType = MagicAttackType.SAVE); typeExpanded = false })
                            }
                        }
                        
                        val isSave = state.attackType == MagicAttackType.SAVE
                        var attrExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp).alpha(if (isSave) 1f else 0.5f)) {
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
                        modifier = Modifier.fillMaxWidth().alpha(if (state.distance.isBlank()) 0.5f else 1f),
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Notes and Link
                    OutlinedTextField(
                        value = state.notes,
                        onValueChange = { state = state.copy(notes = it) },
                        label = { Text("Заметки") },
                        modifier = Modifier.fillMaxWidth().alpha(if (state.notes.isBlank()) 0.5f else 1f),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = state.link ?: "",
                        onValueChange = { state = state.copy(link = it.ifBlank { null }) },
                        label = { Text("Ссылка") },
                        modifier = Modifier.fillMaxWidth().alpha(if (state.link.isNullOrBlank()) 0.5f else 1f),
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Additional Links
                    val linksActive = state.additionalLinks.isNotEmpty()
                    Column(modifier = Modifier.alpha(if (linksActive) 1f else 0.5f)) {
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
                        Button(onClick = { state = state.copy(additionalLinks = state.additionalLinks + ru.quasaris.characters.master.SpellLink()) }, modifier = Modifier.fillMaxWidth()) {
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

@Composable
fun ComponentToggleButton(label: String, selected: Boolean, modifier: Modifier = Modifier, onToggle: (Boolean) -> Unit) {
    Surface(
        onClick = { onToggle(!selected) },
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.widthIn(min = 48.dp).heightIn(min = 48.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(label, fontWeight = FontWeight.Bold, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SpellCardSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 8.dp)
    )
}
