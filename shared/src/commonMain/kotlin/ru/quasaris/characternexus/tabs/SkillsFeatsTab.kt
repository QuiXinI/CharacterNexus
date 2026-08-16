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
    settingsViewModel: SettingsViewModel? = null,
    statsMap: Map<String, String> = emptyMap(),
    onFullscreenDialogOpenChange: (Boolean) -> Unit = {},
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
        addButtonText = "ДОБАВИТЬ ОСОБОЕ ПОЛЕ",
        emptyListText = "Список умений и черт пуст",
        titlePlaceholder = "Название раздела",
        contentPlaceholder = "Описание раздела...",
        settingsViewModel = settingsViewModel,
        statsMap = statsMap,
        onFullscreenDialogOpenChange = onFullscreenDialogOpenChange,
        header = header
    )
}
