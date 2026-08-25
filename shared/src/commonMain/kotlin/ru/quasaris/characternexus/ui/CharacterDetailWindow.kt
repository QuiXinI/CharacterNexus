package ru.quasaris.characternexus.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import kotlinx.coroutines.CoroutineScope
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.key.*
import androidx.compose.ui.focus.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import ru.quasaris.characternexus.backend.KeybindAction
import kotlinx.coroutines.launch
import ru.quasaris.characternexus.tabs.*
import ru.quasaris.characternexus.MainWindow.*
import ru.quasaris.characternexus.HeaderCode.*
import ru.quasaris.characternexus.tabs.attacks.AttacksTab
import ru.quasaris.characternexus.backend.*
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.model.Character
import ru.quasaris.characternexus.backend.CombatCalculations
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeSource
import ru.quasaris.characternexus.tabs.spells.SpellsTab
import ru.quasaris.characternexus.backend.SpellbookManager
import ru.quasaris.characternexus.ui.outerShadow
import androidx.compose.ui.graphics.RectangleShape
import characternexus.shared.generated.resources.Res
import characternexus.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ru.quasaris.characternexus.ui.DiceRollerFab
import ru.quasaris.characternexus.backend.cropper.AvatarCropperWindow
import ru.quasaris.characternexus.*
import ru.quasaris.characternexus.util.*
import ru.quasaris.characternexus.ui.CommonFilePicker
import ru.quasaris.characternexus.PaletteHelper

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
            Text(stringResource(Res.string.char_not_found), color = MaterialTheme.colorScheme.onBackground)
        }
        return
    }

    val state = rememberCharacterDetailState(character, settingsViewModel)
    state.CollectSettings()

    val scope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme

    BackHandler(onBack = onNavigateBack)

    LaunchedEffect(state.level) {
        state.nextLevelExp = getNextLevelThreshold(state.level)
    }

    LaunchedEffect(state.calculatedMaxHp, state.level, state.isManualHP, state.hpLevelData, state.manualHPLevelData) {
        state.syncHPAndHitDice()
    }

    var showExportSheetSaver by remember { mutableStateOf(false) }
    CommonFileSaver(
        show = showExportSheetSaver,
        fileName = "${character.name}_Sheet",
        fileExtension = ArchiveManager.EXPORT_EXTENSION
    ) { saver ->
        showExportSheetSaver = false
        saver?.let {
            val currentCharacterState = state.toCharacter(character)
            scope.launch {
                val bytes = ArchiveManager.getExportBundleBytes(listOf(currentCharacterState))
                it.save(bytes)
            }
        }
    }

    var showExportPortraitSaver by remember { mutableStateOf(false) }
    CommonFileSaver(
        show = showExportPortraitSaver,
        fileName = "${character.name}_Portrait",
        fileExtension = "webp"
    ) { saver ->
        showExportPortraitSaver = false
        saver?.let {
            scope.launch {
                character.imageData?.let { imageId ->
                    val portraitFile = ImageManager.getPortraitFile(imageId, character.uuid)
                    if (platformFileSystem.exists(portraitFile)) {
                        val bytes = platformFileSystem.read(portraitFile) { readByteArray() }
                        it.save(bytes)
                    }
                }
            }
        }
    }

    var showImagePicker by remember { mutableStateOf(false) }
    CommonFilePicker(show = showImagePicker, fileExtensions = listOf("jpg", "png", "webp")) { file ->
        showImagePicker = false
        file?.let {
            scope.launch {
                val bytes = it.readBytes()
                state.bytesToCrop = bytes
                state.imageToCrop = decodeImageBitmap(bytes)
            }
        }
    }

    val pb = getProficiencyBonus(state.level)
    val allConditions = rememberAllConditions()

    val tabs = CharacterTab.entries
    val totalPages = 10000
    val initialPage = totalPages / 2 - (totalPages / 2 % tabs.size)
    val pagerState = rememberPagerState(initialPage = initialPage) { totalPages }
    val sheetState = rememberModalBottomSheetState()
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val rootFocusRequester = remember { FocusRequester() }
    var isRootFocused by remember { mutableStateOf(false) }

    val keybinds by settingsViewModel?.keybinds?.collectAsState() ?: remember { mutableStateOf(emptyMap<KeybindAction, Key>()) }

    DisposableEffect(Unit) {
        onDispose {
            focusManager.clearFocus()
        }
    }

    LaunchedEffect(Unit) {
        rootFocusRequester.requestFocus()
    }

    LaunchedEffect(pagerState.currentPage) {
        state.isEditMode = false
        focusManager.clearFocus()
        rootFocusRequester.requestFocus()
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
        state.handleRestoration(restType, state.statsMap)
    }

    val isAnyFullscreenDialogOpen = state.showEnhancedAC || state.showEnhancedInit || state.showEnhancedSpeed || 
            state.showEnhancedCond || state.showCharacterSettings || state.showHealthSettings || 
            state.showSpellSettings || state.imageToCrop != null || state.isBonusConfigOpen || 
            state.isAttackConfigOpen || state.isSpellEditorOpen || state.isMagicBonusSettingsOpen ||
            state.isFullscreenDynamicFieldOpen || state.isWalletDialogOpen || state.isSpellbookSelectionOpen ||
            state.isArmorClassSubDialogOpen || state.isInitiativeSubDialogOpen || state.isSpeedSubDialogOpen

    val isAnyPanelVisible = state.isLevelPanelVisible || state.isHealthPanelVisible || 
            state.isRestPanelVisible || state.isArmorClassPanelVisible || 
            state.isInitiativePanelVisible || state.isConditionsPanelVisible || 
            state.isSpeedPanelVisible

    LaunchedEffect(isAnyFullscreenDialogOpen) {
        onFullscreenDialogOpenChange(isAnyFullscreenDialogOpen)
        if (!isAnyFullscreenDialogOpen && !isAnyPanelVisible) {
            rootFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(isAnyPanelVisible) {
        if (!isAnyPanelVisible && !isAnyFullscreenDialogOpen) {
            rootFocusRequester.requestFocus()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures {
                    focusManager.clearFocus()
                    rootFocusRequester.requestFocus()
                }
            }
    ) {
        Scaffold(
            containerColor = if (isAnyFullscreenDialogOpen && forceBlurEnabled && !isOled) Color.Transparent.copy(alpha = 0.0f) else colorScheme.background,
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(rootFocusRequester)
                .onFocusChanged { 
                    isRootFocused = it.isFocused 
                }
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        val action = keybinds.entries.find { it.value.keyCode == event.key.keyCode }?.key
                        if (!isRootFocused) {
                            // If cursor is in a text field, Intercept navigation keys to clear focus first
                            if (action == KeybindAction.BACK || action == KeybindAction.OPEN_DRAWER) {
                                focusManager.clearFocus()
                                rootFocusRequester.requestFocus()
                                return@onPreviewKeyEvent true
                            }
                        }
                    }
                    false
                }
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && isRootFocused) {
                        val action = keybinds.entries.find { it.value.keyCode == event.key.keyCode }?.key
                        if (action != null) {
                            when (action) {
                                KeybindAction.BACK -> {
                                    onNavigateBack()
                                    true
                                }
                                KeybindAction.OPEN_DRAWER -> {
                                    onOpenDrawer()
                                    true
                                }
                                KeybindAction.PREV_TAB -> {
                                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                                    true
                                }
                                KeybindAction.NEXT_TAB -> {
                                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                                    true
                                }
                                KeybindAction.TOGGLE_AC -> {
                                    if (state.useNewAC) state.showEnhancedAC = !state.showEnhancedAC
                                    else state.isArmorClassPanelVisible = !state.isArmorClassPanelVisible
                                    true
                                }
                                KeybindAction.TOGGLE_INIT -> {
                                    if (state.useNewInit) state.showEnhancedInit = !state.showEnhancedInit
                                    else state.isInitiativePanelVisible = !state.isInitiativePanelVisible
                                    true
                                }
                                KeybindAction.TOGGLE_HEALTH -> {
                                    state.isHealthPanelVisible = !state.isHealthPanelVisible
                                    true
                                }
                                KeybindAction.TOGGLE_COND -> {
                                    if (state.useNewCond) state.showEnhancedCond = !state.showEnhancedCond
                                    else state.isConditionsPanelVisible = !state.isConditionsPanelVisible
                                    true
                                }
                                KeybindAction.TOGGLE_SPEED -> {
                                    if (state.useNewSpeed) state.showEnhancedSpeed = !state.showEnhancedSpeed
                                    else state.isSpeedPanelVisible = !state.isSpeedPanelVisible
                                    true
                                }
                                KeybindAction.TOGGLE_LEVEL -> {
                                    state.isLevelPanelVisible = !state.isLevelPanelVisible
                                    true
                                }
                                KeybindAction.TOGGLE_REST -> {
                                    state.showRestPopup = !state.showRestPopup
                                    true
                                }
                                KeybindAction.TOGGLE_EDIT_MODE -> {
                                    val tab = tabs[pagerState.currentPage % tabs.size]
                                    if (tab == CharacterTab.STATS) {
                                        state.isAdvancedMode = !state.isAdvancedMode
                                    } else {
                                        state.isEditMode = !state.isEditMode
                                    }
                                    true
                                }
                                KeybindAction.TOGGLE_EXPANSION -> {
                                    val tab = tabs[pagerState.currentPage % tabs.size]
                                    if (tab == CharacterTab.STATS) {
                                        state.isAdvancedMode = !state.isAdvancedMode
                                    } else {
                                        val currentList = when (tab) {
                                            CharacterTab.SKILLS_FEATS -> state.skillsAndTraits
                                            CharacterTab.INVENTORY -> state.inventory
                                            CharacterTab.SPELLS -> state.spells
                                            CharacterTab.NOTES -> state.notes
                                            else -> emptyList()
                                        }
                                        if (currentList.isNotEmpty()) {
                                            val anyCollapsed = currentList.any { !it.isExpanded }
                                            val newState = if (anyCollapsed) {
                                                currentList.map { it.copy(isExpanded = true) }
                                            } else {
                                                currentList.map { it.copy(isExpanded = false) }
                                            }
                                            when (tab) {
                                                CharacterTab.SKILLS_FEATS -> state.skillsAndTraits = newState
                                                CharacterTab.INVENTORY -> state.inventory = newState
                                                CharacterTab.SPELLS -> state.spells = newState
                                                CharacterTab.NOTES -> state.notes = newState
                                                else -> {}
                                            }
                                        }
                                    }
                                    true
                                }
                                KeybindAction.ADD_ITEM -> {
                                    val tab = tabs[pagerState.currentPage % tabs.size]
                                    when (tab) {
                                        CharacterTab.ATTACKS -> state.attacks = state.attacks + AttackEntry()
                                        CharacterTab.BIO -> state.bioLongSections = state.bioLongSections + DynamicNoteState()
                                        CharacterTab.SKILLS_FEATS -> state.skillsAndTraits = state.skillsAndTraits + DynamicNoteState()
                                        CharacterTab.INVENTORY -> state.inventory = state.inventory + DynamicNoteState()
                                        CharacterTab.SPELLS -> state.spells = state.spells + DynamicNoteState()
                                        CharacterTab.NOTES -> state.notes = state.notes + DynamicNoteState()
                                        else -> {}
                                    }
                                    true
                                }
                            }
                        } else false
                    } else false
                }
                .blur(if (isAnyFullscreenDialogOpen && forceBlurEnabled && !isOled) 24.dp else 0.dp)
                .run {
                    if (isAnyFullscreenDialogOpen && forceBlurEnabled && !isOled) {
                        this.drawWithContent {
                            drawContent()
                            drawRect(colorScheme.surface.copy(alpha = 0.1f))
                        }
                    } else this
                },
            topBar = {
                CharacterDetailTopBar(
                    state = state,
                    onOpenDrawer = onOpenDrawer,
                    onRoll = onRoll,
                    onNavigateBack = onNavigateBack,
                    hazeState = hazeState,
                    popupHazeState = popupHazeState,
                    blurPopups = blurPopups,
                    colorScheme = colorScheme,
                    isOled = isOled,
                    isAnyPanelVisible = isAnyPanelVisible,
                    handleRestoration = handleRestoration,
                    currentTab = currentTab,
                    onShowTabSheet = { showTabSheet = true; rootFocusRequester.requestFocus() },
                    onShowSpellSettings = { state.showSpellSettings = true; rootFocusRequester.requestFocus() },
                    onImagePickerClick = { showImagePicker = true; rootFocusRequester.requestFocus() },
                    onExportSheetClick = { showExportSheetSaver = true; rootFocusRequester.requestFocus() },
                    onExportPortraitClick = { showExportPortraitSaver = true; rootFocusRequester.requestFocus() },
                    rootFocusRequester = rootFocusRequester,
                    settingsViewModel = settingsViewModel
                )
            }
        ) { paddingValues ->
            CharacterDetailMainContent(
                paddingValues = paddingValues,
                state = state,
                pagerState = pagerState,
                focusRequester = focusRequester,
                rootFocusRequester = rootFocusRequester,
                scope = scope,
                tabs = tabs,
                character = character,
                onRoll = onRoll,
                pb = pb,
                hazeState = hazeState,
                popupHazeState = popupHazeState,
                forceBlurEnabled = forceBlurEnabled,
                blurPopups = blurPopups,
                settingsViewModel = settingsViewModel,
                spellbookManager = spellbookManager,
                allConditions = allConditions,
                onNavigateBack = onNavigateBack,
                onFullscreenDialogOpenChange = onFullscreenDialogOpenChange,
                showImagePicker = { showImagePicker = true }
            )
        }
    }

    TabSelectionSheet(
        showTabSheet = showTabSheet,
        onDismissRequest = { 
            showTabSheet = false
            rootFocusRequester.requestFocus()
        },
        sheetState = sheetState,
        currentTab = currentTab,
        tabs = tabs,
        pagerState = pagerState,
        scope = scope,
        hazeState = popupHazeState ?: hazeState,
        blurPopups = blurPopups
    )

    if (state.imageToCrop != null && state.bytesToCrop != null) {
        AvatarCropperWindow(
            imageBitmap = state.imageToCrop!!,
            onCrop = { cropped ->
                scope.launch {
                    val croppedBytes = ImageProcessor.encodeToByteArray(cropped)
                    val originalBytes = state.bytesToCrop!!
                    
                    ImageManager.saveCharacterImages(
                        characterUuid = character.uuid,
                        originalBytes = originalBytes,
                        portraitBytes = originalBytes, // Using same for portrait high-res for now
                        croppedBytes = croppedBytes
                    )

                    val seedColor = PaletteHelper.extractSeedColor(croppedBytes)

                    state.characterImageData = generateUuid() // Using fresh UUID as "version" string
                    state.themeSeedColorArgb = seedColor
                    state.imageToCrop = null
                    state.bytesToCrop = null
                }
            },
            onDismiss = { state.imageToCrop = null; state.bytesToCrop = null }
        )
    }
}

@Composable
fun CharacterDetailTopBar(
    state: CharacterDetailState,
    onOpenDrawer: () -> Unit,
    onRoll: (RollResult) -> Unit,
    onNavigateBack: () -> Unit,
    hazeState: HazeState?,
    popupHazeState: HazeState?,
    blurPopups: Boolean,
    colorScheme: ColorScheme,
    isOled: Boolean,
    isAnyPanelVisible: Boolean,
    handleRestoration: (String) -> Unit,
    currentTab: CharacterTab,
    onShowTabSheet: () -> Unit,
    onShowSpellSettings: () -> Unit,
    onImagePickerClick: () -> Unit,
    onExportSheetClick: () -> Unit,
    onExportPortraitClick: () -> Unit,
    rootFocusRequester: FocusRequester,
    settingsViewModel: SettingsViewModel? = null
) {
    // Единый Surface обёртывает Хедер, Табы и Выпадающие Панели!
    Surface(
        tonalElevation = 1.dp,
        shadowElevation = 0.dp,
        color = colorScheme.surface,
        modifier = Modifier.fillMaxWidth().outerShadow(RectangleShape, blur = 6.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            CharacterHeader(
                name = state.name, onNameChange = { state.name = it },
                level = state.level, experience = state.experience, nextLevelExp = state.nextLevelExp,
                characterImageData = state.characterImageData,
                characterUuid = state.characterUuid,
                onAvatarClick = { state.showAvatarMenu = true; rootFocusRequester.requestFocus() },
                onLevelClick = {
                    state.isLevelPanelVisible = !state.isLevelPanelVisible
                    rootFocusRequester.requestFocus()
                },
                onOpenDrawer = onOpenDrawer,
                activeACValue = state.acValue,
                onACClick = { state.isShieldActive = !state.isShieldActive; rootFocusRequester.requestFocus() },
                onACLongClick = {
                    if (state.useNewAC) {
                        state.showEnhancedAC = true
                    } else {
                        state.isArmorClassPanelVisible = !state.isArmorClassPanelVisible
                    }
                    rootFocusRequester.requestFocus()
                },
                isShieldActive = state.isShieldActive,
                activeInitValue = state.initValue,
                onInitClick = {
                    val baseInit = (state.initValue.replace("+", "").toIntOrNull() ?: 0) + (state.exhaustion * 2)
                    val activeEntry = state.initiativeEntries.find { it.id == state.activeInitiativeId }
                    val advantage = if (activeEntry?.hasAdvantage == true) AdvantageType.ADVANTAGE else AdvantageType.NONE
                    onRoll(DiceRoller.roll("Инициатива", baseInit, bonuses = activeEntry?.bonuses ?: emptyList(), stats = state.statsMap, exhaustion = state.exhaustion, sourceType = RollSourceType.ABILITY, advantageType = advantage, advantageLogic = state.advantageLogic))
                    rootFocusRequester.requestFocus()
                },
                onInitLongClick = {
                    if (state.useNewInit) {
                        state.showEnhancedInit = true
                    } else {
                        state.isInitiativePanelVisible = !state.isInitiativePanelVisible
                    }
                    rootFocusRequester.requestFocus()
                },
                currentHp = state.currentHp, maxHp = state.maxHp, tempHp = state.tempHp,
                healthColor = state.healthColor, healthIcon = state.healthIcon,
                onHealthClick = {
                    state.isHealthPanelVisible = !state.isHealthPanelVisible
                    rootFocusRequester.requestFocus()
                },
                conditionsCount = state.exhaustion.toString(),
                selectedConditions = state.selectedConditions,
                onConditionsClick = {
                    if (state.useNewCond) {
                        state.showEnhancedCond = true
                    } else {
                        state.isConditionsPanelVisible = !state.isConditionsPanelVisible
                    }
                    rootFocusRequester.requestFocus()
                },
                activeSpeedValue = state.speedValue,
                onSpeedClick = {
                    if (state.useNewSpeed) {
                        state.showEnhancedSpeed = true
                    } else {
                        state.isSpeedPanelVisible = !state.isSpeedPanelVisible
                    }
                    rootFocusRequester.requestFocus()
                },
                showAvatarMenu = state.showAvatarMenu,
                onDismissAvatarMenu = { state.showAvatarMenu = false; rootFocusRequester.requestFocus() },
                onImagePickerClick = { onImagePickerClick(); state.showAvatarMenu = false; rootFocusRequester.requestFocus() },
                onExportSheetClick = { onExportSheetClick(); state.showAvatarMenu = false; rootFocusRequester.requestFocus() },
                onExportPortraitClick = { onExportPortraitClick(); state.showAvatarMenu = false; rootFocusRequester.requestFocus() },
                onDeletePortraitClick = { state.characterImageData = null; state.showAvatarMenu = false; rootFocusRequester.requestFocus() },
                onSettingsClick = { state.showCharacterSettings = true; state.showAvatarMenu = false; rootFocusRequester.requestFocus() },
                onNavigateBack = onNavigateBack,
                exhaustion = state.exhaustion,
                hasInspiration = state.hasInspiration,
                onInspirationChange = { state.hasInspiration = it; rootFocusRequester.requestFocus() },
                onShortRest = {
                    state.isRestPanelVisible = !state.isRestPanelVisible
                    rootFocusRequester.requestFocus()
                },
                onLongRest = { handleRestoration("long"); rootFocusRequester.requestFocus() },
                onDawn = { handleRestoration("dawn"); rootFocusRequester.requestFocus() },
                showRestPopup = state.showRestPopup,
                onShowRestPopupChange = { state.showRestPopup = it; rootFocusRequester.requestFocus() },
                hazeState = popupHazeState ?: hazeState,
                blurPopups = blurPopups,
                settingsViewModel = settingsViewModel
            )

            TabNavigationBar(
                currentTab = currentTab,
                onShowTabSheet = { onShowTabSheet(); rootFocusRequester.requestFocus() },
                isEditMode = state.isEditMode,
                onToggleEditMode = { 
                    state.isEditMode = !state.isEditMode
                    rootFocusRequester.requestFocus()
                },
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
                    CharacterTab.STATS,
                    CharacterTab.SKILLS_FEATS,
                    CharacterTab.INVENTORY,
                    CharacterTab.SPELLS,
                    CharacterTab.NOTES
                ),
                anyCollapsed = when (currentTab) {
                    CharacterTab.STATS -> !state.isAdvancedMode
                    CharacterTab.SKILLS_FEATS -> state.skillsAndTraits.any { !it.isExpanded }
                    CharacterTab.INVENTORY -> state.inventory.any { !it.isExpanded }
                    CharacterTab.SPELLS -> state.spells.any { !it.isExpanded }
                    CharacterTab.NOTES -> state.notes.any { !it.isExpanded }
                    else -> false
                },
                onToggleAllExpansion = {
                    if (currentTab == CharacterTab.STATS) {
                        state.isAdvancedMode = !state.isAdvancedMode
                    } else {
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
                    }
                    rootFocusRequester.requestFocus()
                },
                onShowSpellSettings = { onShowSpellSettings(); rootFocusRequester.requestFocus() }
            )

            // Расположение панели внутри Surface объединяет её с тенью от TopBar!
            if (isAnyPanelVisible) {
                CharacterDetailExpandingPanels(
                    state = state,
                    colorScheme = colorScheme,
                    isOled = isOled,
                    handleRestoration = handleRestoration
                )
            }
        }
    }
}

@Composable
fun CharacterDetailExpandingPanels(
    state: CharacterDetailState,
    colorScheme: ColorScheme,
    isOled: Boolean,
    handleRestoration: (String) -> Unit
) {
    val panelScrollState = rememberScrollState()
    val screenHeight = 800.dp
    val innerShadowColor = colorScheme.surface
    val allConditions = rememberAllConditions()

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
                    isLevelPanelVisible = state.isLevelPanelVisible,
                    level = state.level,
                    onLevelChange = { state.level = it },
                    experience = state.experience,
                    onExpChange = { state.experience = it },
                    proficiencyBonus = state.proficiencyBonus,
                    onProfChange = { state.proficiencyBonus = it },
                    nextLevelExp = state.nextLevelExp,
                    statsMap = state.statsMap,
                    isHealthPanelVisible = state.isHealthPanelVisible,
                    maxHp = state.maxHp,
                    onMaxHpChange = { state.maxHp = it },
                    tempHp = state.tempHp,
                    onTempHpChange = { state.tempHp = it },
                    currentHp = state.currentHp,
                    onCurrentHpChange = { state.currentHp = it },
                    onHealClick = {
                        state.hpDialogType = "heal"; state.hpDialogValue = ""; state.showHpDialog = true
                    },
                    onDamageClick = {
                        state.hpDialogType = "damage"; state.hpDialogValue = ""; state.showHpDialog = true
                    },
                    onTempClick = {
                        state.hpDialogType = "temp"; state.hpDialogValue = ""; state.showHpDialog = true
                    },
                    healthColor = state.healthColor,
                    clampHp = { },
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
                        state.currentHp = minOf(
                            state.maxHp.toIntOrNull() ?: 0,
                            (state.currentHp.toIntOrNull() ?: 0) + amount
                        ).toString()
                    },
                    onShortRestConfirmed = {
                        handleRestoration("short")
                        state.isRestPanelVisible = false
                    },
                    onLongRest = { handleRestoration("long") },
                    onDawn = { handleRestoration("dawn") },
                    defaultHitDie = state.defaultHitDie,
                    isArmorClassPanelVisible = state.isArmorClassPanelVisible,
                    armorClassEntries = state.armorClassEntries,
                    activeArmorClassId = state.activeArmorClassId,
                    acDeleteConfirmId = state.acDeleteConfirmId,
                    onArmorClassEntries = { state.armorClassEntries = it },
                    onActiveArmorClass = { state.activeArmorClassId = it },
                    onAcDeleteReq = { state.acDeleteConfirmId = it },
                    onAddArmorClass = { state.armorClassEntries = state.armorClassEntries + ArmorClassEntry() },
                    isInitiativePanelVisible = state.isInitiativePanelVisible,
                    initiativeEntries = state.initiativeEntries,
                    activeInitiativeId = state.activeInitiativeId,
                    initDeleteConfirmId = state.initDeleteConfirmId,
                    onInitiativeEntries = { state.initiativeEntries = it },
                    onActiveInitiative = { state.activeInitiativeId = it },
                    onInitDeleteReq = { state.initDeleteConfirmId = it },
                    onAddInitiative = { state.initiativeEntries = state.initiativeEntries + InitiativeEntry() },
                    isConditionsPanelVisible = state.isConditionsPanelVisible,
                    allConditions = allConditions,
                    selectedConditions = state.selectedConditions,
                    onToggleCondition = { cond ->
                        state.selectedConditions =
                            if (state.selectedConditions.contains(cond)) state.selectedConditions - cond else state.selectedConditions + cond
                    },
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
                    isSpeedPanelVisible = state.isSpeedPanelVisible,
                    speedEntries = state.speedEntries,
                    activeSpeedId = state.activeSpeedId,
                    speedDeleteConfirmId = state.speedDeleteConfirmId,
                    onSpeedEntries = { state.speedEntries = it },
                    onActiveSpeed = { state.activeSpeedId = it },
                    onSpeedDeleteReq = { state.speedDeleteConfirmId = it },
                    onAddSpeed = { state.speedEntries = state.speedEntries + SpeedEntry() }
                )
            }

            // Overlay Fades
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterDetailMainContent(
    paddingValues: PaddingValues,
    state: CharacterDetailState,
    pagerState: PagerState,
    focusRequester: FocusRequester,
    rootFocusRequester: FocusRequester,
    scope: CoroutineScope,
    tabs: List<CharacterTab>,
    character: Character,
    onRoll: (RollResult) -> Unit,
    pb: Int,
    hazeState: HazeState?,
    popupHazeState: HazeState?,
    forceBlurEnabled: Boolean,
    blurPopups: Boolean,
    settingsViewModel: SettingsViewModel?,
    spellbookManager: SpellbookManager?,
    allConditions: List<Condition>,
    onNavigateBack: () -> Unit,
    onFullscreenDialogOpenChange: (Boolean) -> Unit,
    showImagePicker: () -> Unit
) {
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val keybinds by settingsViewModel?.keybinds?.collectAsState() ?: remember { mutableStateOf(emptyMap<KeybindAction, Key>()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(Color.Transparent)
            .pointerInput(Unit) {
                detectTapGestures {
                    focusManager.clearFocus()
                    rootFocusRequester.requestFocus()
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
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
                            attributeModifiers = state.attributeModifiers,
                            statsMap = state.statsMap,
                            onBonusConfigOpenChange = { state.isBonusConfigOpen = it },
                            advantageLogic = state.advantageLogic
                        )
                    }
                    CharacterTab.ATTACKS -> {
                        AttacksTab(
                            attacks = state.attacks,
                            proficiencyBonus = pb,
                            attributeModifiers = state.attributeModifiers,
                            onUpdateAttacks = { state.attacks = it },
                            onRoll = onRoll,
                            stats = state.statsMap,
                            exhaustion = state.exhaustion,
                            hazeState = popupHazeState ?: hazeState,
                            popupHazeState = popupHazeState,
                            forceBlurEnabled = forceBlurEnabled,
                            blurPopups = blurPopups,
                            isEditMode = state.isEditMode,
                            settingsViewModel = settingsViewModel,
                            spellSettings = state.spellSettings,
                            advantageLogic = state.advantageLogic,
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
                                showImagePicker()
                            },
                            hazeState = popupHazeState ?: hazeState,
                            popupHazeState = popupHazeState,
                            forceBlurEnabled = forceBlurEnabled,
                            blurPopups = blurPopups,
                            isEditMode = state.isEditMode,
                            settingsViewModel = settingsViewModel,
                            statsMap = state.statsMap,
                            onFullscreenDialogOpenChange = onFullscreenDialogOpenChange,
                            onFullscreenVisibilityChanged = { state.isFullscreenDynamicFieldOpen = it }
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
                            statsMap = state.statsMap,
                            onFullscreenDialogOpenChange = onFullscreenDialogOpenChange,
                            onFullscreenVisibilityChanged = { state.isFullscreenDynamicFieldOpen = it }
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
                            statsMap = state.statsMap,
                            onFullscreenDialogOpenChange = onFullscreenDialogOpenChange,
                            onFullscreenVisibilityChanged = { state.isFullscreenDynamicFieldOpen = it },
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
                            statsMap = state.statsMap,
                            exhaustion = state.exhaustion,
                            advantageLogic = state.advantageLogic,
                            spellbookManager = spellbookManager,
                            onSpellEditorOpenChange = { state.isSpellEditorOpen = it },
                            onMagicBonusSettingsOpenChange = { state.isMagicBonusSettingsOpen = it },
                            onFullscreenDialogOpenChange = onFullscreenDialogOpenChange,
                            onFullscreenVisibilityChanged = { state.isFullscreenDynamicFieldOpen = it },
                            onSpellbookSelectionOpenChange = { state.isSpellbookSelectionOpen = it }
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
                            statsMap = state.statsMap,
                            onFullscreenDialogOpenChange = onFullscreenDialogOpenChange,
                            onFullscreenVisibilityChanged = { state.isFullscreenDynamicFieldOpen = it }
                        )
                    }
                }
            }



            CharacterDetailDialogs(
                state = state,
                statsMap = state.statsMap,
                forceBlurEnabled = forceBlurEnabled,
                blurPopups = blurPopups,
                allConditions = allConditions
            )

            if (state.showCharacterSettings) {
                CharacterSettingsWindow(
                    state = state,
                    statsMap = state.statsMap,
                    onDismiss = { state.showCharacterSettings = false },
                    forceBlurEnabled = blurPopups
                )
            }
        }
    }
}
