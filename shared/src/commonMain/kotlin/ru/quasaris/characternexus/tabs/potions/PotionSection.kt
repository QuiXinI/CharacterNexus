package ru.quasaris.characternexus.tabs.potions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import ru.quasaris.characternexus.backend.DiceRoller
import ru.quasaris.characternexus.backend.SettingsViewModel
import ru.quasaris.characternexus.model.*
import sh.calvin.reorderable.*

@Composable
fun PotionSection(
    potions: List<PotionState>,
    onPotionsChange: (List<PotionState>) -> Unit,
    statsMap: Map<String, String>,
    settingsViewModel: SettingsViewModel?,
    hazeState: HazeState?,
    popupHazeState: HazeState? = null,
    onRoll: (RollResult) -> Unit,
    state: ru.quasaris.characternexus.ui.CharacterDetailState?,
    isTabEditMode: Boolean,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    var isPotionsEditMode by remember { mutableStateOf(false) }
    var draggingPotionId by remember { mutableStateOf<String?>(null) }
    val blurCards by settingsViewModel?.blurCards?.collectAsState() ?: remember { mutableStateOf(true) }

    Column(modifier = if (isExpanded) Modifier.padding(8.dp) else Modifier) {
        if (isExpanded) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { isPotionsEditMode = !isPotionsEditMode },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isPotionsEditMode) Icons.Default.Done else Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (isPotionsEditMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            ReorderableColumn(
                list = potions,
                onSettle = { from, to ->
                    val newList = potions.toMutableList().apply { add(to, removeAt(from)) }
                    onPotionsChange(newList)
                },
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) { _, potion, isDragging ->
                LaunchedEffect(isDragging) {
                    if (isDragging) draggingPotionId = potion.id
                    else if (draggingPotionId == potion.id) draggingPotionId = null
                }

                ReorderableItem {
                    PotionCardItem(
                        potion = potion,
                        onUpdate = { updated ->
                            val newList = potions.map { if (it.id == updated.id) updated else it }
                            onPotionsChange(newList)
                        },
                        onDelete = {
                            val newList = potions.filter { it.id != potion.id }
                            onPotionsChange(newList)
                        },
                        onEdit = {
                            if (state != null) {
                                state.activePotionConfig = potion
                                state.isPotionConfigOpen = true
                            }
                        },
                        onRoll = { formula, title, advantage ->
                            val isHealing = potion.type == PotionType.HEALING
                            onRoll(DiceRoller.roll(
                                title = title,
                                baseModifier = 0,
                                bonuses = listOf(SimpleBonus(formula = formula, name = if (isHealing) "Лечение" else "Урон")),
                                isDamage = !isHealing,
                                isHealing = isHealing,
                                stats = statsMap,
                                exhaustion = 0,
                                sourceType = RollSourceType.OTHER,
                                advantageType = advantage,
                                advantageLogic = state?.advantageLogic ?: AdvantageLogic.TOTAL
                            ))
                        },
                        isEditMode = isPotionsEditMode,
                        isDragging = isDragging,
                        isAnyItemDragging = draggingPotionId != null,
                        statsMap = statsMap,
                        hazeState = hazeState,
                        popupHazeState = popupHazeState,
                        blurCards = blurCards,
                        settingsViewModel = settingsViewModel,
                        state = state,
                        dragModifier = Modifier.draggableHandle()
                    )
                }
            }

            if (isPotionsEditMode) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (state != null) {
                                state.activePotionConfig = PotionState(name = "") // New potion
                                state.isPotionConfigOpen = true
                            }
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
                        Text("ДОБАВИТЬ", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (state != null) {
                                state.isPotionSelectionOpen = true
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.LibraryBooks, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ВЫБРАТЬ", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
