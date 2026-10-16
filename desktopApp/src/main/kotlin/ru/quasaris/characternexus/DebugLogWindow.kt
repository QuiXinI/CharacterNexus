package ru.quasaris.characternexus

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import ru.quasaris.characternexus.ui.DebugLogScreen
import ru.quasaris.characternexus.backend.SettingsViewModel

@Composable
fun DebugLogWindow(
    onClose: () -> Unit = {},
    settingsViewModel: SettingsViewModel? = null
) {
    Window(
        onCloseRequest = onClose, 
        title = "Логи и Отладка",
        state = rememberWindowState(width = 800.dp, height = 600.dp)
    ) {
        DebugLogScreen(
            onDismiss = onClose,
            settingsViewModel = settingsViewModel
        )
    }
}
