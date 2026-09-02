package ru.quasaris.characternexus.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.ExperimentalComposeUiApi
import ru.quasaris.characternexus.ui.Dimensions
import ru.quasaris.characternexus.ui.components.SectionOverlay

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
import dev.chrisbanes.haze.*
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

@OptIn(ExperimentalComposeUiApi::class)
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
    
    val desktopTabs = remember(tabs) { tabs.filter { it != CharacterTab.STATS } }
    val desktopPagerState = rememberPagerState(
        initialPage = 10000 / 2 - (10000 / 2 % desktopTabs.size)
    ) { 10000 }

    val sheetState = rememberModalBottomSheetState()
    var showTabSheet by remember { mutableStateOf(false) }
    val isOled = colorScheme.background == Color.Black

    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val detailFocusRequester = remember { FocusRequester() }
    val rootFocusRequester = remember { FocusRequester() }
    var isRootFocused by remember { mutableStateOf(false) }
    var isRootHasFocus by remember { mutableStateOf(false) }

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

    val handleRestoration = { restType: String ->
        state.handleRestoration(restType, state.statsMap)
    }

    val isAnyFullscreenDialogOpen = state.showEnhancedAC || state.showEnhancedInit || state.showEnhancedSpeed || 
            state.showEnhancedCond || state.showCharacterSettings || state.showHealthSettings || 
            state.showSpellSettings || state.imageToCrop != null || state.isBonusConfigOpen || 
            state.isAttackConfigOpen || state.isSpellEditorOpen || state.isMagicBonusSettingsOpen ||
            state.isFullscreenDynamicFieldOpen || state.isWalletDialogOpen || state.isSpellbookSelectionOpen ||
            state.isArmorClassSubDialogOpen || state.isInitiativeSubDialogOpen || state.isSpeedSubDialogOpen || 
            state.isResourceConfigOpen || state.showHpDialog

    val isAnyPanelVisible = state.isLevelPanelVisible || state.isHealthPanelVisible || 
            state.isRestPanelVisible || state.isArmorClassPanelVisible || 
            state.isInitiativePanelVisible || state.isConditionsPanelVisible || 
            state.isSpeedPanelVisible

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures {
                    focusManager.clearFocus()
                }
            }
    ) {
        state.windowWidth = maxWidth
        val isDesktopMode by remember(maxWidth, state.interfaceMode) {
            derivedStateOf {
                when (state.interfaceMode) {
                    AppInterfaceMode.MOBILE -> false
                    AppInterfaceMode.DESKTOP -> true
                    AppInterfaceMode.AUTO -> maxWidth >= Dimensions.DesktopSplitThreshold
                }
            }
        }
        val isDesktop = isDesktopMode
        
        // On Desktop, side overlays don't block FAB/Overlay. On Mobile, everything does.
        val isTrulyFullscreenBlocking = if (isDesktop) {
            state.imageToCrop != null
        } else {
            isAnyFullscreenDialogOpen || isAnyPanelVisible
        }

        LaunchedEffect(isTrulyFullscreenBlocking) {
            onFullscreenDialogOpenChange(isTrulyFullscreenBlocking)
        }
        
        val activePagerState = if (isDesktop) desktopPagerState else pagerState
        
        Scaffold(
            containerColor = colorScheme.background,
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(rootFocusRequester)
                .onFocusChanged { 
                    isRootFocused = it.isFocused 
                    isRootHasFocus = it.hasFocus
                    if (!it.isFocused && !it.hasFocus && !isAnyFullscreenDialogOpen) {
                         rootFocusRequester.requestFocus()
                    }
                }
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        val action = keybinds.entries.find { it.value.keyCode == event.key.keyCode }?.key
                        
                        if (event.key == Key.Escape) {
                            val isAnyDesktopOverlayOpen = isDesktop && (isAnyFullscreenDialogOpen || isAnyPanelVisible)
                            if (isAnyDesktopOverlayOpen) {
                                state.closeMajorOverlays()
                                return@onPreviewKeyEvent true
                            }
                            if (!isRootFocused) {
                                focusManager.clearFocus()
                                rootFocusRequester.requestFocus()
                                return@onPreviewKeyEvent true
                            }
                        }

                        if (!isRootFocused && !isRootHasFocus) {
                            if (action == KeybindAction.OPEN_DRAWER) {
                                focusManager.clearFocus()
                                rootFocusRequester.requestFocus()
                                return@onPreviewKeyEvent true
                            }
                        }
                    }
                    false
                }
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && (isRootFocused || isRootHasFocus)) {
                        val action = keybinds.entries.find { it.value.keyCode == event.key.keyCode }?.key
                        if (action != null) {
                            when (action) {
                                KeybindAction.SWITCH_FOCUS -> {
                                    if (isDesktop) {
                                        state.activeSection = if (state.activeSection == "left") "right" else "left"
                                    }
                                    true
                                }
                                KeybindAction.BACK -> {
                                    onNavigateBack()
                                    true
                                }
                                KeybindAction.OPEN_DRAWER -> {
                                    onOpenDrawer()
                                    true
                                }
                                KeybindAction.PREV_TAB -> {
                                    scope.launch { activePagerState.animateScrollToPage(activePagerState.currentPage - 1) }
                                    true
                                }
                                KeybindAction.NEXT_TAB -> {
                                    scope.launch { activePagerState.animateScrollToPage(activePagerState.currentPage + 1) }
                                    true
                                }
                                KeybindAction.TOGGLE_AC -> {
                                    if (state.useNewAC) {
                                        if (!state.showEnhancedAC) state.closeMajorOverlays()
                                        state.showEnhancedAC = !state.showEnhancedAC
                                    } else {
                                        if (!state.isArmorClassPanelVisible) state.closeMajorOverlays()
                                        state.isArmorClassPanelVisible = !state.isArmorClassPanelVisible
                                    }
                                    true
                                }
                                KeybindAction.TOGGLE_INIT -> {
                                    if (state.useNewInit) {
                                        if (!state.showEnhancedInit) state.closeMajorOverlays()
                                        state.showEnhancedInit = !state.showEnhancedInit
                                    } else {
                                        if (!state.isInitiativePanelVisible) state.closeMajorOverlays()
                                        state.isInitiativePanelVisible = !state.isInitiativePanelVisible
                                    }
                                    true
                                }
                                KeybindAction.TOGGLE_HEALTH -> {
                                    if (!state.isHealthPanelVisible) state.closeMajorOverlays()
                                    state.isHealthPanelVisible = !state.isHealthPanelVisible
                                    true
                                }
                                KeybindAction.TOGGLE_COND -> {
                                    if (state.useNewCond) {
                                        if (!state.showEnhancedCond) state.closeMajorOverlays()
                                        state.showEnhancedCond = !state.showEnhancedCond
                                    } else {
                                        if (!state.isConditionsPanelVisible) state.closeMajorOverlays()
                                        state.isConditionsPanelVisible = !state.isConditionsPanelVisible
                                    }
                                    true
                                }
                                KeybindAction.TOGGLE_SPEED -> {
                                    if (state.useNewSpeed) {
                                        if (!state.showEnhancedSpeed) state.closeMajorOverlays()
                                        state.showEnhancedSpeed = !state.showEnhancedSpeed
                                    } else {
                                        if (!state.isSpeedPanelVisible) state.closeMajorOverlays()
                                        state.isSpeedPanelVisible = !state.isSpeedPanelVisible
                                    }
                                    true
                                }
                                KeybindAction.TOGGLE_LEVEL -> {
                                    if (!state.isLevelPanelVisible) state.closeMajorOverlays()
                                    state.isLevelPanelVisible = !state.isLevelPanelVisible
                                    true
                                }
                                KeybindAction.TOGGLE_REST -> {
                                    state.showRestPopup = !state.showRestPopup
                                    true
                                }
                                KeybindAction.TOGGLE_EDIT_MODE -> {
                                    val activeTab = if (isDesktop) {
                                        if (state.activeSection == "left") CharacterTab.STATS
                                        else desktopTabs[desktopPagerState.currentPage % desktopTabs.size]
                                    } else tabs[pagerState.currentPage % tabs.size]

                                    if (activeTab == CharacterTab.STATS) {
                                        state.isAdvancedMode = !state.isAdvancedMode
                                    } else {
                                        state.isEditMode = !state.isEditMode
                                    }
                                    true
                                }
                                KeybindAction.TOGGLE_EXPANSION -> {
                                    val activeTab = if (isDesktop) {
                                        if (state.activeSection == "left") CharacterTab.STATS
                                        else desktopTabs[desktopPagerState.currentPage % desktopTabs.size]
                                    } else tabs[pagerState.currentPage % tabs.size]

                                    if (activeTab == CharacterTab.STATS) {
                                        state.isAdvancedMode = !state.isAdvancedMode
                                    } else {
                                        val currentList = when (activeTab) {
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
                                            when (activeTab) {
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
                                    val activeTab = if (isDesktop) {
                                        if (state.activeSection == "left") CharacterTab.STATS
                                        else desktopTabs[desktopPagerState.currentPage % desktopTabs.size]
                                    } else tabs[pagerState.currentPage % tabs.size]

                                    when (activeTab) {
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
                                else -> false
                            }
                        } else false
                    } else false
            },
            topBar = {
                if (!isDesktop) {
                    val currentTab = tabs[pagerState.currentPage % tabs.size]
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
                        onShowTabSheet = { showTabSheet = true },
                        onImagePickerClick = { showImagePicker = true },
                        onExportSheetClick = { showExportSheetSaver = true },
                        onExportPortraitClick = { showExportPortraitSaver = true },
                        settingsViewModel = settingsViewModel
                    )
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize()) {
                CharacterDetailMainContent(
                    paddingValues = paddingValues,
                    state = state,
                    pagerState = pagerState,
                    focusRequester = detailFocusRequester,
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
                    showImagePicker = { showImagePicker = true },
                    onShowTabSheet = { showTabSheet = true },
                    onExportSheetClick = { showExportSheetSaver = true },
                    onExportPortraitClick = { showExportPortraitSaver = true },
                    isDesktop = isDesktop,
                    onOpenDrawer = onOpenDrawer,
                    handleRestoration = handleRestoration,
                    desktopPagerState = desktopPagerState
                )

                if (!isDesktop) {
                    CharacterDetailDialogs(
                        state = state,
                        statsMap = state.statsMap,
                        forceBlurEnabled = forceBlurEnabled,
                        blurPopups = blurPopups,
                        allConditions = allConditions,
                        isDesktop = false,
                        spellbookManager = spellbookManager,
                        onSpellSettingsChange = { state.spellSettings = it },
                        hazeState = hazeState,
                        popupHazeState = popupHazeState,
                        onRoll = onRoll
                    )
                }
            }
        }
    }

    val isDesktopForSheet = when (state.interfaceMode) {
        AppInterfaceMode.MOBILE -> false
        AppInterfaceMode.DESKTOP -> true
        AppInterfaceMode.AUTO -> state.windowWidth >= Dimensions.DesktopSplitThreshold
    }

    TabSelectionSheet(
        showTabSheet = showTabSheet,
        onDismissRequest = { 
            showTabSheet = false
        },
        sheetState = sheetState,
        currentTab = if (isDesktopForSheet) desktopTabs[desktopPagerState.currentPage % desktopTabs.size] else tabs[pagerState.currentPage % tabs.size],
        tabs = if (isDesktopForSheet) desktopTabs else tabs,
        pagerState = if (isDesktopForSheet) desktopPagerState else pagerState,
        scope = scope,
        hazeState = if (isDesktopForSheet) null else (popupHazeState ?: hazeState),
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
    onImagePickerClick: () -> Unit,
    onExportSheetClick: () -> Unit,
    onExportPortraitClick: () -> Unit,
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
                onAvatarClick = { state.showAvatarMenu = !state.showAvatarMenu },
                onLevelClick = {
                    if (!state.isLevelPanelVisible) state.closeMajorOverlays()
                    state.isLevelPanelVisible = !state.isLevelPanelVisible
                },
                onOpenDrawer = onOpenDrawer,
                activeACValue = state.acValue,
                onACClick = { state.isShieldActive = !state.isShieldActive },
                onACLongClick = {
                    state.closeMajorOverlays()
                    if (state.useNewAC) {
                        state.showEnhancedAC = true
                    } else {
                        state.isArmorClassPanelVisible = true
                    }
                },
                isShieldActive = state.isShieldActive,
                activeInitValue = state.initValue,
                onInitClick = {
                    val baseInit = (state.initValue.replace("+", "").toIntOrNull() ?: 0) + (state.exhaustion * 2)
                    val activeEntry = state.initiativeEntries.find { it.id == state.activeInitiativeId }
                    val advantage = if (activeEntry?.hasAdvantage == true) AdvantageType.ADVANTAGE else AdvantageType.NONE
                    onRoll(DiceRoller.roll("Инициатива", baseInit, bonuses = activeEntry?.bonuses ?: emptyList(), stats = state.statsMap, exhaustion = state.exhaustion, sourceType = RollSourceType.ABILITY, advantageType = advantage, advantageLogic = state.advantageLogic))
                },
                onInitLongClick = {
                    state.closeMajorOverlays()
                    if (state.useNewInit) {
                        state.showEnhancedInit = true
                    } else {
                        state.isInitiativePanelVisible = true
                    }
                },
                currentHp = state.currentHp, maxHp = state.maxHp, tempHp = state.tempHp,
                healthColor = state.healthColor, healthIcon = state.healthIcon,
                onHealthClick = {
                    if (!state.isHealthPanelVisible) state.closeMajorOverlays()
                    state.isHealthPanelVisible = !state.isHealthPanelVisible
                },
                conditionsCount = state.exhaustion.toString(),
                selectedConditions = state.selectedConditions,
                onConditionsClick = {
                    state.closeMajorOverlays()
                    if (state.useNewCond) {
                        state.showEnhancedCond = true
                    } else {
                        state.isConditionsPanelVisible = true
                    }
                },
                activeSpeedValue = state.speedValue,
                onSpeedClick = {
                    state.closeMajorOverlays()
                    if (state.useNewSpeed) {
                        state.showEnhancedSpeed = true
                    } else {
                        state.isSpeedPanelVisible = true
                    }
                },
                showAvatarMenu = state.showAvatarMenu,
                onDismissAvatarMenu = { state.showAvatarMenu = false },
                onImagePickerClick = { onImagePickerClick(); state.showAvatarMenu = false },
                onExportSheetClick = { onExportSheetClick(); state.showAvatarMenu = false },
                onExportPortraitClick = { onExportPortraitClick(); state.showAvatarMenu = false },
                onDeletePortraitClick = { state.characterImageData = null; state.showAvatarMenu = false },
                onSettingsClick = { 
                    state.closeMajorOverlays()
                    state.showCharacterSettings = true; state.showAvatarMenu = false 
                },
                onNavigateBack = onNavigateBack,
                exhaustion = state.exhaustion,
                hasInspiration = state.hasInspiration,
                onInspirationChange = { state.hasInspiration = it },
                onShortRest = { 
                    if (!state.isRestPanelVisible) state.closeMajorOverlays()
                    state.isRestPanelVisible = !state.isRestPanelVisible 
                },
                onLongRest = { handleRestoration("long") },
                onDawn = { handleRestoration("dawn") },
                showRestPopup = state.showRestPopup,
                onShowRestPopupChange = { state.showRestPopup = it },
                hazeState = popupHazeState ?: hazeState,
                blurPopups = blurPopups,
                settingsViewModel = settingsViewModel
            )

            TabNavigationBar(
                currentTab = currentTab,
                onShowTabSheet = { onShowTabSheet() },
                actions = {
                    TabActions(
                        tab = currentTab,
                        state = state,
                        onShowSpellSettings = { state.closeMajorOverlays(); state.showSpellSettings = true },
                        isDesktop = false
                    )
                }
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
    showImagePicker: () -> Unit,
    onShowTabSheet: () -> Unit = {},
    onExportSheetClick: () -> Unit = {},
    onExportPortraitClick: () -> Unit = {},
    isDesktop: Boolean = false,
    onOpenDrawer: () -> Unit = {},
    handleRestoration: (String) -> Unit = {},
    desktopPagerState: PagerState
) {
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val keybinds by settingsViewModel?.keybinds?.collectAsState() ?: remember { mutableStateOf(emptyMap<KeybindAction, Key>()) }
    val colorScheme = MaterialTheme.colorScheme

    val desktopTabs = remember(tabs) { tabs.filter { it != CharacterTab.STATS } }
    
    val leftHaze = remember { HazeState() }
    val rightHaze = remember { HazeState() }
    val density = LocalDensity.current

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(Color.Transparent)
            .pointerInput(isDesktop) {
                if (!isDesktop) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.type == PointerEventType.Move || event.type == PointerEventType.Enter) {
                            val position = event.changes.first().position
                            val thresholdPx = with(density) { Dimensions.DesktopLeftColumnWidth.toPx() }
                            val newSection = if (position.x < thresholdPx) "left" else "right"
                            
                            if (state.activeSection != newSection) {
                                state.activeSection = newSection
                                rootFocusRequester.requestFocus()
                            }
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures {
                    focusManager.clearFocus()
                }
            }
    ) {
        if (isDesktop) {
            // LEFT COLUMN: Fixed Stats
            val isLeftActive = state.activeSection == "left"
            Box(
                modifier = Modifier
                    .width(Dimensions.DesktopLeftColumnWidth)
                    .fillMaxHeight()
                    .pointerInput(Unit) {
                        detectTapGestures {
                            state.activeSection = "left"
                            focusManager.clearFocus()
                        }
                    }
                    .background(if (isLeftActive) colorScheme.primary.copy(alpha = 0.02f) else Color.Transparent)
                    .run {
                        if (isLeftActive) this.then(Modifier.border(1.dp, colorScheme.primary.copy(alpha = 0.1f))) else this
                    }
            ) {
                Column(modifier = Modifier.fillMaxSize().hazeSource(state = leftHaze)) {
                    CharacterHeader(
                        name = state.name, onNameChange = { state.name = it },
                        level = state.level, experience = state.experience, nextLevelExp = state.nextLevelExp,
                        characterImageData = state.characterImageData,
                        characterUuid = state.characterUuid,
                        onAvatarClick = { 
                            state.showAvatarMenu = !state.showAvatarMenu 
                        },
                        onLevelClick = { 
                            if (!state.isLevelPanelVisible) state.closeMajorOverlays()
                            state.isLevelPanelVisible = !state.isLevelPanelVisible 
                        },
                        onOpenDrawer = onOpenDrawer,
                        activeACValue = state.acValue,
                        onACClick = { state.isShieldActive = !state.isShieldActive },
                        onACLongClick = { 
                            state.closeMajorOverlays()
                            if (state.useNewAC) state.showEnhancedAC = true else state.isArmorClassPanelVisible = true 
                        },
                        isShieldActive = state.isShieldActive,
                        activeInitValue = state.initValue,
                        onInitClick = {
                            val baseInit = (state.initValue.replace("+", "").toIntOrNull() ?: 0) + (state.exhaustion * 2)
                            val activeEntry = state.initiativeEntries.find { it.id == state.activeInitiativeId }
                            val advantage = if (activeEntry?.hasAdvantage == true) AdvantageType.ADVANTAGE else AdvantageType.NONE
                            onRoll(DiceRoller.roll("Инициатива", baseInit, bonuses = activeEntry?.bonuses ?: emptyList(), stats = state.statsMap, exhaustion = state.exhaustion, sourceType = RollSourceType.ABILITY, advantageType = advantage, advantageLogic = state.advantageLogic))
                        },
                        onInitLongClick = { 
                            state.closeMajorOverlays()
                            if (state.useNewInit) state.showEnhancedInit = true else state.isInitiativePanelVisible = true 
                        },
                        currentHp = state.currentHp, maxHp = state.maxHp, tempHp = state.tempHp,
                        healthColor = state.healthColor, healthIcon = state.healthIcon,
                        onHealthClick = { 
                            if (!state.isHealthPanelVisible) state.closeMajorOverlays()
                            state.isHealthPanelVisible = !state.isHealthPanelVisible 
                        },
                        conditionsCount = state.exhaustion.toString(),
                        selectedConditions = state.selectedConditions,
                        onConditionsClick = { 
                            state.closeMajorOverlays()
                            if (state.useNewCond) state.showEnhancedCond = true else state.isConditionsPanelVisible = true 
                        },
                        activeSpeedValue = state.speedValue,
                        onSpeedClick = { 
                            state.closeMajorOverlays()
                            if (state.useNewSpeed) state.showEnhancedSpeed = true else state.isSpeedPanelVisible = true 
                        },
                        showAvatarMenu = state.showAvatarMenu,
                        onDismissAvatarMenu = { state.showAvatarMenu = false },
                        onImagePickerClick = { showImagePicker(); state.showAvatarMenu = false },
                        onExportSheetClick = { onExportSheetClick(); state.showAvatarMenu = false },
                        onExportPortraitClick = { onExportPortraitClick(); state.showAvatarMenu = false },
                        onDeletePortraitClick = { state.characterImageData = null; state.showAvatarMenu = false },
                        onSettingsClick = { 
                            state.closeMajorOverlays()
                            state.showCharacterSettings = true; state.showAvatarMenu = false 
                        },
                        onNavigateBack = onNavigateBack,
                        exhaustion = state.exhaustion,
                        hasInspiration = state.hasInspiration,
                        onInspirationChange = { state.hasInspiration = it },
                        onShortRest = { 
                            if (!state.isRestPanelVisible) state.closeMajorOverlays()
                            state.isRestPanelVisible = !state.isRestPanelVisible 
                        },
                        onLongRest = { handleRestoration("long") },
                        onDawn = { handleRestoration("dawn") },
                        showRestPopup = state.showRestPopup,
                        onShowRestPopupChange = { 
                            state.showRestPopup = it 
                        },
                        hazeState = leftHaze,
                        blurPopups = blurPopups,
                        settingsViewModel = settingsViewModel
                    )

                    if (state.isAnyPanelVisible && !isDesktop) {
                        CharacterDetailExpandingPanels(
                            state = state,
                            colorScheme = colorScheme,
                            isOled = colorScheme.background == Color.Black,
                            handleRestoration = handleRestoration
                        )
                    }

                    StatsTab(
                        character = character,
                        level = state.level,
                        statsState = state.statsState,
                        onStatsStateChange = { state.statsState = it },
                        onRoll = onRoll,
                        hazeState = leftHaze,
                        popupHazeState = null,
                        forceBlurEnabled = forceBlurEnabled,
                        blurPopups = blurPopups,
                        isAdvancedMode = state.isAdvancedMode,
                        attributeModifiers = state.attributeModifiers,
                        statsMap = state.statsMap,
                        onBonusConfigOpenChange = { 
                            if (it) state.closeMajorOverlays()
                            state.isBonusConfigOpen = it 
                        },
                        advantageLogic = state.advantageLogic,
                        state = state,
                        isDesktop = true
                    )
                }
            }

            VerticalDivider(modifier = Modifier.fillMaxHeight(), color = colorScheme.outlineVariant)
        }

        // RIGHT COLUMN (or full width on mobile)
        val isRightActive = !isDesktop || state.activeSection == "right"
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(if (isDesktop && isRightActive) colorScheme.primary.copy(alpha = 0.02f) else Color.Transparent)
                .run {
                    if (isDesktop && isRightActive) this.then(Modifier.border(1.dp, colorScheme.primary.copy(alpha = 0.1f))) else this
                }
        ) {
            val currentPagerState = if (isDesktop) desktopPagerState else pagerState
            val currentTabs = if (isDesktop) desktopTabs else tabs
            val currentTab = currentTabs[currentPagerState.currentPage % currentTabs.size]

            Column(modifier = Modifier.fillMaxSize().hazeSource(state = rightHaze)) {
                if (isDesktop) {
                    TabNavigationBar(
                        currentTab = currentTab,
                        onShowTabSheet = onShowTabSheet,
                        actions = {
                            TabActions(
                                tab = currentTab,
                                state = state,
                                onShowSpellSettings = { state.closeMajorOverlays(); state.showSpellSettings = true },
                                isDesktop = true
                            )
                        }
                    )
                }

                HorizontalPager(
                    state = currentPagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1
                ) { page ->
                    val tab = currentTabs[page % currentTabs.size]
                    TabContent(
                        tab = tab,
                        state = state,
                        character = character,
                        onRoll = onRoll,
                        pb = pb,
                        hazeState = if (isDesktop) rightHaze else hazeState,
                        popupHazeState = if (isDesktop) null else popupHazeState,
                        forceBlurEnabled = forceBlurEnabled,
                        blurPopups = blurPopups,
                        settingsViewModel = settingsViewModel,
                        spellbookManager = spellbookManager,
                        allConditions = allConditions,
                        onFullscreenDialogOpenChange = onFullscreenDialogOpenChange,
                        showImagePicker = showImagePicker,
                        isDesktop = isDesktop
                    )
                }
            }

            // Desktop-specific overlays for the right column
            if (isDesktop) {
                val isOverlayVisible = state.showSpellSettings || state.isAttackConfigOpen || 
                            state.isSpellEditorOpen || state.isMagicBonusSettingsOpen ||
                            state.isFullscreenDynamicFieldOpen || state.isWalletDialogOpen || 
                            state.isSpellbookSelectionOpen || state.isResourceConfigOpen ||
                            state.showEnhancedAC || state.showEnhancedInit || state.showEnhancedSpeed || 
                            state.showEnhancedCond || state.showCharacterSettings || state.showHealthSettings ||
                            state.isBonusConfigOpen || state.showHpDialog ||
                            state.isLevelPanelVisible || state.isHealthPanelVisible || 
                            state.isRestPanelVisible || state.isArmorClassPanelVisible || 
                            state.isInitiativePanelVisible || state.isConditionsPanelVisible || 
                            state.isSpeedPanelVisible

                SectionOverlay(
                    visible = isOverlayVisible,
                    onDismiss = { state.closeMajorOverlays() },
                    hazeState = rightHaze,
                    fullSize = true
                ) {
                    CharacterDetailDialogs(
                        state = state,
                        statsMap = state.statsMap,
                        forceBlurEnabled = forceBlurEnabled,
                        blurPopups = blurPopups,
                        allConditions = allConditions,
                        isDesktop = true,
                        targetSection = "right",
                        spellbookManager = spellbookManager,
                        onSpellSettingsChange = { state.spellSettings = it },
                        hazeState = rightHaze,
                        popupHazeState = null, // Set to null to avoid loop with global source
                        onRoll = onRoll
                    )
                }
            }
        }
    }
}



@Composable
fun TabContent(
    tab: CharacterTab,
    state: CharacterDetailState,
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
    onFullscreenDialogOpenChange: (Boolean) -> Unit,
    showImagePicker: () -> Unit,
    isDesktop: Boolean = false
) {
    when (tab) {
        CharacterTab.STATS -> {
            StatsTab(
                character = character,
                level = state.level,
                statsState = state.statsState,
                onStatsStateChange = { state.statsState = it },
                onRoll = onRoll,
                hazeState = hazeState,
                popupHazeState = popupHazeState,
                forceBlurEnabled = forceBlurEnabled,
                blurPopups = blurPopups,
                isAdvancedMode = state.isAdvancedMode,
                attributeModifiers = state.attributeModifiers,
                statsMap = state.statsMap,
                onBonusConfigOpenChange = { if (it) state.closeMajorOverlays(); state.isBonusConfigOpen = it },
                advantageLogic = state.advantageLogic,
                state = state,
                isDesktop = isDesktop
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
                hazeState = hazeState,
                popupHazeState = popupHazeState,
                forceBlurEnabled = forceBlurEnabled,
                blurPopups = blurPopups,
                isEditMode = state.isEditMode,
                onToggleEditMode = { state.isEditMode = !state.isEditMode },
                settingsViewModel = settingsViewModel,
                spellSettings = state.spellSettings,
                advantageLogic = state.advantageLogic,
                onAttackConfigOpenChange = { if (it) state.closeMajorOverlays(); state.isAttackConfigOpen = it },
                state = state
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
                hazeState = hazeState,
                popupHazeState = popupHazeState,
                forceBlurEnabled = forceBlurEnabled,
                blurPopups = blurPopups,
                isEditMode = state.isEditMode,
                onToggleEditMode = { state.isEditMode = !state.isEditMode },
                onToggleAllExpansion = {
                    val currentList = state.bioLongSections
                    val anyCollapsed = currentList.any { !it.isExpanded }
                    state.bioLongSections = currentList.map { it.copy(isExpanded = anyCollapsed) }
                },
                anyCollapsed = state.bioLongSections.any { !it.isExpanded },
                settingsViewModel = settingsViewModel,
                statsMap = state.statsMap,
                onFullscreenDialogOpenChange = onFullscreenDialogOpenChange,
                onFullscreenVisibilityChanged = { if (it) state.closeMajorOverlays(); state.isFullscreenDynamicFieldOpen = it },
                state = state
            )
        }
        CharacterTab.SKILLS_FEATS -> {
            SkillsFeatsTab(
                skillsAndTraits = state.skillsAndTraits,
                onSkillsAndTraitsChange = { state.skillsAndTraits = it },
                hazeState = hazeState,
                popupHazeState = popupHazeState,
                forceBlurEnabled = forceBlurEnabled,
                blurPopups = blurPopups,
                isEditMode = state.isEditMode,
                onToggleEditMode = { state.isEditMode = !state.isEditMode },
                onToggleAllExpansion = {
                    val currentList = state.skillsAndTraits
                    val anyCollapsed = currentList.any { !it.isExpanded }
                    state.skillsAndTraits = currentList.map { it.copy(isExpanded = anyCollapsed) }
                },
                anyCollapsed = state.skillsAndTraits.any { !it.isExpanded },
                settingsViewModel = settingsViewModel,
                statsMap = state.statsMap,
                onFullscreenDialogOpenChange = onFullscreenDialogOpenChange,
                onFullscreenVisibilityChanged = { if (it) state.closeMajorOverlays(); state.isFullscreenDynamicFieldOpen = it },
                state = state
            )
        }
        CharacterTab.INVENTORY -> {
            InventoryTab(
                inventory = state.inventory,
                onInventoryChange = { state.inventory = it },
                wallet = state.wallet,
                onWalletChange = { state.wallet = it },
                hazeState = hazeState,
                popupHazeState = popupHazeState,
                forceBlurEnabled = forceBlurEnabled,
                blurPopups = blurPopups,
                isEditMode = state.isEditMode,
                onToggleEditMode = { state.isEditMode = !state.isEditMode },
                onToggleAllExpansion = {
                    val currentList = state.inventory
                    val anyCollapsed = currentList.any { !it.isExpanded }
                    state.inventory = currentList.map { it.copy(isExpanded = anyCollapsed) }
                },
                anyCollapsed = state.inventory.any { !it.isExpanded },
                settingsViewModel = settingsViewModel,
                statsMap = state.statsMap,
                onFullscreenDialogOpenChange = onFullscreenDialogOpenChange,
                onFullscreenVisibilityChanged = { if (it) state.closeMajorOverlays(); state.isFullscreenDynamicFieldOpen = it },
                onWalletDialogOpenChange = { if (it) state.closeMajorOverlays(); state.isWalletDialogOpen = it },
                state = state
            )
        }
        CharacterTab.SPELLS -> {
            SpellsTab(
                spells = state.spells,
                onSpellsChange = { state.spells = it },
                characterLevel = state.level.toIntOrNull() ?: 1,
                spellSettings = state.spellSettings,
                onSpellSettingsChange = { state.spellSettings = it },
                hazeState = hazeState,
                popupHazeState = popupHazeState,
                forceBlurEnabled = forceBlurEnabled,
                blurPopups = blurPopups,
                isEditMode = state.isEditMode,
                onToggleEditMode = { state.isEditMode = !state.isEditMode },
                onToggleAllExpansion = {
                    val currentList = state.spells
                    val anyCollapsed = currentList.any { !it.isExpanded }
                    state.spells = currentList.map { it.copy(isExpanded = anyCollapsed) }
                },
                anyCollapsed = state.spells.any { !it.isExpanded },
                onShowSpellSettings = { state.closeMajorOverlays(); state.showSpellSettings = true },
                settingsViewModel = settingsViewModel,
                onRoll = onRoll,
                statsMap = state.statsMap,
                exhaustion = state.exhaustion,
                advantageLogic = state.advantageLogic,
                spellbookManager = spellbookManager,
                onSpellEditorOpenChange = { if (it) state.closeMajorOverlays(); state.isSpellEditorOpen = it },
                onMagicBonusSettingsOpenChange = { if (it) state.closeMajorOverlays(); state.isMagicBonusSettingsOpen = it },
                onFullscreenDialogOpenChange = onFullscreenDialogOpenChange,
                onFullscreenVisibilityChanged = { if (it) state.closeMajorOverlays(); state.isFullscreenDynamicFieldOpen = it },
                onSpellbookSelectionOpenChange = { if (it) state.closeMajorOverlays(); state.isSpellbookSelectionOpen = it },
                state = state
            )
        }
        CharacterTab.NOTES -> {
            NotesTab(
                notes = state.notes,
                onNotesChange = { state.notes = it },
                hazeState = hazeState,
                popupHazeState = popupHazeState,
                forceBlurEnabled = forceBlurEnabled,
                blurPopups = blurPopups,
                isEditMode = state.isEditMode,
                onToggleEditMode = { state.isEditMode = !state.isEditMode },
                onToggleAllExpansion = {
                    val currentList = state.notes
                    val anyCollapsed = currentList.any { !it.isExpanded }
                    state.notes = currentList.map { it.copy(isExpanded = anyCollapsed) }
                },
                anyCollapsed = state.notes.any { !it.isExpanded },
                settingsViewModel = settingsViewModel,
                statsMap = state.statsMap,
                onFullscreenDialogOpenChange = onFullscreenDialogOpenChange,
                onFullscreenVisibilityChanged = { if (it) state.closeMajorOverlays(); state.isFullscreenDynamicFieldOpen = it },
                state = state
            )
        }
    }
}



