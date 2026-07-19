package ru.quasaris.characters.master.tabs.spells

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpellSlotTracker(
    maxSlots: Int,
    usedSlots: Int,
    onUsedSlotsChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isShortRest: Boolean = false
) {
    if (maxSlots <= 0) return

    val haptic = LocalHapticFeedback.current
    val colorScheme = MaterialTheme.colorScheme
    val color = if (isShortRest) colorScheme.secondary else colorScheme.primary

    FlowRow(
        modifier = modifier,
        horizontalArrangement = if (isShortRest) Arrangement.End else Arrangement.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        maxItemsInEachRow = Int.MAX_VALUE
    ) {
        val range = if (isShortRest) (maxSlots - 1 downTo 0) else (0 until maxSlots)
        for (index in range) {
            val isFilled = index < usedSlots
            val shape = if (isShortRest) CircleShape else RoundedCornerShape(4.dp)
            
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
                        onUsedSlotsChange(if (isFilled) usedSlots - 1 else usedSlots + 1)
                    }
            )
        }
    }
}
