package ru.quasaris.characternexus.tabs.attacks

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.backend.*
import ru.quasaris.characternexus.ui.outerShadow
import ru.quasaris.characternexus.ui.DiceRollAdvantagePopup
import ru.quasaris.characternexus.tabs.spells.SpellCardItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AttackCardItem(
    attack: AttackEntry,
    isEditMode: Boolean = false,
    isDragging: Boolean = false,
    isAnyItemDragging: Boolean = false,
    proficiencyBonus: Int,
    attributeModifiers: Map<Attribute, Int>,
    onEdit: () -> Unit,
    onDelete: () -> Unit = {},
    onRoll: (RollResult) -> Unit = {},
    stats: Map<String, String> = emptyMap(),
    exhaustion: Int = 0,
    hazeState: HazeState? = null,
    popupHazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false,
    blurPopups: Boolean = false,
    dragModifier: Modifier = Modifier,
    modifier: Modifier = Modifier,
    spellSettings: SpellSettings = SpellSettings(),
    spellbookManager: SpellbookManager? = null,
    advantageLogic: AdvantageLogic = AdvantageLogic.TOTAL,
    settingsViewModel: SettingsViewModel? = null,
    collapseActionsOnEdit: Boolean = true
) {
    val colorScheme = MaterialTheme.colorScheme
    val blurCards by settingsViewModel?.blurCards?.collectAsState() ?: remember { mutableStateOf(true) }

    val scale by animateFloatAsState(
        targetValue = when {
            isDragging -> 1.02f
            isEditMode -> 0.95f
            else -> 1f
        },
        label = "dragScale"
    )

    val backgroundBlur by animateDpAsState(
        targetValue = if (isAnyItemDragging && !isDragging) 6.dp else 0.dp,
        label = "backgroundBlur"
    )

    val padding by animateDpAsState(targetValue = if (isEditMode) 8.dp else 0.dp, label = "padding")
    val renderDiceInOrder by settingsViewModel?.renderDiceInOrder?.collectAsState() ?: remember { mutableStateOf(true) }

    val attackCalculation = remember(attack, proficiencyBonus, attributeModifiers, exhaustion, stats, spellSettings, renderDiceInOrder) {
        if (attack.isMagic) {
            val bonuses = if (attack.magicType == MagicAttackType.ATTACK) spellSettings.spellAttackBonuses else spellSettings.spellSaveDcBonuses
            val abilityModifier = if (spellSettings.spellcastingAbility != Attribute.NONE) {
                attributeModifiers[spellSettings.spellcastingAbility] ?: 0
            } else 0

            val baseFlat = (if (attack.magicType == MagicAttackType.SAVE) 8 else 0) + proficiencyBonus + abilityModifier
            val (totalFlat, finalDice) = calculateAttackFormulaParts(
                baseFlat = baseFlat,
                bonuses = bonuses,
                stats = stats,
                renderInOrder = renderDiceInOrder
            )
            return@remember Triple(totalFlat, baseFlat, finalDice)
        }

        if (attack.attribute == Attribute.NONE) {
            return@remember Triple(0, 0, emptyList<DicePart>())
        }
        val attrMod = attributeModifiers[attack.attribute] ?: 0
        val prof = if (attack.isProficient) proficiencyBonus else 0
        val baseFlat = attrMod + prof + attack.attackBonus

        val (totalFlat, finalDice) = calculateAttackFormulaParts(
            baseFlat = baseFlat,
            bonuses = attack.attackBonuses,
            stats = stats,
            renderInOrder = renderDiceInOrder
        )
        Triple(totalFlat, baseFlat, finalDice)
    }

    val totalAttackBonus = attackCalculation.first
    val baseAttackBonus = attackCalculation.second
    val attackDice = attackCalculation.third

    val resolvedSpellCard = remember(attack.spellCard, attack.spellId, spellbookManager) {
        attack.spellCard ?: attack.spellId?.let { id -> spellbookManager?.loadSpells()?.find { it.matchesId(id) } }
    }

    if (attack.isMagic && resolvedSpellCard != null) {
        val isPrepared = remember(attack.spellId, resolvedSpellCard, spellSettings) {
            val preparedSet = if (spellSettings.isSpellbookEnabled) {
                spellSettings.preparedSpellIds.toSet()
            } else {
                (spellSettings.preparedSpellIds + spellSettings.selectedSpellIds).toSet()
            }
            preparedSet.any { preparedId ->
                (attack.spellId != null && preparedId == attack.spellId) || resolvedSpellCard.matchesId(preparedId)
            }
        }

        var isSpellExpanded by remember { mutableStateOf(false) }

        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isEditMode) {
                Icon(
                    imageVector = Icons.Default.UnfoldMore,
                    contentDescription = "Drag",
                    modifier = Modifier
                        .padding(start = 4.dp, end = 4.dp)
                        .size(32.dp)
                        .then(dragModifier),
                    tint = colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                SpellCardItem(
                    spell = resolvedSpellCard,
                    isExpanded = isSpellExpanded,
                    onToggleExpand = { isSpellExpanded = !isSpellExpanded },
                    onEdit = onEdit,
                    onRollDamage = { formula, title, advantage ->
                        onRoll(DiceRoller.roll(
                            title = title,
                            baseModifier = 0,
                            bonuses = listOf(SimpleBonus(formula = formula, name = "Урон")),
                            isDamage = true,
                            stats = stats,
                            exhaustion = 0,
                            sourceType = RollSourceType.ATTACK,
                            advantageType = advantage,
                            advantageLogic = advantageLogic
                        ))
                    },
                    onRollAttack = { advantage ->
                        val bonuses = spellSettings.spellAttackBonuses
                        val abilityModifier = if (spellSettings.spellcastingAbility != Attribute.NONE) {
                            attributeModifiers[spellSettings.spellcastingAbility] ?: 0
                        } else 0
                        val baseFlat = proficiencyBonus + abilityModifier
                        onRoll(DiceRoller.roll(
                            title = "Магическая атака: ${resolvedSpellCard.name}",
                            baseModifier = baseFlat,
                            bonuses = bonuses,
                            isDamage = false,
                            stats = stats,
                            exhaustion = exhaustion,
                            sourceType = RollSourceType.ATTACK,
                            advantageType = advantage,
                            advantageLogic = advantageLogic
                        ))
                    },
                    isEditable = isEditMode,
                    statsMap = stats,
                    characterLevel = stats["level"]?.toIntOrNull() ?: 1,
                    onLongClick = onEdit,
                    spellAttackBonus = totalAttackBonus,
                    spellAttackDice = attackDice,
                    spellSaveDc = totalAttackBonus,
                    spellSaveDice = attackDice,
                    hazeState = hazeState,
                    popupHazeState = popupHazeState,
                    forceBlurEnabled = forceBlurEnabled,
                    isEditMode = isEditMode,
                    collapseOnEdit = collapseActionsOnEdit,
                    isDragging = isDragging,
                    isAnyItemDragging = isAnyItemDragging,
                    isGrayedOut = !isPrepared,
                    settingsViewModel = settingsViewModel
                )
            }

            if (isEditMode) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Delete",
                        tint = colorScheme.error
                    )
                }
            }
        }
        return
    }

    val displayAttackBonus = if (attack.isMagic && attack.magicType == MagicAttackType.SAVE) totalAttackBonus else totalAttackBonus - (exhaustion * 2)
    val isHealing = attack.damageType.lowercase().contains("лечение") || attack.damageType.lowercase().contains("healing")

    val fullDamageText = remember(attack.damageFormula, attack.damageBonus, attack.damageBonuses, stats, renderDiceInOrder) {
        formatFullDamage(
            baseFormula = attack.damageFormula,
            baseDamageBonus = attack.damageBonus,
            bonuses = attack.damageBonuses,
            stats = stats,
            renderInOrder = renderDiceInOrder
        )
    }

    val useHaze = hazeState != null && blurCards

    var showAttackPopup by remember { mutableStateOf(false) }
    var attackBtnSize by remember { mutableStateOf(IntSize.Zero) }

    var showDamagePopup by remember { mutableStateOf(false) }
    var damageBtnSize by remember { mutableStateOf(IntSize.Zero) }

    var isExpanded by remember { mutableStateOf(false) }

    val veryResponsive by settingsViewModel?.veryResponsiveHaptics?.collectAsState() ?: remember { mutableStateOf(true) }
    fun performClickHaptic() {
        if (veryResponsive) {
            ru.quasaris.characternexus.util.PlatformUtils.performHapticFeedback(ru.quasaris.characternexus.util.HapticType.CLICK)
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isEditMode) {
            Icon(
                imageVector = Icons.Default.UnfoldMore,
                contentDescription = "Drag",
                modifier = Modifier
                    .padding(start = 4.dp, end = 4.dp)
                    .size(32.dp)
                    .then(dragModifier),
                tint = colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(scale)
                    .then(
                        if (backgroundBlur > 0.dp) 
                            Modifier.blur(backgroundBlur) 
                        else Modifier
                    )
                    .padding(padding)
                    .outerShadow(
                        shape = RoundedCornerShape(16.dp),
                        blur = 6.dp,
                        offsetY = 3.dp
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .run {
                        if (useHaze) {
                            this.hazeEffect(
                                state = hazeState!!,
                                style = HazeStyle(
                                    blurRadius = 24.dp,
                                    tints = listOf(HazeTint(colorScheme.surfaceContainer.copy(alpha = 0.6f)))
                                )
                            )
                        } else this
                    }
                    .combinedClickable(
                        enabled = !isEditMode,
                        onClick = {
                            performClickHaptic()
                            isExpanded = !isExpanded
                        },
                        onLongClick = {
                            performClickHaptic()
                            onEdit()
                        }
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = if (useHaze) colorScheme.surfaceContainer.copy(alpha = 0.6f)
                                    else colorScheme.surfaceContainer
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = null
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = attack.name.ifBlank { "Безымянная атака" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurface
                            )
                            AttackBadgesRow(attack = attack, modifier = Modifier.padding(top = 2.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (!isEditMode || !collapseActionsOnEdit) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .onGloballyPositioned { coords -> damageBtnSize = coords.size }
                                    .outerShadow(
                                        shape = RoundedCornerShape(8.dp),
                                        blur = 2.dp,
                                        offsetY = 1.dp
                                    )
                                    .combinedClickable(
                                        onClick = {
                                            onRoll(DiceRoller.roll(
                                                title = (if (isHealing) "Лечение: " else "Урон: ") + attack.name,
                                                baseModifier = attack.damageBonus,
                                                bonuses = (attack.damageBonuses + SimpleBonus(formula = attack.damageFormula, name = "Базовый урон")),
                                                isDamage = !isHealing,
                                                isHealing = isHealing,
                                                stats = stats,
                                                sourceType = RollSourceType.ATTACK,
                                                advantageType = AdvantageType.NONE,
                                                advantageLogic = advantageLogic
                                            ))
                                        },
                                        onLongClick = { showDamagePopup = true }
                                    ),
                                color = if (isHealing) Color(0xFF00C46F).copy(alpha = 0.12f) else colorScheme.primary.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = (if (isHealing) "Лечение: " else "") + "$fullDamageText ${attack.damageType}".trim(),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colorScheme.onSurface
                                    )

                                    if (showDamagePopup) {
                                        val density = LocalDensity.current
                                        val sizeDp = with(density) { damageBtnSize.toSize().let { androidx.compose.ui.unit.DpSize((it.width / density.density).dp, (it.height / density.density).dp) } }
                                        DiceRollAdvantagePopup(
                                            onAdvantage = {
                                                onRoll(DiceRoller.roll(
                                                    title = (if (isHealing) "Лечение: " else "Урон: ") + attack.name,
                                                    baseModifier = attack.damageBonus,
                                                    bonuses = (attack.damageBonuses + SimpleBonus(formula = attack.damageFormula, name = "Базовый урон")),
                                                    isDamage = !isHealing,
                                                    isHealing = isHealing,
                                                    stats = stats,
                                                    sourceType = RollSourceType.ATTACK,
                                                    advantageType = AdvantageType.ADVANTAGE,
                                                    advantageLogic = advantageLogic
                                                ))
                                            },
                                            onDisadvantage = {
                                                onRoll(DiceRoller.roll(
                                                    title = (if (isHealing) "Лечение: " else "Урон: ") + attack.name,
                                                    baseModifier = attack.damageBonus,
                                                    bonuses = (attack.damageBonuses + SimpleBonus(formula = attack.damageFormula, name = "Базовый урон")),
                                                    isDamage = !isHealing,
                                                    isHealing = isHealing,
                                                    stats = stats,
                                                    sourceType = RollSourceType.ATTACK,
                                                    advantageType = AdvantageType.DISADVANTAGE,
                                                    advantageLogic = advantageLogic
                                                ))
                                            },
                                            onCritical = {
                                                onRoll(DiceRoller.roll(
                                                    title = (if (isHealing) "Критическое Лечение: " else "Критический Урон: ") + attack.name,
                                                    baseModifier = attack.damageBonus,
                                                    bonuses = (attack.damageBonuses + SimpleBonus(formula = attack.damageFormula, name = "Базовый урон")),
                                                    isDamage = !isHealing,
                                                    isHealing = isHealing,
                                                    stats = stats,
                                                    sourceType = RollSourceType.ATTACK,
                                                    advantageType = AdvantageType.CRITICAL,
                                                    advantageLogic = advantageLogic
                                                ))
                                            },
                                            onDismiss = { showDamagePopup = false },
                                            hazeState = popupHazeState ?: hazeState,
                                            isOled = colorScheme.background == Color.Black,
                                            modifier = Modifier.size(sizeDp)
                                        )
                                    }
                                }
                            }

                            if (attack.isMagic && attack.magicType == MagicAttackType.SAVE) {
                                Surface(
                                    modifier = Modifier.outerShadow(
                                        shape = RoundedCornerShape(8.dp),
                                        blur = 2.dp,
                                        offsetY = 1.dp
                                    ),
                                    color = colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "СЛОЖНОСТЬ",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colorScheme.primary,
                                            fontSize = 10.sp
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = totalAttackBonus.toString(),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = colorScheme.onSurface
                                            )
                                            if (attackDice.isNotEmpty()) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    attackDice.forEach { DiceIcon(it) }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else if (attack.isMagic || attack.attribute != Attribute.NONE) {
                                Surface(
                                    modifier = Modifier
                                        .onGloballyPositioned { coords -> attackBtnSize = coords.size }
                                        .outerShadow(
                                            shape = RoundedCornerShape(8.dp),
                                            blur = 2.dp,
                                            offsetY = 1.dp
                                        )
                                        .combinedClickable(
                                            onClick = {
                                                val bonuses = if (attack.isMagic) spellSettings.spellAttackBonuses else attack.attackBonuses
                                                onRoll(DiceRoller.roll(
                                                    title = if (attack.isMagic) "Магическая атака: ${attack.name}" else "Атака: ${attack.name}",
                                                    baseModifier = baseAttackBonus,
                                                    bonuses = bonuses,
                                                    isDamage = false,
                                                    stats = stats,
                                                    exhaustion = exhaustion,
                                                    sourceType = RollSourceType.ATTACK,
                                                    advantageType = AdvantageType.NONE,
                                                    advantageLogic = advantageLogic
                                                ))
                                            },
                                            onLongClick = { showAttackPopup = true }
                                        ),
                                    color = colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            AttackBonusIndicator(
                                                bonus = displayAttackBonus,
                                                dice = attackDice,
                                                size = 48.dp,
                                                fontSize = 18.sp,
                                                showLabel = false,
                                                showDice = true
                                            )
                                        }

                                        if (showAttackPopup) {
                                            val density = LocalDensity.current
                                            val sizeDp = with(density) { attackBtnSize.toSize().let { androidx.compose.ui.unit.DpSize((it.width / density.density).dp, (it.height / density.density).dp) } }
                                            DiceRollAdvantagePopup(
                                                onAdvantage = {
                                                    val bonuses = if (attack.isMagic) spellSettings.spellAttackBonuses else attack.attackBonuses
                                                    onRoll(DiceRoller.roll(
                                                        title = if (attack.isMagic) "Магическая атака: ${attack.name}" else "Атака: ${attack.name}",
                                                        baseModifier = baseAttackBonus,
                                                        bonuses = bonuses,
                                                        isDamage = false,
                                                        stats = stats,
                                                        exhaustion = exhaustion,
                                                        sourceType = RollSourceType.ATTACK,
                                                        advantageType = AdvantageType.ADVANTAGE,
                                                        advantageLogic = advantageLogic
                                                    ))
                                                },
                                                onDisadvantage = {
                                                    val bonuses = if (attack.isMagic) spellSettings.spellAttackBonuses else attack.attackBonuses
                                                    onRoll(DiceRoller.roll(
                                                        title = if (attack.isMagic) "Магическая атака: ${attack.name}" else "Атака: ${attack.name}",
                                                        baseModifier = baseAttackBonus,
                                                        bonuses = bonuses,
                                                        isDamage = false,
                                                        stats = stats,
                                                        exhaustion = exhaustion,
                                                        sourceType = RollSourceType.ATTACK,
                                                        advantageType = AdvantageType.DISADVANTAGE,
                                                        advantageLogic = advantageLogic
                                                    ))
                                                },
                                                onDismiss = { showAttackPopup = false },
                                                hazeState = popupHazeState ?: hazeState,
                                                isOled = colorScheme.background == Color.Black,
                                                modifier = Modifier.size(sizeDp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = isExpanded && (attack.notes.isNotEmpty() || attack.weaponMasteries.any { it.name.isNotBlank() || it.description.isNotBlank() }),
                        enter = expandIn(expandFrom = Alignment.TopStart) + fadeIn(),
                        exit = shrinkOut(shrinkTowards = Alignment.TopStart) + fadeOut()
                    ) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            if (attack.notes.isNotEmpty()) {
                                Text(
                                    text = attack.notes,
                                    fontSize = 14.sp,
                                    lineHeight = 18.sp,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                            WeaponMasteriesSection(masteries = attack.weaponMasteries)
                        }
                    }
                }
            }
        }

        if (isEditMode) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Delete",
                    tint = colorScheme.error
                )
            }
        }
    }
}
