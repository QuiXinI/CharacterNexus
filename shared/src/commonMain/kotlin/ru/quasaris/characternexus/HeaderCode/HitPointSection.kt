package ru.quasaris.characternexus.HeaderCode

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState

@Composable
fun HealthDialog(
    showDialog: Boolean,
    hpDialogType: String,
    hpDialogValue: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
    isDesktop: Boolean = false
) {
    if (!showDialog) return

    if (isDesktop) {
        HealthDialogContent(
            hpDialogType = hpDialogType,
            hpDialogValue = hpDialogValue,
            onValueChange = onValueChange,
            onDismiss = onDismiss,
            onConfirm = onConfirm
        )
    } else {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthDialogContent(
    hpDialogType: String,
    hpDialogValue: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
    hazeState: HazeState? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val isOled = colorScheme.background == Color.Black

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        when (hpDialogType) {
                            "heal" -> "Лечение"
                            "damage" -> "Получение урона"
                            else -> "Временные Хиты"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = if (hazeState != null && !isOled) Color.Transparent else colorScheme.surface
                )
            )
        },
        containerColor = if (hazeState != null && !isOled) Color.Transparent else colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = hpDialogValue,
                onValueChange = { onValueChange(it.filter { c -> c.isDigit() }) },
                label = { Text("Значение") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Button(
                onClick = {
                    val v = hpDialogValue.toIntOrNull() ?: 0
                    onConfirm(v)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Применить", fontWeight = FontWeight.Bold)
            }
        }
    }
}
