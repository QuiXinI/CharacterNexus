package ru.quasaris.characternexus.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.backend.SettingsViewModel
import dev.chrisbanes.haze.HazeState
import ru.quasaris.characternexus.tabs.cargo.CargoSection
import ru.quasaris.characternexus.tabs.proficiencies.ProficienciesSection

@Composable
fun SkillsFeatsTab(
    skillsAndTraits: List<DynamicNoteState>,
    onSkillsAndTraitsChange: (List<DynamicNoteState>) -> Unit,
    Cargo: CargoState = CargoState(),
    onCargoChange: (CargoState) -> Unit = {},
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
    isDesktop: Boolean = false,
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
        isContentVisible = { it.tag != "Cargo" },
        isAddButtonVisible = false,
        header = header,
        footer = {
            val hasCargoSection = skillsAndTraits.any { it.tag == "Cargo" }
            val hasProficienciesSection = skillsAndTraits.any { it.tag == "Proficiencies" }
            var footerWidth by remember { mutableStateOf(0.dp) }
            val density = LocalDensity.current

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .onSizeChanged { size ->
                        footerWidth = with(density) { size.width.toDp() }
                    }
            ) {
                val addButtonText = if (footerWidth < 200.dp) "ПОЛЕ" else "ДОБАВИТЬ ПОЛЕ"
                val cargoButtonText = if (footerWidth < 200.dp) "ГРУЗ" else "ГРУЗ И ПРЫЖКИ"
                val profButtonText = if (footerWidth < 200.dp) "ВЛАДЕНИЯ" else "БЛОК ВЛАДЕНИЙ"

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val newFields = skillsAndTraits + DynamicNoteState()
                                onSkillsAndTraitsChange(newFields)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(addButtonText, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }

                        if (!hasCargoSection) {
                            Button(
                                onClick = {
                                    val newFields = skillsAndTraits + DynamicNoteState(title = "Размер, Грузоподъёмность и Прыжки", tag = "Cargo")
                                    onSkillsAndTraitsChange(newFields)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(cargoButtonText, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                    }

                    if (!hasProficienciesSection) {
                        Button(
                            onClick = {
                                val newFields = skillsAndTraits + DynamicNoteState(title = "Владения", tag = "Proficiencies")
                                onSkillsAndTraitsChange(newFields)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(profButtonText, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
            }
        },
        extraContent = { field ->
            when (field.tag) {
                "Cargo" -> {
                    CargoSection(
                        Cargo = Cargo,
                        statsMap = statsMap,
                        isExpanded = field.isExpanded,
                        onEditClick = {
                            if (state != null) {
                                state.isCargoConfigOpen = true
                            }
                        }
                    )
                }
                "Proficiencies" -> {
                    if (state != null) {
                        ProficienciesSection(
                            state = state,
                            isExpanded = field.isExpanded
                        )
                    }
                }
            }
        }
    )
}
