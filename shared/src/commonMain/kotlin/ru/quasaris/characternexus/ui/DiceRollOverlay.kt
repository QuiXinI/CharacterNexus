package ru.quasaris.characternexus.ui

import ru.quasaris.characternexus.model.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import org.jetbrains.compose.resources.painterResource
import characternexus.shared.generated.resources.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.LocalHazeStyle
import dev.chrisbanes.haze.hazeEffect
import ru.quasaris.characternexus.backend.*
import ru.quasaris.characternexus.util.log
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

val DiceRollHazeStyle = HazeStyle(
    blurRadius = 24.dp,
    tints = listOf(HazeTint(Color.Black.copy(alpha = 0.2f)))
)

@Composable
fun DiceRollOverlay(
    history: List<RollResult>,
    onClose: () -> Unit,
    themeMode: AppThemeMode = AppThemeMode.M3,
    forceBlurEnabled: Boolean = false,
    hazeState: HazeState? = null,
    alpha: Float = 1.0f,
    isPassThrough: Boolean = true,
    position: DiceRollPosition = DiceRollPosition.BOTTOM_LEFT,
    closeButtonPosition: DiceRollPosition = DiceRollPosition.TOP_RIGHT,
    modifier: Modifier = Modifier
) {
    if (history.isEmpty()) return

    val isOled = themeMode == AppThemeMode.OFF
    val latest = remember(history, history.size) { history.firstOrNull() }
    val previous = remember(history, history.size) { history.drop(1).reversed() }
    val colorScheme = MaterialTheme.colorScheme

    Box(modifier = modifier.padding(16.dp)) {
        Surface(
            modifier = Modifier
                .widthIn(min = 280.dp, max = 340.dp)
                .run {
                    if (forceBlurEnabled && hazeState != null && !isOled) {
                        this.clip(RoundedCornerShape(24.dp))
                            .hazeEffect(state = hazeState, style = DiceRollHazeStyle)
                    } else this
                },
            shape = RoundedCornerShape(24.dp),
            color = when {
                isOled -> Color.Black
                forceBlurEnabled && hazeState != null -> colorScheme.surface.copy(alpha = 0.4f)
                else -> colorScheme.surface.copy(alpha = alpha)
            },
            border = BorderStroke(1.dp, Color.White.copy(alpha = if (isOled) 0.3f else 0.1f)),
            tonalElevation = if (isOled || (forceBlurEnabled && hazeState != null)) 0.dp else 8.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (previous.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        previous.forEach { roll ->
                            RollItem(roll, isCompact = true, isOled = isOled)
                        }
                        HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.3f))
                    }
                }

                latest?.let {
                    RollItem(it, isCompact = false, isOled = isOled)
                }
            }
        }

        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(when(closeButtonPosition) {
                    DiceRollPosition.TOP_LEFT -> Alignment.TopStart
                    DiceRollPosition.TOP_RIGHT -> Alignment.TopEnd
                    DiceRollPosition.BOTTOM_LEFT -> Alignment.BottomStart
                    DiceRollPosition.BOTTOM_RIGHT -> Alignment.BottomEnd
                })
                .padding(4.dp)
                .size(32.dp)
                .background(colorScheme.surfaceVariant.copy(alpha = 0.8f), CircleShape)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun DiceRollingFab(
    onRoll: (Map<Int, Int>) -> Unit,
    offsetX: Float,
    offsetY: Float,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    isOled: Boolean = false,
    alpha: Float = 1.0f,
    forceBlurEnabled: Boolean = true,
    positionKey: Any? = null,
    onDrag: (Float, Float) -> Unit = { _, _ -> }
) {
    var expanded by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }
    val pool = remember { mutableStateMapOf<Int, Int>() }
    val isPoolEmpty = pool.values.sum() == 0
    val haptic = LocalHapticFeedback.current

    val transition = updateTransition(expanded, label = "DiceMenu")
    val springSpec = spring<Float>(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow)
    val menuScale by transition.animateFloat(label = "Scale", transitionSpec = { springSpec }) { if (it) 1f else 0f }
    val mainButtonScale by transition.animateFloat(label = "MainButtonScale", transitionSpec = { springSpec }) { if (it) 0.8f else 1f }
    val dragScale by animateFloatAsState(if (isDragging) 1.2f else 1f, label = "DragScale")

    val diceTypes = listOf(2, 4, 6, 8, 10, 12, 20, 100)
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .run { if (expanded) fillMaxSize() else wrapContentSize() }
            .pointerInput(expanded) {
                if (expanded) {
                    detectTapGestures(onTap = { expanded = false })
                }
            },
        contentAlignment = Alignment.BottomEnd
    ) {
        Box(
            modifier = Modifier
                .offset(x = (-offsetX).dp, y = (-offsetY).dp)
                .size(280.dp),
            contentAlignment = Alignment.Center
        ) {
            if (expanded) {
                diceTypes.forEachIndexed { index, sides ->
                    val angle = (index * 45.0 - 90.0) * PI / 180.0
                    val radius = 80.dp 
                    val offX = (radius.value * cos(angle)).dp
                    val offY = (radius.value * sin(angle)).dp

                    DiceMenuItem(
                        sides = sides,
                        count = pool[sides] ?: 0,
                        onClick = { pool[sides] = (pool[sides] ?: 0) + 1 },
                        onLongClick = { if ((pool[sides] ?: 0) > 0) pool[sides] = (pool[sides] ?: 0) - 1 },
                        modifier = Modifier
                            .offset(x = offX * menuScale, y = offY * menuScale)
                            .scale(menuScale)
                            .alpha(menuScale),
                        isOled = isOled,
                        alpha = alpha,
                        hazeState = hazeState,
                        forceBlurEnabled = forceBlurEnabled
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .size(72.dp)
                    .graphicsLayer {
                        val s = mainButtonScale * dragScale
                        scaleX = s
                        scaleY = s
                    }
                    .shadow(if (!isPoolEmpty && !isOled) 10.dp else 3.dp, CircleShape)
                    .clip(CircleShape)
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                if (expanded) { pool.clear(); expanded = false }
                                isDragging = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onDragEnd = { isDragging = false },
                            onDragCancel = { isDragging = false },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDrag(-dragAmount.x, -dragAmount.y)
                            }
                        )
                    }
                    .clickable {
                        if (!expanded) expanded = true
                        else if (!isPoolEmpty) {
                            onRoll(pool.toMap())
                            pool.clear()
                            expanded = false
                        } else expanded = false
                    },
                shape = CircleShape,
                color = if (isOled) Color.Black else if (isPoolEmpty) colorScheme.surfaceVariant.copy(alpha = alpha) else colorScheme.primary.copy(alpha = alpha),
                border = BorderStroke(1.dp, Color.White.copy(alpha = if (isOled) 0.3f else 0.1f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isPoolEmpty) {
                        Icon(painterResource(Res.drawable.ic_d20_dice), null, modifier = Modifier.size(38.dp), tint = colorScheme.primary)
                    } else {
                        Text("Бросить", fontWeight = FontWeight.Black, fontSize = 16.sp, color = colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun DiceMenuItem(
    sides: Int,
    count: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    isOled: Boolean = false,
    alpha: Float = 1.0f,
    hazeState: HazeState? = null,
    forceBlurEnabled: Boolean = true
) {
    val colorScheme = MaterialTheme.colorScheme
    Box(modifier = modifier.size(48.dp), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.fillMaxSize().shadow(if (count > 0) 6.dp else 2.dp, CircleShape),
            shape = CircleShape,
            color = if (isOled) Color.Black else colorScheme.surfaceVariant.copy(alpha = alpha),
            border = BorderStroke(if (count > 0) 2.dp else 1.dp, if (count > 0) colorScheme.primary else Color.White.copy(alpha = 0.1f))
        ) {
            Box(modifier = Modifier.fillMaxSize().combinedClickable(onClick = onClick, onLongClick = onLongClick), contentAlignment = Alignment.Center) {
                val iconRes = when (sides) {
                    2 -> Res.drawable.ic_d2_dice; 4 -> Res.drawable.ic_d4_dice; 6 -> Res.drawable.ic_d6_dice
                    8 -> Res.drawable.ic_d8_dice; 10 -> Res.drawable.ic_d10_dice; 12 -> Res.drawable.ic_d12_dice
                    20 -> Res.drawable.ic_d20_dice; else -> Res.drawable.ic_d20_dice
                }
                Icon(painterResource(iconRes), null, modifier = Modifier.size(22.dp), tint = colorScheme.primary)
            }
        }
        if (count > 0) {
            Badge(modifier = Modifier.align(Alignment.TopEnd).offset(x = 3.dp, y = (-3).dp), containerColor = colorScheme.primary) { Text(count.toString()) }
        }
    }
}

@Composable
fun RollItem(result: RollResult, isCompact: Boolean, isOled: Boolean) {
    val colorScheme = MaterialTheme.colorScheme
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Text(text = result.title, color = getRollSourceColor(result.sourceType, colorScheme).copy(alpha = if (isCompact) 0.6f else 0.9f), fontSize = if (isCompact) 15.sp else 18.sp, fontWeight = if (isCompact) FontWeight.Bold else FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            val fontSize = if (!isCompact || result.isCriticalFailure || result.isCriticalSuccess) 42.sp else 28.sp
            Text(text = result.total.toString(), color = if (result.isCriticalFailure) Color.Red else if (result.isCriticalSuccess) colorScheme.primary else colorScheme.onSurface, style = TextStyle(fontSize = fontSize, fontWeight = FontWeight.Black))
        }
        Text(text = buildStyledBreakdown(result, colorScheme, isOled), fontSize = if (isCompact) 14.sp else 18.sp, modifier = Modifier.fillMaxWidth())
    }
}

fun buildStyledBreakdown(result: RollResult, colorScheme: ColorScheme, isOled: Boolean): AnnotatedString {
    return buildAnnotatedString { append(result.breakdown) }
}

fun getRollSourceColor(sourceType: RollSourceType, colorScheme: ColorScheme): Color {
    return when (sourceType) {
        RollSourceType.ABILITY -> Color(0xFF0288D1); RollSourceType.SKILL -> Color(0xFF388E3C)
        RollSourceType.SAVING_THROW -> Color(0xFFF57C00); RollSourceType.ATTACK -> Color(0xFFD32F2F)
        RollSourceType.OTHER -> colorScheme.onSurface
    }
}

@Composable
fun DiceRollAdvantagePopup(
    onAdvantage: () -> Unit,
    onDisadvantage: () -> Unit,
    onDismiss: () -> Unit,
    onCritical: (() -> Unit)? = null,
    onUpcast: (() -> Unit)? = null,
    hazeState: HazeState? = null,
    isOled: Boolean = false,
    widthMultiplier: Float = 1f,
    modifier: Modifier = Modifier
) {
    Popup(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier.fillMaxWidth(widthMultiplier).shadow(8.dp, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            color = if (isOled) Color.Black else MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Row(modifier = Modifier.padding(4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                IconButton(onClick = { onAdvantage(); onDismiss() }) { Icon(Icons.Default.KeyboardArrowUp, null, tint = Color.Green) }
                IconButton(onClick = { onDisadvantage(); onDismiss() }) { Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.Red) }
            }
        }
    }
}
