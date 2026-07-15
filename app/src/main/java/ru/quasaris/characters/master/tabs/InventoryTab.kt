package ru.quasaris.characters.master.tabs

import androidx.compose.runtime.Composable
import ru.quasaris.characters.master.DynamicNoteState
import ru.quasaris.characters.master.backend.SettingsViewModel
import dev.chrisbanes.haze.HazeState

@Composable
fun InventoryTab(
    inventory: List<DynamicNoteState>,
    onInventoryChange: (List<DynamicNoteState>) -> Unit,
    hazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false,
    isEditMode: Boolean = false,
    settingsViewModel: SettingsViewModel? = null
) {
    DynamicFieldsTab(
        fields = inventory,
        onFieldsChange = onInventoryChange,
        hazeState = hazeState,
        forceBlurEnabled = forceBlurEnabled,
        isEditMode = isEditMode,
        addButtonText = "ДОБАВИТЬ ОСОБОЕ ПОЛЕ",
        emptyListText = "Инвентарь пуст",
        titlePlaceholder = "Название раздела",
        contentPlaceholder = "Содержимое раздела...",
        settingsViewModel = settingsViewModel
    )
}
