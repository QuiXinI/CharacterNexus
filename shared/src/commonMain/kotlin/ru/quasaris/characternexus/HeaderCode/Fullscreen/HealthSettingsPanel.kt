package ru.quasaris.characternexus.HeaderCode.Fullscreen

import ru.quasaris.characternexus.model.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import characternexus.shared.generated.resources.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import ru.quasaris.characternexus.ui.DialogDimStyle
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.HazeInputScale
import ru.quasaris.characternexus.*
import ru.quasaris.characternexus.tabs.attacks.SectionHeader
import ru.quasaris.characternexus.backend.evaluateFormula
import ru.quasaris.characternexus.tabs.attacks.AttackBonusField
import ru.quasaris.characternexus.tabs.attacks.AddBonusButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthSettingsDialog(
    isManual: Boolean,
    onManualChange: (Boolean) -> Unit,
    manualMaxHp: Int,
    onManualMaxHpChange: (Int) -> Unit,
    isMulticlass: Boolean,
    onMulticlassChange: (Boolean) -> Unit,
    currentHitDie: Int,
    onHitDieChange: (Int) -> Unit,
    hpLevelData: List<HPLevelEntry>,
    onHPLevelDataChange: (List<HPLevelEntry>) -> Unit,
    manualHPLevelData: List<HPLevelEntry>,
    onManualHPLevelDataChange: (List<HPLevelEntry>) -> Unit,
    manualMaxHitDice: Int,
    onManualMaxHitDiceChange: (Int) -> Unit,
    hpBonusesAtLevel: List<AttackBonus>,
    onHpBonusesAtLevelChange: (List<AttackBonus>) -> Unit,
    hpBonusesTotal: List<AttackBonus>,
    onHpBonusesTotalChange: (List<AttackBonus>) -> Unit,
    statsMap: Map<String, String>,
    level: Int,
    forceBlurEnabled: Boolean,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        DialogDimStyle(0f)
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
                        containerColor = if (forceBlurEnabled && !isOled) Color.Transparent.copy(alpha = 0.1f) else colorScheme.surface
                    )
                )
            },
            bottomBar = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 8.dp,
                    color = if (forceBlurEnabled && !isOled) Color.Transparent.copy(alpha = 0.1f) else colorScheme.surface
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
                    ) {
                        Text("Закрыть", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            },
            containerColor = if (forceBlurEnabled && !isOled) Color.Transparent.copy(alpha = 0.1f) else colorScheme.background
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                HealthSettingsContent(
                    isManual = isManual,
                    onManualChange = onManualChange,
                    manualMaxHp = manualMaxHp,
                    onManualMaxHpChange = onManualMaxHpChange,
                    isMulticlass = isMulticlass,
                    onMulticlassChange = onMulticlassChange,
                    currentHitDie = currentHitDie,
                    onHitDieChange = onHitDieChange,
                    hpLevelData = hpLevelData,
                    onHPLevelDataChange = onHPLevelDataChange,
                    manualHPLevelData = manualHPLevelData,
                    onManualHPLevelDataChange = onManualHPLevelDataChange,
                    manualMaxHitDice = manualMaxHitDice,
                    onManualMaxHitDiceChange = onManualMaxHitDiceChange,
                    hpBonusesAtLevel = hpBonusesAtLevel,
                    onHpBonusesAtLevelChange = onHpBonusesAtLevelChange,
                    hpBonusesTotal = hpBonusesTotal,
                    onHpBonusesTotalChange = onHpBonusesTotalChange,
                    statsMap = statsMap,
                    level = level
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthSettingsContent(
    isManual: Boolean,
    onManualChange: (Boolean) -> Unit,
    manualMaxHp: Int,
    onManualMaxHpChange: (Int) -> Unit,
    isMulticlass: Boolean,
    onMulticlassChange: (Boolean) -> Unit,
    currentHitDie: Int,
    onHitDieChange: (Int) -> Unit,
    hpLevelData: List<HPLevelEntry>,
    onHPLevelDataChange: (List<HPLevelEntry>) -> Unit,
    manualHPLevelData: List<HPLevelEntry>,
    onManualHPLevelDataChange: (List<HPLevelEntry>) -> Unit,
    manualMaxHitDice: Int,
    onManualMaxHitDiceChange: (Int) -> Unit,
    hpBonusesAtLevel: List<AttackBonus>,
    onHpBonusesAtLevelChange: (List<AttackBonus>) -> Unit,
    hpBonusesTotal: List<AttackBonus>,
    onHpBonusesTotalChange: (List<AttackBonus>) -> Unit,
    statsMap: Map<String, String>,
    level: Int
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDarkMode = colorScheme.surface.let { 
        (0.299 * it.red + 0.587 * it.green + 0.114 * it.blue) < 0.5
    }
    val warningColor = if (isDarkMode) Color(0xFFEF9A9A) else Color(0xFFD32F2F)

    data class HPGroupState(val countText: String, val hitDie: Int)

    val localGroups = remember(manualHPLevelData, currentHitDie) {
        val initial = mutableListOf<HPGroupState>()
        if (manualHPLevelData.isNotEmpty()) {
            var currentDie = manualHPLevelData[0].hitDie
            var currentCount = 0
            manualHPLevelData.forEach {
                if (it.hitDie == currentDie) {
                    currentCount++
                } else {
                    initial.add(HPGroupState(currentCount.toString(), currentDie))
                    currentDie = it.hitDie
                    currentCount = 1
                }
            }
            if (currentCount > 0) initial.add(HPGroupState(currentCount.toString(), currentDie))
        } else {
            initial.add(HPGroupState("", currentHitDie))
        }
        mutableStateListOf(*initial.toTypedArray())
    }

    val syncToModel = {
        val newList = mutableListOf<HPLevelEntry>()
        var processed = 0
        localGroups.forEach { group ->
            val count = if (group.countText.isBlank() && !isMulticlass) level else group.countText.toIntOrNull() ?: 0
            for (i in 1..count) {
                processed++
                newList.add(HPLevelEntry(level = processed, hitDie = group.hitDie))
            }
        }
        onManualHPLevelDataChange(newList)
        onManualMaxHitDiceChange(newList.size)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
                // Toggles
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                Text("Настроить вручную", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    if (isManual) "Ручной ввод включен" else "Используются авто-расчеты",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(checked = isManual, onCheckedChange = onManualChange, modifier = Modifier.scale(0.8f))
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
                                Text("Мультикласс", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    if (isMulticlass) "Разные кости хитов активны" else "Одна кость для всех уровней",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(checked = isMulticlass, onCheckedChange = onMulticlassChange, modifier = Modifier.scale(0.8f))
                        }
                    }
                }

                val conMod = remember(statsMap) { evaluateFormula("[CON]", statsMap) }

                if (isManual) {
                    SectionHeader("Ручная настройка")
                    
                    OutlinedTextField(
                        value = if (manualMaxHp == 0) "" else manualMaxHp.toString(),
                        onValueChange = { onManualMaxHpChange(it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0) },
                        label = { Text("Максимальное значение хитов") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )

                    val totalSum = localGroups.sumOf { 
                        if (it.countText.isBlank() && !isMulticlass) level else it.countText.toIntOrNull() ?: 0 
                    }
                    val isWarning = totalSum != level

                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SectionHeader(if (isMulticlass) "Настройка костей хитов" else "Настройка кости хитов")
                        Text(
                            text = "$totalSum / $level",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isWarning) warningColor else colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }

                    localGroups.forEachIndexed { groupIdx, group ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = group.countText,
                                onValueChange = { s ->
                                    val filtered = s.filter { it.isDigit() }
                                    localGroups[groupIdx] = group.copy(countText = filtered)
                                    syncToModel()
                                },
                                readOnly = false,
                                label = { Text("Уровней") },
                                placeholder = {
                                    if (!isMulticlass) {
                                        Text("$level (по умолчанию)")
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                colors = if (isWarning) OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = warningColor,
                                    unfocusedBorderColor = warningColor.copy(alpha = 0.7f),
                                    cursorColor = colorScheme.primary,
                                    focusedLabelColor = warningColor,
                                    unfocusedLabelColor = warningColor.copy(alpha = 0.7f)
                                ) else OutlinedTextFieldDefaults.colors()
                            )

                            var expanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = "d${group.hitDie}",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Кость") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clickable { expanded = true }
                                )
                                
                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    listOf(4, 6, 8, 10, 12, 20).forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text("d$option") },
                                            onClick = {
                                                localGroups[groupIdx] = group.copy(hitDie = option)
                                                syncToModel()
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            
                            if (localGroups.size > 1) {
                                IconButton(onClick = {
                                    localGroups.removeAt(groupIdx)
                                    syncToModel()
                                }) {
                                    Icon(Icons.Default.Delete, null, tint = colorScheme.error)
                                }
                            }
                        }
                    }
                    
                    if (isMulticlass) {
                        TextButton(onClick = {
                            localGroups.add(HPGroupState("", currentHitDie))
                            syncToModel()
                        }) {
                            Icon(Icons.Default.Add, null)
                            Text("Добавить другую кость хитов")
                        }
                    }

                    SectionHeader("Бонус к максимуму хитов")
                    hpBonusesTotal.forEachIndexed { idx, bonus ->
                        AttackBonusField(
                            bonus = bonus,
                            showAdvantageLogic = false,
                            onUpdate = { updated ->
                                val newList = hpBonusesTotal.toMutableList()
                                newList[idx] = updated
                                onHpBonusesTotalChange(newList)
                            },
                            onDelete = {
                                val newList = hpBonusesTotal.toMutableList()
                                newList.removeAt(idx)
                                onHpBonusesTotalChange(newList)
                            }
                        )
                    }
                    AddBonusButton {
                        onHpBonusesTotalChange(hpBonusesTotal + AttackBonus())
                    }

                } else {
                    val levelBonus = remember(hpBonusesAtLevel, statsMap) {
                        hpBonusesAtLevel.filter { it.isActive }.sumOf { evaluateFormula(it.formula, statsMap) }
                    }

                    val currentData = hpLevelData.take(level)

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Ур", modifier = Modifier.weight(0.15f), fontWeight = FontWeight.Bold, fontSize = 12.sp * 1.15f)
                        Text("Кость", modifier = Modifier.weight(0.25f), fontWeight = FontWeight.Bold, fontSize = 12.sp * 1.15f)
                        Text("Результат", modifier = Modifier.weight(0.35f), fontWeight = FontWeight.Bold, fontSize = 12.sp * 1.15f)
                        Text("Итого", modifier = Modifier.weight(0.25f), fontWeight = FontWeight.Bold, fontSize = 12.sp * 1.15f, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                    }

                    Box(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            currentData.forEachIndexed { index, entry ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${entry.level}", modifier = Modifier.weight(0.15f), fontWeight = FontWeight.Bold, fontSize = 16.sp * 1.15f)

                                    Box(modifier = Modifier.weight(0.25f)) {
                                        var expanded by remember { mutableStateOf(false) }
                                        val options = listOf(4, 6, 8, 10, 12, 20)
                                        Text(
                                            "d${entry.hitDie}",
                                            modifier = Modifier
                                                .clickable { expanded = true }
                                                .padding(8.dp),
                                            color = colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp * 1.15f
                                        )
                                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                            options.forEach { option ->
                                                DropdownMenuItem(
                                                    text = { Text("d$option") },
                                                    onClick = {
                                                        val newList = hpLevelData.toMutableList()
                                                        if (!isMulticlass) {
                                                            for (i in newList.indices) newList[i] = newList[i].copy(hitDie = option)
                                                            onHitDieChange(option)
                                                        } else {
                                                            newList[index] = newList[index].copy(hitDie = option)
                                                        }
                                                        onHPLevelDataChange(newList)
                                                        expanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    Box(modifier = Modifier.weight(0.35f), contentAlignment = Alignment.Center) {
                                        if (entry.rollResult == null) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().height(42.dp),
                                                horizontalArrangement = Arrangement.spacedBy(1.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        val roll = (1..entry.hitDie).random()
                                                        val newList = hpLevelData.toMutableList()
                                                        newList[index] = entry.copy(rollResult = roll)
                                                        onHPLevelDataChange(newList)
                                                    },
                                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                                    contentPadding = PaddingValues(horizontal = 4.dp),
                                                    shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 0.dp, bottomEnd = 0.dp)
                                                ) {
                                                    val diceIcon = when (entry.hitDie) {
                                                        2 -> Res.drawable.ic_d2_dice
                                                        4 -> Res.drawable.ic_d4_dice
                                                        6 -> Res.drawable.ic_d6_dice
                                                        8 -> Res.drawable.ic_d8_dice
                                                        10 -> Res.drawable.ic_d10_dice
                                                        12 -> Res.drawable.ic_d12_dice
                                                        20 -> Res.drawable.ic_d20_dice
                                                        else -> Res.drawable.ic_d20_dice
                                                    }
                                                    Icon(
                                                        painter = painterResource(diceIcon), 
                                                        contentDescription = null, 
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("Бросок", fontSize = 12.sp * 1.15f)
                                                }
                                                
                                                Button(
                                                    onClick = {
                                                        val newList = hpLevelData.toMutableList()
                                                        newList[index] = entry.copy(rollResult = 0)
                                                        onHPLevelDataChange(newList)
                                                    },
                                                    modifier = Modifier.width(42.dp).fillMaxHeight(),
                                                    contentPadding = PaddingValues(0.dp),
                                                    shape = RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 8.dp, bottomEnd = 8.dp)
                                                ) {
                                                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        } else {
                                            OutlinedTextField(
                                                value = if (entry.rollResult == 0) "" else entry.rollResult.toString(),
                                                onValueChange = { s ->
                                                    val v = s.filter { it.isDigit() }.toIntOrNull() ?: 0
                                                    val newList = hpLevelData.toMutableList()
                                                    newList[index] = entry.copy(rollResult = v)
                                                    onHPLevelDataChange(newList)
                                                },
                                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                                textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontSize = 16.sp * 1.15f),
                                                singleLine = true,
                                                shape = RoundedCornerShape(8.dp),
                                                placeholder = { Text("0", modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
                                                trailingIcon = {
                                                    IconButton(onClick = {
                                                        val newList = hpLevelData.toMutableList()
                                                        newList[index] = entry.copy(rollResult = null)
                                                        onHPLevelDataChange(newList)
                                                    }) {
                                                        Icon(
                                                            imageVector = Icons.Default.Refresh, 
                                                            contentDescription = "Сбросить",
                                                            modifier = Modifier.size(18.dp),
                                                            tint = colorScheme.primary.copy(alpha = 0.7f)
                                                        )
                                                    }
                                                }
                                            )
                                        }
                                    }

                                    val totalLevelHp = (entry.rollResult ?: 0) + conMod + levelBonus
                                    Text(
                                        "$totalLevelHp",
                                        modifier = Modifier.weight(0.25f),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                        fontWeight = FontWeight.Black,
                                        color = colorScheme.primary,
                                        fontSize = 18.sp * 1.15f
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val hdGroups = currentData.groupBy { it.hitDie }.map { "d${it.key}: ${it.value.size}" }.joinToString(", ")
                        val totalRolls = currentData.sumOf { it.rollResult ?: 0 }
                        val totalFixedBonus = hpBonusesTotal.filter { it.isActive }.sumOf { evaluateFormula(it.formula, statsMap) }
                        val totalMaxHP = totalRolls + (conMod * level) + (levelBonus * level) + totalFixedBonus

                        Spacer(modifier = Modifier.weight(0.15f))
                        Text(
                            hdGroups,
                            modifier = Modifier.weight(0.25f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp * 1.15f,
                            color = colorScheme.onSurfaceVariant
                        )
                        Text(
                            "$totalRolls",
                            modifier = Modifier.weight(0.35f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp * 1.15f,
                            color = colorScheme.onSurface
                        )
                        Text(
                            "$totalMaxHP",
                            modifier = Modifier.weight(0.25f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp * 1.15f,
                            color = colorScheme.primary
                        )
                    }

                    SectionHeader("Бонусы к хитам за уровень")
                    hpBonusesAtLevel.forEachIndexed { idx, bonus ->
                        AttackBonusField(
                            bonus = bonus,
                            showAdvantageLogic = false,
                            onUpdate = { updated ->
                                val newList = hpBonusesAtLevel.toMutableList()
                                newList[idx] = updated
                                onHpBonusesAtLevelChange(newList)
                            },
                            onDelete = {
                                val newList = hpBonusesAtLevel.toMutableList()
                                newList.removeAt(idx)
                                onHpBonusesAtLevelChange(newList)
                            }
                        )
                    }
                    AddBonusButton {
                        onHpBonusesAtLevelChange(hpBonusesAtLevel + AttackBonus())
                    }

                    SectionHeader("Бонусы к общему числу хитов")
                    hpBonusesTotal.forEachIndexed { idx, bonus ->
                        AttackBonusField(
                            bonus = bonus,
                            showAdvantageLogic = false,
                            onUpdate = { updated ->
                                val newList = hpBonusesTotal.toMutableList()
                                newList[idx] = updated
                                onHpBonusesTotalChange(newList)
                            },
                            onDelete = {
                                val newList = hpBonusesTotal.toMutableList()
                                newList.removeAt(idx)
                                onHpBonusesTotalChange(newList)
                            }
                        )
                    }
                    AddBonusButton {
                        onHpBonusesTotalChange(hpBonusesTotal + AttackBonus())
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
}
