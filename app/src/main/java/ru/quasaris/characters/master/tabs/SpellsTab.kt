package ru.quasaris.characters.master.tabs

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.quasaris.characters.master.DynamicNoteState
import ru.quasaris.characters.master.backend.SettingsViewModel
import dev.chrisbanes.haze.HazeState

@Composable
fun SpellsTab(
    spells: List<DynamicNoteState>,
    onSpellsChange: (List<DynamicNoteState>) -> Unit,
    hazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false,
    isEditMode: Boolean = false,
    settingsViewModel: SettingsViewModel? = null
) {
    DynamicFieldsTab(
        fields = spells,
        onFieldsChange = onSpellsChange,
        hazeState = hazeState,
        forceBlurEnabled = forceBlurEnabled,
        isEditMode = isEditMode,
        addButtonText = "ДОБАВИТЬ ОСОБОЕ ПОЛЕ",
        emptyListText = "Книга заклинаний пуста",
        titlePlaceholder = "Название заклинания",
        contentPlaceholder = "Описание заклинания...",
        settingsViewModel = settingsViewModel,
        extraContent = { spell ->
            // TODO: Реализовать счетчики ячеек заклинаний для каждого уровня
            if (spell.title.contains("уровень", ignoreCase = true)) {
                Text(
                    text = "Счетчик ячеек (в разработке)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
                )
            }
        }
    )
}
