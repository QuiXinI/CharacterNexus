package ru.quasaris.characters.master

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ru.quasaris.characters.master.ui.theme.quasarisTheme

import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Extension
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.launch
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalDensity
import ru.quasaris.characters.master.backend.AppLifecycleObserver
import ru.quasaris.characters.master.backend.AppScaleManager
import ru.quasaris.characters.master.backend.AppScaleProvider
import ru.quasaris.characters.master.backend.DiceRoller
import ru.quasaris.characters.master.backend.RollResult
import ru.quasaris.characters.master.backend.SettingsManager
import ru.quasaris.characters.master.backend.SettingsViewModel
import ru.quasaris.characters.master.ui.DiceRollOverlay
import androidx.compose.material.icons.filled.MenuBook
import ru.quasaris.characters.master.backend.SpellbookManager
import ru.quasaris.characters.master.SpellbookWindow
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.ClipboardManager
import android.content.Context
import android.content.ClipData
import android.util.Log
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import androidx.compose.runtime.CompositionLocalProvider

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val settingsManager = SettingsManager(applicationContext)
        val appScaleManager = AppScaleManager(applicationContext)
        val settingsViewModel = SettingsViewModel(appScaleManager, settingsManager)

        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val logMessage = "\n--- CRASH LOG [$timestamp] ---\n${Log.getStackTraceString(throwable)}\n------------------------------\n"
            
            try {
                val logFile = File(applicationContext.filesDir, "crash_log.txt")
                FileOutputStream(logFile, true).use { it.write(logMessage.toByteArray()) }
            } catch (e: Exception) {
                Log.e("CrashHandler", "Failed to write crash log", e)
            }

            if (settingsManager.debugInfoEnabled) {
                try {
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("App Crash", logMessage)
                    clipboard.setPrimaryClip(clip)
                } catch (e: Exception) {
                    Log.e("CrashHandler", "Failed to copy to clipboard", e)
                }
            }

            android.os.Process.killProcess(android.os.Process.myPid())
            System.exit(1)
        }

        super.onCreate(savedInstanceState)

        val characterRepository = CharacterRepository(
            context = applicationContext,
            appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        )
        val spellbookManager = SpellbookManager(applicationContext)
        val moduleManager = ru.quasaris.characters.master.backend.ModuleManager(applicationContext)
        val glossaryImporter = ru.quasaris.characters.master.backend.GlossaryImporter(applicationContext, spellbookManager, moduleManager)
        
        ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleObserver(characterRepository))

        enableEdgeToEdge()
        setContent {
            val scaleFactor by settingsViewModel.scaleFactor.collectAsState()
            val sliderHistorySize by settingsViewModel.rollHistorySize.collectAsState()
            val customHistorySize by settingsViewModel.customRollHistorySize.collectAsState()
            val historyLimit = if (sliderHistorySize >= 10) customHistorySize else sliderHistorySize

            var themeMode by remember { mutableStateOf(settingsManager.themeMode) }
            var lastCharacterId by remember { mutableIntStateOf(settingsManager.lastCharacterId) }
            
            val masterBlurEnabled by settingsViewModel.masterBlurEnabled.collectAsState()
            val blurRolls by settingsViewModel.blurRolls.collectAsState()
            val blurFullscreen by settingsViewModel.blurFullscreen.collectAsState()
            val blurPopups by settingsViewModel.blurPopups.collectAsState()
            val rollAlpha by settingsViewModel.rollInterfaceAlpha.collectAsState()
            
            val effectiveBlurRolls = masterBlurEnabled && blurRolls
            val effectiveBlurFullscreen = masterBlurEnabled && blurFullscreen
            val effectiveBlurPopups = masterBlurEnabled && blurPopups

            val rollPassThrough by settingsViewModel.rollPassThrough.collectAsState()
            val rollPosition by settingsViewModel.rollPosition.collectAsState()

            val hazeState = remember { HazeState() }
            val overlayHazeState = remember { HazeState() }

            val characters: SnapshotStateList<Character> = remember {
                mutableStateListOf<Character>().apply {
                    addAll(characterRepository.loadCharacters())
                }
            }

            var rollHistory by remember { mutableStateOf(listOf<RollResult>()) }

            // Sync SnapshotStateList back to Repository for debounced saving
            LaunchedEffect(characters) {
                snapshotFlow { characters.toList() }
                    .collectLatest { list ->
                        characterRepository.updateCharacters(list)
                    }
            }
            
            val lastCharacter = characters.find { it.id == lastCharacterId }
            val avatarColor = lastCharacter?.themeSeedColorArgb ?: settingsManager.lastCharacterSeedColor

            AppScaleProvider(scaleFactor = scaleFactor) {
                quasarisTheme(themeMode = themeMode, avatarColor = avatarColor) {
                    val navController = rememberNavController()
                    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
                    
                    val currentToolbar = androidx.compose.ui.platform.LocalTextToolbar.current
                    val safeToolbar = remember(currentToolbar) {
                        object : androidx.compose.ui.platform.TextToolbar {
                            override val status: androidx.compose.ui.platform.TextToolbarStatus get() = currentToolbar.status
                            override fun hide() = currentToolbar.hide()
                            override fun showMenu(
                                rect: androidx.compose.ui.geometry.Rect,
                                onCopyRequested: (() -> Unit)?,
                                onPasteRequested: (() -> Unit)?,
                                onCutRequested: (() -> Unit)?,
                                onSelectAllRequested: (() -> Unit)?
                            ) {
                                try {
                                    currentToolbar.showMenu(rect, onCopyRequested, onPasteRequested, onCutRequested, onSelectAllRequested)
                                } catch (e: IllegalArgumentException) {
                                    if (e.message?.contains("hierarchy") == true) {
                                        Log.e("SafeTextToolbar", "Ignored hierarchy crash", e)
                                    } else throw e
                                }
                            }
                        }
                    }
                    
                    // Clear focus when destination changes to avoid context menu crashes during transitions
                    LaunchedEffect(navController) {
                        navController.currentBackStackEntryFlow.collect {
                            focusManager.clearFocus()
                        }
                    }

                    var isFullscreenDialogOpen by remember { mutableStateOf(false) }

                    CompositionLocalProvider(
                        androidx.compose.ui.platform.LocalTextToolbar provides safeToolbar
                    ) {
                        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                        val scope = rememberCoroutineScope()
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
                            )

                            // Layer 2: Main UI Source (for popups, FAB and dragged items)
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
                                                        navController.navigate("edit/$lastCharacterId")
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
                                        color = Color.Transparent, // Прозрачный, чтобы видеть глобальный фон
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
                                                    onCharacterClick = { characterId ->
                                                        val char = characters.find { it.id == characterId }
                                                        lastCharacterId = characterId
                                                        settingsManager.lastCharacterId = characterId
                                                        settingsManager.lastCharacterSeedColor = char?.themeSeedColorArgb
                                                        navController.navigate("edit/$characterId")
                                                    },
                                                    onImportCharacter = { importedCharacter ->
                                                        characters.add(importedCharacter)
                                                        characterRepository.saveCharacters(characters)
                                                    },
                                                    onDeleteCharacters = { idsToDelete ->
                                                        characters.removeAll { it.id in idsToDelete }
                                                        characterRepository.saveCharacters(characters)
                                                        if (lastCharacterId in idsToDelete) {
                                                            lastCharacterId = -1
                                                            settingsManager.lastCharacterId = -1
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

                                            composable(
                                                "create_setup"
                                            ) {
                                                CharacterCreationWindow(
                                                    onNavigateBack = { navController.popBackStack() },
                                                    onCharacterCreate = { newChar ->
                                                        characters.add(newChar)
                                                        characterRepository.saveCharacters(characters)
                                                        lastCharacterId = newChar.id
                                                        settingsManager.lastCharacterId = newChar.id
                                                        settingsManager.lastCharacterSeedColor = newChar.themeSeedColorArgb
                                                        navController.navigate("edit/${newChar.id}") {
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
                                                route = "edit/{characterId}",
                                                arguments = listOf(navArgument("characterId") { type = NavType.IntType })
                                            ) { backStackEntry ->
                                                val characterId = backStackEntry.arguments?.getInt("characterId")
                                                val character = characters.find { it.id == characterId }

                                                CharacterDetailWindow(
                                                    character = character,
                                                    onNavigateBack = {
                                                        navController.popBackStack()
                                                    },
                                                    onOpenDrawer = { scope.launch { drawerState.open() } },
                                                    onDeleteCharacter = { charToDelete ->
                                                        characters.removeAll { it.id == charToDelete.id }
                                                        characterRepository.saveCharacters(characters)
                                                        if (lastCharacterId == charToDelete.id) {
                                                            lastCharacterId = -1
                                                            settingsManager.lastCharacterId = -1
                                                        }
                                                        navController.popBackStack()
                                                    },
                                                    onSaveChanges = { updatedCharacter ->
                                                        val index = characters.indexOfFirst { it.id == updatedCharacter.id }
                                                        if (index != -1) {
                                                            characters[index] = updatedCharacter
                                                            characterRepository.saveCharacters(characters)
                                                            if (updatedCharacter.id == lastCharacterId) {
                                                                settingsManager.lastCharacterSeedColor = updatedCharacter.themeSeedColorArgb
                                                            }
                                                        }
                                                    },
                                                    onRoll = { res -> 
                                                        DiceRoller.performHapticFeedback(this@MainActivity, res)
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

                            val rollAlpha by settingsViewModel.rollInterfaceAlpha.collectAsState()
                            val rollPassThrough by settingsViewModel.rollPassThrough.collectAsState()
                            val rollPosition by settingsViewModel.rollPosition.collectAsState()
                            val rollCloseButtonPos by settingsViewModel.rollCloseButtonPosition.collectAsState()

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
        }
    }
}
