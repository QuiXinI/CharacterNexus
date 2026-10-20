package ru.quasaris.characternexus.tabs.attacks

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import ru.quasaris.characternexus.ui.outerShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.graphics.graphicsLayer
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.LocalHazeStyle
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import ru.quasaris.characternexus.ui.theme.rememberEffectiveBlurRadius
import ru.quasaris.characternexus.ui.theme.hazePopover
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.backend.*
import ru.quasaris.characternexus.ui.CharacterDetailState
import ru.quasaris.characternexus.tabs.attacks.calculateAttackFormulaParts
import ru.quasaris.characternexus.ui.DeleteConfirmationDialog
import ru.quasaris.characternexus.ui.DiceRollAdvantagePopup
import ru.quasaris.characternexus.ui.TabControlHeader
import sh.calvin.reorderable.*

import ru.quasaris.characternexus.backend.SpellbookManager

val AttackInfoHazeStyle = HazeStyle(
    blurRadius = 20.dp,
    tints = listOf(HazeTint(Color.Black.copy(alpha = 0.2f)))
)


@Composable
fun AttacksTab(
    attacks: List<AttackEntry>,
    tabAttacks: List<AttackEntry>,
    attacksTabIds: List<String>,
    onUpdateAttacks: (List<AttackEntry>) -> Unit,
    onUpdateAttacksTabIds: (List<String>) -> Unit,
    proficiencyBonus: Int,
    attributeModifiers: Map<Attribute, Int>,
    onRoll: (RollResult) -> Unit = {},
    stats: Map<String, String> = emptyMap(),
    exhaustion: Int = 0,
    hazeState: HazeState? = null,
    popupHazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false,
    blurPopups: Boolean = false,
    isEditMode: Boolean = false,
    onToggleEditMode: () -> Unit = {},
    settingsViewModel: SettingsViewModel? = null,
    spellSettings: SpellSettings = SpellSettings(),
    spellbookManager: SpellbookManager? = null,
    advantageLogic: AdvantageLogic = AdvantageLogic.TOTAL,
    onAttackConfigOpenChange: (Boolean) -> Unit = {},
    state: CharacterDetailState? = null,
    isDesktop: Boolean = false,
    isSingleColumn: Boolean = false,
    header: @Composable () -> Unit = {}
) {
    val currentHazeState = hazeState ?: remember { HazeState() }

    var editingAttack by remember { mutableStateOf<AttackEntry?>(null) }
    var editingAttackIndex by remember { mutableStateOf(-1) }
    
    LaunchedEffect(editingAttack) {
        onAttackConfigOpenChange(editingAttack != null)
        state?.activeAttackConfigId = editingAttack?.id
        state?.editingAttack = editingAttack
        state?.isAttackConfigOpen = editingAttack != null
    }

    LaunchedEffect(state?.isAttackConfigOpen) {
        if (state?.isAttackConfigOpen == false) {
            editingAttack = null
            editingAttackIndex = -1
        } else if (state?.editingAttack != null && editingAttack != state.editingAttack) {
            editingAttack = state.editingAttack
        }
    }

    var attackToDeleteIndex by remember { mutableStateOf<Int?>(null) }

    val gridState = rememberLazyStaggeredGridState()
    val items = remember(tabAttacks) { mutableStateListOf<AttackEntry>().apply { addAll(tabAttacks) } }

    LaunchedEffect(tabAttacks) {
        if (items != tabAttacks) {
            items.clear()
            items.addAll(tabAttacks)
        }
    }

    val collapseActionsOnEdit by settingsViewModel?.collapseActionsOnEdit?.collectAsState() ?: remember { mutableStateOf(true) }

    val reorderableState = rememberReorderableLazyStaggeredGridState(gridState) { from, to ->
        val fromIdx = from.index - 1
        val toIdx = to.index - 1
        if (fromIdx in items.indices && toIdx in items.indices) {
            items.add(toIdx, items.removeAt(fromIdx))
        }
    }

    LaunchedEffect(reorderableState.isAnyItemDragging) {
        if (!reorderableState.isAnyItemDragging) {
            onUpdateAttacksTabIds(items.map { it.id })
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            val columns = if (isDesktop && !isSingleColumn) {
                StaggeredGridCells.Adaptive(minSize = 340.dp)
            } else {
                StaggeredGridCells.Fixed(1)
            }

            LazyVerticalStaggeredGrid(
                columns = columns,
                state = gridState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clipToBounds(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
                verticalItemSpacing = 12.dp,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(span = StaggeredGridItemSpan.FullLine) { 
                    Box(modifier = Modifier.padding(horizontal = 4.dp)) {
                        header() 
                    }
                }

                if (tabAttacks.isEmpty()) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            Text(
                                "Список атак пуст",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                } else {
                    itemsIndexed(items, key = { index, attack -> "${attack.id}_$index" }) { index, attack ->
                        ReorderableItem(reorderableState, key = "${attack.id}_$index") { isDragging ->
                            val dragModifier = if (isEditMode) Modifier.draggableHandle() else Modifier

                            AttackCardItem(
                                attack = attack,
                                isEditMode = isEditMode,
                                isDragging = isDragging,
                                isAnyItemDragging = reorderableState.isAnyItemDragging,
                                proficiencyBonus = proficiencyBonus,
                                attributeModifiers = attributeModifiers,
                                onEdit = {
                                    editingAttack = attack.resolve(attacks)
                                    editingAttackIndex = index
                                },
                                onDelete = { attackToDeleteIndex = index },
                                onRoll = onRoll,
                                stats = stats,
                                exhaustion = exhaustion,
                                hazeState = currentHazeState,
                                popupHazeState = popupHazeState,
                                forceBlurEnabled = forceBlurEnabled,
                                blurPopups = blurPopups,
                                dragModifier = dragModifier,
                                modifier = Modifier.animateItem(),
                                spellSettings = spellSettings,
                                spellbookManager = spellbookManager,
                                advantageLogic = advantageLogic,
                                settingsViewModel = settingsViewModel,
                                collapseActionsOnEdit = collapseActionsOnEdit
                            )
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { 
                val newAttack = AttackEntry()
                onUpdateAttacks(attacks + newAttack)
                onUpdateAttacksTabIds(attacksTabIds + newAttack.id)
                editingAttack = newAttack 
                editingAttackIndex = attacksTabIds.size
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Добавить атаку")
        }
    }

    ru.quasaris.characternexus.ui.AttackDeleteDialog(
        showDialog = attackToDeleteIndex != null,
        onDismiss = { attackToDeleteIndex = null },
        onRemoveFromTab = {
            attackToDeleteIndex?.let { index ->
                if (index in items.indices) {
                    val attackToRemove = items[index]
                    onUpdateAttacksTabIds(attacksTabIds.filterIndexed { idx, _ -> idx != index })
                }
            }
            attackToDeleteIndex = null
        },
        onDeleteCompletely = {
            attackToDeleteIndex?.let { index ->
                if (index in items.indices) {
                    val attackToDelete = items[index]
                    if (state != null) {
                        state.deleteAttackCompletely(attackToDelete.id)
                    } else {
                        val newTabIds = attacksTabIds.filterIndexed { idx, _ -> idx != index }
                        onUpdateAttacksTabIds(newTabIds)
                        if (!newTabIds.contains(attackToDelete.id)) {
                            onUpdateAttacks(attacks.filter { it.id != attackToDelete.id })
                        }
                    }
                }
            }
            attackToDeleteIndex = null
        },
        settingsViewModel = settingsViewModel
    )

    if (editingAttack != null && state == null) {
        AttackConfigDialog(
            attack = editingAttack!!,
            proficiencyBonus = proficiencyBonus,
            attributeModifiers = attributeModifiers,
            onDismiss = {
                editingAttack = null
                editingAttackIndex = -1
            },
            onSave = { updatedAttack ->
                val newAttacks = if (updatedAttack.linkedTo != null) {
                    val primaryId = updatedAttack.linkedTo
                    attacks.map { existing ->
                        if (existing.id == primaryId) {
                            updatedAttack.copy(
                                id = existing.id,
                                linkedTo = existing.linkedTo,
                                linkedIds = existing.linkedIds
                            )
                        } else if (existing.id == updatedAttack.id) {
                            updatedAttack
                        } else {
                            existing
                        }
                    }
                } else if (attacks.any { it.id == updatedAttack.id }) {
                    attacks.map { if (it.id == updatedAttack.id) updatedAttack else it }
                } else {
                    attacks + updatedAttack
                }
                onUpdateAttacks(newAttacks)
                if (!attacksTabIds.contains(updatedAttack.id)) {
                    onUpdateAttacksTabIds(attacksTabIds + updatedAttack.id)
                }
            },
            onDelete = { attackToDelete ->
                val newTabIds = attacksTabIds.filter { it != attackToDelete.id }
                onUpdateAttacksTabIds(newTabIds)
                onUpdateAttacks(attacks.filter { it.id != attackToDelete.id })
                editingAttack = null
                editingAttackIndex = -1
            },
            existingAttacks = attacks,
            attacksTabIds = attacksTabIds,
            editingIndex = editingAttackIndex,
            onLink = { targetId ->
                val currentAtkId = editingAttack?.id
                if (currentAtkId != null && currentAtkId != targetId) {
                    val newAttacks = attacks.linkAttacks(secondaryId = currentAtkId, primaryId = targetId)
                    onUpdateAttacks(newAttacks)
                    val updatedAtk = newAttacks.find { it.id == currentAtkId }?.resolve(newAttacks)
                    if (updatedAtk != null) {
                        editingAttack = updatedAtk
                    }
                }
            },
            onUnlink = {
                val currentAtk = editingAttack
                if (currentAtk != null) {
                    val targetId = currentAtk.id
                    val newAttacks = attacks.unlinkAttack(targetId)
                    onUpdateAttacks(newAttacks)
                    val unlinkedAtk = newAttacks.find { it.id == targetId }
                    if (unlinkedAtk != null) {
                        editingAttack = unlinkedAtk
                    }
                }
            },
            forceBlurEnabled = forceBlurEnabled,
            exhaustion = exhaustion,
            settingsViewModel = settingsViewModel,
            stats = stats,
            spellSettings = spellSettings,
            spellbookManager = spellbookManager
        )
    }
}



