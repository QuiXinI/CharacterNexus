package ru.quasaris.characternexus

import androidx.compose.ui.window.ComposeUIViewController
import ru.quasaris.characternexus.backend.SettingsManager
import ru.quasaris.characternexus.backend.SettingsViewModel
import ru.quasaris.characternexus.backend.CharacterRepository
import ru.quasaris.characternexus.backend.storage.FileSystemCharacterStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

fun MainViewController() = ComposeUIViewController { 
    val settingsManager = SettingsManager()
    val settingsViewModel = SettingsViewModel(settingsManager)
    val characterRepository = CharacterRepository(
        storage = FileSystemCharacterStorage(),
        appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    )

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
