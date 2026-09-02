package ru.quasaris.characternexus.tabs.attacks

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
import ru.quasaris.characternexus.ui.DialogDimStyle
import ru.quasaris.characternexus.ui.BackHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.chrisbanes.haze.*
import org.jetbrains.compose.resources.painterResource
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.backend.SettingsViewModel
import ru.quasaris.characternexus.backend.DicePart
import ru.quasaris.characternexus.backend.parseFormulaParts
import ru.quasaris.characternexus.backend.evaluateFormula
import ru.quasaris.characternexus.backend.preprocessFormula
import ru.quasaris.characternexus.ui.DeleteConfirmationDialog
import ru.quasaris.characternexus.tabs.attacks.AttackBonusIndicator
import ru.quasaris.characternexus.tabs.attacks.DiceIcon
import ru.quasaris.characternexus.tabs.attacks.SectionHeader
import ru.quasaris.characternexus.tabs.attacks.ProficiencyToggle
import ru.quasaris.characternexus.tabs.attacks.AttributeDropdown
import ru.quasaris.characternexus.tabs.attacks.AddBonusButton
import ru.quasaris.characternexus.tabs.attacks.AttackBonusField
import ru.quasaris.characternexus.tabs.attacks.DamageBonusField
import ru.quasaris.characternexus.tabs.attacks.calculateAttackFormulaParts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttackConfigDialog(
    attack: AttackEntry,
    proficiencyBonus: Int,
    attributeModifiers: Map<Attribute, Int>,
    onDismiss: () -> Unit,
    onSave: (AttackEntry) -> Unit,
    onDelete: (AttackEntry) -> Unit,
    forceBlurEnabled: Boolean = false,
    exhaustion: Int = 0,
    settingsViewModel: SettingsViewModel? = null,
    stats: Map<String, String> = emptyMap(),
    spellSettings: SpellSettings = SpellSettings(),
    isDesktop: Boolean = false,
    hazeState: HazeState? = null
) {
    var state by remember { mutableStateOf(attack) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    
    val handleDismiss = {
        focusManager.clearFocus()
        onDismiss()
    }
    
    val handleSave = {
        focusManager.clearFocus()
        onSave(state)
    }

    if (isDesktop) {
        AttackConfigDialogContent(
            attack = state,
            onAttackChange = { state = it },
            proficiencyBonus = proficiencyBonus,
            attributeModifiers = attributeModifiers,
            onDismiss = handleDismiss,
            onSave = handleSave,
            onDelete = { showDeleteConfirm = true },
            forceBlurEnabled = forceBlurEnabled,
            exhaustion = exhaustion,
            stats = stats,
            spellSettings = spellSettings,
            hazeState = hazeState
        )
    } else {
        Dialog(
            onDismissRequest = handleDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            DialogDimStyle(0f)
            AttackConfigDialogContent(
                attack = state,
                onAttackChange = { state = it },
                proficiencyBonus = proficiencyBonus,
                attributeModifiers = attributeModifiers,
                onDismiss = handleDismiss,
                onSave = handleSave,
                onDelete = { showDeleteConfirm = true },
                forceBlurEnabled = forceBlurEnabled,
                exhaustion = exhaustion,
                stats = stats,
                spellSettings = spellSettings,
                hazeState = hazeState
            )
        }
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttackConfigDialogContent(
    attack: AttackEntry,
    onAttackChange: (AttackEntry) -> Unit,
    proficiencyBonus: Int,
    attributeModifiers: Map<Attribute, Int>,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    forceBlurEnabled: Boolean,
    exhaustion: Int,
    stats: Map<String, String>,
    spellSettings: SpellSettings,
    hazeState: HazeState? = null
) {
    val attackCalculation = remember(attack, proficiencyBonus, attributeModifiers, exhaustion, stats, spellSettings) {
        if (attack.isMagic) {
            val bonuses = if (attack.magicType == MagicAttackType.ATTACK) spellSettings.spellAttackBonuses else spellSettings.spellSaveDcBonuses
            val abilityModifier = if (spellSettings.spellcastingAbility != Attribute.NONE) {
                val score = stats[spellSettings.spellcastingAbility.name.lowercase()] ?: "10"
                ru.quasaris.characternexus.backend.calculateModifier(score)
            } else 0
            
            val baseFlat = (if (attack.magicType == MagicAttackType.SAVE) 8 else 0) + proficiencyBonus + abilityModifier
            return@remember calculateAttackFormulaParts(
                baseFlat = baseFlat,
                bonuses = bonuses,
                stats = stats,
                renderInOrder = false // Group dice in config preview
            )
        }

        if (attack.attribute == Attribute.NONE) {
            return@remember Pair(0, emptyList<DicePart>())
        }
        val attrMod = attributeModifiers[attack.attribute] ?: 0
        val prof = if (attack.isProficient) proficiencyBonus else 0
        val baseFlat = attrMod + prof + attack.attackBonus
        
        calculateAttackFormulaParts(
            baseFlat = baseFlat,
            bonuses = attack.attackBonuses,
            stats = stats,
            renderInOrder = false
        )
    }
    
    val totalAttackBonus = attackCalculation.first
    val attackDice = attackCalculation.second

    val colorScheme = MaterialTheme.colorScheme
    val isOled = colorScheme.background == Color.Black

    BackHandler(onBack = onDismiss)

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .run {
                if (forceBlurEnabled && hazeState != null && !isOled) {
                    this.hazeEffect(state = hazeState) {
                        style = HazeStyle(
                            blurRadius = 24.dp,
                            tints = listOf(HazeTint(Color.Black.copy(alpha = 0.4f)))
                        )
                    }
                } else this
            },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Настройки атаки", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = if (forceBlurEnabled && !isOled) Color.Transparent.copy(alpha = 0.0f) else colorScheme.surface
                )
            )
        },
        containerColor = if (forceBlurEnabled && !isOled) Color.Transparent.copy(alpha = 0.0f) else colorScheme.background
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
                        value = attack.name,
                        onValueChange = { onAttackChange(attack.copy(name = it)) },
                        label = { Text("Название") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    AttackBonusIndicator(
                        bonus = totalAttackBonus, 
                        dice = attackDice,
                        showLabel = !attack.isMagic || attack.magicType == MagicAttackType.ATTACK
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
                        Switch(checked = attack.isMagic, onCheckedChange = { onAttackChange(attack.copy(isMagic = it)) })
                    }
                }

                if (attack.isMagic) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = attack.magicType == MagicAttackType.ATTACK,
                            onClick = { onAttackChange(attack.copy(magicType = MagicAttackType.ATTACK)) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) {
                            Text("Бросок атаки")
                        }
                        SegmentedButton(
                            selected = attack.magicType == MagicAttackType.SAVE,
                            onClick = { onAttackChange(attack.copy(magicType = MagicAttackType.SAVE)) },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Text("Спасбросок")
                        }
                    }
                }

                // АТАКА Section
                if (!attack.isMagic) {
                    SectionHeader("Атака")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProficiencyToggle(
                            isProficient = attack.isProficient,
                            proficiencyBonus = proficiencyBonus,
                            onToggle = { onAttackChange(attack.copy(isProficient = it)) },
                            modifier = Modifier.weight(1f)
                        )
                        AttributeDropdown(
                            selectedAttribute = attack.attribute,
                            onAttributeSelected = { onAttackChange(attack.copy(attribute = it)) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    attack.attackBonuses.forEachIndexed { index, bonus ->
                        CompositionLocalProvider(
                            LocalContentColor provides if (attack.attribute != Attribute.NONE) LocalContentColor.current else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        ) {
                            AttackBonusField(
                                bonus = bonus,
                                onUpdate = { updated ->
                                    val newList = attack.attackBonuses.toMutableList()
                                    newList[index] = updated
                                    onAttackChange(attack.copy(attackBonuses = newList))
                                },
                                onDelete = {
                                    val newList = attack.attackBonuses.toMutableList()
                                    newList.removeAt(index)
                                    onAttackChange(attack.copy(attackBonuses = newList))
                                }
                            )
                        }
                    }

                    AddBonusButton(enabled = attack.attribute != Attribute.NONE) {
                        onAttackChange(attack.copy(attackBonuses = attack.attackBonuses + AttackBonus(advantagePreference = AdvantagePreference.NONE)))
                    }
                }

                // УРОН Section
                SectionHeader("Урон")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = attack.damageFormula,
                        onValueChange = { onAttackChange(attack.copy(damageFormula = it)) },
                        label = { Text("Формула") },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("1d8+[STR]") },
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = attack.damageType,
                        onValueChange = { onAttackChange(attack.copy(damageType = it)) },
                        label = { Text("Вид Урона") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                attack.damageBonuses.forEachIndexed { index, bonus ->
                    DamageBonusField(
                        bonus = bonus,
                        onUpdate = { updated ->
                            val newList = attack.damageBonuses.toMutableList()
                            newList[index] = updated
                            onAttackChange(attack.copy(damageBonuses = newList))
                        },
                        onDelete = {
                            val newList = attack.damageBonuses.toMutableList()
                            newList.removeAt(index)
                            onAttackChange(attack.copy(damageBonuses = newList))
                        }
                    )
                }

                AddBonusButton {
                    onAttackChange(attack.copy(damageBonuses = attack.damageBonuses + DamageBonus()))
                }

                // Notes Section
                SectionHeader("Заметки")
                OutlinedTextField(
                    value = attack.notes,
                    onValueChange = { onAttackChange(attack.copy(notes = it)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp),
                    placeholder = { Text("Описание атаки...") },
                    shape = RoundedCornerShape(8.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onAttackChange(attack.copy(showNotes = !attack.showNotes)) }
                ) {
                    Checkbox(
                        checked = attack.showNotes,
                        onCheckedChange = { onAttackChange(attack.copy(showNotes = it)) }
                    )
                    Text("Отображать Заметки")
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Delete Button
                OutlinedButton(
                    onClick = onDelete,
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
                onClick = onSave,
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
