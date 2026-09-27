package ru.quasaris.characternexus.ui.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

@Composable
actual fun rememberDisplayFold(): DisplayFold {
    val context = LocalContext.current
    val density = LocalDensity.current
    val activity = remember(context) { context.findActivity() } ?: return DisplayFold.NONE

    val foldStateFlow = remember(activity, density) {
        WindowInfoTracker.getOrCreate(activity)
            .windowLayoutInfo(activity)
            .map { info ->
                val foldingFeature = info.displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull()
                if (foldingFeature == null) {
                    DisplayFold.NONE
                } else {
                    val orientation = if (foldingFeature.orientation == FoldingFeature.Orientation.VERTICAL) {
                        FoldOrientation.VERTICAL
                    } else {
                        FoldOrientation.HORIZONTAL
                    }
                    val state = if (foldingFeature.state == FoldingFeature.State.HALF_OPENED) {
                        FoldState.HALF_OPENED
                    } else {
                        FoldState.FLAT
                    }
                    val bounds = foldingFeature.bounds
                    with(density) {
                        DisplayFold(
                            isPresent = true,
                            orientation = orientation,
                            state = state,
                            isSeparating = foldingFeature.isSeparating,
                            boundsLeftDp = bounds.left.toDp(),
                            boundsTopDp = bounds.top.toDp(),
                            boundsWidthDp = bounds.width().toDp(),
                            boundsHeightDp = bounds.height().toDp()
                        )
                    }
                }
            }
            .distinctUntilChanged()
    }

    val displayFold by foldStateFlow.collectAsState(initial = DisplayFold.NONE)
    return displayFold
}
