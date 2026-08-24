package ru.quasaris.characternexus.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState

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
