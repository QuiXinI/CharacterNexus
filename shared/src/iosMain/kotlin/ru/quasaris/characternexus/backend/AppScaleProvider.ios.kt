package ru.quasaris.characternexus.backend

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
actual fun AppScaleProvider(
    scaleFactor: Float,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalAppScale provides scaleFactor) {
        content()
    }
}
