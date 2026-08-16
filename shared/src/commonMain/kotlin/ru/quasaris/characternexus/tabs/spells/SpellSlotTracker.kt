package ru.quasaris.characternexus.tabs.spells

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import ru.quasaris.characternexus.model.*
import kotlin.math.abs

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpellSlotTracker(
    maxSlots: Int,
    usedSlots: Int,
    onUsedSlotsChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isShortRest: Boolean = false,
    isDawnRest: Boolean = false,
    alignment: SlotAlignment = SlotAlignment.RIGHT,
    fillDirection: SlotFillDirection = SlotFillDirection.LTR
) {
    if (maxSlots <= 0) return

    val haptic = LocalHapticFeedback.current
    val colorScheme = MaterialTheme.colorScheme
    val color = when {
        isDawnRest -> Color(0xFFCE93D8)
        isShortRest -> colorScheme.secondary
        else -> colorScheme.primary
    }

    FlowRow(
        modifier = modifier,
        horizontalArrangement = when (alignment) {
            SlotAlignment.LEFT -> Arrangement.Start
            SlotAlignment.CENTER -> Arrangement.Center
            SlotAlignment.RIGHT -> Arrangement.End
        },
        verticalArrangement = Arrangement.spacedBy(4.dp),
        maxItemsInEachRow = Int.MAX_VALUE
    ) {
        val centerIndex = (maxSlots - 1) / 2f
        val fillOrder = remember(maxSlots, fillDirection) {
            when (fillDirection) {
                SlotFillDirection.LTR -> (0 until maxSlots).toList()
                SlotFillDirection.RTL -> (maxSlots - 1 downTo 0).toList()
                SlotFillDirection.CENTER -> {
                    (0 until maxSlots).sortedBy { abs(it - centerIndex) }
                }
            }
        }

        for (i in 0 until maxSlots) {
            val isFilled = when (fillDirection) {
                SlotFillDirection.LTR -> i < usedSlots
                SlotFillDirection.RTL -> i >= maxSlots - usedSlots
                SlotFillDirection.CENTER -> {
                    val visualPositionsToFill = fillOrder.take(usedSlots)
                    i in visualPositionsToFill
                }
            }
            
            val shape = when {
                isShortRest -> CircleShape
                isDawnRest -> RoundedCornerShape(8.dp)
                else -> RoundedCornerShape(4.dp)
            }
            
            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .size(24.dp)
                    .clip(shape)
                    .then(
                        if (isFilled) Modifier.background(color)
                        else Modifier.border(1.5.dp, color.copy(alpha = 0.5f), shape)
                    )
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val newUsed = if (isFilled) usedSlots - 1 else usedSlots + 1
                        onUsedSlotsChange(newUsed)
                    }
            )
        }
    }
}
