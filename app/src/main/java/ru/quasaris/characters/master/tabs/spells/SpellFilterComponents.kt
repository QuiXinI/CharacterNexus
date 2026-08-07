package ru.quasaris.characters.master.tabs.spells

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterListOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.quasaris.characters.master.R
import ru.quasaris.characters.master.*

@Composable
fun SpellFiltersArea(
    visible: Boolean,
    filterState: SpellFilterState,
    onFilterChange: (SpellFilterState) -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Row 1: Quick Toggles (K, O, R, Damage, Attack/Save)
                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterIconButton("К", filterState.hasConcentration) { onFilterChange(filterState.copy(hasConcentration = it)) }
                        FilterIconButton("О", filterState.isCircle) { onFilterChange(filterState.copy(isCircle = it)) }
                        FilterIconButton("Р", filterState.isRitual) { onFilterChange(filterState.copy(isRitual = it)) }
                        
                        // Damage Toggle (ic_fire)
                        FilterIconToggleButton(
                            iconRes = R.drawable.ic_fire,
                            contentDescription = "Урон",
                            value = filterState.hasDamage,
                            onValueChange = { onFilterChange(filterState.copy(hasDamage = it)) }
                        )

                        // Attack/Save Toggle (Sword / Shield)
                        val colorScheme = MaterialTheme.colorScheme
                        Surface(
                            onClick = { 
                                val next = when(filterState.attackOrSave) {
                                    MagicAttackType.ATTACK -> MagicAttackType.SAVE
                                    MagicAttackType.SAVE -> null
                                    null -> MagicAttackType.ATTACK
                                }
                                onFilterChange(filterState.copy(attackOrSave = next))
                            },
                            shape = MaterialTheme.shapes.small,
                            color = when(filterState.attackOrSave) {
                                MagicAttackType.ATTACK -> colorScheme.primary
                                MagicAttackType.SAVE -> colorScheme.errorContainer
                                null -> colorScheme.surfaceVariant
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Image(
                                    painter = painterResource(id = if (filterState.attackOrSave == MagicAttackType.ATTACK) R.drawable.ic_sword else R.drawable.ic_shield),
                                    contentDescription = "Тип проверки",
                                    modifier = Modifier.size(24.dp),
                                    colorFilter = ColorFilter.tint(
                                        when(filterState.attackOrSave) {
                                            MagicAttackType.ATTACK -> colorScheme.onPrimary
                                            MagicAttackType.SAVE -> colorScheme.onErrorContainer
                                            null -> colorScheme.onSurfaceVariant
                                        }
                                    )
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = { onFilterChange(SpellFilterState()) },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(Icons.Default.FilterListOff, "Сбросить всё")
                    }
                }

                // Row 2: Levels and Classes
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        MultiChoiceDropdown(
                            label = "Уровни",
                            options = listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9"),
                            selectedOptions = filterState.levels,
                            onSelectionChange = { onFilterChange(filterState.copy(levels = it)) },
                            optionLabel = { if (it == "0") "Заговор" else it }
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        MultiChoiceDropdown(
                            label = "Классы",
                            options = CharacterClass.entries.toList(),
                            selectedOptions = filterState.classes,
                            onSelectionChange = { onFilterChange(filterState.copy(classes = it)) },
                            optionLabel = { it.displayName }
                        )
                    }
                }

                // Row 3: School and Version
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        MultiChoiceDropdown(
                            label = "Школы",
                            options = SpellSchool.entries.toList(),
                            selectedOptions = filterState.schools,
                            onSelectionChange = { onFilterChange(filterState.copy(schools = it)) },
                            optionLabel = { it.displayName }
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        MultiChoiceDropdown(
                            label = "Версии",
                            options = SpellVersion.entries.toList(),
                            selectedOptions = filterState.versions,
                            onSelectionChange = { onFilterChange(filterState.copy(versions = it)) },
                            optionLabel = { it.displayName }
                        )
                    }
                }

                // Row 4: Components
                MultiChoiceDropdown(
                    label = "Компоненты",
                    options = SpellComponentFilter.entries.toList(),
                    selectedOptions = filterState.components,
                    onSelectionChange = { onFilterChange(filterState.copy(components = it)) },
                    optionLabel = { 
                        when(it) {
                            SpellComponentFilter.VERBAL -> "Вербальный"
                            SpellComponentFilter.SOMATIC -> "Соматический"
                            SpellComponentFilter.MATERIAL -> "Материальный"
                            SpellComponentFilter.MATERIAL_COST -> "Материальный (с ценой)"
                            SpellComponentFilter.MATERIAL_CONSUMED -> "Материальный (расходуемый)"
                            SpellComponentFilter.NO_VERBAL -> "Без вербального"
                            SpellComponentFilter.NO_SOMATIC -> "Без соматического"
                            SpellComponentFilter.NO_MATERIAL -> "Без материального"
                            SpellComponentFilter.NO_MATERIAL_COST -> "Без мат. (с ценой)"
                            SpellComponentFilter.NO_MATERIAL_CONSUMED -> "Без мат. (расходуемого)"
                        }
                    }
                )

                // Row 5: Saving Throw Attributes (Inactive if not Save)
                val isSaveActive = filterState.attackOrSave == MagicAttackType.SAVE
                Box(modifier = Modifier.fillMaxWidth().alpha(if (isSaveActive) 1f else 0.5f)) {
                    MultiChoiceDropdown(
                        label = "Характеристика спасброска",
                        options = Attribute.entries.filter { it != Attribute.NONE },
                        selectedOptions = filterState.savingThrowAttributes,
                        onSelectionChange = { if (isSaveActive) onFilterChange(filterState.copy(savingThrowAttributes = it)) },
                        optionLabel = { it.fullName }
                    )
                    if (!isSaveActive) {
                        Box(modifier = Modifier.matchParentSize().alpha(0f)) // Capture clicks
                    }
                }

                // Row 6: Casting Time
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                    Box(modifier = Modifier.weight(1.5f)) {
                        MultiChoiceDropdown(
                            label = "Время наложения",
                            options = CastingTimeType.entries.toList(),
                            selectedOptions = filterState.castingTimeTypes,
                            onSelectionChange = { onFilterChange(filterState.copy(castingTimeTypes = it)) },
                            optionLabel = { it.displayName }
                        )
                    }
                    OutlinedTextField(
                        value = filterState.castingTimeQuery,
                        onValueChange = { onFilterChange(filterState.copy(castingTimeQuery = it)) },
                        label = { Text("Описание") },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.small,
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent
                        )
                    )
                }

                // Row 7: Duration
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                    val durationRequiresValue = filterState.durationUnits.any { it.requiresValue }
                    OutlinedTextField(
                        value = filterState.durationQuery,
                        onValueChange = {
                            val onlyDigits = it.filter { c -> c.isDigit() }
                            onFilterChange(filterState.copy(durationQuery = onlyDigits))
                        },
                        label = { Text("Кол-во") },
                        modifier = Modifier.width(90.dp).alpha(if (durationRequiresValue) 1f else 0.5f),
                        enabled = durationRequiresValue,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = MaterialTheme.shapes.small,
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent
                        )
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        MultiChoiceDropdown(
                            label = "Длительность",
                            options = DurationUnit.entries.toList(),
                            selectedOptions = filterState.durationUnits,
                            onSelectionChange = { onFilterChange(filterState.copy(durationUnits = it)) },
                            optionLabel = { it.displayName }
                        )
                    }
                }

                // Row 8: Damage Types
                MultiChoiceDropdown(
                    label = "Виды урона",
                    options = DamageType.entries.toList(),
                    selectedOptions = filterState.damageTypes,
                    onSelectionChange = { onFilterChange(filterState.copy(damageTypes = it)) },
                    optionLabel = { it.displayName }
                )
            }
        }
    }
}

@Composable
fun FilterIconToggleButton(
    iconRes: Int,
    contentDescription: String,
    value: Boolean?,
    onValueChange: (Boolean?) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        onClick = { 
            val next = when(value) {
                true -> false
                false -> null
                null -> true
            }
            onValueChange(next)
        },
        shape = MaterialTheme.shapes.small,
        color = when(value) {
            true -> colorScheme.primary
            false -> colorScheme.errorContainer
            null -> colorScheme.surfaceVariant
        },
        modifier = Modifier.size(48.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = contentDescription,
                modifier = Modifier.size(24.dp),
                colorFilter = ColorFilter.tint(
                    when(value) {
                        true -> colorScheme.onPrimary
                        false -> colorScheme.onErrorContainer
                        null -> colorScheme.onSurfaceVariant
                    }
                )
            )
        }
    }
}

@Composable
fun FilterIconButton(
    label: String,
    value: Boolean?,
    onValueChange: (Boolean?) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        onClick = { 
            val next = when(value) {
                true -> false
                false -> null
                null -> true
            }
            onValueChange(next)
        },
        shape = MaterialTheme.shapes.small,
        color = when(value) {
            true -> colorScheme.primary
            false -> colorScheme.errorContainer
            null -> colorScheme.surfaceVariant
        },
        modifier = Modifier.size(48.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontWeight = FontWeight.Bold,
                color = when(value) {
                    true -> colorScheme.onPrimary
                    false -> colorScheme.onErrorContainer
                    null -> colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> MultiChoiceDropdown(
    label: String,
    options: List<T>,
    selectedOptions: Set<T>,
    onSelectionChange: (Set<T>) -> Unit,
    optionLabel: (T) -> String
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = if (selectedOptions.isEmpty()) "Все" else selectedOptions.joinToString(", ") { optionLabel(it) },
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true).fillMaxWidth(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                shape = MaterialTheme.shapes.small,
                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent
                )
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    val isSelected = option in selectedOptions
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = null
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(optionLabel(option), fontSize = 14.sp)
                            }
                        },
                        onClick = {
                            val newSelection = if (isSelected) selectedOptions - option else selectedOptions + option
                            onSelectionChange(newSelection)
                        }
                    )
                }
            }
        }
    }
}
