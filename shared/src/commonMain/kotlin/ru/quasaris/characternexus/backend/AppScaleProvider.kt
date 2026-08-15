package ru.quasaris.characternexus.backend

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

val LocalAppScale = staticCompositionLocalOf { 1.0f }

@Composable
expect fun AppScaleProvider(
    scaleFactor: Float,
    content: @Composable () -> Unit
)
