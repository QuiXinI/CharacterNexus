package ru.quasaris.characternexus.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

@Immutable
data class BackEventData(
    val progress: Float,
    val swipeEdge: Int = EDGE_LEFT,
    val touchX: Float = 0f,
    val touchY: Float = 0f
) {
    companion object {
        const val EDGE_LEFT = 0
        const val EDGE_RIGHT = 1
    }
}

@Composable
expect fun BackHandler(enabled: Boolean = true, onBack: () -> Unit)

@Composable
expect fun PredictiveBackHandler(
    enabled: Boolean = true,
    onBack: suspend (progress: Flow<BackEventData>) -> Unit
)

/**
 * Higher-level Predictive Back Box that handles predictive gesture sliding left transform
 * while dragging, and triggers [onBack] when completed.
 * When [onBack] is triggered, the underlying container's existing enter/exit transitions execute.
 */
@Composable
fun PredictiveBackBox(
    enabled: Boolean = true,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (predictiveProgress: Float) -> Unit
) {
    val animatableProgress = remember { Animatable(0f) }

    PredictiveBackHandler(enabled = enabled) { progressFlow ->
        try {
            progressFlow.collect { backEvent ->
                animatableProgress.snapTo(backEvent.progress)
            }
            animatableProgress.snapTo(0f)
            onBack()
        } catch (e: CancellationException) {
            animatableProgress.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            throw e
        }
    }

    val progress = animatableProgress.value

    Box(
        modifier = modifier
            .graphicsLayer {
                val maxShift = if (size.width > 0f) size.width * 0.35f else 150.dp.toPx()
                translationX = -progress * maxShift
            }
    ) {
        content(progress)
    }
}
