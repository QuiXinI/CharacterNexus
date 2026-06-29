package ru.quasaris.characters.master.attacks

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
import ru.quasaris.characters.master.AttackEntry
import ru.quasaris.characters.master.Attribute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttackConfigDialog(
    attack: AttackEntry,
    proficiencyBonus: Int,
    attributeModifiers: Map<Attribute, Int>,
    onDismiss: () -> Unit,
    onSave: (AttackEntry) -> Unit,
    onDelete: (AttackEntry) -> Unit
) {
    var state by remember { mutableStateOf(attack) }

    val totalAttackBonus = remember(state, proficiencyBonus, attributeModifiers) {
        val attrMod = attributeModifiers[state.attribute] ?: 0
        val prof = if (state.isProficient) proficiencyBonus else 0
        attrMod + prof + state.attackBonus
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Настройки атаки", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
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
                            label = { Text("НАЗВАНИЕ") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        AttackBonusIndicator(totalAttackBonus)
                    }

                    // АТАКА Section
                    SectionHeader("АТАКА")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                    AddBonusButton(
                        currentBonus = state.attackBonus,
                        onBonusChange = { state = state.copy(attackBonus = it) }
                    )

                    // УРОН Section
                    SectionHeader("УРОН")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = state.damageFormula,
                            onValueChange = { state = state.copy(damageFormula = it) },
                            label = { Text("ФОРМУЛА") },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("1d8+[STR]") },
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = state.damageType,
                            onValueChange = { state = state.copy(damageType = it) },
                            label = { Text("ВИД УРОНА") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                    AddBonusButton(
                        currentBonus = state.damageBonus,
                        onBonusChange = { state = state.copy(damageBonus = it) }
                    )

                    // ЗАМЕТКИ Section
                    SectionHeader("ЗАМЕТКИ")
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
                        Text("Отображать заметки")
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Delete Button
                    OutlinedButton(
                        onClick = { onDelete(state) },
                        modifier = Modifier.align(Alignment.End),
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
                        .padding(16.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Сохранить")
                }
            }
        }
    }
}

@Composable
fun AttackBonusIndicator(bonus: Int) {
    val bonusText = if (bonus >= 0) "+$bonus" else bonus.toString()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = bonusText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text("АТАКА", fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
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
    Row(
        modifier = modifier
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
            Text("Нет", fontSize = 12.sp)
        }
        Box(
            modifier = Modifier
                .weight(1.5f)
                .fillMaxHeight()
                .background(if (isProficient) activeColor else inactiveColor, RoundedCornerShape(6.dp))
                .clickable { onToggle(true) },
            contentAlignment = Alignment.Center
        ) {
            Text("Мастерство +$proficiencyBonus", fontSize = 12.sp, textAlign = TextAlign.Center)
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

    Box(modifier = modifier) {
        OutlinedTextField(
            value = selectedAttribute.fullName,
            onValueChange = {},
            readOnly = true,
            label = { Text("ХАРАКТЕРИСТИКА") },
            trailingIcon = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(Icons.Default.ArrowDropDown, null)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.45f)
        ) {
            Attribute.entries.filter { it != Attribute.NONE }.forEach { attr ->
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

@Composable
fun AddBonusButton(
    currentBonus: Int,
    onBonusChange: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { /* Could open a small number picker or just increment */ }
    ) {
        IconButton(onClick = { onBonusChange(currentBonus + 1) }) {
            Icon(Icons.Default.Add, contentDescription = "Добавить бонус", tint = MaterialTheme.colorScheme.primary)
        }
        Text(
            text = if (currentBonus == 0) "+ ДОБАВИТЬ БОНУС" else "Бонус: +$currentBonus",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}
