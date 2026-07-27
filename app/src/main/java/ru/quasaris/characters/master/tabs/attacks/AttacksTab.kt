package ru.quasaris.characters.master.tabs.attacks

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import ru.quasaris.characters.master.AttackEntry
import ru.quasaris.characters.master.Attribute
import ru.quasaris.characters.master.MagicAttackType
import ru.quasaris.characters.master.backend.AdvantageType
import ru.quasaris.characters.master.backend.DiceRoller
import ru.quasaris.characters.master.backend.RollResult
import ru.quasaris.characters.master.backend.RollSourceType
import ru.quasaris.characters.master.backend.SettingsViewModel
import ru.quasaris.characters.master.ui.DeleteConfirmationDialog
import ru.quasaris.characters.master.ui.DiceRollAdvantagePopup
import androidx.compose.foundation.shape.RoundedCornerShape
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.HazeStyle

import androidx.compose.ui.draw.clip
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.LocalHazeStyle
import androidx.compose.runtime.CompositionLocalProvider
import ru.quasaris.characters.master.tabs.attacks.AttackBonusIndicator
import ru.quasaris.characters.master.tabs.attacks.DiceIcon
import kotlin.math.roundToInt

/**
 * Стиль размытия для инфо-панелей атак.
 */
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
    forceBlurEnabled: Boolean = false,
    blurPopups: Boolean = false,
    isEditMode: Boolean = false,
    settingsViewModel: SettingsViewModel? = null,
    spellSettings: ru.quasaris.characters.master.SpellSettings = ru.quasaris.characters.master.SpellSettings()
) {
    var editingAttack by remember { mutableStateOf<AttackEntry?>(null) }
    var attackToDeleteIndex by remember { mutableStateOf<Int?>(null) }

    val listState = rememberLazyListState()
    val items = remember(attacks) { mutableStateListOf<AttackEntry>().apply { addAll(attacks) } }
    var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
    var draggingOffset by remember { mutableStateOf(0f) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        if (attacks.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Список атак пуст", 
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(items, key = { _, attack -> attack.id }) { index, attack ->
                    val isDragging = draggedItemIndex == index
                    
                    val dragModifier = if (isEditMode) {
                        Modifier.pointerInput(index) {
                            detectDragGestures(
                                onDragStart = { 
                                    draggedItemIndex = index
                                    draggingOffset = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    draggingOffset += dragAmount.y
                                    
                                    val layoutInfo = listState.layoutInfo
                                    val draggedItemInfo = layoutInfo.visibleItemsInfo.find { it.index == index }
                                    
                                    if (draggedItemInfo != null) {
                                        val currentCenter = draggedItemInfo.offset + (draggedItemInfo.size / 2) + draggingOffset.toInt()
                                        
                                        val targetItem = layoutInfo.visibleItemsInfo.find { item ->
                                            item.index != index && currentCenter in item.offset..(item.offset + item.size)
                                        }

                                        if (targetItem != null) {
                                            val targetIndex = targetItem.index
                                            if (targetIndex in items.indices) {
                                                items.add(targetIndex, items.removeAt(index))
                                                draggingOffset += (draggedItemInfo.offset - targetItem.offset)
                                                draggedItemIndex = targetIndex
                                                onUpdateAttacks(items.toList())
                                            }
                                        }
                                    }
                                },
                                onDragEnd = {
                                    draggedItemIndex = null
                                    draggingOffset = 0f
                                },
                                onDragCancel = {
                                    draggedItemIndex = null
                                    draggingOffset = 0f
                                }
                            )
                        }
                    } else Modifier

                    AttackItem(
                        attack = attack,
                        isEditMode = isEditMode,
                        isDragging = isDragging,
                        proficiencyBonus = proficiencyBonus,
                        attributeModifiers = attributeModifiers,
                        onClick = { if (!isEditMode) editingAttack = attack },
                        onDelete = {
                            attackToDeleteIndex = index
                        },
                        onRoll = onRoll,
                        stats = stats,
                        exhaustion = exhaustion,
                        hazeState = hazeState,
                        forceBlurEnabled = forceBlurEnabled,
                        blurPopups = blurPopups,
                        dragModifier = dragModifier,
                        modifier = Modifier.animateItem(),
                        spellSettings = spellSettings
                    )
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
    proficiencyBonus: Int,
    attributeModifiers: Map<Attribute, Int>,
    onClick: () -> Unit,
    onDelete: () -> Unit = {},
    onRoll: (RollResult) -> Unit = {},
    stats: Map<String, String> = emptyMap(),
    exhaustion: Int = 0,
    hazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false,
    blurPopups: Boolean = false,
    dragModifier: Modifier = Modifier,
    modifier: Modifier = Modifier,
    spellSettings: ru.quasaris.characters.master.SpellSettings = ru.quasaris.characters.master.SpellSettings()
) {
    val scale by animateFloatAsState(targetValue = if (isEditMode) 0.95f else 1f)
    val padding by animateDpAsState(targetValue = if (isEditMode) 8.dp else 0.dp)
    
    val attackCalculation = remember(attack, proficiencyBonus, attributeModifiers, exhaustion, stats, spellSettings) {
        if (attack.isMagic) {
            val bonuses = if (attack.magicType == MagicAttackType.ATTACK) spellSettings.spellAttackBonuses else spellSettings.spellSaveDcBonuses
            
            val abilityModifier = if (spellSettings.spellcastingAbility != Attribute.NONE) {
                val statKey = when (spellSettings.spellcastingAbility) {
                    Attribute.STRENGTH -> "strength"
                    Attribute.DEXTERITY -> "dexterity"
                    Attribute.CONSTITUTION -> "constitution"
                    Attribute.INTELLIGENCE -> "intelligence"
                    Attribute.WISDOM -> "wisdom"
                    Attribute.CHARISMA -> "charisma"
                    else -> ""
                }
                ru.quasaris.characters.master.backend.calculateModifier(stats[statKey] ?: "10")
            } else 0
            
            var totalFlat = (if (attack.magicType == MagicAttackType.SAVE) 8 else 0) + proficiencyBonus + abilityModifier - (if (attack.magicType == MagicAttackType.ATTACK) exhaustion * 2 else 0)
            val allDice = mutableMapOf<Int, Int>()
            
            bonuses.forEach { bonus ->
                val (fFlat, fDice) = parseFormulaParts(bonus.formula, attributeModifiers, proficiencyBonus, stats)
                totalFlat += fFlat
                fDice.forEach { allDice[it.sides] = (allDice[it.sides] ?: 0) + it.count }
            }
            return@remember Pair(totalFlat, allDice.map { DicePart(it.value, it.key) }.sortedBy { it.sides })
        }

        if (attack.attribute == Attribute.NONE) {
            return@remember Pair(0, emptyList<DicePart>())
        }
        val attrMod = attributeModifiers[attack.attribute] ?: 0
        val prof = if (attack.isProficient) proficiencyBonus else 0
        var totalFlat = attrMod + prof + attack.attackBonus - (exhaustion * 2)
        val allDice = mutableMapOf<Int, Int>()
        
        attack.attackBonuses.forEach { bonus ->
            val (fFlat, fDice) = parseFormulaParts(bonus.formula, attributeModifiers, proficiencyBonus, stats)
            totalFlat += fFlat
            fDice.forEach { allDice[it.sides] = (allDice[it.sides] ?: 0) + it.count }
        }
        Pair(totalFlat, allDice.map { DicePart(it.value, it.key) }.sortedBy { it.sides })
    }

    
    val totalAttackBonus = attackCalculation.first
    val attackDice = attackCalculation.second

    val fullDamageText = formatFullDamage(
        baseFormula = attack.damageFormula,
        baseDamageBonus = attack.damageBonus,
        bonusFormulas = attack.damageBonuses.map { it.formula },
        attributeModifiers = attributeModifiers,
        proficiencyBonus = proficiencyBonus,
        stats = stats
    )

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
            .padding(padding)
            .clickable(enabled = !isEditMode, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isEditMode) 0.6f else 0.5f)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 8.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
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
                        color = MaterialTheme.colorScheme.onSurface,
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
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                )
                            }

                            if (showInfo) {
                                val colorScheme = MaterialTheme.colorScheme
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
                                    
                                    SideEffect {
                                        window?.let { w ->
                                            w.addFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
                                            w.addFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
                                            w.addFlags(android.view.WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH)
                                            w.clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                                            w.setDimAmount(0f)
                                            
                                            val params = w.attributes
                                            params.width = android.view.WindowManager.LayoutParams.WRAP_CONTENT
                                            params.height = android.view.WindowManager.LayoutParams.WRAP_CONTENT
                                            
                                            // Position precisely to the left of the button
                                            params.gravity = android.view.Gravity.TOP or android.view.Gravity.END
                                            val screenWidth = view.context.resources.displayMetrics.widthPixels
                                            params.x = (screenWidth - iconPosition.x).toInt() + 8 // 8px margin from icon
                                            params.y = iconPosition.y.toInt() - 16 // Slight upward offset for better alignment
                                            
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
                                                .padding(8.dp) // Smaller padding for "popover" look
                                                .widthIn(max = 260.dp)
                                                .run {
                                                    if (blurPopups && hazeState != null && !isOled) {
                                                        this.clip(RoundedCornerShape(16.dp))
                                                            .hazeEffect(state = hazeState) {
                                                                inputScale = HazeInputScale.Fixed(0.6f)
                                                            }
                                                    } else this
                                                }
                                                .clickable { showInfo = false },
                                            shape = RoundedCornerShape(16.dp),
                                            color = if (isOled) Color.Black else colorScheme.surface.copy(alpha = if (blurPopups) 0.4f else 0.85f),
                                            tonalElevation = 8.dp,
                                            border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = if (isOled) 0.3f else 0.15f))
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
                
                if (!isEditMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Damage Section
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .onGloballyPositioned { coords ->
                                    damageBtnSize = coords.size
                                }
                                .combinedClickable(
                                    onClick = {
                                        val damageFlat = attack.damageBonus
                                        val bonusFormulas = attack.damageBonuses.map { it.formula } + attack.damageFormula
                                        onRoll(DiceRoller.roll(
                                            title = "Урон: ${attack.name}",
                                            baseModifier = damageFlat,
                                            bonusFormulas = bonusFormulas,
                                            isDamage = true,
                                            stats = stats,
                                            sourceType = RollSourceType.ATTACK,
                                            advantageType = AdvantageType.NONE
                                        ))
                                    },
                                    onLongClick = { showDamagePopup = true }
                                ),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$fullDamageText ${attack.damageType}".trim(),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                
                                if (showDamagePopup) {
                                    val density = LocalDensity.current
                                    val sizeDp = with(density) { damageBtnSize.toSize().let { androidx.compose.ui.unit.DpSize((it.width / density.density).dp, (it.height / density.density).dp) } }
                                    DiceRollAdvantagePopup(
                                        onAdvantage = {
                                            onRoll(DiceRoller.roll(
                                                title = "Урон: ${attack.name}",
                                                baseModifier = attack.damageBonus,
                                                bonusFormulas = attack.damageBonuses.map { it.formula } + attack.damageFormula,
                                                isDamage = true,
                                                stats = stats,
                                                sourceType = RollSourceType.ATTACK,
                                                advantageType = AdvantageType.ADVANTAGE
                                            ))
                                        },
                                        onDisadvantage = {
                                            onRoll(DiceRoller.roll(
                                                title = "Урон: ${attack.name}",
                                                baseModifier = attack.damageBonus,
                                                bonusFormulas = attack.damageBonuses.map { it.formula } + attack.damageFormula,
                                                isDamage = true,
                                                stats = stats,
                                                sourceType = RollSourceType.ATTACK,
                                                advantageType = AdvantageType.DISADVANTAGE
                                            ))
                                        },
                                        onCritical = {
                                            onRoll(DiceRoller.roll(
                                                title = "Критический Урон: ${attack.name}",
                                                baseModifier = attack.damageBonus,
                                                bonusFormulas = attack.damageBonuses.map { it.formula } + attack.damageFormula,
                                                isDamage = true,
                                                stats = stats,
                                                sourceType = RollSourceType.ATTACK,
                                                advantageType = AdvantageType.CRITICAL
                                            ))
                                        },
                                        onDismiss = { showDamagePopup = false },
                                        hazeState = hazeState,
                                        isOled = MaterialTheme.colorScheme.background == Color.Black,
                                        modifier = Modifier.size(sizeDp)
                                    )
                                }
                            }
                        }

                        // Modifier Section
                        if (attack.isMagic && attack.magicType == MagicAttackType.SAVE) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "СЛОЖНОСТЬ",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 10.sp
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = totalAttackBonus.toString(),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
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
                                    .onGloballyPositioned { coords ->
                                        attackBtnSize = coords.size
                                    }
                                    .combinedClickable(
                                        onClick = {
                                            val bonuses = if (attack.isMagic) spellSettings.spellAttackBonuses.map { it.formula } else attack.attackBonuses.map { it.formula }
                                            onRoll(DiceRoller.roll(
                                                title = if (attack.isMagic) "Магическая атака: ${attack.name}" else "Атака: ${attack.name}",
                                                baseModifier = totalAttackBonus + (if (attack.magicType == MagicAttackType.SAVE) 0 else exhaustion * 2),
                                                bonusFormulas = bonuses,
                                                isDamage = false,
                                                stats = stats,
                                                exhaustion = exhaustion,
                                                sourceType = RollSourceType.ATTACK,
                                                advantageType = AdvantageType.NONE
                                            ))
                                        },
                                        onLongClick = { showAttackPopup = true }
                                    ),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        AttackBonusIndicator(
                                            bonus = totalAttackBonus,
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
                                                val bonuses = if (attack.isMagic) spellSettings.spellAttackBonuses.map { it.formula } else attack.attackBonuses.map { it.formula }
                                                onRoll(DiceRoller.roll(
                                                    title = if (attack.isMagic) "Магическая атака: ${attack.name}" else "Атака: ${attack.name}",
                                                    baseModifier = totalAttackBonus + (if (attack.magicType == MagicAttackType.SAVE) 0 else exhaustion * 2),
                                                    bonusFormulas = bonuses,
                                                    isDamage = false,
                                                    stats = stats,
                                                    exhaustion = exhaustion,
                                                    sourceType = RollSourceType.ATTACK,
                                                    advantageType = AdvantageType.ADVANTAGE
                                                ))
                                            },
                                            onDisadvantage = {
                                                val bonuses = if (attack.isMagic) spellSettings.spellAttackBonuses.map { it.formula } else attack.attackBonuses.map { it.formula }
                                                onRoll(DiceRoller.roll(
                                                    title = if (attack.isMagic) "Магическая атака: ${attack.name}" else "Атака: ${attack.name}",
                                                    baseModifier = totalAttackBonus + (if (attack.magicType == MagicAttackType.SAVE) 0 else exhaustion * 2),
                                                    bonusFormulas = bonuses,
                                                    isDamage = false,
                                                    stats = stats,
                                                    exhaustion = exhaustion,
                                                    sourceType = RollSourceType.ATTACK,
                                                    advantageType = AdvantageType.DISADVANTAGE
                                                ))
                                            },
                                            onDismiss = { showAttackPopup = false },
                                            hazeState = hazeState,
                                            isOled = MaterialTheme.colorScheme.background == Color.Black,
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
                        tint = Color(0xFFE57373)
                    )
                }
            }
        }
    }
}
