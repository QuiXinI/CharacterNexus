package ru.quasaris.characternexus

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import ru.quasaris.characternexus.ui.BackNavigationManager
import ru.quasaris.characternexus.ui.GlobalActionRegistry
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import ru.quasaris.characternexus.backend.CharacterRepository
import ru.quasaris.characternexus.backend.SettingsManager
import ru.quasaris.characternexus.backend.SettingsViewModel
import ru.quasaris.characternexus.backend.storage.FileSystemCharacterStorage
import ru.quasaris.characternexus.backend.ModuleManager
import ru.quasaris.characternexus.backend.SpellbookManager
import ru.quasaris.characternexus.backend.MagicItemManager
import ru.quasaris.characternexus.backend.glossary.GlossaryImporter
import ru.quasaris.characternexus.model.DiceRollPosition
import ru.quasaris.characternexus.util.DesktopCrashHandler

import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date

fun main() {
    val settingsManager = SettingsManager()
    DesktopCrashHandler.initialize(settingsManager)

    try {
        val workingDir = System.getProperty("user.dir")
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
        
        // Basic startup log
        File("startup-log.txt").appendText("[$timestamp] App starting. Working dir: $workingDir\n")
        
        runApp(settingsManager)
    } catch (e: Throwable) {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        e.printStackTrace(pw)
        val stackTrace = sw.toString()
        
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
        val logFile = File("crash-log.txt")
        logFile.appendText("CRASH AT $timestamp\n\n$stackTrace\n\n")
        
        e.printStackTrace()
        System.exit(1)
    }
}

fun runApp(settingsManager: SettingsManager) = application {
    // Step 1: Initialize Platform Context
    platformContext = PlatformContext()

    val settingsViewModel = remember { SettingsViewModel(settingsManager) }
    
    val moduleManager = remember { ModuleManager() }
    val spellbookManager = remember { SpellbookManager() }
    val magicItemManager = remember { MagicItemManager(moduleManager) }
    val glossaryImporter = remember { GlossaryImporter(spellbookManager, moduleManager) }

    val characterRepository = remember {
        CharacterRepository(
            storage = FileSystemCharacterStorage(),
            appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        )
    }

    Window(
        onCloseRequest = {
            characterRepository.flushBlocking()
            exitApplication()
        },
        title = "Character Nexus",
        icon = painterResource("icon.png"),
        onKeyEvent = {
            if (it.type == KeyEventType.KeyDown) {
                when (it.key) {
                    Key.Escape -> BackNavigationManager.pop()
                    Key.Tab -> {
                        GlobalActionRegistry.toggleDrawer()
                        true
                    }
                    else -> false
                }
            } else {
                false
            }
        }
    ) {
        val scaleFactor by settingsViewModel.scaleFactor.collectAsState()
        val rollHistorySize by settingsViewModel.rollHistorySize.collectAsState()
        val customRollHistorySize by settingsViewModel.customRollHistorySize.collectAsState()
        val masterBlurEnabled by settingsViewModel.masterBlurEnabled.collectAsState()
        val blurRolls by settingsViewModel.blurRolls.collectAsState()
        val blurFullscreen by settingsViewModel.blurFullscreen.collectAsState()
        val blurPopups by settingsViewModel.blurPopups.collectAsState()
        val rollAlpha by settingsViewModel.rollInterfaceAlpha.collectAsState()
        val rollPassThrough by settingsViewModel.rollPassThrough.collectAsState()
        val rollPosition by settingsViewModel.rollPosition.collectAsState()
        val rollCloseButtonPos by settingsViewModel.rollCloseButtonPosition.collectAsState()

        App(
            initialLastCharacterUuid = settingsManager.lastCharacterUuid,
            initialLastCharacterSeedColor = settingsManager.lastCharacterSeedColor,
            settingsViewModel = settingsViewModel,
            characterRepository = characterRepository,
            spellbookManager = spellbookManager,
            moduleManager = moduleManager,
            magicItemManager = magicItemManager,
            glossaryImporter = glossaryImporter,
            getScaleFactor = { scaleFactor },
            getRollHistorySize = { rollHistorySize },
            getCustomRollHistorySize = { customRollHistorySize },
            getMasterBlurEnabled = { masterBlurEnabled },
            getBlurRolls = { blurRolls },
            getBlurFullscreen = { blurFullscreen },
            getBlurPopups = { blurPopups },
            getRollInterfaceAlpha = { rollAlpha },
            getRollPassThrough = { rollPassThrough },
            getRollPosition = { rollPosition },
            getRollCloseButtonPosition = { rollCloseButtonPos },
            loadCharacters = { characterRepository.loadCharacters() },
            getFullCharacter = { uuid -> characterRepository.getFullCharacter(uuid) },
            updateCharacter = { char -> characterRepository.updateCharacter(char) },
            deleteCharacter = { uuid -> characterRepository.deleteCharacter(uuid) },
            onCharacterUuidChange = { uuid -> settingsManager.lastCharacterUuid = uuid },
            onSeedColorChange = { color -> settingsManager.lastCharacterSeedColor = color }
        )
    }
}
