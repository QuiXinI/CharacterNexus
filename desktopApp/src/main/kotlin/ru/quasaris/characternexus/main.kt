package ru.quasaris.characternexus

import androidx.compose.runtime.remember
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import ru.quasaris.characternexus.backend.CharacterRepository
import ru.quasaris.characternexus.backend.storage.FileSystemCharacterStorage
import ru.quasaris.characternexus.model.DiceRollPosition

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Character Nexus",
        icon = painterResource("icon.png"),
    ) {
        val characterRepository = remember {
            CharacterRepository(
                storage = FileSystemCharacterStorage(),
                appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            )
        }
        App(
            initialThemeMode = "dark",
            initialLastCharacterId = -1,
            initialLastCharacterSeedColor = null,
            settingsViewModel = null,
            characterRepository = characterRepository,
            spellbookManager = null,
            moduleManager = null,
            glossaryImporter = null,
            getScaleFactor = { 1.0f },
            getRollHistorySize = { 5 },
            getCustomRollHistorySize = { 10 },
            getMasterBlurEnabled = { true },
            getBlurRolls = { true },
            getBlurFullscreen = { true },
            getBlurPopups = { true },
            getRollInterfaceAlpha = { 1.0f },
            getRollPassThrough = { false },
            getRollPosition = { DiceRollPosition.BOTTOM_LEFT },
            getRollCloseButtonPosition = { DiceRollPosition.TOP_RIGHT },
            loadCharacters = { characterRepository.loadCharacters() },
            getFullCharacter = { uuid -> characterRepository.getFullCharacter(uuid) },
            updateCharacter = { char -> characterRepository.updateCharacter(char) },
            deleteCharacter = { uuid -> characterRepository.deleteCharacter(uuid) },
            onCharacterIdChange = {},
            onSeedColorChange = {}
        )
    }
}
