package ru.quasaris.characternexus.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.chrisbanes.haze.HazeState
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.ui.menu.MenuWindow as RealMenuWindow

@Composable
fun AppScaleProvider(scaleFactor: Float, content: @Composable () -> Unit) {
    content()
}

@Composable
fun quasarisTheme(themeMode: String, avatarColor: Int?, content: @Composable () -> Unit) {
    content()
}

@Composable
fun MenuWindow(
    characters: List<CharacterSummary>,
    onNavigateToCreate: () -> Unit,
    onCharacterClick: (String) -> Unit,
    onImportCharacter: (Character) -> Unit,
    onDeleteCharacters: (List<String>) -> Unit,
    onOpenDrawer: () -> Unit,
    settingsViewModel: Any?,
    hazeState: HazeState,
    popupHazeState: HazeState,
    forceBlurEnabled: Boolean,
    blurPopups: Boolean
) {
    RealMenuWindow(
        characters = characters,
        onNavigateToCreate = onNavigateToCreate,
        onCharacterClick = onCharacterClick,
        onImportCharacter = onImportCharacter,
        onDeleteCharacters = onDeleteCharacters,
        onOpenDrawer = onOpenDrawer,
        settingsViewModel = settingsViewModel,
        hazeState = hazeState,
        popupHazeState = popupHazeState,
        forceBlurEnabled = forceBlurEnabled,
        blurPopups = blurPopups
    )
}

@Composable
fun SettingsWindow(
    onOpenDrawer: () -> Unit,
    onThemeModeChange: (String) -> Unit,
    settingsViewModel: Any?
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Settings Window Stub") }
}

@Composable
fun FormulaInfoWindow(onNavigateBack: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Formula Info Window Stub") }
}

@Composable
fun SpellbookWindow(
    spellbookManager: Any?,
    glossaryImporter: Any?,
    onOpenDrawer: () -> Unit,
    onFullscreenDialogOpenChange: (Boolean) -> Unit,
    hazeState: HazeState,
    popupHazeState: HazeState,
    forceBlurEnabled: Boolean,
    settingsViewModel: Any?
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Spellbook Window Stub") }
}

@Composable
fun GlossaryWindow(
    spellbookManager: Any?,
    moduleManager: Any?,
    onOpenDrawer: () -> Unit,
    onNavigateToSpells: () -> Unit,
    hazeState: HazeState,
    popupHazeState: HazeState,
    forceBlurEnabled: Boolean,
    settingsViewModel: Any?
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Glossary Window Stub") }
}

@Composable
fun ModulesWindow(
    moduleManager: Any?,
    glossaryImporter: Any?,
    onOpenDrawer: () -> Unit,
    hazeState: HazeState,
    forceBlurEnabled: Boolean,
    settingsViewModel: Any?
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Modules Window Stub") }
}

@Composable
fun CharacterCreationWindow(
    onNavigateBack: () -> Unit,
    onCharacterCreate: (Character) -> Unit,
    hazeState: HazeState,
    popupHazeState: HazeState,
    forceBlurEnabled: Boolean,
    blurPopups: Boolean
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Creation Window Stub") }
}

@Composable
fun CharacterDetailWindow(
    character: Character?,
    onNavigateBack: () -> Unit,
    onOpenDrawer: () -> Unit,
    onDeleteCharacter: (Character) -> Unit,
    onSaveChanges: (Character) -> Unit,
    onRoll: (RollResult) -> Unit,
    onFullscreenDialogOpenChange: (Boolean) -> Unit,
    hazeState: HazeState,
    popupHazeState: HazeState,
    forceBlurEnabled: Boolean,
    blurPopups: Boolean,
    settingsViewModel: Any?,
    spellbookManager: Any?
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Detail Window Stub") }
}

@Composable
fun DiceRollOverlay(
    history: List<RollResult>,
    onClose: () -> Unit,
    themeMode: String,
    forceBlurEnabled: Boolean,
    hazeState: HazeState,
    alpha: Float,
    isPassThrough: Boolean,
    position: DiceRollPosition,
    closeButtonPosition: DiceRollPosition,
    modifier: Modifier = Modifier
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomStart) { Text("Dice Roll Overlay Stub") }
}
