package ru.quasaris.characternexus.tabs

import androidx.compose.runtime.Composable
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.backend.SettingsViewModel
import dev.chrisbanes.haze.HazeState

@Composable
fun NotesTab(
    notes: List<DynamicNoteState>,
    onNotesChange: (List<DynamicNoteState>) -> Unit,
    hazeState: HazeState? = null,
    popupHazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false,
    blurPopups: Boolean = false,
    isEditMode: Boolean = false,
    onToggleEditMode: () -> Unit = {},
    onToggleAllExpansion: () -> Unit = {},
    anyCollapsed: Boolean = false,
    settingsViewModel: SettingsViewModel? = null,
    statsMap: Map<String, String> = emptyMap(),
    onFullscreenDialogOpenChange: (Boolean) -> Unit = {},
    onFullscreenVisibilityChanged: (Boolean) -> Unit = {},
    state: ru.quasaris.characternexus.ui.CharacterDetailState? = null,
    header: @Composable () -> Unit = {}
) {
    DynamicFieldsTab(
        fields = notes,
        onFieldsChange = onNotesChange,
        hazeState = hazeState,
        popupHazeState = popupHazeState,
        forceBlurEnabled = forceBlurEnabled,
        blurPopups = blurPopups,
        isEditMode = isEditMode,
        onToggleEditMode = onToggleEditMode,
        onToggleAllExpansion = onToggleAllExpansion,
        anyCollapsed = anyCollapsed,
        addButtonText = "ДОБАВИТЬ ЗАМЕТКУ",
        emptyListText = "Список заметок пуст",
        titlePlaceholder = "Заголовок заметки",
        contentPlaceholder = "Текст заметки...",
        settingsViewModel = settingsViewModel,
        statsMap = statsMap,
        onFullscreenDialogOpenChange = onFullscreenDialogOpenChange,
        onFullscreenVisibilityChanged = onFullscreenVisibilityChanged,
        state = state,
        header = header
    )
}
