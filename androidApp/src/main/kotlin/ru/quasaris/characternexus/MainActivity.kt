package ru.quasaris.characternexus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import ru.quasaris.characternexus.backend.SettingsManager
import ru.quasaris.characternexus.backend.SettingsViewModel
import ru.quasaris.characternexus.backend.CharacterRepository
import ru.quasaris.characternexus.backend.storage.FileSystemCharacterStorage
import ru.quasaris.characternexus.util.PlatformUtils

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Step 1: Initialize Platform Context
        platformContext = PlatformContext(applicationContext)
        PlatformUtils.androidContext = applicationContext
        
        val settingsManager = SettingsManager()
        val settingsViewModel = SettingsViewModel(settingsManager)

        val characterRepository = CharacterRepository(
            storage = FileSystemCharacterStorage(),
            appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        )
        
        // TODO: Initialize other managers when fully migrated to common
        val spellbookManager = null 
        val moduleManager = null
        val glossaryImporter = null

        enableEdgeToEdge()

        setContent {
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
                spellbookManager = spellbookManager,
                moduleManager = moduleManager,
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
                onCharacterIdChange = { id -> settingsManager.lastCharacterId = id },
                onSeedColorChange = { color -> settingsManager.lastCharacterSeedColor = color }
            )
        }
    }
}
