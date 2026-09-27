package ru.quasaris.characternexus.ui

import android.annotation.SuppressLint
import androidx.activity.BackEventCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled, onBack)
}

@SuppressLint("UnusedFlow")
@Composable
actual fun PredictiveBackHandler(
    enabled: Boolean,
    onBack: suspend (progress: Flow<BackEventData>) -> Unit
) {
    PredictiveBackHandler(enabled = enabled) { progressFlow ->
        onBack(
            progressFlow.map { event ->
                BackEventData(
                    progress = event.progress,
                    swipeEdge = if (event.swipeEdge == BackEventCompat.EDGE_LEFT) BackEventData.EDGE_LEFT else BackEventData.EDGE_RIGHT,
                    touchX = event.touchX,
                    touchY = event.touchY
                )
            }
        )
    }
}
