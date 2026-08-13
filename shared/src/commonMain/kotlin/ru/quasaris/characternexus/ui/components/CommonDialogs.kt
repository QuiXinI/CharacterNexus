package ru.quasaris.characternexus.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun HealthDialog(
    showDialog: Boolean,
    hpDialogType: String,
    hpDialogValue: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    when (hpDialogType) {
                        "heal" -> "Лечение"
                        "damage" -> "Получение урона"
                        else -> "Временные Хиты"
                    }
                )
            },
            text = {
                OutlinedTextField(
                    value = hpDialogValue,
                    onValueChange = { onValueChange(it.filter { c -> c.isDigit() }) },
                    label = { Text("Значение") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val v = hpDialogValue.toIntOrNull() ?: 0
                    onConfirm(v)
                }) { Text("ОК") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Отмена") }
            }
        )
    }
}

@Composable
fun DeleteConfirmationDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    title: String = "Подтвердите удаление",
    text: String = "Это действие нельзя будет отменить."
) {
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
