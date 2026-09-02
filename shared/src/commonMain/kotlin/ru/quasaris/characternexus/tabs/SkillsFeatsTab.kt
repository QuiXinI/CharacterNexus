package ru.quasaris.characternexus.tabs

import androidx.compose.runtime.Composable
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.backend.SettingsViewModel
import dev.chrisbanes.haze.HazeState

@Composable
fun SkillsFeatsTab(
    skillsAndTraits: List<DynamicNoteState>,
    onSkillsAndTraitsChange: (List<DynamicNoteState>) -> Unit,
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
        fields = skillsAndTraits,
        onFieldsChange = onSkillsAndTraitsChange,
        hazeState = hazeState,
        popupHazeState = popupHazeState,
        forceBlurEnabled = forceBlurEnabled,
        blurPopups = blurPopups,
        isEditMode = isEditMode,
        onToggleEditMode = onToggleEditMode,
        onToggleAllExpansion = onToggleAllExpansion,
        anyCollapsed = anyCollapsed,
        addButtonText = "ДОБАВИТЬ ОСОБОЕ ПОЛЕ",
        emptyListText = "Список умений и черт пуст",
        titlePlaceholder = "Название раздела",
        contentPlaceholder = "Описание раздела...",
        settingsViewModel = settingsViewModel,
        statsMap = statsMap,
        onFullscreenDialogOpenChange = onFullscreenDialogOpenChange,
        onFullscreenVisibilityChanged = onFullscreenVisibilityChanged,
        state = state,
        header = header
    )
}
