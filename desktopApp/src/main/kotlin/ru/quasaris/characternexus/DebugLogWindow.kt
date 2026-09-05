package ru.quasaris.characternexus

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import ru.quasaris.characternexus.ui.DebugLogScreen

@Composable
fun DebugLogWindow() {
    Window(
        onCloseRequest = {}, 
        title = "Live Logs",
        state = rememberWindowState(width = 800.dp, height = 600.dp)
    ) {
        DebugLogScreen(onDismiss = {})
    }
}
