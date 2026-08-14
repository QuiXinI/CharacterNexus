package ru.quasaris.characternexus

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.savedstate.read
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.quasaris.characternexus.backend.CharacterRepository
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.ui.*
import ru.quasaris.characternexus.util.PlatformUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(
    initialThemeMode: String,
    initialLastCharacterId: Int,
    initialLastCharacterSeedColor: Int?,
    settingsViewModel: Any?, // TODO: Use actual type when migrated
    characterRepository: CharacterRepository,
    spellbookManager: Any?,
    moduleManager: Any?,
    glossaryImporter: Any?,
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
    onCharacterIdChange: (Int) -> Unit,
    onSeedColorChange: (Int?) -> Unit
) {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components {
                add(KtorNetworkFetcherFactory())
            }
            .crossfade(true)
            .build()
    }

    val scaleFactor = getScaleFactor()
    val sliderHistorySize = getRollHistorySize()
    val customHistorySize = getCustomRollHistorySize()
    val historyLimit = if (sliderHistorySize >= 10) customHistorySize else sliderHistorySize

    var themeMode by remember { mutableStateOf(initialThemeMode) }
    var lastCharacterId by remember { mutableIntStateOf(initialLastCharacterId) }

    val masterBlurEnabled = getMasterBlurEnabled()
    val blurRolls = getBlurRolls()
    val blurFullscreen = getBlurFullscreen()
    val blurPopups = getBlurPopups()
    val rollAlpha = getRollInterfaceAlpha()

    val effectiveBlurRolls = masterBlurEnabled && blurRolls
    val effectiveBlurFullscreen = masterBlurEnabled && blurFullscreen
    val effectiveBlurPopups = masterBlurEnabled && blurPopups

    val rollPassThrough = getRollPassThrough()
    val rollPosition = getRollPosition()
    val rollCloseButtonPos = getRollCloseButtonPosition()

    val hazeState = remember { HazeState() }
    val overlayHazeState = remember { HazeState() }

    val characters = remember { mutableStateListOf<CharacterSummary>() }
    val isInitialized by characterRepository.isInitialized.collectAsState()
    
    if (!isInitialized) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LaunchedEffect(isInitialized) {
        if (isInitialized) {
            characters.clear()
            characters.addAll(characterRepository.loadCharacters())
        }
    }
    
    // Also sync with repository updates
    LaunchedEffect(Unit) {
        characterRepository.charactersSummaryState.collectLatest { newSummaries ->
            characters.clear()
            characters.addAll(newSummaries)
        }
    }

    var rollHistory by remember { mutableStateOf(listOf<RollResult>()) }

    val lastCharacter = characters.find { it.id == lastCharacterId }
    val avatarColor = lastCharacter?.themeSeedColorArgb ?: initialLastCharacterSeedColor

    AppScaleProvider(scaleFactor = scaleFactor) {
        quasarisTheme(themeMode = themeMode, avatarColor = avatarColor) {
            val navController = rememberNavController()
            val focusManager = LocalFocusManager.current

            // Clear focus when destination changes
            LaunchedEffect(navController) {
                navController.currentBackStackEntryFlow.collect {
                    focusManager.clearFocus()
                }
            }

            var isFullscreenDialogOpen by remember { mutableStateOf(false) }

            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val scope = rememberCoroutineScope()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            val animDuration = 550
            val navHostOffsetSpec = tween<IntOffset>(durationMillis = animDuration, easing = FastOutSlowInEasing)

            Box(modifier = Modifier.fillMaxSize()) {
                // Layer 1: Global Background Source
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .hazeSource(state = hazeState)
                )

                // Layer 2: Main UI Source
                Box(modifier = Modifier.fillMaxSize().hazeSource(state = overlayHazeState)) {
                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        gesturesEnabled = currentRoute == "menu" ||
                                currentRoute == "settings" ||
                                currentRoute == "spellbook" ||
                                currentRoute == "glossary" ||
                                currentRoute == "formula_info" ||
                                currentRoute?.startsWith("edit/") == true,
                        drawerContent = {
                            ModalDrawerSheet {
                                Spacer(Modifier.height(12.dp))
                                NavigationDrawerItem(
                                    label = { Text("Главный экран") },
                                    selected = currentRoute == "menu",
                                    onClick = {
                                        scope.launch { drawerState.close() }
                                        if (currentRoute != "menu") {
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

                                NavigationDrawerItem(
                                    label = { Text("Книга заклинаний") },
                                    selected = currentRoute == "spellbook",
                                    onClick = {
                                        scope.launch { drawerState.close() }
                                        if (currentRoute != "spellbook") {
                                            navController.navigate("spellbook")
                                        }
                                    },
                                    icon = { Icon(Icons.Default.AutoFixHigh, null) },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                )

                                NavigationDrawerItem(
                                    label = { Text("Глоссарий") },
                                    selected = currentRoute == "glossary",
                                    onClick = {
                                        scope.launch { drawerState.close() }
                                        if (currentRoute != "glossary") {
                                            navController.navigate("glossary")
                                        }
                                    },
                                    icon = { Icon(Icons.Default.HistoryEdu, null) },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                )

                                NavigationDrawerItem(
                                    label = { Text("Модули") },
                                    selected = currentRoute == "modules",
                                    onClick = {
                                        scope.launch { drawerState.close() }
                                        if (currentRoute != "modules") {
                                            navController.navigate("modules")
                                        }
                                    },
                                    icon = { Icon(Icons.Default.Extension, null) },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                )
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
                                            characterRepository.updateCharacter(importedCharacter)
                                        },
                                        onDeleteCharacters = { uuidsToDelete ->
                                            uuidsToDelete.forEach { characterRepository.deleteCharacter(it) }
                                            if (characters.none { it.id == lastCharacterId }) {
                                                lastCharacterId = -1
                                                onCharacterIdChange(-1)
                                            }
                                        },
                                        onOpenDrawer = { scope.launch { drawerState.open() } },
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
                                        onThemeModeChange = { themeMode = it },
                                        settingsViewModel = settingsViewModel
                                    )
                                }

                                composable("formula_info") {
                                    FormulaInfoWindow(
                                        onNavigateBack = { navController.popBackStack() }
                                    )
                                }

                                composable("spellbook") {
                                    SpellbookWindow(
                                        spellbookManager = spellbookManager,
                                        glossaryImporter = glossaryImporter,
                                        onOpenDrawer = { scope.launch { drawerState.open() } },
                                        onFullscreenDialogOpenChange = { isFullscreenDialogOpen = it },
                                        hazeState = hazeState,
                                        popupHazeState = overlayHazeState,
                                        forceBlurEnabled = effectiveBlurFullscreen,
                                        settingsViewModel = settingsViewModel
                                    )
                                }

                                composable("glossary") {
                                    GlossaryWindow(
                                        spellbookManager = spellbookManager,
                                        moduleManager = moduleManager,
                                        onOpenDrawer = { scope.launch { drawerState.open() } },
                                        onNavigateToSpells = { navController.navigate("spellbook") },
                                        hazeState = hazeState,
                                        popupHazeState = overlayHazeState,
                                        forceBlurEnabled = effectiveBlurFullscreen,
                                        settingsViewModel = settingsViewModel
                                    )
                                }

                                composable("modules") {
                                    ModulesWindow(
                                        moduleManager = moduleManager,
                                        glossaryImporter = glossaryImporter,
                                        onOpenDrawer = { scope.launch { drawerState.open() } },
                                        hazeState = hazeState,
                                        forceBlurEnabled = effectiveBlurFullscreen,
                                        settingsViewModel = settingsViewModel
                                    )
                                }

                                composable("create_setup") {
                                    CharacterCreationWindow(
                                        onNavigateBack = { navController.popBackStack() },
                                        onCharacterCreate = { newChar ->
                                            characterRepository.updateCharacter(newChar)
                                            lastCharacterId = newChar.id
                                            onCharacterIdChange(newChar.id)
                                            onSeedColorChange(newChar.themeSeedColorArgb)
                                            navController.navigate("edit/${newChar.uuid}") {
                                                popUpTo("menu")
                                            }
                                        },
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
                                            character = characterRepository.getFullCharacter(characterUuid)
                                        }
                                    }

                                    CharacterDetailWindow(
                                        character = character,
                                        onNavigateBack = {
                                            navController.popBackStack()
                                        },
                                        onOpenDrawer = { scope.launch { drawerState.open() } },
                                        onDeleteCharacter = { charToDelete ->
                                            characterRepository.deleteCharacter(charToDelete.uuid)
                                            if (lastCharacterId == charToDelete.id) {
                                                lastCharacterId = -1
                                                onCharacterIdChange(-1)
                                            }
                                            navController.popBackStack()
                                        },
                                        onSaveChanges = { updatedCharacter ->
                                            characterRepository.updateCharacter(updatedCharacter)
                                            val index = characters.indexOfFirst { it.uuid == updatedCharacter.uuid }
                                            if (index != -1) {
                                                characters[index] = updatedCharacter.toSummary()
                                                if (updatedCharacter.id == lastCharacterId) {
                                                    onSeedColorChange(updatedCharacter.themeSeedColorArgb)
                                                }
                                            }
                                        },
                                        onRoll = { res ->
                                            PlatformUtils.performHapticFeedback()
                                            rollHistory = (listOf(res) + rollHistory).take(maxOf(1, historyLimit))
                                        },
                                        onFullscreenDialogOpenChange = { isFullscreenDialogOpen = it },
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

                val isKeyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
                val isOnCharacterScreen = currentRoute?.startsWith("edit/") == true
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
