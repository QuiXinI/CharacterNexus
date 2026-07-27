package ru.quasaris.characters.master.ui

import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import android.graphics.Region
import java.lang.reflect.Proxy
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import ru.quasaris.characters.master.backend.RollSourceType
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.LocalHazeStyle
import ru.quasaris.characters.master.backend.AppThemeMode
import ru.quasaris.characters.master.backend.RollResult
import ru.quasaris.characters.master.backend.DiceRollPosition

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
    alpha: Float = 0.4f,
    isPassThrough: Boolean = true,
    position: DiceRollPosition = DiceRollPosition.BOTTOM_LEFT,
    modifier: Modifier = Modifier
) {
    if (history.isEmpty()) return

    val isOled = themeMode == AppThemeMode.OFF
    val latest = remember(history, history.size) { history.firstOrNull() }
    val previous = remember(history, history.size) { history.drop(1).reversed() }
    val colorScheme = MaterialTheme.colorScheme

    var closeButtonRect by remember { mutableStateOf<Rect?>(null) }

    Dialog(
        onDismissRequest = onClose,
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
                // Настройка параметров окна для позиционирования в углу и прозрачности
                w.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
                w.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)

                // Флаг NOT_TOUCHABLE убираем, чтобы мы могли перехватывать нажатия на кнопку закрытия.
                w.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                w.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                w.setDimAmount(0f)

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

        // Сохраняем актуальные значения, чтобы не пересоздавать listener при каждом изменении
        val currentIsPassThrough by rememberUpdatedState(isPassThrough)
        val currentCloseButtonRect by rememberUpdatedState(closeButtonRect)

        // Управление областью касания (Touchable Region) через рефлексию
        DisposableEffect(window) {
            if (window == null) return@DisposableEffect onDispose {}

            val view = window.decorView
            val viewTreeObserver = view.viewTreeObserver
            var listener: Any? = null
            var removeListener: (() -> Unit)? = null

            try {
                val listenerClass = Class.forName("android.view.ViewTreeObserver\$OnComputeInternalInsetsListener")
                val internalInsetsInfoClass = Class.forName("android.view.ViewTreeObserver\$InternalInsetsInfo")

                // Надежный обход скрытых API (Greylist)
                val setTouchableInsetsMethod = internalInsetsInfoClass.declaredMethods.find { it.name == "setTouchableInsets" }?.apply { isAccessible = true }
                val touchableRegionField = internalInsetsInfoClass.declaredFields.find { it.name == "touchableRegion" }?.apply { isAccessible = true }

                if (setTouchableInsetsMethod != null && touchableRegionField != null) {
                    listener = Proxy.newProxyInstance(
                        listenerClass.classLoader,
                        arrayOf(listenerClass)
                    ) { _, method, args ->
                        if (method.name == "onComputeInternalInsets") {
                            val info = args?.get(0)
                            if (info != null && currentIsPassThrough) {
                                // 3 == TOUCHABLE_INSETS_REGION
                                setTouchableInsetsMethod.invoke(info, 3)
                                val region = touchableRegionField.get(info) as Region
                                region.setEmpty() // Делаем всё окно пропускающим клики

                                // Вырезаем из пустоты "островок" нашей кнопки, делая её кликабельной
                                currentCloseButtonRect?.let { rect ->
                                    region.set(
                                        rect.left.toInt(),
                                        rect.top.toInt(),
                                        rect.right.toInt(),
                                        rect.bottom.toInt()
                                    )
                                }
                            }
                        }
                        null
                    }

                    val addMethod = viewTreeObserver.javaClass.methods.find { it.name == "addOnComputeInternalInsetsListener" }?.apply { isAccessible = true }
                    addMethod?.invoke(viewTreeObserver, listener)

                    removeListener = {
                        try {
                            // Получаем актуальный ViewTreeObserver перед удалением
                            val vto = if (view.viewTreeObserver.isAlive) view.viewTreeObserver else view.viewTreeObserver
                            val removeMethod = vto.javaClass.methods.find { it.name == "removeOnComputeInternalInsetsListener" }?.apply { isAccessible = true }
                            removeMethod?.invoke(vto, listener)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            onDispose {
                removeListener?.invoke()
            }
        }

        // Принудительно заставляем Android пересчитать Insets при смене позиции кнопки или состояния проницаемости
        LaunchedEffect(isPassThrough, closeButtonRect) {
            window?.decorView?.requestLayout()
        }

        // Оборачиваем в CompositionLocalProvider для передачи стиля в hazeEffect
        CompositionLocalProvider(LocalHazeStyle provides DiceRollHazeStyle) {
            Surface(
                modifier = modifier
                    .padding(16.dp)
                    .widthIn(min = 280.dp, max = 340.dp)
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
                    // История предыдущих бросков
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

                    // Кнопка закрытия
                    Box(modifier = Modifier.fillMaxWidth()) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(32.dp)
                                .border(1.dp, colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                .onGloballyPositioned { coords ->
                                    val pos = coords.positionInWindow()
                                    val newRect = Rect(pos.x, pos.y, pos.x + coords.size.width, pos.y + coords.size.height)
                                    // Обязательная проверка, чтобы не уйти в бесконечный цикл рекомпозиции
                                    if (closeButtonRect != newRect) {
                                        closeButtonRect = newRect
                                    }
                                }
                        ) {
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

        Spacer(modifier = Modifier.height(if (isCompact) 2.dp else 4.dp))
        Text(
            text = buildStyledBreakdown(result, colorScheme, isOled),
            fontSize = if (isCompact) 14.sp else 18.sp,
            lineHeight = if (isCompact) 16.sp else 22.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

fun buildStyledBreakdown(result: RollResult, colorScheme: ColorScheme, isOled: Boolean): AnnotatedString {
    return buildAnnotatedString {
        var first = true

        result.mainD20?.let { value ->
            val d20Color = getD20Color(value, colorScheme.primary, isOled)
            withStyle(SpanStyle(color = d20Color, fontWeight = FontWeight.Black)) {
                append(value.toString())
            }
            first = false
        }

        result.bonusDice.forEach { dice ->
            val diceVal = dice.value
            val sign = if (diceVal >= 0) " + " else " - "
            withStyle(SpanStyle(color = colorScheme.onSurface.copy(alpha = 0.7f), fontWeight = FontWeight.Light)) {
                if (!first) append(sign)
                else if (diceVal < 0) append("-")
            }

            withStyle(SpanStyle(color = getDiceColor(kotlin.math.abs(diceVal), dice.sides), fontWeight = FontWeight.Bold)) {
                append(kotlin.math.abs(diceVal).toString())
            }
            first = false
        }

        result.flatBonuses.forEach { bonusVal ->
            val sign = if (bonusVal >= 0) " + " else " - "
            withStyle(SpanStyle(color = colorScheme.onSurface.copy(alpha = 0.7f), fontWeight = FontWeight.Light)) {
                if (!first) append(sign)
                else if (bonusVal < 0) append("-")
            }

            withStyle(SpanStyle(color = colorScheme.onSurface, fontWeight = FontWeight.Normal)) {
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