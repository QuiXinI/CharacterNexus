package ru.quasaris.characters.master.HeaderCode

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.HazeInputScale
import ru.quasaris.characters.master.ArmorClassEntry
import ru.quasaris.characters.master.FormulaEntry
import ru.quasaris.characters.master.InitiativeEntry
import ru.quasaris.characters.master.ShieldEntry
import ru.quasaris.characters.master.SpeedEntry
import ru.quasaris.characters.master.AttackBonus
import ru.quasaris.characters.master.AdvantagePreference
import ru.quasaris.characters.master.backend.Condition
import ru.quasaris.characters.master.tabs.attacks.SectionHeader
import ru.quasaris.characters.master.tabs.attacks.AttackBonusIndicator
import ru.quasaris.characters.master.backend.parseFormulaParts
import ru.quasaris.characters.master.backend.DicePart
import ru.quasaris.characters.master.tabs.attacks.AttackBonusField
import ru.quasaris.characters.master.tabs.attacks.AddBonusButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedStatDialog(
    title: String,
    statType: String, // "AC", "INIT", "SPEED"
    activeEntry: FormulaEntry?,
    allEntries: List<FormulaEntry>,
    onAllEntriesChange: (List<FormulaEntry>) -> Unit,
    onActiveIdChange: (String?) -> Unit,
    statsMap: Map<String, String>,
    hazeState: HazeState?,
    forceBlurEnabled: Boolean,
    onDismiss: () -> Unit,
    // AC Specific
    isShieldActive: Boolean = false,
    onShieldActiveChange: (Boolean) -> Unit = {},
    activeShield: ShieldEntry? = null,
    allShields: List<ShieldEntry> = emptyList(),
    onShieldChange: (ShieldEntry) -> Unit = {},
    onAllShieldsChange: (List<ShieldEntry>) -> Unit = {},
    onActiveShieldIdChange: (String?) -> Unit = {}
) {
    var editingEntry by remember { mutableStateOf<FormulaEntry?>(null) }
    var editingShield by remember { mutableStateOf<ShieldEntry?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val colorScheme = MaterialTheme.colorScheme
        val isOled = colorScheme.background == Color.Black

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(title, fontWeight = FontWeight.Black) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть")
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
                    // Total Indicator
                    val totalCalc = remember(activeEntry, activeShield, isShieldActive, statsMap, statType) {
                        val baseVal = calculateEntryTotal(activeEntry, statsMap, statType)
                        if (statType == "AC" && isShieldActive && activeShield != null) {
                            val sVal = calculateEntryTotal(activeShield, statsMap, "SHIELD")
                            Pair(baseVal.first + sVal.first, baseVal.second + sVal.second)
                        } else baseVal
                    }

                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                        AttackBonusIndicator(
                            bonus = totalCalc.first,
                            dice = totalCalc.second,
                            size = 140.dp,
                            fontSize = 54.sp,
                            showLabel = false,
                            showPlus = statType != "AC" && statType != "SPEED",
                            diceSize = if (statType == "INIT") 44.dp else 24.dp
                        )
                    }

                    // Variants Section
                    SectionHeader("Варианты")
                    allEntries.forEach { entry ->
                        StatVariantItem(
                            entry = entry,
                            isActive = entry.id == activeEntry?.id,
                            statsMap = statsMap,
                            statType = statType,
                            onClick = { onActiveIdChange(entry.id) },
                            onLongClick = { editingEntry = entry }
                        )
                    }

                    Button(
                        onClick = {
                            val new = when(statType) {
                                "AC" -> ArmorClassEntry()
                                "INIT" -> InitiativeEntry()
                                "SPEED" -> SpeedEntry()
                                else -> ArmorClassEntry()
                            }
                            onAllEntriesChange(allEntries + new)
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Добавить вариант")
                    }

                    // AC Specific: Shield Section
                    if (statType == "AC") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            SectionHeader("Щит")
                            Switch(
                                checked = isShieldActive,
                                onCheckedChange = onShieldActiveChange,
                                modifier = Modifier.scale(0.8f)
                            )
                        }

                        allShields.forEach { shield ->
                            StatVariantItem(
                                entry = shield,
                                isActive = shield.id == activeShield?.id,
                                statsMap = statsMap,
                                statType = "SHIELD",
                                onClick = { onActiveShieldIdChange(shield.id) },
                                onLongClick = { editingShield = shield }
                            )
                        }

                        Button(
                            onClick = { onAllShieldsChange(allShields + ShieldEntry()) },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Добавить щит")
                        }
                    }

                    Spacer(modifier = Modifier.height(100.dp))
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Готово", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Sub-Dialogs for Editing
    editingEntry?.let { entry ->
        EditVariantDialog(
            title = "Настройка: ${entry.name.ifBlank { title }}",
            entry = entry,
            statsMap = statsMap,
            statType = statType,
            onSave = { updated ->
                val newList = allEntries.toMutableList()
                val idx = newList.indexOfFirst { it.id == updated.id }
                if (idx != -1) {
                    newList[idx] = updated
                    onAllEntriesChange(newList)
                }
                editingEntry = null
            },
            onDelete = {
                val newList = allEntries.toMutableList()
                newList.removeAll { it.id == entry.id }
                if (entry.id == activeEntry?.id) onActiveIdChange(null)
                onAllEntriesChange(newList)
                editingEntry = null
            },
            onDismiss = { editingEntry = null },
            hazeState = hazeState,
            forceBlurEnabled = forceBlurEnabled
        )
    }

    editingShield?.let { shield ->
        EditVariantDialog(
            title = "Настройка щита: ${shield.name.ifBlank { "Без названия" }}",
            entry = shield,
            statsMap = statsMap,
            statType = "SHIELD",
            onSave = { updated ->
                val newList = allShields.toMutableList()
                val idx = newList.indexOfFirst { it.id == updated.id }
                if (idx != -1) {
                    newList[idx] = updated as ShieldEntry
                    onAllShieldsChange(newList)
                }
                editingShield = null
            },
            onDelete = {
                val newList = allShields.toMutableList()
                newList.removeAll { it.id == shield.id }
                if (shield.id == activeShield?.id) onActiveShieldIdChange(null)
                onAllShieldsChange(newList)
                editingShield = null
            },
            onDismiss = { editingShield = null },
            hazeState = hazeState,
            forceBlurEnabled = forceBlurEnabled
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StatVariantItem(
    entry: FormulaEntry,
    isActive: Boolean,
    statsMap: Map<String, String>,
    statType: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val totalVal = calculateEntryTotal(entry, statsMap, statType)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) colorScheme.primaryContainer.copy(alpha = 0.5f) else colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (isActive) BorderStroke(2.dp, colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.name.ifBlank { "Без названия" }, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium)
                Text(
                    text = getFullFormula(entry),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (entry is InitiativeEntry && entry.hasAdvantage) {
                    Icon(Icons.Default.KeyboardArrowUp, null, tint = colorScheme.primary, modifier = Modifier.size(24.dp))
                }
                
                val displayVal = if (statType != "AC" && statType != "SPEED" && totalVal.first >= 0) "+${totalVal.first}" else totalVal.first.toString()
                Text(
                    text = displayVal,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = if (isActive) colorScheme.primary else colorScheme.onSurface
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditVariantDialog(
    title: String,
    entry: FormulaEntry,
    statsMap: Map<String, String>,
    statType: String,
    onSave: (FormulaEntry) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    hazeState: HazeState?,
    forceBlurEnabled: Boolean
) {
    var state by remember { mutableStateOf(entry) }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val colorScheme = MaterialTheme.colorScheme
        val isOled = colorScheme.background == Color.Black

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(title, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                        }
                    },
                    actions = {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, null, tint = colorScheme.error)
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
                    // Indicator for this variant
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        val calc = calculateEntryTotal(state, statsMap, statType)
                        AttackBonusIndicator(
                            bonus = calc.first,
                            dice = calc.second,
                            showLabel = false,
                            showPlus = statType != "AC" && statType != "SPEED"
                        )
                    }

                    SectionHeader("Основное")
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = { s -> state = updateEntry(state, name = s) },
                        label = { Text("Название") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = state.formula,
                        onValueChange = { s -> state = updateEntry(state, formula = s) },
                        label = { Text("Формула") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    
                    if (state is InitiativeEntry) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    state = (state as InitiativeEntry).copy(hasAdvantage = !(state as InitiativeEntry).hasAdvantage)
                                },
                            colors = CardDefaults.cardColors(containerColor = colorScheme.primary.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp), 
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Преимущество", modifier = Modifier.weight(1f), fontWeight = FontWeight.Black, fontSize = 18.sp)
                                Icon(
                                    Icons.Default.KeyboardArrowUp, 
                                    null, 
                                    tint = if ((state as InitiativeEntry).hasAdvantage) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.2f),
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }
                    }

                    SectionHeader("Бонусы")
                    val bonuses = when(state) {
                        is ArmorClassEntry -> state.bonuses
                        is InitiativeEntry -> state.bonuses
                        is SpeedEntry -> state.bonuses
                        is ShieldEntry -> state.bonuses
                        else -> emptyList()
                    }

                    bonuses.forEachIndexed { index, bonus ->
                        AttackBonusField(
                            bonus = bonus,
                            showAdvantageLogic = state is InitiativeEntry,
                            onUpdate = { updated ->
                                val newList = bonuses.toMutableList()
                                newList[index] = updated
                                state = updateEntry(state, bonuses = newList)
                            },
                            onDelete = {
                                val newList = bonuses.toMutableList()
                                newList.removeAt(index)
                                state = updateEntry(state, bonuses = newList)
                            }
                        )
                    }

                    AddBonusButton {
                        state = updateEntry(state, bonuses = bonuses + AttackBonus(advantagePreference = AdvantagePreference.NONE))
                    }

                    Spacer(modifier = Modifier.height(80.dp))
                }

                Button(
                    onClick = { onSave(state) },
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
    }
}

private fun calculateEntryTotal(entry: FormulaEntry?, statsMap: Map<String, String>, type: String): Pair<Int, List<DicePart>> {
    if (entry == null) return Pair(if (type == "AC") 10 else 0, emptyList())
    val (fFlat, fDice) = parseFormulaParts(entry.formula, statsMap)
    var total = fFlat
    val allDice = mutableListOf<DicePart>()
    if (type == "INIT") {
        allDice.addAll(fDice)
    }
    
    val bonuses = when(entry) {
        is ArmorClassEntry -> entry.bonuses
        is InitiativeEntry -> entry.bonuses
        is SpeedEntry -> entry.bonuses
        is ShieldEntry -> entry.bonuses
        else -> emptyList()
    }
    
    bonuses.filter { it.isActive }.forEach { b ->
        val (bF, bD) = parseFormulaParts(b.formula, statsMap)
        total += bF
        if (type == "INIT") {
            allDice.addAll(bD)
        }
    }
    
    // Dice combination logic
    val combinedDice = mutableMapOf<Int, Int>()
    allDice.forEach { combinedDice[it.sides] = (combinedDice[it.sides] ?: 0) + it.count }
    val diceList = combinedDice.filter { it.value != 0 }.map { DicePart(it.value, it.key) }.sortedBy { it.sides }
    
    return Pair(total, diceList)
}

private fun updateEntry(entry: FormulaEntry, name: String? = null, formula: String? = null, bonuses: List<AttackBonus>? = null): FormulaEntry {
    return when(entry) {
        is ArmorClassEntry -> entry.copy(name = name ?: entry.name, formula = formula ?: entry.formula, bonuses = bonuses ?: entry.bonuses)
        is InitiativeEntry -> entry.copy(name = name ?: entry.name, formula = formula ?: entry.formula, bonuses = bonuses ?: entry.bonuses)
        is SpeedEntry -> entry.copy(name = name ?: entry.name, formula = formula ?: entry.formula, bonuses = bonuses ?: entry.bonuses)
        is ShieldEntry -> entry.copy(name = name ?: entry.name, formula = formula ?: entry.formula, bonuses = bonuses ?: entry.bonuses)
        else -> entry
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedHealthSettingsDialog(
    currentHitDie: Int,
    onHitDieChange: (Int) -> Unit,
    hazeState: HazeState?,
    forceBlurEnabled: Boolean,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val colorScheme = MaterialTheme.colorScheme
        val isOled = colorScheme.background == Color.Black

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Настройки Хитов", fontWeight = FontWeight.Black) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть")
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SectionHeader("Кость Хитов")
                
                var expanded by remember { mutableStateOf(false) }
                val options = listOf(4, 6, 8, 10, 12, 20)
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = "d$currentHitDie",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Тип кости хитов") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        options.forEach { option ->
                            DropdownMenuItem(
                                text = { Text("d$option") },
                                onClick = {
                                    onHitDieChange(option)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Text(
                    "Эта кость будет использоваться по умолчанию при бросках на коротком отдыхе.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedConditionsDialog(
    allConditions: List<Condition>,
    selectedConditions: List<String>,
    onToggleCondition: (String) -> Unit,
    exhaustion: Int,
    onExhaustionChange: (Int) -> Unit,
    hazeState: HazeState?,
    forceBlurEnabled: Boolean,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val colorScheme = MaterialTheme.colorScheme
        val isOled = colorScheme.background == Color.Black

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Состояния и Истощение", fontWeight = FontWeight.Black) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть")
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Exhaustion Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = colorScheme.errorContainer.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Уровень Истощения", color = colorScheme.error, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 16.dp)) {
                            IconButton(onClick = { if (exhaustion > 0) onExhaustionChange(exhaustion - 1) }) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = colorScheme.error, modifier = Modifier.size(48.dp))
                            }
                            Text(exhaustion.toString(), color = colorScheme.error, fontSize = 72.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 32.dp))
                            IconButton(onClick = { if (exhaustion < 6) onExhaustionChange(exhaustion + 1) }) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = colorScheme.error, modifier = Modifier.size(48.dp))
                            }
                        }
                        
                        if (exhaustion > 0) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 8.dp)) {
                                if (exhaustion == 6) Text("СМЕРТЬ", color = colorScheme.error, fontWeight = FontWeight.Black, fontSize = 20.sp)
                                Text("-${exhaustion * 2} к проверкам к20", color = colorScheme.onSurfaceVariant)
                                Text("-${exhaustion * 5} фт к скорости", color = colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                SectionHeader("Активные состояния")
                
                allConditions.forEach { condition ->
                    val isSelected = selectedConditions.contains(condition.name)
                    EnhancedConditionItem(condition, isSelected) { onToggleCondition(condition.name) }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun EnhancedConditionItem(condition: Condition, isSelected: Boolean, onToggle: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) colorScheme.primaryContainer.copy(alpha = 0.5f) else colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isSelected, onCheckedChange = { onToggle() })
                Text(
                    condition.name, 
                    modifier = Modifier.weight(1f).padding(start = 8.dp), 
                    fontSize = 18.sp, 
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) colorScheme.primary else colorScheme.onSurface
                )
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null,
                    tint = colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            
            AnimatedVisibility(visible = expanded) {
                Text(
                    formatConditionDescription(condition.description, colorScheme.primary),
                    modifier = Modifier.padding(start = 48.dp, top = 8.dp, end = 8.dp),
                    fontSize = 16.sp,
                    color = colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }
    }
}

private fun formatConditionDescription(text: String, primaryColor: Color): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        val lines = text.lines()
        lines.forEachIndexed { i, line ->
            var l = line.trim()
            if (l.startsWith("- ")) {
                withStyle(SpanStyle(fontWeight = FontWeight.Black, color = primaryColor)) {
                    append("• ")
                }
                l = l.substring(2)
            }
            val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
            var last = 0
            boldRegex.findAll(l).forEach { m ->
                append(l.substring(last, m.range.first))
                withStyle(SpanStyle(
                    fontWeight = FontWeight.Bold,
                    color = primaryColor.copy(alpha = 0.9f)
                )) { 
                    append(m.groupValues[1]) 
                }
                last = m.range.last + 1
            }
            append(l.substring(last))
            if (i < lines.size - 1) append("\n")
        }
    }
}
