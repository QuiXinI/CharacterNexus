package ru.quasaris.characters.master.tabs

import androidx.compose.runtime.Composable
import ru.quasaris.characters.master.DynamicNoteState
import ru.quasaris.characters.master.backend.SettingsViewModel
import dev.chrisbanes.haze.HazeState

@Composable
fun SkillsFeatsTab(
    skillsAndTraits: List<DynamicNoteState>,
    onSkillsAndTraitsChange: (List<DynamicNoteState>) -> Unit,
    hazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false,
    blurPopups: Boolean = false,
    isEditMode: Boolean = false,
    settingsViewModel: SettingsViewModel? = null,
    statsMap: Map<String, String> = emptyMap()
) {
    DynamicFieldsTab(
        fields = skillsAndTraits,
        onFieldsChange = onSkillsAndTraitsChange,
        hazeState = hazeState,
        forceBlurEnabled = forceBlurEnabled,
        blurPopups = blurPopups,
        isEditMode = isEditMode,
        addButtonText = "ДОБАВИТЬ ОСОБОЕ ПОЛЕ",
        emptyListText = "Список умений и черт пуст",
        titlePlaceholder = "Название раздела",
        contentPlaceholder = "Описание раздела...",
        settingsViewModel = settingsViewModel,
        statsMap = statsMap
    )
}
