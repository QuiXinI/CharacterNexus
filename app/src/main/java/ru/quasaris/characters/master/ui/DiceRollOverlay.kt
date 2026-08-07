package ru.quasaris.characters.master.ui

import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.window.Popup
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.LocalHazeStyle
import dev.chrisbanes.haze.hazeEffect
import ru.quasaris.characters.master.R
import ru.quasaris.characters.master.backend.*
import ru.quasaris.characters.master.backend.DieRoll
import ru.quasaris.characters.master.backend.RollResult
import ru.quasaris.characters.master.backend.AdvantageType
import ru.quasaris.characters.master.backend.RollSourceType
import ru.quasaris.characters.master.backend.DiceRollPosition
import ru.quasaris.characters.master.backend.AppThemeMode
import ru.quasaris.characters.master.backend.AppScaleProvider
import ru.quasaris.characters.master.backend.LocalAppScale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Глобальный стиль для оверлея бросков кубов.
 * Настроен для оптимальной производительности и корректного отображения углов.
 */
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

    var anchorPos by remember { mutableStateOf<IntOffset?>(null) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        AppScaleProvider(LocalAppScale.current) {
            val view = LocalView.current
            val window = (view.parent as? DialogWindowProvider)?.window

            SideEffect {
                window?.let { w ->
                    // Базовые настройки окна (чтобы оно не блокировало весь экран)
                    w.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
                    w.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
                    w.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                    w.setDimAmount(0f)

                    // Управление прозрачностью ДЛЯ КЛИКОВ всей панели
                    if (isPassThrough) {
                        w.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                    } else {
                        w.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                    }

                    val params = w.attributes
                    params.gravity = when (position) {
                        DiceRollPosition.TOP_LEFT -> Gravity.TOP or Gravity.START
                        DiceRollPosition.TOP_RIGHT -> Gravity.TOP or Gravity.END
                        DiceRollPosition.BOTTOM_LEFT -> Gravity.BOTTOM or Gravity.START
                        DiceRollPosition.BOTTOM_RIGHT -> Gravity.BOTTOM or Gravity.END
                    }
                    params.width = WindowManager.LayoutParams.WRAP_CONTENT
                    params.height = WindowManager.LayoutParams.WRAP_CONTENT
                    w.attributes = params

                    if (!isOled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        w.setBackgroundBlurRadius(120)
                    }
                    w.setBackgroundDrawableResource(android.R.color.transparent)
                }
            }

            CompositionLocalProvider(LocalHazeStyle provides DiceRollHazeStyle) {
                Surface(
                    modifier = modifier
                        .padding(16.dp)
                        .widthIn(min = 280.dp, max = 340.dp)
                        .onGloballyPositioned { coords ->
                            // Вычисляем АБСОЛЮТНУЮ позицию выбранного угла панели на физическом экране
                            val location = IntArray(2)
                            view.getLocationOnScreen(location)
                            val posInRoot = coords.positionInRoot()

                            val newPos = when (closeButtonPosition) {
                                DiceRollPosition.TOP_LEFT -> IntOffset(
                                    x = location[0] + posInRoot.x.roundToInt(),
                                    y = location[1] + posInRoot.y.roundToInt()
                                )
                                DiceRollPosition.TOP_RIGHT -> IntOffset(
                                    x = location[0] + posInRoot.x.roundToInt() + coords.size.width,
                                    y = location[1] + posInRoot.y.roundToInt()
                                )
                                DiceRollPosition.BOTTOM_LEFT -> IntOffset(
                                    x = location[0] + posInRoot.x.roundToInt(),
                                    y = location[1] + posInRoot.y.roundToInt() + coords.size.height
                                )
                                DiceRollPosition.BOTTOM_RIGHT -> IntOffset(
                                    x = location[0] + posInRoot.x.roundToInt() + coords.size.width,
                                    y = location[1] + posInRoot.y.roundToInt() + coords.size.height
                                )
                            }

                            if (anchorPos != newPos) {
                                anchorPos = newPos
                            }
                        }
                        .run {
                            if (forceBlurEnabled && hazeState != null && !isOled) {
                                this.clip(RoundedCornerShape(24.dp))
                                    .then(
                                        remember(hazeState) {
                                            Modifier.hazeEffect(state = hazeState, style = DiceRollHazeStyle) {
                                                inputScale = HazeInputScale.Fixed(0.6f)
                                            }
                                        }
                                    )
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
                        // История
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

                        // Последний результат
                        latest?.let {
                            RollItem(it, isCompact = false, isOled = isOled)
                        }
                    }
                }
            }
        }
    }

    // Создаем второе независимое окно ТОЛЬКО после того, как узнали точные координаты угла.
    anchorPos?.let { pos ->
        Dialog(
            onDismissRequest = onClose,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = false, // BackPress перехватывается первым окном
                dismissOnClickOutside = false
            )
        ) {
            AppScaleProvider(LocalAppScale.current) {
                val btnView = LocalView.current
                val btnWindow = (btnView.parent as? DialogWindowProvider)?.window
                val colorScheme = MaterialTheme.colorScheme

                SideEffect {
                    btnWindow?.let { w ->
                        // Это окно ВСЕГДА активно и ловит клики (нет FLAG_NOT_TOUCHABLE)
                        w.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
                        // FLAG_NOT_TOUCH_MODAL означает, что окно ловит клики только В СВОИХ ГРАНИЦАХ
                        w.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
                        w.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                        w.setDimAmount(0f)
                        w.setBackgroundDrawableResource(android.R.color.transparent)

                        val params = w.attributes
                        params.gravity = Gravity.TOP or Gravity.START
                        // Центрируем кнопку (32dp) по углу панели.
                        // 16dp - это смещение, чтобы центр кнопки совпал с углом
                        val offsetPx = (16 * btnView.resources.displayMetrics.density).roundToInt()
                        params.x = pos.x - offsetPx
                        params.y = pos.y - offsetPx

                        params.width = WindowManager.LayoutParams.WRAP_CONTENT
                        params.height = WindowManager.LayoutParams.WRAP_CONTENT
                        w.attributes = params
                    }
                }

                // Визуальная и интерактивная часть кнопки в отдельном окне
                Surface(
                    modifier = Modifier
                        .size(32.dp)
                        .run {
                            if (forceBlurEnabled && hazeState != null && !isOled) {
                                this.clip(RoundedCornerShape(12.dp))
                                    .then(
                                        remember(hazeState) {
                                            Modifier.hazeEffect(state = hazeState, style = DiceRollHazeStyle) {
                                                inputScale = HazeInputScale.Fixed(0.6f)
                                            }
                                        }
                                    )
                            } else this
                        }
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            onClickLabel = "Закрыть окно бросков",
                            onClick = onClose
                        ),
                    shape = RoundedCornerShape(12.dp),
                    color = when {
                        isOled -> Color.Black
                        forceBlurEnabled && hazeState != null -> colorScheme.surface.copy(alpha = 0.4f)
                        else -> colorScheme.surface.copy(alpha = alpha)
                    },
                    border = BorderStroke(1.dp, Color.White.copy(alpha = if (isOled) 0.3f else 0.1f)),
                    tonalElevation = if (isOled || (forceBlurEnabled && hazeState != null)) 0.dp else 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Закрыть",
                            modifier = Modifier.size(20.dp),
                            tint = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * FAB для бросков кубов с радиальным меню выбора костей.
 * Поддерживает перетаскивание и Haze-эффект.
 * Реализован через Dialog для обеспечения системного размытия и корректного наложения.
 */
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
    val springSpec = spring<Float>(
        dampingRatio = 0.5f,
        stiffness = Spring.StiffnessLow
    )
    val menuScale by transition.animateFloat(
        label = "Scale",
        transitionSpec = { springSpec }
    ) { if (it) 1f else 0f }
    val mainButtonScale by transition.animateFloat(
        label = "MainButtonScale",
        transitionSpec = { springSpec }
    ) { if (it) 0.8f else 1f }

    val dragScale by animateFloatAsState(if (isDragging) 1.2f else 1f, label = "DragScale")

    val diceTypes = listOf(2, 4, 6, 8, 10, 12, 20, 100)
    val colorScheme = MaterialTheme.colorScheme

    // 1. Меню и Блюр (Большое окно 224dp)
    Dialog(
        onDismissRequest = { expanded = false },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        AppScaleProvider(LocalAppScale.current) {
            val view = LocalView.current
            val window = (view.parent as? DialogWindowProvider)?.window
            val density = view.resources.displayMetrics.density

            SideEffect {
                window?.let { w ->
                    w.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
                    w.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
                    w.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
                    w.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                    w.setDimAmount(0f)

                    // Пропускаем клики, если меню закрыто
                    if (!expanded) {
                        w.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                    } else {
                        w.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                    }

                    val params = w.attributes
                    params.gravity = Gravity.BOTTOM or Gravity.END
                    
                    val baseOffsetX = offsetX * density
                    val baseOffsetY = offsetY * density
                    
                    // Увеличиваем до 224dp, чтобы блюр не обрезался
                    params.width = (224 * density).roundToInt()
                    params.height = (224 * density).roundToInt()
                    params.x = baseOffsetX.roundToInt()
                    params.y = baseOffsetY.roundToInt()
                    
                    w.attributes = params
                    w.setBackgroundDrawableResource(android.R.color.transparent)
                }
            }

            CompositionLocalProvider(LocalHazeStyle provides DiceRollHazeStyle) {
                Box(
                    modifier = modifier
                        .size(224.dp)
                        .then(
                            if (expanded) {
                                Modifier.clickable(
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null,
                                    onClick = { expanded = false }
                                )
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    diceTypes.forEachIndexed { index, sides ->
                        val angle = Math.toRadians(index * 45.0 - 90.0)
                        val radius = 80.dp // Чуть больше радиус для 224dp окна

                        val offsetXPx = (radius.value * cos(angle)).dp
                        val offsetYPx = (radius.value * sin(angle)).dp

                        DiceMenuItem(
                            sides = sides,
                            count = pool[sides] ?: 0,
                            onClick = {
                                pool[sides] = (pool[sides] ?: 0) + 1
                            },
                            onLongClick = {
                                if ((pool[sides] ?: 0) > 0) {
                                    pool[sides] = (pool[sides] ?: 0) - 1
                                }
                            },
                            modifier = Modifier
                                .offset(x = offsetXPx * menuScale, y = offsetYPx * menuScale)
                                .scale(menuScale)
                                .alpha(menuScale),
                            isOled = isOled,
                            alpha = alpha,
                            hazeState = hazeState,
                            forceBlurEnabled = forceBlurEnabled,
                            positionKey = positionKey to expanded
                        )
                    }
                }
            }
        }
    }

    // 2. Кнопка FAB (Маленькое окно 72dp)
    Dialog(
        onDismissRequest = { expanded = false },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        AppScaleProvider(LocalAppScale.current) {
            val view = LocalView.current
            val window = (view.parent as? DialogWindowProvider)?.window
            val density = view.resources.displayMetrics.density

            SideEffect {
                window?.let { w ->
                    w.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
                    w.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
                    w.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
                    w.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                    w.setDimAmount(0f)

                    val params = w.attributes
                    params.gravity = Gravity.BOTTOM or Gravity.END
                    
                    val baseOffsetX = offsetX * density
                    val baseOffsetY = offsetY * density
                    
                    // Центрируем 72dp окно внутри 224dp области
                    // (224 - 72) / 2 = 76dp
                    val offsetAdjustment = 76 * density
                    
                    params.width = (72 * density).roundToInt()
                    params.height = (72 * density).roundToInt()
                    params.x = (baseOffsetX + offsetAdjustment).roundToInt()
                    params.y = (baseOffsetY + offsetAdjustment).roundToInt()
                    
                    w.attributes = params
                    w.setBackgroundDrawableResource(android.R.color.transparent)
                }
            }

            Surface(
                modifier = Modifier
                    .size(72.dp)
                    .scale(mainButtonScale * dragScale)
                    .shadow(if (!isPoolEmpty && !isOled) 10.dp else 3.dp, CircleShape)
                    .run {
                        if (forceBlurEnabled && hazeState != null && !isOled) {
                            this.hazeEffect(state = hazeState, style = DiceRollHazeStyle) {
                                inputScale = HazeInputScale.Fixed(0.6f)
                            }
                            .clip(CircleShape)
                        } else this.clip(CircleShape)
                    }
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                if (expanded) {
                                    pool.clear()
                                    expanded = false
                                }
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
                        if (!expanded) {
                            expanded = true
                        } else if (!isPoolEmpty) {
                            onRoll(pool.toMap())
                            pool.clear()
                            expanded = false
                        } else {
                            expanded = false
                        }
                    },
                shape = CircleShape,
                color = when {
                    isOled -> Color.Black
                    forceBlurEnabled && hazeState != null -> colorScheme.surface.copy(alpha = alpha)
                    isPoolEmpty -> colorScheme.surfaceVariant.copy(alpha = alpha)
                    else -> colorScheme.primary.copy(alpha = alpha)
                },
                tonalElevation = if (isOled || (forceBlurEnabled && hazeState != null)) 0.dp else 8.dp,
                shadowElevation = 0.dp,
                border = BorderStroke(1.dp, Color.White.copy(alpha = if (isOled) 0.3f else 0.1f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (!isPoolEmpty && !isOled) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = colorScheme.primary.copy(alpha = 0.15f),
                            shape = CircleShape,
                            shadowElevation = 19.dp
                        ) {}
                    }

                    AnimatedContent(
                        targetState = isPoolEmpty,
                        transitionSpec = {
                            fadeIn(tween(300)) togetherWith fadeOut(tween(300))
                        },
                        label = "CentralIcon"
                    ) { empty ->
                        if (empty) {
                            Icon(
                                painter = painterResource(R.drawable.ic_d20_dice),
                                contentDescription = "Open Dice Menu",
                                modifier = Modifier.size(38.dp),
                                tint = if (alpha < 0.3f && forceBlurEnabled) colorScheme.primary else if (isPoolEmpty) colorScheme.primary else colorScheme.onPrimary
                            )
                        } else {
                            Text(
                                "Бросить",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = if (alpha < 0.3f && forceBlurEnabled) colorScheme.primary else colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
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
    forceBlurEnabled: Boolean = true,
    positionKey: Any? = null
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .size(48.dp),
        contentAlignment = Alignment.Center
    ) {
        // 1. Background & Shadow layer
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .shadow(if (count > 0) 6.dp else 2.dp, CircleShape)
                .run {
                    if (forceBlurEnabled && hazeState != null && !isOled) {
                        this.hazeEffect(state = hazeState, style = DiceRollHazeStyle) {
                            inputScale = HazeInputScale.Fixed(0.6f)
                        }
                        .clip(CircleShape)
                    } else this.clip(CircleShape)
                },
            shape = CircleShape,
            color = when {
                isOled -> Color.Black
                forceBlurEnabled && hazeState != null -> colorScheme.surface.copy(alpha = alpha)
                else -> colorScheme.surfaceVariant.copy(alpha = alpha)
            },
            tonalElevation = if (isOled || (forceBlurEnabled && hazeState != null)) 0.dp else 4.dp,
            shadowElevation = 0.dp
        ) {
            // 2. Icon layer
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                when (sides) {
                    100 -> {
                        Box(modifier = Modifier.size(26.dp)) {
                            Icon(
                                painter = painterResource(R.drawable.ic_d10_dice),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(16.dp)
                                    .align(Alignment.TopStart),
                                tint = colorScheme.primary
                            )
                            Icon(
                                painter = painterResource(R.drawable.ic_d10_dice),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(16.dp)
                                    .align(Alignment.BottomEnd),
                                tint = colorScheme.primary
                            )
                        }
                    }
                    else -> {
                        val iconRes = when (sides) {
                            2 -> R.drawable.ic_d2_dice
                            4 -> R.drawable.ic_d4_dice
                            6 -> R.drawable.ic_d6_dice
                            8 -> R.drawable.ic_d8_dice
                            10 -> R.drawable.ic_d10_dice
                            12 -> R.drawable.ic_d12_dice
                            20 -> R.drawable.ic_d20_dice
                            else -> R.drawable.ic_d20_dice
                        }
                        Icon(
                            painter = painterResource(iconRes),
                            contentDescription = "d$sides",
                            modifier = Modifier.size(22.dp),
                            tint = colorScheme.primary
                        )
                    }
                }
            }
        }

        // 3. Stroke layer (drawn on top of icon)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    if (count > 0) BorderStroke(2.dp, colorScheme.primary)
                    else BorderStroke(1.dp, Color.White.copy(alpha = if (isOled) 0.3f else 0.1f)),
                    CircleShape
                )
        )

        // 4. Count (Badge) layer
        if (count > 0) {
            Badge(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 3.dp, y = (-3).dp),
                containerColor = colorScheme.primary,
                contentColor = colorScheme.onPrimary
            ) {
                Text(count.toString())
            }
        }
    }
}

@Composable
fun RollItem(result: RollResult, isCompact: Boolean, isOled: Boolean) {
    val colorScheme = MaterialTheme.colorScheme
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = result.title,
                color = getRollSourceColor(result.sourceType, colorScheme).copy(alpha = if (isCompact) 0.6f else 0.9f),
                fontSize = if (isCompact) 15.sp else 18.sp,
                fontWeight = if (isCompact) FontWeight.Bold else FontWeight.ExtraBold,
                modifier = Modifier.weight(1f)
            )

            val isCrit = result.isCriticalFailure || result.isCriticalSuccess
            val fontSize = when {
                !isCompact -> 42.sp
                isCrit -> 42.sp
                else -> 28.sp
            }

            val resultColor = when {
                result.isCriticalFailure -> Color(0xFFEF5350)
                result.isCriticalSuccess -> if (isOled) Color(0xFF00E1FF) else colorScheme.primary
                else -> colorScheme.onSurface
            }

            val brush = if (result.isCriticalSuccess) {
                Brush.linearGradient(
                    colors = listOf(Color(0xFF00E1FF), Color(0xFF00ffd9))
                )
            } else if (result.isCriticalFailure) {
                Brush.linearGradient(
                    colors = listOf(Color(0xFFFF5252), Color(0xFFFFB74D))
                )
            } else null

            Row(verticalAlignment = Alignment.Bottom) {
                if (result.unusedTotal != null) {
                    Text(
                        text = result.unusedTotal.toString(),
                        color = colorScheme.onSurface.copy(alpha = 0.4f),
                        style = TextStyle(
                            fontSize = (fontSize.value * 0.5f).sp,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.padding(end = 8.dp, bottom = (fontSize.value * 0.15f).dp)
                    )
                }
                Text(
                    text = if (result.isCriticalFailure && !result.isDamage) "1" else result.total.toString(),
                    color = if (brush == null) resultColor else Color.Unspecified,
                    style = TextStyle(
                        brush = brush,
                        fontSize = fontSize,
                        fontWeight = FontWeight.Black
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(if (isCompact) 2.dp else 4.dp))
        val hasAlt = (result.advantageType != AdvantageType.NONE || result.alternativeDice != null || result.alternativeFlatBonuses != null)
        if (hasAlt && !isCompact) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = buildStyledBreakdown(result, colorScheme, isOled, useSecond = false),
                    fontSize = 18.sp,
                    lineHeight = 22.sp
                )
                Text(
                    text = buildStyledBreakdown(result, colorScheme, isOled, useSecond = true),
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    color = colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        } else {
            Text(
                text = buildStyledBreakdown(result, colorScheme, isOled),
                fontSize = if (isCompact) 14.sp else 18.sp,
                lineHeight = if (isCompact) 16.sp else 22.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

fun buildStyledBreakdown(result: RollResult, colorScheme: ColorScheme, isOled: Boolean, useSecond: Boolean = false): AnnotatedString {
    return buildAnnotatedString {
        var first = true

        val diceList = if (useSecond) result.alternativeDice ?: emptyList() else result.bonusDice
        val flatList = if (useSecond) result.alternativeFlatBonuses ?: emptyList() else result.flatBonuses
        val d20Value = if (useSecond) result.alternativeD20 else result.mainD20

        d20Value?.let { value ->
            val d20Color = getD20Color(value, colorScheme.primary, isOled)
            withStyle(SpanStyle(color = d20Color, fontWeight = FontWeight.Black)) {
                append(value.toString())
            }
            first = false
        }

        diceList.forEach { dice ->
            val diceVal = dice.value
            val sign = if (diceVal >= 0) " + " else " - "
            withStyle(SpanStyle(color = colorScheme.onSurface.copy(alpha = if (useSecond) 0.4f else 0.7f), fontWeight = FontWeight.Light)) {
                if (!first) append(sign)
                else if (diceVal < 0) append("-")
            }

            withStyle(SpanStyle(color = getDiceColor(kotlin.math.abs(diceVal), dice.sides).copy(alpha = if (useSecond) 0.6f else 1.0f), fontWeight = FontWeight.Bold)) {
                append(kotlin.math.abs(diceVal).toString())
            }

            dice.discardedValue?.let { disc ->
                withStyle(SpanStyle(color = colorScheme.onSurface.copy(alpha = 0.4f), fontWeight = FontWeight.Normal, textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)) {
                    append(" (")
                    append(kotlin.math.abs(disc).toString())
                    append(")")
                }
            }
            first = false
        }

        flatList.forEach { bonusVal ->
            if (bonusVal == 0) return@forEach
            val sign = if (bonusVal >= 0) " + " else " - "
            withStyle(SpanStyle(color = colorScheme.onSurface.copy(alpha = if (useSecond) 0.4f else 0.7f), fontWeight = FontWeight.Light)) {
                if (!first) append(sign)
                else if (bonusVal < 0) append("-")
            }

            withStyle(SpanStyle(color = colorScheme.onSurface.copy(alpha = if (useSecond) 0.6f else 1.0f), fontWeight = FontWeight.Normal)) {
                append(kotlin.math.abs(bonusVal).toString())
            }
            first = false
        }

        if (length == 0) {
            withStyle(SpanStyle(color = colorScheme.onSurface.copy(alpha = 0.6f))) {
                append(result.breakdown)
            }
        }
    }
}

fun getRollSourceColor(sourceType: RollSourceType, colorScheme: ColorScheme): Color {
    val isDark = colorScheme.surface.luminance() < 0.5f
    return when (sourceType) {
        RollSourceType.ABILITY -> if (isDark) Color(0xFF81D4FA) else Color(0xFF0288D1)
        RollSourceType.SKILL -> if (isDark) Color(0xFFA5D6A7) else Color(0xFF388E3C)
        RollSourceType.SAVING_THROW -> if (isDark) Color(0xFFFFCC80) else Color(0xFFF57C00)
        RollSourceType.ATTACK -> if (isDark) Color(0xFFEF9A9A) else Color(0xFFD32F2F)
        RollSourceType.OTHER -> colorScheme.onSurface
    }
}

fun getDiceColor(value: Int, sides: Int): Color {
    if (sides <= 1) return Color.White
    val ratio = ((value - 1).toFloat() / (sides - 1).toFloat()).coerceIn(0f, 1f)

    return if (ratio < 0.5f) {
        val localRatio = (ratio * 2f).coerceIn(0f, 1f)
        Color(red = 1f, green = localRatio, blue = 0f)
    } else {
        val localRatio = ((ratio - 0.5f) * 2f).coerceIn(0f, 1f)
        Color(red = (1f - localRatio).coerceIn(0f, 1f), green = 1f, blue = 0f)
    }
}

fun getD20Color(value: Int, themeColor: Color, isOled: Boolean): Color {
    val ratio = ((value - 1).toFloat() / 19f).coerceIn(0f, 1f)
    return if (isOled) {
        if (value == 1) Color.Red
        else {
            val blue = Color(0xFF00E1FF)
            val purple = Color(0xFF868efc)
            lerp(blue, purple, ratio)
        }
    } else {
        lerp(Color.Red, themeColor, ratio)
    }
}

@Composable
fun DiceRollAdvantagePopup(
    onAdvantage: () -> Unit,
    onDisadvantage: () -> Unit,
    onDismiss: () -> Unit,
    onCritical: (() -> Unit)? = null,
    hazeState: HazeState? = null,
    isOled: Boolean = false,
    widthMultiplier: Float = 1f,
    modifier: Modifier = Modifier
) {
    Popup(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.PopupProperties(focusable = true)
    ) {
        AppScaleProvider(LocalAppScale.current) {
            val colorScheme = MaterialTheme.colorScheme
            val critBrush = Brush.linearGradient(
                colors = listOf(Color(0xFF00E1FF), Color(0xFF00ffd9))
            )

            Surface(
                modifier = modifier
                    .fillMaxWidth(widthMultiplier)
                    .shadow(8.dp, RoundedCornerShape(12.dp))
                    .run {
                        if (hazeState != null && !isOled) {
                            this.clip(RoundedCornerShape(12.dp))
                                .then(
                                    remember(hazeState) {
                                        Modifier.hazeEffect(state = hazeState, style = DiceRollHazeStyle) {
                                            inputScale = HazeInputScale.Fixed(0.6f)
                                        }
                                    }
                                )
                        } else this
                    },
                shape = RoundedCornerShape(12.dp),
                color = if (isOled) Color.Black else if (hazeState != null) colorScheme.surface.copy(alpha = 0.4f) else colorScheme.surface,
                tonalElevation = if (isOled || hazeState != null) 0.dp else 8.dp,
                border = BorderStroke(1.dp, Color.White.copy(alpha = if (isOled) 0.3f else 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(2.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            onAdvantage()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowUp,
                            contentDescription = "Преимущество",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    
                    if (onCritical != null) {
                        VerticalDivider(modifier = Modifier.height(20.dp), color = colorScheme.outlineVariant.copy(alpha = 0.5f))
                        IconButton(
                            onClick = {
                                onCritical()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        ) {
                            Text(
                                "!",
                                style = TextStyle(
                                    brush = critBrush,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black
                                )
                            )
                        }
                    }

                    VerticalDivider(modifier = Modifier.height(20.dp), color = colorScheme.outlineVariant.copy(alpha = 0.5f))
                    
                    IconButton(
                        onClick = {
                            onDisadvantage()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = "Помеха",
                            tint = Color(0xFFF44336),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}
