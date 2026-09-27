package ru.quasaris.characternexus.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    val currentOnBack = rememberUpdatedState(onBack)
    
    DisposableEffect(enabled) {
        if (enabled) {
            val callback: () -> Boolean = {
                currentOnBack.value()
                true
            }
            BackNavigationManager.register(callback)
            onDispose {
                BackNavigationManager.unregister(callback)
            }
        } else {
            onDispose {}
        }
    }
}

@Composable
actual fun PredictiveBackHandler(
    enabled: Boolean,
    onBack: suspend (progress: Flow<BackEventData>) -> Unit
) {
    val scope = rememberCoroutineScope()
    val currentOnBack = rememberUpdatedState(onBack)

    BackHandler(enabled = enabled) {
        scope.launch {
            currentOnBack.value(emptyFlow())
        }
    }
}
