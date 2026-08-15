package ru.quasaris.characternexus.backend

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

@Composable
actual fun AppScaleProvider(
    scaleFactor: Float,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current

    val newDensity = Density(
        density = density.density * scaleFactor,
        fontScale = density.fontScale * scaleFactor
    )

    CompositionLocalProvider(
        LocalDensity provides newDensity,
        LocalAppScale provides scaleFactor
    ) {
        content()
    }
}
