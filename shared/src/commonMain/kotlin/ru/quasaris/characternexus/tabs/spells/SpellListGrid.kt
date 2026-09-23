package ru.quasaris.characternexus.tabs.spells

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.backend.DicePart
import dev.chrisbanes.haze.HazeState

@Composable
fun SpellListGrid(
    spells: List<SpellCard>,
    filterState: SpellFilterState,
    expandedIds: Set<String>,
    onToggleExpand: (String) -> Unit,
    modifier: Modifier = Modifier,
    selectedIds: Set<String> = emptySet(),
    onToggleSelect: ((String, Boolean) -> Unit)? = null,
    statsMap: Map<String, String> = emptyMap(),
    characterLevel: Int = 1,
    spellAttackBonus: Int = 0,
    spellAttackDice: List<DicePart> = emptyList(),
    spellSaveDc: Int = 0,
    spellSaveDice: List<DicePart> = emptyList(),
    onRollDamage: (String, String, AdvantageType) -> Unit = { _, _, _ -> },
    onRollAttack: (AdvantageType) -> Unit = {},
    hazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false,
    blurCards: Boolean = true,
    isEditable: Boolean = false,
    onEdit: (SpellCard) -> Unit = {},
    spellOverrides: Map<String, SpellCard> = emptyMap(),
    settingsViewModel: ru.quasaris.characternexus.backend.SettingsViewModel? = null
) {
    val grouped = remember(spells) { spells.groupBy { it.level } }
    val sortedLevels = remember(grouped) { grouped.keys.sortedBy { it.toIntOrNull() ?: Int.MAX_VALUE } }
    val isCompact = filterState.isCompact

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 340.dp),
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        sortedLevels.forEach { levelStr ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = if (levelStr == "0") "ЗАГОВОРЫ" else "$levelStr УРОВЕНЬ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )
            }
            
            items(grouped[levelStr] ?: emptyList(), key = { it.id }) { spell ->
                val isSelected = remember(spell, selectedIds) {
                    selectedIds.any { idString -> spell.matchesId(idString) }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (onToggleSelect != null) {
                        androidx.compose.material3.Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleSelect(spell.id, it) }
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    
                    SpellCardItem(settingsViewModel = settingsViewModel, 
                        spell = spell,
                        isExpanded = spell.id in expandedIds,
                        onToggleExpand = { onToggleExpand(spell.id) },
                        modifier = Modifier.weight(1f),
                        isSelected = isSelected,
                        onLongClick = { onToggleSelect?.invoke(spell.id, !isSelected) },
                        onEdit = { onEdit(spell) },
                        isEditable = isEditable,
                        statsMap = statsMap,
                        characterLevel = characterLevel,
                        spellAttackBonus = spellAttackBonus,
                        spellAttackDice = spellAttackDice,
                        spellSaveDc = spellSaveDc,
                        spellSaveDice = spellSaveDice,
                        onRollDamage = onRollDamage,
                        onRollAttack = onRollAttack,
                        hazeState = hazeState,
                        forceBlurEnabled = forceBlurEnabled,
                        blurCards = blurCards,
                        isCompact = isCompact,
                        isOverridden = spell.id in spellOverrides
                    )
                }
            }
        }
    }
}
