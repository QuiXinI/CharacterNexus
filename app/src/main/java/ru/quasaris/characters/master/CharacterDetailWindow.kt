package ru.quasaris.characters.master

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.quasaris.characters.master.tabs.*
import ru.quasaris.characters.master.MainWindow.*
import ru.quasaris.characters.master.HeaderCode.*
import ru.quasaris.characters.master.tabs.attacks.AttacksTab
import ru.quasaris.characters.master.backend.*
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeSource
import ru.quasaris.characters.master.tabs.spells.SpellsTab
import ru.quasaris.characters.master.backend.SpellbookManager
import ru.quasaris.characters.master.ui.DiceRollingFab
import ru.quasaris.characters.master.backend.cropper.AvatarCropperWindow

/**
 * Стиль размытия для нижней панели выбора вкладок.
 */
val TabSheetHazeStyle = HazeStyle(
    blurRadius = 24.dp,
    tints = listOf(HazeTint(Color.Black.copy(alpha = 0.25f)))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterDetailWindow(
    character: Character?,
    onNavigateBack: () -> Unit,
    onDeleteCharacter: (Character) -> Unit,
    onSaveChanges: (Character) -> Unit,
    onOpenDrawer: () -> Unit = {},
    onRoll: (RollResult) -> Unit = {},
    onFullscreenDialogOpenChange: (Boolean) -> Unit = {},
    hazeState: HazeState? = null,
    popupHazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false,
    blurPopups: Boolean = false,
    settingsViewModel: SettingsViewModel? = null,
    spellbookManager: SpellbookManager? = null
) {
    if (character == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Персонаж не найден!", color = MaterialTheme.colorScheme.onBackground)
        }
        return
    }

    val state = rememberCharacterDetailState(character, settingsViewModel)
    
    val useNewAC by settingsViewModel?.useNewACInterface?.collectAsState() ?: remember { mutableStateOf(true) }
    val useNewInit by settingsViewModel?.useNewInitInterface?.collectAsState() ?: remember { mutableStateOf(true) }
    val useNewCond by settingsViewModel?.useNewCondInterface?.collectAsState() ?: remember { mutableStateOf(true) }
    val useNewSpeed by settingsViewModel?.useNewSpeedInterface?.collectAsState() ?: remember { mutableStateOf(true) }

    val diceFabOffsetX by settingsViewModel?.diceFabOffsetX?.collectAsState() ?: remember { mutableStateOf(-40f) }
    val diceFabOffsetY by settingsViewModel?.diceFabOffsetY?.collectAsState() ?: remember { mutableStateOf(-40f) }
    val diceFabAlphaSetting by settingsViewModel?.diceFabAlpha?.collectAsState() ?: remember { mutableStateOf(1.0f) }
    val diceFabBlurEnabled by settingsViewModel?.diceFabBlurEnabled?.collectAsState() ?: remember { mutableStateOf(true) }
    val masterBlurEnabled by settingsViewModel?.masterBlurEnabled?.collectAsState() ?: remember { mutableStateOf(true) }

    val effectiveDiceFabBlur = masterBlurEnabled && diceFabBlurEnabled
    val effectiveDiceFabAlpha = diceFabAlphaSetting
    val diceFabEnabled by settingsViewModel?.diceFabEnabled?.collectAsState() ?: remember { mutableStateOf(true) }

    val advantageLogic by settingsViewModel?.advantageLogic?.collectAsState() ?: remember { mutableStateOf(AdvantageLogic.TOTAL) }

    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.level) {
        state.nextLevelExp = getNextLevelThreshold(state.level)
    }

    val baseStatsMapForHP = remember(state.statsState, state.level, state.proficiencyBonus, state.manualMaxHitDice, state.hitDiceMap, state.hitDiceEntries) {
        val totalMaxHD = state.hitDiceMap.values.sum()
        val totalCurrentHD = totalMaxHD - state.hitDiceEntries.sumOf { it.spent }
        state.statsState.toStatsMap(state.level, state.proficiencyBonus) + mapOf(
            "manualMaxHitDice" to state.manualMaxHitDice.toString(),
            "totalMaxHitDice" to totalMaxHD.toString(),
            "totalCurrentHitDice" to totalCurrentHD.toString()
        )
    }

    val conMod = remember(baseStatsMapForHP) { evaluateFormula("[CON]", baseStatsMapForHP) }
    val levelInt = remember(state.level) { state.level.toIntOrNull() ?: 1 }
    val calculatedMaxHp = remember(state.isManualHP, state.manualMaxHp, state.hpLevelData, state.manualHPLevelData, conMod, levelInt, state.hpBonusesAtLevel, state.hpBonusesTotal, baseStatsMapForHP) {
        val totalFixedBonus = state.hpBonusesTotal.filter { it.isActive }.sumOf { evaluateFormula(it.formula, baseStatsMapForHP) }
        
        if (state.isManualHP) {
            state.manualMaxHp + totalFixedBonus
        } else {
            val totalPerLevelBonus = state.hpBonusesAtLevel.filter { it.isActive }.sumOf { evaluateFormula(it.formula, baseStatsMapForHP) }
            val dataToUse = state.hpLevelData.take(levelInt)
            val totalRolls = dataToUse.sumOf { it.rollResult ?: 0 }
            totalRolls + (conMod * levelInt) + (totalPerLevelBonus * levelInt) + totalFixedBonus
        }
    }

    LaunchedEffect(calculatedMaxHp) {
        state.maxHp = calculatedMaxHp.toString()
    }

    LaunchedEffect(state.level, state.isManualHP, state.hpLevelData, state.manualHPLevelData) {
        val targetLevel = state.level.toIntOrNull() ?: 1
        
        // Sync hit dice counters for short rest
        val dataToSync = if (state.isManualHP) state.manualHPLevelData else state.hpLevelData.take(targetLevel)
        val groups = dataToSync.groupBy { it.hitDie }
        
        // Update hitDiceMap
        val newHitDiceMap = groups.mapValues { it.value.size }
        if (newHitDiceMap != state.hitDiceMap) {
            state.hitDiceMap = newHitDiceMap
        }

        val newHitDiceEntries = groups.map { (die, list) ->
            val existing = state.hitDiceEntries.find { it.formula.endsWith("d$die") }
            ru.quasaris.characters.master.HitDiceEntry(
                id = existing?.id ?: java.util.UUID.randomUUID().toString(),
                name = existing?.name ?: "Кости Хитов d$die",
                formula = "${list.size}d$die",
                spent = existing?.spent?.coerceAtMost(list.size) ?: 0
            )
        }.sortedByDescending { 
            it.formula.split('d').lastOrNull()?.toIntOrNull() ?: 0 
        }

        if (newHitDiceEntries != state.hitDiceEntries) {
            state.hitDiceEntries = newHitDiceEntries
        }

        if (state.hpLevelData.size < targetLevel) {
            val newList = state.hpLevelData.toMutableList()
            for (i in newList.size + 1..targetLevel) {
                val die = if (state.isMulticlassHP) state.defaultHitDie else (newList.lastOrNull()?.hitDie ?: state.defaultHitDie)
                newList.add(ru.quasaris.characters.master.HPLevelEntry(level = i, hitDie = die))
            }
            state.hpLevelData = newList
        }
    }

    val fileCreatorLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/${ArchiveManager.EXPORT_EXTENSION}")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val currentCharacterState = state.toCharacter(character)
        scope.launch {
            ArchiveManager.exportCharacter(context, currentCharacterState, uri)
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(it)
                    val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                    state.bitmapToCrop = bitmap
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
    }

    val statsMap = remember(state.statsState, state.level, state.proficiencyBonus, state.spellSettings, state.currentHp, state.maxHp, state.tempHp, state.exhaustion, state.selectedConditions, state.activeArmorClassId, state.armorClassEntries, state.isShieldActive, state.activeShieldId, state.shieldEntries, state.manualMaxHitDice) {
        val pbVal = (state.proficiencyBonus.replace("+", "").toIntOrNull() ?: getProficiencyBonus(state.level))

        val baseStats = state.statsState.toStatsMap(state.level, pbVal.toString()) + ("manualMaxHitDice" to state.manualMaxHitDice.toString())
        val mutableStats = baseStats.toMutableMap()

        Attribute.entries.forEach { attr ->
            if (attr == Attribute.NONE) return@forEach
            val key = attr.name.lowercase()
            val baseScore = baseStats[key] ?: "10"
            val effScore = ru.quasaris.characters.master.tabs.attacks.calculateTotalBonus(
                bonuses = state.statsState.statBonuses.filter { it.attribute == attr && it.type == StatBonusType.CHARACTERISTIC_VALUE },
                stats = baseStats,
                initialValue = baseScore.toIntOrNull() ?: 10
            ).toString()

            mutableStats[key] = effScore
            mutableStats["base_$key"] = baseScore
        }

        mutableStats.apply {
            put("[MAG ATC BON]", state.spellSettings.spellAttackBonus.ifBlank { "0" })
            put("[МАГ АТК БОН]", state.spellSettings.spellAttackBonus.ifBlank { "0" })
            put("[MAG SAVE BON]", state.spellSettings.spellSaveDcBonus.ifBlank { "0" })
            put("[МАГ СПАС БОН]", state.spellSettings.spellSaveDcBonus.ifBlank { "0" })

            if (state.spellSettings.spellcastingAbility != Attribute.NONE) {
                val score = get(state.spellSettings.spellcastingAbility.name.lowercase()) ?: "10"
                val mod = calculateModifier(score)
                put("[mdmg]", mod.toString())
            } else {
                put("[mdmg]", "0")
            }

            put("hp", state.currentHp)
            put("max_hp", state.maxHp)
            put("temp_hp", state.tempHp)
            put("xp", state.experience)
            put("exhaustion", state.exhaustion.toString())
            put("conditions", state.selectedConditions.size.toString())

            if (state.spellSettings.spellcastingAbility != Attribute.NONE) {
                val score = get(state.spellSettings.spellcastingAbility.name.lowercase()) ?: "10"
                val mod = calculateModifier(score)
                put("[MAG MOD]", mod.toString())
                put("[МАГ МОД]", mod.toString())
            } else {
                put("[MAG MOD]", "0")
                put("[МАГ МОД]", "0")
            }

            val ac = CombatCalculations.calculateAC(
                state.activeArmorClassId, state.armorClassEntries, this, state.isShieldActive, state.activeShieldId, state.shieldEntries
            )
            put("ac", ac)
        }
    }

    val attributeModifiers = remember(statsMap) {
        Attribute.entries.filter { it != Attribute.NONE }.associateWith { attr ->
            calculateModifier(statsMap[attr.name.lowercase()] ?: "10")
        }
    }
    val pb = getProficiencyBonus(state.level)

    val acValue = CombatCalculations.calculateAC(state.activeArmorClassId, state.armorClassEntries, statsMap, state.isShieldActive, state.activeShieldId, state.shieldEntries)
    val initValue = CombatCalculations.calculateInitiative(state.activeInitiativeId, state.initiativeEntries, statsMap, state.exhaustion)
    val speedValue = CombatCalculations.calculateSpeed(state.activeSpeedId, state.speedEntries, statsMap, state.exhaustion)

    val healthState = remember(state.currentHp, state.maxHp) {
        val c = state.currentHp.toIntOrNull() ?: 0; val m = state.maxHp.toIntOrNull() ?: 0
        when { c <= 0 -> "dead"; m > 0 && (c <= m / 2) -> "bloodied"; else -> "healthy" }
    }
    val healthColor = when(healthState) { "dead" -> Color(0xFF454545); "bloodied" -> Color(0xFFE57373); else -> Color(0xFF00C46F) }
    val healthIcon = when(healthState) { "dead" -> R.drawable.ic_health_death; "bloodied" -> R.drawable.ic_health_bloodied; else -> R.drawable.ic_health }
    val allConditions = rememberAllConditions(context)

    val tabs = CharacterTab.entries
    val totalPages = 10000
    val initialPage = totalPages / 2 - (totalPages / 2 % tabs.size)
    val pagerState = rememberPagerState(initialPage = initialPage) { totalPages }
    val sheetState = rememberModalBottomSheetState()
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    DisposableEffect(Unit) {
        onDispose {
            focusManager.clearFocus()
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        state.isEditMode = false
        focusManager.clearFocus()
    }

    LaunchedEffect(
        state.name, state.characterClass, state.order, state.level, state.experience, state.characterImageData,
        state.statsState, state.maxHp, state.currentHp, state.tempHp, state.proficiencyBonus, state.selectedConditions, state.exhaustion,
        state.attacks, state.armorClassEntries, state.activeArmorClassId, state.initiativeEntries,
        state.activeInitiativeId, state.speedEntries, state.activeSpeedId, state.isShieldActive,
        state.shieldEntries, state.activeShieldId, state.themeSeedColorArgb, state.notes,
        state.skillsAndTraits, state.inventory, state.spells, state.spellSettings, state.wallet,
        state.bioShortFields, state.bioLongSections, state.hitDiceEntries, state.hitDiceMap, state.defaultHitDie, state.hpLevelData, state.manualHPLevelData, state.isMulticlassHP,
        state.isManualHP, state.manualMaxHp, state.manualMaxHitDice,
        state.hpBonusesAtLevel, state.hpBonusesTotal, state.hasInspiration
    ) {
        onSaveChanges(state.toCharacter(character))
    }

    val currentTab = tabs[pagerState.currentPage % tabs.size]
    var showTabSheet by remember { mutableStateOf(false) }
    val isOled = colorScheme.background == Color.Black

    val handleRestoration = { restType: String ->
        state.handleRestoration(restType, statsMap)
    }

    val isAnyFullscreenDialogOpen = state.showEnhancedAC || state.showEnhancedInit || state.showEnhancedSpeed || 
            state.showEnhancedCond || state.showCharacterSettings || state.showHealthSettings || 
            state.showSpellSettings || state.bitmapToCrop != null || state.isBonusConfigOpen || 
            state.isAttackConfigOpen || state.isSpellEditorOpen || state.isMagicBonusSettingsOpen ||
            state.isFullscreenDynamicFieldOpen || state.isWalletDialogOpen

    LaunchedEffect(isAnyFullscreenDialogOpen) {
        onFullscreenDialogOpenChange(isAnyFullscreenDialogOpen)
    }

    @Composable
    fun ExpandingPanelsWrapper() {
        val panelScrollState = rememberScrollState()
        val screenHeight = LocalConfiguration.current.screenHeightDp.dp
        val innerShadowColor = colorScheme.surface

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = screenHeight / 2)
                .clipToBounds()
                .drawBehind {
                    if (isOled) {
                        // Base glow for OLED/Dark theme
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    innerShadowColor.copy(alpha = 0.1f),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = 16.dp.toPx()
                            )
                        )
                    }
                }
        )
        {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Scrollable Content
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(panelScrollState)
                        .padding(horizontal = 4.dp)
                ) {
                    ExpandingPanelsSection(
                        isLevelPanelVisible = state.isLevelPanelVisible, level = state.level, onLevelChange = { state.level = it },
                        experience = state.experience, onExpChange = { state.experience = it },
                        proficiencyBonus = state.proficiencyBonus, onProfChange = { state.proficiencyBonus = it },
                        nextLevelExp = state.nextLevelExp, statsMap = statsMap,
                        isHealthPanelVisible = state.isHealthPanelVisible, maxHp = state.maxHp, onMaxHpChange = { state.maxHp = it },
                        tempHp = state.tempHp, onTempHpChange = { state.tempHp = it },
                        currentHp = state.currentHp, onCurrentHpChange = { state.currentHp = it },
                        onHealClick = { state.hpDialogType = "heal"; state.hpDialogValue = ""; state.showHpDialog = true },
                        onDamageClick = { state.hpDialogType = "damage"; state.hpDialogValue = ""; state.showHpDialog = true },
                        onTempClick = { state.hpDialogType = "temp"; state.hpDialogValue = ""; state.showHpDialog = true },
                        healthColor = healthColor, clampHp = { },
                        hpPanelHitDice = state.hitDiceEntries,
                        onSpentHitDiceChange = { idx, newValue ->
                            val newList = state.hitDiceEntries.toMutableList()
                            if (idx in newList.indices) {
                                newList[idx] = newList[idx].copy(spent = newValue)
                                state.hitDiceEntries = newList
                            }
                        },
                        onOpenHealthSettings = { state.showHealthSettings = true },
                        isRestPanelVisible = state.isRestPanelVisible,
                        onRestPanelDismiss = { state.isRestPanelVisible = false },
                        onRestPanelHitDiceChange = { state.hitDiceEntries = it },
                        onHealAmount = { amount ->
                            state.currentHp = minOf(state.maxHp.toIntOrNull() ?: 0, (state.currentHp.toIntOrNull() ?: 0) + amount).toString()
                        },
                        onShortRestConfirmed = {
                            handleRestoration("short")
                            state.isRestPanelVisible = false
                        },
                        onLongRest = { handleRestoration("long") },
                        onDawn = { handleRestoration("dawn") },
                        defaultHitDie = character.defaultHitDie,
                        isArmorClassPanelVisible = state.isArmorClassPanelVisible, armorClassEntries = state.armorClassEntries,
                        activeArmorClassId = state.activeArmorClassId, acDeleteConfirmId = state.acDeleteConfirmId,
                        onArmorClassEntries = { state.armorClassEntries = it }, onActiveArmorClass = { state.activeArmorClassId = it },
                        onAcDeleteReq = { state.acDeleteConfirmId = it }, onAddArmorClass = { state.armorClassEntries = state.armorClassEntries + ArmorClassEntry() },
                        isInitiativePanelVisible = state.isInitiativePanelVisible, initiativeEntries = state.initiativeEntries,
                        activeInitiativeId = state.activeInitiativeId, initDeleteConfirmId = state.initDeleteConfirmId,
                        onInitiativeEntries = { state.initiativeEntries = it }, onActiveInitiative = { state.activeInitiativeId = it },
                        onInitDeleteReq = { state.initDeleteConfirmId = it }, onAddInitiative = { state.initiativeEntries = state.initiativeEntries + InitiativeEntry() },
                        isConditionsPanelVisible = state.isConditionsPanelVisible, allConditions = allConditions,
                        selectedConditions = state.selectedConditions,
                        onToggleCondition = { cond -> state.selectedConditions = if (state.selectedConditions.contains(cond)) state.selectedConditions - cond else state.selectedConditions + cond },
                        exhaustion = state.exhaustion,
                        onExhaustionChange = { state.exhaustion = it },
                        isShieldActive = state.isShieldActive,
                        onShieldActiveChange = { state.isShieldActive = it },
                        shieldEntries = state.shieldEntries,
                        activeShieldId = state.activeShieldId,
                        shieldDeleteConfirmId = state.shieldDeleteConfirmId,
                        onShieldEntries = { state.shieldEntries = it },
                        onActiveShield = { state.activeShieldId = it },
                        onShieldDeleteReq = { state.shieldDeleteConfirmId = it },
                        onAddShield = { state.shieldEntries = state.shieldEntries + ShieldEntry() },
                        isSpeedPanelVisible = state.isSpeedPanelVisible, speedEntries = state.speedEntries,
                        activeSpeedId = state.activeSpeedId, speedDeleteConfirmId = state.speedDeleteConfirmId,
                        onSpeedEntries = { state.speedEntries = it }, onActiveSpeed = { state.activeSpeedId = it },
                        onSpeedDeleteReq = { state.speedDeleteConfirmId = it }, onAddSpeed = { state.speedEntries = state.speedEntries + SpeedEntry() }
                    )
                }

                // Overlay Fades (с плавным угасанием при сворачивании)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .matchParentSize()
                        .drawWithContent {
                            drawContent()

                            val shadowHeight = 14.dp.toPx()
                            val fadeAlpha = (size.height / (shadowHeight * 2)).coerceIn(0f, 1f)

                            if (fadeAlpha > 0.05f) {
                                val currentShadowColor = innerShadowColor.copy(alpha = innerShadowColor.alpha * fadeAlpha)
                                val actualShadowHeight = minOf(shadowHeight, size.height / 2f)

                                // Top Fade
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(currentShadowColor, Color.Transparent),
                                        startY = 0f,
                                        endY = actualShadowHeight
                                    ),
                                    size = size.copy(height = actualShadowHeight)
                                )

                                // Bottom Fade
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, currentShadowColor),
                                        startY = size.height - actualShadowHeight,
                                        endY = size.height
                                    ),
                                    topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - actualShadowHeight),
                                    size = size.copy(height = actualShadowHeight)
                                )
                            }
                        }
                )
            }
        }
    }

    Scaffold(
        containerColor = colorScheme.background,
        modifier = Modifier
            .blur(if (isAnyFullscreenDialogOpen && forceBlurEnabled) 24.dp else 0.dp)
            .run {
                if (isAnyFullscreenDialogOpen && forceBlurEnabled && !isOled) {
                    this.drawWithContent {
                        drawContent()
                        drawRect(colorScheme.surface.copy(alpha = 0.2f))
                    }
                } else this
            },
        topBar = {
            // Единый Surface обёртывает Хедер, Табы и Выпадающие Панели!
            Surface(
                tonalElevation = 1.dp,
                shadowElevation = 6.dp, // <--- Эта тень автоматически отбрасывается ВНИЗ под текущую границу всего блока!
                color = colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    CharacterHeader(
                        name = state.name, onNameChange = { state.name = it },
                        level = state.level, experience = state.experience, nextLevelExp = state.nextLevelExp,
                        characterImageData = state.characterImageData,
                        onAvatarClick = { state.showAvatarMenu = true },
                        onLevelClick = {
                            state.isLevelPanelVisible = !state.isLevelPanelVisible
                        },
                        onOpenDrawer = onOpenDrawer,
                        activeACValue = acValue,
                        onACClick = { state.isShieldActive = !state.isShieldActive },
                        onACLongClick = {
                            if (useNewAC) {
                                state.showEnhancedAC = true
                            } else {
                                state.isArmorClassPanelVisible = !state.isArmorClassPanelVisible
                            }
                        },
                        isShieldActive = state.isShieldActive,
                        activeInitValue = initValue,
                        onInitClick = {
                            val baseInit = (initValue.replace("+", "").toIntOrNull() ?: 0) + (state.exhaustion * 2)
                            val activeEntry = state.initiativeEntries.find { it.id == state.activeInitiativeId }
                            val advantage = if (activeEntry?.hasAdvantage == true) AdvantageType.ADVANTAGE else AdvantageType.NONE
                            onRoll(DiceRoller.roll("Инициатива", baseInit, bonuses = activeEntry?.bonuses ?: emptyList(), stats = statsMap, exhaustion = state.exhaustion, sourceType = RollSourceType.ABILITY, advantageType = advantage, advantageLogic = advantageLogic))
                        },
                        onInitLongClick = {
                            if (useNewInit) {
                                state.showEnhancedInit = true
                            } else {
                                state.isInitiativePanelVisible = !state.isInitiativePanelVisible
                            }
                        },
                        currentHp = state.currentHp, maxHp = state.maxHp, tempHp = state.tempHp,
                        healthColor = healthColor, healthIcon = healthIcon,
                        onHealthClick = {
                            state.isHealthPanelVisible = !state.isHealthPanelVisible
                        },
                        conditionsCount = state.exhaustion.toString(),
                        selectedConditions = state.selectedConditions,
                        onConditionsClick = {
                            if (useNewCond) {
                                state.showEnhancedCond = true
                            } else {
                                state.isConditionsPanelVisible = !state.isConditionsPanelVisible
                            }
                        },
                        activeSpeedValue = speedValue,
                        onSpeedClick = {
                            if (useNewSpeed) {
                                state.showEnhancedSpeed = true
                            } else {
                                state.isSpeedPanelVisible = !state.isSpeedPanelVisible
                            }
                        },
                        showAvatarMenu = state.showAvatarMenu,
                        onDismissAvatarMenu = { state.showAvatarMenu = false },
                        onImagePickerClick = { imagePickerLauncher.launch("image/*"); state.showAvatarMenu = false },
                        onDownloadClick = { fileCreatorLauncher.launch("MP_${state.name}.${ArchiveManager.EXPORT_EXTENSION}"); state.showAvatarMenu = false },
                        onDeletePortraitClick = { state.characterImageData = null; state.showAvatarMenu = false },
                        onSettingsClick = { state.showCharacterSettings = true; state.showAvatarMenu = false },
                        selectedImageUri = null,
                        onNavigateBack = onNavigateBack,
                        exhaustion = state.exhaustion,
                        hasInspiration = state.hasInspiration,
                        onInspirationChange = { state.hasInspiration = it },
                        onShortRest = {
                            state.isRestPanelVisible = !state.isRestPanelVisible
                        },
                        onLongRest = { handleRestoration("long") },
                        onDawn = { handleRestoration("dawn") },
                        hazeState = popupHazeState ?: hazeState,
                        blurPopups = blurPopups
                    )

                    TabNavigationBar(
                        currentTab = currentTab,
                        onShowTabSheet = { showTabSheet = true },
                        isEditMode = state.isEditMode,
                        onToggleEditMode = { state.isEditMode = !state.isEditMode },
                        isAdvancedMode = state.isAdvancedMode,
                        onToggleAdvancedMode = { state.isAdvancedMode = !state.isAdvancedMode },
                        hasContentToEdit = when(currentTab) {
                            CharacterTab.ATTACKS -> state.attacks.isNotEmpty()
                            CharacterTab.NOTES -> state.notes.isNotEmpty()
                            CharacterTab.SKILLS_FEATS -> state.skillsAndTraits.isNotEmpty()
                            CharacterTab.INVENTORY -> state.inventory.isNotEmpty()
                            CharacterTab.SPELLS -> state.spells.isNotEmpty()
                            CharacterTab.BIO -> true
                            else -> false
                        },
                        collapsibleTabs = listOf(
                            CharacterTab.SKILLS_FEATS,
                            CharacterTab.INVENTORY,
                            CharacterTab.SPELLS,
                            CharacterTab.NOTES
                        ),
                        anyCollapsed = when (currentTab) {
                            CharacterTab.SKILLS_FEATS -> state.skillsAndTraits.any { !it.isExpanded }
                            CharacterTab.INVENTORY -> state.inventory.any { !it.isExpanded }
                            CharacterTab.SPELLS -> state.spells.any { !it.isExpanded }
                            CharacterTab.NOTES -> state.notes.any { !it.isExpanded }
                            else -> false
                        },
                        onToggleAllExpansion = {
                            val currentList = when (currentTab) {
                                CharacterTab.SKILLS_FEATS -> state.skillsAndTraits
                                CharacterTab.INVENTORY -> state.inventory
                                CharacterTab.SPELLS -> state.spells
                                CharacterTab.NOTES -> state.notes
                                else -> emptyList()
                            }
                            val anyCollapsed = currentList.any { !it.isExpanded }
                            val newState = if (anyCollapsed) {
                                currentList.map { it.copy(isExpanded = true) }
                            } else {
                                currentList.map { it.copy(isExpanded = false) }
                            }
                            when (currentTab) {
                                CharacterTab.SKILLS_FEATS -> state.skillsAndTraits = newState
                                CharacterTab.INVENTORY -> state.inventory = newState
                                CharacterTab.SPELLS -> state.spells = newState
                                CharacterTab.NOTES -> state.notes = newState
                                else -> {}
                            }
                        },
                        onShowSpellSettings = { state.showSpellSettings = true }
                    )

                    // Расположение панели внутри Surface объединяет её с тенью от TopBar!
                    ExpandingPanelsWrapper()
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(Color.Transparent)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1
            ) { page ->
                val tab = tabs[page % tabs.size]

                when (tab) {
                    CharacterTab.STATS -> {
                        StatsTab(
                            character = character,
                            level = state.level,
                            statsState = state.statsState,
                            onStatsStateChange = { state.statsState = it },
                            onRoll = onRoll,
                            hazeState = popupHazeState ?: hazeState,
                            popupHazeState = popupHazeState,
                            forceBlurEnabled = forceBlurEnabled,
                            blurPopups = blurPopups,
                            isAdvancedMode = state.isAdvancedMode,
                            advantageLogic = advantageLogic,
                            attributeModifiers = attributeModifiers,
                            statsMap = statsMap,
                            onBonusConfigOpenChange = { state.isBonusConfigOpen = it }
                        )
                    }
                    CharacterTab.ATTACKS -> {
                        AttacksTab(
                            attacks = state.attacks,
                            proficiencyBonus = pb,
                            attributeModifiers = attributeModifiers,
                            onUpdateAttacks = { state.attacks = it },
                            onRoll = onRoll,
                            stats = statsMap,
                            exhaustion = state.exhaustion,
                            hazeState = popupHazeState ?: hazeState,
                            popupHazeState = popupHazeState,
                            forceBlurEnabled = forceBlurEnabled,
                            blurPopups = blurPopups,
                            isEditMode = state.isEditMode,
                            settingsViewModel = settingsViewModel,
                            spellSettings = state.spellSettings,
                            advantageLogic = advantageLogic,
                            onAttackConfigOpenChange = { state.isAttackConfigOpen = it }
                        )
                    }
                    CharacterTab.BIO -> {
                        BioTab(
                            character = character.copy(
                                bioShortFields = state.bioShortFields,
                                bioLongSections = state.bioLongSections,
                                imageData = state.characterImageData
                            ),
                            onCharacterChange = { updated ->
                                state.bioShortFields = updated.bioShortFields
                                state.bioLongSections = updated.bioLongSections
                                state.characterImageData = updated.imageData
                            },
                            onAvatarEditRequest = {
                                imagePickerLauncher.launch("image/*")
                            },
                            hazeState = popupHazeState ?: hazeState,
                            popupHazeState = popupHazeState,
                            forceBlurEnabled = forceBlurEnabled,
                            blurPopups = blurPopups,
                            isEditMode = state.isEditMode,
                            settingsViewModel = settingsViewModel,
                            statsMap = statsMap,
                            onFullscreenDialogOpenChange = { state.isFullscreenDynamicFieldOpen = it }
                        )
                    }
                    CharacterTab.SKILLS_FEATS -> {
                        SkillsFeatsTab(
                            skillsAndTraits = state.skillsAndTraits,
                            onSkillsAndTraitsChange = { state.skillsAndTraits = it },
                            hazeState = popupHazeState ?: hazeState,
                            popupHazeState = popupHazeState,
                            forceBlurEnabled = forceBlurEnabled,
                            blurPopups = blurPopups,
                            isEditMode = state.isEditMode,
                            settingsViewModel = settingsViewModel,
                            statsMap = statsMap,
                            onFullscreenDialogOpenChange = { state.isFullscreenDynamicFieldOpen = it }
                        )
                    }
                    CharacterTab.INVENTORY -> {
                        InventoryTab(
                            inventory = state.inventory,
                            onInventoryChange = { state.inventory = it },
                            wallet = state.wallet,
                            onWalletChange = { state.wallet = it },
                            hazeState = popupHazeState ?: hazeState,
                            popupHazeState = popupHazeState,
                            forceBlurEnabled = forceBlurEnabled,
                            blurPopups = blurPopups,
                            isEditMode = state.isEditMode,
                            settingsViewModel = settingsViewModel,
                            statsMap = statsMap,
                            onFullscreenDialogOpenChange = { state.isFullscreenDynamicFieldOpen = it },
                            onWalletDialogOpenChange = { state.isWalletDialogOpen = it }
                        )
                    }
                    CharacterTab.SPELLS -> {
                        SpellsTab(
                            spells = state.spells,
                            onSpellsChange = { state.spells = it },
                            characterLevel = state.level.toIntOrNull() ?: 1,
                            spellSettings = state.spellSettings,
                            onSpellSettingsChange = { state.spellSettings = it },
                            hazeState = popupHazeState ?: hazeState,
                            popupHazeState = popupHazeState,
                            forceBlurEnabled = forceBlurEnabled,
                            blurPopups = blurPopups,
                            isEditMode = state.isEditMode,
                            settingsViewModel = settingsViewModel,
                            onRoll = onRoll,
                            statsMap = statsMap,
                            exhaustion = state.exhaustion,
                            advantageLogic = advantageLogic,
                            spellbookManager = spellbookManager,
                            onSpellEditorOpenChange = { state.isSpellEditorOpen = it },
                            onMagicBonusSettingsOpenChange = { state.isMagicBonusSettingsOpen = it }
                        )
                    }
                    CharacterTab.NOTES -> {
                        NotesTab(
                            notes = state.notes,
                            onNotesChange = { state.notes = it },
                            hazeState = popupHazeState ?: hazeState,
                            popupHazeState = popupHazeState,
                            forceBlurEnabled = forceBlurEnabled,
                            blurPopups = blurPopups,
                            isEditMode = state.isEditMode,
                            settingsViewModel = settingsViewModel,
                            statsMap = statsMap
                        )
                    }
                }
            }

            val isKeyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

            if (!isKeyboardVisible && diceFabEnabled && !isAnyFullscreenDialogOpen) {
                DiceRollingFab(
                    onRoll = { pool ->
                        val res = DiceRoller.rollPool(pool)
                        onRoll(res)
                    },
                    offsetX = diceFabOffsetX,
                    offsetY = diceFabOffsetY,
                    hazeState = popupHazeState ?: hazeState,
                    isOled = colorScheme.background == Color.Black,
                    alpha = effectiveDiceFabAlpha,
                    forceBlurEnabled = effectiveDiceFabBlur,
                    positionKey = diceFabOffsetX to diceFabOffsetY,
                    onDrag = { dx, dy ->
                        val density = context.resources.displayMetrics.density
                        settingsViewModel?.updateDiceFabPosition(
                            diceFabOffsetX + dx / density,
                            diceFabOffsetY + dy / density
                        )
                    }
                )
            }

            CharacterDetailDialogs(
                state = state,
                statsMap = statsMap,
                hazeState = popupHazeState ?: hazeState,
                forceBlurEnabled = forceBlurEnabled,
                blurPopups = blurPopups,
                allConditions = allConditions
            )

            if (state.showCharacterSettings) {
                CharacterSettingsWindow(
                    state = state,
                    statsMap = statsMap,
                    onDismiss = { state.showCharacterSettings = false },
                    hazeState = popupHazeState ?: hazeState,
                    forceBlurEnabled = blurPopups
                )
            }
        }
    }

    TabSelectionSheet(
        showTabSheet = showTabSheet,
        onDismissRequest = { showTabSheet = false },
        sheetState = sheetState,
        currentTab = currentTab,
        tabs = tabs,
        pagerState = pagerState,
        scope = scope,
        hazeState = popupHazeState ?: hazeState,
        blurPopups = blurPopups
    )

    if (state.bitmapToCrop != null) {
        AvatarCropperWindow(
            imageToCrop = state.bitmapToCrop!!,
            hazeState = popupHazeState ?: hazeState,
            forceBlurEnabled = blurPopups,
            onCropSuccess = { cropped ->
                scope.launch {
                    val id = ImageManager.saveBitmapAsOriginal(context, state.bitmapToCrop!!)
                    ImageManager.saveCropped(context, id, cropped)

                    val portraitFile = ImageManager.getPortraitFile(context, id)
                    var seedColor: Int? = null
                    if (portraitFile.exists()) {
                        val bitmap = BitmapFactory.decodeFile(portraitFile.absolutePath)
                        if (bitmap != null) {
                            seedColor = PaletteHelper.extractSeedColor(bitmap)
                        }
                    }
                    state.characterImageData = id
                    state.themeSeedColorArgb = seedColor
                    state.bitmapToCrop = null
                }
            },
            onCancel = { state.bitmapToCrop = null }
        )
    }
}