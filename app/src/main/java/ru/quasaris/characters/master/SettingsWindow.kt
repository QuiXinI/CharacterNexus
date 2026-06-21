package ru.quasaris.characters.master

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsWindow(
    onOpenDrawer: () -> Unit
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    var isModernLayout by remember { mutableStateOf(settingsManager.isModernLayout) }
    val colorScheme = MaterialTheme.colorScheme

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Меню")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.surface,
                    titleContentColor = colorScheme.onSurface
                )
            )
        },
        containerColor = colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Интерфейс",
                style = MaterialTheme.typography.titleMedium,
                color = colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Современный M3 интерфейс",
                        fontSize = 16.sp,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = "Использовать новый компактный вид (выключите для Legacy)",
                        fontSize = 12.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isModernLayout,
                    onCheckedChange = {
                        isModernLayout = it
                        settingsManager.isModernLayout = it
                    }
                )
            }
            
            HorizontalDivider(color = colorScheme.outlineVariant)
            
            Text(
                text = "О приложении",
                style = MaterialTheme.typography.titleMedium,
                color = colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "D&D Character Sheet Master\nВерсия 2.0.0 (M3 Refactor)",
                fontSize = 14.sp,
                color = colorScheme.onSurfaceVariant
            )
        }
    }
}
