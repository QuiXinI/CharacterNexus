package ru.quasaris.characternexus.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import characternexus.shared.generated.resources.*
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Перечисление типов кубиков с их иконками и количеством граней.
 */
enum class DiceType(val sides: Int, val iconRes: DrawableResource) {
    D2(2, Res.drawable.ic_d2_dice),
    D4(4, Res.drawable.ic_d4_dice),
    D6(6, Res.drawable.ic_d6_dice),
    D8(8, Res.drawable.ic_d8_dice),
    D10(10, Res.drawable.ic_d10_dice),
    D12(12, Res.drawable.ic_d12_dice),
    D20(20, Res.drawable.ic_d20_dice),
    D100(100, Res.drawable.ic_d10_dice) // Для D100 используем иконку D10 (или спец. обработку)
}

private val HazeFabStyle = HazeStyle(
    blurRadius = 24.dp,
    tints = listOf(HazeTint(Color.Black.copy(alpha = 0.2f)))
)

/**
 * Stateful-обертка для DiceRollerFab.
 * Управляет состоянием пула кубиков, позицией и развернутостью.
 */
@Composable
fun DiceRollerFab(
    onRoll: (Map<Int, Int>) -> Unit,
    hazeState: HazeState?,
    modifier: Modifier = Modifier,
    isOled: Boolean = false,
    alpha: Float = 1.0f,
    forceBlurEnabled: Boolean = true,
    initialOffsetX: Float = 0f,
    initialOffsetY: Float = 0f,
    onPositionChange: (Float, Float) -> Unit = { _, _ -> }
) {
    var isExpanded by remember { mutableStateOf(false) }
    val dicePool = remember { mutableStateMapOf<Int, Int>() }
    var isDragging by remember { mutableStateOf(false) }
    
    // Позиция FAB. Используем initialOffset для инициализации.
    var fabOffset by remember { mutableStateOf(IntOffset(initialOffsetX.roundToInt(), initialOffsetY.roundToInt())) }
    
    // Синхронизация при внешнем изменении (например, из настроек)
    LaunchedEffect(initialOffsetX, initialOffsetY) {
        if (!isDragging) {
            fabOffset = IntOffset(initialOffsetX.roundToInt(), initialOffsetY.roundToInt())
        }
    }

    // Инициализация пула нулями
    LaunchedEffect(Unit) {
        DiceType.entries.forEach { dicePool[it.sides] = 0 }
    }

    DiceRollerFabStateless(
        isExpanded = isExpanded,
        dicePool = dicePool,
        offset = fabOffset,
        hazeState = hazeState,
        isOled = isOled,
        alpha = alpha,
        forceBlurEnabled = forceBlurEnabled,
        onToggleExpand = { isExpanded = !isExpanded },
        onDiceClick = { sides -> 
            dicePool[sides] = (dicePool[sides] ?: 0) + 1 
        },
        onDiceLongClick = { sides ->
            val current = dicePool[sides] ?: 0
            if (current > 0) dicePool[sides] = current - 1
        },
        onRollClick = {
            onRoll(dicePool.toMap())
            dicePool.keys.forEach { dicePool[it] = 0 }
            isExpanded = false
        },
        onResetAndDrag = {
            dicePool.keys.forEach { dicePool[it] = 0 }
            isExpanded = false
        },
        onDragStart = { isDragging = true },
        onDragEnd = { isDragging = false },
        onPositionChange = { delta ->
            val newX = fabOffset.x + delta.x
            val newY = fabOffset.y + delta.y
            fabOffset = IntOffset(newX.roundToInt(), newY.roundToInt())
            onPositionChange(newX, newY)
        },
        modifier = modifier
    )
}

/**
 * Stateless UI компонент плавающей кнопки броска кубиков.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DiceRollerFabStateless(
    isExpanded: Boolean,
    dicePool: Map<Int, Int>,
    offset: IntOffset,
    hazeState: HazeState?,
    isOled: Boolean,
    alpha: Float,
    forceBlurEnabled: Boolean,
    onToggleExpand: () -> Unit,
    onDiceClick: (Int) -> Unit,
    onDiceLongClick: (Int) -> Unit,
    onRollClick: () -> Unit,
    onResetAndDrag: () -> Unit,
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {},
    onPositionChange: (androidx.compose.ui.geometry.Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val transition = updateTransition(targetState = isExpanded, label = "FabTransition")

    val radialRadius by transition.animateDp(
        label = "RadialRadius",
        transitionSpec = { 
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ) 
        }
    ) { expanded -> if (expanded) 90.dp else 0.dp }

    val fabScale by transition.animateFloat(
        label = "FabScale",
        transitionSpec = { 
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ) 
        }
    ) { expanded ->
        if (expanded) 0.8f else 1f
    }

    val isAnyDiceSelected = dicePool.values.any { it > 0 }

    Box(
        modifier = modifier
            .offset { offset }
            .size(72.dp)
            .alpha(alpha),
        contentAlignment = Alignment.Center
    ) {
        // Фоновая подложка для закрытия при клике в пустоту (в развернутом состоянии)
        if (isExpanded) {
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggleExpand
                    )
            )
        }

        // 8 кнопок кубиков
        DiceType.entries.forEachIndexed { index, dice ->
            val angle = index * (2 * PI / 8) - PI / 2 // Начинаем сверху
            val diceOffsetX = with(density) { (radialRadius.toPx() * cos(angle)).roundToInt() }
            val diceOffsetY = with(density) { (radialRadius.toPx() * sin(angle)).roundToInt() }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + scaleIn(initialScale = 0f),
                exit = fadeOut() + scaleOut(targetScale = 0f),
                modifier = Modifier.offset { IntOffset(diceOffsetX, diceOffsetY) }
            ) {
                DiceButton(
                    dice = dice,
                    count = dicePool[dice.sides] ?: 0,
                    hazeState = hazeState,
                    isOled = isOled,
                    forceBlurEnabled = forceBlurEnabled,
                    onClick = { onDiceClick(dice.sides) },
                    onLongClick = { onDiceLongClick(dice.sides) }
                )
            }
        }

        // Центральная кнопка
        Box(
            modifier = Modifier
                .size(if (isExpanded) 64.dp else 72.dp)
                .scale(fabScale)
                .clip(CircleShape)
                .run {
                    if (forceBlurEnabled && hazeState != null && !isOled) {
                        this.hazeEffect(state = hazeState, style = HazeFabStyle)
                    } else this
                }
                .background(
                    color = if (isOled) Color.Black else MaterialTheme.colorScheme.surface.copy(alpha = if (forceBlurEnabled && hazeState != null) 0.4f else 1.0f),
                    shape = CircleShape
                )
                .border(1.dp, Color.White.copy(alpha = if (isOled) 0.3f else 0.2f), CircleShape)
                .pointerInput(isExpanded) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { 
                            if (isExpanded) onResetAndDrag()
                            onDragStart()
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            onPositionChange(dragAmount)
                        },
                        onDragEnd = { onDragEnd() },
                        onDragCancel = { onDragEnd() }
                    )
                }
                .clickable(
                    onClick = {
                        if (isExpanded) {
                            if (isAnyDiceSelected) onRollClick() else onToggleExpand()
                        } else onToggleExpand()
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            val showRollText = isExpanded && isAnyDiceSelected
            AnimatedContent(
                targetState = showRollText,
                transitionSpec = {
                    (fadeIn() + scaleIn()).togetherWith(fadeOut() + scaleOut())
                },
                label = "CentralButtonContent"
            ) { targetShowRoll ->
                if (targetShowRoll) {
                    Text(
                        text = "БРОСОК",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                } else {
                    Icon(
                        painter = painterResource(Res.drawable.ic_d20_dice),
                        contentDescription = "Dice Roller",
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * Отдельная кнопка кубика в радиальном меню.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DiceButton(
    dice: DiceType,
    count: Int,
    hazeState: HazeState?,
    isOled: Boolean,
    forceBlurEnabled: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier.size(52.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .run {
                    if (forceBlurEnabled && hazeState != null && !isOled) {
                        this.hazeEffect(state = hazeState, style = HazeFabStyle)
                    } else this
                }
                .background(
                    color = if (isOled) Color.Black else colorScheme.surface.copy(alpha = if (forceBlurEnabled && hazeState != null) 0.4f else 1.0f),
                    shape = CircleShape
                )
                .border(
                    width = if (count > 0) 2.dp else 1.dp,
                    color = if (count > 0) colorScheme.primary else Color.White.copy(alpha = if (isOled) 0.3f else 0.1f),
                    shape = CircleShape
                )
                .pointerInput(onClick, onLongClick) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                                event.changes.forEach { it.consume() }
                                onLongClick()
                            }
                        }
                    }
                }
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                ),
            contentAlignment = Alignment.Center
        ) {
            if (dice == DiceType.D100) {
                Box(modifier = Modifier.size(24.dp)) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_d10_dice),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp).align(Alignment.TopStart),
                        tint = colorScheme.primary
                    )
                    Icon(
                        painter = painterResource(Res.drawable.ic_d10_dice),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp).align(Alignment.BottomEnd),
                        tint = colorScheme.primary
                    )
                }
            } else {
                Icon(
                    painter = painterResource(dice.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = colorScheme.primary
                )
            }
        }

        if (count > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .background(colorScheme.primary, CircleShape)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = count.toString(),
                    color = colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 9.sp)
                )
            }
        }
    }
}