package ru.quasaris.characters.master.tabs.attacks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import ru.quasaris.characters.master.backend.DicePart
import ru.quasaris.characters.master.backend.parseFormulaParts
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import ru.quasaris.characters.master.R
import ru.quasaris.characters.master.Attribute
import ru.quasaris.characters.master.AttackBonus
import ru.quasaris.characters.master.DamageBonus
import ru.quasaris.characters.master.BonusOperation
import ru.quasaris.characters.master.AdvantagePreference

@Composable
fun AttackBonusIndicator(
    bonus: Int,
    dice: List<DicePart>,
    size: androidx.compose.ui.unit.Dp = 60.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 20.sp,
    showLabel: Boolean = true,
    showDice: Boolean = true,
    diceOnLeft: Boolean = false,
    showPlus: Boolean = true,
    diceSize: androidx.compose.ui.unit.Dp = 24.dp
) {
    val bonusText = if (bonus >= 0) (if (showPlus) "+$bonus" else bonus.toString()) else bonus.toString()
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (showLabel) {
            Text("Атака", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (diceOnLeft && showDice && dice.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    dice.forEach { die -> DiceIcon(die, size = diceSize) }
                }
            }

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

            if (!diceOnLeft && showDice && dice.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    dice.forEach { die -> DiceIcon(die, size = diceSize) }
                }
            }
        }
    }
}

@Composable
fun DiceIcon(die: DicePart, size: androidx.compose.ui.unit.Dp = 24.dp) {
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
            modifier = Modifier.size(size),
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
            
            val density = androidx.compose.ui.platform.LocalDensity.current
            val textFontSize = with(density) { (size.toPx() * 0.58f).toSp() }

            Box(modifier = Modifier.padding(top = 1.dp)) {
                listOf(-0.5f to -0.5f, 0.5f to -0.5f, -0.5f to 0.5f, 0.5f to 0.5f).forEach { (dx, dy) ->
                    Text(
                        text = die.count.toString(),
                        fontSize = textFontSize,
                        fontWeight = FontWeight.Black,
                        color = outlineColor,
                        modifier = Modifier.offset(dx.dp, dy.dp)
                    )
                }
                Text(
                    text = die.count.toString(),
                    fontSize = textFontSize,
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
fun AdvantagePreferenceLabel(preference: AdvantagePreference) {
    val colorScheme = MaterialTheme.colorScheme
    when (preference) {
        AdvantagePreference.NONE -> Text("Авто", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary)
        AdvantagePreference.IGNORE_ADVANTAGE -> Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Default.KeyboardArrowUp, null, modifier = Modifier.size(20.dp).alpha(0.4f))
            Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp), tint = Color.Red)
        }
        AdvantagePreference.ALWAYS_ADVANTAGE -> Icon(Icons.Default.KeyboardArrowUp, null, modifier = Modifier.size(24.dp))
        AdvantagePreference.IGNORE_DISADVANTAGE -> Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Default.KeyboardArrowDown, null, modifier = Modifier.size(20.dp).alpha(0.4f))
            Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp), tint = Color.Red)
        }
        AdvantagePreference.ALWAYS_DISADVANTAGE -> Icon(Icons.Default.KeyboardArrowDown, null, modifier = Modifier.size(24.dp))
        AdvantagePreference.IGNORE_BOTH -> Box(contentAlignment = Alignment.Center) {
            Row(horizontalArrangement = Arrangement.spacedBy((-4).dp)) {
                Icon(Icons.Default.KeyboardArrowUp, null, modifier = Modifier.size(14.dp).alpha(0.4f))
                Icon(Icons.Default.KeyboardArrowDown, null, modifier = Modifier.size(14.dp).alpha(0.4f))
            }
            Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp), tint = Color.Red)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttackBonusField(
    bonus: AttackBonus,
    showAdvantageLogic: Boolean = true,
    onUpdate: (AttackBonus) -> Unit,
    onDelete: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (bonus.isActive) 1f else 0.5f),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = bonus.isActive,
                onCheckedChange = { onUpdate(bonus.copy(isActive = it)) },
                modifier = Modifier.scale(0.8f)
            )
            Spacer(Modifier.width(8.dp))
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
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
             SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(0.4f)) {
                 SegmentedButton(
                     selected = bonus.operation == BonusOperation.ADD,
                     onClick = { onUpdate(bonus.copy(operation = BonusOperation.ADD)) },
                     shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                 ) { Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                 SegmentedButton(
                     selected = bonus.operation == BonusOperation.SUBTRACT,
                     onClick = { onUpdate(bonus.copy(operation = BonusOperation.SUBTRACT)) },
                     shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                 ) { Text("-", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                 SegmentedButton(
                     selected = bonus.operation == BonusOperation.OVERRIDE,
                     onClick = { onUpdate(bonus.copy(operation = BonusOperation.OVERRIDE)) },
                     shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                 ) { Text("=", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
             }
             
             OutlinedTextField(
                 value = bonus.formula,
                 onValueChange = { onUpdate(bonus.copy(formula = it)) },
                 label = { Text("Формула") },
                 modifier = Modifier.weight(0.6f),
                 shape = RoundedCornerShape(8.dp)
             )
        }
        
        if (showAdvantageLogic) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Логика преимущества/помехи", fontSize = 12.sp, color = colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = bonus.advantagePreference == AdvantagePreference.NONE,
                        onClick = { onUpdate(bonus.copy(advantagePreference = AdvantagePreference.NONE)) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 6)
                    ) { AdvantagePreferenceLabel(AdvantagePreference.NONE) }
                    SegmentedButton(
                        selected = bonus.advantagePreference == AdvantagePreference.IGNORE_ADVANTAGE,
                        onClick = { onUpdate(bonus.copy(advantagePreference = AdvantagePreference.IGNORE_ADVANTAGE)) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 6)
                    ) { AdvantagePreferenceLabel(AdvantagePreference.IGNORE_ADVANTAGE) }
                    SegmentedButton(
                        selected = bonus.advantagePreference == AdvantagePreference.ALWAYS_ADVANTAGE,
                        onClick = { onUpdate(bonus.copy(advantagePreference = AdvantagePreference.ALWAYS_ADVANTAGE)) },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 6)
                    ) { AdvantagePreferenceLabel(AdvantagePreference.ALWAYS_ADVANTAGE) }
                    SegmentedButton(
                        selected = bonus.advantagePreference == AdvantagePreference.IGNORE_DISADVANTAGE,
                        onClick = { onUpdate(bonus.copy(advantagePreference = AdvantagePreference.IGNORE_DISADVANTAGE)) },
                        shape = SegmentedButtonDefaults.itemShape(index = 3, count = 6)
                    ) { AdvantagePreferenceLabel(AdvantagePreference.IGNORE_DISADVANTAGE) }
                    SegmentedButton(
                        selected = bonus.advantagePreference == AdvantagePreference.ALWAYS_DISADVANTAGE,
                        onClick = { onUpdate(bonus.copy(advantagePreference = AdvantagePreference.ALWAYS_DISADVANTAGE)) },
                        shape = SegmentedButtonDefaults.itemShape(index = 4, count = 6)
                    ) { AdvantagePreferenceLabel(AdvantagePreference.ALWAYS_DISADVANTAGE) }
                    SegmentedButton(
                        selected = bonus.advantagePreference == AdvantagePreference.IGNORE_BOTH,
                        onClick = { onUpdate(bonus.copy(advantagePreference = AdvantagePreference.IGNORE_BOTH)) },
                        shape = SegmentedButtonDefaults.itemShape(index = 5, count = 6)
                    ) { AdvantagePreferenceLabel(AdvantagePreference.IGNORE_BOTH) }
                }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp, color = colorScheme.outlineVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DamageBonusField(
    bonus: DamageBonus,
    onUpdate: (DamageBonus) -> Unit,
    onDelete: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (bonus.isActive) 1f else 0.5f),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = bonus.isActive,
                onCheckedChange = { onUpdate(bonus.copy(isActive = it)) },
                modifier = Modifier.scale(0.8f)
            )
            Spacer(Modifier.width(8.dp))
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
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
             SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(0.4f)) {
                 SegmentedButton(
                     selected = bonus.operation == BonusOperation.ADD,
                     onClick = { onUpdate(bonus.copy(operation = BonusOperation.ADD)) },
                     shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                 ) { Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                 SegmentedButton(
                     selected = bonus.operation == BonusOperation.SUBTRACT,
                     onClick = { onUpdate(bonus.copy(operation = BonusOperation.SUBTRACT)) },
                     shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                 ) { Text("-", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                 SegmentedButton(
                     selected = bonus.operation == BonusOperation.OVERRIDE,
                     onClick = { onUpdate(bonus.copy(operation = BonusOperation.OVERRIDE)) },
                     shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                 ) { Text("=", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
             }
             
             OutlinedTextField(
                 value = bonus.formula,
                 onValueChange = { onUpdate(bonus.copy(formula = it)) },
                 label = { Text("Формула") },
                 modifier = Modifier.weight(0.6f),
                 shape = RoundedCornerShape(8.dp)
             )
        }
        
        OutlinedTextField(
            value = bonus.damageType,
            onValueChange = { onUpdate(bonus.copy(damageType = it)) },
            label = { Text("Вид Урона") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        )
        
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Логика преимущества/помехи", fontSize = 12.sp, color = colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = bonus.advantagePreference == AdvantagePreference.NONE,
                    onClick = { onUpdate(bonus.copy(advantagePreference = AdvantagePreference.NONE)) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 6)
                ) { AdvantagePreferenceLabel(AdvantagePreference.NONE) }
                SegmentedButton(
                    selected = bonus.advantagePreference == AdvantagePreference.IGNORE_ADVANTAGE,
                    onClick = { onUpdate(bonus.copy(advantagePreference = AdvantagePreference.IGNORE_ADVANTAGE)) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 6)
                ) { AdvantagePreferenceLabel(AdvantagePreference.IGNORE_ADVANTAGE) }
                SegmentedButton(
                    selected = bonus.advantagePreference == AdvantagePreference.ALWAYS_ADVANTAGE,
                    onClick = { onUpdate(bonus.copy(advantagePreference = AdvantagePreference.ALWAYS_ADVANTAGE)) },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 6)
                ) { AdvantagePreferenceLabel(AdvantagePreference.ALWAYS_ADVANTAGE) }
                SegmentedButton(
                    selected = bonus.advantagePreference == AdvantagePreference.IGNORE_DISADVANTAGE,
                    onClick = { onUpdate(bonus.copy(advantagePreference = AdvantagePreference.IGNORE_DISADVANTAGE)) },
                    shape = SegmentedButtonDefaults.itemShape(index = 3, count = 6)
                ) { AdvantagePreferenceLabel(AdvantagePreference.IGNORE_DISADVANTAGE) }
                SegmentedButton(
                    selected = bonus.advantagePreference == AdvantagePreference.ALWAYS_DISADVANTAGE,
                    onClick = { onUpdate(bonus.copy(advantagePreference = AdvantagePreference.ALWAYS_DISADVANTAGE)) },
                    shape = SegmentedButtonDefaults.itemShape(index = 4, count = 6)
                ) { AdvantagePreferenceLabel(AdvantagePreference.ALWAYS_DISADVANTAGE) }
                SegmentedButton(
                    selected = bonus.advantagePreference == AdvantagePreference.IGNORE_BOTH,
                    onClick = { onUpdate(bonus.copy(advantagePreference = AdvantagePreference.IGNORE_BOTH)) },
                    shape = SegmentedButtonDefaults.itemShape(index = 5, count = 6)
                ) { AdvantagePreferenceLabel(AdvantagePreference.IGNORE_BOTH) }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp, color = colorScheme.outlineVariant)
    }
}
