package ru.quasaris.characternexus.tabs.spells

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import dev.chrisbanes.haze.*
import ru.quasaris.characternexus.ui.theme.rememberEffectiveBlurRadius
import ru.quasaris.characternexus.ui.theme.hazePopover
import ru.quasaris.characternexus.ui.DialogDimStyle
import ru.quasaris.characternexus.ui.BackHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.backend.SpellSlotCalculator
import ru.quasaris.characternexus.tabs.attacks.DiceIcon
import ru.quasaris.characternexus.backend.DicePart
import ru.quasaris.characternexus.backend.parseFormulaParts
import ru.quasaris.characternexus.backend.calculateModifier
import ru.quasaris.characternexus.backend.getProficiencyBonus
import ru.quasaris.characternexus.HeaderCode.Fullscreen.EditVariantDialog
import kotlin.math.floor

fun formatFloat(value: Float): String {
    return if (value == floor(value)) value.toInt().toString() else value.toString()
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SpellSettingsDialog(
    settings: SpellSettings,
    characterLevel: Int,
    onSettingsChange: (SpellSettings) -> Unit,
    onDismiss: () -> Unit,
    onSubDialogOpenChange: (Boolean) -> Unit = {},
    forceBlurEnabled: Boolean = false,
    statsMap: Map<String, String> = emptyMap(),
    isDesktop: Boolean = false,
    hazeState: HazeState? = null,
    popupHazeState: HazeState? = null,
    settingsViewModel: ru.quasaris.characternexus.backend.SettingsViewModel? = null
) {
    val focusManager = LocalFocusManager.current
    var isMagicEnabled by remember { mutableStateOf(settings.isMagicEnabled) }
    var spellcastingAbility by remember { mutableStateOf(settings.spellcastingAbility) }
    var isSpellbookEnabled by remember { mutableStateOf(settings.isSpellbookEnabled) }
    var spellAttackBonuses by remember { mutableStateOf(settings.spellAttackBonuses) }
    var spellSaveDcBonuses by remember { mutableStateOf(settings.spellSaveDcBonuses) }
    var spellMode by remember { mutableStateOf(settings.spellMode) }

    var showAttackBonusDialog by remember { mutableStateOf(false) }
    var showSaveDcBonusDialog by remember { mutableStateOf(false) }
    val isSubDialogOpen = showAttackBonusDialog || showSaveDcBonusDialog
    val blurRadius = rememberEffectiveBlurRadius(settingsViewModel)
    
    LaunchedEffect(isSubDialogOpen) {
        onSubDialogOpenChange(isSubDialogOpen)
    }

    val pb = getProficiencyBonus(characterLevel.toString())
    val currentAbilityModifier = remember(spellcastingAbility, statsMap) {
        if (spellcastingAbility != Attribute.NONE) {
            val statKey = spellcastingAbility.name.lowercase()
            calculateModifier(statsMap[statKey] ?: "10")
        } else 0
    }

    val currentAttackBonus = remember(spellAttackBonuses, currentAbilityModifier, pb, statsMap) {
        var totalFlat = pb + currentAbilityModifier
        val allDice = mutableMapOf<Int, Int>()
        spellAttackBonuses.forEach { bonus ->
            val (fFlat, fDice) = parseFormulaParts(bonus.formula, statsMap)
            totalFlat += fFlat
            fDice.forEach { allDice[it.sides] = (allDice[it.sides] ?: 0) + it.count }
        }
        Pair(totalFlat, allDice.map { DicePart(it.value, it.key) }.sortedBy { it.sides })
    }

    val currentSaveDc = remember(spellSaveDcBonuses, currentAbilityModifier, pb, statsMap) {
        var totalFlat = 8 + pb + currentAbilityModifier
        val allDice = mutableMapOf<Int, Int>()
        spellSaveDcBonuses.forEach { bonus ->
            val (fFlat, fDice) = parseFormulaParts(bonus.formula, statsMap)
            totalFlat += fFlat
            fDice.forEach { allDice[it.sides] = (allDice[it.sides] ?: 0) + it.count }
        }
        Pair(totalFlat, allDice.map { DicePart(it.value, it.key) }.sortedBy { it.sides })
    }

    val spellAttackBonusPreview = if (currentAttackBonus.first >= 0) "+${currentAttackBonus.first}" else currentAttackBonus.first.toString()
    val spellSaveDcPreview = currentSaveDc.first.toString()

    var casterType by remember { mutableStateOf(settings.casterType) }
    var isMulticlass by remember { mutableStateOf(settings.isMulticlass) }
    var fullCasterLevel by remember { mutableIntStateOf(settings.fullCasterLevel) }
    var halfCasterLevel by remember { mutableIntStateOf(settings.halfCasterLevel) }
    var thirdCasterLevel by remember { mutableIntStateOf(settings.thirdCasterLevel) }
    
    val specialSlots = remember { mutableStateListOf<SpecialSlotSettings>().apply { addAll(settings.specialSlots) } }
    var overrideSlots by remember { mutableStateOf(settings.overrideSlots) }
    var pactSlotLevel by remember { mutableStateOf(settings.pactSlotLevel) }
    var pactSlotsCount by remember { mutableIntStateOf(settings.pactSlotsCount) }
    var isPactEnabled by remember { mutableStateOf(settings.isPactEnabled) }
    var allowCantripUpcast by remember { mutableStateOf(settings.allowCantripUpcast) }
    
    var isSpecialSlotsEditMode by remember { mutableStateOf(false) }

    val autoSlots = remember(casterType, isMulticlass, fullCasterLevel, halfCasterLevel, thirdCasterLevel, characterLevel) {
        if (isMulticlass) {
            SpellSlotCalculator.getMulticlassSlots(fullCasterLevel, halfCasterLevel, thirdCasterLevel)
        } else {
            SpellSlotCalculator.getSlotsForLevel(casterType, characterLevel)
        }
    }

    val onSave = {
        focusManager.clearFocus()
        onSettingsChange(
            settings.copy(
                isMagicEnabled = isMagicEnabled,
                spellcastingAbility = spellcastingAbility,
                spellAttackBonuses = spellAttackBonuses,
                spellSaveDcBonuses = spellSaveDcBonuses,
                spellMode = spellMode,
                casterType = casterType,
                isMulticlass = isMulticlass,
                fullCasterLevel = fullCasterLevel,
                halfCasterLevel = halfCasterLevel,
                thirdCasterLevel = thirdCasterLevel,
                specialSlots = specialSlots.toList(),
                overrideSlots = overrideSlots,
                pactSlotLevel = pactSlotLevel,
                pactSlotsCount = pactSlotsCount,
                isPactEnabled = isPactEnabled,
                isSpellbookEnabled = isSpellbookEnabled,
                usedSlots = settings.usedSlots.filterKeys { it in overrideSlots.keys || it == pactSlotLevel || it in specialSlots.map { s -> s.level } },
                usedSlotsShortRest = settings.usedSlotsShortRest.filterKeys { it in overrideSlots.keys || it == pactSlotLevel || it in specialSlots.map { s -> s.level } },
                usedSlotsDawn = settings.usedSlotsDawn.filterKeys { it in overrideSlots.keys || it == pactSlotLevel || it in specialSlots.map { s -> s.level } },
                allowCantripUpcast = allowCantripUpcast
            )
        )
        onDismiss()
    }

    if (isDesktop) {
        SpellSettingsDialogContent(
            onDismiss = onDismiss,
            forceBlurEnabled = forceBlurEnabled,
            isMagicEnabled = isMagicEnabled,
            onIsMagicEnabledChange = { isMagicEnabled = it },
            isSpellbookEnabled = isSpellbookEnabled,
            onIsSpellbookEnabledChange = { isSpellbookEnabled = it },
            spellcastingAbility = spellcastingAbility,
            onSpellcastingAbilityChange = { spellcastingAbility = it },
            spellSaveDcPreview = spellSaveDcPreview,
            currentSaveDc = currentSaveDc,
            onSaveDcClick = { showSaveDcBonusDialog = true },
            spellAttackBonusPreview = spellAttackBonusPreview,
            currentAttackBonus = currentAttackBonus,
            onAttackBonusClick = { showAttackBonusDialog = true },
            spellMode = spellMode,
            onSpellModeChange = { spellMode = it },
            isMulticlass = isMulticlass,
            onIsMulticlassChange = { isMulticlass = it },
            fullCasterLevel = fullCasterLevel,
            onFullCasterLevelChange = { fullCasterLevel = it },
            halfCasterLevel = halfCasterLevel,
            onHalfCasterLevelChange = { halfCasterLevel = it },
            thirdCasterLevel = thirdCasterLevel,
            onThirdCasterLevelChange = { thirdCasterLevel = it },
            casterType = casterType,
            onCasterTypeChange = { casterType = it },
            overrideSlots = overrideSlots,
            onOverrideSlotsChange = { overrideSlots = it },
            autoSlots = autoSlots.toIntArray(),
            isSpecialSlotsEditMode = isSpecialSlotsEditMode,
            onIsSpecialSlotsEditModeChange = { isSpecialSlotsEditMode = it },
            specialSlots = specialSlots,
            isPactEnabled = isPactEnabled,
            onIsPactEnabledChange = { isPactEnabled = it },
            pactSlotLevel = pactSlotLevel,
            onPactSlotLevelChange = { pactSlotLevel = it },
            pactSlotsCount = pactSlotsCount,
            onPactSlotsCountChange = { pactSlotsCount = it },
            allowCantripUpcast = allowCantripUpcast,
            onAllowCantripUpcastChange = { allowCantripUpcast = it },
            onSave = onSave,
            hazeState = popupHazeState ?: hazeState,
            blurRadius = blurRadius,
            showAttackBonusDialog = showAttackBonusDialog,
            onShowAttackBonusDialogChange = { showAttackBonusDialog = it },
            showSaveDcBonusDialog = showSaveDcBonusDialog,
            onShowSaveDcBonusDialogChange = { showSaveDcBonusDialog = it },
            spellAttackBonuses = spellAttackBonuses,
            onSpellAttackBonusesChange = { spellAttackBonuses = it },
            spellSaveDcBonuses = spellSaveDcBonuses,
            onSpellSaveDcBonusesChange = { spellSaveDcBonuses = it },
            statsMap = statsMap,
            pb = pb,
            currentAbilityModifier = currentAbilityModifier,
            settingsViewModel = settingsViewModel,
            isDesktop = isDesktop
        )
    } else {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            DialogDimStyle(0f)
            SpellSettingsDialogContent(
                onDismiss = onDismiss,
                forceBlurEnabled = forceBlurEnabled,
                isMagicEnabled = isMagicEnabled,
                onIsMagicEnabledChange = { isMagicEnabled = it },
                isSpellbookEnabled = isSpellbookEnabled,
                onIsSpellbookEnabledChange = { isSpellbookEnabled = it },
                spellcastingAbility = spellcastingAbility,
                onSpellcastingAbilityChange = { spellcastingAbility = it },
                spellSaveDcPreview = spellSaveDcPreview,
                currentSaveDc = currentSaveDc,
                onSaveDcClick = { showSaveDcBonusDialog = true },
                spellAttackBonusPreview = spellAttackBonusPreview,
                currentAttackBonus = currentAttackBonus,
                onAttackBonusClick = { showAttackBonusDialog = true },
                spellMode = spellMode,
                onSpellModeChange = { spellMode = it },
                isMulticlass = isMulticlass,
                onIsMulticlassChange = { isMulticlass = it },
                fullCasterLevel = fullCasterLevel,
                onFullCasterLevelChange = { fullCasterLevel = it },
                halfCasterLevel = halfCasterLevel,
                onHalfCasterLevelChange = { halfCasterLevel = it },
                thirdCasterLevel = thirdCasterLevel,
                onThirdCasterLevelChange = { thirdCasterLevel = it },
                casterType = casterType,
                onCasterTypeChange = { casterType = it },
                overrideSlots = overrideSlots,
                onOverrideSlotsChange = { overrideSlots = it },
                autoSlots = autoSlots.toIntArray(),
                isSpecialSlotsEditMode = isSpecialSlotsEditMode,
                onIsSpecialSlotsEditModeChange = { isSpecialSlotsEditMode = it },
                specialSlots = specialSlots,
                isPactEnabled = isPactEnabled,
                onIsPactEnabledChange = { isPactEnabled = it },
                pactSlotLevel = pactSlotLevel,
                onPactSlotLevelChange = { pactSlotLevel = it },
                pactSlotsCount = pactSlotsCount,
                onPactSlotsCountChange = { pactSlotsCount = it },
                allowCantripUpcast = allowCantripUpcast,
                onAllowCantripUpcastChange = { allowCantripUpcast = it },
                onSave = onSave,
                hazeState = popupHazeState ?: hazeState,
                blurRadius = blurRadius,
                showAttackBonusDialog = showAttackBonusDialog,
                onShowAttackBonusDialogChange = { showAttackBonusDialog = it },
                showSaveDcBonusDialog = showSaveDcBonusDialog,
                onShowSaveDcBonusDialogChange = { showSaveDcBonusDialog = it },
                spellAttackBonuses = spellAttackBonuses,
                onSpellAttackBonusesChange = { spellAttackBonuses = it },
                spellSaveDcBonuses = spellSaveDcBonuses,
                onSpellSaveDcBonusesChange = { spellSaveDcBonuses = it },
                statsMap = statsMap,
                pb = pb,
                currentAbilityModifier = currentAbilityModifier,
                settingsViewModel = settingsViewModel,
                isDesktop = isDesktop
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SpellSettingsDialogContent(
    onDismiss: () -> Unit,
    forceBlurEnabled: Boolean,
    isMagicEnabled: Boolean,
    onIsMagicEnabledChange: (Boolean) -> Unit,
    isSpellbookEnabled: Boolean,
    onIsSpellbookEnabledChange: (Boolean) -> Unit,
    spellcastingAbility: Attribute,
    onSpellcastingAbilityChange: (Attribute) -> Unit,
    spellSaveDcPreview: String,
    currentSaveDc: Pair<Int, List<DicePart>>,
    onSaveDcClick: () -> Unit,
    spellAttackBonusPreview: String,
    currentAttackBonus: Pair<Int, List<DicePart>>,
    onAttackBonusClick: () -> Unit,
    spellMode: SpellMode,
    onSpellModeChange: (SpellMode) -> Unit,
    isMulticlass: Boolean,
    onIsMulticlassChange: (Boolean) -> Unit,
    fullCasterLevel: Int,
    onFullCasterLevelChange: (Int) -> Unit,
    halfCasterLevel: Int,
    onHalfCasterLevelChange: (Int) -> Unit,
    thirdCasterLevel: Int,
    onThirdCasterLevelChange: (Int) -> Unit,
    casterType: CasterType,
    onCasterTypeChange: (CasterType) -> Unit,
    overrideSlots: Map<Float, Int>,
    onOverrideSlotsChange: (Map<Float, Int>) -> Unit,
    autoSlots: IntArray,
    isSpecialSlotsEditMode: Boolean,
    onIsSpecialSlotsEditModeChange: (Boolean) -> Unit,
    specialSlots: MutableList<SpecialSlotSettings>,
    isPactEnabled: Boolean,
    onIsPactEnabledChange: (Boolean) -> Unit,
    pactSlotLevel: Float,
    onPactSlotLevelChange: (Float) -> Unit,
    pactSlotsCount: Int,
    onPactSlotsCountChange: (Int) -> Unit,
    allowCantripUpcast: Boolean,
    onAllowCantripUpcastChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    hazeState: HazeState? = null,
    blurRadius: androidx.compose.ui.unit.Dp = 24.dp,
    showAttackBonusDialog: Boolean = false,
    onShowAttackBonusDialogChange: (Boolean) -> Unit = {},
    showSaveDcBonusDialog: Boolean = false,
    onShowSaveDcBonusDialogChange: (Boolean) -> Unit = {},
    spellAttackBonuses: List<AttackBonus> = emptyList(),
    onSpellAttackBonusesChange: (List<AttackBonus>) -> Unit = {},
    spellSaveDcBonuses: List<AttackBonus> = emptyList(),
    onSpellSaveDcBonusesChange: (List<AttackBonus>) -> Unit = {},
    statsMap: Map<String, String> = emptyMap(),
    pb: Int = 0,
    currentAbilityModifier: Int = 0,
    settingsViewModel: ru.quasaris.characternexus.backend.SettingsViewModel? = null,
    isDesktop: Boolean = false
) {
    val focusManager = LocalFocusManager.current
    val colorScheme = MaterialTheme.colorScheme
    val isOled = colorScheme.background == Color.Black
    val localHazeState = remember { HazeState() }
    val masterBlurEnabled by settingsViewModel?.masterBlurEnabled?.collectAsState() ?: remember { mutableStateOf(true) }
    val isSubDialogOpen = showAttackBonusDialog || showSaveDcBonusDialog

    BackHandler(onBack = onDismiss)

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .run {
                    if (isSubDialogOpen && masterBlurEnabled) {
                        this.blur(blurRadius)
                    } else this
                }
        ) {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text("Настройки заклинаний", fontWeight = FontWeight.Black) },
                        navigationIcon = {
                            IconButton(onClick = {
                                focusManager.clearFocus()
                                onDismiss()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Закрыть")
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = if (forceBlurEnabled && !isOled && !isSubDialogOpen) Color.Transparent.copy(alpha = 0.0f) else colorScheme.surface
                        )
                    )
                },
                containerColor = if (forceBlurEnabled && !isOled && !isSubDialogOpen) Color.Transparent.copy(alpha = 0.0f) else colorScheme.background,
                modifier = Modifier
                    .fillMaxSize()
                    .run {
                        if (forceBlurEnabled && hazeState != null && !isOled) {
                            this.hazeEffect(state = hazeState) {
                                style = HazeStyle(
                                    blurRadius = blurRadius,
                                    tints = listOf(HazeTint(Color.Black.copy(alpha = 0.2f)))
                                )
                            }
                        } else this
                    }
                    .hazeSource(state = localHazeState)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDrag = { change, _ ->
                                change.consume()
                                focusManager.clearFocus()
                            }
                        )
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { focusManager.clearFocus() }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
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
                                Text("Использовать магию", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    if (isMagicEnabled) "Магия активна" else "Магия отключена",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(checked = isMagicEnabled, onCheckedChange = onIsMagicEnabledChange)
                        }
                    }

                    if (isMagicEnabled) {
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
                                    Text("Книга заклинаний", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        "Разделение на книгу и подготовку",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(checked = isSpellbookEnabled, onCheckedChange = onIsSpellbookEnabledChange)
                            }
                        }

                        // Characteristic Selection
                        SectionTitle("ХАРАКТЕРИСТИКА")
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = if (spellcastingAbility == Attribute.NONE) "Без характеристики" else spellcastingAbility.fullName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Ключевая характеристика") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                Attribute.entries.forEach { attr ->
                                    DropdownMenuItem(
                                        text = { Text(if (attr == Attribute.NONE) "Без характеристики" else attr.fullName) },
                                        onClick = {
                                            onSpellcastingAbilityChange(attr)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Bonuses with Preview
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedCard(
                                onClick = onSaveDcClick,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Спасбросок", style = MaterialTheme.typography.labelSmall, color = colorScheme.primary)
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(spellSaveDcPreview, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        if (currentSaveDc.second.isNotEmpty()) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                currentSaveDc.second.forEach { DiceIcon(it) }
                                            }
                                        }
                                    }
                                }
                            }
                            OutlinedCard(
                                onClick = onAttackBonusClick,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Бонус атаки", style = MaterialTheme.typography.labelSmall, color = colorScheme.primary)
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(spellAttackBonusPreview, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        if (currentAttackBonus.second.isNotEmpty()) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                currentAttackBonus.second.forEach { DiceIcon(it) }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Spell Mode
                        SectionTitle("РЕЖИМ ЗАКЛИНАНИЙ")
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = spellMode == SpellMode.TEXT,
                                onClick = { onSpellModeChange(SpellMode.TEXT) },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                            ) {
                                Text("Текст")
                            }
                            SegmentedButton(
                                selected = spellMode == SpellMode.HYBRID,
                                onClick = { onSpellModeChange(SpellMode.HYBRID) },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                            ) {
                                Text("Гибрид")
                            }
                            SegmentedButton(
                                selected = spellMode == SpellMode.CARDS,
                                onClick = { onSpellModeChange(SpellMode.CARDS) },
                                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                            ) {
                                Text("Карточки")
                            }
                        }

                        // Caster Type / Multiclass
                        SectionTitle("КОЛИЧЕСТВО ЯЧЕЕК")
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("Мультикласс", modifier = Modifier.weight(1f))
                            Switch(checked = isMulticlass, onCheckedChange = onIsMulticlassChange)
                        }

                        if (isMulticlass) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                LevelInputRow("Заклинатель", fullCasterLevel, onFullCasterLevelChange)
                                LevelInputRow("Полузаклинатель", halfCasterLevel, onHalfCasterLevelChange)
                                LevelInputRow("Особый заклинатель", thirdCasterLevel, onThirdCasterLevelChange)
                            }
                        } else {
                            var casterExpanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = casterExpanded,
                                onExpandedChange = { casterExpanded = it },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = casterType.displayName,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Тип заклинателя") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = casterExpanded) },
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = casterExpanded,
                                    onDismissRequest = { casterExpanded = false }
                                ) {
                                    CasterType.entries.forEach { type ->
                                        DropdownMenuItem(
                                            text = { Text(type.displayName) },
                                            onClick = {
                                                onCasterTypeChange(type)
                                                casterExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Slot Overrides Grid (3x3)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (row in 0 until 3) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    for (col in 1..3) {
                                        val level = row * 3 + col
                                        val levelFloat = level.toFloat()
                                        val manualValue = overrideSlots[levelFloat]
                                        val autoValue = autoSlots[level - 1]

                                        val displayValue = when {
                                            manualValue != null -> if (manualValue == 0) "" else manualValue.toString()
                                            else -> if (autoValue == 0) "" else autoValue.toString()
                                        }

                                        OutlinedTextField(
                                            value = displayValue,
                                            onValueChange = { newValue ->
                                                if (newValue.isBlank()) {
                                                    onOverrideSlotsChange(overrideSlots.toMutableMap().apply { put(levelFloat, 0) })
                                                } else {
                                                    val count = newValue.toIntOrNull()
                                                    if (count != null) {
                                                        onOverrideSlotsChange(overrideSlots.toMutableMap().apply { put(levelFloat, count) })
                                                    }
                                                }
                                            },
                                            label = { Text("${level}-й") },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            trailingIcon = if (overrideSlots.containsKey(levelFloat)) {
                                                {
                                                    IconButton(onClick = {
                                                        onOverrideSlotsChange(overrideSlots.toMutableMap().apply { remove(levelFloat) })
                                                    }) {
                                                        Icon(Icons.Default.Refresh, contentDescription = "Сброс", modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            } else null
                                        )
                                    }
                                }
                            }
                        }

                        // Special Slots Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            SectionTitle("ОСОБЫЕ ЯЧЕЙКИ")
                            if (specialSlots.isNotEmpty()) {
                                IconButton(onClick = { onIsSpecialSlotsEditModeChange(!isSpecialSlotsEditMode) }) {
                                    Icon(
                                        if (isSpecialSlotsEditMode) Icons.Default.EditOff else Icons.Default.Edit,
                                        contentDescription = "Редактировать",
                                        tint = if (isSpecialSlotsEditMode) colorScheme.primary else colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Special Slots
                        val specialSlotPositions = remember { mutableStateMapOf<Int, Float>() }
                        var draggedSlotIndex by remember { mutableStateOf<Int?>(null) }
                        var draggingSlotOffset by remember { mutableStateOf(0f) }

                        specialSlots.forEachIndexed { index, slot ->
                            SpecialSlotItem(
                                slot = slot,
                                index = index,
                                isEditMode = isSpecialSlotsEditMode,
                                isDragging = draggedSlotIndex == index,
                                onSlotChange = { updatedSlot -> specialSlots[index] = updatedSlot },
                                onDelete = { specialSlots.removeAt(index) },
                                onDrag = { offset ->
                                    draggedSlotIndex = index
                                    draggingSlotOffset += offset
                                    
                                    val currentPos = (specialSlotPositions[index] ?: 0f) + draggingSlotOffset
                                    val targetIndex = specialSlotPositions.entries
                                            .filter { it.key != index }
                                            .find { currentPos in it.value..(it.value + 100f) } // Rough height check
                                            ?.key

                                    if (targetIndex != null) {
                                        val item = specialSlots.removeAt(index)
                                        specialSlots.add(targetIndex, item)
                                        draggedSlotIndex = targetIndex
                                        draggingSlotOffset = 0f
                                    }
                                },
                                onDragEnd = {
                                    draggedSlotIndex = null
                                    draggingSlotOffset = 0f
                                },
                                modifier = Modifier.onGloballyPositioned { coords ->
                                    specialSlotPositions[index] = coords.positionInWindow().y
                                }
                            )
                        }
                        
                        Button(
                            onClick = { specialSlots.add(SpecialSlotSettings(level = 0f, count = 0)) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ДОБАВИТЬ ОСОБУЮ ЯЧЕЙКУ")
                        }

                        // Pact Slots
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            SectionTitle("ЯЧЕЙКИ ДОГОВОРА")
                            Switch(checked = isPactEnabled, onCheckedChange = onIsPactEnabledChange)
                        }

                        AnimatedVisibility(visible = isPactEnabled) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    var pactLevelText by remember { mutableStateOf(formatFloat(pactSlotLevel)) }
                                    OutlinedTextField(
                                        value = pactLevelText,
                                        onValueChange = { newVal ->
                                            pactLevelText = newVal
                                            newVal.toFloatOrNull()?.let { onPactSlotLevelChange(it) }
                                        },
                                        label = { Text("Уровень") },
                                        placeholder = {},
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                    )
                                    OutlinedTextField(
                                        value = if (pactSlotsCount == 0) "" else pactSlotsCount.toString(),
                                        onValueChange = { newVal ->
                                            if (newVal.isBlank()) {
                                                onPactSlotsCountChange(0)
                                            } else {
                                                val v = newVal.toIntOrNull()
                                                if (v != null) onPactSlotsCountChange(v)
                                            }
                                        },
                                        label = { Text("Количество") },
                                        placeholder = {},
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                }
                            }
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
                                Text("Апкаст заговоров", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    "Позволить усиливать заклинания 0 уровня",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(checked = allowCantripUpcast, onCheckedChange = onAllowCantripUpcastChange)
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = onSave,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Сохранить", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (showAttackBonusDialog) {
            MagicBonusSettingsDialog(
                title = "Магическая атака",
                bonuses = spellAttackBonuses,
                baseModifier = pb + currentAbilityModifier,
                stats = statsMap,
                proficiencyBonus = pb,
                onDismiss = { onShowAttackBonusDialogChange(false) },
                onSave = {
                    onSpellAttackBonusesChange(it)
                    onShowAttackBonusDialogChange(false)
                },
                forceBlurEnabled = forceBlurEnabled,
                isDesktop = isDesktop,
                hazeState = localHazeState,
                settingsViewModel = settingsViewModel,
                isNested = true,
                asOverlay = true
            )
        }

        if (showSaveDcBonusDialog) {
            MagicBonusSettingsDialog(
                title = "Магическая сложность",
                bonuses = spellSaveDcBonuses,
                baseModifier = 8 + pb + currentAbilityModifier,
                stats = statsMap,
                proficiencyBonus = pb,
                onDismiss = { onShowSaveDcBonusDialogChange(false) },
                onSave = {
                    onSpellSaveDcBonusesChange(it)
                    onShowSaveDcBonusDialogChange(false)
                },
                forceBlurEnabled = forceBlurEnabled,
                isDesktop = isDesktop,
                hazeState = localHazeState,
                settingsViewModel = settingsViewModel,
                isNested = true,
                asOverlay = true
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecialSlotItem(
    slot: SpecialSlotSettings,
    index: Int,
    isEditMode: Boolean,
    isDragging: Boolean,
    onSlotChange: (SpecialSlotSettings) -> Unit,
    onDelete: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(targetValue = if (isDragging) 1.05f else 1f, label = "scale")
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        AnimatedContent(
            targetState = isEditMode,
            transitionSpec = {
                (fadeIn() + expandVertically()).togetherWith(fadeOut() + shrinkVertically())
            },
            label = "SpecialSlotContent"
        ) { editMode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(if (editMode) 4.dp else 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (editMode) {
                    Icon(
                        imageVector = Icons.Default.UnfoldMore,
                        contentDescription = "Reorder",
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .size(32.dp)
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        onDrag(dragAmount.y)
                                    },
                                    onDragEnd = onDragEnd,
                                    onDragCancel = onDragEnd
                                )
                            },
                        tint = colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = if (editMode) 8.dp else 0.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (editMode) {
                        Text(
                            text = slot.name.ifBlank { "Без названия" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = slot.name,
                                onValueChange = { onSlotChange(slot.copy(name = it)) },
                                label = { Text("Название") },
                                placeholder = { Text("Название") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            IconButton(onClick = onDelete) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Delete",
                                    tint = colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            var levelText by remember(slot.id) { mutableStateOf(formatFloat(slot.level)) }
                            OutlinedTextField(
                                value = levelText,
                                onValueChange = { newVal ->
                                    levelText = newVal
                                    newVal.toFloatOrNull()?.let { onSlotChange(slot.copy(level = it)) }
                                },
                                label = { Text("Уровень") },
                                placeholder = {},
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                trailingIcon = if (slot.level != 1f) {
                                    {
                                        IconButton(onClick = { 
                                            onSlotChange(slot.copy(level = 1f))
                                            levelText = "1"
                                        }) {
                                            Icon(Icons.Default.Refresh, contentDescription = "Сброс", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                } else null
                            )
                            OutlinedTextField(
                                value = if (slot.count == 0) "" else slot.count.toString(),
                                onValueChange = { newVal ->
                                    if (newVal.isBlank()) {
                                        onSlotChange(slot.copy(count = 0))
                                    } else {
                                        val c = newVal.toIntOrNull()
                                        if (c != null) onSlotChange(slot.copy(count = c))
                                    }
                                },
                                label = { Text("Количество") },
                                placeholder = {},
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Восстановление", style = MaterialTheme.typography.bodyMedium)

                            SingleChoiceSegmentedButtonRow {
                                SegmentedButton(
                                    selected = slot.restoreOnShortRest,
                                    onClick = { onSlotChange(slot.copy(restoreOnShortRest = true, restoreOnDawn = false)) },
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                                ) {
                                    Icon(
                                        Icons.Default.WbSunny,
                                        contentDescription = "Short Rest",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                SegmentedButton(
                                    selected = slot.restoreOnDawn,
                                    onClick = { onSlotChange(slot.copy(restoreOnShortRest = false, restoreOnDawn = true)) },
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                                ) {
                                    Icon(
                                        Icons.Default.WbTwilight,
                                        contentDescription = "Dawn",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                SegmentedButton(
                                    selected = !slot.restoreOnShortRest && !slot.restoreOnDawn,
                                    onClick = { onSlotChange(slot.copy(restoreOnShortRest = false, restoreOnDawn = false)) },
                                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                                ) {
                                    Icon(
                                        Icons.Default.NightsStay,
                                        contentDescription = "Long Rest",
                                        modifier = Modifier.size(18.dp)
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

@Composable
fun LevelInputRow(label: String, value: Int, onValueChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f))
        OutlinedTextField(
            value = if (value == 0) "" else value.toString(),
            onValueChange = { newVal ->
                if (newVal.isBlank()) {
                    onValueChange(0)
                } else {
                    val v = newVal.toIntOrNull()
                    if (v != null) onValueChange(v.coerceIn(0, 20))
                }
            },
            modifier = Modifier.width(80.dp),
            shape = RoundedCornerShape(8.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
            singleLine = true
        )
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 8.dp)
    )
}
