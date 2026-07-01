package ru.quasaris.characters.master.ui

import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import ru.quasaris.characters.master.backend.AppThemeMode
import ru.quasaris.characters.master.backend.RollResult

@Composable
fun DiceRollOverlay(
    history: List<RollResult>,
    onClose: () -> Unit,
    themeMode: AppThemeMode = AppThemeMode.M3,
    modifier: Modifier = Modifier
) {
    if (history.isEmpty()) return

    val isOled = themeMode == AppThemeMode.OFF
    val latest = history.first()
    val previous = history.drop(1).reversed()
    val colorScheme = MaterialTheme.colorScheme

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
                // FLAG_NOT_TOUCH_MODAL + FLAG_NOT_FOCUSABLE allows interaction with what's behind
                w.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
                w.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
                w.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                w.setDimAmount(0f)

                // Position the window in the bottom-left corner
                val params = w.attributes
                params.gravity = Gravity.BOTTOM or Gravity.START
                params.width = WindowManager.LayoutParams.WRAP_CONTENT
                params.height = WindowManager.LayoutParams.WRAP_CONTENT
                w.attributes = params

                if (!isOled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    w.setBackgroundBlurRadius(120)
                }
                w.setBackgroundDrawableResource(android.R.color.transparent)
            }
        }

        Surface(
            modifier = Modifier
                .padding(16.dp)
                .widthIn(min = 280.dp, max = 340.dp),
            shape = RoundedCornerShape(24.dp),
            color = if (isOled) Color.Black else colorScheme.surface.copy(alpha = 0.2f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = if (isOled) 0.3f else 0.1f)),
            tonalElevation = if (isOled) 0.dp else 8.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // History (Previous) at the top
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

                // Latest Roll at the bottom
                RollItem(latest, isCompact = false, isOled = isOled)

                // Small Circular Close Button
                Box(modifier = Modifier.fillMaxWidth()) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(32.dp)
                            .border(1.dp, colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
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
                color = colorScheme.onSurface.copy(alpha = if (isCompact) 0.6f else 0.9f),
                fontSize = if (isCompact) 15.sp else 18.sp,
                fontWeight = if (isCompact) FontWeight.Bold else FontWeight.ExtraBold,
                modifier = Modifier.weight(1f)
            )

            val totalColor = when {
                result.isCriticalFailure -> Color(0xFFEF5350)
                result.isCriticalSuccess -> if (isOled) Color(0xFF00E1FF) else colorScheme.primary
                else -> colorScheme.onSurface
            }

            // Sizes for crit 20 and crit 1 should be the same as current even in compact
            val isCrit = result.isCriticalFailure || result.isCriticalSuccess
            val fontSize = when {
                !isCompact -> 42.sp
                isCrit -> 42.sp // Same as current for crits
                else -> 28.sp
            }

            Text(
                text = if (result.isCriticalFailure && !result.isDamage) "1" else result.total.toString(),
                color = totalColor,
                fontSize = fontSize,
                fontWeight = FontWeight.Black
            )
        }

        Spacer(modifier = Modifier.height(if (isCompact) 2.dp else 4.dp))
        Text(
            text = buildStyledBreakdown(result, colorScheme),
            fontSize = if (isCompact) 14.sp else 18.sp,
            lineHeight = if (isCompact) 16.sp else 22.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

fun buildStyledBreakdown(result: RollResult, colorScheme: ColorScheme): AnnotatedString {
    return buildAnnotatedString {
        var first = true

        // Main D20
        result.mainD20?.let {
            withStyle(SpanStyle(color = colorScheme.onSurface, fontWeight = FontWeight.Black)) {
                append(it.toString())
            }
            first = false
        }

        // Bonus Dice
        result.bonusDice.forEach { dice ->
            val diceVal = dice.value
            if (!first) append(if (diceVal >= 0) " + " else " - ")
            else if (diceVal < 0) append("-")

            withStyle(SpanStyle(color = getDiceColor(kotlin.math.abs(diceVal), dice.sides), fontWeight = FontWeight.Bold)) {
                append(kotlin.math.abs(diceVal).toString())
            }
            first = false
        }

        // Flat Bonuses
        result.flatBonuses.forEach { bonusVal ->
            if (!first) append(if (bonusVal >= 0) " + " else " - ")
            else if (bonusVal < 0) append("-")

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

fun getDiceColor(value: Int, sides: Int): Color {
    if (sides <= 1) return Color.White
    val ratio = (value - 1).toFloat() / (sides - 1).toFloat()

    return if (ratio < 0.5f) {
        val localRatio = ratio * 2f
        Color(red = 1f, green = localRatio, blue = 0f)
    } else {
        val localRatio = (ratio - 0.5f) * 2f
        Color(red = 1f - localRatio, green = 1f, blue = 0f)
    }
}
