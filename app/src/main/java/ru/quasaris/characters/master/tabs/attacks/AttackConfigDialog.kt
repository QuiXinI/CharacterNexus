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
import ru.quasaris.characters.master.AdvantagePreference
import ru.quasaris.characters.master.R
import ru.quasaris.characters.master.AttackBonus
import ru.quasaris.characters.master.AttackEntry
import ru.quasaris.characters.master.Attribute
import ru.quasaris.characters.master.DamageBonus
import ru.quasaris.characters.master.MagicAttackType
import ru.quasaris.characters.master.backend.SettingsViewModel
import ru.quasaris.characters.master.ui.DeleteConfirmationDialog
import ru.quasaris.characters.master.tabs.attacks.AttackBonusIndicator
import ru.quasaris.characters.master.tabs.attacks.DiceIcon
import ru.quasaris.characters.master.tabs.attacks.SectionHeader
import ru.quasaris.characters.master.tabs.attacks.ProficiencyToggle
import ru.quasaris.characters.master.tabs.attacks.AttributeDropdown
import ru.quasaris.characters.master.tabs.attacks.AddBonusButton
import ru.quasaris.characters.master.tabs.attacks.AttackBonusField
import ru.quasaris.characters.master.tabs.attacks.DamageBonusField

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
    forceBlurEnabled: Boolean = false,
    exhaustion: Int = 0,
    settingsViewModel: SettingsViewModel? = null,
    stats: Map<String, String> = emptyMap(),
    spellSettings: ru.quasaris.characters.master.SpellSettings = ru.quasaris.characters.master.SpellSettings()
) {
    var state by remember { mutableStateOf(attack) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val attackCalculation = remember(state, proficiencyBonus, attributeModifiers, exhaustion, stats, spellSettings) {
        if (state.isMagic) {
            val bonuses = if (state.magicType == MagicAttackType.ATTACK) spellSettings.spellAttackBonuses else spellSettings.spellSaveDcBonuses
            val pbVal = proficiencyBonus
            
            // Re-calculate pb + mod for magic
            val abilityModifier = if (spellSettings.spellcastingAbility != Attribute.NONE) {
                val statKey = when (spellSettings.spellcastingAbility) {
                    Attribute.STRENGTH -> "strength"
                    Attribute.DEXTERITY -> "dexterity"
                    Attribute.CONSTITUTION -> "constitution"
                    Attribute.INTELLIGENCE -> "intelligence"
                    Attribute.WISDOM -> "wisdom"
                    Attribute.CHARISMA -> "charisma"
                    else -> ""
                }
                ru.quasaris.characters.master.backend.calculateModifier(stats[statKey] ?: "10")
            } else 0
            
            var totalFlat = (if (state.magicType == MagicAttackType.SAVE) 8 else 0) + pbVal + abilityModifier - (if (state.magicType == MagicAttackType.ATTACK) exhaustion * 2 else 0)
            val allDice = mutableMapOf<Int, Int>()
            
            bonuses.forEach { bonus ->
                val (fFlat, fDice) = parseFormulaParts(bonus.formula, stats)
                totalFlat += fFlat
                fDice.forEach { allDice[it.sides] = (allDice[it.sides] ?: 0) + it.count }
            }
            return@remember Pair(totalFlat, allDice.map { DicePart(it.value, it.key) }.sortedBy { it.sides })
        }

        if (state.attribute == Attribute.NONE) {
            return@remember Pair(0, emptyList<DicePart>())
        }
        val attrMod = attributeModifiers[state.attribute] ?: 0
        val prof = if (state.isProficient) proficiencyBonus else 0
        
        // Sum up base bonus + all flat bonuses from additional bonus fields
        var totalFlat = attrMod + prof + state.attackBonus - (exhaustion * 2)
        val allDice = mutableMapOf<Int, Int>()
        
        state.attackBonuses.forEach { bonus ->
            val (fFlat, fDice) = parseFormulaParts(bonus.formula, stats)
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
                        style = HazeStyle(blurRadius = 24.dp, tints = listOf(HazeTint(colorScheme.surface.copy(alpha = 0.1f))))
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
                        AttackBonusIndicator(
                            bonus = totalAttackBonus, 
                            dice = attackDice,
                            showLabel = !state.isMagic || state.magicType == MagicAttackType.ATTACK
                        )
                    }

                    // MAGIC Toggle
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = colorScheme.primary.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Магическая атака", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                            Switch(checked = state.isMagic, onCheckedChange = { state = state.copy(isMagic = it) })
                        }
                    }

                    if (state.isMagic) {
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = state.magicType == MagicAttackType.ATTACK,
                                onClick = { state = state.copy(magicType = MagicAttackType.ATTACK) },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                            ) {
                                Text("Бросок атаки")
                            }
                            SegmentedButton(
                                selected = state.magicType == MagicAttackType.SAVE,
                                onClick = { state = state.copy(magicType = MagicAttackType.SAVE) },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                            ) {
                                Text("Спасбросок")
                            }
                        }
                    }

                    // АТАКА Section
                    if (!state.isMagic) {
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
                            state = state.copy(attackBonuses = state.attackBonuses + AttackBonus(advantagePreference = AdvantagePreference.IGNORE_BOTH))
                        }
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
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        border = BorderStroke(1.dp, Color.Red),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Удалить")
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }

                DeleteConfirmationDialog(
                    showDialog = showDeleteConfirm,
                    onDismiss = { showDeleteConfirm = false },
                    onConfirm = {
                        onDelete(state)
                        showDeleteConfirm = false
                    },
                    title = "Удалить атаку?",
                    settingsViewModel = settingsViewModel
                )

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
