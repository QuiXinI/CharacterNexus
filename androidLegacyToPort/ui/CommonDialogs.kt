package ru.quasaris.characters.master.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import ru.quasaris.characters.master.backend.SettingsViewModel

@Composable
fun DeleteConfirmationDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    title: String = "Подтвердите удаление",
    text: String = "Это действие нельзя будет отменить.",
    settingsViewModel: SettingsViewModel? = null
) {
    val deletionWarningEnabled by settingsViewModel?.deletionWarningEnabled?.collectAsState() ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }

    if (!deletionWarningEnabled && showDialog) {
        onConfirm()
        return
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title) },
            text = { Text(text) },
            confirmButton = {
                TextButton(onClick = {
                    onConfirm()
                    onDismiss()
                }) {
                    Text("УДАЛИТЬ", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("ОТМЕНА")
                }
            }
        )
    }
}
