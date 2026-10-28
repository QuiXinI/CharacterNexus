package ru.quasaris.characternexus.ui

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
}

@Composable
actual fun PredictiveBackHandler(
    enabled: Boolean,
    onBack: suspend (progress: Flow<BackEventData>) -> Unit
) {
}
