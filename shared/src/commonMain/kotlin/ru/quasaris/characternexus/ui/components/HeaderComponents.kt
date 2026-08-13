package ru.quasaris.characternexus.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StatIconBox(
    value: String, 
    icon: DrawableResource, 
    onClick: () -> Unit = {}, 
    isHighlighted: Boolean = false, 
    onLongClick: (() -> Unit)? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .size(46.dp)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                },
                onLongClick = onLongClick?.let {
                    {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        it()
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        val tint = if (isHighlighted)
            colorScheme.primary.copy(alpha = 0.75f)
        else
            colorScheme.onSurface.copy(alpha = 0.55f)

        Image(
            painter = painterResource(icon), 
            contentDescription = null, 
            modifier = Modifier.fillMaxSize(), 
            colorFilter = ColorFilter.tint(tint)
        )

        Box(contentAlignment = Alignment.Center) {
            val strokeColor = colorScheme.background
            val fontSize = 18.sp
            val strokeWidth = with(density) { (fontSize.toPx() * 0.1f) }

            Text(
                value,
                fontSize = fontSize,
                color = strokeColor,
                fontWeight = FontWeight.Bold,
                style = TextStyle(
                    drawStyle = Stroke(width = strokeWidth)
                )
            )
            Text(
                value,
                fontSize = fontSize,
                color = colorScheme.onSurface,
                fontWeight = FontWeight.Black
            )
        }
    }
}
