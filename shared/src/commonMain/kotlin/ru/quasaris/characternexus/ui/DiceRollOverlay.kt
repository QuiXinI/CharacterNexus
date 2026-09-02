package ru.quasaris.characternexus.ui

import ru.quasaris.characternexus.model.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.*
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt
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

import ru.quasaris.characternexus.ui.theme.rememberEffectiveBlurRadius
import ru.quasaris.characternexus.ui.theme.hazePopover

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
        Box(
            modifier = Modifier
                .widthIn(min = 280.dp, max = 340.dp)
                .run {
                    if (forceBlurEnabled && hazeState != null && !isOled) {
                        this.clip(RoundedCornerShape(24.dp))
                            .hazeEffect(state = hazeState, style = DiceRollHazeStyle)
                            .background(colorScheme.surface.copy(alpha = 0.4f))
                    } else {
                        this.outerShadow(shape = RoundedCornerShape(24.dp), blur = 8.dp)
                            .background(
                                color = when {
                                    isOled -> Color.Black
                                    else -> colorScheme.surface.copy(alpha = alpha)
                                },
                                shape = RoundedCornerShape(24.dp)
                            )
                    }
                }
                .border(1.dp, Color.White.copy(alpha = if (isOled) 0.3f else 0.1f), RoundedCornerShape(24.dp))
                .then(
                    if (isPassThrough) Modifier 
                    else Modifier.pointerInput(Unit) { detectTapGestures { } } // Блокируем клики, если НЕ пропуск
                )
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

        // Отдельная кнопка закрытия, привязанная к углу
        Surface(
            modifier = Modifier
                .align(when(closeButtonPosition) {
                    DiceRollPosition.TOP_LEFT -> Alignment.TopStart
                    DiceRollPosition.TOP_RIGHT -> Alignment.TopEnd
                    DiceRollPosition.BOTTOM_LEFT -> Alignment.BottomStart
                    DiceRollPosition.BOTTOM_RIGHT -> Alignment.BottomEnd
                })
                .offset(
                    x = when(closeButtonPosition) {
                        DiceRollPosition.TOP_LEFT, DiceRollPosition.BOTTOM_LEFT -> (-8).dp
                        DiceRollPosition.TOP_RIGHT, DiceRollPosition.BOTTOM_RIGHT -> 8.dp
                    },
                    y = when(closeButtonPosition) {
                        DiceRollPosition.TOP_LEFT, DiceRollPosition.TOP_RIGHT -> (-8).dp
                        DiceRollPosition.BOTTOM_LEFT, DiceRollPosition.BOTTOM_RIGHT -> 8.dp
                    }
                )
                .size(32.dp)
                .run {
                    if (forceBlurEnabled && hazeState != null && !isOled) {
                        this.clip(RoundedCornerShape(12.dp))
                            .hazeEffect(state = hazeState, style = DiceRollHazeStyle)
                    } else this
                }
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClose),
            shape = RoundedCornerShape(12.dp),
            color = if (isOled) Color.Black else colorScheme.surfaceVariant.copy(alpha = 0.9f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
            tonalElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Close, 
                    contentDescription = "Close", 
                    modifier = Modifier.size(18.dp),
                    tint = colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// DiceRollingFab and DiceMenuItem removed, moved to DiceRollerFab.kt

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
                result.isHealing -> Color(0xFF00C46F)
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
            } else if (result.isHealing) {
                Brush.linearGradient(
                    colors = listOf(Color(0xFF00C46F), Color(0xFF69F0AE))
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

        val ordered = if (useSecond) result.altOrderedParts else result.orderedParts
        val d20Value = if (useSecond) result.alternativeD20 else result.mainD20

        d20Value?.let { value ->
            val d20Color = getD20Color(value, colorScheme.primary, isOled)
            withStyle(SpanStyle(color = d20Color, fontWeight = FontWeight.Black)) {
                append(value.toString())
            }
            first = false
        }

        if (ordered != null && ordered.isNotEmpty()) {
            ordered.forEach { part ->
                when (part) {
                    is RollPart.Dice -> {
                        val diceVal = part.roll.value
                        val sign = if (diceVal >= 0) " + " else " - "
                        withStyle(SpanStyle(color = colorScheme.onSurface.copy(alpha = if (useSecond) 0.4f else 0.7f), fontWeight = FontWeight.Light)) {
                            if (!first) append(sign)
                            else if (diceVal < 0) append("-")
                        }

                        withStyle(SpanStyle(color = getDiceColor(kotlin.math.abs(diceVal), part.roll.sides).copy(alpha = if (useSecond) 0.6f else 1.0f), fontWeight = FontWeight.Bold)) {
                            append(kotlin.math.abs(diceVal).toString())
                        }

                        part.roll.discardedValue?.let { disc ->
                            withStyle(SpanStyle(color = colorScheme.onSurface.copy(alpha = 0.4f), fontWeight = FontWeight.Normal, textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)) {
                                append(" (")
                                append(kotlin.math.abs(disc).toString())
                                append(")")
                            }
                        }
                    }
                    is RollPart.Flat -> {
                        val bonusVal = part.value
                        if (bonusVal == 0) return@forEach
                        val sign = if (bonusVal >= 0) " + " else " - "
                        withStyle(SpanStyle(color = colorScheme.onSurface.copy(alpha = if (useSecond) 0.4f else 0.7f), fontWeight = FontWeight.Light)) {
                            if (!first) append(sign)
                            else if (bonusVal < 0) append("-")
                        }

                        withStyle(SpanStyle(color = colorScheme.onSurface.copy(alpha = if (useSecond) 0.6f else 1.0f), fontWeight = FontWeight.Normal)) {
                            append(kotlin.math.abs(bonusVal).toString())
                        }
                    }
                }
                first = false
            }
        } else {
            // Fallback to legacy lists if orderedParts is not available
            val diceList = if (useSecond) result.alternativeDice ?: emptyList() else result.bonusDice
            val flatList = if (useSecond) result.alternativeFlatBonuses ?: emptyList() else result.flatBonuses

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
    onUpcast: (() -> Unit)? = null,
    hazeState: HazeState? = null,
    isOled: Boolean = false,
    widthMultiplier: Float = 1f,
    modifier: Modifier = Modifier,
    settingsViewModel: ru.quasaris.characternexus.backend.SettingsViewModel? = null
) {
    Popup(
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        val colorScheme = MaterialTheme.colorScheme
        val critBrush = Brush.linearGradient(colors = listOf(Color(0xFF00E1FF), Color(0xFF00ffd9)))
        val goldenBrush = Brush.linearGradient(colors = listOf(Color(0xFFFFD700), Color(0xFFFFA500)))
        val advBrush = Brush.linearGradient(colors = listOf(Color(0xFF00ff5e), Color(0xFF92cf80)))
        val disBrush = Brush.linearGradient(colors = listOf(Color(0xFFFF1100), Color(0xFFE18275)))

        val blurRadius = rememberEffectiveBlurRadius(settingsViewModel)

        Surface(
            modifier = modifier
                .fillMaxWidth(widthMultiplier)
                .outerShadow(RoundedCornerShape(12.dp), blur = 8.dp)
                .hazePopover(
                    state = hazeState,
                    blurRadius = blurRadius,
                    isOled = isOled
                ),
            shape = RoundedCornerShape(12.dp),
            color = if (isOled) Color.Black else if (hazeState != null) colorScheme.surface.copy(alpha = 0.2f) else colorScheme.surface,
            border = BorderStroke(1.dp, Color.White.copy(alpha = if (isOled) 0.3f else 0.1f)),
            tonalElevation = if (isOled || hazeState != null) 0.dp else 8.dp
        ) {
            Row(
                modifier = Modifier.padding(2.dp).height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onAdvantage(); onDismiss() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(28.dp)
                            .graphicsLayer(alpha = 0.99f)
                            .drawWithContent {
                                drawContent()
                                drawRect(advBrush, blendMode = BlendMode.SrcIn)
                            }
                    )
                }

                if (onCritical != null) {
                    VerticalDivider(modifier = Modifier.padding(vertical = 8.dp).fillMaxHeight(), color = colorScheme.outlineVariant.copy(alpha = 0.5f))
                    IconButton(
                        onClick = { onCritical(); onDismiss() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "!",
                            style = TextStyle(brush = critBrush, fontSize = 28.sp, fontWeight = FontWeight.Black)
                        )
                    }
                }

                if (onUpcast != null) {
                    VerticalDivider(modifier = Modifier.padding(vertical = 8.dp).fillMaxHeight(), color = colorScheme.outlineVariant.copy(alpha = 0.5f))
                    IconButton(
                        onClick = { onUpcast(); onDismiss() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardDoubleArrowUp,
                            null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(28.dp)
                                .graphicsLayer(alpha = 0.99f)
                                .drawWithContent {
                                    drawContent()
                                    drawRect(goldenBrush, blendMode = BlendMode.SrcIn)
                                }
                        )
                    }
                }

                VerticalDivider(modifier = Modifier.padding(vertical = 8.dp).fillMaxHeight(), color = colorScheme.outlineVariant.copy(alpha = 0.5f))
                
                IconButton(
                    onClick = { onDisadvantage(); onDismiss() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(28.dp)
                            .graphicsLayer(alpha = 0.99f)
                            .drawWithContent {
                                drawContent()
                                drawRect(disBrush, blendMode = BlendMode.SrcIn)
                            }
                    )
                }
            }
        }
    }
}
