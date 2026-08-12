package ru.quasaris.characters.master.tabs.spells

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun ExportModuleDialog(
    initialName: String = "",
    initialDescription: String = "",
    initialVersion: String = "1.0.0",
    initialId: String = "",
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String, version: String, id: String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var description by remember { mutableStateOf(initialDescription) }
    var version by remember { mutableStateOf(initialVersion) }
    var id by remember { mutableStateOf(initialId) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Text("Экспорт модуля", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название модуля") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = id,
                    onValueChange = { id = it },
                    label = { Text("ID модуля (англ, без пробелов)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = version,
                    onValueChange = { version = it },
                    label = { Text("Версия") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("ОТМЕНА") }
                    Button(
                        onClick = { onConfirm(name, description, version, id) },
                        enabled = name.isNotBlank() && id.isNotBlank() && version.isNotBlank()
                    ) {
                        Text("ЭКСПОРТ")
                    }
                }
            }
        }
    }
}
