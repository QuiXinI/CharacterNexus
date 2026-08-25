package ru.quasaris.characternexus

import ru.quasaris.characternexus.model.Character
import ru.quasaris.characternexus.model.CharacterSummary
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.model.AppThemeMode
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.savedstate.read
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.quasaris.characternexus.ui.*
import ru.quasaris.characternexus.util.*
import ru.quasaris.characternexus.backend.*
import ru.quasaris.characternexus.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(
    initialLastCharacterId: Int,
    initialLastCharacterSeedColor: Int?,
    settingsViewModel: SettingsViewModel,
    characterRepository: CharacterRepository,
    spellbookManager: SpellbookManager?,
    moduleManager: ModuleManager?,
    glossaryImporter: GlossaryImporter?,
    getScaleFactor: @Composable () -> Float,
    getRollHistorySize: @Composable () -> Int,
    getCustomRollHistorySize: @Composable () -> Int,
    getMasterBlurEnabled: @Composable () -> Boolean,
    getBlurRolls: @Composable () -> Boolean,
    getBlurFullscreen: @Composable () -> Boolean,
    getBlurPopups: @Composable () -> Boolean,
    getRollInterfaceAlpha: @Composable () -> Float,
    getRollPassThrough: @Composable () -> Boolean,
    getRollPosition: @Composable () -> DiceRollPosition,
    getRollCloseButtonPosition: @Composable () -> DiceRollPosition,
    loadCharacters: () -> List<CharacterSummary>,
    getFullCharacter: suspend (String) -> Character?,
    updateCharacter: (Character) -> Unit,
    deleteCharacter: (String) -> Unit,
    onCharacterIdChange: (Int) -> Unit,
    onSeedColorChange: (Int?) -> Unit
) {
    val scaleFactor = getScaleFactor()
    val sliderHistorySize = getRollHistorySize()
    val customHistorySize = getCustomRollHistorySize()
    val historyLimit = if (sliderHistorySize >= 10) customHistorySize else sliderHistorySize

    val themeMode by settingsViewModel.themeMode.collectAsState()
    val themeBehavior by settingsViewModel.themeBehavior.collectAsState()
    val m3SeedColor by settingsViewModel.m3SeedColor.collectAsState()
    var lastCharacterId by remember { mutableIntStateOf(initialLastCharacterId) }

    val masterBlurEnabled = getMasterBlurEnabled()
    val blurRolls = getBlurRolls()
    val blurFullscreen = getBlurFullscreen()
    val blurPopups = getBlurPopups()
    val blurRadiusVal by settingsViewModel.blurRadius.collectAsState()
    val customBlurRadiusVal by settingsViewModel.customBlurRadius.collectAsState()
    val rollAlpha = getRollInterfaceAlpha()

    val effectiveBlurRolls = masterBlurEnabled && blurRolls
    val effectiveBlurFullscreen = masterBlurEnabled && blurFullscreen
    val effectiveBlurPopups = masterBlurEnabled && blurPopups
    val targetBlurRadius = if (blurRadiusVal >= 48) customBlurRadiusVal else blurRadiusVal

    val rollPassThrough = getRollPassThrough()
    val rollPosition = getRollPosition()
    val rollCloseButtonPos = getRollCloseButtonPosition()

    val diceFabEnabled by settingsViewModel.diceFabEnabled.collectAsState()
    val diceFabOffsetX by settingsViewModel.diceFabOffsetX.collectAsState()
    val diceFabOffsetY by settingsViewModel.diceFabOffsetY.collectAsState()
    val diceFabAlpha by settingsViewModel.diceFabAlpha.collectAsState()
    val diceFabBlurEnabled by settingsViewModel.diceFabBlurEnabled.collectAsState()

    val hazeState = remember { HazeState() }
    val overlayHazeState = remember { HazeState() }

    val characters: SnapshotStateList<CharacterSummary> = remember {
        mutableStateListOf<CharacterSummary>().apply {
            addAll(loadCharacters())
        }
    }

    var rollHistory by remember { mutableStateOf(listOf<RollResult>()) }

    val lastCharacter = characters.find { it.id == lastCharacterId }
    val avatarColor = lastCharacter?.themeSeedColorArgb ?: initialLastCharacterSeedColor

    AppScaleProvider(scaleFactor = scaleFactor) {
        quasarisTheme(
            themeBehavior = themeBehavior,
            themeMode = themeMode,
            avatarColor = avatarColor,
            m3SeedColor = m3SeedColor
        ) {
            val colorScheme = MaterialTheme.colorScheme
            val isOled = colorScheme.background == Color.Black

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .onPreviewKeyEvent {
                        if (it.type == KeyEventType.KeyDown && it.key == Key.Tab) {
                            GlobalActionRegistry.toggleDrawer()
                            true
                        } else {
                            false
                        }
                    },
                color = colorScheme.background,
                contentColor = colorScheme.onBackground
            ) {
                val navController = rememberNavController()
                val focusManager = LocalFocusManager.current

                // Global back navigation registration
                val canPop = navController.previousBackStackEntry != null
                BackHandler(enabled = canPop) {
                    navController.popBackStack()
                }

                // Clear focus when destination changes
                LaunchedEffect(navController) {
                    navController.currentBackStackEntryFlow.collect {
                        focusManager.clearFocus()
                    }
                }

                var fullscreenDialogCount by remember { mutableIntStateOf(0) }
                val isFullscreenDialogOpen = fullscreenDialogCount > 0
                val onFullscreenDialogOpenChange: (Boolean) -> Unit = remember {
                    { opened ->
                        if (opened) fullscreenDialogCount++
                        else fullscreenDialogCount = maxOf(0, fullscreenDialogCount - 1)
                    }
                }
                val blurRadius by animateDpAsState(
                    targetValue = if (isFullscreenDialogOpen && effectiveBlurFullscreen && !isOled) targetBlurRadius.dp else 0.dp,
                    animationSpec = tween(durationMillis = 300)
                )

                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()

                // Register global drawer toggle
                LaunchedEffect(drawerState) {
                    GlobalActionRegistry.onToggleDrawer = {
                        scope.launch {
                            if (drawerState.isOpen) drawerState.close() else drawerState.open()
                        }
                    }
                }

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val animDuration = 550
                val navHostOffsetSpec = tween<IntOffset>(durationMillis = animDuration, easing = FastOutSlowInEasing)

                Box(modifier = Modifier.fillMaxSize()) {
                    // Layer 1: Global Background Source (for cards)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .hazeSource(state = hazeState)
                            .blur(blurRadius)
                    )

                    // Layer 2: Main UI Source (for popups, FAB and dragged items)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .hazeSource(state = overlayHazeState)
                            .blur(blurRadius)
                    ) {
                        ModalNavigationDrawer(
                            drawerState = drawerState,
                            gesturesEnabled = true, // Enabled globally
                            drawerContent = {
                                ModalDrawerSheet {
                                    Spacer(Modifier.height(12.dp))
                                    NavigationDrawerItem(
                                        label = { Text("Главный экран") },
                                        selected = currentRoute == "menu",
                                        onClick = {
                                            scope.launch { drawerState.close() }
                                            if (currentRoute != "menu") {
                                                NavigationPathManager.clear()
                                                navController.navigate("menu") {
                                                    popUpTo("menu") { inclusive = true }
                                                }
                                            }
                                        },
                                        icon = { Icon(Icons.Default.Person, null) },
                                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                    )

                                    if (lastCharacterId != -1) {
                                        NavigationDrawerItem(
                                            label = { Text("Последний персонаж") },
                                            selected = false,
                                            onClick = {
                                                scope.launch { drawerState.close() }
                                                val lastChar = characters.find { it.id == lastCharacterId }
                                                if (lastChar != null) {
                                                    navController.navigate("edit/${lastChar.uuid}")
                                                }
                                            },
                                            icon = { Icon(Icons.Default.History, null) },
                                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                        )
                                    }

                                    NavigationDrawerItem(
                                        label = { Text("Настройки") },
                                        selected = currentRoute == "settings",
                                        onClick = {
                                            scope.launch { drawerState.close() }
                                            if (currentRoute != "settings") {
                                                NavigationPathManager.clear()
                                                navController.navigate("settings")
                                            }
                                        },
                                        icon = { Icon(Icons.Default.Settings, null) },
                                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                    )

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 28.dp))

                                    NavigationDrawerItem(
                                        label = { Text("Справочник формул") },
                                        selected = currentRoute == "formula_info",
                                        onClick = {
                                            scope.launch { drawerState.close() }
                                            if (currentRoute != "formula_info") {
                                                navController.navigate("formula_info")
                                            }
                                        },
                                        icon = { Icon(Icons.Default.Functions, null) },
                                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                    )

                                    // GLOSSARY with Hierarchy
                                    NavigationDrawerItem(
                                        label = { Text("Глоссарий") },
                                        selected = currentRoute == "glossary",
                                        onClick = {
                                            scope.launch { drawerState.close() }
                                            NavigationPathManager.clear()
                                            navController.navigate("glossary") {
                                                popUpTo("glossary") { inclusive = true }
                                            }
                                        },
                                        icon = { Icon(Icons.Default.HistoryEdu, null) },
                                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                    )
                                    
                                    if (NavigationPathManager.currentSection == "glossary") {
                                        val fullPath = NavigationPathManager.path
                                        fullPath.dropLast(1).forEach { node ->
                                            if (node.level == 0 && (node.id == "hub" || node.id == "modules")) return@forEach
                                            
                                            NavigationDrawerItem(
                                                label = { Text(node.label, fontWeight = FontWeight.Normal) },
                                                selected = false,
                                                onClick = {
                                                    scope.launch { drawerState.close() }
                                                    node.onClick?.invoke()
                                                },
                                                modifier = Modifier
                                                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                                                    .padding(start = (node.level * 16).dp)
                                            )
                                        }
                                    }

                                    // MODULES with Hierarchy
                                    NavigationDrawerItem(
                                        label = { Text("Модули") },
                                        selected = currentRoute == "modules",
                                        onClick = {
                                            scope.launch { drawerState.close() }
                                            NavigationPathManager.clear()
                                            navController.navigate("modules") {
                                                popUpTo("modules") { inclusive = true }
                                            }
                                        },
                                        icon = { Icon(Icons.Default.Extension, null) },
                                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                    )

                                    if (NavigationPathManager.currentSection == "modules") {
                                        val fullPath = NavigationPathManager.path
                                        fullPath.dropLast(1).forEach { node ->
                                            if (node.level == 0 && (node.id == "modules" || node.id == "hub")) return@forEach

                                            NavigationDrawerItem(
                                                label = { Text(node.label, fontWeight = FontWeight.Normal) },
                                                selected = false,
                                                onClick = {
                                                    scope.launch { drawerState.close() }
                                                    node.onClick?.invoke()
                                                },
                                                modifier = Modifier
                                                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                                                    .padding(start = (node.level * 16).dp)
                                            )
                                        }
                                    }
                                }
                            }
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = Color.Transparent,
                            ) {
                                NavHost(
                                    navController = navController,
                                    startDestination = "menu",
                                    enterTransition = {
                                        slideInHorizontally(initialOffsetX = { it }, animationSpec = navHostOffsetSpec)
                                    },
                                    exitTransition = {
                                        slideOutHorizontally(targetOffsetX = { -it }, animationSpec = navHostOffsetSpec)
                                    },
                                    popEnterTransition = {
                                        slideInHorizontally(initialOffsetX = { -it }, animationSpec = navHostOffsetSpec)
                                    },
                                    popExitTransition = {
                                        slideOutHorizontally(targetOffsetX = { it }, animationSpec = navHostOffsetSpec)
                                    }
                                ) {
                                    composable("menu") {
                                        MenuWindow(
                                            characters = characters,
                                            onNavigateToCreate = { navController.navigate("create_setup") },
                                            onCharacterClick = { characterUuid ->
                                                val char = characters.find { it.uuid == characterUuid }
                                                if (char != null) {
                                                    lastCharacterId = char.id
                                                    onCharacterIdChange(char.id)
                                                    onSeedColorChange(char.themeSeedColorArgb)
                                                    navController.navigate("edit/${char.uuid}")
                                                }
                                            },
                                            onImportCharacter = { importedCharacter ->
                                                updateCharacter(importedCharacter)
                                                characters.clear()
                                                characters.addAll(loadCharacters())
                                            },
                                            onDeleteCharacters = { uuidsToDelete ->
                                                uuidsToDelete.forEach { deleteCharacter(it) }
                                                characters.removeAll { it.uuid in uuidsToDelete }
                                                if (characters.none { it.id == lastCharacterId }) {
                                                    lastCharacterId = -1
                                                    onCharacterIdChange(-1)
                                                }
                                            },
                                            onReorderCharacters = { orderedUuids ->
                                                characterRepository.updateSummariesOrder(orderedUuids)
                                                characters.clear()
                                                characters.addAll(loadCharacters())
                                            },
                                            getFullCharacter = getFullCharacter,
                                            onOpenDrawer = { scope.launch { drawerState.open() } },
                                            onFullscreenDialogOpenChange = onFullscreenDialogOpenChange,
                                            settingsViewModel = settingsViewModel,
                                            hazeState = hazeState,
                                            popupHazeState = overlayHazeState,
                                            forceBlurEnabled = effectiveBlurFullscreen,
                                            blurPopups = effectiveBlurPopups
                                        )
                                    }

                                    composable("settings") {
                                        SettingsWindow(
                                            onOpenDrawer = { scope.launch { drawerState.open() } },
                                            settingsViewModel = settingsViewModel,
                                            hazeState = hazeState,
                                            onFullscreenDialogOpenChange = onFullscreenDialogOpenChange
                                        )
                                    }

                                    composable("formula_info") {
                                        FormulaInfoWindow(
                                            onNavigateBack = { navController.popBackStack() }
                                        )
                                    }

                                    composable("spellbook") {
                                        if (spellbookManager != null && glossaryImporter != null) {
                                            SpellbookWindow(
                                                spellbookManager = spellbookManager,
                                                glossaryImporter = glossaryImporter,
                                                onOpenDrawer = { scope.launch { drawerState.open() } },
                                                onFullscreenDialogOpenChange = onFullscreenDialogOpenChange,
                                                forceBlurEnabled = effectiveBlurFullscreen,
                                                settingsViewModel = settingsViewModel
                                            )
                                        }
                                    }

                                    composable("glossary") {
                                        if (spellbookManager != null && moduleManager != null) {
                                            GlossaryWindow(
                                                spellbookManager = spellbookManager,
                                                moduleManager = moduleManager,
                                                onOpenDrawer = { scope.launch { drawerState.open() } },
                                                onFullscreenDialogOpenChange = onFullscreenDialogOpenChange,
                                                forceBlurEnabled = effectiveBlurFullscreen,
                                                settingsViewModel = settingsViewModel
                                            )
                                        }
                                    }

                                    composable("modules") {
                                        if (moduleManager != null && glossaryImporter != null && spellbookManager != null) {
                                            ModulesWindow(
                                                moduleManager = moduleManager,
                                                spellbookManager = spellbookManager,
                                                glossaryImporter = glossaryImporter,
                                                onOpenDrawer = { scope.launch { drawerState.open() } },
                                                onFullscreenDialogOpenChange = onFullscreenDialogOpenChange,
                                                forceBlurEnabled = effectiveBlurFullscreen,
                                                settingsViewModel = settingsViewModel
                                            )
                                        }
                                    }

                                    composable("create_setup") {
                                        CharacterCreationWindow(
                                            onNavigateBack = { navController.popBackStack() },
                                            onCharacterCreate = { newChar ->
                                                updateCharacter(newChar)
                                                characters.clear()
                                                characters.addAll(loadCharacters())
                                                lastCharacterId = newChar.id
                                                onCharacterIdChange(newChar.id)
                                                onSeedColorChange(newChar.themeSeedColorArgb)
                                                navController.navigate("edit/${newChar.uuid}") {
                                                    popUpTo("menu")
                                                }
                                            },
                                            onFullscreenDialogOpenChange = onFullscreenDialogOpenChange,
                                            hazeState = hazeState,
                                            popupHazeState = overlayHazeState,
                                            forceBlurEnabled = effectiveBlurFullscreen,
                                            blurPopups = effectiveBlurPopups
                                        )
                                    }

                                    composable(
                                        route = "edit/{characterUuid}",
                                        arguments = listOf(navArgument("characterUuid") { type = NavType.StringType })
                                    ) { backStackEntry ->
                                        val characterUuid = backStackEntry.arguments?.read { getString("characterUuid") }
                                        var character by remember { mutableStateOf<Character?>(null) }

                                        LaunchedEffect(characterUuid) {
                                            if (characterUuid != null) {
                                                character = getFullCharacter(characterUuid)
                                            }
                                        }

                                        CharacterDetailWindow(
                                            character = character,
                                            onNavigateBack = {
                                                navController.popBackStack()
                                            },
                                            onOpenDrawer = { scope.launch { drawerState.open() } },
                                            onDeleteCharacter = { charToDelete ->
                                                deleteCharacter(charToDelete.uuid)
                                                characters.removeAll { it.uuid == charToDelete.uuid }
                                                if (lastCharacterId == charToDelete.id) {
                                                    lastCharacterId = -1
                                                    onCharacterIdChange(-1)
                                                }
                                                navController.popBackStack()
                                            },
                                            onSaveChanges = { updatedCharacter ->
                                                updateCharacter(updatedCharacter)
                                                val index = characters.indexOfFirst { it.uuid == updatedCharacter.uuid }
                                                if (index != -1) {
                                                    val newSummary = updatedCharacter.toSummary()
                                                    if (characters[index] != newSummary) {
                                                        characters[index] = newSummary
                                                    }
                                                    if (updatedCharacter.id == lastCharacterId) {
                                                        onSeedColorChange(updatedCharacter.themeSeedColorArgb)
                                                    }
                                                }
                                            },
                                            onRoll = { res ->
                                                val hapticType = when {
                                                    res.isCriticalSuccess -> HapticType.SUCCESS
                                                    res.isCriticalFailure -> HapticType.ERROR
                                                    else -> HapticType.CLICK
                                                }
                                                PlatformUtils.performHapticFeedback(hapticType)
                                                rollHistory = (listOf(res) + rollHistory).take(maxOf(1, historyLimit))
                                            },
                                            onFullscreenDialogOpenChange = onFullscreenDialogOpenChange,
                                            hazeState = hazeState,
                                            popupHazeState = overlayHazeState,
                                            forceBlurEnabled = effectiveBlurFullscreen,
                                            blurPopups = effectiveBlurPopups,
                                            settingsViewModel = settingsViewModel,
                                            spellbookManager = spellbookManager
                                        )
                                    }
                                }
                            }
                        }
                    }

                    val density = LocalDensity.current
                    val isKeyboardVisible = WindowInsets.ime.getBottom(density) > 0
                    val isOnCharacterScreen = currentRoute?.startsWith("edit/") == true

                    if (!isKeyboardVisible && diceFabEnabled && isOnCharacterScreen && !isFullscreenDialogOpen) {
                        DiceRollerFab(
                            onRoll = { pool: Map<Int, Int> ->
                                val res = DiceRoller.rollPool(pool)
                                val hapticType = when {
                                    res.isCriticalSuccess -> HapticType.SUCCESS
                                    res.isCriticalFailure -> HapticType.ERROR
                                    else -> HapticType.CLICK
                                }
                                PlatformUtils.performHapticFeedback(hapticType)
                                rollHistory = (listOf(res) + rollHistory).take(maxOf(1, historyLimit))
                            },
                            hazeState = overlayHazeState,
                            modifier = Modifier.align(Alignment.BottomEnd),
                            isOled = isOled,
                            alpha = diceFabAlpha,
                            forceBlurEnabled = masterBlurEnabled && diceFabBlurEnabled,
                            initialOffsetX = diceFabOffsetX * density.density,
                            initialOffsetY = diceFabOffsetY * density.density,
                            onPositionChange = { x, y ->
                                settingsViewModel.updateDiceFabPosition(
                                    x / density.density,
                                    y / density.density
                                )
                            }
                        )
                    }

                    if (rollHistory.isNotEmpty() && isOnCharacterScreen && !isKeyboardVisible && !isFullscreenDialogOpen) {
                        DiceRollOverlay(
                            history = rollHistory,
                            onClose = { rollHistory = emptyList() },
                            themeMode = themeMode,
                            forceBlurEnabled = effectiveBlurRolls,
                            hazeState = overlayHazeState,
                            alpha = if (effectiveBlurRolls) 0f else rollAlpha,
                            isPassThrough = rollPassThrough,
                            position = rollPosition,
                            closeButtonPosition = rollCloseButtonPos,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .navigationBarsPadding()
                        )
                    }
                }
            }
        }
    }
}
