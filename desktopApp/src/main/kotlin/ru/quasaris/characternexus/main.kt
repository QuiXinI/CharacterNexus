package ru.quasaris.characternexus

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import ru.quasaris.characternexus.model.DiceRollPosition

import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date

fun main() {
    try {
        val workingDir = System.getProperty("user.dir")
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
        
        // Basic startup log
        File("startup-log.txt").appendText("[$timestamp] App starting. Working dir: $workingDir\n")
        
        runApp()
    } catch (e: Throwable) {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        e.printStackTrace(pw)
        val stackTrace = sw.toString()
        
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
        val logFile = File("crash-log.txt")
        logFile.writeText("CRASH AT $timestamp\n\n$stackTrace")
        
        // Also print to console
        e.printStackTrace()
        
        // Exit with error code to let launcher know something went wrong
        System.exit(1)
    }
}

fun runApp() = application {
    // Step 1: Initialize Platform Context
    platformContext = PlatformContext()

    val settingsManager = remember { SettingsManager() }
    val settingsViewModel = remember { SettingsViewModel(settingsManager) }
    
    val characterRepository = remember {
        CharacterRepository(
            storage = FileSystemCharacterStorage(),
            appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        )
    }

    val isDebug = System.getProperty("debug") == "true" || System.getenv("DEBUG") == "true"
    if (isDebug) {
        DebugLogWindow()
    }

    Window(
        onCloseRequest = {
            characterRepository.flushBlocking()
            exitApplication()
        },
        title = "Character Nexus",
        icon = painterResource("icon.png"),
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
            initialThemeMode = settingsManager.themeMode,
            initialLastCharacterId = settingsManager.lastCharacterId,
            initialLastCharacterSeedColor = settingsManager.lastCharacterSeedColor,
            settingsViewModel = settingsViewModel,
            characterRepository = characterRepository,
            spellbookManager = null,
            moduleManager = null,
            glossaryImporter = null,
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
            onCharacterIdChange = { id -> settingsManager.lastCharacterId = id },
            onSeedColorChange = { color -> settingsManager.lastCharacterSeedColor = color }
        )
    }
}
