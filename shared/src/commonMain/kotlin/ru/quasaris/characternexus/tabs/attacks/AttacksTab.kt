package ru.quasaris.characternexus.tabs.attacks

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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

val AttackInfoHazeStyle = HazeStyle(
    blurRadius = 20.dp,
    tints = listOf(HazeTint(Color.Black.copy(alpha = 0.2f)))
)


@Composable
fun AttacksTab(
    attacks: List<AttackEntry>,
    proficiencyBonus: Int,
    attributeModifiers: Map<Attribute, Int>,
    onUpdateAttacks: (List<AttackEntry>) -> Unit,
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
    advantageLogic: AdvantageLogic = AdvantageLogic.TOTAL,
    onAttackConfigOpenChange: (Boolean) -> Unit = {},
    state: CharacterDetailState? = null,
    header: @Composable () -> Unit = {}
) {
    val currentHazeState = hazeState ?: remember { HazeState() }

    var editingAttack by remember { mutableStateOf<AttackEntry?>(null) }
    
    LaunchedEffect(editingAttack) {
        onAttackConfigOpenChange(editingAttack != null)
        state?.activeAttackConfigId = editingAttack?.id
        state?.editingAttack = editingAttack
        state?.isAttackConfigOpen = editingAttack != null
    }

    LaunchedEffect(state?.isAttackConfigOpen) {
        if (state?.isAttackConfigOpen == false) {
            editingAttack = null
        } else if (state?.editingAttack != null && editingAttack != state.editingAttack) {
            editingAttack = state.editingAttack
        }
    }

    var attackToDeleteIndex by remember { mutableStateOf<Int?>(null) }

    val listState = rememberLazyListState()
    val items = remember(attacks) { mutableStateListOf<AttackEntry>().apply { addAll(attacks) } }

    val collapseActionsOnEdit by settingsViewModel?.collapseActionsOnEdit?.collectAsState() ?: remember { mutableStateOf(true) }

    val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
        val fromIdx = from.index - 1
        val toIdx = to.index - 1
        if (fromIdx in items.indices && toIdx in items.indices) {
            items.add(toIdx, items.removeAt(fromIdx))
        }
    }

    LaunchedEffect(reorderableState.isAnyItemDragging) {
        if (!reorderableState.isAnyItemDragging) {
            onUpdateAttacks(items.toList())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().weight(1f).clipToBounds(),
                contentPadding = PaddingValues(top = 0.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { 
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        header() 
                    }
                }

                if (attacks.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            Text(
                                "Список атак пуст",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                } else {
                    itemsIndexed(items, key = { _, attack -> attack.id }) { index, attack ->
                        ReorderableItem(reorderableState, key = attack.id) { isDragging ->
                            val dragModifier = if (isEditMode) Modifier.draggableHandle() else Modifier

                            AttackCardItem(
                                attack = attack,
                                isEditMode = isEditMode,
                                isDragging = isDragging,
                                isAnyItemDragging = reorderableState.isAnyItemDragging,
                                proficiencyBonus = proficiencyBonus,
                                attributeModifiers = attributeModifiers,
                                onEdit = { editingAttack = attack },
                                onDelete = { attackToDeleteIndex = index },
                                onRoll = onRoll,
                                stats = stats,
                                exhaustion = exhaustion,
                                hazeState = currentHazeState,
                                popupHazeState = popupHazeState,
                                forceBlurEnabled = forceBlurEnabled,
                                blurPopups = blurPopups,
                                dragModifier = dragModifier,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .animateItem(),
                                spellSettings = spellSettings,
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
            onClick = { editingAttack = AttackEntry() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Добавить атаку")
        }
    }

    DeleteConfirmationDialog(
        showDialog = attackToDeleteIndex != null,
        onDismiss = { attackToDeleteIndex = null },
        onConfirm = {
            attackToDeleteIndex?.let { index ->
                if (index in items.indices) {
                    items.removeAt(index)
                    onUpdateAttacks(items.toList())
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
            onDismiss = { editingAttack = null },
            onSave = { updatedAttack ->
                val newAttacks = if (attacks.any { it.id == updatedAttack.id }) {
                    attacks.map { if (it.id == updatedAttack.id) updatedAttack else it }
                } else {
                    attacks + updatedAttack
                }
                onUpdateAttacks(newAttacks)
            },
            onDelete = { attackToDelete ->
                onUpdateAttacks(attacks.filter { it.id != attackToDelete.id })
                editingAttack = null
            },
            forceBlurEnabled = forceBlurEnabled,
            exhaustion = exhaustion,
            settingsViewModel = settingsViewModel,
            stats = stats,
            spellSettings = spellSettings
        )
    }
}



