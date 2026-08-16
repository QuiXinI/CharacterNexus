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

fun main() = application {
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

    Window(
        onCloseRequest = ::exitApplication,
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
