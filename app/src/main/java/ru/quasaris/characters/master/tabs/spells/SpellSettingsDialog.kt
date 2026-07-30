package ru.quasaris.characters.master.tabs.spells

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.quasaris.characters.master.Attribute
import ru.quasaris.characters.master.CasterType
import ru.quasaris.characters.master.SpellMode
import ru.quasaris.characters.master.SpellSettings
import ru.quasaris.characters.master.SpecialSlotSettings
import ru.quasaris.characters.master.backend.SpellSlotCalculator
import ru.quasaris.characters.master.tabs.attacks.DiceIcon
import ru.quasaris.characters.master.tabs.attacks.DicePart
import ru.quasaris.characters.master.tabs.attacks.parseFormulaParts
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.HazeStyle
import ru.quasaris.characters.master.backend.calculateModifier
import ru.quasaris.characters.master.backend.getProficiencyBonus
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.HazeInputScale
import kotlin.math.floor

fun formatFloat(value: Float): String {
    return if (value == floor(value)) value.toInt().toString() else value.toString()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SpellSettingsDialog(
    settings: SpellSettings,
    characterLevel: Int,
    onSettingsChange: (SpellSettings) -> Unit,
    onDismiss: () -> Unit,
    hazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false,
    statsMap: Map<String, String> = emptyMap()
) {
    val focusManager = LocalFocusManager.current
    var isMagicEnabled by remember { mutableStateOf(settings.isMagicEnabled) }
    var spellcastingAbility by remember { mutableStateOf(settings.spellcastingAbility) }
    var spellAttackBonuses by remember { mutableStateOf(settings.spellAttackBonuses) }
    var spellSaveDcBonuses by remember { mutableStateOf(settings.spellSaveDcBonuses) }
    var spellMode by remember { mutableStateOf(settings.spellMode) }

    var showAttackBonusDialog by remember { mutableStateOf(false) }
    var showSaveDcBonusDialog by remember { mutableStateOf(false) }

    val pb = getProficiencyBonus(characterLevel.toString())
    val currentAbilityModifier = remember(spellcastingAbility, statsMap) {
        if (spellcastingAbility != Attribute.NONE) {
            val statKey = when (spellcastingAbility) {
                Attribute.STRENGTH -> "strength"
                Attribute.DEXTERITY -> "dexterity"
                Attribute.CONSTITUTION -> "constitution"
                Attribute.INTELLIGENCE -> "intelligence"
                Attribute.WISDOM -> "wisdom"
                Attribute.CHARISMA -> "charisma"
                else -> ""
            }
            calculateModifier(statsMap[statKey] ?: "10")
        } else 0
    }

    val currentAttackBonus = remember(spellAttackBonuses, currentAbilityModifier, pb, statsMap) {
        var totalFlat = pb + currentAbilityModifier
        val allDice = mutableMapOf<Int, Int>()
        spellAttackBonuses.forEach { bonus ->
            val (fFlat, fDice) = parseFormulaParts(bonus.formula, statsMap)
            totalFlat += fFlat
            fDice.forEach { allDice[it.sides] = (allDice[it.sides] ?: 0) + it.count }
        }
        Pair(totalFlat, allDice.map { DicePart(it.value, it.key) }.sortedBy { it.sides })
    }

    val currentSaveDc = remember(spellSaveDcBonuses, currentAbilityModifier, pb, statsMap) {
        var totalFlat = 8 + pb + currentAbilityModifier
        val allDice = mutableMapOf<Int, Int>()
        spellSaveDcBonuses.forEach { bonus ->
            val (fFlat, fDice) = parseFormulaParts(bonus.formula, statsMap)
            totalFlat += fFlat
            fDice.forEach { allDice[it.sides] = (allDice[it.sides] ?: 0) + it.count }
        }
        Pair(totalFlat, allDice.map { DicePart(it.value, it.key) }.sortedBy { it.sides })
    }

    val spellAttackBonusPreview = if (currentAttackBonus.first >= 0) "+${currentAttackBonus.first}" else currentAttackBonus.first.toString()
    val spellSaveDcPreview = currentSaveDc.first.toString()

    var casterType by remember { mutableStateOf(settings.casterType) }
    var isMulticlass by remember { mutableStateOf(settings.isMulticlass) }
    var fullCasterLevel by remember { mutableIntStateOf(settings.fullCasterLevel) }
    var halfCasterLevel by remember { mutableIntStateOf(settings.halfCasterLevel) }
    var thirdCasterLevel by remember { mutableIntStateOf(settings.thirdCasterLevel) }
    
    val specialSlots = remember { mutableStateListOf<SpecialSlotSettings>().apply { addAll(settings.specialSlots) } }
    var overrideSlots by remember { mutableStateOf(settings.overrideSlots) }
    var pactSlotLevel by remember { mutableStateOf(settings.pactSlotLevel) }
    var pactSlotsCount by remember { mutableIntStateOf(settings.pactSlotsCount) }
    var isPactEnabled by remember { mutableStateOf(settings.isPactEnabled) }
    
    var isSpecialSlotsEditMode by remember { mutableStateOf(false) }

    val autoSlots = remember(casterType, isMulticlass, fullCasterLevel, halfCasterLevel, thirdCasterLevel, characterLevel) {
        if (isMulticlass) {
            SpellSlotCalculator.getMulticlassSlots(fullCasterLevel, halfCasterLevel, thirdCasterLevel)
        } else {
            SpellSlotCalculator.getSlotsForLevel(casterType, characterLevel)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val colorScheme = MaterialTheme.colorScheme
        val isOled = colorScheme.background == Color.Black

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Настройки заклинаний", fontWeight = FontWeight.Black) },
                    navigationIcon = {
                        IconButton(onClick = {
                            focusManager.clearFocus()
                            onDismiss()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = if (forceBlurEnabled && !isOled) Color.Transparent else colorScheme.surface
                    )
                )
            },
            containerColor = if (forceBlurEnabled && !isOled) Color.Transparent else colorScheme.background,
            modifier = Modifier
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, _ ->
                            change.consume()
                            focusManager.clearFocus()
                        }
                    )
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { focusManager.clearFocus() }
                .run {
                    if (forceBlurEnabled && hazeState != null && !isOled) {
                        hazeEffect(state = hazeState) {
                            style = HazeStyle(blurRadius = 24.dp, tints = listOf(HazeTint(colorScheme.surface.copy(alpha = 0.1f))))
                            inputScale = HazeInputScale.Fixed(0.7f)
                        }
                    } else this
                }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
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
                            Text("Использовать магию", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                if (isMagicEnabled) "Магия активна" else "Магия отключена",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = isMagicEnabled, onCheckedChange = { isMagicEnabled = it })
                    }
                }

                if (isMagicEnabled) {
                    // Characteristic Selection
                    SectionTitle("ХАРАКТЕРИСТИКА")
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = if (spellcastingAbility == Attribute.NONE) "Без характеристики" else spellcastingAbility.fullName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Ключевая характеристика") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            Attribute.entries.forEach { attr ->
                                DropdownMenuItem(
                                    text = { Text(if (attr == Attribute.NONE) "Без характеристики" else attr.fullName) },
                                    onClick = {
                                        spellcastingAbility = attr
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Bonuses with Preview
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedCard(
                            onClick = { showSaveDcBonusDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Спасбросок", style = MaterialTheme.typography.labelSmall, color = colorScheme.primary)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(spellSaveDcPreview, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    if (currentSaveDc.second.isNotEmpty()) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            currentSaveDc.second.forEach { DiceIcon(it) }
                                        }
                                    }
                                }
                            }
                        }
                        OutlinedCard(
                            onClick = { showAttackBonusDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Бонус атаки", style = MaterialTheme.typography.labelSmall, color = colorScheme.primary)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(spellAttackBonusPreview, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    if (currentAttackBonus.second.isNotEmpty()) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            currentAttackBonus.second.forEach { DiceIcon(it) }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Spell Mode
                    SectionTitle("РЕЖИМ ЗАКЛИНАНИЙ")
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = spellMode == SpellMode.TEXT,
                            onClick = { spellMode = SpellMode.TEXT },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) {
                            Text("Текст")
                        }
                        SegmentedButton(
                            selected = spellMode == SpellMode.CARDS,
                            onClick = { /* spellMode = SpellMode.CARDS */ },
                            enabled = false,
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Text("Карточки")
                        }
                    }

                    // Caster Type / Multiclass
                    SectionTitle("КОЛИЧЕСТВО ЯЧЕЕК")
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Мультикласс", modifier = Modifier.weight(1f))
                        Switch(checked = isMulticlass, onCheckedChange = { isMulticlass = it })
                    }

                    if (isMulticlass) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            LevelInputRow("Заклинатель", fullCasterLevel) { fullCasterLevel = it }
                            LevelInputRow("Полузаклинатель", halfCasterLevel) { halfCasterLevel = it }
                            LevelInputRow("Особый заклинатель", thirdCasterLevel) { thirdCasterLevel = it }
                        }
                    } else {
                        var casterExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = casterExpanded,
                            onExpandedChange = { casterExpanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = casterType.displayName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Тип заклинателя") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = casterExpanded) },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = casterExpanded,
                                onDismissRequest = { casterExpanded = false }
                            ) {
                                CasterType.entries.forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type.displayName) },
                                        onClick = {
                                            casterType = type
                                            casterExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Slot Overrides Grid (3x3)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (row in 0 until 3) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                for (col in 1..3) {
                                    val level = row * 3 + col
                                    val levelFloat = level.toFloat()
                                    val manualValue = overrideSlots[levelFloat]
                                    val autoValue = autoSlots[level - 1]

                                    val displayValue = when {
                                        manualValue != null -> if (manualValue == 0) "" else manualValue.toString()
                                        else -> if (autoValue == 0) "" else autoValue.toString()
                                    }

                                    OutlinedTextField(
                                        value = displayValue,
                                        onValueChange = { newValue ->
                                            if (newValue.isBlank()) {
                                                overrideSlots = overrideSlots.toMutableMap().apply { put(levelFloat, 0) }
                                            } else {
                                                val count = newValue.toIntOrNull()
                                                if (count != null) {
                                                    overrideSlots = overrideSlots.toMutableMap().apply { put(levelFloat, count) }
                                                }
                                            }
                                        },
                                        label = { Text("${level}-й") },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        trailingIcon = if (overrideSlots.containsKey(levelFloat)) {
                                            {
                                                IconButton(onClick = {
                                                    overrideSlots = overrideSlots.toMutableMap().apply { remove(levelFloat) }
                                                }) {
                                                    Icon(Icons.Default.Refresh, contentDescription = "Сброс", modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        } else null
                                    )
                                }
                            }
                        }
                    }

                    // Special Slots Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SectionTitle("ОСОБЫЕ ЯЧЕЙКИ")
                        if (specialSlots.isNotEmpty()) {
                            IconButton(onClick = { isSpecialSlotsEditMode = !isSpecialSlotsEditMode }) {
                                Icon(
                                    if (isSpecialSlotsEditMode) Icons.Default.EditOff else Icons.Default.Edit,
                                    contentDescription = "Редактировать",
                                    tint = if (isSpecialSlotsEditMode) colorScheme.primary else colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Special Slots
                    val specialSlotPositions = remember { mutableStateMapOf<Int, Float>() }
                    var draggedSlotIndex by remember { mutableStateOf<Int?>(null) }
                    var draggingSlotOffset by remember { mutableStateOf(0f) }

                    specialSlots.forEachIndexed { index, slot ->
                        SpecialSlotItem(
                            slot = slot,
                            index = index,
                            isEditMode = isSpecialSlotsEditMode,
                            isDragging = draggedSlotIndex == index,
                            onSlotChange = { updatedSlot -> specialSlots[index] = updatedSlot },
                            onDelete = { specialSlots.removeAt(index) },
                            onDrag = { offset ->
                                draggedSlotIndex = index
                                draggingSlotOffset += offset
                                
                                val currentPos = (specialSlotPositions[index] ?: 0f) + draggingSlotOffset
                                val targetIndex = specialSlotPositions.entries
                                    .filter { it.key != index }
                                    .find { currentPos in it.value..(it.value + 100f) } // Rough height check
                                    ?.key

                                if (targetIndex != null) {
                                    val item = specialSlots.removeAt(index)
                                    specialSlots.add(targetIndex, item)
                                    draggedSlotIndex = targetIndex
                                    draggingSlotOffset = 0f
                                }
                            },
                            onDragEnd = {
                                draggedSlotIndex = null
                                draggingSlotOffset = 0f
                            },
                            modifier = Modifier.onGloballyPositioned { coords ->
                                specialSlotPositions[index] = coords.positionInWindow().y
                            }
                        )
                    }
                    
                    Button(
                        onClick = { specialSlots.add(SpecialSlotSettings(level = 0f, count = 0)) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ДОБАВИТЬ ОСОБУЮ ЯЧЕЙКУ")
                    }

                    // Pact Slots
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SectionTitle("ЯЧЕЙКИ ДОГОВОРА")
                        Switch(checked = isPactEnabled, onCheckedChange = { isPactEnabled = it })
                    }

                    AnimatedVisibility(visible = isPactEnabled) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                var pactLevelText by remember { mutableStateOf(formatFloat(pactSlotLevel)) }
                                OutlinedTextField(
                                    value = pactLevelText,
                                    onValueChange = { newVal ->
                                        pactLevelText = newVal
                                        newVal.toFloatOrNull()?.let { pactSlotLevel = it }
                                    },
                                    label = { Text("Уровень") },
                                    placeholder = {},
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                )
                                OutlinedTextField(
                                    value = if (pactSlotsCount == 0) "" else pactSlotsCount.toString(),
                                    onValueChange = { newVal ->
                                        if (newVal.isBlank()) {
                                            pactSlotsCount = 0
                                        } else {
                                            val v = newVal.toIntOrNull()
                                            if (v != null) pactSlotsCount = v
                                        }
                                    },
                                    label = { Text("Количество") },
                                    placeholder = {},
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        onSettingsChange(
                            settings.copy(
                                isMagicEnabled = isMagicEnabled,
                                spellcastingAbility = spellcastingAbility,
                                spellAttackBonuses = spellAttackBonuses,
                                spellSaveDcBonuses = spellSaveDcBonuses,
                                spellMode = spellMode,
                                casterType = casterType,
                                isMulticlass = isMulticlass,
                                fullCasterLevel = fullCasterLevel,
                                halfCasterLevel = halfCasterLevel,
                                thirdCasterLevel = thirdCasterLevel,
                                specialSlots = specialSlots.toList(),
                                overrideSlots = overrideSlots,
                                pactSlotLevel = pactSlotLevel,
                                pactSlotsCount = pactSlotsCount,
                                isPactEnabled = isPactEnabled,
                                usedSlots = settings.usedSlots.filterKeys { it in overrideSlots.keys || it == pactSlotLevel || it in specialSlots.map { s -> s.level } },
                                usedSlotsShortRest = settings.usedSlotsShortRest.filterKeys { it in overrideSlots.keys || it == pactSlotLevel || it in specialSlots.map { s -> s.level } }
                            )
                        )
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Сохранить", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showAttackBonusDialog) {
        MagicBonusSettingsDialog(
            title = "Магическая атака",
            bonuses = spellAttackBonuses,
            baseModifier = pb + currentAbilityModifier,
            stats = statsMap,
            proficiencyBonus = pb,
            onDismiss = { showAttackBonusDialog = false },
            onSave = {
                spellAttackBonuses = it
                showAttackBonusDialog = false
            },
            hazeState = hazeState,
            forceBlurEnabled = forceBlurEnabled
        )
    }

    if (showSaveDcBonusDialog) {
        MagicBonusSettingsDialog(
            title = "Магическая сложность",
            bonuses = spellSaveDcBonuses,
            baseModifier = 8 + pb + currentAbilityModifier,
            stats = statsMap,
            proficiencyBonus = pb,
            onDismiss = { showSaveDcBonusDialog = false },
            onSave = {
                spellSaveDcBonuses = it
                showSaveDcBonusDialog = false
            },
            hazeState = hazeState,
            forceBlurEnabled = forceBlurEnabled
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecialSlotItem(
    slot: SpecialSlotSettings,
    index: Int,
    isEditMode: Boolean,
    isDragging: Boolean,
    onSlotChange: (SpecialSlotSettings) -> Unit,
    onDelete: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(targetValue = if (isDragging) 1.05f else 1f, label = "scale")
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        AnimatedContent(
            targetState = isEditMode,
            transitionSpec = {
                (fadeIn() + expandVertically()).togetherWith(fadeOut() + shrinkVertically())
            },
            label = "SpecialSlotContent"
        ) { editMode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(if (editMode) 4.dp else 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (editMode) {
                    Icon(
                        imageVector = Icons.Default.UnfoldMore,
                        contentDescription = "Reorder",
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .size(32.dp)
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        onDrag(dragAmount.y)
                                    },
                                    onDragEnd = onDragEnd,
                                    onDragCancel = onDragEnd
                                )
                            },
                        tint = colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = if (editMode) 8.dp else 0.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (editMode) {
                        Text(
                            text = slot.name.ifBlank { "Без названия" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = slot.name,
                                onValueChange = { onSlotChange(slot.copy(name = it)) },
                                label = { Text("Название") },
                                placeholder = { Text("Название") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            IconButton(onClick = onDelete) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Delete",
                                    tint = colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            var levelText by remember(slot.id) { mutableStateOf(formatFloat(slot.level)) }
                            OutlinedTextField(
                                value = levelText,
                                onValueChange = { newVal ->
                                    levelText = newVal
                                    newVal.toFloatOrNull()?.let { onSlotChange(slot.copy(level = it)) }
                                },
                                label = { Text("Уровень") },
                                placeholder = {},
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                trailingIcon = if (slot.level != 1f) {
                                    {
                                        IconButton(onClick = { 
                                            onSlotChange(slot.copy(level = 1f))
                                            levelText = "1"
                                        }) {
                                            Icon(Icons.Default.Refresh, contentDescription = "Сброс", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                } else null
                            )
                            OutlinedTextField(
                                value = if (slot.count == 0) "" else slot.count.toString(),
                                onValueChange = { newVal ->
                                    if (newVal.isBlank()) {
                                        onSlotChange(slot.copy(count = 0))
                                    } else {
                                        val c = newVal.toIntOrNull()
                                        if (c != null) onSlotChange(slot.copy(count = c))
                                    }
                                },
                                label = { Text("Количество") },
                                placeholder = {},
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Восстановление", style = MaterialTheme.typography.bodyMedium)

                            SingleChoiceSegmentedButtonRow {
                                SegmentedButton(
                                    selected = slot.restoreOnShortRest,
                                    onClick = { onSlotChange(slot.copy(restoreOnShortRest = true)) },
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                                ) {
                                    Icon(
                                        Icons.Default.WbSunny,
                                        contentDescription = "Short Rest",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                SegmentedButton(
                                    selected = !slot.restoreOnShortRest,
                                    onClick = { onSlotChange(slot.copy(restoreOnShortRest = false)) },
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                                ) {
                                    Icon(
                                        Icons.Default.NightsStay,
                                        contentDescription = "Long Rest",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LevelInputRow(label: String, value: Int, onValueChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f))
        OutlinedTextField(
            value = if (value == 0) "" else value.toString(),
            onValueChange = { newVal ->
                if (newVal.isBlank()) {
                    onValueChange(0)
                } else {
                    val v = newVal.toIntOrNull()
                    if (v != null) onValueChange(v.coerceIn(0, 20))
                }
            },
            modifier = Modifier.width(80.dp),
            shape = RoundedCornerShape(8.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
            singleLine = true
        )
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 8.dp)
    )
}
