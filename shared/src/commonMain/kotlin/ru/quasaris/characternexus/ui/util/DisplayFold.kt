package ru.quasaris.characternexus.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class FoldOrientation {
    VERTICAL,
    HORIZONTAL
}

enum class FoldState {
    FLAT,
    HALF_OPENED
}

@Immutable
data class DisplayFold(
    val isPresent: Boolean = false,
    val orientation: FoldOrientation = FoldOrientation.VERTICAL,
    val state: FoldState = FoldState.FLAT,
    val isSeparating: Boolean = false,
    val boundsLeftDp: Dp = 0.dp,
    val boundsTopDp: Dp = 0.dp,
    val boundsWidthDp: Dp = 0.dp,
    val boundsHeightDp: Dp = 0.dp
) {
    val isVerticalFold: Boolean
        get() = isPresent && orientation == FoldOrientation.VERTICAL && boundsLeftDp > 0.dp

    val isHorizontalFold: Boolean
        get() = isPresent && orientation == FoldOrientation.HORIZONTAL && boundsTopDp > 0.dp

    companion object {
        val NONE = DisplayFold()
    }
}

val LocalDisplayFold = staticCompositionLocalOf { DisplayFold.NONE }

@Composable
expect fun rememberDisplayFold(): DisplayFold
