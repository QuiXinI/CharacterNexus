package ru.quasaris.characters.master.tabs

import androidx.compose.runtime.Composable
import ru.quasaris.characters.master.DynamicNoteState
import ru.quasaris.characters.master.backend.SettingsViewModel
import dev.chrisbanes.haze.HazeState

@Composable
fun NotesTab(
    notes: List<DynamicNoteState>,
    onNotesChange: (List<DynamicNoteState>) -> Unit,
    hazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false,
    blurPopups: Boolean = false,
    isEditMode: Boolean = false,
    settingsViewModel: SettingsViewModel? = null,
    statsMap: Map<String, String> = emptyMap()
) {
    DynamicFieldsTab(
        fields = notes,
        onFieldsChange = onNotesChange,
        hazeState = hazeState,
        forceBlurEnabled = forceBlurEnabled,
        blurPopups = blurPopups,
        isEditMode = isEditMode,
        addButtonText = "ДОБАВИТЬ ЗАМЕТКУ",
        emptyListText = "Список заметок пуст",
        titlePlaceholder = "Заголовок заметки",
        contentPlaceholder = "Текст заметки...",
        settingsViewModel = settingsViewModel,
        statsMap = statsMap
    )
}
