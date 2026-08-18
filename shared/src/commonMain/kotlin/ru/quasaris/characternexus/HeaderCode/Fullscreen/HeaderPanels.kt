package ru.quasaris.characternexus.HeaderCode.Fullscreen

import ru.quasaris.characternexus.model.*
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.quasaris.characternexus.*
import ru.quasaris.characternexus.tabs.attacks.SectionHeader
import ru.quasaris.characternexus.tabs.attacks.AttackBonusIndicator
import ru.quasaris.characternexus.backend.parseFormulaParts
import ru.quasaris.characternexus.backend.DicePart
import ru.quasaris.characternexus.tabs.attacks.AttackBonusField
import ru.quasaris.characternexus.tabs.attacks.AddBonusButton
import ru.quasaris.characternexus.HeaderCode.getFullFormula

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
                        containerColor = if (forceBlurEnabled && !isOled) Color.Transparent.copy(alpha = 0.1f) else colorScheme.surface
                    )
                )
            },
            containerColor = if (forceBlurEnabled && !isOled) Color.Transparent.copy(alpha = 0.1f) else colorScheme.background
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

internal fun calculateEntryTotal(entry: FormulaEntry?, statsMap: Map<String, String>, type: String): Pair<Int, List<DicePart>> {
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

internal fun updateEntry(entry: FormulaEntry, name: String? = null, formula: String? = null, bonuses: List<AttackBonus>? = null): FormulaEntry {
    return when(entry) {
        is ArmorClassEntry -> entry.copy(name = name ?: entry.name, formula = formula ?: entry.formula, bonuses = bonuses ?: entry.bonuses)
        is InitiativeEntry -> entry.copy(name = name ?: entry.name, formula = formula ?: entry.formula, bonuses = bonuses ?: entry.bonuses)
        is SpeedEntry -> entry.copy(name = name ?: entry.name, formula = formula ?: entry.formula, bonuses = bonuses ?: entry.bonuses)
        is ShieldEntry -> entry.copy(name = name ?: entry.name, formula = formula ?: entry.formula, bonuses = bonuses ?: entry.bonuses)
        else -> entry
    }
}
