package ru.quasaris.characternexus.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider

@Composable
actual fun DialogDimStyle(dimAmount: Float) {
    val view = LocalView.current
    SideEffect {
        val window = (view.parent as? DialogWindowProvider)?.window
        window?.setDimAmount(dimAmount)
        window?.setBackgroundDrawableResource(android.R.color.transparent)
    }
}
