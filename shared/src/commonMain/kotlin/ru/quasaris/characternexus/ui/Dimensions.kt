package ru.quasaris.characternexus.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object Dimensions {
    val DesktopLeftColumnWidth = 450.dp
    val MinDesktopLeftColumnWidth = 320.dp
    val OverlayCornerRadius = 24.dp
    val TabButtonSize = 40.dp
    val HeaderHeight = 56.dp
    val DesktopSplitThreshold = 800.dp

    /**
     * Clamps the desktop left column width such that:
     * - It leaves enough room for both left and right panels based on available [totalWidth].
     */
    fun clampDesktopLeftColumnWidth(
        currentWidth: Dp,
        totalWidth: Dp,
        minWidth: Dp = MinDesktopLeftColumnWidth,
        foldGapDp: Dp = 0.dp
    ): Dp {
        if (totalWidth <= 0.dp) return currentWidth.coerceAtLeast(100.dp)
        val minLeft = minOf(minWidth, (totalWidth * 0.2f).coerceAtLeast(100.dp))
        val maxLeft = maxOf(minLeft, totalWidth - minLeft - foldGapDp)
        return currentWidth.coerceIn(minLeft, maxLeft)
    }
}

/**
 * A draggable divider bar used between panels in desktop mode.
 * Users can click and drag horizontally to resize panels.
 * Double-tapping the handle resets the splitter position to the platform default.
 */
@Composable
fun DesktopSplitterHandle(
    onDragStart: (pointerPositionXInHandleDp: Dp) -> Unit,
    onDoubleTap: () -> Unit = {},
    modifier: Modifier = Modifier,
    isDragging: Boolean = false,
    width: Dp = 10.dp,
    color: Color = MaterialTheme.colorScheme.outlineVariant,
    activeColor: Color = MaterialTheme.colorScheme.primary
) {
    val density = LocalDensity.current
    var isHovered by remember { mutableStateOf(false) }
    val isActive = isHovered || isDragging

    val handleColor by animateColorAsState(
        targetValue = if (isActive) activeColor else color,
        label = "SplitterLineColor"
    )

    val gripColor by animateColorAsState(
        targetValue = if (isActive) activeColor else MaterialTheme.colorScheme.outline,
        label = "SplitterGripColor"
    )

    val actualWidth = maxOf(10.dp, width)

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(actualWidth)
            .pointerHoverIcon(PointerIcon.Hand)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        when (event.type) {
                            PointerEventType.Enter -> isHovered = true
                            PointerEventType.Exit -> isHovered = false
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        onDoubleTap()
                    }
                )
            }
            .pointerInput(density) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startXDp = with(density) { down.position.x.toDp() }
                        onDragStart(startXDp)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Center thin vertical line
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(if (isActive) 2.dp else 1.dp)
                .background(handleColor)
        )

        // Always visible rounded grip handle indicator in the center
        Box(
            modifier = Modifier
                .width(if (isActive) 6.dp else 5.dp)
                .height(36.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(gripColor)
        )
    }
}

