package ru.quasaris.characters.master.tabs.attacks

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
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import ru.quasaris.characters.master.ui.outerShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.LocalHazeStyle
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import ru.quasaris.characters.master.AttackEntry
import ru.quasaris.characters.master.Attribute
import ru.quasaris.characters.master.BonusOperation
import ru.quasaris.characters.master.MagicAttackType
import ru.quasaris.characters.master.SimpleBonus
import ru.quasaris.characters.master.SpellSettings
import ru.quasaris.characters.master.backend.*
import ru.quasaris.characters.master.ui.DeleteConfirmationDialog
import ru.quasaris.characters.master.ui.DiceRollAdvantagePopup
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
    settingsViewModel: SettingsViewModel? = null,
    spellSettings: SpellSettings = SpellSettings(),
    advantageLogic: AdvantageLogic = AdvantageLogic.TOTAL,
    onAttackConfigOpenChange: (Boolean) -> Unit = {},
    header: @Composable () -> Unit = {}
) {
    val currentHazeState = hazeState ?: remember { HazeState() }

    var editingAttack by remember { mutableStateOf<AttackEntry?>(null) }
    
    LaunchedEffect(editingAttack) {
        onAttackConfigOpenChange(editingAttack != null)
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
            onUpdateAttacks(items.toList())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().clipToBounds(),
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

                        AttackItem(
                            attack = attack,
                            isEditMode = isEditMode,
                            isDragging = isDragging,
                            isAnyItemDragging = reorderableState.isAnyItemDragging,
                            proficiencyBonus = proficiencyBonus,
                            attributeModifiers = attributeModifiers,
                            onClick = { if (!isEditMode) editingAttack = attack },
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

    editingAttack?.let { attack ->
        AttackConfigDialog(
            attack = attack,
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
                editingAttack = null
            },
            onDelete = { attackToDelete ->
                onUpdateAttacks(attacks.filter { it.id != attackToDelete.id })
                editingAttack = null
            },
            hazeState = hazeState,
            forceBlurEnabled = forceBlurEnabled,
            exhaustion = exhaustion,
            settingsViewModel = settingsViewModel,
            stats = stats,
            spellSettings = spellSettings
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AttackItem(
    attack: AttackEntry,
    isEditMode: Boolean = false,
    isDragging: Boolean = false,
    isAnyItemDragging: Boolean = false,
    proficiencyBonus: Int,
    attributeModifiers: Map<Attribute, Int>,
    onClick: () -> Unit,
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
    advantageLogic: AdvantageLogic = AdvantageLogic.TOTAL,
    settingsViewModel: SettingsViewModel? = null,
    collapseActionsOnEdit: Boolean = true
) {
    val colorScheme = MaterialTheme.colorScheme
    val internalHazeState = remember { HazeState() }
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
            val totalFlat = calculateTotalBonus(bonuses, stats, initialValue = baseFlat)

            val allDiceList = mutableListOf<DicePart>()
            bonuses.filter { it.isActive }.forEach { bonus ->
                val (_, fDice) = parseFormulaParts(bonus.formula, stats)
                fDice.forEach {
                    val sign = if (bonus.operation == BonusOperation.SUBTRACT) -1 else 1
                    allDiceList.add(DicePart(it.count * sign, it.sides))
                }
            }
            val finalDice = if (renderDiceInOrder) allDiceList else {
                allDiceList.groupBy { it.sides }.map { (sides, parts) -> DicePart(parts.sumOf { it.count }, sides) }.sortedBy { it.sides }
            }
            return@remember Triple(totalFlat, baseFlat, finalDice)
        }

        if (attack.attribute == Attribute.NONE) {
            return@remember Triple(0, 0, emptyList<DicePart>())
        }
        val attrMod = attributeModifiers[attack.attribute] ?: 0
        val prof = if (attack.isProficient) proficiencyBonus else 0
        val baseFlat = attrMod + prof + attack.attackBonus
        val totalFlat = calculateTotalBonus(attack.attackBonuses, stats, initialValue = baseFlat)

        val allDiceList = mutableListOf<DicePart>()
        attack.attackBonuses.filter { it.isActive }.forEach { bonus ->
            val (_, fDice) = parseFormulaParts(bonus.formula, stats)
            fDice.forEach {
                val sign = if (bonus.operation == BonusOperation.SUBTRACT) -1 else 1
                allDiceList.add(DicePart(it.count * sign, it.sides))
            }
        }
        val finalDice = if (renderDiceInOrder) allDiceList else {
            allDiceList.groupBy { it.sides }.map { (sides, parts) -> DicePart(parts.sumOf { it.count }, sides) }.sortedBy { it.sides }
        }
        Triple(totalFlat, baseFlat, finalDice)
    }

    val totalAttackBonus = attackCalculation.first
    val baseAttackBonus = attackCalculation.second
    val attackDice = attackCalculation.third

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
    var showInfo by remember { mutableStateOf(false) }
    var iconPosition by remember { mutableStateOf(Offset.Zero) }

    var showAttackPopup by remember { mutableStateOf(false) }
    var attackBtnSize by remember { mutableStateOf(IntSize.Zero) }

    var showDamagePopup by remember { mutableStateOf(false) }
    var damageBtnSize by remember { mutableStateOf(IntSize.Zero) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .then(
                if (backgroundBlur > 0.dp) 
                    Modifier.blur(backgroundBlur, edgeTreatment = BlurredEdgeTreatment.Unbounded) 
                else Modifier
            )
            .padding(padding)
            .outerShadow(
                shape = RoundedCornerShape(16.dp),
                blur = 6.dp,
                offsetY = 3.dp
            )
            .run {
                if (useHaze) {
                    val targetState = if (isDragging) (popupHazeState ?: hazeState!!) else hazeState!!
                    this.clip(RoundedCornerShape(16.dp))
                        .hazeEffect(
                            state = targetState,
                            style = HazeStyle(
                                blurRadius = 24.dp,
                                tints = listOf(HazeTint(colorScheme.surfaceContainer.copy(alpha = 0.6f)))
                            )
                        )
                } else this
            }
            .clickable(enabled = !isEditMode, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (useHaze) colorScheme.surfaceContainer.copy(alpha = 0.6f)
                            else colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().hazeSource(state = internalHazeState),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isEditMode) {
                Icon(
                    imageVector = Icons.Default.UnfoldMore,
                    contentDescription = "Drag",
                    modifier = Modifier
                        .padding(start = 12.dp, end = 4.dp)
                        .size(32.dp)
                        .then(dragModifier),
                    tint = colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = attack.name.ifBlank { "Безымянная атака" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    if (!isEditMode) {
                        Box {
                            IconButton(
                                onClick = { showInfo = true },
                                modifier = Modifier
                                    .size(32.dp)
                                    .onGloballyPositioned { coordinates ->
                                        iconPosition = coordinates.positionInWindow()
                                    }
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = "Описание",
                                    tint = colorScheme.primary.copy(alpha = 0.7f)
                                )
                            }

                            if (showInfo) {
                                val isOled = colorScheme.background == Color.Black

                                Dialog(
                                    onDismissRequest = { showInfo = false },
                                    properties = DialogProperties(
                                        usePlatformDefaultWidth = false,
                                        dismissOnBackPress = true,
                                        dismissOnClickOutside = true
                                    )
                                ) {
                                    val view = LocalView.current
                                    val window = (view.parent as? DialogWindowProvider)?.window

                                    LaunchedEffect(window) {
                                        window?.let { w ->
                                            w.addFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
                                            w.addFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
                                            w.addFlags(android.view.WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH)
                                            w.clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                                            w.setDimAmount(0f)

                                            val params = w.attributes
                                            params.width = android.view.WindowManager.LayoutParams.WRAP_CONTENT
                                            params.height = android.view.WindowManager.LayoutParams.WRAP_CONTENT
                                            params.gravity = android.view.Gravity.TOP or android.view.Gravity.END

                                            val screenWidth = view.context.resources.displayMetrics.widthPixels
                                            params.x = (screenWidth - iconPosition.x).toInt() + 8
                                            params.y = iconPosition.y.toInt() - 16

                                            w.attributes = params

                                            w.decorView.setOnTouchListener { _, event ->
                                                if (event.action == android.view.MotionEvent.ACTION_OUTSIDE) {
                                                    showInfo = false
                                                }
                                                false
                                            }

                                            if (!isOled && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                                w.setBackgroundBlurRadius(80)
                                            }
                                            w.setBackgroundDrawableResource(android.R.color.transparent)
                                        }
                                    }

                                    CompositionLocalProvider(LocalHazeStyle provides AttackInfoHazeStyle) {
                                        Surface(
                                            modifier = Modifier
                                                .padding(8.dp)
                                                .widthIn(max = 260.dp)
                                                .run {
                                                    if (blurPopups && hazeState != null && !isOled) {
                                                        this.clip(RoundedCornerShape(16.dp))
                                                            .hazeEffect(state = hazeState) {
                                                                inputScale = HazeInputScale.Fixed(0.6f)
                                                            }
                                                    } else this
                                                }
                                                .then(if (!isOled) Modifier.outerShadow(RoundedCornerShape(16.dp), blur = 6.dp, offsetY = 3.dp) else Modifier)
                                                .clickable { showInfo = false },
                                            shape = RoundedCornerShape(16.dp),
                                            color = if (isOled) Color.Black else colorScheme.surfaceContainerHigh.copy(alpha = if (blurPopups) 0.4f else 0.95f),
                                            tonalElevation = 0.dp,
                                            shadowElevation = 0.dp
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(
                                                    text = attack.name,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = colorScheme.primary,
                                                    modifier = Modifier.padding(bottom = 4.dp)
                                                )
                                                Text(
                                                    text = attack.notes.ifBlank { "Нет описания" },
                                                    fontSize = 14.sp,
                                                    lineHeight = 18.sp,
                                                    color = colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

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
            }

            if (isEditMode) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.padding(end = 4.dp)
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
}
