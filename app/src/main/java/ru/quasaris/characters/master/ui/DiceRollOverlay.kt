package ru.quasaris.characters.master.ui

import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalView
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
import ru.quasaris.characters.master.backend.AdvantageType
import ru.quasaris.characters.master.backend.AppScaleProvider
import ru.quasaris.characters.master.backend.AppThemeMode
import ru.quasaris.characters.master.backend.DiceRollPosition
import ru.quasaris.characters.master.backend.LocalAppScale
import ru.quasaris.characters.master.backend.RollResult
import ru.quasaris.characters.master.backend.RollSourceType
import kotlin.math.roundToInt

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

    // Стейт для хранения абсолютных экранных координат ВЕРХНЕГО ПРАВОГО УГЛА панели.
    // Именно к ним мы привяжем второе, полностью независимое окно.
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
                                        remember(history.size) {
                                            Modifier.hazeEffect(state = hazeState) {
                                                inputScale = HazeInputScale.Fixed(0.6f)
                                            }
                                        }
                                    )
                            } else this
                        },
                    shape = RoundedCornerShape(24.dp),
                    color = if (isOled) Color.Black else colorScheme.surface.copy(alpha = alpha),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = if (isOled) 0.3f else 0.1f)),
                    tonalElevation = if (isOled) 0.dp else 8.dp
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
                                        remember(history.size) {
                                            Modifier.hazeEffect(state = hazeState) {
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
                    color = if (isOled) Color.Black else colorScheme.surface.copy(alpha = alpha),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = if (isOled) 0.3f else 0.1f)),
                    tonalElevation = if (isOled) 0.dp else 4.dp
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
                                .hazeEffect(state = hazeState) {
                                    inputScale = HazeInputScale.Fixed(0.6f)
                                }
                        } else this
                    },
                shape = RoundedCornerShape(12.dp),
                color = if (isOled) Color.Black else if (hazeState != null) colorScheme.surface.copy(alpha = 0.4f) else colorScheme.surface,
                tonalElevation = 8.dp,
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

