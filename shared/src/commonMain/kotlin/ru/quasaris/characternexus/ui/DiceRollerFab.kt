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

enum class DiceType(val sides: Int, val iconRes: DrawableResource) {
    D2(2, Res.drawable.ic_d2_dice),
    D4(4, Res.drawable.ic_d4_dice),
    D6(6, Res.drawable.ic_d6_dice),
    D8(8, Res.drawable.ic_d8_dice),
    D10(10, Res.drawable.ic_d10_dice),
    D12(12, Res.drawable.ic_d12_dice),
    D20(20, Res.drawable.ic_d20_dice),
    D100(100, Res.drawable.ic_d10_dice)
}

private val HazeFabStyle = HazeStyle(
    blurRadius = 32.dp,
    tints = listOf(HazeTint(Color.Black.copy(alpha = 0.15f))),
    noiseFactor = 0f
)

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

    var fabOffset by remember { mutableStateOf(IntOffset(initialOffsetX.roundToInt(), initialOffsetY.roundToInt())) }

    LaunchedEffect(initialOffsetX, initialOffsetY) {
        if (!isDragging) {
            fabOffset = IntOffset(initialOffsetX.roundToInt(), initialOffsetY.roundToInt())
        }
    }

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
        isDragging = isDragging,
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
    isDragging: Boolean = false,
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
        if (expanded) 0.85f else 1f
    }

    val isAnyDiceSelected = dicePool.values.any { it > 0 }

    val dragScale by animateFloatAsState(
        targetValue = if (isDragging) 1.1f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "DragScale"
    )

    // Якорь фиксированного размера (72dp) гарантирует, что центр FAB не смещается при раскрытии меню
    Box(
        modifier = modifier
            .offset { offset }
            .size(72.dp),
        contentAlignment = Alignment.Center
    ) {
        // Контейнер для меню с "unbounded" размером позволяет костям вылетать за пределы якоря
        Box(
            modifier = Modifier
                .wrapContentSize(unbounded = true)
                .size(280.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onToggleExpand
                        )
                )
            }

            DiceType.entries.forEachIndexed { index, dice ->
                val angle = index * (2 * PI / 8) - PI / 2
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
                        alpha = alpha,
                        forceBlurEnabled = forceBlurEnabled,
                        onClick = { onDiceClick(dice.sides) },
                        onLongClick = { onDiceLongClick(dice.sides) }
                    )
                }
            }
        }

        val surfaceColor = MaterialTheme.colorScheme.surface
        Box(
            modifier = Modifier
                .size(if (isExpanded) 64.dp else 72.dp)
                .scale(fabScale * dragScale)
                .outerShadow(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = if (isOled) 0.6f else 0.25f),
                    blur = if (isAnyDiceSelected) 16.dp else 10.dp,
                    offsetY = 4.dp
                )
                .clip(CircleShape)
                .run {
                    if (forceBlurEnabled && hazeState != null && !isOled) {
                        this.background(Color.Transparent)
                    } else {
                        this.background(if (isOled) Color.Black.copy(alpha = alpha) else surfaceColor.copy(alpha = alpha))
                    }
                }
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
            if (forceBlurEnabled && hazeState != null && !isOled) {
                Box(
                    modifier = Modifier
                        .wrapContentSize(unbounded = true)
                        .size(160.dp)
                        .background(surfaceColor.copy(alpha = alpha * 0.4f))
                        .hazeEffect(state = hazeState, style = HazeFabStyle)
                )
            }

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
                            color = MaterialTheme.colorScheme.primary.copy(alpha = alpha)
                        )
                    )
                } else {
                    Icon(
                        painter = painterResource(Res.drawable.ic_d20_dice),
                        contentDescription = "Dice Roller",
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = alpha)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DiceButton(
    dice: DiceType,
    count: Int,
    hazeState: HazeState?,
    isOled: Boolean,
    alpha: Float,
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
                .outerShadow(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = if (isOled) 0.5f else 0.25f),
                    blur = if (count > 0) 10.dp else 6.dp,
                    offsetY = 2.dp
                )
                .clip(CircleShape)
                .run {
                    if (forceBlurEnabled && hazeState != null && !isOled) {
                        this.background(Color.Transparent)
                    } else {
                        this.background(if (isOled) Color.Black.copy(alpha = alpha) else colorScheme.surface.copy(alpha = alpha))
                    }
                }
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
            if (forceBlurEnabled && hazeState != null && !isOled) {
                Box(
                    modifier = Modifier
                        .wrapContentSize(unbounded = true)
                        .size(80.dp)
                        .background(colorScheme.surface.copy(alpha = alpha * 0.4f))
                        .hazeEffect(state = hazeState, style = HazeFabStyle)
                )
            }

            if (dice == DiceType.D100) {
                Box(modifier = Modifier.size(24.dp)) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_d10_dice),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp).align(Alignment.TopStart),
                        tint = colorScheme.primary.copy(alpha = alpha)
                    )
                    Icon(
                        painter = painterResource(Res.drawable.ic_d10_dice),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp).align(Alignment.BottomEnd),
                        tint = colorScheme.primary.copy(alpha = alpha)
                    )
                }
            } else {
                Icon(
                    painter = painterResource(dice.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = colorScheme.primary.copy(alpha = alpha)
                )
            }
        }

        if (count > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .background(colorScheme.primary.copy(alpha = alpha), CircleShape)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = count.toString(),
                    color = colorScheme.onPrimary.copy(alpha = alpha),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 9.sp)
                )
            }
        }
    }
}