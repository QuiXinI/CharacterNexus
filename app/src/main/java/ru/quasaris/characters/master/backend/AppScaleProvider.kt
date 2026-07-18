package ru.quasaris.characters.master.backend

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

@Composable
fun AppScaleProvider(
    scaleFactor: Float,
    content: @Composable () -> Unit
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    // Override configuration with new fontScale
    val newConfiguration = android.content.res.Configuration(configuration).apply {
        fontScale *= scaleFactor
    }

    // Override density with new density and fontScale
    val newDensity = Density(
        density = density.density * scaleFactor,
        fontScale = density.fontScale * scaleFactor
    )

    CompositionLocalProvider(
        LocalConfiguration provides newConfiguration,
        LocalDensity provides newDensity
    ) {
        content()
    }
}
