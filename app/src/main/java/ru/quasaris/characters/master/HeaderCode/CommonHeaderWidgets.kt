package ru.quasaris.characters.master.HeaderCode

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.quasaris.characters.master.R

val SquirclePath = GenericShape { size, _ ->
    val r = size.width * 0.25f
    moveTo(r, 0f)
    lineTo(size.width - r, 0f)
    quadraticTo(size.width, 0f, size.width, r)
    lineTo(size.width, size.height - r)
    quadraticTo(size.width, size.height, size.width - r, size.height)
    lineTo(r, size.height)
    quadraticTo(0f, size.height, 0f, size.height - r)
    lineTo(0f, r)
    quadraticTo(0f, 0f, r, 0f)
    close()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StatIconBox(value: String, iconRes: Int, onClick: () -> Unit = {}, isHighlighted: Boolean = false, onLongClick: (() -> Unit)? = null) {
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

        if (iconRes == R.drawable.ic_sword) {
            Box(Modifier.fillMaxSize()) {
                Image(painterResource(R.drawable.ic_sword), null, modifier = Modifier.size(46.dp), colorFilter = ColorFilter.tint(tint))
                Image(painterResource(R.drawable.ic_sword), null, modifier = Modifier.size(46.dp).graphicsLayer(scaleX = -1f), colorFilter = ColorFilter.tint(tint))
            }
        } else Image(painterResource(iconRes), null, modifier = Modifier.fillMaxSize(), colorFilter = ColorFilter.tint(tint))

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
