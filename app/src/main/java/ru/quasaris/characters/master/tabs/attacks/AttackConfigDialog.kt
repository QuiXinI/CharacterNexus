package ru.quasaris.characters.master.tabs.attacks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.HazeInputScale
import ru.quasaris.characters.master.R
import ru.quasaris.characters.master.AttackBonus
import ru.quasaris.characters.master.AttackEntry
import ru.quasaris.characters.master.Attribute
import ru.quasaris.characters.master.DamageBonus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttackConfigDialog(
    attack: AttackEntry,
    proficiencyBonus: Int,
    attributeModifiers: Map<Attribute, Int>,
    onDismiss: () -> Unit,
    onSave: (AttackEntry) -> Unit,
    onDelete: (AttackEntry) -> Unit,
    hazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false
) {
    var state by remember { mutableStateOf(attack) }

    val attackCalculation = remember(state, proficiencyBonus, attributeModifiers) {
        if (state.attribute == Attribute.NONE) {
            return@remember Pair(0, emptyList<DicePart>())
        }
        val attrMod = attributeModifiers[state.attribute] ?: 0
        val prof = if (state.isProficient) proficiencyBonus else 0
        
        // Sum up base bonus + all flat bonuses from additional bonus fields
        var totalFlat = attrMod + prof + state.attackBonus
        val allDice = mutableMapOf<Int, Int>()
        
        state.attackBonuses.forEach { bonus ->
            val (fFlat, fDice) = parseFormulaParts(bonus.formula, attributeModifiers, proficiencyBonus)
            totalFlat += fFlat
            fDice.forEach { allDice[it.sides] = (allDice[it.sides] ?: 0) + it.count }
        }
        
        Pair(totalFlat, allDice.map { DicePart(it.value, it.key) }.sortedBy { it.sides })
    }
    
    val totalAttackBonus = attackCalculation.first
    val attackDice = attackCalculation.second

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val colorScheme = MaterialTheme.colorScheme
        val isOled = colorScheme.background == Color.Black

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Настройки атаки", fontWeight = FontWeight.Black) },
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
                        style = HazeStyle(blurRadius = 24.dp, tints = listOf(HazeTint(Color.Black.copy(alpha = 0.2f))))
                        inputScale = HazeInputScale.Fixed(0.7f)
                    }
                } else this
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header with Bonus Indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = state.name,
                            onValueChange = { state = state.copy(name = it) },
                            label = { Text("Название") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        AttackBonusIndicator(totalAttackBonus, attackDice)
                    }

                    // АТАКА Section
                    SectionHeader("Атака")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProficiencyToggle(
                            isProficient = state.isProficient,
                            proficiencyBonus = proficiencyBonus,
                            onToggle = { state = state.copy(isProficient = it) },
                            modifier = Modifier.weight(1f)
                        )
                        AttributeDropdown(
                            selectedAttribute = state.attribute,
                            onAttributeSelected = { state = state.copy(attribute = it) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    state.attackBonuses.forEachIndexed { index, bonus ->
                        CompositionLocalProvider(
                            LocalContentColor provides if (state.attribute != Attribute.NONE) LocalContentColor.current else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        ) {
                            AttackBonusField(
                                bonus = bonus,
                                onUpdate = { updated ->
                                    val newList = state.attackBonuses.toMutableList()
                                    newList[index] = updated
                                    state = state.copy(attackBonuses = newList)
                                },
                                onDelete = {
                                    val newList = state.attackBonuses.toMutableList()
                                    newList.removeAt(index)
                                    state = state.copy(attackBonuses = newList)
                                }
                            )
                        }
                    }

                    AddBonusButton(enabled = state.attribute != Attribute.NONE) {
                        state = state.copy(attackBonuses = state.attackBonuses + AttackBonus())
                    }

                    // УРОН Section
                    SectionHeader("Урон")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = state.damageFormula,
                            onValueChange = { state = state.copy(damageFormula = it) },
                            label = { Text("Формула") },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("1d8+[STR]") },
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = state.damageType,
                            onValueChange = { state = state.copy(damageType = it) },
                            label = { Text("Вид Урона") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    state.damageBonuses.forEachIndexed { index, bonus ->
                        DamageBonusField(
                            bonus = bonus,
                            onUpdate = { updated ->
                                val newList = state.damageBonuses.toMutableList()
                                newList[index] = updated
                                state = state.copy(damageBonuses = newList)
                            },
                            onDelete = {
                                val newList = state.damageBonuses.toMutableList()
                                newList.removeAt(index)
                                state = state.copy(damageBonuses = newList)
                            }
                        )
                    }

                    AddBonusButton {
                        state = state.copy(damageBonuses = state.damageBonuses + DamageBonus())
                    }

                    // Notes Section
                    SectionHeader("Заметки")
                    OutlinedTextField(
                        value = state.notes,
                        onValueChange = { state = state.copy(notes = it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp),
                        placeholder = { Text("Описание атаки...") },
                        shape = RoundedCornerShape(8.dp)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { state = state.copy(showNotes = !state.showNotes) }
                    ) {
                        Checkbox(
                            checked = state.showNotes,
                            onCheckedChange = { state = state.copy(showNotes = it) }
                        )
                        Text("Отображать Заметки")
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Delete Button
                    OutlinedButton(
                        onClick = { onDelete(state) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        border = BorderStroke(1.dp, Color.Red),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Удалить")
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }

                // Save FAB or Button (Optional, usually dialogs have Save/Cancel, but let's add a Save button)
                Button(
                    onClick = { onSave(state) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Сохранить", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AttackBonusIndicator(
    bonus: Int,
    dice: List<DicePart>,
    size: androidx.compose.ui.unit.Dp = 60.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 20.sp,
    showLabel: Boolean = true,
    showDice: Boolean = true
) {
    val bonusText = if (bonus >= 0) "+$bonus" else bonus.toString()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(size)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = bonusText,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        if (showLabel) {
            Text("Атака", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }

        if (showDice && dice.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                dice.forEach { die ->
                    DiceIcon(die)
                }
            }
        }
    }
}

@Composable
fun DiceIcon(die: DicePart) {
    val iconRes = when (die.sides) {
        4 -> R.drawable.ic_d4_dice
        6 -> R.drawable.ic_d6_dice
        8 -> R.drawable.ic_d8_dice
        10 -> R.drawable.ic_d10_dice
        12 -> R.drawable.ic_d12_dice
        20 -> R.drawable.ic_d20_dice
        else -> null
    }

    if (iconRes != null) {
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = "d${die.sides}",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxSize()
            )
            
            val textColor = MaterialTheme.colorScheme.primary
            val outlineColor = MaterialTheme.colorScheme.background
            
            Box(modifier = Modifier.padding(top = 1.dp)) {
                listOf(-0.5f to -0.5f, 0.5f to -0.5f, -0.5f to 0.5f, 0.5f to 0.5f).forEach { (dx, dy) ->
                    Text(
                        text = die.count.toString(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = outlineColor,
                        modifier = Modifier.offset(dx.dp, dy.dp)
                    )
                }
                Text(
                    text = die.count.toString(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = textColor
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 24.sp,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
fun ProficiencyToggle(
    isProficient: Boolean,
    proficiencyBonus: Int,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            "Мастерство",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    RoundedCornerShape(8.dp)
                )
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val activeColor = MaterialTheme.colorScheme.primaryContainer
            val inactiveColor = Color.Transparent

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (!isProficient) activeColor else inactiveColor, RoundedCornerShape(6.dp))
                    .clickable { onToggle(false) },
                contentAlignment = Alignment.Center
            ) {
                Text("Нет", fontSize = 16.sp)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (isProficient) activeColor else inactiveColor, RoundedCornerShape(6.dp))
                    .clickable { onToggle(true) },
                contentAlignment = Alignment.Center
            ) {
                Text("+$proficiencyBonus", fontSize = 16.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttributeDropdown(
    selectedAttribute: Attribute,
    onAttributeSelected: (Attribute) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            "Характеристика",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = selectedAttribute.fullName,
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(8.dp)
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.45f)
            ) {
                Attribute.entries.forEach { attr ->
                    DropdownMenuItem(
                        text = { Text(attr.fullName) },
                        onClick = {
                            onAttributeSelected(attr)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AddBonusButton(
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val contentColor = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 8.dp)
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = "Добавить бонус",
            color = contentColor,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
fun AttackBonusField(
    bonus: AttackBonus,
    onUpdate: (AttackBonus) -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = bonus.name,
                onValueChange = { onUpdate(bonus.copy(name = it)) },
                label = { Text("Название") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Close, contentDescription = "Удалить", tint = Color.Red)
            }
        }
        OutlinedTextField(
            value = bonus.formula,
            onValueChange = { onUpdate(bonus.copy(formula = it)) },
            label = { Text("Формула") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@Composable
fun DamageBonusField(
    bonus: DamageBonus,
    onUpdate: (DamageBonus) -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = bonus.name,
                onValueChange = { onUpdate(bonus.copy(name = it)) },
                label = { Text("Название") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Close, contentDescription = "Удалить", tint = Color.Red)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = bonus.formula,
                onValueChange = { onUpdate(bonus.copy(formula = it)) },
                label = { Text("Формула") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            )
            OutlinedTextField(
                value = bonus.damageType,
                onValueChange = { onUpdate(bonus.copy(damageType = it)) },
                label = { Text("Вид Урона") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}
