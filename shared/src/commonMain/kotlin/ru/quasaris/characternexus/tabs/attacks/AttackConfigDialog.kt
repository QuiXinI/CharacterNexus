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
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import ru.quasaris.characternexus.ui.DialogDimStyle
import ru.quasaris.characternexus.ui.BackHandler
import ru.quasaris.characternexus.ui.PredictiveBackBox
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

import androidx.compose.material.icons.filled.Check
import ru.quasaris.characternexus.backend.SpellbookManager
import ru.quasaris.characternexus.tabs.spells.SpellCardItem

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
    spellbookManager: SpellbookManager? = null,
    existingAttacks: List<AttackEntry> = emptyList(),
    isDesktop: Boolean = false,
    hazeState: HazeState? = null
) {
    var state by remember { mutableStateOf(attack) }
    
    LaunchedEffect(state) {
        if (state != attack) {
            onSave(state)
        }
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    
    val handleDismiss = {
        focusManager.clearFocus()
        onDismiss()
    }
    
    if (isDesktop) {
        AttackConfigDialogContent(
            attack = state,
            onAttackChange = { state = it },
            proficiencyBonus = proficiencyBonus,
            attributeModifiers = attributeModifiers,
            onDismiss = handleDismiss,
            onDelete = { showDeleteConfirm = true },
            forceBlurEnabled = forceBlurEnabled,
            exhaustion = exhaustion,
            stats = stats,
            spellSettings = spellSettings,
            spellbookManager = spellbookManager,
            existingAttacks = existingAttacks,
            settingsViewModel = settingsViewModel,
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
                onDelete = { showDeleteConfirm = true },
                forceBlurEnabled = forceBlurEnabled,
                exhaustion = exhaustion,
                stats = stats,
                spellSettings = spellSettings,
                spellbookManager = spellbookManager,
                existingAttacks = existingAttacks,
                settingsViewModel = settingsViewModel,
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
    onDelete: () -> Unit,
    forceBlurEnabled: Boolean,
    exhaustion: Int,
    stats: Map<String, String>,
    spellSettings: SpellSettings,
    spellbookManager: SpellbookManager? = null,
    existingAttacks: List<AttackEntry> = emptyList(),
    settingsViewModel: SettingsViewModel? = null,
    hazeState: HazeState? = null
) {
    var selectedTab by remember { mutableStateOf(0) }
    val allPreMadeMasteries = rememberAllWeaponMasteries()

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

    val preparedSpells = remember(spellSettings, spellbookManager) {
        val baseSpells = spellbookManager?.loadSpells() ?: emptyList()
        val allAvailable = baseSpells.map { spellSettings.spellOverrides[it.id] ?: it }.toMutableList()
        val baseIds = baseSpells.map { it.id }.toSet()
        spellSettings.spellOverrides.values.forEach { overrideSpell ->
            if (overrideSpell.id !in baseIds) {
                allAvailable.add(overrideSpell)
            }
        }
        val selectedSet = spellSettings.selectedSpellIds.toSet()
        val preparedSet = spellSettings.preparedSpellIds.toSet()

        fun matchesAny(spell: SpellCard, idSet: Set<String>): Boolean {
            return idSet.any { spell.matchesId(it) }
        }

        val filtered = if (spellSettings.isSpellbookEnabled) {
            allAvailable.filter { matchesAny(it, preparedSet) || (matchesAny(it, selectedSet) && it.isRitual) }
        } else {
            allAvailable.filter { matchesAny(it, selectedSet) }
        }

        filtered.sortedWith(
            compareBy<SpellCard> { it.level.toIntOrNull() ?: 99 }
                .thenBy { it.name }
        )
    }

    val groupedSpells = remember(preparedSpells) {
        preparedSpells.groupBy { it.level }.entries.sortedBy { (lvl, _) ->
            lvl.toIntOrNull() ?: 99
        }
    }

    PredictiveBackBox(
        onBack = onDismiss,
        modifier = Modifier.fillMaxSize()
    ) { _ ->
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
            Column {
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
                if (attack.isMagic) {
                    SecondaryTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = if (forceBlurEnabled && !isOled) Color.Transparent else colorScheme.surface,
                        contentColor = colorScheme.primary,
                        divider = {}
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Настройки", fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Заклинания", fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }
        },
        containerColor = if (forceBlurEnabled && !isOled) Color.Transparent.copy(alpha = 0.0f) else colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!attack.isMagic || selectedTab == 0) {
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
                                onAttributeSelected = { onAttributeSelected -> onAttackChange(attack.copy(attribute = onAttributeSelected)) },
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

                    // Параметры оружия и дистанция Section
                    SectionHeader("Тип атаки и дистанция")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = attack.isMelee,
                            onClick = { onAttackChange(attack.copy(isMelee = !attack.isMelee)) },
                            label = { Text("Рукопашная", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = attack.isRanged,
                            onClick = { onAttackChange(attack.copy(isRanged = !attack.isRanged)) },
                            label = { Text("Дальнобойная", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (attack.isMelee) {
                        OutlinedTextField(
                            value = attack.reach,
                            onValueChange = { onAttackChange(attack.copy(reach = it)) },
                            label = { Text("Досягаемость (рукопашная)") },
                            placeholder = { Text("5 фт.") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    if (attack.isRanged) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = attack.rangeNormal,
                                onValueChange = { onAttackChange(attack.copy(rangeNormal = it)) },
                                label = { Text("Обычная дистанция") },
                                placeholder = { Text("20 фт.") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            OutlinedTextField(
                                value = attack.rangeMax,
                                onValueChange = { onAttackChange(attack.copy(rangeMax = it)) },
                                label = { Text("Предельная дистанция (с помехой)") },
                                placeholder = { Text("60 фт.") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    // Оружейное мастерство Section
                    SectionHeader("Оружейное мастерство")

                    if (allPreMadeMasteries.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            allPreMadeMasteries.forEach { preMade ->
                                val isSelected = attack.weaponMasteries.any { 
                                    it.name.trim().equals(preMade.name.trim(), ignoreCase = true) 
                                }
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (isSelected) {
                                            val newList = attack.weaponMasteries.filterNot { 
                                                it.name.trim().equals(preMade.name.trim(), ignoreCase = true) 
                                            }
                                            onAttackChange(attack.copy(weaponMasteries = newList))
                                        } else {
                                            val newMastery = WeaponMastery(
                                                name = preMade.name,
                                                description = preMade.description
                                            )
                                            onAttackChange(attack.copy(weaponMasteries = attack.weaponMasteries + newMastery))
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = preMade.name,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                )
                            }
                        }
                    }

                    attack.weaponMasteries.forEachIndexed { index, mastery ->
                        val isPreMade = allPreMadeMasteries.any { 
                            it.name.trim().equals(mastery.name.trim(), ignoreCase = true) 
                        }
                        if (isPreMade) {
                            PreMadeWeaponMasteryCard(
                                mastery = mastery,
                                onDelete = {
                                    val newList = attack.weaponMasteries.toMutableList()
                                    newList.removeAt(index)
                                    onAttackChange(attack.copy(weaponMasteries = newList))
                                }
                            )
                        } else {
                            WeaponMasteryField(
                                mastery = mastery,
                                onUpdate = { updated ->
                                    val newList = attack.weaponMasteries.toMutableList()
                                    newList[index] = updated
                                    onAttackChange(attack.copy(weaponMasteries = newList))
                                },
                                onDelete = {
                                    val newList = attack.weaponMasteries.toMutableList()
                                    newList.removeAt(index)
                                    onAttackChange(attack.copy(weaponMasteries = newList))
                                }
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onAttackChange(attack.copy(weaponMasteries = attack.weaponMasteries + WeaponMastery()))
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Добавить своё мастерство",
                            color = colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
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

                    Spacer(modifier = Modifier.height(100.dp))
                }
            } else {
                // Tab 1: Prepared Spells List
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (preparedSpells.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Нет подготовленных заклинаний",
                                color = colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    } else {
                        Text(
                            "Выберите заклинание для этой атаки:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )

                        groupedSpells.forEach { (levelStr, levelSpells) ->
                            val levelTitle = remember(levelStr) {
                                val lvl = levelStr.toIntOrNull() ?: 0
                                if (lvl == 0) "Заговоры" else "$lvl уровень"
                            }

                            SectionHeader(levelTitle)

                            levelSpells.forEach { spellCard ->
                                val isCurrentSelected = attack.spellId == spellCard.id ||
                                        (attack.spellCard != null && attack.spellCard.matchesId(spellCard.id))

                                val isAlreadyAdded = remember(existingAttacks, attack.id, spellCard) {
                                    existingAttacks.any { existing ->
                                        existing.id != attack.id && (
                                            existing.spellId == spellCard.id ||
                                            (existing.spellCard != null && existing.spellCard.matchesId(spellCard.id))
                                        )
                                    }
                                }

                                var isExpanded by remember { mutableStateOf(false) }

                                Box(modifier = Modifier.fillMaxWidth()) {
                                    SpellCardItem(
                                        spell = spellCard,
                                        isExpanded = isExpanded,
                                        onToggleExpand = { isExpanded = !isExpanded },
                                        isSelected = isCurrentSelected,
                                        isGrayedOut = isAlreadyAdded,
                                        settingsViewModel = settingsViewModel
                                    )

                                    if (isAlreadyAdded) {
                                        Surface(
                                            color = colorScheme.surfaceVariant.copy(alpha = 0.85f),
                                            shape = RoundedCornerShape(16.dp),
                                            modifier = Modifier
                                                .matchParentSize()
                                                .clickable(enabled = false) {}
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Surface(
                                                    color = colorScheme.surfaceContainerHigh,
                                                    shape = RoundedCornerShape(8.dp),
                                                    border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.3f))
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Text(
                                                            "Уже добавлено в атаки",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .clickable {
                                                    if (isCurrentSelected) {
                                                        onAttackChange(
                                                            attack.copy(
                                                                spellId = null,
                                                                spellCard = null
                                                            )
                                                        )
                                                    } else {
                                                        onAttackChange(
                                                            attack.copy(
                                                                isMagic = true,
                                                                spellId = spellCard.id,
                                                                spellCard = spellCard,
                                                                name = spellCard.name,
                                                                damageFormula = spellCard.damageFormula,
                                                                damageType = spellCard.damageTypes.firstOrNull()?.displayName ?: spellCard.damageType,
                                                                magicType = if (spellCard.attackTypes.contains(MagicAttackType.SAVE) || spellCard.attackType == MagicAttackType.SAVE) MagicAttackType.SAVE else MagicAttackType.ATTACK,
                                                                notes = spellCard.description
                                                            )
                                                        )
                                                    }
                                                }
                                        ) {
                                            if (isCurrentSelected) {
                                                Surface(
                                                    color = colorScheme.primary,
                                                    shape = RoundedCornerShape(topEnd = 16.dp, bottomStart = 8.dp),
                                                    modifier = Modifier.align(Alignment.TopEnd)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = colorScheme.onPrimary,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                        Text(
                                                            "Выбрано",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = colorScheme.onPrimary
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(100.dp))
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.surfaceVariant.compositeOver(colorScheme.background),
                    contentColor = colorScheme.onSurfaceVariant
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    focusedElevation = 0.dp,
                    hoveredElevation = 0.dp
                )
            ) {
                Text("Закрыть", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
}
