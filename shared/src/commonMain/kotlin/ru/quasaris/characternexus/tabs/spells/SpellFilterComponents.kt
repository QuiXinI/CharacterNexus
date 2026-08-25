package ru.quasaris.characternexus.tabs.spells

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterListOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import characternexus.shared.generated.resources.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.quasaris.characternexus.model.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpellFiltersArea(
    visible: Boolean,
    filterState: SpellFilterState,
    onFilterChange: (SpellFilterState) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .heightIn(max = 400.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Row 1: Quick Toggles (K, O, R, Damage, Attack/Save)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterIconButton("К", filterState.hasConcentration) { onFilterChange(filterState.copy(hasConcentration = it)) }
                        FilterIconButton("О", filterState.isCircle) { onFilterChange(filterState.copy(isCircle = it)) }
                        FilterIconButton("Р", filterState.isRitual) { onFilterChange(filterState.copy(isRitual = it)) }
                        
                        FilterIconToggleButton(
                            iconRes = Res.drawable.ic_fire,
                            contentDescription = "Урон",
                            value = filterState.hasDamage,
                            onValueChange = { onFilterChange(filterState.copy(hasDamage = it)) }
                        )

                        FilterIconToggleButton(
                            iconRes = Res.drawable.ic_sword,
                            contentDescription = "Атака",
                            value = filterState.hasAttack,
                            onValueChange = { onFilterChange(filterState.copy(hasAttack = it)) }
                        )

                        FilterIconToggleButton(
                            iconRes = Res.drawable.ic_shield,
                            contentDescription = "Спасбросок",
                            value = filterState.hasSave,
                            onValueChange = { onFilterChange(filterState.copy(hasSave = it)) }
                        )
                    }

                    IconButton(
                        onClick = { onFilterChange(SpellFilterState()) }
                    ) {
                        Icon(Icons.Default.FilterListOff, "Сбросить всё")
                    }
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    maxItemsInEachRow = Int.MAX_VALUE
                ) {
                    val itemModifier = Modifier.widthIn(min = 130.dp).weight(1f)

                    MultiChoiceDropdown(
                        label = "Уровни",
                        options = listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9"),
                        selectedOptions = filterState.levels,
                        onSelectionChange = { onFilterChange(filterState.copy(levels = it)) },
                        optionLabel = { if (it == "0") "Заговор" else it },
                        modifier = itemModifier
                    )

                    MultiChoiceDropdown(
                        label = "Классы",
                        options = CharacterClass.entries.toList(),
                        selectedOptions = filterState.classes,
                        onSelectionChange = { onFilterChange(filterState.copy(classes = it)) },
                        optionLabel = { it.displayName },
                        modifier = itemModifier
                    )

                    MultiChoiceDropdown(
                        label = "Школы",
                        options = SpellSchool.entries.toList(),
                        selectedOptions = filterState.schools,
                        onSelectionChange = { onFilterChange(filterState.copy(schools = it)) },
                        optionLabel = { it.displayName },
                        modifier = itemModifier
                    )

                    MultiChoiceDropdown(
                        label = "Версии",
                        options = SpellVersion.entries.toList(),
                        selectedOptions = filterState.versions,
                        onSelectionChange = { onFilterChange(filterState.copy(versions = it)) },
                        optionLabel = { it.displayName },
                        modifier = itemModifier
                    )

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
                        },
                        modifier = itemModifier
                    )

                    val isSaveActive = filterState.hasSave == true
                    Box(modifier = itemModifier.alpha(if (isSaveActive) 1f else 0.5f)) {
                        MultiChoiceDropdown(
                            label = "Характеристика спаса",
                            options = Attribute.entries.filter { it != Attribute.NONE },
                            selectedOptions = filterState.savingThrowAttributes,
                            onSelectionChange = { if (isSaveActive) onFilterChange(filterState.copy(savingThrowAttributes = it)) },
                            optionLabel = { it.fullName }
                        )
                        if (!isSaveActive) {
                            Box(modifier = Modifier.matchParentSize().clickable(enabled = false) {})
                        }
                    }

                    MultiChoiceDropdown(
                        label = "Вид урона",
                        options = DamageType.entries.toList(),
                        selectedOptions = filterState.damageTypes,
                        onSelectionChange = { onFilterChange(filterState.copy(damageTypes = it)) },
                        optionLabel = { it.displayName },
                        modifier = itemModifier
                    )

                    MultiChoiceDropdown(
                        label = "Время наложения",
                        options = CastingTimeType.entries.toList(),
                        selectedOptions = filterState.castingTimeTypes,
                        onSelectionChange = { onFilterChange(filterState.copy(castingTimeTypes = it)) },
                        optionLabel = { it.displayName },
                        modifier = itemModifier
                    )

                    Column(modifier = itemModifier) {
                        Text("Описание времени наложения", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        OutlinedTextField(
                            value = filterState.castingTimeQuery,
                            onValueChange = { onFilterChange(filterState.copy(castingTimeQuery = it)) },
                            label = null,
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.small,
                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent
                            )
                        )
                    }

                    MultiChoiceDropdown(
                        label = "Длительность",
                        options = DurationUnit.entries.toList(),
                        selectedOptions = filterState.durationUnits,
                        onSelectionChange = { onFilterChange(filterState.copy(durationUnits = it)) },
                        optionLabel = { it.displayName },
                        modifier = itemModifier
                    )

                    val durationRequiresValue = filterState.durationUnits.any { it.requiresValue }
                    Column(modifier = itemModifier.alpha(if (durationRequiresValue) 1f else 0.5f)) {
                        Text("Длит. (кол-во)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        OutlinedTextField(
                            value = filterState.durationQuery,
                            onValueChange = {
                                val onlyDigits = it.filter { c -> c.isDigit() }
                                onFilterChange(filterState.copy(durationQuery = onlyDigits))
                            },
                            label = null,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = durationRequiresValue,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = MaterialTheme.shapes.small,
                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FilterIconToggleButton(
    iconRes: DrawableResource,
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
                painter = painterResource(iconRes),
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
    optionLabel: (T) -> String,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
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
